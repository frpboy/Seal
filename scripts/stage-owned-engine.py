#!/usr/bin/env python3
"""Copy pinned engine into app asset/source-built library resource, no compiled AAR import."""
import argparse,pathlib,hashlib,shutil
p=argparse.ArgumentParser();p.add_argument('--input',required=True);p.add_argument('--root',required=True);a=p.parse_args()
f=pathlib.Path(a.input);assert f.is_file() and not f.is_symlink();assert hashlib.sha256(f.read_bytes()).hexdigest()=='e51e69e89489f199530f13d32a4342da3ddb2cf34842f364603a066db05afbf3'
r=pathlib.Path(a.root)
for dest in [r/'app/src/main/assets/functional-engine/yt-dlp',r/'dependencies/frpboy-library/src/main/res/raw/ytdlp']:
 dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(f,dest)
