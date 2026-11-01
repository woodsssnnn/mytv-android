#!/usr/bin/env python3
"""瘦身 EPG 构建脚本。stdin: fanmingming e.xml 全文；stdout: 过滤后的 xml。
只保留精选源频道的 <channel>/<programme> 块（频道清单复用 build_source.OWNER）。"""
import re
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from build_source import ORDER

WANT = set(ORDER)

def main():
    text = sys.stdin.read()
    parts = []
    m = re.search(r"^.*?<tv[^>]*>", text, re.S)
    if m:
        parts.append(m.group(0))
    for c in sorted(WANT, key=len, reverse=True):
        pat = re.compile(
            r'<channel id="' + re.escape(c) + r'">.*?</channel>'
            r'|<programme [^>]*channel="' + re.escape(c) + r'"[^>]*>.*?</programme>', re.S)
        parts.extend(m.group(0) for m in pat.finditer(text))
    parts.append("</tv>")
    sys.stdout.write("\n".join(parts))

if __name__ == "__main__":
    main()
