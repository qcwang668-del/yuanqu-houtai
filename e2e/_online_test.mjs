import puppeteer from "file:///C:/Users/wangyan/AppData/Roaming/npm/node_modules/puppeteer/lib/puppeteer/puppeteer.js"
const URL = "http://120.79.142.141/admin/park-screen"
const b = await puppeteer.launch({ headless: true, executablePath: "C:/Users/wangyan/AppData/Local/ms-playwright/chromium-1228/chrome-win64/chrome.exe", args: ["--no-sandbox", "--disable-extensions", "--disable-background-networking", "--disable-features=PaintHolding"], defaultViewport: { width: 1920, height: 1080 } })
const p = await b.newPage()
const t0 = Date.now()
p.on('console', m => console.log('[CONSOLE]', m.text()))
p.on('pageerror', e => console.log('[PAGEERROR]', e.message))
try {
  await p.goto(URL, { waitUntil: "networkidle2", timeout: 60000 })
  console.log('LOADED in', (Date.now() - t0) + 'ms')
  const title = await p.title()
  console.log('TITLE:', title)
  const url = p.url()
  console.log('URL:', url)
  // 检查是否有 loading / 错误提示
  const bodyText = await p.evaluate(() => document.body.innerText.slice(0, 500))
  console.log('BODY:', bodyText)
  await p.screenshot({ path: "C:/Users/wangyan/Documents/ChatGPT/深投控/liqi-deploy/liqi-saas.v2/e2e/evidence/online_screen_loading.png", fullPage: true })
} catch (e) {
  console.log('ERR:', e.message)
} finally { await b.close() }
