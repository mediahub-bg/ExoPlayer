package com.google.android.exoplayer2.source.hls.playlist;

import android.net.Uri;
import com.google.android.exoplayer2.upstream.ParsingLoadable;
import java.io.IOException;
import java.io.InputStream;

public final class PlayAtStartHlsPlaylistParser implements ParsingLoadable.Parser<HlsPlaylist> {

  private final ParsingLoadable.Parser<HlsPlaylist> parser;

  public PlayAtStartHlsPlaylistParser(ParsingLoadable.Parser<HlsPlaylist>parser) {
    this.parser = parser;
  }

  @Override
  public HlsPlaylist parse(Uri uri, InputStream inputStream) throws IOException {
    HlsPlaylist playlist = parser.parse(uri, inputStream);
    if (playlist instanceof HlsMediaPlaylist) {
      HlsMediaPlaylist srcPlaylist = (HlsMediaPlaylist) playlist;
      playlist = new HlsMediaPlaylist(
          srcPlaylist.playlistType,
          srcPlaylist.baseUri,
          srcPlaylist.tags,
          0,
          srcPlaylist.preciseStart,
          srcPlaylist.startTimeUs,
          srcPlaylist.hasDiscontinuitySequence,
          srcPlaylist.discontinuitySequence,
          srcPlaylist.mediaSequence,
          srcPlaylist.version,
          srcPlaylist.targetDurationUs,
          srcPlaylist.partTargetDurationUs,
          srcPlaylist.hasIndependentSegments,
          srcPlaylist.hasEndTag,
          srcPlaylist.hasProgramDateTime,
          srcPlaylist.protectionSchemes,
          srcPlaylist.segments,
          srcPlaylist.trailingParts,
          srcPlaylist.serverControl,
          srcPlaylist.renditionReports
      );
    }
    return playlist;
  }
}
