<template>
  <!-- 搜索 -->
  <ContentWrap>
    <el-form
      class="-mb-15px"
      :model="queryParams"
      ref="queryFormRef"
      :inline="true"
      label-width="90px"
    >
      <el-form-item label="企业名称" prop="enterpriseName">
        <el-input
          v-model="queryParams.enterpriseName"
          placeholder="输入企业名称"
          clearable
          class="!w-200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="手机号" prop="phone">
        <el-input
          v-model="queryParams.phone"
          placeholder="请输入手机号"
          clearable
          class="!w-200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="参保人数" prop="insuredBand">
        <el-select
          v-model="insuredBand"
          placeholder="请选择参保人数区间"
          clearable
          class="!w-200px"
          @change="handleQuery"
        >
          <el-option v-for="(b, i) in INSURED_BANDS" :key="i" :label="b.label" :value="i" />
        </el-select>
      </el-form-item>
      <el-form-item label="关联园区" prop="parkId">
        <el-select
          v-model="queryParams.parkId"
          placeholder="请选择园区"
          clearable
          class="!w-200px"
          @change="handleQuery"
        >
          <el-option
            v-for="p in parkList"
            :key="p.id"
            :label="p.parkName"
            :value="p.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" />搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" />重置</el-button>
        <el-button type="success" plain @click="handleExport" :loading="exportLoading">
          <Icon icon="ep:download" class="mr-5px" />导出
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="displayList" :stripe="true">
      <el-table-column label="序号" type="index" align="center" width="60" />
      <el-table-column label="企业名称" align="center" prop="enterpriseName" show-overflow-tooltip>
        <template #default="scope">
          <el-link type="primary" :underline="false" @click="openDetail(scope.row)">
            {{ scope.row.enterpriseName }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column label="法人" align="center" prop="legalPerson" />
      <el-table-column
        label="注册地址"
        align="center"
        prop="registerAddress"
        show-overflow-tooltip
      />
      <el-table-column label="获取补贴金额" align="center" prop="subsidyAmount" width="130">
        <template #default="scope">
          <span v-if="scope.row.subsidyAmount != null" style="color: #e6462c; font-weight: 600">¥ {{ scope.row.subsidyAmount }}</span>
          <span v-else style="color: #98a2b3">-</span>
        </template>
      </el-table-column>
      <el-table-column label="已申报项目" align="center" prop="declaredProjects" width="110">
        <template #default="scope">
          <span style="color: #98a2b3">-</span>
        </template>
      </el-table-column>
      <el-table-column label="参保人数" align="center" prop="insuredCount" width="100">
        <template #default="scope">{{ scope.row.insuredCount != null ? scope.row.insuredCount + ' 人' : '-' }}</template>
      </el-table-column>
      <el-table-column label="创建人" align="center" prop="creatorName" />
      <el-table-column
        label="创建时间"
        align="center"
        prop="createTime"
        :formatter="dateFormatter"
        width="170"
      />
      <el-table-column label="操作" align="center" width="100" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openDetail(scope.row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>

  <!-- 企业详情抽屉 -->
  <EnterpriseDrawer v-model="detailVisible" :enterprise="detailEnt" />
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { getMemberPage, exportMember } from '@/api/liqi/member'
import { getEnterprisePage } from '@/api/liqi/enterprise'
import { getParkList } from '@/api/liqi/park'
import download from '@/utils/download'
import EnterpriseDrawer from '@/views/park/enterpriseList/EnterpriseDrawer.vue'
import { detailFromParkEntity, detailFromBackend } from '@/views/park/enterpriseList/enterpriseData'
import { getEnterpriseDetail } from '@/api/liqi/enterprise'

defineOptions({ name: 'EnterpriseMemberMgSys' })

const message = useMessage()

// 企业详情抽屉
const detailVisible = ref(false)
const detailEnt = ref<any>(null)
const openDetail = async (row: any) => {
  detailEnt.value = detailFromParkEntity(row)
  detailVisible.value = true
  // 尝试拉企业库真实详情（含股东/联系人等子表）
  if (row.id) {
    try {
      const payload: any = await getEnterpriseDetail(row.id)
      if (payload?.enterprise) detailEnt.value = detailFromBackend(payload)
    } catch { /* 保留列表行构造的详情 */ }
  }
}

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const exportLoading = ref(false)
const queryFormRef = ref()
const parkList = ref<any[]>([]) // 关联园区下拉数据
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  enterpriseName: undefined,
  phone: undefined,
  parkId: undefined
})

