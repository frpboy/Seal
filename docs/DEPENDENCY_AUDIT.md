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

Local generic debug JVM tests passed, including four new tests for upstream
VideoInfo parsing, updater JSON tree shape, both upstream ZIP extraction APIs,
and the existing traversal guard. Android instrumentation compilation passed.
The instrumentation suite also exercises the same APIs on Android and starts
the bundled downloader with `--version`, offline. CI runtime results must be
checked on the exact PR head before merging. These tests do not substitute for
provider/storage, updater-network, accessibility or full download testing.

Local release/R8 qualification could not finish within the local resource
budget. It is pending CI, not a passing local release build.

## Primary sources

- https://repo.maven.apache.org/maven2/io/github/junkfood02/youtubedl-android/library/0.17.3/library-0.17.3-sources.jar
- https://repo.maven.apache.org/maven2/io/github/junkfood02/youtubedl-android/common/0.17.3/common-0.17.3-sources.jar
- https://repo.maven.apache.org/maven2/com/fasterxml/jackson/jackson-bom/2.18.11/jackson-bom-2.18.11.pom
- https://repo.maven.apache.org/maven2/org/apache/commons/commons-compress/1.26.0/commons-compress-1.26.0.pom
- https://api.osv.dev/v1/querybatch
- https://api.osv.dev/v1/vulns/GHSA-j288-q9x7-2f5v
- https://api.osv.dev/v1/vulns/GHSA-gwrp-pvrq-jmwv
- https://api.osv.dev/v1/vulns/GHSA-hrmr-f5m6-m9pq
