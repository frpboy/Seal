#!/usr/bin/env python3
"""Review-only SDK setup/guest-boot diagnostics. No native/App build, APK or user data."""
import os,pathlib,json,time,subprocess,hashlib,signal,tempfile,xml.etree.ElementTree as ET,stat,socket
sdk=pathlib.Path(os.environ['ANDROID_HOME']);root=pathlib.Path(os.environ['RUNNER_TEMP'])/'seali-sdk-diagnostics';assert not root.exists();root.mkdir(mode=0o700);out=root/'evidence';out.mkdir()
manager=sdk/'cmdline-tools/16.0/bin/sdkmanager';avd=sdk/'cmdline-tools/16.0/bin/avdmanager';adb=sdk/'platform-tools/adb';image='system-images;android-34;google_apis;x86_64';end=time.monotonic()+2700;emulator=None
receipt={'sdk_install_properties_pass':False,'host_loader_pass':False,'sdk_setup_pass':False,'guest_boot_pass':False,'app_runtime_tested':False,'source_sha':os.environ.get('GITHUB_SHA'),'scope':'SDK/API34 x86_64 4KB synthetic boot only; no App or native build'}
os.environ['ANDROID_AVD_HOME']=str(root/'avd');os.environ['ANDROID_USER_HOME']=str(root/'android-user');os.environ['ANDROID_EMULATOR_HOME']=str(root/'emulator-home');os.environ['ANDROID_SERIAL']='emulator-5558'
for n in ['avd','android-user','emulator-home']:(root/n).mkdir()
def save(): (out/'receipt.json').write_text(json.dumps(receipt,indent=2))
def properties(path):
 result={}
 for line in path.read_text().splitlines():
  line=line.strip()
  if not line or line.startswith('#') or '=' not in line:continue
  key,value=line.split('=',1);key=key.strip();assert key not in result,'duplicate property '+key;result[key]=value.strip()
 return result
def cmd(args,timeout=60,name=None,input=None,check=True):
 remain=end-time.monotonic();assert remain>0,'phase deadline reached'
 limit=min(timeout,remain);started=time.monotonic();metadata={'timeout_seconds':limit,'timed_out':False,'exit_code':None};data=b''
 with tempfile.TemporaryFile() as f:
  try:
   r=subprocess.run([str(x) for x in args],stdout=f,stderr=subprocess.STDOUT,input=input,timeout=limit)
   metadata['exit_code']=r.returncode
  except subprocess.TimeoutExpired:
   metadata['timed_out']=True
   raise
  finally:
   metadata['elapsed_seconds']=round(time.monotonic()-started,3);f.seek(0,2);size=f.tell();f.seek(max(0,size-1024*1024));data=f.read(1024*1024);metadata['log_bytes']=size;metadata['tail_bytes']=len(data)
   if name:
    (out/name).write_bytes(data);(out/(name+'.command.json')).write_text(json.dumps(metadata,indent=2))
 if check and r.returncode:raise RuntimeError(str(args[0])+' exit '+str(r.returncode))
 return data
