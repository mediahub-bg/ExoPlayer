package com.google.android.exoplayer2.source.hls.playlist;

import androidx.annotation.Nullable;
import com.google.android.exoplayer2.upstream.ParsingLoadable;

public final class PlayAtStartHlsPlaylistParserFactory implements HlsPlaylistParserFactory {

  @Override
  public ParsingLoadable.Parser<HlsPlaylist> createPlaylistParser() {
    ParsingLoadable.Parser<HlsPlaylist> parser =
        new DefaultHlsPlaylistParserFactory().createPlaylistParser();
    return new PlayAtStartHlsPlaylistParser(parser);
  }

  @Override
  public ParsingLoadable.Parser<HlsPlaylist> createPlaylistParser(
      HlsMultivariantPlaylist multivariantPlaylist,
      @Nullable HlsMediaPlaylist previousMediaPlaylist) {
    ParsingLoadable.Parser<HlsPlaylist> parser =
        new DefaultHlsPlaylistParserFactory().createPlaylistParser(
            multivariantPlaylist, previousMediaPlaylist);
    return new PlayAtStartHlsPlaylistParser(parser);
  }
}
