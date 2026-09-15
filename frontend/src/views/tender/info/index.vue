<template>
  <!-- 搜索 -->
  <ContentWrap>
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="90px">
      <el-form-item label="关键词" prop="keyword">
        <div class="keyword-group">
          <el-select v-model="queryParams.scope" class="!w-140px">
            <el-option label="全文" value="full" />
            <el-option label="标题" value="title" />
            <el-option label="项目名称/标的物" value="product" />
          </el-select>
          <el-input v-model="queryParams.keyword" placeholder="请输入项目名称等关键词" clearable class="!w-240px" @keyup.enter="handleQuery" />
        </div>
      </el-form-item>
      <el-form-item label="我的订阅" prop="subscriptionId">
        <el-select v-model="queryParams.subscriptionId" placeholder="不按订阅过滤" clearable class="!w-180px" @change="handleQuery">
          <el-option v-for="s in SUBSCRIPTIONS" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" />搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" />重置</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 筛选 -->
  <ContentWrap>
    <div class="filter-row">
      <span class="filter-label">信息类型</span>
      <el-radio-group v-model="queryParams.type" @change="handleQuery">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button v-for="t in NOTICE_TYPES" :key="t" :value="t">{{ t }}</el-radio-button>
      </el-radio-group>
    </div>
    <div class="filter-row">
      <span class="filter-label">业务地区</span>
      <el-select v-model="queryParams.province" placeholder="全国" clearable class="!w-140px" @change="onProvinceChange">
        <el-option v-for="p in PROVINCES" :key="p.name" :label="p.name" :value="p.name" />
      </el-select>
      <el-select v-model="queryParams.city" placeholder="全省" clearable class="!w-140px ml-10px" :disabled="!queryParams.province" @change="handleQuery">
        <el-option v-for="c in cityOptions" :key="c" :label="c" :value="c" />
      </el-select>
    </div>
    <div class="filter-row">
      <span class="filter-label">行业分类</span>
      <el-select v-model="queryParams.industry" placeholder="全部行业" clearable class="!w-160px" @change="handleQuery">
        <el-option v-for="i in INDUSTRIES" :key="i" :label="i" :value="i" />
      </el-select>
      <span class="filter-label ml-24px">采购单位类型</span>
      <el-select v-model="queryParams.buyerType" placeholder="不限" clearable class="!w-160px" @change="handleQuery">
        <el-option v-for="b in BUYER_TYPES" :key="b" :label="b" :value="b" />
      </el-select>
    </div>
    <div class="filter-row">
      <span class="filter-label">发布时间</span>
      <el-radio-group v-model="queryParams.publishRange" @change="handleQuery">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="today">今天</el-radio-button>
        <el-radio-button value="7d">近7天</el-radio-button>
        <el-radio-button value="30d">近30天</el-radio-button>
      </el-radio-group>
      <el-date-picker
        v-model="customRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        class="ml-10px" style="width: 260px"
        @change="onCustomRange"
      />
    </div>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true">
      <el-table-column label="序号" type="index" align="center" width="60" />
      <el-table-column label="标题" min-width="320">
        <template #default="s">
          <el-link type="primary" class="!text-left" @click="openDetail(s.row)">{{ s.row.title }}</el-link>
          <div class="project-no">{{ s.row.projectNo }}</div>
        </template>
      </el-table-column>
      <el-table-column label="信息类型" align="center" width="110">
        <template #default="s">
          <el-tag :type="typeTagType(s.row.type)">{{ s.row.type }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="地区" align="center" width="120">
        <template #default="s">{{ s.row.province }}-{{ s.row.city }}</template>
      </el-table-column>
      <el-table-column label="行业分类" prop="industry" align="center" width="110" />
      <el-table-column label="采购单位" prop="buyer" min-width="200" show-overflow-tooltip />
      <el-table-column label="预算金额(万元)" align="center" width="120">
        <template #default="s">{{ s.row.budget ?? '—' }}</template>
      </el-table-column>
      <el-table-column label="发布时间" prop="publishTime" align="center" width="160" />
      <el-table-column label="操作" align="center" width="220" fixed="right">
        <template #default="s">
          <el-button link type="primary" @click="openDetail(s.row)">详情</el-button>
          <el-button link :type="s.row.favorited ? 'warning' : 'info'" @click="toggleFavorite(s.row)">
            <Icon :icon="s.row.favorited ? 'ep:star-filled' : 'ep:star'" class="mr-2px" />{{ s.row.favorited ? '已收藏' : '收藏' }}
          </el-button>
          <el-button link type="success" @click="pushToEnterprise(s.row)">推送企业</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>

  <!-- 详情抽屉 -->
  <el-drawer v-model="detailVisible" size="720px" :with-header="false" destroy-on-close>
    <div v-if="detail" class="detail-wrap">
      <!-- 头部 -->
      <div class="detail-header">
        <div class="detail-title">{{ detail.title }}</div>
        <div class="detail-tags">
          <el-tag :type="typeTagType(detail.type)" class="mr-6px">{{ detail.type }}</el-tag>
          <el-tag type="info" class="mr-6px">{{ detail.province }}-{{ detail.city }}</el-tag>
          <el-tag type="info">{{ detail.industry }}</el-tag>
        </div>
        <div class="detail-meta">
          <span>发布时间：{{ detail.publishTime }}</span>
          <el-link type="primary" class="ml-16px" @click="openSource">查看原文<Icon icon="ep:top-right" class="ml-2px" /></el-link>
        </div>
      </div>

      <!-- 时间节点 -->
      <div class="time-cards">
        <div class="time-card">
          <div class="time-label">报名截止</div>
          <div class="time-value" :class="{ urgent: isUrgent(detail.signupDeadline) }">{{ detail.signupDeadline || '详见公告' }}</div>
        </div>
        <div class="time-card">
          <div class="time-label">开标时间</div>
          <div class="time-value">{{ detail.openTime || '详见公告' }}</div>
        </div>
        <div class="time-card">
          <div class="time-label">预算金额</div>
          <div class="time-value budget">{{ detail.budget != null ? detail.budget + ' 万元' : '详见公告' }}</div>
        </div>
      </div>

      <!-- 项目信息 -->
      <div class="detail-section">
        <div class="section-title">项目信息</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="项目编号">{{ detail.projectNo }}</el-descriptions-item>
          <el-descriptions-item label="采购单位">{{ detail.buyer }}</el-descriptions-item>
          <el-descriptions-item label="代理机构">{{ detail.agency || '—' }}</el-descriptions-item>
          <el-descriptions-item label="标的物">{{ detail.subject }}</el-descriptions-item>
          <el-descriptions-item label="联系人">{{ detail.contactPerson || '—' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ detail.contactPhone || '—' }}</el-descriptions-item>
          <el-descriptions-item label="联系地址" :span="2">{{ detail.contactAddress || '—' }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 公告正文 -->
      <div class="detail-section">
        <div class="section-title">公告正文</div>
        <div class="notice-content">
          <p v-for="(p, i) in detail.content" :key="i">{{ p }}</p>
        </div>
      </div>

      <!-- 附件 -->
      <div class="detail-section">
        <div class="section-title">附件下载</div>
        <div v-for="(f, i) in detail.attachments" :key="i" class="attachment" @click="downloadAttachment(f)">
          <Icon icon="ep:document" class="mr-6px" />
          <span class="attachment-name">{{ f.name }}</span>
          <span class="attachment-size">{{ f.size }}</span>
        </div>
        <div v-if="!detail.attachments.length" class="no-attachment">暂无附件</div>
      </div>

      <!-- 操作区 -->
      <div class="detail-footer">
        <el-button :type="detail.favorited ? 'warning' : 'default'" @click="toggleFavorite(detail)">
          <Icon :icon="detail.favorited ? 'ep:star-filled' : 'ep:star'" class="mr-4px" />{{ detail.favorited ? '已收藏' : '收藏' }}
        </el-button>
        <el-button type="success" @click="pushToEnterprise(detail)">
          <Icon icon="ep:promotion" class="mr-4px" />推送园区企业
        </el-button>
        <el-button type="primary" @click="createSubscription">
          <Icon icon="ep:bell" class="mr-4px" />按此条件生成订阅
        </el-button>
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
defineOptions({ name: 'TenderInfo' })

const message = useMessage()

const NOTICE_TYPES = ['招标公告', '招标预告', '变更公告']
const INDUSTRIES = ['建筑工程', '医疗器械', '信息化', '物业服务', '办公设备', '教育装备', '交通物流', '其他']
const BUYER_TYPES = ['政府', '学校', '医疗', '公安', '金融企业', '部队', '通信运营商']
const PROVINCES = [
  { name: '广东', cities: ['深圳市', '广州市', '东莞市', '佛山市', '珠海市'] },
  { name: '北京', cities: ['北京市'] },
  { name: '上海', cities: ['上海市'] },
  { name: '江苏', cities: ['南京市', '苏州市', '无锡市'] },
  { name: '浙江', cities: ['杭州市', '宁波市', '温州市'] },
  { name: '四川', cities: ['成都市', '绵阳市'] },
  { name: '湖北', cities: ['武汉市', '宜昌市'] },
  { name: '陕西', cities: ['西安市'] }
]

// 与「标讯订阅」演示数据保持一致，用于按订阅器过滤
const SUBSCRIPTIONS = [
  { id: 1, name: '深圳-智慧园区', keywords: ['智慧园区', '园区运营', '物业管理'], provinces: ['广东'], industries: ['信息化', '物业服务'] },
  { id: 2, name: '全国-物业保洁', keywords: ['物业', '保洁'], provinces: [] as string[], industries: ['物业服务'] }
]

const buildContent = (item: any) => [
  `${item.buyer}就「${item.title.replace(/^.*?项目/, '本项目')}」（项目编号：${item.projectNo}）进行公开${item.type === '招标预告' ? '预告' : '招标'}，欢迎符合条件的供应商参加投标。`,
  `一、项目基本情况：项目名称：${item.title}；标的物：${item.subject}；预算金额：${item.budget != null ? item.budget + '万元' : '详见招标文件'}；采购需求详见招标文件。`,
  '二、申请人的资格要求：满足《中华人民共和国政府采购法》第二十二条规定；落实采购政策需满足的资格要求：无；本项目的特定资格要求：详见招标文件。',
  `三、获取招标文件：有意向的供应商请于报名截止时间前联系${item.agency || item.buyer}获取招标文件；联系人：${item.contactPerson}；联系电话：${item.contactPhone}。`,
  `四、提交投标文件截止时间、开标时间和地点：开标时间：${item.openTime || '详见公告'}；地点：${item.contactAddress}。`,
  '五、公告期限：自本公告发布之日起5个工作日。'
]

const mock = (arr: any[]) => arr.map((item) => ({ favorited: false, ...item, content: buildContent(item) }))

// 演示数据（标讯后端接口未接入，先本地维护）
const allData = ref<any[]>(mock([
  {
    id: 1, title: '深圳湾科技生态园智慧园区运营管理系统采购项目', type: '招标公告',
    projectNo: 'SZCG2026-0901', province: '广东', city: '深圳市', industry: '信息化',
    buyer: '深圳市南山区政务服务数据管理局', buyerType: '政府', budget: 486.5,
    publishTime: '2026-09-14 09:30', signupDeadline: '2026-09-20 17:00', openTime: '2026-09-28 09:30',
    agency: '深圳市国际招标有限公司', subject: '智慧园区运营管理系统一套',
    contactPerson: '李工', contactPhone: '0755-86001122', contactAddress: '深圳市南山区科苑南路2666号',
    attachments: [{ name: '招标文件-SZCG2026-0901.pdf', size: '2.4MB' }, { name: '技术需求书.docx', size: '860KB' }],
    sourceUrl: 'https://www.ccgp.gov.cn/'
  },
  {
    id: 2, title: '前海深港现代服务业合作区园区物业服务项目', type: '招标公告',
    projectNo: 'QH-ZB-2026-118', province: '广东', city: '深圳市', industry: '物业服务',
    buyer: '深圳市前海深港现代服务业合作区管理局', buyerType: '政府', budget: 1280,
    publishTime: '2026-09-13 14:20', signupDeadline: '2026-09-19 18:00', openTime: '2026-09-25 10:00',
    agency: '深圳市建星项目管理顾问有限公司', subject: '园区综合物业服务（三年期）',
    contactPerson: '张小姐', contactPhone: '0755-88990011', contactAddress: '深圳市前海合作区桂湾三路',
    attachments: [{ name: '招标公告.pdf', size: '1.1MB' }],
    sourceUrl: 'https://www.ccgp.gov.cn/'
  },
  {
    id: 3, title: '深圳市人民医院医疗设备维保服务采购项目', type: '招标公告',
    projectNo: 'SZRM-2026-WB-033', province: '广东', city: '深圳市', industry: '医疗器械',
    buyer: '深圳市人民医院', buyerType: '医疗', budget: 356,
    publishTime: '2026-09-12 10:05', signupDeadline: '2026-09-18 17:30', openTime: '2026-09-24 14:30',
    agency: '', subject: '大型医疗设备年度维保服务',
    contactPerson: '陈工', contactPhone: '0755-25533018', contactAddress: '深圳市罗湖区东门北路1017号',
    attachments: [{ name: '招标文件.pdf', size: '3.2MB' }],
    sourceUrl: 'https://www.ccgp.gov.cn/'
  },
  {
    id: 4, title: '广州大学城高校后勤保洁服务集中采购项目', type: '招标公告',
    projectNo: 'GDZB-2026-0912', province: '广东', city: '广州市', industry: '物业服务',
    buyer: '广州大学后勤管理处', buyerType: '学校', budget: 620,
    publishTime: '2026-09-11 16:40', signupDeadline: '2026-09-17 17:00', openTime: '2026-09-23 09:00',
    agency: '广东省机电设备招标中心有限公司', subject: '教学楼及宿舍区保洁服务',
    contactPerson: '王老师', contactPhone: '020-39366688', contactAddress: '广州市番禺区大学城外环西路230号',
    attachments: [{ name: '招标文件.pdf', size: '1.8MB' }, { name: '保洁服务标准.pdf', size: '420KB' }],
    sourceUrl: 'https://www.ccgp.gov.cn/'
  },
  {
    id: 5, title: '北京市政务云数据中心机房运维服务招标预告', type: '招标预告',
    projectNo: 'BJZW-YG-2026-076', province: '北京', city: '北京市', industry: '信息化',
    buyer: '北京市大数据中心', buyerType: '政府', budget: 2100,
    publishTime: '2026-09-10 09:00', signupDeadline: '', openTime: '',
    agency: '', subject: '数据中心机房基础设施运维服务',
    contactPerson: '刘工', contactPhone: '010-65288899', contactAddress: '北京市朝阳区日坛北路33号',
    attachments: [],
    sourceUrl: 'https://www.ccgp.gov.cn/'
  },
  {
    id: 6, title: '苏州市工业园区智慧灯杆建设项目（一期）', type: '招标公告',
    projectNo: 'SZYQ-2026-JS-054', province: '江苏', city: '苏州市', industry: '建筑工程',
    buyer: '苏州工业园区管理委员会', buyerType: '政府', budget: 3680,
    publishTime: '2026-09-09 11:15', signupDeadline: '2026-09-15 17:00', openTime: '2026-09-21 13:30',
    agency: '苏州正信工程造价咨询有限公司', subject: '智慧灯杆采购及安装工程',
    contactPerson: '周工', contactPhone: '0512-62886888', contactAddress: '苏州工业园区现代大道999号',
    attachments: [{ name: '招标文件.pdf', size: '4.5MB' }, { name: '图纸清单.zip', size: '12MB' }],
    sourceUrl: 'https://www.ccgp.gov.cn/'
  },
  {
    id: 7, title: '杭州市公安局指挥中心智能化改造项目变更公告', type: '变更公告',
    projectNo: 'HZGA-2026-BG-009', province: '浙江', city: '杭州市', industry: '信息化',
    buyer: '杭州市公安局', buyerType: '公安', budget: 890,
    publishTime: '2026-09-08 15:30', signupDeadline: '2026-09-16 17:00', openTime: '2026-09-22 09:30',
    agency: '浙江省成套招标代理有限公司', subject: '指挥中心大屏及调度系统改造',
    contactPerson: '吴警官', contactPhone: '0571-87666110', contactAddress: '杭州市上城区华光路35号',
    attachments: [{ name: '变更公告.pdf', size: '380KB' }],
    sourceUrl: 'https://www.ccgp.gov.cn/'
  },
  {
    id: 8, title: '成都高新区产业园区食堂餐饮服务采购项目', type: '招标公告',
    projectNo: 'CDGX-2026-CY-021', province: '四川', city: '成都市', industry: '其他',
    buyer: '成都高新技术产业开发区管理委员会', buyerType: '政府', budget: 260,
    publishTime: '2026-09-05 10:50', signupDeadline: '2026-09-12 17:00', openTime: '2026-09-18 10:00',
    agency: '', subject: '产业园区食堂承包经营服务',
    contactPerson: '赵经理', contactPhone: '028-85338899', contactAddress: '成都市高新区天府大道北段18号',
    attachments: [{ name: '招标文件.pdf', size: '1.5MB' }],
    sourceUrl: 'https://www.ccgp.gov.cn/'
  },
  {
    id: 9, title: '武汉市教育装备标准化采购项目（实验室设备）', type: '招标预告',
    projectNo: 'WHJY-YG-2026-043', province: '湖北', city: '武汉市', industry: '教育装备',
    buyer: '武汉市教育局', buyerType: '学校', budget: 1450,
    publishTime: '2026-09-03 09:10', signupDeadline: '', openTime: '',
    agency: '', subject: '中小学理化生实验室成套设备',
    contactPerson: '孙老师', contactPhone: '027-85608900', contactAddress: '武汉市江汉区常青路58号',
    attachments: [],
    sourceUrl: 'https://www.ccgp.gov.cn/'
  },
  {
    id: 10, title: '上海金融城办公楼宇智能化系统维保项目', type: '招标公告',
    projectNo: 'SHJR-2026-WB-067', province: '上海', city: '上海市', industry: '信息化',
    buyer: '上海陆家嘴金融贸易区开发股份有限公司', buyerType: '金融企业', budget: 320,
    publishTime: '2026-08-28 14:00', signupDeadline: '2026-09-10 17:00', openTime: '2026-09-16 09:30',
    agency: '上海国际招标有限公司', subject: '楼宇智能化系统年度维保',
    contactPerson: '钱工', contactPhone: '021-68886655', contactAddress: '上海市浦东新区陆家嘴环路1000号',
    attachments: [{ name: '招标文件.pdf', size: '2.0MB' }],
    sourceUrl: 'https://www.ccgp.gov.cn/'
  }
]))

// ---- 查询 ----
const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const queryFormRef = ref()
const customRange = ref<string[]>([])
const queryParams = reactive({
  keyword: '',
  scope: 'full',
  subscriptionId: undefined as number | undefined,
  type: '',
  province: '',
  city: '',
  industry: '',
  buyerType: '',
  publishRange: '',
  pageNo: 1,
  pageSize: 10
})

const cityOptions = computed(() => PROVINCES.find((p) => p.name === queryParams.province)?.cities || [])
const onProvinceChange = () => {
  queryParams.city = ''
  handleQuery()
}
const onCustomRange = (val: string[] | null) => {
  if (val && val.length) queryParams.publishRange = 'custom'
  handleQuery()
}

const matchKeyword = (item: any, kw: string) => {
  if (!kw) return true
  if (queryParams.scope === 'title') return item.title.includes(kw)
  if (queryParams.scope === 'product') return item.title.includes(kw) || item.subject.includes(kw)
  return (
    item.title.includes(kw) ||
    item.subject.includes(kw) ||
    item.buyer.includes(kw) ||
    item.content.some((p: string) => p.includes(kw))
  )
}
const matchPublishTime = (item: any) => {
  const day = item.publishTime.slice(0, 10)
  if (queryParams.publishRange === 'custom' && customRange.value?.length === 2) {
    return day >= customRange.value[0] && day <= customRange.value[1]
  }
  if (!queryParams.publishRange) return true
  const today = new Date()
  const fmt = (d: Date) => d.toISOString().slice(0, 10)
  if (queryParams.publishRange === 'today') return day === fmt(today)
  const days = queryParams.publishRange === '7d' ? 7 : 30
  const start = new Date(today.getTime() - days * 86400000)
  return day >= fmt(start)
}
const matchSubscription = (item: any) => {
  const sub = SUBSCRIPTIONS.find((s) => s.id === queryParams.subscriptionId)
  if (!sub) return true
  const kwOk = !sub.keywords.length || sub.keywords.some((k) => item.title.includes(k) || item.subject.includes(k))
  const regionOk = !sub.provinces.length || sub.provinces.includes(item.province)
  const industryOk = !sub.industries.length || sub.industries.includes(item.industry)
  return kwOk && regionOk && industryOk
}

const getList = () => {
  loading.value = true
  try {
    const filtered = allData.value.filter(
      (item) =>
        matchKeyword(item, queryParams.keyword.trim()) &&
        matchSubscription(item) &&
        (!queryParams.type || item.type === queryParams.type) &&
        (!queryParams.province || item.province === queryParams.province) &&
        (!queryParams.city || item.city === queryParams.city) &&
        (!queryParams.industry || item.industry === queryParams.industry) &&
        (!queryParams.buyerType || item.buyerType === queryParams.buyerType) &&
        matchPublishTime(item)
    )
    total.value = filtered.length
    const start = (queryParams.pageNo - 1) * queryParams.pageSize
    list.value = filtered.slice(start, start + queryParams.pageSize)
  } finally {
    loading.value = false
  }
}
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}
const resetQuery = () => {
  queryFormRef.value?.resetFields()
  customRange.value = []
  handleQuery()
}
const typeTagType = (type: string) =>
  type === '招标公告' ? 'primary' : type === '招标预告' ? 'warning' : 'danger'

// ---- 详情 ----
const detailVisible = ref(false)
const detail = ref<any>(null)
const openDetail = (row: any) => {
  detail.value = row
  detailVisible.value = true
}
const isUrgent = (deadline: string) => {
  if (!deadline) return false
  const diff = new Date(deadline.replace(' ', 'T')).getTime() - Date.now()
  return diff > 0 && diff < 7 * 86400000
}
const openSource = () => {
  if (detail.value?.sourceUrl) window.open(detail.value.sourceUrl, '_blank')
}
const downloadAttachment = (f: any) => {
  message.info('附件「' + f.name + '」下载功能待后端接入')
}
const toggleFavorite = (row: any) => {
  row.favorited = !row.favorited
  message.success(row.favorited ? '已收藏' : '已取消收藏')
}
const pushToEnterprise = async (row: any) => {
  await message.confirm('确定将标讯「' + row.title + '」推送给园区匹配企业？')
  message.success('已推送给 8 家匹配企业（演示）')
}
const createSubscription = () => {
  if (!detail.value) return
  message.success('已按「' + detail.value.subject + '」生成订阅，可在「标讯订阅」中查看')
}

getList()
</script>

<style scoped>
.keyword-group { display: flex; gap: 6px; }

.filter-row {
  display: flex;
  align-items: center;
  margin-bottom: 12px;

  &:last-child {
    margin-bottom: 0;
  }
}

.filter-label {
  width: 90px;
  flex-shrink: 0;
  color: #606266;
  font-size: 14px;

  &.ml-24px {
    width: auto;
  }
}

.project-no {
  font-size: 12px;
  color: #909399;
}

.detail-wrap {
  padding: 4px 8px 20px;
}

.detail-header {
  border-bottom: 1px solid #ebeef5;
  padding-bottom: 16px;
}

.detail-title {
  font-size: 18px;
  font-weight: 600;
  line-height: 1.5;
}

.detail-tags {
  margin-top: 10px;
}

.detail-meta {
  margin-top: 10px;
  font-size: 13px;
  color: #909399;
  display: flex;
  align-items: center;
}

.time-cards {
  display: flex;
  gap: 12px;
  margin-top: 16px;
}

.time-card {
  flex: 1;
  background: #f5f7fa;
  border-radius: 6px;
  padding: 12px;
}

.time-label {
  font-size: 12px;
  color: #909399;
}

.time-value {
  margin-top: 6px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;

  &.urgent {
    color: var(--el-color-danger);
  }

  &.budget {
    color: var(--el-color-primary);
  }
}

.detail-section {
  margin-top: 20px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 12px;
  padding-left: 8px;
  border-left: 3px solid var(--el-color-primary);
}

.notice-content {
  font-size: 14px;
  line-height: 1.9;
  color: #606266;

  p {
    margin: 0 0 10px;
    text-indent: 2em;
  }
}

.attachment {
  display: flex;
  align-items: center;
  padding: 8px 10px;
  background: #f5f7fa;
  border-radius: 4px;
  margin-bottom: 8px;
  cursor: pointer;

  &:hover {
    background: #ecf5ff;
  }
}

.attachment-name {
  flex: 1;
  font-size: 13px;
  color: #606266;
}

.attachment-size {
  font-size: 12px;
  color: #909399;
}

.no-attachment {
  color: #909399;
  font-size: 13px;
}

.detail-footer {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>