// ------- 参保人数分档（前端筛选，暂无后端字段，接入后改服务端） -------
const INSURED_BANDS = [
  { label: '0 人', min: 0, max: 0 },
  { label: '1-49 人', min: 1, max: 49 },
  { label: '50-99 人', min: 50, max: 99 },
  { label: '100-499 人', min: 100, max: 499 },
  { label: '500 人以上', min: 500, max: Infinity }
]
const insuredBand = ref<number | undefined>(undefined)

// 参保人数真实值缓存：liqi_member 与 liqi_enterprise 按 id 一一对应（同一批深圳湾生态园企业），
// 会员表无参保人数，故从企业库按 id 回填真实 insuredCount。
const entMap = ref<Record<string, any>>({})
const loadEntMap = async () => {
  const size = 200 // 后端每页上限 200，分页拉全量构建映射
  const map: Record<string, any> = {}
  const collect = (rows: any[]) => rows.forEach((e) => (map[String(e.id)] = e))
  try {
    const first = await getEnterprisePage({ pageNo: 1, pageSize: size })
    collect(first.list || [])
    const pages = Math.min(Math.ceil((first.total || 0) / size), 40) // 安全上限
    const reqs: Promise<any>[] = []
    for (let p = 2; p <= pages; p++) reqs.push(getEnterprisePage({ pageNo: p, pageSize: size }))
    const rest = await Promise.all(reqs)
    rest.forEach((d) => collect(d.list || []))
    entMap.value = map
  } catch {
    entMap.value = map
  }
}

/**
 * 行数据补充：全部取自企业库（liqi_enterprise），不再使用演示派生值。
 * - insuredCount 参保人数、subsidyTotalMoney 补贴总金额均为企业库字段；
 * - 已申报项目数暂无数据源，展示为“-”。
 */
const enrichRow = (row: any) => {
  const ent = entMap.value[String(row.id)] || {}
  return {
    ...row,
    insuredCount: ent.insuredCount ?? null,
    subsidyAmount: ent.subsidyTotalMoney ?? null,
    declaredProjects: null,
    industry: ent.industryLv1Name || ent.industry || row.industry || '',
    regStatus: ent.regStatus || '',
    companyScale: ent.companyScale ?? null
  }
}

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    if (!Object.keys(entMap.value).length) await loadEntMap()
    const data = await getMemberPage(queryParams)
    list.value = (data.list || []).map(enrichRow)
    total.value = data.total
  } finally {
    loading.value = false
  }
}

// 参保人数分档：前端过滤（会员表暂无该字段，接入后端后改服务端筛选）
const displayList = computed(() => {
  if (insuredBand.value === undefined || insuredBand.value === null) return list.value
  const b = INSURED_BANDS[insuredBand.value]
  return list.value.filter((r) => r.insuredCount != null && r.insuredCount >= b.min && r.insuredCount <= b.max)
})

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryFormRef.value?.resetFields()
  insuredBand.value = undefined
  handleQuery()
}


/** 导出 */
const handleExport = async () => {
  try {
    await message.exportConfirm()
    exportLoading.value = true
    const data = await exportMember(queryParams)
    download.excel(data, '园区客户管理.xls')
  } catch {
  } finally {
    exportLoading.value = false
  }
}

onMounted(async () => {
  getList()
  // 加载园区下拉
  parkList.value = await getParkList()
})
</script>
