#!/usr/bin/env python3
"""Check the public producer source identity before local Maven qualification.
SEALI_BASELINE_* pin the Seali commit X7 extracted the producer sources from (recorded in the producer's source-baseline-bindings.json); PRODUCER_* pin the producer candidate itself."""
import argparse,hashlib,json,pathlib,subprocess
PRODUCER='03deec547243ebe120a537e38d667a3a1ffc588e'
PRODUCER_TREE='6a159ed514b638edabca3a00d17c619b51242069'
SEALI_BASELINE_COMMIT='6e6c6e1a8f62f0d45d26cef386d726b145abf209'
SEALI_BASELINE_TREE='d989b3482317cab099aefd015f367a53b8e118c4'
def check(root,producer):
 root=pathlib.Path(root).resolve(strict=True);producer=pathlib.Path(producer).resolve(strict=True)
 if subprocess.check_output(['git','-C',str(producer),'rev-parse','HEAD'],text=True).strip()!=PRODUCER:raise ValueError('producer commit differs')
 if subprocess.check_output(['git','-C',str(producer),'rev-parse','HEAD^{tree}'],text=True).strip()!=PRODUCER_TREE:raise ValueError('producer tree differs')
 if subprocess.check_output(['git','-C',str(producer),'status','--porcelain','--untracked-files=no'],text=True).strip():raise ValueError('producer source is dirty')
 manifest=producer/'source-baseline-bindings.json';lock=json.loads(manifest.read_text())
 if lock['baselineCommit']!=SEALI_BASELINE_COMMIT or lock['baselineTree']!=SEALI_BASELINE_TREE:raise ValueError('Seali source binding differs')
 checked=[]
 for row in lock['files']:
  for owner,name in [(producer,row['file']),(root,row['sourcePath'])]:
   rel=pathlib.PurePosixPath(name)
   if rel.is_absolute() or '..' in rel.parts:raise ValueError('unsafe source binding path')
   p=owner/name
   if p.resolve()!=p.absolute() or not p.is_file() or hashlib.sha256(p.read_bytes()).hexdigest()!=row['sha256']:raise ValueError('source binding differs: '+name)
  checked.append(row['file'])
 if len(checked)<25 or len(set(checked))!=len(checked):raise ValueError('incomplete/duplicate source binding')
 return {'producer_commit':PRODUCER,'producer_tree':PRODUCER_TREE,'baseline_commit':SEALI_BASELINE_COMMIT,'baseline_tree':SEALI_BASELINE_TREE,'source_files_checked':len(checked),'scope':'public producer source identity, not artifact digest, runtime or publication approval'}
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--root',required=True);p.add_argument('--producer',required=True);a=p.parse_args();print(json.dumps(check(a.root,a.producer),indent=2))
