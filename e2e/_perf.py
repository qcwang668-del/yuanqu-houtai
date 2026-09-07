import urllib.request, time, re
base = "http://120.79.142.141/admin/"
t0 = time.time()
req = urllib.request.Request(base + "index.html", headers={"Accept-Encoding": "gzip,deflate"})
with urllib.request.urlopen(req, timeout=30) as r:
    html = r.read()
print("index.html:", len(html), "B,", round(time.time() - t0, 3), "s")
text = html.decode("utf-8", "ignore")
scripts = re.findall(r"src=\"(/admin/assets/[^\"]+\.js)\"", text)
css = re.findall(r"href=\"(/admin/assets/[^\"]+\.css)\"", text)
print("js files:", len(scripts))
print("css files:", len(css))
for s in scripts:
    print("  JS", s[-70:])
for s in css:
    print("  CSS", s[-70:])
total_bytes = 0
total_time = 0
for path in scripts[:5]:
    t = time.time()
    req = urllib.request.Request("http://120.79.142.141" + path, headers={"Accept-Encoding": "gzip,deflate"})
    with urllib.request.urlopen(req, timeout=60) as r:
        data = r.read()
    dt = time.time() - t
    total_bytes += len(data)
    total_time += dt
    name = path.split("/")[-1][:50]
    print(f"  {name:50s} {len(data):>8}B  {dt:5.2f}s  {len(data)/dt/1024:.1f}KB/s")
print("total top5 js gz:", total_bytes, "B,", round(total_time, 2), "s")
