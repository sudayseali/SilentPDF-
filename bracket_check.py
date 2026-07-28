import sys
filename = sys.argv[1]
with open(filename) as f:
    lines = f.readlines()

depth = 0
for i, line in enumerate(lines):
    import re
    line_no_str = re.sub(r'".*?"', '""', line)
    line_no_str = re.sub(r'//.*', '', line_no_str)
    
    opens = line_no_str.count('{')
    closes = line_no_str.count('}')
    
    depth += opens - closes
    if depth < 0:
        print(f"Negative depth at line {i+1}: {line.strip()}")
        break

print(f"Final depth: {depth}")
