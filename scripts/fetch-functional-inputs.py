#!/usr/bin/env python3
"""Fetch only pinned public app inputs. No account, secret or user media input."""
import pathlib,hashlib,urllib.request,os,json
root=pathlib.Path(os.environ['CUSTODY_ROOT']);d=root/'app-inputs';d.mkdir();rows=[]
items=[('library-0.17.3.aar','https://repo.maven.apache.org/maven2/io/github/junkfood02/youtubedl-android/library/0.17.3/library-0.17.3.aar','9d29b148187242d6e754fb59f126b62dde5c1c7fcadaa29d14dbe13659495e4c'),('common-0.17.3.aar','https://repo.maven.apache.org/maven2/io/github/junkfood02/youtubedl-android/common/0.17.3/common-0.17.3.aar','92b33cdab9f5451a6258416db5b1034487641132bf3b40ff4ee9507e444ba198'),('ffmpeg-0.17.3.aar','https://repo.maven.apache.org/maven2/io/github/junkfood02/youtubedl-android/ffmpeg/0.17.3/ffmpeg-0.17.3.aar','e12402803f7a61802fded6bd021422688cb7a03e924068a1aec983dae88ea3c1'),('yt-dlp','https://github.com/yt-dlp/yt-dlp/releases/download/2026.10.26/yt-dlp','e51e69e89489f199530f13d32a4342da3ddb2cf34842f364603a066db05afbf3'),('yt-dlp.tar.gz','https://github.com/yt-dlp/yt-dlp/releases/download/2026.10.26/yt-dlp.tar.gz','21f942d258d25cc4e60b4982a25c423460f0bebb835f38e6dfd1fcb140087f95')]
for name,url,want in items:
 data=urllib.request.urlopen(url,timeout=60).read();assert hashlib.sha256(data).hexdigest()==want,name;p=d/name;p.write_bytes(data);rows.append({'file':name,'url':url,'sha256':want,'bytes':len(data)})
(root/'evidence/app-inputs.json').write_text(json.dumps(rows,indent=2))
with open(os.environ['GITHUB_ENV'],'a') as f:f.write('APP_INPUTS='+str(d)+'\n')
