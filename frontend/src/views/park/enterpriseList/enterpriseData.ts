// 企业详情类型定义 + 后端数据 → 前端详情结构转换
// 数据来源：liqi_enterprise 企业库（后端 /liqi/enterprise/{page,get}），不再包含 mock 示例。

export interface EnterpriseContact {
  phone: string
  name: string
  tags: string[]
  status: string
  star: number
  platform: string
  type: 'mobile' | 'tel' | 'email'
}
export interface FinancingRecord {
  id: number | string
  rzTime: string
  rzAmt: string
  rzRound: string
  investors: string
}

export interface EnterpriseProject {
  title: string
  org: string
  region: string
  year: string
  subsidy: string
  remark?: string
  level?: string
}
export interface BasicPair {
  label: string
  value: string
}
export interface DetailSection {
  title: string
  count?: number | string
  type: 'kv' | 'table' | 'tags' | 'stat'
  maxHeight?: number
  pairs?: { label: string; value: string }[]
  columns?: { prop: string; label: string; width?: number; ellipsis?: boolean }[]
  rows?: Record<string, any>[]
  tags?: string[]
  stats?: { label: string; value: number | string; unit?: string }[]
  note?: string
}
export interface RichTab {
  name: string
  label: string
  sections: DetailSection[]
}

export interface EnterpriseDetail {
  id: string
  name: string
  logo: string
  status: string
  tags: string[]
  moreTags: number
  legalPerson: string
  regCapital: string
  creditCode: string
  industry: string
  regAddress: string
  scope: string
  entityId?: string
  enrichSource?: string
  basicLeft: BasicPair[]
  basicRight: BasicPair[]
  contactCounts: { all: number; mobile: number; tel: number; email: number }
  contacts: EnterpriseContact[]
  projectPie: { name: string; value: number }[]
  projectTrend: { years: string[]; subsidy: number[]; count: number[] }
  projectTotal: number
  projects: EnterpriseProject[]
  financing: FinancingRecord[]
  richTabs?: RichTab[]
}

/** 企业规模字典 */
const SCALE_MAP: Record<number, string> = { 1: '微型', 2: '小型', 3: '中型', 4: '大型' }
/** 回填数据源字典 */
const SOURCE_MAP: Record<string, string> = { qiyedata: '企业数据平台', tyc: '天眼查' }

/** 日期格式化 */
function fmtD(d: any): string {
  if (Array.isArray(d)) return `${d[0]}-${String(d[1]).padStart(2, '0')}-${String(d[2]).padStart(2, '0')}`
  return d || '-'
}

/** 日期时间格式化（融资时间只到日）：兼容 [y,m,d,H,i,s] 数组与字符串 */
function fmtDT(d: any): string {
  if (d === null || d === undefined || d === '') return '—'
  if (Array.isArray(d)) return `${d[0]}-${String(d[1]).padStart(2, '0')}-${String(d[2]).padStart(2, '0')}`
  // 后端 LocalDateTime 默认序列化为毫秒时间戳
  if (typeof d === 'number') {
    const dt = new Date(d)
    if (isNaN(dt.getTime())) return '—'
    return `${dt.getFullYear()}-${String(dt.getMonth() + 1).padStart(2, '0')}-${String(dt.getDate()).padStart(2, '0')}`
  }
  const s = String(d)
  const m = s.match(/^(\d{4})-(\d{2})-(\d{2})/)
  if (m) return `${m[1]}-${m[2]}-${m[3]}`
  if (/^\d{10,}$/.test(s)) return fmtDT(Number(s))
  return s
}

/** 融资金额：空值与「未披露/未透露」统一显示为占位符 */
function normalizeAmt(v: any): string {
  const s = (v == null ? '' : String(v)).trim()
  if (!s || /^(未披露|未透露|未公布|不明确|保密)$/.test(s)) return '—'
  return s
}

/** 投资方：JSON 数组字符串 → 「、」拼接的名称串 */
function parseInvestors(raw: any): string {
  if (!raw) return '—'
  if (Array.isArray(raw)) return raw.map((x: any) => x?.name).filter(Boolean).join('、') || '—'
  try {
    const arr = JSON.parse(String(raw))
    if (!Array.isArray(arr)) return '—'
    return arr.map((x: any) => x?.name).filter(Boolean).join('、') || '—'
  } catch {
    return String(raw)
  }
}

