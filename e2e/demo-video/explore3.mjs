import { chromium } from 'playwright'
import fs from 'node:fs'
const BASE = 'http://127.0.0.1:5180'
const OUT = 'explore-out'
const browser = await chromium.launch({ headless: true, args: ['--no-sandbox', '--disable-dev-shm-usage'] })
const ctx = await browser.newContext({ viewport: { width: 1440, height: 810 }, storageState: 'auth.json' })
const page = await ctx.newPage()
const dump = async () => page.evaluate(() => {
  const uniq = a => [...new Set(a.map(s => (s || '').trim()).filter(Boolean))]
  return {
    hash: location.pathname,
    active: document.querySelector('.el-menu-item.is-active')?.innerText?.trim() || '',
    btns: uniq([...document.querySelectorAll('button')].map(e => (e.innerText || '').trim())).filter(x => x && x.length <= 12).slice(0, 30),
    th: uniq([...document.querySelectorAll('.el-table__header th .cell')].map(e => e.innerText)).slice(0, 24),
    labels: uniq([...document.querySelectorAll('.el-form-item__label')].map(e => e.innerText)).slice(0, 24),
    cards: uniq([...document.querySelectorAll('.el-card__header, .card-header, h3, .panel-title')].map(e => e.innerText)).slice(0, 12),
  }
})
const paths = [
  ['首页', '/index'],
  ['平台-用户管理', '/platformManage/clientUser'],
  ['平台-匹配线索', '/platformManage/matchClues'],
  ['企业-会员线索', '/enterpriseManage/memberCluesMg'],
  ['企业-获批动态', '/enterpriseManage/approvalDynamics'],
]
const out = []
for (const [key, p] of paths) {
  await page.goto(BASE + p, { waitUntil: 'domcontentloaded' }).catch(() => {})
  await page.waitForTimeout(2200)
  const info = await dump(); info.key = key
  out.push(info)
  await page.screenshot({ path: `${OUT}/v3-${key}.png` })
  console.log(`\n== [${key}] active=${info.active} path=${info.hash}`)
  console.log('  卡片:', info.cards.join(' | '))
  console.log('  按钮:', info.btns.join(' | '))
  console.log('  表头:', info.th.join(' | '))
  console.log('  标签:', info.labels.join(' | '))
}
fs.writeFileSync(`${OUT}/pages3.json`, JSON.stringify(out, null, 2))
await browser.close()
console.log('\n done')
