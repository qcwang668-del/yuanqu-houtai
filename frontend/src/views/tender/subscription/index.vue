<template>
  <!-- 搜索 -->
  <ContentWrap>
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="90px">
      <el-form-item label="订阅名称" prop="name">
        <el-input v-model="queryParams.name" placeholder="请输入订阅名称" clearable class="!w-220px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" />搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" />重置</el-button>
        <el-button type="primary" plain @click="openSetting()"><Icon icon="ep:plus" class="mr-5px" />新增订阅</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true">
      <el-table-column label="序号" type="index" align="center" width="60" />
      <el-table-column label="订阅名称" prop="name" min-width="180" show-overflow-tooltip />
      <el-table-column label="关键词" align="center" min-width="200" show-overflow-tooltip>
        <template #default="s">{{ s.row.keywords.filter(Boolean).join('、') || '—' }}</template>
      </el-table-column>
      <el-table-column label="查询范围" align="center" width="110">
        <template #default="s">{{ scopeText(s.row.searchScope) }}</template>
      </el-table-column>
      <el-table-column label="采购单位" align="center" min-width="160" show-overflow-tooltip>
        <template #default="s">{{ s.row.buyerUnits.join('、') }}</template>
      </el-table-column>
      <el-table-column label="业务地区" align="center" min-width="140" show-overflow-tooltip>
        <template #default="s">{{ s.row.regions.join('、') }}</template>
      </el-table-column>
      <el-table-column label="创建时间" prop="createTime" align="center" width="170" />
      <el-table-column label="操作" align="center" width="170" fixed="right">
        <template #default="s">
          <el-button link type="primary" @click="openSetting(s.row)">订阅设置</el-button>
          <el-button link type="danger" @click="handleDelete(s.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>

  <!-- 订阅设置弹窗 -->
  <Dialog v-model="settingVisible" :title="settingTitle" width="1080px" scroll max-height="600px">
    <div class="setting-body">
      <!-- 左侧设置区 -->
      <div class="setting-main">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
          <el-form-item label="订阅名称" prop="name">
            <div class="name-row">
              <el-input v-model="form.name" placeholder="请输入订阅器名称" class="!w-360px" maxlength="30" />
              <span class="name-tip">（用订阅器名称来区别您创建的多个订阅器，如：北京-物业保洁）</span>
            </div>
          </el-form-item>


          <el-form-item label="关键词组">
            <div class="keyword-area">
              <div v-for="row in keywordRows" :key="row" class="keyword-row">
                <el-input v-model="form.keywords[(row - 1) * 2]" :placeholder="keywordPlaceholders[((row - 1) * 2) % 6]" class="!w-200px" maxlength="20" />
                <span class="keyword-or">或</span>
                <el-input v-model="form.keywords[(row - 1) * 2 + 1]" :placeholder="keywordPlaceholders[((row - 1) * 2 + 1) % 6]" class="!w-200px" maxlength="20" />
              </div>
              <div v-if="keywordRows < 6" class="keyword-more">
                <el-button plain @click="addKeywordRow"><Icon icon="ep:plus" class="mr-4px" />添加更多关键词</el-button>
                <span class="keyword-tip">（还可添加{{ 12 - keywordRows * 2 }}/12个关键词）</span>
              </div>
            </div>
          </el-form-item>

          <el-form-item label="查询范围">
            <el-radio-group v-model="form.searchScope">
              <el-radio value="full">全文</el-radio>
              <el-radio value="title">标题</el-radio>
              <el-radio value="product">产品/标的</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="行业分类">
            <div class="industry-tags">
              <span v-for="ind in INDUSTRIES" :key="ind" class="industry-tag" :class="{ active: form.industries.includes(ind) }" @click="toggleIndustry(ind)">{{ ind }}</span>
            </div>
          </el-form-item>

          <el-form-item label="采购单位">
            <el-checkbox-group v-model="form.buyerUnits" @change="onBuyerUnitsChange">
              <el-checkbox v-for="u in BUYER_UNITS" :key="u" :value="u">{{ u }}</el-checkbox>
            </el-checkbox-group>
          </el-form-item>

          <el-form-item label="业务地区">
            <div class="region-row">
              <el-tag v-for="r in form.regions" :key="r" closable class="mr-6px" @close="removeRegion(r)">{{ r }}</el-tag>
              <el-link type="danger" @click="openRegion">
                <Icon icon="ep:edit" class="mr-2px" />修改
              </el-link>
            </div>
          </el-form-item>

          <el-form-item label="微信推送">
            <span class="wechat-text">
              您未绑定微信
              <el-link type="danger" class="ml-8px" @click="bindWechat">立即绑定</el-link>
            </span>
          </el-form-item>
        </el-form>
      </div>

      <!-- 右侧结果预览 -->
      <div class="setting-preview">
        <div class="preview-title">| 结果预览</div>
        <div class="preview-count">近一周可获取<span class="preview-num">{{ previewCount }}</span>条招中标信息</div>
        <div v-if="previewCount === 0" class="preview-empty">
          <Icon icon="ep:folder-opened" :size="72" color="#dcdfe6" />
          <div class="preview-empty-text">暂无预览信息</div>
        </div>
        <div v-else class="preview-list">
          <div v-for="(item, i) in previewList" :key="i" class="preview-item">{{ item }}</div>
          <div class="preview-more">保存订阅后可在标讯信息中查看全部结果</div>
        </div>
      </div>
    </div>
    <template #footer>
      <el-button @click="settingVisible = false">取 消</el-button>
      <el-button type="primary" @click="submitSetting">保存订阅</el-button>
    </template>
  </Dialog>

  <!-- 业务地区选择弹窗 -->
  <Dialog v-model="regionVisible" title="选择业务地区" width="560px">
    <el-checkbox v-model="regionAll" @change="onRegionAllChange">全国</el-checkbox>
    <el-divider class="!my-10px" />
    <el-checkbox-group v-model="regionChecked" :disabled="regionAll">
      <el-checkbox v-for="p in PROVINCES" :key="p" :value="p" class="!w-90px">{{ p }}</el-checkbox>
    </el-checkbox-group>
    <template #footer>
      <el-button @click="regionVisible = false">取 消</el-button>
      <el-button type="primary" @click="confirmRegion">确 定</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