/** 从后端详情响应（{enterprise, shareholders, contacts, ips, risks, projects}）构建 EnterpriseDetail */
export function detailFromBackend(payload: any): EnterpriseDetail {
  const e = payload?.enterprise || {}
  const holders = payload?.shareholders || []
  const contacts = payload?.contacts || []
  const ips = payload?.ips || []
  const risks = payload?.risks || []
  const projects = payload?.projects || []
  const financing = payload?.financing || []

  const tagSource = e.finalShowInfo || e.tags
  const tags: string[] = (tagSource ? String(tagSource).split(/[;；]/).filter(Boolean) : []).slice(0, 6)
  const establish = fmtD(e.establishDate)
  const enriched = e.enrichStatus === 1

  // 联系方式统计
  const mobiles = contacts.filter((c: any) => c.contactType === 'mobile')
  const tels = contacts.filter((c: any) => c.contactType === 'tel')
  const emails = contacts.filter((c: any) => c.contactType === 'email')

  // 联系人列表：后端字段 contact → phone, contactType → type
  const contactList: EnterpriseContact[] = contacts.map((c: any) => ({
    phone: c.contact || '',
    name: c.name || '',
    tags: (c.tags || '').split(';').filter(Boolean),
    status: '正常',
    star: c.star || 3,
    platform: c.platform || '企业库',
    type: (c.contactType || 'mobile') as 'mobile' | 'tel' | 'email'
  }))

  // 项目列表
  const projectList: EnterpriseProject[] = projects.map((p: any) => ({
    title: p.title || '',
    org: p.org || '',
    region: p.region || '',
    year: p.declareYear || '',
    subsidy: p.subsidy || '',
    level: ''
  }))

  // 融资记录：后端 rzTime/rzAmt/rzRound/investorInfo → 前端展示结构（时间倒序由后端保证）
  const financingList: FinancingRecord[] = financing.map((r: any) => ({
    id: r.id,
    rzTime: fmtDT(r.rzTime),
    rzAmt: normalizeAmt(r.rzAmt),
    rzRound: r.rzRound || '—',
    investors: parseInvestors(r.investorInfo)
  }))

  // 构建富页签（人员投资、知识产权、经营信息、经营风险、企业发展）
  const rich = buildRichTabs(e, holders, ips, risks, tags, financingList)

  return {
    id: String(e.id ?? ''),
    name: e.enterpriseName || '-',
    logo: e.shortName || (e.enterpriseName || '企业').slice(0, 4),
    status: e.regStatus || '存续',
    tags: tags.length ? tags : (enriched ? ['存续'] : ['未回填']),
    moreTags: Math.max(0, tags.length - 4),
    legalPerson: e.legalPerson || '-',
    regCapital: e.registeredCapital || '-',
    creditCode: e.creditCode || '-',
    industry: e.industry || '-',
    regAddress: e.registerAddress || '-',
    scope: e.businessScope || '—',
    entityId: e.entityId || e.creditCode || '',
    enrichSource: e.enrichSource || '',
    basicLeft: [
      { label: '企业名称', value: e.enterpriseName || '-' },
      { label: '曾用名', value: e.formerName || '-' },
      { label: '企业英文名称', value: e.englishName || '-' },
      { label: '注册资本', value: e.registeredCapital || '-' },
      { label: '注册资本币种', value: e.regCapitalType || '-' },
      { label: '成立日期', value: establish },
      { label: '统一社会信用代码', value: e.creditCode || e.entityId || '-' },
      { label: '企业类型', value: e.companyOrgType || '-' },
      { label: '所属行业', value: e.industry || '-' },
      { label: '行业一级分类', value: e.industryLv1Name || '-' },
      { label: '行业二级分类', value: e.industryLv2Name || '-' },
      { label: '核准日期', value: e.checkDate || '-' },
      { label: '地址', value: e.registerAddress || '-' }
    ],
    basicRight: [
      { label: '工商注册号', value: e.regNumber || '-' },
      { label: '法定代表人', value: e.legalPerson || '-' },
      { label: '经营状态', value: e.regStatus || '-' },
      { label: '组织机构代码', value: e.orgNumber || '-' },
      { label: '企业规模', value: SCALE_MAP[e.companyScale] || '-' },
      { label: '参保人数', value: e.insuredCount != null ? String(e.insuredCount) : '-' },
      { label: '经营期限', value: e.opFrom || e.opTo ? `${e.opFrom || '-'} 至 ${e.opTo || '-'}` : '-' },
      { label: '登记机关', value: e.regInstitute || '-' },
      { label: '官网', value: e.website || '-' },
      { label: '邮箱', value: e.email || '-' },
      { label: '补贴总金额（元）', value: e.subsidyTotalMoney != null ? String(e.subsidyTotalMoney) : '-' },
      { label: '数据来源', value: SOURCE_MAP[e.enrichSource] || '未同步' }
    ],
    contactCounts: { all: contacts.length, mobile: mobiles.length, tel: tels.length, email: emails.length },
    contacts: contactList.slice(0, 20),
    projectPie: [],
    projectTrend: { years: [], subsidy: [], count: [] },
    projectTotal: projects.length,
    projects: projectList,
    financing: financingList,
    richTabs: rich
  }
}

