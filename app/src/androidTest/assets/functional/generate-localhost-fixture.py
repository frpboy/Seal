"""Generate an ephemeral localhost-only TLS fixture using the bundled Cryptodome.
No persistent key, remote host or production trust store is used.
"""
import base64, datetime, os, pathlib, secrets
from Cryptodome.PublicKey import RSA
from Cryptodome.Hash import SHA256
from Cryptodome.Signature import pkcs1_15

def der(tag, value):
    size = len(value)
    length = bytes([size]) if size < 128 else bytes([0x80 + (size.bit_length() + 7) // 8]) + size.to_bytes((size.bit_length() + 7) // 8, 'big')
    return bytes([tag]) + length + value

def sequence(*parts): return der(0x30, b''.join(parts))
def integer(value):
    raw = value.to_bytes(max(1, (value.bit_length() + 7) // 8), 'big')
    return der(2, b'\0' + raw if raw[0] & 0x80 else raw)
def pem(label, raw):
    text = base64.b64encode(raw).decode('ascii')
    return ('-----BEGIN ' + label + '-----\n' + '\n'.join(text[i:i+64] for i in range(0, len(text), 64)) + '\n-----END ' + label + '-----\n').encode('ascii')

def generate(work):
    key = RSA.generate(2048)
    # sha256WithRSAEncryption and commonName OIDs, with a localhost-only SAN.
    algorithm = sequence(der(6, bytes.fromhex('2a864886f70d01010b')), der(5, b''))
    name = sequence(der(0x31, sequence(der(6, bytes.fromhex('550403')), der(12, b'Seali ephemeral localhost fixture ONLY'))))
    now = datetime.datetime.now(datetime.timezone.utc)
    validity = sequence(*(der(0x18, date.strftime('%Y%m%d%H%M%SZ').encode('ascii')) for date in (now - datetime.timedelta(days=1), now + datetime.timedelta(days=1))))
    san = sequence(der(6, bytes.fromhex('551d11')), der(4, sequence(der(0x82, b'localhost'))))
    tbs = sequence(der(0xa0, integer(2)), integer(secrets.randbits(128) or 1), algorithm, name, validity, name, key.public_key().export_key(format='DER'), der(0xa3, sequence(san)))
    certificate = sequence(tbs, algorithm, der(3, b'\0' + pkcs1_15.new(key).sign(SHA256.new(tbs))))
    for filename, payload in [('localhost-test-key.pem', key.export_key(format='PEM', pkcs=8)), ('localhost-test-cert.pem', pem('CERTIFICATE', certificate))]:
        path = pathlib.Path(work) / filename
        fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
        with os.fdopen(fd, 'wb') as output: output.write(payload)
