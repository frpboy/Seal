# Locked source-input preparation

This tool only prepares source inputs. It does not download, compile, sign, install, publish or execute Android code. It does not qualify the contents of Termux, an NDK, the SSL/Expat stage, a host Python interpreter or any runtime result.

Use a complete reviewed source bundle containing `python-baseline` and `python-refresh` as described by the lock. Source archives are not committed here. The bundle is still private preparation, not a completed source/relink release delivery.

```sh
python3 scripts/owned-source-prep/test-seali-prepare-owned-producer-inputs.py
python3 scripts/owned-source-prep/seali-prepare-owned-producer-inputs.py \
  --bundle /absolute/source-bundle --output /absolute/fresh-prepared-root
```

The output must not exist. Preparation uses a temporary sibling directory, removes it on failure and renames it on successful completion. Run without concurrent writers to the output parent or input tree. Output root is normalized to0700; copied input and recipe files are0600. Invoke recipe shell scripts with `bash`, not direct execution. Termux source files retain their archive permissions masked to0777; special files and file/link ancestors are rejected. Source generators may depend on executable source scripts, so these modes are intentional.

The embedded SHA256 fixes `seali-owned-producer-lock.json`. The lock identifies22 input files, exact original metadata and39 active recipe files. Every metadata row's hash and size must agree with its input lock. All original metadata fields (URLs, source/pin origins, tags, commits and other fields) are retained in the combined `inputs/source-inputs.json`; active recipes use `file` and `sha256`, with extra fields preserved for provenance.

Archive limits are deliberately20000 members,32MiB per member and128MiB total expanded payload, independent of compressed size. The actual reviewed Termux input has10649 members,17052111 declared payload bytes and23 symlinks. Valid internal relative links such as `../aarch64/...` are supported by whole-graph validation before extraction. Hardlinks must name an earlier regular archive member exactly, never a symlink or a path through one. No target code is run.

Actual local source-only evidence:32 synthetic tests passed, including optimized Python checks, metadata drift, output modes and field preservation. A separate full locked-source CLI preparation under `python -O` passed. The archive preflight is not an independent security review of its source contents or build/runtime qualification.

The complete native producer pipeline, full source/relink delivery, fresh qualified signed candidate and Android runtime/upgrade checks remain separate gates. Do not dispatch the legacy native workflow for these refreshed inputs. Unexpected rebuilt hashes need fresh review and coherent deliberate pin changes, not automatic trust.