save()
try:
 props=sdk/'cmdline-tools/16.0/source.properties';text=props.read_text();assert properties(props)['Pkg.Revision']=='16.0'
 (out/'selected-tools-source.properties').write_text(text)
 latest=sdk/'cmdline-tools/latest/source.properties'
 if latest.exists():(out/'preinstalled-latest-source.properties').write_bytes(latest.read_bytes())
 receipt['tools']={'sdkmanager':str(manager.resolve()),'avdmanager':str(avd.resolve()),'source_properties_sha256':hashlib.sha256(props.read_bytes()).hexdigest()}
 assert cmd([manager,'--version'],name='sdkmanager-version.txt').decode().strip()=='16.0';cmd([avd,'list','target'],name='avdmanager-targets-before.txt')
 # Inspect authoritative package metadata through bounded curl. No version/image substitution.
 import urllib.request
 def metadata(url,file):
  with urllib.request.urlopen(url,timeout=30) as response:data=response.read(4*1024*1024)
  (out/file).write_bytes(data);return ET.fromstring(data)
 tree=metadata('https://dl.google.com/android/repository/sys-img/google_apis/sys-img2-3.xml','system-image-metadata.xml')
 image_package=next(p for p in tree.iter('remotePackage') if p.attrib.get('path')==image)
 assert image_package.findtext('revision/major')=='14'
 complete=image_package.find('archives/archive/complete')
 assert complete.findtext('size')=='1563721130' and complete.findtext('checksum')=='e0f6c9a0691aa27bd597d0deb1bcfdc943ac8ca7' and complete.findtext('url')=='x86_64-34_r14.zip'
 tree=metadata('https://dl.google.com/android/repository/repository2-3.xml','repository-metadata.xml')
 package=next(p for p in tree.iter('remotePackage') if p.attrib.get('path')=='emulator' and p.find('channelRef').attrib['ref']=='channel-0')
 assert [package.findtext('revision/'+x) for x in ['major','minor','micro']]==['37','2','13']
 archive=next(x for x in package.findall('archives/archive') if x.findtext('host-os')=='linux' and x.findtext('host-arch')=='x64');complete=archive.find('complete')
 assert complete.findtext('size')=='349658551' and complete.findtext('checksum')=='f41f9e31eb5ca64926603c03265c50bbc0ff4172' and complete.findtext('url')=='emulator-linux_x64-16740729.zip'
 receipt['metadata_exact_pins_pass']=True;save()
 # Runner-local privileged package mutation only: no source, account, billing or host-image change.
 assert properties(pathlib.Path('/etc/os-release'))['VERSION_ID']=='"24.04"'
 assert cmd(['dpkg','--print-architecture'],name='host-architecture.txt').decode().strip()=='amd64'
 cmd(['apt-cache','policy','libpulse0:amd64'],name='libpulse-policy-before.txt')
 cmd(['sudo','-n','apt-get','-o','Acquire::Retries=0','-o','Acquire::http::Timeout=30','-o','Acquire::https::Timeout=30','update'],timeout=120,name='apt-update.txt')
 cmd(['apt-cache','policy','libpulse0:amd64'],name='libpulse-policy-after.txt')
 install=['apt-get','-o','Acquire::Retries=0','-o','Acquire::http::Timeout=30','-o','Acquire::https::Timeout=30','--yes','--no-install-recommends','--no-upgrade','--no-remove','install','libpulse0:amd64=1:16.1+dfsg1-2ubuntu10.1']
 plan=cmd(['env','LC_ALL=C',*install[:1],'--simulate',*install[1:]],timeout=30,name='libpulse-install-plan.txt').decode()
 assert not any(line.startswith('Remv ') or (line.startswith('Inst ') and '[' in line.split(' (',1)[0]) for line in plan.splitlines()),'package upgrade/removal plan forbidden'
 cmd(['sudo','-n','env','LC_ALL=C','DEBIAN_FRONTEND=noninteractive',*install],timeout=180,name='libpulse-install.txt')
 installed=cmd(['dpkg-query','-W','-f=${Package} ${Architecture} ${Version} ${db:Status-Status}\n','libpulse0:amd64'],name='libpulse-installed.txt').decode().strip()
 assert installed=='libpulse0 amd64 1:16.1+dfsg1-2ubuntu10.1 installed'
 cmd(['dpkg-query','-L','libpulse0:amd64'],name='libpulse-files.txt')

 # Explicit chosen v16 for both tools, verbose diagnostic install. No claim of raw ZIP integrity
 # when sdkmanager does not retain its download. Capture any surviving temp archive below.
 cmd([manager,'--verbose','--channel=0','emulator',image],timeout=600,name='sdkmanager-install-verbose.txt')
 for rel,version in [('emulator','37.2.13'),('system-images/android-34/google_apis/x86_64','14')]:
  p=sdk/rel/'source.properties';text=p.read_text();assert properties(p)['Pkg.Revision']==version;(out/(rel.replace('/','_')+'-source.properties')).write_text(text)
 img=sdk/'system-images/android-34/google_apis/x86_64';assert (img/'system.img').is_file() and (img/'system.img').stat().st_size>0
 assert properties(img/'source.properties')['AndroidVersion.ApiLevel']=='34' and properties(img/'source.properties')['SystemImage.Abi']=='x86_64'
 cmd([manager,'--list_installed'],name='sdk-installed.txt')
 receipt['sdk_install_properties_pass']=True;save()
 qemu=sdk/'emulator/qemu/linux-x86_64/qemu-system-x86_64'
 loader=cmd(['env','LD_LIBRARY_PATH='+str(sdk/'emulator/lib64'),'ldd',qemu],name='emulator-loader.txt').decode()
 assert 'not found' not in loader
 cmd([sdk/'emulator/emulator','-version'],name='emulator-version.txt')
 receipt['host_loader_pass']=True
 receipt['sdk_setup_pass']=True;save()
 assert b'emulator-5558' not in cmd([adb,'devices'],timeout=15,name='devices-before.txt')
 for port in [5558,5559]:
  with socket.socket() as probe:probe.bind(('127.0.0.1',port))
 cmd([avd,'create','avd','--force','--name','seali-sdk-diagnostic','--package',image,'--device','pixel_6'],input=b'no\n',name='avd-create.txt')
 kvm=pathlib.Path('/dev/kvm');accel='on' if kvm.exists() and stat.S_ISCHR(kvm.stat().st_mode) and os.access(kvm,os.R_OK|os.W_OK) else 'off';receipt['acceleration']=accel
 log=(out/'emulator.txt').open('wb');emulator=subprocess.Popen([str(sdk/'emulator/emulator'),'-avd','seali-sdk-diagnostic','-no-window','-no-audio','-no-boot-anim','-gpu','swiftshader_indirect','-no-snapshot','-accel',accel,'-port','5558'],stdout=log,stderr=subprocess.STDOUT,start_new_session=True)
 boot_end=min(end,time.monotonic()+1500)
 while time.monotonic()<boot_end:
  if emulator.poll() is not None:raise RuntimeError('emulator exited before boot')
  try:ready=cmd([adb,'shell','getprop','sys.boot_completed'],timeout=min(15,max(1,boot_end-time.monotonic())),check=False).strip()==b'1'
  except subprocess.TimeoutExpired:ready=False
  if ready:break
  time.sleep(5)
 else:raise RuntimeError('boot deadline exceeded')
 guest={k:cmd([adb,'shell','getprop',k],timeout=15).decode().strip() for k in ['ro.product.cpu.abi','ro.build.version.sdk','ro.build.fingerprint']};guest['page_size']=cmd([adb,'shell','getconf','PAGE_SIZE'],timeout=15).decode().strip();receipt['guest']=guest
 assert guest['ro.product.cpu.abi']=='x86_64' and guest['ro.build.version.sdk']=='34' and guest['page_size']=='4096'
 receipt['guest_boot_pass']=True
