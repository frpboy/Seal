# Native licensing qualification status

This is an engineering checklist, not a legal opinion or release clearance.

## Observed notices and configuration

The app's GPLv3 LICENSE and existing attribution remain intact. The inherited
FFmpeg 0.18.1 executable in each of four ABIs contains an embedded configure
string with --enable-gpl and --enable-version3. FFmpeg documents that combination
as GPLv3. Credits therefore identify FFmpeg as GPLv3, not GPLv2. This is binary
configuration evidence, not proof of the full producing source/build inputs.

Official references inspected November 8, 2026:
- https://ffmpeg.org/doxygen/trunk/md_LICENSE.html
- https://ffmpeg.org/legal.html

libwebp 1.6.0 uses its supplied BSD-style COPYING and includes AUTHORS/PATENTS.
Those three files remain unchanged under native/webp16k and are now included
under app/src/main/assets/third_party/libwebp for packaged access. Credits names
libwebp. Source release must carry those same files and the exact source/build
recipe, version and checksum in REVIEW.md; no attribution was removed.

## Release gates still open

- Verify the final APK actually contains the three assets and inspect Credits UI.
- Recover and preserve corresponding source, modifications and build recipes for
  inherited FFmpeg and all bundled libraries, including unchanged payloads. A
  list of library names, a Maven AAR, or configure string is not corresponding
  source. Existing upstream provenance gaps are not resolved by this correction.
- Package complete applicable third-party notices and source delivery/offer in
  the release workflow. GPL/BSD labels alone do not discharge those obligations.
- Confirm any source offer/source archive is accessible to the binary recipient
  and corresponds to that exact distributed version. Retain checksums and links.
- Preserve existing GPLv3 app license and modification/upstream attribution.

No qualified public release is authorized by this checklist. Runtime and
compatibility qualification are separate from licensing qualification.
