"""Validate the produced single JAR in real Forge/ModLauncher module loaders, without Minecraft."""
from pathlib import Path
from zipfile import ZipFile
import subprocess, json, hashlib, os, shutil, tempfile
patch=Path(__file__).resolve().parents[2]
output=patch/'build/bundle-verification'
output.mkdir(parents=True,exist_ok=True)
version=next(line.split('=',1)[1] for line in (patch/'gradle.properties').read_text().splitlines() if line.startswith('mod_version='))
bundle=patch/'build/distribution'/f'vh3_translation_patch-{version}.jar'
cache=Path(os.environ['USERPROFILE'])/'.gradle/caches/modules-2/files-2.1'
coordinates={
 'net.minecraftforge/fmlloader':'1.18.2-40.3.11','net.minecraftforge/forgespi':'4.0.15-4.x',
 'cpw.mods/modlauncher':'9.1.3','cpw.mods/securejarhandler':'1.0.8',
 'net.minecraftforge/accesstransformers':'8.0.4','net.sf.jopt-simple/jopt-simple':'5.0.4',
 'org.apache.logging.log4j/log4j-api':'2.17.1','org.apache.logging.log4j/log4j-core':'2.17.1',
 'com.google.guava/guava':'31.0.1-jre','com.google.code.gson/gson':'2.8.9',
 'org.ow2.asm/asm':'9.7.1','org.ow2.asm/asm-tree':'9.7.1',
 'org.ow2.asm/asm-commons':'9.7.1','org.ow2.asm/asm-util':'9.7.1','org.ow2.asm/asm-analysis':'9.7.1',
 'com.electronwill.night-config/core':'3.6.4','com.electronwill.night-config/toml':'3.6.4',
 'org.apache.maven/maven-artifact':'3.8.5','org.apache.commons/commons-lang3':'3.12.0'
}
jars=[]
for module,v in coordinates.items():
 matches=list((cache/module/v).glob('*/'+module.split('/')[-1]+'-'+v+'.jar'))
 if len(matches)!=1:raise RuntimeError(f'Expected cached {module}:{v}: {matches}')
 jars.append(matches[0])
game=Path(tempfile.mkdtemp(prefix=version+'-',dir=output))
(game/'mods').mkdir(exist_ok=True)
installed=game/'mods'/bundle.name
shutil.copyfile(bundle,installed)
core=patch.parent.parent/'origin-3.21.7/the_vault-1.18.2-3.21.6.6884.jar'
shutil.copyfile(core,game/'mods'/core.name)
shutil.copytree(patch.parent/'program/基础+硬编码汉化/config/vaultpatcher_asm',game/'config/vaultpatcher_asm',dirs_exist_ok=True)
shutil.copytree(patch.parent/'program/基础+硬编码汉化/vaultpatcher/modules',game/'vaultpatcher/modules',dirs_exist_ok=True)
with ZipFile(bundle) as jar:
 assert 'META-INF/mods.toml' not in jar.namelist()
 assert 'com/dricetea/vh3patch/TranslationPatchMod.class' not in jar.namelist()
 inner=jar.read('META-INF/vh3_translation_patch/runtime.jar')
 assert hashlib.sha256(inner).hexdigest() in jar.read('META-INF/vh3_translation_patch/runtime.properties').decode()
 assert inner==(patch/'runtime/build/libs'/f'vh3_translation_patch-{version}.jar').read_bytes()
 for n in jar.namelist():
  assert not n.endswith('.json') and '/test/' not in n
java=Path(os.environ.get('ProgramFiles','C:/Program Files'))/'Java/jdk-17/bin/java.exe'
logging=output/'log4j2.xml'
logging.write_text('<Configuration status="OFF"><Appenders/><Loggers><Root level="off"/></Loggers></Configuration>',encoding='utf-8')
classes=output/'probe-classes';classes.mkdir(exist_ok=True)
subprocess.run([str(java.with_name('javac.exe')),'-encoding','UTF-8','-cp',os.pathsep.join(map(str,jars)),'-d',str(classes),str(Path(__file__).with_name('BundleProbe.java'))],check=True)
command=[str(java),'-Duser.language=en','-Duser.country=US','-Dlog4j.configurationFile='+logging.as_uri(),'--add-opens','java.base/java.util.jar=cpw.mods.securejarhandler','--add-opens','java.base/java.lang.invoke=cpw.mods.securejarhandler','--module-path',os.pathsep.join(map(str,jars)),'--add-modules','ALL-MODULE-PATH','-cp',str(classes),'BundleProbe',str(game),str(installed),version]
result=subprocess.run(command,capture_output=True,text=True,errors='replace')
report={'version':version,'bundleSha256':hashlib.sha256(bundle.read_bytes()).hexdigest(),'runtimeSha256':hashlib.sha256(inner).hexdigest(),'exitCode':result.returncode,'stdout':result.stdout,'stderr':result.stderr,'gameLaunched':False}
(output/'report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(result.stdout);print(result.stderr);raise SystemExit(result.returncode)
