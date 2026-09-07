<template>
  <!-- 搜索 -->
  <ContentWrap>
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="90px">
      <el-form-item label="政策标题" prop="title">
        <el-input v-model="queryParams.title" placeholder="请输入政策标题" clearable class="!w-200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="分类" prop="category">
        <el-select v-model="queryParams.category" placeholder="全部分类" clearable class="!w-160px">
          <el-option v-for="c in CATEGORIES" :key="c" :label="c" :value="c" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部状态" clearable class="!w-140px">
          <el-option label="已上架" :value="0" />
          <el-option label="已下架" :value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" />搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" />重置</el-button>
        <el-button type="primary" plain @click="openForm()"><Icon icon="ep:plus" class="mr-5px" />发布园区政策</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true">
      <el-table-column label="序号" type="index" align="center" width="60" />
      <el-table-column label="政策标题" prop="title" min-width="240" show-overflow-tooltip />
      <el-table-column label="园区/发布主体" prop="parkName" align="center" width="160" show-overflow-tooltip />
      <el-table-column label="最高补贴(万)" prop="subsidyMax" align="center" width="110">
        <template #default="s">{{ s.row.subsidyMax || '—' }}</template>
      </el-table-column>
      <el-table-column label="地区" prop="region" align="center" width="130" show-overflow-tooltip />
      <el-table-column label="截止日期" prop="deadline" align="center" width="110">
        <template #default="s">{{ s.row.deadline || '长期' }}</template>
      </el-table-column>
      <el-table-column label="发布日期" prop="publishDate" align="center" width="110" />
      <el-table-column label="状态" prop="status" align="center" width="90">
        <template #default="s">
          <el-tag :type="s.row.status === 0 ? 'success' : 'info'">{{ s.row.status === 0 ? '已上架' : '已下架' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="270" fixed="right">
        <template #default="s">
          <el-button link type="primary" @click="openForm(s.row.id)">编辑</el-button>
          <el-button link type="success" @click="openPush(s.row)"><Icon icon="ep:promotion" class="mr-2px" />推送企业</el-button>
          <el-button link :type="s.row.status === 0 ? 'warning' : 'success'" @click="toggleStatus(s.row)">
            {{ s.row.status === 0 ? '下架' : '上架' }}
          </el-button>
          <el-button link type="danger" @click="handleDelete(s.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>

  <!-- 新增/编辑弹窗 -->
  <Dialog v-model="dialog.visible" :title="dialog.title" width="760px">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
      <el-form-item label="政策标题" prop="title">
        <el-input v-model="form.title" placeholder="请输入政策标题" />
      </el-form-item>
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="园区/发布主体" prop="parkName">
            <el-input v-model="form.parkName" placeholder="如 深圳湾生态科技园" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="发布部门">
            <el-input v-model="form.publishOrg" placeholder="发布部门/来源" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="12">
        <el-col :span="8">
          <el-form-item label="最高补贴(万)">
            <el-input v-model="form.subsidyMax" placeholder="如 20.00" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="所属地区">
            <el-input v-model="form.region" placeholder="如 深圳市/南山区" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="所属行业">
            <el-input v-model="form.industry" placeholder="如 科技服务" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="12">
        <el-col :span="8">
          <el-form-item label="分类">
            <el-select v-model="form.category" placeholder="选择分类" clearable class="!w-full">
              <el-option v-for="c in CATEGORIES" :key="c" :label="c" :value="c" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="截止日期">
            <el-date-picker v-model="form.deadline" type="date" value-format="YYYY-MM-DD" placeholder="长期有效可不填" class="!w-full" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="发布日期">
            <el-date-picker v-model="form.publishDate" type="date" value-format="YYYY-MM-DD" placeholder="默认今天" class="!w-full" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="申报联系人">
            <el-input v-model="form.contactName" placeholder="园区服务联系人" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="咨询电话">
            <el-input v-model="form.contactPhone" placeholder="咨询/申报电话" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="标签">
        <el-input v-model="form.tags" placeholder="多个标签用 ; 分隔，如 园区发布;研发补贴" />
      </el-form-item>
      <el-form-item label="摘要">
        <el-input v-model="form.summary" type="textarea" :rows="2" placeholder="一句话摘要" />
      </el-form-item>
      <el-form-item label="政策正文">
        <el-input v-model="form.content" type="textarea" :rows="6" placeholder="政策正文/申报条件/材料清单（支持换行）" />
      </el-form-item>
      <el-form-item label="原文链接">
        <el-input v-model="form.sourceUrl" placeholder="外部原文链接（选填）" />
      </el-form-item>
      <el-form-item label="可见范围">
        <el-radio-group v-model="form.visibleScope">
          <el-radio label="all">全部可见</el-radio>
          <el-radio label="park">仅本园区</el-radio>
          <el-radio label="region">仅本地区</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="客户对象">
        <el-radio-group v-model="form.pushTargetMode">
          <el-radio label="all">本园区全部客户企业</el-radio>
          <el-radio label="filter">按企业画像筛选</el-radio>
        </el-radio-group>
      </el-form-item>
      <template v-if="form.pushTargetMode === 'filter'">
        <el-form-item label="参保人数">
          <el-input-number v-model="pushFilter.insuredCountMin" :min="0" :max="999999" :controls="false"
            placeholder="下限" class="!w-120px" />
          <span class="mx-8px" style="color:#909399">-</span>
          <el-input-number v-model="pushFilter.insuredCountMax" :min="0" :max="999999" :controls="false"
            placeholder="上限" class="!w-120px" />
          <span class="ml-8px text-13px" style="color:#909399">人</span>
        </el-form-item>
        <el-form-item label="是否有融资">
          <el-radio-group v-model="pushFilter.financed">
            <el-radio :label="undefined">不限</el-radio>
            <el-radio :label="true">有融资</el-radio>
            <el-radio :label="false">无融资</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="成立日期">
          <el-date-picker v-model="establishRange" type="daterange" value-format="YYYY-MM-DD"
            start-placeholder="起始日期" end-placeholder="截止日期" class="!w-320px" />
        </el-form-item>
        <el-form-item label="所属行业">
          <el-cascader v-model="industrySelected" :options="industryOptions"
            :props="{ multiple: true, checkStrictly: true, emitPath: true }"
            placeholder="选择一级 / 二级行业（可多选）" clearable filterable
            collapse-tags collapse-tags-tooltip class="!w-full" />
        </el-form-item>
        <el-form-item label="实缴资本">
          <el-input-number v-model="pushFilter.actualCapitalMin" :min="0" :controls="false"
            placeholder="下限" class="!w-120px" />
          <span class="mx-8px" style="color:#909399">-</span>
          <el-input-number v-model="pushFilter.actualCapitalMax" :min="0" :controls="false"
            placeholder="上限" class="!w-120px" />
          <span class="ml-8px text-13px" style="color:#909399">万元</span>
          <el-tooltip placement="top" content="数据取自工商登记注册资本金额（reg_capital_amount）">
            <Icon icon="ep:question-filled" class="ml-4px" style="color:#c0c4cc" />
          </el-tooltip>
        </el-form-item>
        <el-form-item label="圈选结果">
          <el-tag v-if="form.id" :type="filterCount > 0 ? 'success' : 'info'" size="large">
            共 {{ filterCount }} 家客户企业
          </el-tag>
          <el-tag v-else type="info" size="large">保存后可预览圈选结果</el-tag>
          <el-button v-if="form.id" link type="primary" class="ml-10px"
            :loading="filterCounting" @click="previewFilterList">
            预览名单
          </el-button>
          <div class="mt-4px text-12px" style="color:#909399;line-height:1.6">
            圈选范围为园区客户管理中归属本园区的企业；行业条件仅对已完成工商补全的企业生效。
          </div>
        </el-form-item>
      </template>
      <el-form-item label="状态">
        <el-radio-group v-model="form.status">
          <el-radio :label="0">立即上架</el-radio>
          <el-radio :label="1">存为草稿（下架）</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialog.visible = false">取消</el-button>
      <el-button type="primary" @click="submitForm">保存</el-button>
    </template>
  </Dialog>

  <!-- 推送企业对话框（园区运营筛选本园区符合企业并勾选推送） -->
  <Dialog v-model="pushDialog.visible" :title="pushDialog.title" width="860px">
    <div class="mb-10px text-13px" style="color:#606266;line-height:1.6">
      圈选范围：园区客户管理中「<b style="color:#0066ff">{{ pushDialog.parkName }}</b>」的客户企业，
      默认勾选「画像符合且未推送」的企业，可手动调整。
      <div v-if="pushDialog.filterSummary" class="mt-4px">
        筛选条件：<span style="color:#0066ff">{{ pushDialog.filterSummary }}</span>
      </div>
      <div v-if="pushDialog.unpushableCount > 0" class="mt-4px" style="color:#e6a23c">
        其中 {{ pushDialog.unpushableCount }} 家客户尚未在小程序注册绑定，暂无法接收推送（已置灰）。
      </div>
    </div>
    <el-table ref="pushTableRef" v-loading="pushDialog.loading" :data="pushDialog.list" :stripe="true"
      max-height="440" row-key="enterpriseName" @selection-change="onPushSelect"
      :row-class-name="(o) => (o.row.pushable === false ? 'row-unpushable' : '')">
      <el-table-column type="selection" width="48" align="center"
        :selectable="(r) => r.pushable !== false && !r.pushed" />
      <el-table-column label="企业名称" prop="enterpriseName" min-width="190" show-overflow-tooltip />
      <el-table-column label="法人" prop="legalPerson" width="80" align="center" show-overflow-tooltip />
      <el-table-column label="参保人数" prop="insuredCount" width="90" align="center">
        <template #default="s">{{ s.row.insuredCount ?? '—' }}</template>
      </el-table-column>
      <el-table-column label="融资" width="70" align="center">
        <template #default="s">
          <el-tag v-if="s.row.financed" type="success" size="small">有</el-tag>
          <span v-else style="color:#c0c4cc">—</span>
        </template>
      </el-table-column>
      <el-table-column label="实缴资本" width="105" align="center">
        <template #default="s">
          {{ s.row.actualCapital != null ? Number(s.row.actualCapital).toLocaleString() + ' 万' : '—' }}
        </template>
      </el-table-column>
      <el-table-column label="成立日期" prop="establishDate" width="105" align="center">
        <template #default="s">{{ s.row.establishDate || '—' }}</template>
      </el-table-column>
      <el-table-column label="行业" width="110" align="center" show-overflow-tooltip>
        <template #default="s">{{ s.row.industryLv2Name || s.row.industry || '—' }}</template>
      </el-table-column>
      <el-table-column label="画像" width="70" align="center">
        <template #default="s">
          <el-tag v-if="s.row.matched" type="success" size="small">符合</el-tag>
          <el-tag v-else type="info" size="small">—</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="96" align="center">
        <template #default="s">
          <el-tooltip v-if="s.row.pushable === false" :content="s.row.unpushableReason" placement="top">
            <el-tag type="info" size="small">未注册</el-tag>
          </el-tooltip>
          <el-tag v-else-if="s.row.pushed" type="warning" size="small">已推</el-tag>
          <el-tag v-else type="success" size="small" effect="plain">可推送</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="匹配理由" min-width="150">
        <template #default="s">
          <span style="color:#909399;font-size:12px">{{ (s.row.reasons || []).join('；') || '—' }}</span>
        </template>
      </el-table-column>
    </el-table>
    <template #footer>
      <el-button @click="pushDialog.visible = false">取消</el-button>
      <el-button type="primary" :loading="pushDialog.submitting" @click="submitPush">
        确认推送（{{ pushDialog.checked.length }} 家）
      </el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { getPolicyPage, createPolicy, updatePolicy, deletePolicy, updatePolicyStatus, getPolicy } from '@/api/liqi/policyOps'
import {
  getParkCandidates,
  pushParkPolicy,
  getParkCandidatesByFilter,
  getParkCandidatesCount,
  getIndustryOptions
} from '@/api/liqi/pushMessage'

defineOptions({ name: 'LiqiPolicyPublish' })

const CATEGORIES = ['专精特新', '研发投入', '技术改造', '设备更新', '高企认定', '创业扶持']
const message = useMessage()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryFormRef = ref()
const queryParams = reactive({ pageNo: 1, pageSize: 10, title: undefined, category: undefined, status: undefined })

const dialog = reactive({ visible: false, title: '' })
const formRef = ref()
const form = reactive<any>({})
const rules = { title: [{ required: true, message: '请输入政策标题', trigger: 'blur' }] }

// ---- 客户对象：企业画像圈选 ----
const emptyFilter = () => ({
  insuredCountMin: undefined,
  insuredCountMax: undefined,
  financed: undefined,
  establishDateStart: undefined,
  establishDateEnd: undefined,
  industryLv1Names: undefined,
  industryLv2Names: undefined,
  actualCapitalMin: undefined,
  actualCapitalMax: undefined
})
const pushFilter = reactive<any>(emptyFilter())
const establishRange = ref<string[]>([])
const industrySelected = ref<any[]>([])
const industryOptions = ref<any[]>([])
const filterCount = ref(0)
const filterCounting = ref(false)

/** 行业级联选项：从企业库实际数据分组去重 */
const loadIndustryOptions = async () => {
  if (industryOptions.value.length) return
  const rows = (await getIndustryOptions()) || []
  const lv1Map = new Map<string, any>()
  rows.forEach((r: any) => {
    const lv1 = r.lv1
    const lv2 = r.lv2
    if (!lv1) return
    if (!lv1Map.has(lv1)) lv1Map.set(lv1, { value: lv1, label: lv1, children: [] })
    if (lv2) lv1Map.get(lv1).children.push({ value: lv2, label: lv2 })
  })
  industryOptions.value = Array.from(lv1Map.values()).map((n: any) => {
    if (!n.children.length) delete n.children
    return n
  })
}

/** 级联选中值 → pushFilter 的一级/二级行业名 */
watch(industrySelected, (paths: any[]) => {
  const lv1: string[] = []
  const lv2: string[] = []
  ;(paths || []).forEach((p: any) => {
    const arr = Array.isArray(p) ? p : [p]
    if (arr.length === 1) lv1.push(arr[0])
    else if (arr.length >= 2) lv2.push(arr[arr.length - 1])
  })
  pushFilter.industryLv1Names = lv1.length ? lv1.join(',') : undefined
  pushFilter.industryLv2Names = lv2.length ? lv2.join(',') : undefined
})

/** 日期区间 → pushFilter */
watch(establishRange, (v: any) => {
  pushFilter.establishDateStart = v?.[0] || undefined
  pushFilter.establishDateEnd = v?.[1] || undefined
})

/** 条件变化后防抖刷新命中数量 */
let countTimer: any = null
const refreshFilterCount = () => {
  if (!form.id || form.pushTargetMode !== 'filter') return
  clearTimeout(countTimer)
  countTimer = setTimeout(async () => {
    filterCounting.value = true
    try {
      filterCount.value = (await getParkCandidatesCount('L' + form.id, { ...pushFilter })) || 0
    } finally {
      filterCounting.value = false
    }
  }, 400)
}
watch(() => ({ ...pushFilter }), refreshFilterCount, { deep: true })
watch(() => form.pushTargetMode, (mode: string) => {
  if (mode === 'filter') {
    loadIndustryOptions()
    refreshFilterCount()
  }
})

/** 把当前筛选条件序列化进表单字段 */
const syncFilterToForm = () => {
  form.pushFilterConditions =
    form.pushTargetMode === 'filter' ? JSON.stringify(pushFilter) : undefined
}

/** 人类可读的筛选条件摘要 */
const buildFilterSummary = (f: any) => {
  if (!f) return ''
  const parts: string[] = []
  if (f.insuredCountMin != null || f.insuredCountMax != null) {
    parts.push(`参保人数 ${f.insuredCountMin ?? '不限'}-${f.insuredCountMax ?? '不限'} 人`)
  }
  if (f.financed === true) parts.push('有融资')
  if (f.financed === false) parts.push('无融资')
  if (f.establishDateStart || f.establishDateEnd) {
    parts.push(`成立于 ${f.establishDateStart || '不限'} ~ ${f.establishDateEnd || '不限'}`)
  }
  const inds = [f.industryLv1Names, f.industryLv2Names].filter(Boolean).join(',')
  if (inds) parts.push(inds)
  if (f.actualCapitalMin != null || f.actualCapitalMax != null) {
    parts.push(`实缴资本 ${f.actualCapitalMin ?? '不限'}-${f.actualCapitalMax ?? '不限'} 万元`)
  }
  return parts.join(' · ')
}

const getList = async () => {
  loading.value = true
  try {
    const data = await getPolicyPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}
const handleQuery = () => { queryParams.pageNo = 1; getList() }
const resetQuery = () => { queryFormRef.value?.resetFields(); handleQuery() }

const openForm = async (id?: number) => {
  Object.keys(form).forEach((k) => delete form[k])
  form.visibleScope = 'all'
  form.status = 0
  form.pushTargetMode = 'all'
  // 重置客户对象筛选态
  Object.assign(pushFilter, emptyFilter())
  establishRange.value = []
  industrySelected.value = []
  filterCount.value = 0
  if (id) {
    const data = await getPolicy(id)
    Object.assign(form, data)
    form.pushTargetMode = data.pushTargetMode || 'all'
    // 回填已保存的筛选条件，支持反复调整
    if (data.pushFilterConditions) {
      try {
        const saved = JSON.parse(data.pushFilterConditions)
        Object.assign(pushFilter, emptyFilter(), saved)
        establishRange.value =
          saved.establishDateStart || saved.establishDateEnd
            ? [saved.establishDateStart, saved.establishDateEnd]
            : []
        const paths: any[] = []
        ;(saved.industryLv1Names || '').split(',').filter(Boolean).forEach((v: string) => paths.push([v]))
        ;(saved.industryLv2Names || '').split(',').filter(Boolean).forEach((v: string) => paths.push([v]))
        industrySelected.value = paths
      } catch (e) {
        Object.assign(pushFilter, emptyFilter())
      }
    }
    if (form.pushTargetMode === 'filter') {
      await loadIndustryOptions()
      refreshFilterCount()
    }
    dialog.title = '编辑园区政策'
  } else {
    dialog.title = '发布园区政策'
  }
  dialog.visible = true
}
const submitForm = async () => {
  await formRef.value?.validate()
  syncFilterToForm()
  if (form.id) {
    await updatePolicy(form)
    message.success('修改成功')
  } else {
    await createPolicy(form)
    message.success('发布成功')
  }
  dialog.visible = false
  getList()
}

/** 发布表单内「预览名单」：直接复用推送弹窗展示圈选结果 */
const previewFilterList = async () => {
  if (!form.id) return
  await openPush({ id: form.id, title: form.title, parkName: form.parkName,
    pushTargetMode: 'filter', pushFilterConditions: JSON.stringify(pushFilter) })
}
const toggleStatus = async (row: any) => {
  const next = row.status === 0 ? 1 : 0
  await updatePolicyStatus(row.id, next)
  message.success(next === 0 ? '已上架' : '已下架')
  getList()
}
const handleDelete = async (id: number) => {
  await message.delConfirm('确定删除该园区政策？')
  await deletePolicy(id)
  message.success('删除成功')
  getList()
}

// ---- 推送企业（园区运营手动筛选本园区符合企业） ----
const pushTableRef = ref()
const pushDialog = reactive({
  visible: false,
  title: '',
  loading: false,
  submitting: false,
  policyId: '',
  parkName: '',
  filterSummary: '',
  unpushableCount: 0,
  list: [] as any[],
  checked: [] as any[]
})
const onPushSelect = (rows: any[]) => { pushDialog.checked = rows }
const openPush = async (row: any) => {
  const policyId = 'L' + row.id
  pushDialog.visible = true
  pushDialog.title = '推送园区政策 · ' + row.title
  pushDialog.loading = true
  pushDialog.list = []
  pushDialog.checked = []
  pushDialog.policyId = policyId
  pushDialog.parkName = row.parkName || '本园区'
  pushDialog.filterSummary = ''
  pushDialog.unpushableCount = 0
  try {
    // 按政策配置的客户对象模式分流：filter 走画像圈选，其余沿用原有全量候选
    const useFilter = row.pushTargetMode === 'filter'
    let data: any[] = []
    if (useFilter) {
      let saved: any = {}
      try {
        saved = row.pushFilterConditions ? JSON.parse(row.pushFilterConditions) : {}
      } catch (e) {
        saved = {}
      }
      pushDialog.filterSummary = buildFilterSummary(saved)
      data = (await getParkCandidatesByFilter(policyId, saved)) || []
    } else {
      data = (await getParkCandidates(policyId)) || []
    }
    pushDialog.list = data
    pushDialog.unpushableCount = data.filter((d: any) => d.pushable === false).length
    // 默认勾选：可推送 且 画像符合 且 未推送 的企业
    await nextTick()
    data.forEach((item: any) => {
      if (item.pushable !== false && item.matched && !item.pushed) {
        pushTableRef.value?.toggleRowSelection(item, true)
      }
    })
    if (!data.length) {
      message.info(useFilter
        ? '没有符合筛选条件的园区客户，可放宽画像条件后重试'
        : '本园区暂无绑定企业，企业在 H5 绑定后会自动归属园区')
    }
  } finally {
    pushDialog.loading = false
  }
}
const submitPush = async () => {
  const userIds = pushDialog.checked
    .filter((c: any) => c.pushable !== false && c.userId != null)
    .map((c: any) => c.userId)
  if (!userIds.length) {
    message.warning('请至少勾选一家企业')
    return
  }
  await message.confirm(`确定推送给选中的 ${userIds.length} 家企业？`)
  pushDialog.submitting = true
  try {
    const n = await pushParkPolicy(pushDialog.policyId, userIds)
    message.success(`已成功推送 ${n} 条消息`)
    pushDialog.visible = false
  } finally {
    pushDialog.submitting = false
  }
}

getList()
</script>

<style scoped>
:deep(.row-unpushable) {
  color: #c0c4cc;
  background-color: #fafafa;
}
</style>