import subprocess
import re

out = ""
try:
    print("Running gradle...")
    subprocess.check_output(["cmd.exe", "/c", "gradlew.bat", "app:assembleDebug", "--console=plain", "--no-daemon"], stderr=subprocess.STDOUT)
    print("Success")
except subprocess.CalledProcessError as e:
    out = e.output.decode('utf-8', errors='ignore')

errors = []
for line in out.split('\n'):
    line = line.replace('\r', '')
    if "e: file://" in line or "e: C:" in line or "e: c:" in line:
        errors.append(line.strip())
    elif "Unresolved reference" in line:
        errors.append(line.strip())

with open("compilation_errors.txt", "w", encoding="utf-8") as f:
    f.write("\n".join(errors))
print("Errors extracted to compilation_errors.txt:", len(errors))