/** 构建富页签（人员/投资、知产、经营信息、经营风险、企业发展）——数据全部来自后端 */
function buildRichTabs(e: any, holders: any[], ips: any[], risks: any[], tags: string[], financing: FinancingRecord[] = []): RichTab[] {
  const lp = e.legalPerson || '—'
  return [
    {
      name: 'people', label: '人员/投资信息',
      sections: [
        {
          title: '主要人员', count: 1, type: 'table',
          columns: [{ prop: 'name', label: '姓名', width: 140 }, { prop: 'post', label: '职务' }],
          rows: [{ name: lp, post: '法定代表人' }]
        },
        {
          title: '股东信息', count: holders.length, type: 'table',
          note: holders.length ? '' : '暂无数据',
          columns: [{ prop: 'name', label: '股东名称' }, { prop: 'ratio', label: '持股比例', width: 140 }],
          rows: holders.length ? holders.map((h: any) => ({ name: h.name, ratio: h.percent || '—' })) : []
        },
        {
          title: '对外投资', count: 0, type: 'table', note: '暂无数据',
          columns: [{ prop: 'name', label: '被投资企业' }, { prop: 'ratio', label: '出资比例', width: 120 }], rows: []
        }
      ]
    },
    {
      name: 'ip', label: '知识产权信息',
      sections: [
        {
          title: '知识产权概览', type: 'stat',
          stats: [
            { label: '专利', value: ips.filter((i: any) => i.ipType === 'patent').length || '—', unit: '件' },
            { label: '商标', value: ips.filter((i: any) => i.ipType === 'trademark').length || '—', unit: '件' },
            { label: '软件著作权', value: '—', unit: '件' },
            { label: '作品著作权', value: '—', unit: '件' }
          ]
        },
        {
          title: '专利', type: 'table', note: ips.length ? '' : '暂无数据',
          columns: [{ prop: 'name', label: '专利名称' }, { prop: 'type', label: '类型', width: 120 }, { prop: 'status', label: '法律状态', width: 100 }],
          rows: ips.filter((i: any) => i.ipType === 'patent').map((i: any) => ({ name: i.name, type: i.category || '—', status: i.status || '—' }))
        }
      ]
    },
    {
      name: 'operate', label: '经营信息',
      sections: [
        { title: '纳税信用', type: 'kv', pairs: [{ label: '纳税人资质', value: '—' }, { label: '纳税信用等级', value: '—' }] },
        { title: '招投标', type: 'table', note: '暂无数据', columns: [{ prop: 'title', label: '项目名称' }, { prop: 'role', label: '身份', width: 90 }], rows: [] }
      ]
    },
    {
      name: 'risk', label: '经营风险',
      sections: [
        {
          title: '风险概览', type: 'stat',
          stats: [
            { label: '裁判文书', value: risks.filter((r: any) => r.riskType === 'judgment').length || '—' },
            { label: '被执行人', value: '—' },
            { label: '限制高消费', value: '—' },
            { label: '经营异常', value: risks.filter((r: any) => r.riskType === 'abnormal').length || '—' },
            { label: '行政处罚', value: risks.filter((r: any) => r.riskType === 'punish').length || '—' }
          ]
        },
        {
          title: '裁判文书', type: 'table', note: '暂无数据',
          columns: [{ prop: 'cause', label: '案由' }, { prop: 'role', label: '身份', width: 100 }, { prop: 'date', label: '裁判日期', width: 120 }], rows: []
        }
      ]
    },
    {
      name: 'growth', label: '企业发展',
      sections: [
        { title: '企业标签', type: 'tags', tags: tags.length ? tags : ['存续'] },
        {
          title: '融资概览', type: 'stat',
          stats: [
            { label: '融资次数', value: financing.length || '—', unit: financing.length ? '次' : '' },
            { label: '最新轮次', value: financing.length ? financing[0].rzRound : '—' },
            { label: '最新融资金额', value: financing.length ? financing[0].rzAmt : '—' },
            { label: '最新融资时间', value: financing.length ? financing[0].rzTime : '—' }
          ]
        },
        {
          title: '融资历程', count: financing.length, type: 'table',
          maxHeight: 240,
          note: financing.length ? '' : '暂无融资数据',
          columns: [
            { prop: 'rzTime', label: '融资时间', width: 120 },
            { prop: 'rzRound', label: '融资轮次', width: 120 },
            { prop: 'rzAmt', label: '融资金额', width: 160 },
            { prop: 'investors', label: '投资方', ellipsis: true }
          ],
          rows: financing
        },
        { title: '企业新闻', type: 'table', note: '暂无数据', columns: [{ prop: 'title', label: '标题' }, { prop: 'date', label: '日期', width: 130 }], rows: [] }
      ]
    }
  ]
}

/** 从后端企业列表行（/liqi/enterprise/page 返回）构造详情（降级方案：仅用主表字段，无子表） */
export function detailFromParkEntity(e: any): EnterpriseDetail {
  return detailFromBackend({ enterprise: e, shareholders: [], contacts: [], ips: [], risks: [], projects: [], financing: [] })
}
