#!/usr/bin/env python3
"""Run narrow safety-rule checks without Android, a phone, or third-party packages."""
import os
from pathlib import Path
import subprocess
import tempfile

root = Path(__file__).resolve().parents[1]
java_bin = Path(os.environ['JAVA_HOME']) / 'bin'
with tempfile.TemporaryDirectory(prefix='still-policy-') as temporary:
    subprocess.run([str(java_bin / 'javac'), '-d', temporary,
                    str(root / 'src/org/stillhome/launcher/PrivacyPolicy.java'),
                    str(root / 'tests/PrivacyPolicyTest.java')], check=True)
    subprocess.run([str(java_bin / 'java'), '-cp', temporary,
                    'org.stillhome.launcher.PrivacyPolicyTest'], check=True)
