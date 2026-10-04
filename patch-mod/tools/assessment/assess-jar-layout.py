"""Reproduce packaging discovery against cached Forge 40.3.11. No game launch or install.
Run from any directory with Python 3 and JDK 17 installed; all outputs stay under patch-mod/build.
"""
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
import subprocess, json, hashlib, os, argparse

patch=Path(__file__).resolve().parents[2]
output=patch/'build/merge-assessment'
output.mkdir(parents=True,exist_ok=True)
version=next(line.split('=',1)[1] for line in (patch/'gradle.properties').read_text().splitlines() if line.startswith('mod_version='))
installed=patch.parent/'translate-packs/mods'
parser=argparse.ArgumentParser(description='Historical two-JAR assessment; supply archived pre-merge artifacts.')
parser.add_argument('--runtime',type=Path,required=True)
parser.add_argument('--transformer',type=Path,required=True)
args=parser.parse_args()
runtime=args.runtime
transformer=args.transformer
cache=Path(os.environ['USERPROFILE'])/'.gradle/caches/modules-2/files-2.1'
coordinates={
 'net.minecraftforge/fmlloader':'1.18.2-40.3.11','net.minecraftforge/forgespi':'4.0.15-4.x',
 'cpw.mods/modlauncher':'9.1.3','cpw.mods/securejarhandler':'1.0.8',
 'net.minecraftforge/accesstransformers':'8.0.4','net.sf.jopt-simple/jopt-simple':'5.0.4',
 'org.apache.logging.log4j/log4j-api':'2.17.1','org.apache.logging.log4j/log4j-core':'2.17.1',
 'com.google.guava/guava':'31.0.1-jre','com.google.code.gson/gson':'2.8.9',
 'org.ow2.asm/asm':'9.7.1','org.ow2.asm/asm-tree':'9.7.1',
 'com.electronwill.night-config/core':'3.6.4','com.electronwill.night-config/toml':'3.6.4',
 'org.apache.maven/maven-artifact':'3.8.5','org.apache.commons/commons-lang3':'3.12.0'
}
jars=[]
for module,v in coordinates.items():
 matches=list((cache/module/v).glob('*/'+module.split('/')[-1]+'-'+v+'.jar'))
 if len(matches)!=1:raise RuntimeError(f'Expected cached {module}:{v}: {matches}')
 jars.append(matches[0])
def fixture(scenario,name,base,nested=None,flat=False):
 folder=output/scenario/'mods';folder.mkdir(parents=True,exist_ok=True)
 with ZipFile(folder/name,'w',ZIP_DEFLATED) as target,ZipFile(base) as source:
  names=set()
  for n in source.namelist():target.writestr(n,source.read(n));names.add(n)
  if nested:target.writestr('META-INF/jarjar/embedded.jar',nested.read_bytes())
  if flat:
   with ZipFile(runtime) as inner:
    for n in inner.namelist():
     if n not in names:target.writestr(n,inner.read(n))
fixture('pair','runtime.jar',runtime);fixture('pair','transformer.jar',transformer)
fixture('flat','flat.jar',transformer,flat=True)
fixture('runtime-outer','runtime-outer.jar',runtime,nested=transformer)
fixture('transformer-outer','transformer-outer.jar',transformer,nested=runtime)
(output/'extracted').mkdir(exist_ok=True)
with ZipFile(output/'transformer-outer/mods/transformer-outer.jar') as bundle:
 (output/'extracted/runtime.jar').write_bytes(bundle.read('META-INF/jarjar/embedded.jar'))
java=Path(os.environ.get('ProgramFiles','C:/Program Files'))/'Java/jdk-17/bin/java.exe'
logging=output/'log4j2.xml';logging.write_text('<Configuration status="OFF"><Appenders/><Loggers><Root level="off"/></Loggers></Configuration>',encoding='utf-8')
command=[str(java),'-Dlog4j.configurationFile='+logging.as_uri(),'--add-opens','java.base/java.util.jar=ALL-UNNAMED','--add-opens','java.base/java.lang.invoke=ALL-UNNAMED','-cp',os.pathsep.join(map(str,jars)),str(Path(__file__).with_name('JarLayoutProbe.java')),str(output)]
result=subprocess.run(command,capture_output=True,text=True,errors='replace')
(output/'probe-output.txt').write_text(result.stdout+'\n'+result.stderr,encoding='utf-8')
report={'runtimeVersion':version,'forge':'40.3.11','modLauncher':'9.1.3','exitCode':result.returncode,'stdout':result.stdout,'stderr':result.stderr,'dependencies':{p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in jars},'gameLaunched':False}
(output/'probe-report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(result.stdout);print(result.stderr);raise SystemExit(result.returncode)
