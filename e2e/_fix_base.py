p = "frontend/.env.local"
s = open(p, encoding="utf-8").read()
old = "VITE_BASE_PATH=/"
new = "VITE_BASE_PATH=/admin/"
if old in s:
    s = s.replace(old, new)
    open(p, "w", encoding="utf-8").write(s)
    print("LOCAL UPDATED")
else:
    print("LOCAL NOT FOUND")

# 同时修正 .env.prod，保持一致
p2 = "frontend/.env.prod"
s2 = open(p2, encoding="utf-8").read()
if old in s2:
    s2 = s2.replace(old, new)
    open(p2, "w", encoding="utf-8").write(s2)
    print("PROD UPDATED")
else:
    print("PROD NOT FOUND")
