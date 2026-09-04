import puppeteer from "file:///C:/Users/wangyan/AppData/Roaming/npm/node_modules/puppeteer/lib/puppeteer/puppeteer.js"
const URL = "http://127.0.0.1:5180"
const OUT = "C:/Users/wangyan/Documents/ChatGPT/深投控/liqi-deploy/liqi-saas.v2/e2e/evidence/"
const b = await puppeteer.launch({ headless: true, executablePath: "C:/Users/wangyan/AppData/Local/ms-playwright/chromium-1228/chrome-win64/chrome.exe", args: ["--no-sandbox"], defaultViewport: { width: 1920, height: 1080 } })
const p = await b.newPage()
const wait = ms => new Promise(r => setTimeout(r, ms))
try {
  await p.goto(URL + "/", { waitUntil: "networkidle2", timeout: 60000 })
  await wait(3500)
  const clicked = await p.evaluate(() => {
    const bs = Array.from(document.querySelectorAll("button"))
    const t = bs.find(e => e.textContent.trim() === "登录" && e.offsetParent !== null)
    if (t) { t.click(); return true }
    return false
  })
  console.log("clicked:", clicked)
  await wait(8000)
  console.log("URL after login:", p.url())
  await p.screenshot({ path: OUT + "dbg_after_login.png" })
  await p.goto(URL + "/park-screen", { waitUntil: "networkidle2", timeout: 60000 })
  await wait(8000)
  console.log("URL screen:", p.url())
  const rows = await p.$$eval("#dashRank .r", els => els.map(e => {
    const nm = e.querySelector(".nm"), vv = e.querySelector(".vv")
    const r = vv ? vv.getBoundingClientRect() : null
    const cs = vv ? getComputedStyle(vv) : null
    return { name: nm ? nm.textContent.trim() : "", val: vv ? vv.textContent.trim() : "",
      hPx: r ? Math.round(r.height) : 0, lineH: cs ? cs.lineHeight : "", ws: cs ? cs.whiteSpace : "",
      overflow: vv ? (vv.scrollWidth > vv.clientWidth + 1) : null }
  }))
  console.log(JSON.stringify(rows, null, 1))
  console.log("NOTE:", await p.$eval("#rankNote", e => e.textContent.trim()).catch(() => "-"))
  const el = await p.$("#dashRank"); if (el) await el.screenshot({ path: OUT + "rank_fund.png" })
  await p.screenshot({ path: OUT + "rank_screen_full.png" })
} catch (e) { console.log("ERR:", e.message) } finally { await b.close() }