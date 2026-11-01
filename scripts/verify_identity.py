#!/usr/bin/env python3
"""内容一致性校验。stdin: identity-input.tsv(canon TAB url); stdout: identity.tsv(url TAB verdict)。
verdict: ok=同频道最大簇成员; mismatch=离群(串台/测试卡); unknown=帧不足/不可判。
方法: 每条活线 ffmpeg 抽帧, 台标区二值 dHash, 同频道内聚类, 最大簇(>=2)即真台。"""
import os
import subprocess
import sys
import tempfile

from PIL import Image

def dhash(img, size=8):
    g = img.convert('L').point(lambda p: 255 if p > 185 else 0)
    g = g.resize((size + 1, size), Image.LANCZOS)
    px = list(g.getdata())
    bits = 0
    for r in range(size):
        for c in range(size):
            bits = (bits << 1) | (1 if px[r * (size + 1) + c] > px[r * (size + 1) + c + 1] else 0)
    return bits

def ham(a, b):
    return bin(a ^ b).count('1')

def main():
    tmpdir = tempfile.mkdtemp(prefix="identity-")
    chans = {}
    for line in sys.stdin:
        p = line.rstrip("\n").split("\t")
        if len(p) == 2 and p[1]:
            chans.setdefault(p[0], []).append(p[1])

    out = []
    templates = {}
    for pair in os.environ.get("TPL_HASHES", "").split(","):
        p = pair.split(":")
        if len(p) == 2 and p[0] and p[1].isdigit():
            templates[p[0]] = int(p[1])
    for canon, urls in chans.items():
        hashes = {}
        for u in urls:
            f = os.path.join(tmpdir, str(abs(hash(u))) + ".jpg")
            subprocess.run(
                ["ffmpeg", "-y", "-loglevel", "error", "-rw_timeout", "8000000",
                 "-i", u, "-frames:v", "1", "-q:v", "4", f],
                capture_output=True)
            if os.path.exists(f):
                try:
                    im = Image.open(f)
                    w, h = im.size
                    crop = im.crop((int(w*0.035), int(h*0.05), int(w*0.145), int(h*0.20)))
                    hashes[u] = dhash(crop)
                except Exception:
                    pass
            os.remove(f) if os.path.exists(f) else None
        near = {u: sum(1 for v in hashes.values() if ham(hashes[u], v) <= 12) for u in hashes}
        best = max(near.values()) if near else 0
        for u in urls:
            if u not in hashes:
                out.append(u + "\tunknown")
            elif best >= 2 and near[u] == best:
                out.append(u + "\tok")
            elif best >= 2:
                out.append(u + "\tmismatch")
            elif canon in templates and ham(hashes[u], templates[canon]) <= 12:
                out.append(u + "\tok")
            elif canon in templates:
                out.append(u + "\tmismatch")
            else:
                out.append(u + "\tunknown")
    sys.stdout.write("\n".join(out) + "\n")

if __name__ == "__main__":
    main()
