// preflight.mjs —— 录制前「干跑」预检测试(模板)。与 make-video.mjs 同款元素查找(iframe 感知)。
// 逐场景真实执行/校验，断言每个场景的「预期生成物」是否出现，全绿(exit 0)才可录制：
//   · open  → 点开卡片后断言面板打开(marker=sc.waitFor||sc.expect||面板输入框出现)
//   · enter → 点进子系统/文章后断言子页 marker(sc.waitFor) 出现(含新标签转 goto)
//   · ask   → 断言推荐问题元素在页面(含 iframe)命中、可点
//   · chat  → 断言输入框(含 iframe)存在、可打字
//   · click/tab/point/sweep → 断言每个 target 元素命中(box 非空)
//   · 任意场景 sc.expect='文本' → 额外断言该文本(含 iframe)出现
// 有 FAIL(exit 1) 则禁止录制并列出差异；exit 2=脚本异常。
// 用法: LD_LIBRARY_PATH=~/.local/chrome-libs node scripts/preflight.mjs [start] [end]
import { chromium } from 'playwright'
import fs from 'node:fs'
import { PAGES } from './video-scenes.mjs'

const ROOT = 'evidence/video', SHOTS = ROOT + '/shots'
fs.mkdirSync(SHOTS, { recursive: true })
// —— 登录态注入:力企云用真实 UI 登录后保存的 storageState(与 make-video.mjs 一致)——
const BASE = 'http://127.0.0.1:5180'
const STORAGE = 'auth.json'

const framesAll = page => [page.mainFrame(), ...page.frames().filter(f => f !== page.mainFrame())]
const box = async (page, text) => {
  for (const fr of framesAll(page)) {
    const cands = [fr.getByRole('button', { name: text, exact: false }), fr.getByRole('tab', { name: text, exact: false }), fr.getByText(text, { exact: false })]
    for (const loc of cands) {
      const n = await loc.count().catch(() => 0)
      for (let i = 0; i < Math.min(n, 6); i++) {
        let b = await loc.nth(i).boundingBox().catch(() => null)
        if (!b || b.width <= 0 || b.height <= 0) continue
        if (b.y < 0 || b.y > 806) { await loc.nth(i).scrollIntoViewIfNeeded({ timeout: 1200 }).catch(() => {}); await page.waitForTimeout(250); b = await loc.nth(i).boundingBox().catch(() => null); if (!b) continue }
        if (b.width > 0 && b.height > 0 && b.y >= 0 && b.y <= 806) return b
      }
    }
  }
  return null
}
const clickBox = async (page, b) => { await page.mouse.click(Math.round(b.x + Math.min(b.width, 140) / 2), Math.round(b.y + b.height / 2)); await page.waitForTimeout(700) }
const hasText = async (page, t) => { for (const fr of framesAll(page)) { if (await fr.getByText(t, { exact: false }).count().catch(() => 0)) return true } return false }
const hasInput = async (page) => { for (const fr of framesAll(page)) { if (await fr.locator('[placeholder*="请输入"], textarea, [contenteditable="true"]').count().catch(() => 0)) return true } return false }
const panelReady = async (page, marker, ms = 7000) => {
  const end = Date.now() + ms
  while (Date.now() < end) {
    if (await hasInput(page)) return true
    if (marker && marker !== '请输入内容' && await hasText(page, marker)) return true
    await page.waitForTimeout(300)
  }
  return false
}

