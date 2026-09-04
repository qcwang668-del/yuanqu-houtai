// 通用「配音-操作同步」引擎:逐页真实操作 + 可见光标 + 男声配音 + 字幕 → 培训视频片段,最后可拼接
// 用法:
//   node make-video.mjs 0 12      生成 scenes 中第 0..12 个页面的 clip
//   node make-video.mjs all       生成全部 clip
//   node make-video.mjs concat    把 clips/*.mp4 按序拼成 全量培训视频.mp4
import { chromium } from 'playwright'
import { execSync, spawnSync } from 'node:child_process'
import crypto from 'node:crypto'
import fs from 'node:fs'
import { PAGES } from './video-scenes.mjs'

const BASE = 'http://127.0.0.1:5180'   // 力企云前端(GPU 本地)
// GPU 可直连微软 TTS,无需代理;仅当探测到 mihomo 9090 才用其端口
let PROXY = ''
try {
  const cfg = JSON.parse(execSync('curl -s --max-time 2 http://127.0.0.1:9090/configs', { encoding: 'utf8' }))
  if (cfg['mixed-port']) PROXY = `http://127.0.0.1:${cfg['mixed-port']}`
} catch {}
const VOICE = 'zh-CN-XiaoxiaoNeural'   // 女声·晓晓
const STORAGE = 'auth.json'            // 真实 UI 登录后保存的 storageState
const FF = process.cwd() + '/bin/ffmpeg'
const FP = process.cwd() + '/bin/ffprobe'
const ROOT = 'evidence/video'
const CLIPS = ROOT + '/clips'
const TTS = ROOT + '/tts'
fs.mkdirSync(CLIPS, { recursive: true })
fs.mkdirSync(TTS, { recursive: true })

// CJK 字体(用于烧录字幕);找不到就不烧字幕
const FONT = [process.cwd() + '/assets/fonts/SimHei.ttf', '/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc', '/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc']
  .find(p => fs.existsSync(p))
const FONT_NAME = FONT && FONT.includes('SimHei') ? 'SimHei' : 'sans'

const dur = f => parseFloat((spawnSync(FP, ['-v', 'error', '-show_entries', 'format=duration', '-of', 'default=noprint_wrappers=1:nokey=1', f], { encoding: 'utf8' }).stdout || '0').trim()) || 2

const tts = (text) => {
  const key = crypto.createHash('md5').update(VOICE + '|' + text).digest('hex').slice(0, 16)
  const f = `${TTS}/${key}.mp3`
  if (!fs.existsSync(f) || fs.statSync(f).size < 500) {
    execSync(`python3 -m edge_tts ${PROXY ? '--proxy ' + PROXY : ''} --voice ${VOICE} --rate=+8% --text ${JSON.stringify(text)} --write-media ${f}`, { stdio: 'ignore' })
    if (!fs.existsSync(f) || fs.statSync(f).size < 500) throw new Error('TTS 生成失败(空文件): ' + text.slice(0, 20))
  }
  return { f, d: dur(f) }
}

const srtTime = s => {
  const h = Math.floor(s / 3600), m = Math.floor(s % 3600 / 60), sec = Math.floor(s % 60), ms = Math.round((s - Math.floor(s)) * 1000)
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')},${String(ms).padStart(3, '0')}`
}

// 字幕强制单行(WrapStyle=2)下超长口播会左右溢出、两头看不见。
// 把一句按 maxChars 贪心切成多个单行片段:接近上限时优先回退到最近标点断句,
// 否则硬切;返回单行片段数组(供逐段"滚动"显示)。
const PUNCTS = '，。、；：！？,.;:!?…—～'
// 英文单词/数字字符:切分片段时禁止从这类字符中间断开(否则一个英文单词会被拆到两段字幕、跨行显示)。
const isWordChar = c => c != null && /[A-Za-z0-9]/.test(c)
// 字幕显示去掉首尾无效标点(尤其结尾句读),音频口播不受影响(读原句)。
const stripPunct = x => (x || '').replace(/^[，。、；：！？,.;:!?…—～·・\s]+|[，。、；：！？,.;:!?…—～·・\s]+$/g, '')
const splitSubtitle = (text, maxChars = 20) => {
  const segs = []
  let s = (text || '').trim()
  while (s.length > maxChars) {
    let cut = maxChars
    // 1) 优先在 [0.5max, max] 窗口内回退到标点之后断句(中英文标点都算)
    let p = -1
    for (let i = Math.min(maxChars, s.length - 1); i >= Math.floor(maxChars * 0.5); i--) {
      if (PUNCTS.includes(s[i])) { p = i + 1; break }
    }
    if (p > 0) {
      cut = p
    } else if (isWordChar(s[cut - 1]) && isWordChar(s[cut])) {
      // 2) 无标点且切点正好落在英文单词/数字内部:回退到最近的空格或连字符边界断开,保证整词不跨行
      //    (连字符 nginx-ingress 在其处断行是正常的英文断词);整词超长且无边界时才硬切(极少见)。
      const floor = Math.floor(maxChars * 0.4)
      let j = cut
      while (j > floor && s[j - 1] !== ' ' && s[j - 1] !== '-') j--
      if (j > floor) cut = j
    }
    segs.push(stripPunct(s.slice(0, cut)))
    s = s.slice(cut).trim()
  }
  if (s) segs.push(stripPunct(s))
  return segs.filter(Boolean)
}

