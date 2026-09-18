#!/usr/bin/env python3
"""Read the disposable fixture's actual entity region before starting the second server."""
import gzip, io, pathlib, struct, sys, zlib

def payload(stream, kind):
    def number(fmt):
        return struct.unpack('>'+fmt, stream.read(struct.calcsize('>'+fmt)))[0]
    def string():
        return stream.read(number('H')).decode('utf-8')
    if kind == 0: return None
    if kind in (1, 2, 3, 4, 5, 6): return number({1:'b',2:'h',3:'i',4:'q',5:'f',6:'d'}[kind])
    if kind == 7: return stream.read(number('i'))
    if kind == 8: return string()
    if kind == 9:
        child=number('B'); count=number('i'); return [payload(stream,child) for _ in range(count)]
    if kind == 10:
        result={}
        while True:
            child=number('B')
            if child == 0: return result
            name=string(); result[name]=payload(stream,child)
    if kind in (11,12): return [number('i' if kind==11 else 'q') for _ in range(number('i'))]
    raise ValueError('Unknown NBT tag '+str(kind))

world=pathlib.Path(sys.argv[1]); region=world/'dimensions/sporebound/blighted_world/entities/r.0.0.mca'
raw=region.read_bytes(); location=int.from_bytes(raw[(11+11*32)*4:(11+11*32)*4+4],'big')
assert location>>8, 'The restart fixture entity chunk was not saved'
offset=(location>>8)*4096; length=int.from_bytes(raw[offset:offset+4],'big'); compression=raw[offset+4]
encoded=raw[offset+5:offset+4+length]
blob={1:gzip.decompress,2:zlib.decompress,3:lambda b:b}[compression](encoded)
stream=io.BytesIO(blob); kind=stream.read(1)[0]; name_length=int.from_bytes(stream.read(2),'big');stream.read(name_length)
root=payload(stream,kind)
entities=root.get('Entities',[]);matches=[e for e in entities if e.get('BiomassOrigin')=='sporebound:restart_fixture']
assert len(matches)==1, 'Expected one saved biomass fixture, found '+str([(e.get('id'),e.get('BiomassOrigin')) for e in entities])
e=matches[0];assert e['BiomassMass']==5 and e['BiomassHunger']==100
print('SPOREBOUND ENTITY DISK AUDIT PASS: biomass saved with mass=5, hunger=100 at '+str(e['Pos']),flush=True)
