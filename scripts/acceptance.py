#!/usr/bin/env python3
"""Run isolated native fixtures; require positive markers, not merely Gradle exit 0."""
import argparse, pathlib, shutil, subprocess, sys
root=pathlib.Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser();p.add_argument('--profile',choices=['core','compat'],default='core');p.add_argument('--directory',required=True);args=p.parse_args()
run=root/args.directory
if run.exists():raise SystemExit('Use a fresh fixture directory; existing worlds are never deleted.')
run.mkdir(parents=True)
(run/'eula.txt').write_text('eula=true\n')
(run/'server.properties').write_text('server-ip=127.0.0.1\nserver-port=25585\nonline-mode=false\nlevel-seed=918271\nview-distance=4\nsimulation-distance=4\nmax-tick-time=180000\n')
if args.profile=='compat':
    mods=run/'mods';mods.mkdir()
    for jar in (root/'compatibility/mods').glob('*.jar'):shutil.copy2(jar,mods/jar.name)
    if len(list(mods.glob('*.jar')))!=2:raise SystemExit('Two exact compatibility jars are required.')
for phase in ['write','read']:
    mode=args.profile+'-'+phase;log=root/(args.directory+'-'+phase+'.log')
    with log.open('w') as output:
        result=subprocess.run(['bash','gradlew','--no-daemon','runServer','-PserverDir='+args.directory,'-Pvalidation='+mode],cwd=root,stdout=output,stderr=subprocess.STDOUT,timeout=600)
    text=log.read_text(errors='replace')
    if result.returncode or 'SPOREBOUND ACCEPTANCE PASS: '+mode not in text or 'SPOREBOUND ACCEPTANCE FAILED' in text:
        print(text[-14000:]);raise SystemExit('Acceptance failed: '+mode)
    print(next(line for line in text.splitlines() if 'SPOREBOUND ACCEPTANCE PASS' in line),flush=True)

    if phase=='write':subprocess.run([sys.executable,str(root/'scripts/entity-fixture-audit.py'),str(run/'world')],check=True)
