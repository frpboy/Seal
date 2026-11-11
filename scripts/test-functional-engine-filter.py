#!/usr/bin/env python3
"""Evaluate only the three reviewed filter functions extracted from pinned engine source."""
import ast,hashlib,json,operator,pathlib,re,tarfile,argparse
p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--out',required=True);a=p.parse_args();source=pathlib.Path(a.source)
assert hashlib.sha256(source.read_bytes()).hexdigest()=='21f942d258d25cc4e60b4982a25c423460f0bebb835f38e6dfd1fcb140087f95'
with tarfile.open(source) as t:code=t.extractfile('yt-dlp/yt_dlp/utils/_utils.py').read().decode()
tree=ast.parse(code);nodes=[n for n in tree.body if isinstance(n,ast.FunctionDef) and n.name in ['_match_one','match_str']];assert len(nodes)==2
ns={'re':re,'operator':operator};exec(compile(ast.Module(body=nodes,type_ignores=[]),'pinned-reviewed-filter-functions','exec'),ns)
filter='!is_live & !is_upcoming & live_status!=?is_live & live_status!=?is_upcoming & live_status!=?post_live'
cases=[('missing generic fields',{},True),('normal',{'is_live':False,'is_upcoming':False,'live_status':'not_live'},True),('null preflight live on re-extract',{'is_live':True,'live_status':'is_live'},False),('stale preflight live on re-extract',{'live_status':'is_live'},False),('upcoming',{'is_upcoming':True},False),('post live',{'live_status':'post_live'},False)]
rows=[]
for name,info,want in cases:
 result=bool(ns['match_str'](filter,info,incomplete=False));assert result==want,(name,result);rows.append({'case':name,'actual_engine_item':info,'accepted':result})
# A playlist container is not a complete video; independently apply to each extracted child.
playlist=[cases[1][1],cases[2][1]];assert [bool(ns['match_str'](filter,i,incomplete=False)) for i in playlist]==[True,False]
pathlib.Path(a.out).write_text(json.dumps({'pinned_source_sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'filter':filter,'cases':rows,'playlist_item_results':[True,False],'scope':'host-only actual reviewed filter functions, not Android or complete engine runtime'},indent=2));print('PASS pinned filter functions, six cases + playlist per-item')