// 把一句(起点 clock、配音时长 d)按片段字符占比分摊时长,末段吸附句尾消除漂移;
// 收集多条 cue 对象({start,end,text})进 cues,返回该句结束时钟(=clock+d)。
const pushCues = (cues, say, clock, d, maxChars) => {
  const segs = splitSubtitle(say, maxChars)
  const total = segs.reduce((a, x) => a + x.length, 0) || 1
  let acc = clock
  segs.forEach((seg, k) => {
    const end = (k === segs.length - 1) ? clock + d : acc + d * seg.length / total
    cues.push({ start: acc, end, text: seg })
    acc = end
  })
  return clock + d
}

// 字幕提前量(秒):让每条字幕比其配音提前 LEAD 出现——"第一个字说出来时字幕已在屏上",
// 而不是说到一半才更新。做法:所有 cue 起点整体前移 LEAD,并让上一条在下一条出现的瞬间收尾
// (无缝切换、不重叠、不留空档);首条已在 0 处不再前移。cues 原本连续递增,前移后仍保持有序。
const leadSubtitles = (cues, lead) => {
  if (!lead || cues.length === 0) return cues
  for (const c of cues) c.start = Math.max(0, c.start - lead)
  for (let k = 0; k < cues.length - 1; k++) {
    if (cues[k].end > cues[k + 1].start) cues[k].end = cues[k + 1].start
  }
  return cues
}

const CURSOR_JS = () => {
  if (document.getElementById('__cursor')) return
  const c = document.createElement('div')
  c.id = '__cursor'
  c.style.cssText = 'position:fixed;left:0;top:0;z-index:2147483647;pointer-events:none;width:28px;height:28px;transition:left .05s linear,top .05s linear;filter:drop-shadow(0 1px 2px rgba(0,0,0,.4))'
  c.innerHTML = '<svg width="28" height="28" viewBox="0 0 28 28"><path d="M3 3 L3 22 L8 17 L12 25 L15.5 23.5 L11.5 15.5 L19 15.5 Z" fill="#111" stroke="#fff" stroke-width="1.6"/></svg>'
  document.body.appendChild(c)
  window.__mc = (x, y) => { const el = document.getElementById('__cursor'); if (el) { el.style.left = x + 'px'; el.style.top = y + 'px' } }
  document.addEventListener('mousemove', e => window.__mc(e.clientX, e.clientY), true)
  window.__ripple = (x, y) => {
    const d = document.createElement('div')
    d.style.cssText = `position:fixed;left:${x - 20}px;top:${y - 20}px;width:40px;height:40px;border:3px solid #e6483d;border-radius:50%;z-index:2147483646;pointer-events:none;opacity:.9;transition:all .5s ease-out`
    document.body.appendChild(d)
    requestAnimationFrame(() => { d.style.transform = 'scale(1.9)'; d.style.opacity = '0' })
    setTimeout(() => d.remove(), 560)
  }
}

