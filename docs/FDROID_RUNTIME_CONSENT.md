# F-Droid runtime update consent preparation

This source preparation is not a store submission, published build or F-Droid acceptance claim.

The existing `fdroid` flavor retains its package/version/signing configuration.
A new, default-false consent setting is separate from legacy automatic-update settings.
The automatic entry point checks it before the empty-version bootstrap and before network work.
The central update utility checks admission again before downloading executable code.

Both manual update entry points ask before one download. This does not turn on automatic updates.
The existing channel/interval dialog asks separately before automatic downloads and can turn them off.
The warning names stable/nightly GitHub sources, explains that executable code bypasses F-Droid
review/update checks, and offers refusal without requiring consent to use the bundled version.
Play's existing refusal and other flavors' existing behavior are preserved.

## Qualification still required

- Actual Android compilation of the Compose/resource changes and actual variant tests.
- Actual fresh F-Droid install and upgrade from legacy settings: no external executable download
  before consent, including empty version and old automatic-update=true.
- Decline, dismissal, rotation/navigation/cancellation, repeated-click and manual one-shot paths.
- Automatic opt-in, interval/channel changes, opt-out and stable/nightly source behavior.
- Real dialog pixels on narrow screens and large fonts, with equally usable refusal.
- Network observation at the central download boundary; source tests alone do not prove this.
- Existing native/source/notices/signing/runtime release gates remain separate.

No release build, signing, public APK upload, emulator experiment, deployment or store submission
is part of this preparation. The pure Kotlin/JUnit admission tests and Python source-boundary
checks are intentionally narrower than Android runtime or network qualification.
