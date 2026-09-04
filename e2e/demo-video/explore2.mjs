// 探查 v2:用 storageState 免登录,逐个点父菜单→子菜单,记录 hash + dump 真实元素
import { chromium } from 'playwright'
import fs from 'node:fs'
const BASE = 'http://127.0.0.1:5180'
const OUT = 'explore-out'
const browser = await chromium.launch({ headless: true, args: ['--no-sandbox', '--disable-dev-shm-usage'] })
const ctx = await browser.newContext({ viewport: { width: 1440, height: 810 }, storageState: 'auth.json' })
const page = await ctx.newPage()
await page.goto(BASE + '/index', { waitUntil: 'domcontentloaded', timeout: 20000 }).catch(() => {})
await page.waitForTimeout(3000)

const dump = async () => page.evaluate(() => {
  const uniq = a => [...new Set(a.map(s => (s || '').trim()).filter(Boolean))]
  return {
    hash: location.hash || location.pathname,
    active: document.querySelector('.el-menu-item.is-active')?.innerText?.trim() || '',
    title: uniq([...document.querySelectorAll('h1,h2,h3,.el-card__header,.card-header')].map(e => e.innerText)).slice(0, 6),
    tabs: uniq([...document.querySelectorAll('.el-tabs__item')].map(e => e.innerText)),
    btns: uniq([...document.querySelectorAll('.app-container button, .el-main button')].map(e => (e.innerText || '').trim())).filter(x => x && x.length <= 12).slice(0, 24),
    th: uniq([...document.querySelectorAll('.el-table__header th .cell')].map(e => e.innerText)).slice(0, 24),
    labels: uniq([...document.querySelectorAll('.el-form-item__label')].map(e => e.innerText)).slice(0, 20),
  }
})

// 目标:父菜单 → 子菜单文字
const targets = [
  { key: '首页', parent: null, child: null },
  { key: '平台-用户管理', parent: '平台管理', child: '用户管理' },
  { key: '平台-匹配线索', parent: '平台管理', child: '匹配线索管理' },
  { key: '企业-园区客户', parent: '企业管理', child: '园区客户管理' },
  { key: '企业-获批动态', parent: '企业管理', child: '企业获批动态' },
  { key: '导入导出记录', parent: '我的导入导出', child: '导入导出记录' },
  { key: '系统-网站配置', parent: '系统管理', child: '网站配置' },
]
const results = []
for (const t of targets) {
  try {
    if (t.parent) {
      // 展开父菜单
      const p = page.locator('.el-sub-menu__title', { hasText: t.parent }).first()
      if (await p.count()) { await p.click().catch(() => {}); await page.waitForTimeout(700) }
      // 点子菜单(在展开的 inline 菜单里精确匹配)
      const c = page.locator('.el-menu-item', { hasText: t.child }).filter({ hasText: t.child }).last()
      if (await c.count()) { await c.click().catch(() => {}); await page.waitForTimeout(1800) }
    } else {
      await page.goto(BASE + '/index', { waitUntil: 'domcontentloaded' }).catch(() => {})
      await page.waitForTimeout(1800)
    }
    const info = await dump(); info.key = t.key
    results.push(info)
    await page.screenshot({ path: `${OUT}/v2-${t.key}.png` })
    console.log(`\n== [${t.key}] active=${info.active} hash=${info.hash}`)
    console.log('  标题:', info.title.join(' | '))
    console.log('  tabs:', info.tabs.join(' | '))
    console.log('  按钮:', info.btns.join(' | '))
    console.log('  表头:', info.th.join(' | '))
    console.log('  表单标签:', info.labels.join(' | '))
  } catch (e) { console.log(`[${t.key}] ERR`, e.message) }
}
fs.writeFileSync(`${OUT}/pages2.json`, JSON.stringify(results, null, 2))
await browser.close()
console.log('\n探查v2完成')