defineOptions({ name: 'TenderSubscription' })

const message = useMessage()

const INDUSTRIES = ['全部', '建筑工程', '医疗器械', '信息化', '物业服务', '办公设备', '教育装备', '其他']
const BUYER_UNITS = ['不限', '医疗', '学校', '政府', '公安', '金融企业', '部队', '通信运营商', '自定义']
const PROVINCES = [
  '北京', '天津', '河北', '山西', '内蒙古', '辽宁', '吉林', '黑龙江',
  '上海', '江苏', '浙江', '安徽', '福建', '江西', '山东', '河南',
  '湖北', '湖南', '广东', '广西', '海南', '重庆', '四川', '贵州',
  '云南', '西藏', '陕西', '甘肃', '青海', '宁夏', '新疆', '深圳'
]
const keywordPlaceholders = [
  '输入业务关键词',
  '建议长度为2-6个字',
  '关键词之间为或关系',
  '最多支持12个关键词',
  '避免使用有歧义的词',
  '右侧查看结果预览'
]

// ---- 列表 ----
const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const queryFormRef = ref()
const queryParams = reactive({ name: '', pageNo: 1, pageSize: 10 })

// 演示数据（标讯后端接口未接入，先本地维护）
const allData = ref<any[]>([
  {
    id: 1,
    name: '深圳-智慧园区',
    keywords: ['智慧园区', '园区运营', '物业管理', '', '', '', '', '', '', '', '', ''],
    searchScope: 'full',
    industries: ['信息化', '物业服务'],
    buyerUnits: ['政府', '学校'],
    regions: ['广东', '深圳'],
    createTime: '2026-09-10 10:30:00'
  },
  {
    id: 2,
    name: '全国-物业保洁',
    keywords: ['物业', '保洁', '', '', '', '', '', '', '', '', '', ''],
    searchScope: 'title',
    industries: ['物业服务'],
    buyerUnits: ['不限'],
    regions: ['全国'],
    createTime: '2026-09-12 15:42:00'
  }
])

