# Seali accessibility statement

Seali aims to make its Android downloader understandable and usable with different input methods and assistive technologies. Accessibility is part of review, not a conformance claim.

## Environments and current evidence

The current source targets Android and contains both Classic and v2 interfaces. See [BUILDING.md](BUILDING.md) for the actual build requirements and [SEALI_STATUS.md](SEALI_STATUS.md) for published-build limits. Changing between interfaces can affect layout and navigation and needs testing in both modes.

There is no completed accessibility audit or published conformance report for the current Seali build. TalkBack, switch access, hardware keyboard navigation, focus order, large text, display scaling, contrast and reduced-animation behavior have not been fully qualified across both interfaces. No WCAG or other accessibility-standard conformance is claimed. A successful build or screenshot alone does not prove accessibility.

## Reporting a barrier

Use [Seali issues](https://github.com/frpboy/Seali/issues) for a non-sensitive accessibility report, or email the current maintainer, **Rahul Muraleedharan**, at **frpboy12@gmail.com**. Do not include private download links, cookies, credentials or personal media.

Helpful details include:

- Seali version and download source; Android and device versions.
- Classic or v2 interface and the affected screen or task.
- Assistive technology, input method, text/display scaling and relevant settings.
- Steps, expected behavior and observed barrier. Include redacted screenshots only when useful and safe.

No response deadline or fix deadline is promised. Reports help identify work, but this statement does not mean every barrier is already fixed.

## Contributing improvements

See [CONTRIBUTING.md](CONTRIBUTING.md). Keep labels, content descriptions and focus behavior meaningful. Validate changes with the relevant assistive technology where possible, preserve unrelated behavior, and state exactly what was tested or remains unverified. Do not weaken tests or remove accessibility checks to obtain a passing build.
