#include <stdint.h>
#include <stdio.h>
#include <string.h>
#include <webp/encode.h>
#include <webp/decode.h>
#include <webp/mux.h>
#include <webp/demux.h>

#define CHECK(x) do { if (!(x)) { fprintf(stderr, "FAIL line %d: %s\n", __LINE__, #x); return 1; } } while (0)
int main(void) {
  const uint8_t rgba[] = {255,0,0,255, 0,255,0,255, 0,0,255,255, 255,255,255,255};
  uint8_t *encoded = NULL, *decoded = NULL;
  int width = 0, height = 0;
  size_t size = WebPEncodeLosslessRGBA(rgba, 2, 2, 8, &encoded);
  CHECK(size > 0 && encoded);
  CHECK(WebPGetInfo(encoded, size, &width, &height) && width == 2 && height == 2);
  decoded = WebPDecodeRGBA(encoded, size, &width, &height);
  CHECK(decoded && width == 2 && height == 2 && !memcmp(decoded, rgba, sizeof(rgba)));
  WebPFree(decoded);
  WebPData input = {encoded, size}, assembled = {0};
  WebPMux *mux = WebPMuxCreate(&input, 1);
  CHECK(mux);
  const uint8_t metadata[] = {'s','e','a','l','i'};
  WebPData exif = {metadata, sizeof(metadata)};
  CHECK(WebPMuxSetChunk(mux, "EXIF", &exif, 1) == WEBP_MUX_OK);
  CHECK(WebPMuxAssemble(mux, &assembled) == WEBP_MUX_OK);
  WebPDemuxer *demux = WebPDemux(&assembled);
  CHECK(demux && WebPDemuxGetI(demux, WEBP_FF_CANVAS_WIDTH) == 2);
  WebPChunkIterator chunk;
  CHECK(WebPDemuxGetChunk(demux, "EXIF", 1, &chunk));
  CHECK(chunk.chunk.size == sizeof(metadata) && !memcmp(chunk.chunk.bytes, metadata, sizeof(metadata)));
  WebPDemuxReleaseChunkIterator(&chunk);
  WebPDemuxDelete(demux);
  WebPMuxDelete(mux);
  WebPDataClear(&assembled);
  WebPFree(encoded);
  encoded = NULL;
  size = WebPEncodeRGBA(rgba, 2, 2, 8, 85.0f, &encoded);
  CHECK(size > 0 && encoded);
  decoded = WebPDecodeRGBA(encoded, size, &width, &height);
  CHECK(decoded && width == 2 && height == 2);
  WebPFree(decoded);
  WebPFree(encoded);
  CHECK(WebPDecodeRGBA((const uint8_t *)"invalid", 7, &width, &height) == NULL);
  printf("PASS WebP %06x: lossless pixels, lossy decode, mux/demux metadata, invalid decode\n", WebPGetDecoderVersion());
  return 0;
}