async function genClip(idx, cfg, browser) {
  const tag = `${String(idx).padStart(2, '0')}_${cfg.name}`
  const tCtx = Date.now() // 录像大约从建上下文开始
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 810 }, storageState: STORAGE, recordVideo: { dir: ROOT + '/raw', size: { width: 1440, height: 810 } } })
  const page = await ctx.newPage()
  await page.goto(BASE + cfg.path, { waitUntil: 'networkidle', timeout: 30000 }).catch(() => {})
  await page.waitForTimeout(1400)

  const ensureCursor = async () => { await page.evaluate(CURSOR_JS).catch(() => {}) }
  await ensureCursor()

  // 主框架 + 所有 iframe。很多系统的助手/AI 面板、聊天输入框、推荐问题都渲染在 iframe 内,
  // 只搜主框架会全部命不中(实战踩坑:蓝凌助手面板在 ai-lanbots iframe 里)。
  // iframe 元素的 boundingBox() 返回主视口坐标,鼠标可直接点。
  const framesAll = () => [page.mainFrame(), ...page.frames().filter(f => f !== page.mainFrame())]
  const box = async (text) => {
    // 先按按钮/标签页角色定位,再退化到文本;跨 frame、逐个取第一个视口内可见框
    for (const fr of framesAll()) {
      const cands = [
        fr.getByRole('button', { name: text, exact: false }),
        fr.getByRole('tab', { name: text, exact: false }),
        fr.getByText(text, { exact: false }),
      ]
      for (const loc of cands) {
        const n = await loc.count().catch(() => 0)
        for (let i = 0; i < Math.min(n, 6); i++) {
          let b = await loc.nth(i).boundingBox().catch(() => null)
          if (!b || b.width <= 0 || b.height <= 0) continue
          if (b.y < 0 || b.y > 806) { // 在首屏外:滚动到可见再取框
            await loc.nth(i).scrollIntoViewIfNeeded({ timeout: 1200 }).catch(() => {})
            await page.waitForTimeout(250)
            b = await loc.nth(i).boundingBox().catch(() => null)
            if (!b) continue
          }
          if (b.width > 0 && b.height > 0 && b.y >= 0 && b.y <= 806) return b
        }
      }
    }
    return null
  }
  const moveTo = async (x, y) => { await page.mouse.move(x, y, { steps: 22 }); await page.waitForTimeout(150) }
  const toText = async (t) => {
    let b = await box(t)
    if (!b) { await page.waitForTimeout(400); b = await box(t) }
    if (b) await moveTo(Math.round(b.x + Math.min(b.width, 140) / 2), Math.round(b.y + b.height / 2))
    else console.warn(`   ⚠️未命中: ${t}`)
    return !!b
  }
  const clickText = async (t) => {
    const b = await box(t); if (!b) return false
    const x = Math.round(b.x + Math.min(b.width, 140) / 2), y = Math.round(b.y + b.height / 2)
    await moveTo(x, y); await page.evaluate(([x, y]) => window.__ripple(x, y), [x, y]).catch(() => {})
    await page.mouse.click(x, y); await page.waitForTimeout(600); await ensureCursor(); return true
  }
  // 校验面板/子页确实打开:优先看输入框 placeholder(占位符是属性、getByText 找不到,须用选择器);
  // 也可传 waitFor=子页里的可见文本。用于"先确定打开了子页面/面板,再做后续操作"。
  const panelReady = async (waitFor, ms = 7000) => {
    const end = Date.now() + ms
    while (Date.now() < end) {
      for (const fr of framesAll()) {   // 面板常在 iframe 内,须跨 frame 检测
        try {
          const byInput = await fr.locator('[placeholder*="请输入"], textarea, [contenteditable="true"]').count().catch(() => 0)
          const byText = (waitFor && waitFor !== '请输入内容')
            ? await fr.getByText(waitFor, { exact: false }).count().catch(() => 0) : 0
          if (byInput > 0 || byText > 0) return true
        } catch {}
      }
      await page.waitForTimeout(300)
    }
    return false
  }
  // 点开卡片/菜单并校验面板/子页出现。关键:单次点击即可打开,禁止"重试再点"——
  // 弹出的面板会盖住部分卡片,二次点击未被盖住的卡片会把面板 toggle 关闭(实战踩坑)。
  const openCard = async (t, waitFor) => {
    await clickText(t)
    if (!(await panelReady(waitFor))) console.warn(`   ⚠️面板/子页未确认打开(可能仅检测失败): ${t}`)
    await ensureCursor()
  }

  // 停顿把光标停在中间,作为对齐锚点;记录 scene1 开始的偏移(=页面加载耗时)
  await moveTo(720, 320)
  const tStart = Date.now()
  const offset = Math.max(0, (tStart - tCtx) / 1000 - 0.3) // 视频开头需裁掉的加载段

  // 逐场景:配音 + 动作 + 停留=配音时长(不加垫时,避免累积漂移);记录字幕时间轴
  const cues = []                               // 字幕 cue 对象数组,循环后统一前移再序列化
  const maxChars = cfg.subtitle?.maxChars || 20 // 单行上限,可按模块覆盖
  const lead = cfg.subtitle?.lead ?? 0.3        // 字幕提前量(秒),可按模块覆盖
  const segStarts = []   // 每句动作真正开始时的视频时刻(秒);音频与字幕都按它对齐,消除漂移
  for (let i = 0; i < cfg.scenes.length; i++) {
    const sc = cfg.scenes[i]
    const { f, d } = tts(sc.say)
    sc._f = f; sc._d = d
    const t0 = Date.now()
    segStarts.push(Math.max(0, (t0 - tCtx) / 1000 - offset))  // 该句在裁剪后视频里的起始秒
    try {
      if (sc.open) await openCard(sc.open, sc.waitFor)          // 点开面板/子页并校验打开
      else if (sc.ask) { await clickText(sc.ask); await page.waitForTimeout(sc.wait || 9000); await ensureCursor() } // 点推荐问题真实提问,等 AI 出答(可剪辑跳过)
      else if (sc.click) await clickText(sc.click)
      else if (sc.tab) await clickText(sc.tab)
      else if (sc.sweep) {
        const per = Math.max(650, (d * 1000 - 400) / sc.sweep.length)
        for (const t of sc.sweep) { const st = Date.now(); try { await toText(t) } catch {} await page.waitForTimeout(Math.max(0, per - (Date.now() - st))) }
      }
      else if (sc.point) await toText(sc.point)
      else await moveTo(720, 320)
    } catch {}
    const spent = (Date.now() - t0) / 1000
    await page.waitForTimeout(Math.max(0, (d - spent)) * 1000)
  }
  // 字幕按每句真实起始时刻(segStart)排布,与配音同起,再拆成多段单行滚动
  for (let i = 0; i < cfg.scenes.length; i++) pushCues(cues, cfg.scenes[i].say, segStarts[i], cfg.scenes[i]._d, maxChars)
  leadSubtitles(cues, lead)                                    // 整体前移,让字幕先于配音出现
  const srt = cues.map((c, i) => `${i + 1}\n${srtTime(c.start)} --> ${srtTime(c.end)}\n${c.text}\n`)
  await page.waitForTimeout(300)
  const vid = page.video()
  await ctx.close()
  const webm = await vid.path()

  // 音频对齐合成:每句配音延迟到该句动作开始的视频时刻(segStart)再混成一条音轨——
  // 消除"操作耗时>配音时长"导致的逐句漂移,做到讲到哪=鼠标指到哪。
  const allMp3 = `${TTS}/${tag}.mp3`
  const delays = segStarts.map(s => Math.max(0, Math.round(s * 1000)))
  const inArgs = cfg.scenes.map(s => `-i "${process.cwd()}/${s._f}"`).join(' ')
  const fc = cfg.scenes.map((s, i) => `[${i}:a]adelay=${delays[i]}|${delays[i]}[a${i}]`).join(';')
  const mix = cfg.scenes.map((s, i) => `[a${i}]`).join('') + `amix=inputs=${cfg.scenes.length}:normalize=0:dropout_transition=0[a]`
  execSync(`${FF} -y ${inArgs} -filter_complex "${fc};${mix}" -map "[a]" -ac 2 ${allMp3}`, { stdio: 'ignore' })

  // 字幕(可选烧录)
  let vf = ''
  if (FONT) {
    const srtF = `${TTS}/${tag}.srt`
    fs.writeFileSync(srtF, srt.join('\n'))
    const fontsDir = FONT.substring(0, FONT.lastIndexOf('/'))
    vf = `-vf "subtitles=${srtF}:force_style='FontName=${FONT_NAME},FontSize=22,PrimaryColour=&H00FFFFFF,OutlineColour=&HC0000000,BorderStyle=1,Outline=2,Shadow=1,MarginV=30,WrapStyle=2':fontsdir=${fontsDir}"`
  }
  const clip = `${CLIPS}/${tag}.mp4`
  execSync(`${FF} -y -ss ${offset.toFixed(2)} -i "${webm}" -i ${allMp3} ${vf} -map 0:v:0 -map 1:a:0 -c:v libx264 -pix_fmt yuv420p -r 24 -c:a aac -shortest "${clip}"`, { stdio: 'ignore' })
  const secs = cfg.scenes.reduce((a, s) => a + s._d + 0.45, 0)
  console.log(`✅ ${tag}  ~${secs.toFixed(0)}s  ${(fs.statSync(clip).size / 1024 / 1024).toFixed(1)}MB`)
}

