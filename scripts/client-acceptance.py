#!/usr/bin/env python3
import argparse,gzip,hashlib,json,os,pathlib,shutil,signal,struct,subprocess,time,urllib.request
root=pathlib.Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser()
parser.add_argument('--source',default='run-core-final/world',help='Completed acceptance world to copy; never modified')
deps=parser.add_mutually_exclusive_group()
deps.add_argument('--compat',action='store_true')
deps.add_argument('--civilis',action='store_true',help='Download the exact locked Civillis dependency for HUD validation')
args=parser.parse_args()
run=root/'run-client'
if run.exists():raise SystemExit('Archive or rename the previous disposable client directory first; refusing to overwrite.')
(run/'saves').mkdir(parents=True)
shutil.copytree(root/args.source,run/'saves/sporebound-preview')
# Start the copied preview at index 6 even if the server acceptance fixture lowered it.
state=run/'saves/sporebound-preview/dimensions/sporebound/blighted_world/data/sporebound_corruption.dat'
raw=gzip.decompress(state.read_bytes());field=b'\x06\x00\x05index';offset=raw.index(field)+len(field)
state.write_bytes(gzip.compress(raw[:offset]+struct.pack('>d',6)+raw[offset+8:]))
if args.compat:
    (run/'mods').mkdir()
    jars=list((root/'compatibility/mods').glob('*.jar'))
    if len(jars)!=2:raise SystemExit('The two exact compatibility JARs are required.')
    for jar in jars:shutil.copy2(jar,run/'mods'/jar.name)
if args.civilis:
    lock=json.loads((root/'compatibility/civilis.lock.json').read_text())
    with urllib.request.urlopen(lock['url'],timeout=60) as response: jar=response.read()
    if hashlib.sha256(jar).hexdigest()!=lock['sha256']:raise SystemExit('Civillis dependency checksum mismatch')
    (run/'mods').mkdir()
    (run/'mods'/lock['filename']).write_bytes(jar)
(run/'options.txt').write_text('tutorialStep:none\npauseOnLostFocus:false\nguiScale:2\nrenderDistance:6\nsimulationDistance:5\nmaxFps:30\nautoJump:false\n')
env=dict(os.environ,LIBGL_ALWAYS_SOFTWARE='1')
log=root/'client-validation.log'
timed_out=False
with log.open('w') as out:
    process=subprocess.Popen(['xvfb-run','-a','-s','-screen 0 1280x720x24','bash','gradlew','--no-daemon','runClient','-PclientValidation'],cwd=root,env=env,stdout=out,stderr=subprocess.STDOUT,start_new_session=True)
    try:
        deadline=time.monotonic()+600
        shutdown_deadline=None
        while process.poll() is None:
            if (run/'client-validation.json').is_file() and shutdown_deadline is None:
                shutdown_deadline=time.monotonic()+60
            remaining=min(deadline,shutdown_deadline or deadline)-time.monotonic()
            if remaining<=0:raise subprocess.TimeoutExpired(process.args,600 if shutdown_deadline is None else 60)
            try:process.wait(timeout=min(1,remaining))
            except subprocess.TimeoutExpired:pass
    except subprocess.TimeoutExpired:
        timed_out=True
        # Preserve thread stacks before stopping a stalled game or Gradle process.
        try:
            listing=subprocess.run(['jcmd','-l'],capture_output=True,text=True,timeout=10)
            for line in listing.stdout.splitlines():
                if 'jdk.jcmd' in line:continue
                pid=line.split(maxsplit=1)[0]
                if pid.isdigit():
                    dump=subprocess.run(['jcmd',pid,'Thread.print'],capture_output=True,text=True,timeout=10)
                    out.write(dump.stdout+dump.stderr)
                    for block in dump.stdout.split('\n\n'):
                        if any(name in block for name in ['\"Server thread\"','\"Render thread\"','deadlock']):print(block,flush=True)
        except (OSError,subprocess.TimeoutExpired):pass
        os.killpg(process.pid,signal.SIGTERM)
        try:process.wait(timeout=10)
        except subprocess.TimeoutExpired:
            os.killpg(process.pid,signal.SIGKILL)
            process.wait()
text=log.read_text(errors='replace')
if timed_out:
    lines=text.splitlines()
    for index,line in enumerate(lines):
        if 'SPOREBOUND SHUTDOWN CHUNK REQUEST' in line:print('\n'.join(lines[index:index+35]))
if timed_out:print('Client acceptance exceeded its startup/test or 60-second shutdown deadline; process group stopped.')
assert not timed_out and process.returncode==0 and 'SPOREBOUND CLIENT ACCEPTANCE PASS' in text, '\n'.join(line for line in text.splitlines() if any(word in line for word in ['AssertionError', 'CHECK PASS', 'Caused by:', 'Exception']))+'\n'+text[-10000:]
for name in ['01-dormant','02-blighted-world','03-overrun','04-return','05-remnant-grove','06-ribbed-highlands','07-hud-off','08-fungal-remnants','09-biomass-absorption','10-biomass-integrated','11-index-art-stages','12-hivebound-armor','13-hivebound-evolution','14-hive-sense','15-hive-infusion']:
    assert (run/'screenshots'/(name+'.png')).is_file(),name
assert 'arrival is above bedrock and collision free' in text
assert 'arrival has a solid landing surface' in text
print('SPOREBOUND REAL CLIENT ACCEPTANCE PASS')