async function checkPage(idx, cfg, browser, report) {
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 810 }, storageState: STORAGE })
  const page = await ctx.newPage()
  await page.goto(BASE + cfg.path, { waitUntil: 'domcontentloaded', timeout: 40000 }).catch(() => {})
  await page.waitForTimeout(2600)

  for (let s = 0; s < cfg.scenes.length; s++) {
    const sc = cfg.scenes[s]
    const action = sc.open ? 'open' : sc.enter ? 'enter' : sc.ask ? 'ask' : sc.chat ? 'chat' : sc.click ? 'click' : sc.tab ? 'tab' : sc.sweep ? 'sweep' : sc.point ? 'point' : sc.scroll ? 'scroll' : 'say'
    let ok = true, detail = ''
    try {
      if (sc.open) {
        const b = await box(page, sc.open)
        if (!b) { ok = false; detail = `卡片/菜单未命中: ${sc.open}` }
        else { await clickBox(page, b); ok = await panelReady(page, sc.waitFor || sc.expect); detail = ok ? `面板已打开` : `❌点开后面板未出现(期望 ${sc.waitFor || sc.expect || '输入框'})` }
      } else if (sc.enter) {
        const b = await box(page, sc.enter)
        if (!b) { ok = false; detail = `子系统入口未命中: ${sc.enter}` }
        else {
          let popup = null; const grab = p => { popup = p }; ctx.on('page', grab)
          await clickBox(page, b); await page.waitForTimeout(1500); ctx.off('page', grab)
          if (popup) { const u = popup.url(); await popup.close().catch(() => {}); if (u && !u.startsWith('about')) { await page.goto(u, { waitUntil: 'load', timeout: 30000 }).catch(() => {}); await page.waitForTimeout(4000) } }
          ok = await panelReady(page, sc.waitFor || sc.expect, 8000); detail = ok ? `子页已进入` : `❌进入后子页 marker 未现(期望 ${sc.waitFor || sc.expect})`
        }
      } else if (sc.ask) {
        const b = await box(page, sc.ask)
        ok = !!b; detail = ok ? `推荐问题可点: ${sc.ask}` : `❌推荐问题未命中(iframe 内也没有): ${sc.ask}`
      } else if (sc.chat) {
        ok = await hasInput(page); detail = ok ? `输入框可打字` : `❌未找到可打字输入框(iframe 内也没有)`
      } else {
        const targets = [sc.click, sc.tab, sc.point, ...(sc.sweep || [])].filter(Boolean)
        for (const t of targets) { if (!(await box(page, t))) { ok = false; detail += `未命中: ${t}; ` } }
        if (ok && targets.length) detail = `元素全部命中(${targets.length}个)`
        else if (!targets.length) detail = action === 'scroll' ? '滚动(无需断言)' : '纯口播(无需断言)'
      }
      if (ok && sc.expect && !await hasText(page, sc.expect)) { ok = false; detail += ` ❌期望文本未现: ${sc.expect}` }
    } catch (e) { ok = false; detail = '异常: ' + e.message }
    await page.screenshot({ path: `${SHOTS}/preflight_${String(idx).padStart(2, '0')}_${s}.png` }).catch(() => {})
    report.push({ page: cfg.name, scene: s, action, ok, detail })
    console.log(`  [${ok ? 'PASS' : 'FAIL'}] ${cfg.name} #${s} (${action}) ${detail}`)
  }
  await ctx.close()
}

async function main() {
  const s = parseInt(process.argv[2] ?? '0'), e = parseInt(process.argv[3] ?? String(PAGES.length - 1))
  const browser = await chromium.launch({ headless: true, args: ['--no-sandbox', '--disable-dev-shm-usage'] })
  const report = []
  for (let i = s; i <= e && i < PAGES.length; i++) {
    console.log(`\n── 预检 片${String(i).padStart(2, '0')} ${PAGES[i].name} ──`)
    try { await checkPage(i, PAGES[i], browser, report) }
    catch (err) { report.push({ page: PAGES[i].name, scene: -1, ok: false, detail: '页异常: ' + err.message }) }
  }
  await browser.close()
  const fails = report.filter(r => !r.ok)
  fs.writeFileSync(`${ROOT}/preflight-report.json`, JSON.stringify(report, null, 2))
  console.log(`\n═══ 预检结果: ${report.length - fails.length}/${report.length} PASS ═══`)
  if (fails.length) {
    console.log('❌ 未通过，禁止录制。请先修复以下场景(改 target / 加 waitFor / 调 open 目标):')
    fails.forEach(f => console.log(`   · ${f.page} #${f.scene}: ${f.detail}`))
    process.exit(1)
  }
  console.log('✅ 所有场景「预期生成物」均命中，操作与预期一致，可以开始录制。')
  process.exit(0)
}
main().catch(e => { console.error('FATAL', e); process.exit(2) })
