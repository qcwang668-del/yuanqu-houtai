// 扫描 clips 目录,自动生成预览网页 index.html(与 mp4 同目录,相对路径引用)
import { execSync } from 'node:child_process'
import fs from 'node:fs'
const DIR = 'evidence/video/clips'
const FP = process.cwd() + '/bin/ffprobe'
const TOTAL = 27 // 规划总段数

const files = fs.readdirSync(DIR).filter(f => /^\d\d_.*\.mp4$/.test(f)).sort()
const items = files.map(f => {
  let d = 0
  try { d = parseFloat(execSync(`${FP} -v error -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 "${DIR}/${f}"`, { encoding: 'utf8' }).trim()) || 0 } catch {}
  const m = f.match(/^(\d\d)_(.*)\.mp4$/)
  return { no: m[1], name: m[2], file: f, dur: d }
})
const pct = Math.round(items.length / TOTAL * 100)
const cards = items.map(it => `
  <div class="card">
    <video controls preload="metadata" src="${encodeURIComponent(it.file)}"></video>
    <div class="cap"><span class="du">${Math.floor(it.dur / 60)}:${String(Math.round(it.dur % 60)).padStart(2, '0')}</span><span class="no">${it.no}</span><span class="nm">${it.name}</span></div>
  </div>`).join('')

const html = `<!DOCTYPE html><html lang="zh-CN"><head><meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>派任董事履职管理系统 · 操作培训视频(片段预览)</title>
<style>
:root{--brand:#1a5fb4;--brand2:#2b7de9;--ink:#233240;--sub:#6b7a89;--line:#e6ecf3;--bg:#f4f7fb;--ok:#0d7d5a}
*{box-sizing:border-box}body{margin:0;font-family:-apple-system,"PingFang SC","Microsoft YaHei",sans-serif;background:var(--bg);color:var(--ink)}
.wrap{max-width:1100px;margin:0 auto;padding:26px 18px 80px}
header{background:linear-gradient(135deg,var(--brand),var(--brand2));color:#fff;border-radius:16px;padding:24px 30px;box-shadow:0 10px 28px rgba(26,95,180,.22)}
header h1{margin:0 0 8px;font-size:22px}header p{margin:3px 0;opacity:.94;font-size:14px}
.chips{display:flex;flex-wrap:wrap;gap:8px;margin-top:12px}
.chip{background:rgba(255,255,255,.18);border:1px solid rgba(255,255,255,.28);padding:4px 12px;border-radius:999px;font-size:12.5px}
.bar{display:flex;gap:14px;align-items:center;margin:18px 2px 6px;flex-wrap:wrap}
.prog{flex:1;min-width:220px;height:10px;background:#e3e9f0;border-radius:99px;overflow:hidden}
.prog>i{display:block;height:100%;width:${pct}%;background:linear-gradient(90deg,var(--ok),#3bb088)}
.bar b{color:var(--brand)}
.note{background:#fff6e6;border:1px solid #ffe0a8;color:#8a5a00;border-radius:10px;padding:10px 14px;font-size:13px;margin:10px 0}
.grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(320px,1fr));gap:18px;margin-top:16px}
.card{background:#fff;border:1px solid var(--line);border-radius:14px;overflow:hidden;box-shadow:0 2px 12px rgba(31,41,51,.05)}
.card video{width:100%;display:block;background:#000;aspect-ratio:16/9}
.cap{padding:11px 14px}
.cap .no{display:inline-block;font-size:12px;font-weight:700;color:#fff;background:var(--brand);border-radius:5px;padding:1px 8px;margin-right:8px}
.cap .nm{font-weight:700;font-size:15px}.cap .du{float:right;font-size:12px;color:var(--sub)}
footer{text-align:center;color:#9aa7b4;font-size:12.5px;margin-top:40px}
</style></head><body><div class="wrap">
<header><h1>派任董事履职管理系统 · 操作培训视频(片段预览)</h1>
<p>男声解说 · 可见鼠标演示 · 口播与操作逐句同步 · 内容全部取自真实页面</p>
<div class="chips"><span class="chip">🎬 已生成 ${items.length} 段</span><span class="chip">🗂️ 规划共 ${TOTAL} 段</span><span class="chip">🔊 男声·云健</span><span class="chip">🖱️ 鼠标高亮+点击涟漪</span></div></header>
<div class="bar"><b>进度 ${items.length} / ${TOTAL}</b><div class="prog"><i></i></div><span style="font-size:12.5px;color:var(--sub)">逐段生成中,生成一段即刷新本页</span></div>
<div class="note">⚠️ 阶段性预览,请重点看:①鼠标是否清晰 ②讲解与画面操作是否对得上 ③口播与页面是否一致 ④男声语速节奏。有要调整的现在提,我统一应用,避免返工。</div>
<div class="grid">${cards}</div>
<footer>本页与视频同目录 · 双击 index.html 播放 · AI 自动生成的培训素材</footer>
</div></body></html>`

fs.writeFileSync(`${DIR}/index.html`, html)
console.log(`预览网页已更新: ${DIR}/index.html  (${items.length}/${TOTAL} 段)`)
items.forEach(it => console.log(`  ${it.no} ${it.name}  ${it.dur.toFixed(0)}s`))
