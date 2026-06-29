import os
import re

directory = r"C:\Users\User\AndroidStudioProjects\Ozamiz\app\src\main\java\uz\oybek\ozamiz"
pattern = re.compile(r'(?<!import )\b(uz\.oybek\.ozamiz\.data\.[a-zA-Z0-9_\.]+\.([A-Z][a-zA-Z0-9_]*))\b')

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    matches = list(pattern.finditer(content))
    if not matches:
        return False

    needed_imports = set()
    for match in matches:
        full_match = match.group(1)
        class_name = match.group(2)
        needed_imports.add(f"import {full_match}")

    new_content = pattern.sub(r'\2', content)

    lines = new_content.split('\n')
    package_idx = -1
    for i, line in enumerate(lines):
        if line.startswith('package '):
            package_idx = i
            break
            
    if package_idx == -1:
        return False
        
    existing_imports = set(l.strip() for l in lines if l.startswith('import '))
    imports_to_add = needed_imports - existing_imports
    
    # Check if we really need to write
    if not imports_to_add and new_content == content:
        return False
        
    if imports_to_add:
        res = lines[:package_idx+1]
        res.append("")
        for imp in sorted(imports_to_add):
            res.append(imp)
        res.extend(lines[package_idx+1:])
        lines = res

    with open(filepath, 'w', encoding='utf-8') as f:
        f.write('\n'.join(lines))
        
    return True

count = 0
for root, _, files in os.walk(directory):
    for f in files:
        if f.endswith('.kt') and not "fix_imports.py" in f:
            if process_file(os.path.join(root, f)):
                count += 1
print(f"Fixed {count} files")
