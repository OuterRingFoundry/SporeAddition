#!/usr/bin/env python3
"""Disposable upgrade and damaged-census tests; never edits the source world."""
import argparse,gzip,hashlib,pathlib,shutil,struct,subprocess
root=pathlib.Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--prefix',required=True);args=p.parse_args()
for case in ['upgrade','truncated','missing-list']:
    run=root/(args.prefix+'-'+case)
    if run.exists():raise SystemExit('Refusing to overwrite existing fixture '+str(run))
    run.mkdir();shutil.copytree(root/args.source,run/'world')
    (run/'eula.txt').write_text('eula=true\n')
    (run/'server.properties').write_text('server-ip=127.0.0.1\nserver-port=25585\nonline-mode=false\nview-distance=4\nsimulation-distance=4\nmax-tick-time=180000\n')
    state=run/'world/dimensions/sporebound/blighted_world/data/sporebound_corruption.dat'
    raw=gzip.decompress(state.read_bytes());field=b'\x06\x00\x05index';offset=raw.index(field)+len(field)
    state.write_bytes(gzip.compress(raw[:offset]+struct.pack('>d',4.25)+raw[offset+8:]))
    census=run/'world/DIM-1/data/sporebound_hives.dat'
    if case=='upgrade':census.unlink()
    elif case=='truncated':census.write_bytes(census.read_bytes()[:9])
    else:
        raw=gzip.decompress(census.read_bytes()).replace(b'\x09\x00\x06active',b'\x09\x00\x06broken',1)
        census.write_bytes(gzip.compress(raw))
    before=hashlib.sha256(census.read_bytes()).hexdigest() if census.exists() else None
    log=root/(run.name+'.log')
    with log.open('w') as out:
        result=subprocess.run(['bash','gradlew','--no-daemon','runServer','-PserverDir='+run.name,'-Pvalidation=core-read'],cwd=root,stdout=out,stderr=subprocess.STDOUT,timeout=600)
    text=log.read_text(errors='replace')
    if case=='upgrade':
        assert result.returncode==0 and 'SPOREBOUND ACCEPTANCE PASS: core-read' in text,text[-10000:]
        assert 'unloaded Hive Minds count survives process restart' in text
        assert census.exists()
    else:
        assert 'Refusing to reset existing Hive Mind census' in text,text[-10000:]
        assert 'SPOREBOUND ACCEPTANCE PASS' not in text
        assert hashlib.sha256(census.read_bytes()).hexdigest()==before
    print('SPOREBOUND HIVE CENSUS CHECK PASS: '+case,flush=True)
