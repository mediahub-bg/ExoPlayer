/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.exoplayer2.mediacodec;

import android.media.MediaCodec;
import android.media.MediaCrypto;
import android.media.MediaFormat;
import android.os.HandlerThread;
import android.view.Surface;
import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.decoder.CryptoInfo;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A {@link MediaCodecAdapter} that operates the underlying {@link MediaCodec} in asynchronous mode,
 * routes {@link MediaCodec.Callback} callbacks on a dedicated thread that is managed internally,
 * and queues input buffers asynchronously.
 */
@RequiresApi(23)
/* package */ final class AsynchronousMediaCodecAdapter implements MediaCodecAdapter {

  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @IntDef({STATE_CREATED, STATE_CONFIGURED, STATE_STARTED, STATE_SHUT_DOWN})
  private @interface State {}

  private static final int STATE_CREATED = 0;
  private static final int STATE_CONFIGURED = 1;
  private static final int STATE_STARTED = 2;
  private static final int STATE_SHUT_DOWN = 3;

  private final MediaCodec codec;
  private final AsynchronousMediaCodecCallback asynchronousMediaCodecCallback;
  private final AsynchronousMediaCodecBufferEnqueuer bufferEnqueuer;
  @State private int state;

  /**
   * Creates an instance that wraps the specified {@link MediaCodec}.
   *
   * @param codec The {@link MediaCodec} to wrap.
   * @param trackType One of {@link C#TRACK_TYPE_AUDIO} or {@link C#TRACK_TYPE_VIDEO}. Used for
   *     labelling the internal thread accordingly.
   */
  /* package */ AsynchronousMediaCodecAdapter(MediaCodec codec, int trackType) {
    this(
        codec,
        new HandlerThread(createCallbackThreadLabel(trackType)),
        new HandlerThread(createQueueingThreadLabel(trackType)));
  }

  @VisibleForTesting
  /* package */ AsynchronousMediaCodecAdapter(
      MediaCodec codec, HandlerThread callbackThread, HandlerThread enqueueingThread) {
    this.codec = codec;
    this.asynchronousMediaCodecCallback = new AsynchronousMediaCodecCallback(callbackThread);
    this.bufferEnqueuer = new AsynchronousMediaCodecBufferEnqueuer(codec, enqueueingThread);
    this.state = STATE_CREATED;
  }

  @Override
  public void configure(
      @Nullable MediaFormat mediaFormat,
      @Nullable Surface surface,
      @Nullable MediaCrypto crypto,
      int flags) {
    asynchronousMediaCodecCallback.initialize(codec);
    codec.configure(mediaFormat, surface, crypto, flags);
    state = STATE_CONFIGURED;
  }

  @Override
  public void start() {
    bufferEnqueuer.start();
    codec.start();
    state = STATE_STARTED;
  }

  @Override
  public void queueInputBuffer(
      int index, int offset, int size, long presentationTimeUs, int flags) {
    bufferEnqueuer.queueInputBuffer(index, offset, size, presentationTimeUs, flags);
  }

  @Override
  public void queueSecureInputBuffer(
      int index, int offset, CryptoInfo info, long presentationTimeUs, int flags) {
    bufferEnqueuer.queueSecureInputBuffer(index, offset, info, presentationTimeUs, flags);
  }

  @Override
  public int dequeueInputBufferIndex() {
    return asynchronousMediaCodecCallback.dequeueInputBufferIndex();
  }

  @Override
  public int dequeueOutputBufferIndex(MediaCodec.BufferInfo bufferInfo) {
    return asynchronousMediaCodecCallback.dequeueOutputBufferIndex(bufferInfo);
  }

  @Override
  public MediaFormat getOutputFormat() {
    return asynchronousMediaCodecCallback.getOutputFormat();
  }

  @Override
  public void flush() {
    // The order of calls is important:
    // First, flush the bufferEnqueuer to stop queueing input buffers.
    // Second, flush the codec to stop producing available input/output buffers.
    // Third, flush the callback after flushing the codec so that in-flight callbacks are discarded.
    bufferEnqueuer.flush();
    codec.flush();
    // When flushAsync() is completed, start the codec again.
    asynchronousMediaCodecCallback.flushAsync(/* onFlushCompleted= */ codec::start);
  }

  @Override
  public void shutdown() {
      if (state == STATE_STARTED) {
        bufferEnqueuer.shutdown();
      }
      if (state == STATE_CONFIGURED || state == STATE_STARTED) {
      asynchronousMediaCodecCallback.shutdown();
      }
      state = STATE_SHUT_DOWN;
  }

  @Override
  public MediaCodec getCodec() {
    return codec;
  }

  @VisibleForTesting
  /* package */ void onError(MediaCodec.CodecException error) {
    asynchronousMediaCodecCallback.onError(codec, error);
  }

  @VisibleForTesting
  /* package */ void onOutputFormatChanged(MediaFormat format) {
    asynchronousMediaCodecCallback.onOutputFormatChanged(codec, format);
  }

  private static String createCallbackThreadLabel(int trackType) {
    return createThreadLabel(trackType, /* prefix= */ "ExoPlayer:MediaCodecAsyncAdapter:");
  }

  private static String createQueueingThreadLabel(int trackType) {
    return createThreadLabel(trackType, /* prefix= */ "ExoPlayer:MediaCodecQueueingThread:");
  }

  private static String createThreadLabel(int trackType, String prefix) {
    StringBuilder labelBuilder = new StringBuilder(prefix);
    if (trackType == C.TRACK_TYPE_AUDIO) {
      labelBuilder.append("Audio");
    } else if (trackType == C.TRACK_TYPE_VIDEO) {
      labelBuilder.append("Video");
    } else {
      labelBuilder.append("Unknown(").append(trackType).append(")");
    }
    return labelBuilder.toString();
  }
}