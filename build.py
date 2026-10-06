#!/usr/bin/env python3
"""Build with a local JDK and Android SDK; downloads no dependencies.

Set ANDROID_SDK_ROOT and JAVA_HOME. Usage:
python3 build.py --build-dir PATH --signing-dir PRIVATE_PATH --output FILE.apk
Keep PRIVATE_PATH safe: the same signing key is needed for app updates.
"""
import argparse, os, pathlib, secrets, shutil, subprocess, zipfile

p = argparse.ArgumentParser()
p.add_argument('--build-dir', required=True, type=pathlib.Path)
p.add_argument('--signing-dir', required=True, type=pathlib.Path)
p.add_argument('--output', required=True, type=pathlib.Path)
a = p.parse_args()
root = pathlib.Path(__file__).resolve().parent
sdk = pathlib.Path(os.environ['ANDROID_SDK_ROOT'])
jdk = pathlib.Path(os.environ['JAVA_HOME']) / 'bin'
bt = sdk / 'build-tools' / '36.1.0'
android = sdk / 'platforms' / 'android-36' / 'android.jar'
build = a.build_dir.resolve(); signing = a.signing_dir.resolve()
build.mkdir(parents=True, exist_ok=True); signing.mkdir(parents=True, exist_ok=True)
signing.chmod(0o700)
for folder in ('classes', 'generated', 'dex'):
    destination = build / folder
    if destination.exists(): shutil.rmtree(destination)
    destination.mkdir()

def run(*args): subprocess.run([str(x) for x in args], check=True)

run(bt/'aapt2', 'compile', '--dir', root/'res', '-o', build/'resources.zip')
run(bt/'aapt2', 'link', '-o', build/'resources.apk', '-I', android,
    '--manifest', root/'AndroidManifest.xml', '--java', build/'generated', build/'resources.zip')
sources = list((root/'src').rglob('*.java')) + list((build/'generated').rglob('*.java'))
run(jdk/'javac', '-encoding', 'UTF-8', '-source', '8', '-target', '8', '-Xlint:-options',
    '-classpath', android, '-d', build/'classes', *sources)
with zipfile.ZipFile(build/'classes.jar', 'w') as jar:
    for source in sorted((build/'classes').rglob('*.class')):
        jar.write(source, source.relative_to(build/'classes'))
run(jdk/'java', '-cp', bt/'lib/d8.jar', 'com.android.tools.r8.D8', '--release', '--min-api', '30',
    '--lib', android, '--output', build/'dex', build/'classes.jar')
shutil.copyfile(build/'resources.apk', build/'unsigned.apk')
with zipfile.ZipFile(build/'unsigned.apk', 'a', compression=zipfile.ZIP_DEFLATED) as apk:
    for source in sorted((build/'dex').glob('*.dex')): apk.write(source, source.name)
run(bt/'zipalign', '-f', '4', build/'unsigned.apk', build/'aligned.apk')
password = signing/'password.txt'; key = signing/'still-home.p12'
if not key.exists():
    password.write_text(secrets.token_urlsafe(32)); password.chmod(0o600)
    run(jdk/'keytool', '-genkeypair', '-keystore', key, '-storetype', 'PKCS12', '-alias', 'still-home',
        '-storepass:file', password, '-keypass:file', password, '-keyalg', 'RSA', '-keysize', '3072',
        '-validity', '10000', '-dname', 'CN=Still Home personal build', '-noprompt')
    key.chmod(0o600)
a.output.parent.mkdir(parents=True, exist_ok=True)
run(jdk/'java', '-jar', bt/'lib/apksigner.jar', 'sign', '--ks', key, '--ks-key-alias', 'still-home',
    '--ks-pass', 'file:'+str(password), '--out', a.output.resolve(), build/'aligned.apk')
run(jdk/'java', '-jar', bt/'lib/apksigner.jar', 'verify', '--verbose', a.output.resolve())
print('Built:', a.output.resolve())
