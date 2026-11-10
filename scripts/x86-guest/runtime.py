#!/usr/bin/env python3
"""Private synthetic guest qualification. No user media, credentials or public artifacts."""
import os,pathlib,subprocess,time,json,signal,re,tempfile
root=pathlib.Path(os.environ['CUSTODY_ROOT']);out=root/'app-qualification/runtime';out.mkdir(parents=True)
sdk=pathlib.Path(os.environ['ANDROID_HOME']);adb=str(sdk/'platform-tools/adb');manager=str(sdk/'cmdline-tools/latest/bin/sdkmanager');avd=str(sdk/'cmdline-tools/latest/bin/avdmanager');image='system-images;android-34;google_apis;x86_64';emulator=None
deadline=time.monotonic()+4200  # Absolute seventy-minute work cap, then separately bounded cleanup.
receipt={'scope':'private x86_64 API34 synthetic guest only; not ARM/API24/16KB/real-site execution','runtime_pass':False,'required_screenshot_bytes_complete':False,'screenshot_png_decode_and_pixels_verified':False}
(out/'receipt.json').write_text(json.dumps(receipt,indent=2))
def cmd(args,timeout=60,filename=None,input=None,check=True):
 remaining=deadline-time.monotonic()
 if remaining<=0:raise subprocess.TimeoutExpired(args,0)
 with tempfile.TemporaryFile() as f:
  p=subprocess.run(args,input=input,stdout=f,stderr=subprocess.STDOUT,timeout=min(timeout,remaining))
  size=f.tell();f.seek(0);data=f.read(8*1024*1024)
 if size>8*1024*1024:raise RuntimeError('command output exceeds private 8MiB bound')
 if filename:(out/filename).write_bytes(data)
 if check and p.returncode:raise RuntimeError('step failed: '+str(args[0])+' exit '+str(p.returncode))
 return data
try:
 cmd([manager,'emulator',image],timeout=600,filename='sdk-install.txt')
 cmd([manager,'--list_installed'],filename='sdk-installed.txt')
 cmd([avd,'create','avd','--force','--name','seali-functional-private','--package',image,'--device','pixel_6'],input=b'no\n',filename='avd-create.txt')
 kvm=pathlib.Path('/dev/kvm');accel='on' if kvm.exists() and os.access(kvm,os.R_OK|os.W_OK) else 'off';receipt['acceleration']=accel
 # Existing access only. No sudo, chmod, host permissions changes or paid runner.
 log=(out/'emulator.txt').open('wb');emulator=subprocess.Popen([str(sdk/'emulator/emulator'),'-avd','seali-functional-private','-no-window','-no-audio','-no-boot-anim','-gpu','swiftshader_indirect','-no-snapshot','-accel',accel],stdout=log,stderr=subprocess.STDOUT,start_new_session=True)
 end=time.monotonic()+1500
 while time.monotonic()<end:
  if emulator.poll() is not None:raise RuntimeError('emulator exited')
  try:
   ready=cmd([adb,'shell','getprop','sys.boot_completed'],timeout=min(15,max(1,int(end-time.monotonic()))),check=False).strip()==b'1'
  except subprocess.TimeoutExpired:
   ready=False  # Only normal boot readiness polling may retry within the same deadline.
  if ready:break
  time.sleep(5)
 else:raise RuntimeError('boot timed out at 1500 seconds')
 props={n:cmd([adb,'shell','getprop',n],timeout=15).decode().strip() for n in ['ro.product.cpu.abi','ro.build.version.sdk','ro.build.fingerprint']};props['page_size']=cmd([adb,'shell','getconf','PAGE_SIZE'],timeout=15).decode().strip();receipt['guest']=props
 assert props['ro.product.cpu.abi']=='x86_64' and props['ro.build.version.sdk']=='34'
 assert props['page_size']=='4096','This lane qualifies only measured 4KB API34 guest'
 app=list(pathlib.Path('app/build/outputs/apk/generic/debug').glob('*.apk'));test=list(pathlib.Path('app/build/outputs/apk/androidTest/generic/debug').glob('*.apk'));assert len(app)==len(test)==1
 for f,label in [(app[0],'app'),(test[0],'test')]:
  text=cmd([adb,'install','-r',str(f)],timeout=180,filename='install-'+label+'.txt');assert b'Success' in text
 cmd([adb,'logcat','-c']);cmd([adb,'shell','monkey','-p','com.frpboy.seali.debug','-c','android.intent.category.LAUNCHER','1'],filename='launch.txt')
 end=min(deadline,time.monotonic()+120);pid=None
 while time.monotonic()<end:
  current=cmd([adb,'shell','pidof','com.frpboy.seali.debug'],timeout=min(15,max(1,end-time.monotonic())),check=False).decode().strip()
  if current and pid is None:pid=current
  assert not pid or current==pid,'Startup process died or restarted'
  status=cmd([adb,'exec-out','run-as','com.frpboy.seali.debug','cat','files/seali-init-status'],timeout=min(15,max(1,end-time.monotonic())),check=False).decode().strip()
  if status==f'OK pid={pid}':break
  assert not status.startswith('ERROR'),'App initialization failed'
  time.sleep(2)
 else:raise RuntimeError('startup status timed out')
 receipt['startup_pid']=pid
 cmd([adb,'exec-out','screencap','-p'],timeout=30,filename='startup.png')
 expected={
 'PythonComponentsFunctionalTest':{'importsHashesTlsAndBoundedXml'},
 'FFmpegFunctionalTest':{'localMergeMp3OpusCoverAndProbe'},
 'FunctionalAppDownloadTest':{'actualAppFetchDownloadsAndConvertsMp3','productionDownloadCancellationStopsChildAndReturnsFailure','unsupportedSectionsAndLiveFailBeforeNetwork'},
 'FunctionalEngineMigrationTest':{'oldEngineGetsVerifiedCurrentAndFirstBackupSurvives','updaterDirectoryRemovalCannotDeleteStateOrBackupAndFallbackIsCurrent','legitimateParentAliasIsAcceptedButChildSymlinkIsRejected','failedTemporaryOutputOpenLeavesOldEngineAndNoMigrationMarker'},
 'CreditsFunctionalVisualTest':{'noticeDialogRendersAndScrolls'},
 }
 # One cumulative twenty-minute budget, bounded by the absolute work deadline as well.
 tests_end=min(deadline,time.monotonic()+1200);receipt['classes']={}
 for cls,methods in expected.items():
  remaining=tests_end-time.monotonic();assert remaining>0
  text=cmd([adb,'shell','am','instrument','-w','-r','-e','class','com.frpboy.seali.'+cls,'com.frpboy.seali.debug.test/androidx.test.runner.AndroidJUnitRunner'],timeout=remaining,filename=cls+'.txt').decode(errors='replace')
  counts=re.findall(r'^OK \((\d+) tests?\)\s*$',text,re.M)
  reported=set(re.findall(r'^INSTRUMENTATION_STATUS: test=(\S+)\s*$',text,re.M))
  codes=[int(x) for x in re.findall(r'^INSTRUMENTATION_STATUS_CODE: (-?\d+)\s*$',text,re.M)]
  assert counts==[str(len(methods))] and reported==methods,(cls,counts,reported)
  assert codes.count(0)==len(methods) and all(c in (0,1) for c in codes),(cls,codes)
  assert not any(x in text for x in ['FAILURES!!!','INSTRUMENTATION_FAILED','shortMsg=','AssumptionViolated','org.junit.Ignore']),cls
  receipt['classes'][cls]={'passed':True,'expected_methods':sorted(methods),'count':len(methods)}
 receipt['runtime_pass']=True
