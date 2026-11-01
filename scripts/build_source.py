#!/usr/bin/env python3
"""老人民精选源构建脚本（数据链）。stdin: 合并候选 TSV(canon\turl)，stdout: m3u。
配合 probe-results TSV（code\tlat\tcanon\turl，环境变量 PROBE_FILE 指定，默认 probe-final.tsv）。
规则：实测存活优先按延迟；健康域名加分；8线路/台；频道名对齐 fanmingming EPG（CCTV1 无连字符）。"""
import io
import os
import re
import sys

CCTV = ["CCTV1", "CCTV2", "CCTV3", "CCTV4", "CCTV5", "CCTV6", "CCTV7", "CCTV8",
        "CCTV9", "CCTV10", "CCTV11", "CCTV12", "CCTV13", "CCTV14", "CCTV15",
        "CCTV16", "CCTV17", "CCTV5+"]
WEISHI = ["湖南卫视", "浙江卫视", "东方卫视", "江苏卫视", "北京卫视", "安徽卫视",
          "山东卫视", "天津卫视", "深圳卫视", "江西卫视", "湖北卫视", "黑龙江卫视",
          "辽宁卫视", "广东卫视", "广西卫视", "重庆卫视", "四川卫视", "河南卫视",
          "河北卫视", "吉林卫视", "东南卫视", "海南卫视", "贵州卫视", "云南卫视",
          "甘肃卫视", "青海卫视", "内蒙古卫视", "宁夏卫视", "新疆卫视", "西藏卫视",
          "三沙卫视", "大湾区卫视", "厦门卫视", "延边卫视", "安多卫视", "康巴卫视"]
ORDER = CCTV + WEISHI
LOGO_BASE = "https://live.fanmingming.com/tv/"

def bad(u):
    t = ("wsSecret", "wsTime", "auth_key", "sign=", "token=", "Expires=",
         "exp_hrchy", "douyincdn", "huoshan", "toutiao",
         "javascript://", "rtmp://", "udp://", "rtp://")
    return any(tok in u for tok in t)

def normalize(name):
    m = re.match(r"CCTV-?(\d{1,2})\+?", name, re.I)
    if m:
        return "CCTV5+" if (m.group(1) == "5" and "+" in name) else "CCTV" + m.group(1)
    return next((w for w in WEISHI if name.startswith(w)), None)

def main():
    candidates = {}
    for line in io.TextIOWrapper(sys.stdin.buffer, encoding="utf-8", errors="replace"):
        p = line.rstrip("\n").split("\t")
        if len(p) == 2 and p[1] and not bad(p[1]) and p[1].startswith(("http://", "https://")):
            c = normalize(p[0])
            if c:
                lst = candidates.setdefault(c, [])
                if p[1] not in lst:
                    lst.append(p[1])

    alive = {}
    lat = {}
    probe_file = os.environ.get("PROBE_FILE", "probe-final.tsv")
    if os.path.exists(probe_file):
        for line in io.open(probe_file, encoding="utf-8", errors="replace"):
            p = line.rstrip("\n").split("\t")
            if len(p) >= 4 and re.match(r"^\d+$", p[0]) and 200 <= int(p[0]) < 400:
                c = normalize(p[2])
                try:
                    lat[p[3]] = float(p[1])
                except ValueError:
                    lat[p[3]] = 9.9
                if c and p[3] not in alive.setdefault(c, []):
                    alive[c].append(p[3])

    healthy = ("wqwqwq", "118.122.144.115", "38.75.136.137", "58.57.40.22",
               "204.12.221.218", "173.208.212.130")

    out = ["#EXTM3U"]
    n = 0
    for pos, canon in enumerate(ORDER, 1):
        grp = "央视" if canon.startswith("CCTV") else "卫视"
        logo = LOGO_BASE + canon + ".png"
        verified = sorted(set(alive.get(canon, [])), key=lambda u: lat.get(u, 9.9))[:5]
        pool = [u for u in candidates.get(canon, []) if u not in verified]
        pool.sort(key=lambda u: (0 if any(h in u for h in healthy) else 1))
        lines = (verified + pool[:8 - len(verified)]) if verified else pool[:8]
        if not lines:
            print("MISS " + canon, file=sys.stderr)
            continue
        for u in lines:
            out.append(f'#EXTINF:-1 tvg-chno="{pos}" tvg-id="{canon}" tvg-name="{canon}" '
                       f'tvg-logo="{logo}" group-title="{grp}",{canon}')
            out.append(u)
            n += 1
    print("\n".join(out))
    print(f"# v3 {len(ORDER)} 频道 / {n} 线路", file=sys.stderr)

if __name__ == "__main__":
    main()
