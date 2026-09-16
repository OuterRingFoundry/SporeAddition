#!/usr/bin/env python3
import argparse,gzip,json,os,pathlib,shutil,struct,subprocess
root=pathlib.Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser()
parser.add_argument('--source',default='run-core-final/world',help='Completed acceptance world to copy; never modified')
parser.add_argument('--compat',action='store_true')
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
(run/'options.txt').write_text('tutorialStep:none\npauseOnLostFocus:false\nguiScale:2\nrenderDistance:6\nsimulationDistance:4\nmaxFps:30\nautoJump:false\n')
env=dict(os.environ,LIBGL_ALWAYS_SOFTWARE='1')
log=root/'client-validation.log'
with log.open('w') as out:
    result=subprocess.run(['xvfb-run','-a','-s','-screen 0 1280x720x24','bash','gradlew','--no-daemon','runClient','-PclientValidation'],cwd=root,env=env,stdout=out,stderr=subprocess.STDOUT,timeout=600)
text=log.read_text(errors='replace')
assert result.returncode==0 and 'SPOREBOUND CLIENT ACCEPTANCE PASS' in text, '\n'.join(line for line in text.splitlines() if any(word in line for word in ['AssertionError', 'CHECK PASS', 'Caused by:', 'Exception']))+'\n'+text[-10000:]
for name in ['01-dormant','02-blighted-world','03-overrun','04-return','05-remnant-grove','06-ribbed-highlands','07-hud-off']:
    assert (run/'screenshots'/(name+'.png')).is_file(),name
assert 'arrival is above bedrock and collision free' in text
assert 'arrival has a solid landing surface' in text
print('SPOREBOUND REAL CLIENT ACCEPTANCE PASS')
