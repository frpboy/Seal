# Isolated ARM64 owned dependency integration unit

Based on PR54 head702cd02c5a676bda11d4534b74c09580ab6218b3. Not committed or dispatched. Matching MMKV1.3.17 source Java/AIDL/consumer rules + independently source-built ARM64 native are local project dependencies; compileOnly upstream FastNative stub is not shipped. No fake runtime annotation class. NDKr28c differs upstreamr28.1. API24 preserved. No multi-ABI assertion.

Python source producer3.11.18 + Cryptodome3.24.2 output38f6e236ba307217895ffd180569b6cacb7c854cc43ad8c9eac461c3430638df and launcher9726de75497c51d033dd1b4bdb9931344815bb3260633d7aa46587e7ea8ab3ce are staged only by scripts/stage-source-owned-python.py after exact size/hash verification. Media payload unchanged. QuickJS source/recipe unchanged, emitted local ELF differs from historical CI ELF due absolute __FILE__ build-root strings; prefix-mapped reproduction exactly recreates historical bytes. Runtime proof does not transfer between builds. No inherited Python fallback selected.

OPENSSL_CONF selects dynamic PYTHONHOME/etc/tls/openssl.cnf in Python invocation only. Descendants inherit naturally; unrelated ffmpeg/aria2 ProcessBuilders untouched. Actual extracted118B config matches ledgerdc0489b417d3ec948a5368da397dd00f4eba360e7170883d577d1f25e15c697d. This is packaging/environment proof, not OpenSSL runtime loading/TLS proof.

Complete corresponding source retained: baseline521b89e816949445036fd01de7241324aa4da443ba25f338d59669d188404d07 + refresh e982ac7d619733c511a8e88ca5dbc87190f9c76394b03284c3b952d12e2bc0ae. Both independently reconstructed/hash-verified and all693baseline/930refresh file hashes checked. Retained independently built MMKV source/native custody is separate. Public source delivery/relink and final licensing review remain release gates, not satisfied by private custody alone.

Actual local helper/wrapper tests11PASS, MMKV AAR build, app Kotlin compile and debug APK build succeeded. Debug signing is local non-production, not release family. Independent final APK inspection is a separate receipt. No Android execution, JNI registration/store reopen, TLS/XML, GCM, download/EJS/merge/cancel/reaping, installed identity/data-preserving upgrade or release result. Source review required before any merge/publication. Never uninstall or clear user data to test.

functional-alpha-payload-manifest.json is a staging record: apk_built=false means the staging step did not build an APK. Actual later debug build is documented by separate receipt; target_executed remains false.

Repository promotion includes source modules, pinned build/audit recipes and staging contracts, not generated native payloads. Native MMKV .so must be source-built/audited/staged before packaging; mergeNativeLibs fails closed if missing. Existing source-only CI can compile/tests without pretending native qualification. Source producer CI wiring is still pending and legacy native workflow must not be dispatched for these new inputs.
