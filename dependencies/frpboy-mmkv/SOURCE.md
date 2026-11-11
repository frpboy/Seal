# MMKV source build contract

Upstream Tencent/MMKV v1.3.17 commit8bf97e7816dbfe016273cc2b8f5c3f1b36f9621c, archive SHA2561593761a62bd7ffe5eaa90a1530dc2c9e587110081da00f230557cc0c3fb3179. Source: https://codeload.github.com/Tencent/MMKV/tar.gz/refs/tags/v1.3.17 . Java/AIDL/consumer rules are unchanged upstream. License retained.

Run scripts/mmkv-source/build-mmkv-arm64.sh with verified NDKr28c, SOURCE_ARCHIVE and fresh absolute WORK. Recipe builds ARM64/API24, same staticSTL/defaultflavor, checks canonical16K layout and API24 imports/NEEDED set. The recipe retains WORK/build/libmmkv.so unstripped and emits WORK/packaging/libmmkv.so with r28c llvm-strip --strip-unneeded; both hashes and stripped layout audit are recorded. Retain unstripped custody and exact source/tool/build command receipts. Current reviewed stripped artifact SHA2566544cd777e2b7bfdc450dcfbb22e1145062e9077296700f1251abcd03724fae0 (610544B). Never claim rebuilt bytes identical without checking. No prebuilt .so is committed.

Stage admitted source-built native at src/main/jniLibs/arm64-v8a/libmmkv.so before app packaging. This module supplies only ARM64; not a multiABI drop-in. Java-only/source CI does not establish native packaging or execution. Existing native packaging check must fail if absent or mismatched. No app/store runtime assertion; synthetic1.3.12-to1.3.17 store reopen and AndroidJNI/olderART still required. NDKr28c differs upstreamr28.1.

Packaging guard verifies exact current SHA and regular non-symlink file. A fresh build with different path/tool-induced bytes must pass independent source/ELF/import review and a deliberate new pin, not automatic acceptance. This is a pinned recipe with audits, not a byte reproducibility claim.
