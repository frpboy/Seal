# Downloader dependency audit

Reviewed on 2026-11-07 against main `aed0630a`.

## Scope and change

The selected generic release runtime graph included Jackson core/databind 2.11.1,
Commons IO 2.5 and Commons Compress 1.12 through youtubedl-android 0.17.3.
OSV returned 28 advisory records with 28 distinct CVE aliases. This is a
version-based inventory, not proof of 28 exploitable app vulnerabilities.

- Align Jackson annotations/core/databind with the 2.18.11 BOM, staying on Jackson 2.
- Require Compress 1.26.0 and IO at least 2.14.0. Compress selects IO 2.15.1.
- Require Lang 3.18.0 because Compress newly introduces Lang 3.14.0, affected by
  GHSA-j288-q9x7-2f5v. This constraint avoids trading old advisories for a new one.
- Keep youtubedl-android 0.17.3 and its native runtime unchanged.

The patched selected graph contains 195 Maven coordinates. A fresh OSV
querybatch returned no advisory records for those selected versions. This does
not cover native binaries, Python packages, future advisories or all app logic.

## Call-site review

The published 0.17.3 source uses a plain ObjectMapper for video metadata and
updater JSON. JSON resource exhaustion is relevant to untrusted metadata.
No polymorphic/default typing, JsonView, JDK JsonNode serialization, or
InetAddress/Path target types were found in the reviewed wrapper. Advisories
requiring those paths are not independently demonstrated as reachable.

Commons ZIP APIs are used to unpack the bundled runtime. ZIP parser changes
therefore need compatibility tests even though the installed archive input is
packaged with the app. DUMP/7z/TAR-specific findings are not evidence of reachable
attacks through this ZIP-only wrapper. IO usage is copy/delete/text conversion;
XmlStreamReader and FilenameUtils.normalize were not found in that wrapper.

## Qualification

The owner approved Android 8.0 / API26 as the new minimum on 2026-11-07,
dropping Android 7.0 and 7.1. This chooses the version-only dependency update;
the experimental custom wrapper in PR24 is not adopted. The production
minSdk is now 26 and the supported-device matrix is API26 and API35. This is
an explicit product support change, not a skipped supported-device failure.
Published alpha assets remain unchanged.

Earlier API24 qualification failed ObjectMapper initialization because Jackson
2.18 requires SDK26 (`java.lang.BootstrapMethodError` missing), and Compress
ZIP paths failed with missing `java.nio.file.LinkOption`. Preserved evidence:
https://github.com/frpboy/Seal/actions/runs/42344698544

Before the minimum change, exact head b577a9997d892660f2065f09ed03fc080c115e79
passed release/R8 packaging and ten actual instrumentation tests on both API26
and API35: persisted/reactive history, header callbacks and cancel/retry
callback dispatch, JSON shapes, stream/file ZIP extraction, traversal rejection,
and bundled downloader `--version` offline. Evidence:
https://github.com/frpboy/Seal/actions/runs/42352192460

Actual screenshots show normal and 2x-font keyboard controls readable on API35
and 2x-font keyboard controls readable on API26. The API26 normal-font final
screenshot has no keyboard despite a native visibility signal; that specific
keyboard-visible case is NOT closed by the green test. Focus and geometry are
not substitutes for final pixel inspection.

The minSdk26 configuration and integrated cancellation fixes passed release/R8
packaging and twelve actual tests on API26/API35 at exact head
7c017f220daa8f83c67a108cc4df0838070c7861:
https://github.com/frpboy/Seal/actions/runs/42359320631

A real loopback transfer test exposed upstream cancellation returning true on
API26 while the download continued successfully. Evidence:
https://github.com/frpboy/Seal/actions/runs/42355363772
The app now tracks its execution threads and interrupts the wrapper's blocking
wait, whose existing InterruptedException handler destroys the Process. No
reflection, shell command or copied upstream wrapper was added. Metadata,
legacy/v2 downloads, custom commands, dialog and notification cancellation use
this boundary. Tests require partial bytes, cancellation without a completed
file, retry and exact recovered bytes. An app-level custom-command test also
caught restart incorrectly routing to ReadyWithInfo despite having no parsed
VideoInfo. Custom-command restart now routes to Idle; regular downloads keep
ReadyWithInfo. The app task test now passes Canceled -> restart -> Completed.
This does not establish cancellation of every ffmpeg/aria2 child process.

A thirteenth fixture checks FileProvider cache-file read bytes, opening/sharing
intent MIME and read-grant flags, and missing-file rejection. Fresh exact-head
CI is required for that fixture and any further changes.
These tests do not prove real network download cancellation/retry, provider or
storage permissions, network updater behavior, recreation, TalkBack, reduced
motion, native/Python security, or overall release readiness. Those release
gates remain separate. Do not publish merely because this matrix is green.

Primary compatibility references:
- https://github.com/FasterXML/jackson/wiki/Jackson-Release-2.18
- https://github.com/FasterXML/jackson-databind/issues/4653

## Primary sources

- https://repo.maven.apache.org/maven2/io/github/junkfood02/youtubedl-android/library/0.17.3/library-0.17.3-sources.jar
- https://repo.maven.apache.org/maven2/io/github/junkfood02/youtubedl-android/common/0.17.3/common-0.17.3-sources.jar
- https://repo.maven.apache.org/maven2/com/fasterxml/jackson/jackson-bom/2.18.11/jackson-bom-2.18.11.pom
- https://repo.maven.apache.org/maven2/org/apache/commons/commons-compress/1.26.0/commons-compress-1.26.0.pom
- https://api.osv.dev/v1/querybatch
- https://api.osv.dev/v1/vulns/GHSA-j288-q9x7-2f5v
- https://api.osv.dev/v1/vulns/GHSA-gwrp-pvrq-jmwv
- https://api.osv.dev/v1/vulns/GHSA-hrmr-f5m6-m9pq
