p='e2e/_online_test.mjs'
s=open(p,encoding='utf-8').read()
s=s.replace('args: ["--no-sandbox"]', 'args: ["--no-sandbox", "--disable-extensions", "--disable-background-networking", "--disable-features=PaintHolding"]')
open(p,'w',encoding='utf-8').write(s)
print('ok')