except BaseException as error:
 receipt['error_type']=type(error).__name__;raise
finally:
 save();end=time.monotonic()+180
 try:
  # Metadata only of existing candidate temp files, never env/credential dumps or image upload.
  rows=[];roots=[sdk/'.temp',sdk/'temp',pathlib.Path.home()/'.android/cache']
  for base in roots:
   if not base.exists():continue
   for p in base.rglob('*'):
    if len(rows)>=12 or time.monotonic()>=end:break
    if p.is_file() and not p.is_symlink():
     row={'path':str(p.relative_to(base)),'root':str(base),'bytes':p.stat().st_size}
     with p.open('rb') as f:row['first16_hex']=f.read(16).hex()
     if p.stat().st_size<=2*1024*1024:row['sha256']=hashlib.sha256(p.read_bytes()).hexdigest()
     else:row['whole_digest_not_taken']='bounded diagnostic avoids large archive hash after failed setup'
     rows.append(row)
  (out/'surviving-temp-files.json').write_text(json.dumps(rows,indent=2));receipt['raw_failed_archive_retained_in_evidence']=False
  if emulator is not None:cmd([adb,'logcat','-d','-t','500'],timeout=20,name='private-boot-logcat.txt',check=False)
 except Exception as error:receipt['capture_error']=type(error).__name__
 finally:
  if emulator is not None:
   try:
    if emulator.poll() is None:os.killpg(emulator.pid,signal.SIGTERM)
    try:emulator.wait(timeout=30)
    except subprocess.TimeoutExpired:os.killpg(emulator.pid,signal.SIGKILL);emulator.wait(timeout=10)
   except Exception as error:receipt['shutdown_error']=type(error).__name__
  if 'log' in globals():log.close()
  p=out/'emulator.txt'
  if p.exists() and p.stat().st_size>1024*1024:
   with p.open('rb') as f:f.seek(-1024*1024,2);tail=f.read()
   p.write_bytes(tail)
  save()
