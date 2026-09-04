// 探查:真实 UI 登录力企云(5180)→存 storageState→dump 菜单路由与各页真实元素
import { chromium } from 'playwright'
import fs from 'node:fs'
const BASE = 'http://127.0.0.1:5180'
const OUT = 'explore-out'
fs.mkdirSync(OUT, { recursive: true })

const browser = await chromium.launch({ headless: true, args: ['--no-sandbox', '--disable-dev-shm-usage'] })
const ctx = await browser.newContext({ viewport: { width: 1440, height: 810 } })
const page = await ctx.newPage()

// 1) 登录
await page.goto(BASE + '/', { waitUntil: 'networkidle', timeout: 30000 }).catch(() => {})
await page.waitForTimeout(1500)
console.log('登录前 URL:', page.url())
// 填账号密码(验证码本地已关)
const userInp = page.locator('input:not([type=password])').first()
const passInp = page.locator('input[type=password]').first()
await userInp.fill('admin').catch(() => {})
await passInp.fill('admin123').catch(() => {})
await page.waitForTimeout(300)
// 点登录按钮
const btn = page.getByRole('button', { name: /登\s*录|登录/ }).first()
if (await btn.count()) await btn.click().catch(() => {})
else await passInp.press('Enter').catch(() => {})
await page.waitForTimeout(3500)
await page.waitForLoadState('networkidle').catch(() => {})
console.log('登录后 URL:', page.url())
await page.screenshot({ path: `${OUT}/after-login.png` })

// 2) 存 storageState
await ctx.storageState({ path: 'auth.json' })
console.log('storageState 已存 auth.json')
// dump localStorage 键名(供适配参考)
const ls = await page.evaluate(() => Object.fromEntries(Object.entries(localStorage).map(([k, v]) => [k, String(v).slice(0, 60)])))
fs.writeFileSync(`${OUT}/localStorage.json`, JSON.stringify(ls, null, 2))
console.log('localStorage 键:', Object.keys(ls).join(', '))

// 3) dump 侧边菜单(文本 + 跳转 hash)
const dumpMenu = async () => {
  return await page.evaluate(() => {
    const items = []
    document.querySelectorAll('.el-menu a, aside a, .el-menu-item, .el-sub-menu__title').forEach(el => {
      const t = (el.innerText || '').trim().split('\n')[0]
      const href = el.getAttribute('href') || (el.closest('a')?.getAttribute('href')) || ''
      if (t) items.push({ text: t, href })
    })
    return items
  })
}
const menu = await dumpMenu()
fs.writeFileSync(`${OUT}/menu.json`, JSON.stringify(menu, null, 2))
console.log('\n== 侧边菜单 ==')
menu.forEach(m => console.log(' ', m.text, m.href ? '  →' + m.href : ''))

// 4) 逐个菜单页:点击进入后 dump 标题/tab/按钮/表头
const dumpPage = async (label) => {
  await page.waitForTimeout(1200)
  const info = await page.evaluate(() => {
    const uniq = a => [...new Set(a.map(s => s.trim()).filter(Boolean))]
    const H = uniq([...document.querySelectorAll('h1,h2,h3,.el-page-header__content,.app-container .card-header,.breadcrumb')].map(e => e.innerText))
    const tabs = uniq([...document.querySelectorAll('.el-tabs__item')].map(e => e.innerText))
    const btns = uniq([...document.querySelectorAll('button')].map(e => (e.innerText || '').trim())).filter(x => x && x.length <= 12)
    const th = uniq([...document.querySelectorAll('.el-table__header th .cell, table th')].map(e => e.innerText))
    const menuActive = document.querySelector('.el-menu-item.is-active')?.innerText?.trim() || ''
    return { url: location.hash || location.pathname, H, tabs, btns: btns.slice(0, 20), th: th.slice(0, 20), menuActive }
  })
  info.label = label
  return info
}

const results = []
// 遍历顶层可点菜单文字
const topMenus = ['首页', '平台管理', '企业管理', '我的导入导出', '系统管理']
for (const name of topMenus) {
  const link = page.getByText(name, { exact: true }).first()
  if (await link.count()) {
    await link.click().catch(() => {})
    await page.waitForTimeout(900)
    // 若展开出子菜单,点第一个子项
    const subFirst = page.locator('.el-menu--inline .el-menu-item, .el-sub-menu.is-opened .el-menu-item').first()
    if (await subFirst.count()) { await subFirst.click().catch(() => {}) }
    const info = await dumpPage(name)
    await page.screenshot({ path: `${OUT}/page-${name}.png` })
    results.push(info)
    console.log(`\n== [${name}] active=${info.menuActive} url=${info.url}`)
    console.log('  标题:', info.H.join(' | '))
    console.log('  tabs:', info.tabs.join(' | '))
    console.log('  按钮:', info.btns.join(' | '))
    console.log('  表头:', info.th.join(' | '))
  } else {
    console.log(`\n== [${name}] 菜单未找到`)
  }
}
fs.writeFileSync(`${OUT}/pages.json`, JSON.stringify(results, null, 2))
await browser.close()
console.log('\n探查完成,产物在', OUT)
