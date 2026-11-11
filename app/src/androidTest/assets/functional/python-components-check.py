"""On-device controlled component checks. No remote network or production credentials."""
import ssl,hashlib,hmac,pyexpat,xml.etree.ElementTree as ET,sys,threading,http.server,urllib.request,pathlib,json,time,socket,resource
work=pathlib.Path(sys.argv[1]);assert ssl.OPENSSL_VERSION.startswith('OpenSSL 3.5.9 '),ssl.OPENSSL_VERSION
assert pyexpat.EXPAT_VERSION=='expat_2.10.0',pyexpat.EXPAT_VERSION
assert hashlib.sha256(b'abc').hexdigest()=='ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad'
assert hmac.new(b'key',b'The quick brown fox jumps over the lazy dog',hashlib.sha256).hexdigest()=='f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8'
assert hashlib.scrypt(b'password',salt=b'salt',n=16,r=1,p=1,dklen=32).hex()=='45133c3dfba48c82235df51a5349924110eee893752f0d4168d2e2aee5722d82'
requests=[]
class Handler(http.server.BaseHTTPRequestHandler):
 def do_GET(self):
  requests.append(self.path);self.send_response(200);self.end_headers();self.wfile.write(b'component-https-ok')
 def log_message(self,*args):pass
server=http.server.ThreadingHTTPServer(('127.0.0.1',0),Handler)
ctx=ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER);ctx.load_cert_chain(str(work/'localhost-test-cert.pem'),str(work/'localhost-test-key.pem'))
server.socket=ctx.wrap_socket(server.socket,server_side=True);thread=threading.Thread(target=server.serve_forever,daemon=True);thread.start()
try:
 url='https://localhost:%d/ok'%server.server_port
 trusted=ssl.create_default_context(cafile=str(work/'localhost-test-cert.pem'))
 with urllib.request.urlopen(url,context=trusted,timeout=5) as response:assert response.read()==b'component-https-ok'
 rejected=False
 try:urllib.request.urlopen(url,context=ssl.create_default_context(),timeout=5)
 except urllib.error.URLError as e:rejected=isinstance(e.reason,ssl.SSLCertVerificationError)
 assert rejected,'Untrusted self-signed certificate must fail verification'
 # Wrong-hostname validation must also fail, even when this test CA is trusted.
 rejected=False
 try:urllib.request.urlopen('https://127.0.0.1:%d/host'%server.server_port,context=trusted,timeout=5)
 except urllib.error.URLError as e:rejected=isinstance(e.reason,ssl.SSLCertVerificationError)
 assert rejected,'Hostname mismatch must fail verification'
 assert requests==['/ok'],requests
finally:server.shutdown();server.server_close();thread.join(5)
root=ET.fromstring('<MPD xmlns="urn:mpeg:dash:schema:mpd:2011"><Period id="1"/></MPD>');assert root.tag=='{urn:mpeg:dash:schema:mpd:2011}MPD'
try:ET.fromstring('<root><broken></root>');raise AssertionError('Malformed XML accepted')
except ET.ParseError:pass
xml_peak_before=resource.getrusage(resource.RUSAGE_SELF).ru_maxrss
# GE-off is no expansion, not a promise that all DOCTYPE syntax is rejected.
for source in ['<!DOCTYPE r [<!ENTITY x "EXPANDED">]><r>&x;</r>','<!DOCTYPE r [<!ENTITY a "1234567890"><!ENTITY b "&a;&a;&a;&a;">]><r>&b;</r>']:
 start=time.monotonic()
 try:
  parsed=ET.fromstring(source);assert 'EXPANDED' not in ''.join(parsed.itertext()) and '1234567890' not in ''.join(parsed.itertext())
 except ET.ParseError:pass
 assert time.monotonic()-start<2,'Small entity fixture exceeded bounded wall time'
# Track a local external URL. Parameter/general DTD references must not fetch it.
requests.clear();http=http.server.ThreadingHTTPServer(('127.0.0.1',0),Handler);thread=threading.Thread(target=http.serve_forever,daemon=True);thread.start()
try:
 for source in ['<!DOCTYPE r SYSTEM "http://127.0.0.1:%d/external"><r/>'%http.server_port,'<!DOCTYPE r [<!ENTITY x SYSTEM "http://127.0.0.1:%d/entity">]><r>&x;</r>'%http.server_port]:
  try:ET.fromstring(source)
  except ET.ParseError:pass
 assert not requests,requests
finally:http.shutdown();http.server_close();thread.join(5)
xml_peak_after=resource.getrusage(resource.RUSAGE_SELF).ru_maxrss
assert xml_peak_after-xml_peak_before < 64*1024,'Small XML fixture peak RSS grew by 64 MiB or more'
print(json.dumps({'xml_fixture_peak_rss_growth_kib':xml_peak_after-xml_peak_before,'openssl':ssl.OPENSSL_VERSION,'expat':pyexpat.EXPAT_VERSION,'hashlib_hmac_scrypt':'PASS','controlled_valid_https':'PASS','bad_certificate_and_hostname_rejected':'PASS','namespace_xml':'PASS','malformed_xml_rejected':'PASS','small_entity_no_expansion':'PASS','external_dtd_entity_no_fetch':'PASS','scope':'Controlled fixtures only, not a blanket security claim'}))