async function main() {
  const arg = process.argv[2] || 'all'
  if (arg === 'concat') {
    const files = fs.readdirSync(CLIPS).filter(f => f.endsWith('.mp4')).sort()
    const list = `${ROOT}/concat.txt`
    fs.writeFileSync(list, files.map(f => `file '${process.cwd()}/${CLIPS}/${f}'`).join('\n'))
    const out = `${ROOT}/全量培训视频.mp4`
    execSync(`${FF} -y -f concat -safe 0 -i ${list} -c copy "${out}"`, { stdio: 'ignore' })
    console.log('✅ 已拼接', files.length, '段 →', out, (fs.statSync(out).size / 1024 / 1024).toFixed(1) + 'MB', '时长', dur(out).toFixed(0) + 's')
    return
  }
  let s = 0, e = PAGES.length - 1
  if (arg !== 'all') { s = parseInt(arg); e = parseInt(process.argv[3] ?? arg) }
  const browser = await chromium.launch({ headless: true, args: ['--no-sandbox', '--disable-dev-shm-usage'] })
  for (let i = s; i <= e && i < PAGES.length; i++) {
    try { await genClip(i, PAGES[i], browser) } catch (err) { console.error(`❌ ${i} ${PAGES[i].name}:`, err.message) }
  }
  await browser.close()
}
main().catch(e => { console.error('FATAL', e); process.exit(1) })