except BaseException as error:
 receipt['runtime_pass']=False;receipt['error_type']=type(error).__name__
 raise
finally:
 # Persist the actual work result before evidence capture can fail or time out.
 (out/'receipt.json').write_text(json.dumps(receipt,indent=2))
 deadline=time.monotonic()+180
 try:
  cmd([adb,'logcat','-d','-t','3000'],timeout=20,filename='private-logcat.txt',check=False)
  listing=cmd([adb,'exec-out','run-as','com.frpboy.seali.debug','find','files','-type','f'],timeout=20,check=False).decode(errors='replace').splitlines()
  priority=['files/seali-credits-notices-top.png','files/seali-credits-notices-scrolled.png']
  component=sorted(n for n in listing if n.endswith('/python-components-result.txt'))
  chosen=priority+component+['files/seali-init-status']+sorted(n for n in listing if n.startswith(('files/seali-','files/functional-')) and n not in priority+component+['files/seali-init-status'])
  pulled=0;total=0;capture_rows=[]
  for i,n in enumerate(chosen):
   p=pathlib.PurePosixPath(n);assert not p.is_absolute() and '..' not in p.parts
   if p.suffix not in ['.txt','.png','.json'] and n!='files/seali-init-status':continue
   row={'source':n,'status':'omitted'};capture_rows.append(row)
   if pulled>=12 or time.monotonic()>=deadline:
    row['reason']='capture_count_or_deadline';continue
   if n not in listing:
    row['reason']='missing';continue
   size_text=cmd([adb,'exec-out','run-as','com.frpboy.seali.debug','stat','-c','%s',n],timeout=10,check=False).strip()
   if not size_text.isdigit():
    row['reason']='size_unverifiable';continue
   size=int(size_text);row['source_bytes']=size
   if size>2*1024*1024 or total+size>8*1024*1024:
    row['reason']='per_file_or_aggregate_cap';continue
   data=cmd([adb,'exec-out','run-as','com.frpboy.seali.debug','cat',n],timeout=15,check=False)
   if len(data)!=size:
    row['reason']='whole_byte_count_mismatch';continue
   dest=str(i)+'-'+p.name;(out/dest).write_bytes(data);total+=len(data);pulled+=1
   row.update({'status':'complete','saved':dest,'bytes':len(data)})
  receipt['capture']={'files':pulled,'bytes':total,'max_files':12,'max_each':2*1024*1024,'max_total':8*1024*1024,'deadline_seconds':180,'rows':capture_rows}
  receipt['required_screenshot_bytes_complete']=all(any(r['source']==n and r['status']=='complete' for r in capture_rows) for n in priority)
  receipt['screenshot_png_decode_and_pixels_verified']=False
 except Exception as error:receipt['evidence_capture_error']=type(error).__name__
 finally:
  if emulator is not None:
   try:
    if emulator.poll() is None:os.killpg(emulator.pid,signal.SIGTERM)
    try:emulator.wait(timeout=30)
    except subprocess.TimeoutExpired:
     os.killpg(emulator.pid,signal.SIGKILL);emulator.wait(timeout=10)
   except Exception as error:receipt['shutdown_error']=type(error).__name__
  try:
   log.close() if 'log' in globals() else None
   emulator_log=out/'emulator.txt'
   if emulator_log.exists() and emulator_log.stat().st_size>1024*1024:
    with emulator_log.open('rb') as f:f.seek(-1024*1024,2);tail=f.read()
    emulator_log.write_bytes(tail);receipt['emulator_log_tail_only']=True
  except Exception as error:receipt['log_cap_error']=type(error).__name__
  (out/'receipt.json').write_text(json.dumps(receipt,indent=2))
