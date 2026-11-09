# Seali showcase assets

## Logo

The README uses the actual 512 x 512 Seali icon at
[`fastlane/metadata/android/en-US/images/icon.png`](../../fastlane/metadata/android/en-US/images/icon.png).
It matches the launcher identity introduced in
[PR6's merged commit](https://github.com/frpboy/Seali/commit/7e623fa4dba214b5a6790f3983e41c99613b3ec2).
The logo is part of this repository, covered by its [license](../../LICENSE).
Do not attribute the new chevron mark to the original Seal artwork.

## Screenshots

The existing nine Fastlane screenshots are historical upstream Seal screens.
They are not current Seali screenshots and must not be presented as a preview
of the latest Seali source or release.

New public screenshots need actual rendered Seali screens, a recorded source
commit/version and a clear distinction between published builds and unreleased
changes. Use clean local demonstration data. Exclude personal download history,
private media links, account details, notifications, cookies and credentials.
Do not replace actual screens with fabricated product mockups.

A suitable runtime capture route has not yet been admitted for current source.
Do not imply that the pending contact/About or queue-clarity changes have
shipped because their source or unit tests passed. Preserve their separate
visual checks before using them in a current-source gallery.

## Existing store metadata

The proper-name title is `Seali` in all 21 existing locale title files. The
English description now distinguishes inherited capabilities, alpha.5 public
APKs and alpha.6 unreleased source. Other translated descriptions are retained,
not newly verified translations. This edit does not submit the app to a store.

Keep using the existing Fastlane icon/screenshot structure. The nine historical
images stay unchanged until a real, reviewed Seali capture set can replace them;
do not pass them off as current screenshots in a listing or README gallery.

## User-device gallery

Four actual user-supplied captures, received November 9, 2026, are stored in `alpha6-device/`. The installed version is reported as 2.0.0-alpha.6 (200000162), Android16/API36; the drawer independently displays 2.0.0-alpha.6. Exact installed APK hash/source commit is not established. These are not captures of PR30, PR38, PR41 or the latest source, and do not close their runtime/visual gates. Public APK release status is unchanged.

The queue image is an empty All tab, not successful-download evidence. The drawer is cropped below the notification and inside its right edge, retaining its actual alpha.6 label. The video and audio images are rectangular crops below the sheet's rounded top corners, excluding the other app behind them. These crops remove the other app, rounded sheet corners, top margin and drag handle while retaining the visible in-sheet chevron glyph and configuration controls. No pixels were painted, redrawn, recolored, selectively blurred or substituted. No personal history, failed-item URL, private media, notification, account detail or raw capture is included.

Pixel crop coordinates and output SHA256 are recorded in `alpha6-device/provenance.json`. Coordinates refer to original784x1599 images. Raw captures remain private, outside the repository. Exported PNGs contain no EXIF/source-image metadata. Historical upstream Fastlane screenshots remain untouched and are not in this gallery; no store screenshot acceptance is claimed.
