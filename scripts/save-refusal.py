#!/usr/bin/env python3
"""Verify damaged/future/invalid saves are preserved and never reset silently."""
import gzip,hashlib,pathlib,shutil,struct,subprocess
root=pathlib.Path(__file__).resolve().parents[1]
source=root/'run-core-final'
if not source.exists():raise SystemExit('Run final core acceptance first')
for mode in ['truncated','future','out-of-range']:
    target=root/('run-refuse-'+mode)
    if target.exists():raise SystemExit('Refusing to overwrite existing fixture '+str(target))
    shutil.copytree(source,target)
    path=target/'world/data/sporebound_corruption.dat'
    if mode=='truncated':path.write_bytes(b'\x1f\x8b\x08')
    else:
        raw=gzip.decompress(path.read_bytes())
        if mode=='future':
            old=b'\x03\x00\x06schema'+struct.pack('>i',1)
            assert old in raw;raw=raw.replace(old,b'\x03\x00\x06schema'+struct.pack('>i',99),1)
        else:
            field=b'\x06\x00\x05index';offset=raw.index(field)+len(field)
            raw=raw[:offset]+struct.pack('>d',11)+raw[offset+8:]
        path.write_bytes(gzip.compress(raw))
    original=hashlib.sha256(path.read_bytes()).hexdigest()
    log=root/(target.name+'.log')
    with log.open('w') as out:
        subprocess.run(['bash','gradlew','--no-daemon','runServer','-PserverDir='+target.name,'-Pvalidation=refuse'],cwd=root,stdout=out,stderr=subprocess.STDOUT,timeout=240)
    text=log.read_text(errors='replace')
    assert 'Refusing to reset existing Sporebound corruption save:' in text,mode
    assert 'SPOREBOUND ACCEPTANCE PASS' not in text,mode
    assert hashlib.sha256(path.read_bytes()).hexdigest()==original,mode
    print('SPOREBOUND SAVE REFUSAL PASS: '+mode,flush=True)
