# Seali security policy

Seali is an independently maintained Android downloader. It is alpha software, not a security-audited or production-qualified release. Repository branding and successful compilation do not establish security of the app or its bundled native dependencies.

## Reporting a suspected vulnerability

Email **Rahul Muraleedharan** at **hi@frpboy.in**, the public maintainer contact listed at https://www.frpboy.in/. Use a subject such as "Seali security report".

Do not open a public issue containing exploit details, private download URLs, cookies, credentials, personal files or logs containing secrets. Start with a brief, redacted description and ask how to share sensitive evidence. This contact is not an encrypted reporting service. Do not send passwords or authentication tokens.

Useful non-sensitive details include the Seali version and APK source, Android version, affected feature, reproduction steps using synthetic data, expected and observed behavior, and likely impact. Test only on your own devices and data, or with the owner's permission.

## Maintenance and support limits

The published alpha and current main may differ. Check [SEALI_STATUS.md](SEALI_STATUS.md) and include the exact version or commit in a report. No release currently has a guaranteed security-support period, response deadline or fix deadline. A report does not imply acceptance of a security claim or a bounty; no bounty program is offered here.

Reports can inform investigation, fixes and release notes after review. Do not assume that reporting here also notifies upstream projects. The maintainer may need a separately coordinated upstream report where a dependency is affected.

## Dependencies and scope

Known native dependency provenance and qualification gaps are recorded in [docs/NATIVE_DEPENDENCY_PROVENANCE.md](docs/NATIVE_DEPENDENCY_PROVENANCE.md). Required third-party licenses and credits remain in place. This policy does not claim those gaps are closed or that every reported vulnerability applies to the shipped configuration.

For ordinary bugs and feature requests, use [Seali issues](https://github.com/frpboy/Seali/issues) and [CONTRIBUTING.md](CONTRIBUTING.md). For community conduct concerns, see [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md).
