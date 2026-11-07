#!/usr/bin/env python3
"""Fixed synthetic evidence allowlist, including partial failure evidence."""
from pathlib import Path
import shutil
root=Path('safe-release-evidence');root.mkdir(exist_ok=False)
allow=['release-prep-identity.txt','native-alignment.json','ux-egress-fence.txt']
allow += ['onboarding-'+n+'.png' for n in ['step-1','step-2-recreated','step-3','cold-share',
    'warm-share','input-sheet-deferred','after-sheet-hidden','about-debug-update-off',
    'manual-update','settings-back','landscape-font15']]
for name in allow:
    source=Path(name)
    if source.exists():
        assert source.is_file() and not source.is_symlink()
        shutil.copyfile(source,root/name)
size=sum(p.stat().st_size for p in root.iterdir())
assert 0<size<=20*1024*1024,'Safe evidence empty or exceeds20MiB'
cipher=Path('private-release-upload')
cipher_size=sum(p.stat().st_size for p in cipher.iterdir()) if cipher.exists() else 0
assert size+cipher_size<=300*1024*1024,'Combined uploads exceed300MiB'
print('Only fixed synthetic UI/scalar evidence staged; combined ceiling passed')
