# Maintenance candidate against official QuickJS2026-06-04

This is a review candidate, not full security clearance. Combined patch changes only quickjs.c and libunicode.c; no Intl/Temporal/Atomics.waitAsync or new compiler proposals are added.

Upstream-derived changes included:
- cd97c2b4a85a0cbd3bf3a56681c2d2c92f4bda68: hold exception reference through build_backtrace OOM paths, applies with line-offset only.
- 817509a87c3c0af362df576574784bae0dae8129: return instead of repeating unicode_script failure cleanup loop, applies with line-offset only.
- 79f58c104c910db5bdf63111f7430a45e65b117a: free allocated BigInt on truncated bytecode. Source cleanup, not permission to load untrusted bytecode.
- 5febaaa49f4558b93bd637b1bea7473db5a2880e: preserve AutoInitProperty state until allocations succeed and release assignment value on error. Applies with line-offset only.
- e8b69194b3b60686ce32b1df65ded68bfd519bd4: cap captured-variable references at16bits and propagate compiler errors. Applies with line-offset only.
- b25da59246d520e0eb087070131299a2983ccb76: avoid length publication before array storage allocation; fence INT32_MAX appends. Four exact upstream hunks apply; the remaining `idx==INT32_MAX` fallback is mapped to June's equivalent final fast-array append block and existing generic_create_prop label, not the newer source's rearranged block.
- 813636e7af1b622c69223f2a63969ee2074deb9c: clear free_func/opaque after detachment or ownership transfer to avoid duplicate release. First exact upstream hunk applies; second is mapped to June's equivalent transfer block, whose following immutable-buffer condition does not exist in June.

Explicitly excluded:
- e36f8644e33529ddededc4398501ad3cd7c91b24: later pointer-based getset allocation is not in June's inline representation. Its failed contextual apply is not silently rewritten.
- Intl/Temporal and waitAsync audit/fix paths: feature source absent in June, not added.

Exact postpatch hashes:
quickjs.c ec3aba7e06a6f0a53274672856819acf3777c9c91e3f23642f1d0781454eee63
libunicode.c0477d38bd5b2ea0d5d51e190992a94c1470974cab0443c4703cbe76c8e975a68
Combined patch SHA256c30c6308ef87c667882a9c65cd9b3286224e6242a3ccadadf3c68f9161b22702

Host validation: patched qjs rebuilt; official language/closure/loop/bigint/builtin scripts exit0; local bounded512K OOM/error-allocation fixture exits0; ArrayBuffer.transfer content+detached-state fixture exits0; exact bundled EJS core/lib load still passes. No sanitizer claim, exploit-proof claim, complete test262 claim, Android binary, API24/16KB execution or actual YouTube challenge success.

Every referenced commit was read through the public GitHub commit API. To recover full originals use https://api.github.com/repos/bellard/quickjs/commits/<full SHA> . Raw JSON originals included in the packet. Reviewer must inspect manual two-context mappings and determine whether additional maintenance/security changes are needed. Current upstream compare was3d5e064e9dd67c70f7962836505a7fa067bf0a4e...e585640ffa2cf88ce7c2d587583c6fe15928ec2f (125commits); this is not a claim that all125commits were independently audited.