const getList = () => {
  loading.value = true
  try {
    const filtered = allData.value.filter((item) => !queryParams.name || item.name.includes(queryParams.name))
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
  handleQuery()
}
const scopeText = (scope: string) => ({ full: '全文', title: '标题', product: '产品/标的' })[scope] || '全文'

// ---- 订阅设置弹窗 ----
const formRef = ref()
const settingVisible = ref(false)
const settingTitle = ref('新增订阅设置')
const keywordRows = ref(3)
const emptyForm = () => ({
  id: undefined as number | undefined,
  name: '',
  keywords: Array(12).fill('') as string[],
  searchScope: 'full',
  industries: [] as string[],
  buyerUnits: ['不限'] as string[],
  regions: ['全国'] as string[]
})
const form = reactive(emptyForm())
const rules = {
  name: [{ required: true, message: '请输入订阅器名称', trigger: 'blur' }]
}

const openSetting = (row?: any) => {
  Object.assign(form, emptyForm())
  keywordRows.value = 3
  if (row) {
    form.id = row.id
    form.name = row.name
    form.keywords = [...row.keywords, ...Array(12).fill('')].slice(0, 12)
    form.searchScope = row.searchScope
    form.industries = [...row.industries]
    form.buyerUnits = [...row.buyerUnits]
    form.regions = [...row.regions]
    keywordRows.value = Math.max(3, Math.ceil(row.keywords.filter(Boolean).length / 2))
    settingTitle.value = '订阅设置'
  } else {
    settingTitle.value = '新增订阅设置'
  }
  settingVisible.value = true
}

const addKeywordRow = () => {
  if (keywordRows.value < 6) keywordRows.value += 1
}
const toggleIndustry = (ind: string) => {
  if (ind === '全部') {
    form.industries = form.industries.includes('全部') ? [] : ['全部']
    return
  }
  form.industries = form.industries.filter((v) => v !== '全部')
  const idx = form.industries.indexOf(ind)
  if (idx >= 0) {
    form.industries.splice(idx, 1)
  } else {
    form.industries.push(ind)
  }
}
const onBuyerUnitsChange = (vals: string[]) => {
  // 「不限」与其他选项互斥，且至少保留一项
  const last = vals[vals.length - 1]
  if (last === '不限') {
    form.buyerUnits = ['不限']
  } else {
    const next = vals.filter((v) => v !== '不限')
    form.buyerUnits = next.length ? next : ['不限']
  }
}

// 业务地区
const regionVisible = ref(false)
const regionAll = ref(true)
const regionChecked = ref<string[]>([])
const openRegion = () => {
  regionAll.value = form.regions.includes('全国')
  regionChecked.value = form.regions.filter((v) => v !== '全国')
  regionVisible.value = true
}
const onRegionAllChange = (val: boolean) => {
  if (val) regionChecked.value = []
}
const confirmRegion = () => {
  if (regionAll.value) {
    form.regions = ['全国']
  } else if (regionChecked.value.length) {
    form.regions = [...regionChecked.value]
  } else {
    message.warning('请选择至少一个业务地区')
    return
  }
  regionVisible.value = false
}
const removeRegion = (r: string) => {
  if (form.regions.length <= 1) {
    message.warning('至少保留一个业务地区')
    return
  }
  form.regions = form.regions.filter((v) => v !== r)
}

const bindWechat = () => {
  message.info('微信绑定功能建设中，敬请期待')
}

// 结果预览（演示逻辑：有关键词则给出拟真预览）
const filledKeywords = computed(() => form.keywords.filter((k) => k && k.trim()))
const previewCount = computed(() => (filledKeywords.value.length ? filledKeywords.value.length * 9 + 3 : 0))
const previewList = computed(() =>
  filledKeywords.value.slice(0, 3).map((k, i) => k + '相关项目招中标信息（' + ['招标公告', '中标结果', '招标预告'][i % 3] + '）')
)

const submitSetting = async () => {
  await formRef.value?.validate()
  const payload = {
    ...form,
    keywords: [...form.keywords],
    industries: [...form.industries],
    buyerUnits: [...form.buyerUnits],
    regions: [...form.regions]
  }
  if (form.id) {
    const idx = allData.value.findIndex((item) => item.id === form.id)
    if (idx >= 0) allData.value[idx] = { ...allData.value[idx], ...payload }
    message.success('订阅设置已保存')
  } else {
    allData.value.unshift({
      ...payload,
      id: Date.now(),
      createTime: new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
    })
    message.success('订阅创建成功')
  }
  settingVisible.value = false
  getList()
}

const handleDelete = async (row: any) => {
  await message.delConfirm('确定删除订阅「' + row.name + '」？')
  allData.value = allData.value.filter((item) => item.id !== row.id)
  message.success('删除成功')
  getList()
}

getList()
</script>

<style scoped>
.setting-body {
  display: flex;
  gap: 24px;
}

.setting-main {
  flex: 1;
  min-width: 0;
  border-right: 1px solid #ebeef5;
  padding-right: 24px;
}

.setting-preview {
  width: 300px;
  flex-shrink: 0;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.name-tip {
  font-size: 12px;
  color: #909399;
}

.keyword-area {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.keyword-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.keyword-or {
  color: #606266;
}

.keyword-more {
  display: flex;
  align-items: center;
  gap: 10px;
}

.keyword-tip {
  font-size: 12px;
  color: #909399;
}

.industry-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.industry-tag {
  padding: 2px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  color: #606266;
  user-select: none;

  &.active {
    color: var(--el-color-danger);
    border-color: var(--el-color-danger);
    background-color: #fef0f0;
  }
}

.region-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}

.wechat-text {
  color: #606266;
}

.preview-title {
  font-weight: 600;
  margin-bottom: 8px;
}

.preview-count {
  font-size: 13px;
  color: #606266;
  margin-bottom: 20px;
}

.preview-num {
  color: var(--el-color-danger);
  font-weight: 600;
  margin: 0 2px;
}

.preview-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 0;
}

.preview-empty-text {
  margin-top: 12px;
  color: #909399;
  font-size: 13px;
}

.preview-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.preview-item {
  padding: 8px 10px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 13px;
  color: #606266;
}

.preview-more {
  font-size: 12px;
  color: #909399;
}
</style>
