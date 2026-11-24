<template>
  <div class="ps-page">
    <ContentWrap>
      <div class="ps-head">
        <div>
          <div class="ps-title">园区会员席位</div>
          <div class="ps-sub">
            园区指派即生效，用户端统一展示会员身份；席位不可回收、不可转派，会员到期后席位自然释放。
          </div>
        </div>
        <div class="ps-head-actions">
          <span class="ps-label">当前园区</span>
          <el-select v-model="parkId" class="ps-park-select" @change="reloadAll">
            <el-option v-for="p in parks" :key="p.id" :label="p.parkName" :value="p.id" />
          </el-select>
        </div>
      </div>

      <div class="ps-notice">
        <b>规则</b>
        <span>
          一个账号只有一套会员身份；已有有效会员的账号不可重复指派。账号解除企业关联不影响会员状态，仅在列表提示“企业归属变化”。
          园区合同、结算与发票在财务系统处理，本页仅做席位授权、指派与增购/续费。
        </span>
      </div>

      <div class="ps-metrics">
        <div class="ps-metric">
          <span>授权席位</span>
          <b>{{ overview.totalQty || 0 }}<small>席</small></b>
          <em>有效批次 {{ overview.batchCount || 0 }} 个</em>
        </div>
        <div class="ps-metric">
          <span>已指派</span>
          <b>{{ overview.usedQty || 0 }}<small>席</small></b>
          <em class="ok">当前有效会员占用</em>
        </div>
        <div class="ps-metric">
          <span>剩余可指派</span>
          <b>{{ overview.remainingQty || 0 }}<small>席</small></b>
          <em>指派成功后立即扣减</em>
        </div>
        <div class="ps-metric">
          <span>企业归属变化</span>
          <b>{{ overview.companyChangedQty || 0 }}<small>个</small></b>
          <em>会员不失效，仅提示</em>
        </div>
      </div>
    </ContentWrap>

    <ContentWrap>
      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <el-tab-pane label="席位池" name="pool">
          <div class="ps-toolbar">
            <el-button type="primary" @click="openAssign()">+ 指派会员</el-button>
            <el-button @click="openOrder('PURCHASE')">增购席位</el-button>
            <el-button @click="openOrder('RENEW')">续费席位</el-button>
            <span class="ps-hint">增购/续费无审批，支付完成即生成有效批次</span>
            <span v-if="overview.expiringQty" class="ps-expiring">
              30 天内到期 {{ overview.expiringQty }} 个会员
            </span>
          </div>
          <el-table v-loading="loading.overview" :data="overview.batches || []" stripe>
            <el-table-column label="授权批次" prop="batchNo" min-width="180" />
            <el-table-column label="来源" prop="source" width="100">
              <template #default="{ row }">
                <el-tag size="small" effect="plain">{{ row.source }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="授权席位" prop="qty" width="100" align="right" />
            <el-table-column label="累计已指派" prop="usedQty" width="110" align="right" />
            <el-table-column label="批次剩余" prop="remainingQty" width="100" align="right" />
            <el-table-column label="使用进度" min-width="150">
              <template #default="{ row }">
                <el-progress
                  :percentage="batchPercent(row)"
                  :stroke-width="6"
                  :show-text="false"
                  color="#2a73dc"
                />
                <span class="ps-progress-text">{{ batchPercent(row) }}%</span>
              </template>
            </el-table-column>
            <el-table-column label="生效日期" width="120" align="center">
              <template #default="{ row }">{{ fmtDate(row.startDate) }}</template>
            </el-table-column>
            <el-table-column label="到期日期" width="120" align="center">
              <template #default="{ row }">{{ fmtDate(row.expireDate) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag size="small" type="success" effect="light">使用中</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="本园区账号" name="accounts">
          <div class="ps-toolbar">
            <el-input
              v-model="accountQuery.keyword"
              placeholder="手机号 / 用户名 / 企业名称"
              clearable
              class="ps-search"
              @keyup.enter="searchAccounts"
              @clear="searchAccounts"
            />
            <el-button type="primary" plain @click="searchAccounts">查询</el-button>
            <span class="ps-hint">仅展示当前园区账号；已有会员的账号不可重复指派</span>
          </div>
          <el-table v-loading="loading.accounts" :data="accountList" stripe>
            <el-table-column label="账号" min-width="150">
              <template #default="{ row }">
                <div class="ps-user-cell">
                  <span class="ps-avatar">{{ (row.userName || '?').slice(0, 1) }}</span>
                  <div>
                    <b>{{ row.userName }}</b>
                    <small>ID {{ row.userId }}</small>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="手机号" prop="phone" width="130" />
            <el-table-column label="所属企业" prop="company" min-width="200" show-overflow-tooltip>
              <template #default="{ row }">
                <span>{{ row.company || '未维护企业' }}</span>
                <el-tag v-if="row.companyChanged" size="small" type="warning" class="ps-ml6">
                  企业归属变化
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="会员状态" width="110" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="memberTagType(row.memberStatus)" effect="light">
                  {{ memberStatusText(row.memberStatus) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="会员等级" width="110" align="center">
              <template #default="{ row }">{{ row.memberPlanName || '—' }}</template>
            </el-table-column>
            <el-table-column label="会员到期日" width="130" align="center">
              <template #default="{ row }">{{ fmtDate(row.memberExpireTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="100" align="center" fixed="right">
              <template #default="{ row }">
                <el-button
                  link
                  type="primary"
                  :disabled="row.memberStatus === 'ACTIVE'"
                  @click="openAssign(row)"
                >
                  {{ row.memberStatus === 'ACTIVE' ? '已开通' : '指派' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <Pagination
            :total="accountTotal"
            v-model:page="accountQuery.pageNo"
            v-model:limit="accountQuery.pageSize"
            @pagination="loadAccounts"
          />
        </el-tab-pane>

        <el-tab-pane label="指派记录" name="logs">
          <el-table v-loading="loading.logs" :data="logList" stripe>
            <el-table-column label="时间" width="160" align="center">
              <template #default="{ row }">{{ fmtDateTime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="账号" min-width="130">
              <template #default="{ row }">
                <b>{{ row.userName }}</b>
                <div class="ps-muted">{{ row.phone }}</div>
              </template>
            </el-table-column>
            <el-table-column label="企业" prop="company" min-width="200" show-overflow-tooltip />
            <el-table-column label="会员等级" prop="planName" width="110" align="center" />
            <el-table-column label="动作" width="90" align="center">
              <template #default="{ row }">
                <el-tag size="small" type="primary" effect="light">指派</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="结果" width="90" align="center">
              <template #default="{ row }">
                <el-tag size="small" type="success" effect="light">成功</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="备注" prop="remark" min-width="220" show-overflow-tooltip />
            <el-table-column label="操作人" prop="operator" width="100" align="center" />
          </el-table>
          <Pagination
            :total="logTotal"
            v-model:page="logQuery.pageNo"
            v-model:limit="logQuery.pageSize"
            @pagination="loadLogs"
          />
        </el-tab-pane>

        <el-tab-pane label="增购 / 续费记录" name="orders">
          <el-table v-loading="loading.orders" :data="orderList" stripe>
            <el-table-column label="订单号" prop="orderNo" min-width="190" />
            <el-table-column label="类型" width="100" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="row.orderType === 'PURCHASE' ? 'primary' : 'success'" effect="light">
                  {{ row.orderType === 'PURCHASE' ? '增购' : '续费' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="席位" prop="qty" width="90" align="right" />
            <el-table-column label="单价" width="100" align="right">
              <template #default="{ row }">¥{{ money(row.unitPrice) }}</template>
            </el-table-column>
            <el-table-column label="合同金额" width="130" align="right">
              <template #default="{ row }"><b>¥{{ money(row.amount) }}</b></template>
            </el-table-column>
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag size="small" type="success" effect="light">已支付</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="支付时间" width="160" align="center">
              <template #default="{ row }">{{ fmtDateTime(row.payTime) }}</template>
            </el-table-column>
            <el-table-column label="备注" prop="remark" min-width="180" show-overflow-tooltip />
          </el-table>
          <Pagination
            :total="orderTotal"
            v-model:page="orderQuery.pageNo"
            v-model:limit="orderQuery.pageSize"
            @pagination="loadOrders"
          />
        </el-tab-pane>
      </el-tabs>
    </ContentWrap>

    <!-- 指派弹窗 -->
    <el-dialog v-model="assignVisible" title="指派园区会员" width="860px" destroy-on-close>
      <div class="ps-dialog-grid">
        <div class="ps-dialog-left">
          <div class="ps-search-row">
            <el-input
              v-model="assignKeyword"
              placeholder="输入手机号 / 用户名 / 企业名称"
              clearable
              @keyup.enter="searchAssignAccounts"
            />
            <el-button type="primary" @click="searchAssignAccounts">查询</el-button>
          </div>
          <div v-loading="assignLoading" class="ps-account-list">
            <div
              v-for="a in assignAccounts"
              :key="a.userId"
              class="ps-account-item"
              :class="{ active: assignForm.userId === a.userId, blocked: a.memberStatus === 'ACTIVE' }"
              @click="selectAssignAccount(a)"
            >
              <span class="ps-avatar">{{ (a.userName || '?').slice(0, 1) }}</span>
              <div class="ps-account-main">
                <b>{{ a.userName }}</b>
                <small>{{ a.phone }} · {{ a.company || '未维护企业' }}</small>
              </div>
              <el-tag size="small" :type="memberTagType(a.memberStatus)" effect="light">
                {{ memberStatusText(a.memberStatus) }}
              </el-tag>
            </div>
            <el-empty v-if="!assignLoading && assignAccounts.length === 0" description="本园区未找到匹配账号" :image-size="60" />
          </div>
        </div>

        <div class="ps-dialog-right">
          <template v-if="assignAccount">
            <div class="ps-user-line">
              <span class="ps-avatar big">{{ (assignAccount.userName || '?').slice(0, 1) }}</span>
              <div>
                <b>{{ assignAccount.userName }}</b>
                <small>{{ assignAccount.phone }} · ID {{ assignAccount.userId }}</small>
              </div>
              <el-tag
                size="small"
                :type="memberTagType(assignAccount.memberStatus)"
                effect="light"
                class="ps-ml-auto"
              >
                {{ memberStatusText(assignAccount.memberStatus) }}
              </el-tag>
            </div>

            <div v-if="assignAccount.memberStatus === 'ACTIVE'" class="ps-block">
              <b>该账号已有有效会员，不能重复指派</b>
              <div class="ps-block-line">
                当前等级：{{ assignAccount.memberPlanName }}，到期日：
                {{ fmtDate(assignAccount.memberExpireTime) }}
              </div>
              <div class="ps-block-line">本次不会扣减席位。会员到期后可重新指派。</div>
            </div>

            <template v-else>
              <div class="ps-field-label">选择会员等级</div>
              <div class="ps-plan-list">
                <div
                  v-for="p in plans"
                  :key="p.planCode"
                  class="ps-plan-item"
                  :class="{ active: assignForm.planCode === p.planCode }"
                  @click="assignForm.planCode = p.planCode"
                >
                  <div class="ps-plan-head">
                    <b>{{ p.planName }}</b>
                    <span>¥{{ money(p.price) }} / 年</span>
                  </div>
                  <div class="ps-plan-benefit">{{ p.benefits }}</div>
                </div>
              </div>

              <div class="ps-kv">
                <div>
                  <span>所属企业</span>
                  <b>{{ assignAccount.company || '未维护企业' }}</b>
                </div>
                <div>
                  <span>有效期</span>
                  <b>{{ selectedPlan?.durationDays || 365 }} 天，指派成功立即生效</b>
                </div>
              </div>

              <div v-if="overview.remainingQty > 0" class="ps-ok">
                校验通过：将从剩余席位中扣减 1 个，会员到期日
                {{ fmtDate(estimatedExpire) }}。不会产生“待生效”状态。
              </div>
              <div v-else class="ps-block">
                当前园区没有可用的有效席位，请先由平台增购或续费。
              </div>
            </template>
          </template>
          <el-empty v-else description="请选择左侧账号后继续" :image-size="70" />
        </div>
      </div>

      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button
          type="primary"
          :disabled="!canAssign"
          :loading="assigning"
          @click="confirmAssign"
        >
          确认指派
        </el-button>
      </template>
    </el-dialog>

    <!-- 增购 / 续费弹窗 -->
    <el-dialog
      v-model="orderVisible"
      :title="orderForm.orderType === 'PURCHASE' ? '增购园区席位' : '续费园区席位'"
      width="480px"
      destroy-on-close
    >
      <div class="ps-warn">
        园区订单无需审批，支付完成即生成有效批次并立即增加可指派席位。合同与发票由财务系统处理。
      </div>
      <el-form label-width="96px" class="ps-order-form">
        <el-form-item label="席位数量">
          <el-input-number v-model="orderForm.qty" :min="1" :max="5000" controls-position="right" />
        </el-form-item>
        <el-form-item label="合同单价">
          <el-input-number v-model="orderForm.unitPrice" :min="0" :precision="2" :step="50" controls-position="right" />
          <span class="ps-unit">元 / 席</span>
        </el-form-item>
        <el-form-item label="合同金额">
          <b class="ps-amount">¥{{ money(orderForm.qty * orderForm.unitPrice) }}</b>
        </el-form-item>
        <el-form-item label="生效方式">
          <span>支付完成立即生效，批次有效期 365 天</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="orderVisible = false">取消</el-button>
        <el-button type="primary" :loading="orderSubmitting" @click="submitOrder">
          确认支付并生效
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  assignParkMember,
  createParkSeatOrder,
  getMemberPlanList,
  getParkSeatAccountPage,
  getParkSeatLogPage,
  getParkSeatOrderPage,
  getParkSeatOverview,
  getParkSeatParkList,
  type MemberPlanVO,
  type ParkSeatAccountVO,
  type ParkSeatOverviewVO
} from '@/api/liqi/parkSeat'

defineOptions({ name: 'ParkMemberSeat' })

const parks = ref<any[]>([])
const parkId = ref<number>()
const activeTab = ref('pool')
const overview = ref<Partial<ParkSeatOverviewVO>>({
  totalQty: 0,
  usedQty: 0,
  remainingQty: 0,
  expiringQty: 0,
  companyChangedQty: 0,
  batchCount: 0,
  batches: []
})
const plans = ref<MemberPlanVO[]>([])
const loading = reactive({ overview: false, accounts: false, logs: false, orders: false })

// ---------------- 列表 ----------------
const accountQuery = reactive({ parkId: undefined as number | undefined, keyword: '', pageNo: 1, pageSize: 10 })
const accountList = ref<ParkSeatAccountVO[]>([])
const accountTotal = ref(0)

const logQuery = reactive({ parkId: undefined as number | undefined, pageNo: 1, pageSize: 10 })
const logList = ref<any[]>([])
const logTotal = ref(0)

const orderQuery = reactive({ parkId: undefined as number | undefined, pageNo: 1, pageSize: 10 })
const orderList = ref<any[]>([])
const orderTotal = ref(0)

const loadParks = async () => {
  try {
    const data: any = await getParkSeatParkList()
    parks.value = data || []
    if (!parkId.value && parks.value.length) {
      parkId.value = parks.value[0].id
    }
  } catch (e: any) {
    ElMessage.error(e?.msg || '园区列表加载失败')
  }
}

const loadOverview = async () => {
  if (!parkId.value) return
  loading.overview = true
  try {
    overview.value = await getParkSeatOverview(parkId.value)
  } finally {
    loading.overview = false
  }
}

const loadPlans = async () => {
  try {
    plans.value = (await getMemberPlanList()) || []
  } catch (e) {
    // 套餐加载失败不阻断页面
  }
}

const loadAccounts = async () => {
  if (!parkId.value) return
  loading.accounts = true
  try {
    accountQuery.parkId = parkId.value
    const data: any = await getParkSeatAccountPage(accountQuery)
    accountList.value = data.list || []
    accountTotal.value = data.total || 0
  } finally {
    loading.accounts = false
  }
}

const loadLogs = async () => {
  if (!parkId.value) return
  loading.logs = true
  try {
    logQuery.parkId = parkId.value
    const data: any = await getParkSeatLogPage(logQuery)
    logList.value = data.list || []
    logTotal.value = data.total || 0
  } finally {
    loading.logs = false
  }
}

const loadOrders = async () => {
  if (!parkId.value) return
  loading.orders = true
  try {
    orderQuery.parkId = parkId.value
    const data: any = await getParkSeatOrderPage(orderQuery)
    orderList.value = data.list || []
    orderTotal.value = data.total || 0
  } finally {
    loading.orders = false
  }
}

const reloadAll = async () => {
  accountQuery.pageNo = 1
  logQuery.pageNo = 1
  orderQuery.pageNo = 1
  await Promise.all([loadOverview(), loadAccounts(), loadLogs(), loadOrders()])
}

const searchAccounts = () => {
  accountQuery.pageNo = 1
  loadAccounts()
}

const onTabChange = (name: string) => {
  if (name === 'accounts') loadAccounts()
  if (name === 'logs') loadLogs()
  if (name === 'orders') loadOrders()
}

// ---------------- 指派 ----------------
const assignVisible = ref(false)
const assigning = ref(false)
const assignLoading = ref(false)
const assignKeyword = ref('')
const assignAccounts = ref<ParkSeatAccountVO[]>([])
const assignAccount = ref<ParkSeatAccountVO | null>(null)
const assignForm = reactive<{ userId?: number; planCode: string }>({ userId: undefined, planCode: '' })

const selectedPlan = computed(() =>
  plans.value.find((p) => p.planCode === assignForm.planCode)
)

const estimatedExpire = computed(() => {
  const d = new Date()
  d.setDate(d.getDate() + (selectedPlan.value?.durationDays || 365))
  return d
})

const canAssign = computed(
  () =>
    !!assignAccount.value &&
    assignAccount.value.memberStatus !== 'ACTIVE' &&
    !!assignForm.planCode &&
    (overview.value.remainingQty || 0) > 0
)

const searchAssignAccounts = async () => {
  if (!parkId.value) return
  assignLoading.value = true
  try {
    const data: any = await getParkSeatAccountPage({
      parkId: parkId.value,
      keyword: assignKeyword.value,
      pageNo: 1,
      pageSize: 50
    })
    assignAccounts.value = data.list || []
  } finally {
    assignLoading.value = false
  }
}

const openAssign = async (row?: ParkSeatAccountVO) => {
  assignVisible.value = true
  assignForm.planCode = plans.value[0]?.planCode || 'pro'
  assignKeyword.value = ''
  assignAccount.value = null
  assignForm.userId = undefined
  await searchAssignAccounts()
  if (row) {
    const matched = assignAccounts.value.find((a) => a.userId === row.userId)
    assignAccount.value =
      matched ||
      ({
        ...row,
        memberStatus: row.memberStatus
      } as ParkSeatAccountVO)
    assignForm.userId = row.userId
  }
}

const selectAssignAccount = (account: ParkSeatAccountVO) => {
  assignAccount.value = account
  assignForm.userId = account.userId
}

const confirmAssign = async () => {
  if (!canAssign.value || !assignAccount.value || !parkId.value) return
  const plan = selectedPlan.value
  try {
    await ElMessageBox.confirm(
      `确认给「${assignAccount.value.userName}（${assignAccount.value.phone}）」开通 ${plan?.planName}？\n` +
        `指派后立即生效，有效期 ${plan?.durationDays || 365} 天，席位不可回收、不可转派。`,
      '二次确认',
      { type: 'warning', confirmButtonText: '确认指派并生效', cancelButtonText: '返回修改' }
    )
  } catch {
    return
  }
  assigning.value = true
  try {
    const res: any = await assignParkMember({
      parkId: parkId.value,
      userId: assignAccount.value.userId,
      planCode: assignForm.planCode
    })
    ElMessage.success(
      `已为 ${assignAccount.value.userName} 开通 ${res.planName}，到期日 ${fmtDate(res.expireTime)}`
    )
    assignVisible.value = false
    await reloadAll()
  } catch (e: any) {
    ElMessage.error(e?.msg || '指派失败')
  } finally {
    assigning.value = false
  }
}

// ---------------- 增购 / 续费 ----------------
const orderVisible = ref(false)
const orderSubmitting = ref(false)
const orderForm = reactive<{
  orderType: 'PURCHASE' | 'RENEW'
  qty: number
  unitPrice: number
}>({ orderType: 'PURCHASE', qty: 10, unitPrice: 800 })

const openOrder = (type: 'PURCHASE' | 'RENEW') => {
  orderForm.orderType = type
  orderForm.qty = 10
  orderForm.unitPrice = 800
  orderVisible.value = true
}

const submitOrder = async () => {
  if (!parkId.value) return
  if (!orderForm.qty || orderForm.qty < 1) {
    ElMessage.warning('请输入席位数量')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认${orderForm.orderType === 'PURCHASE' ? '增购' : '续费'} ${orderForm.qty} 个席位？\n` +
        `合同金额 ¥${money(orderForm.qty * orderForm.unitPrice)}，支付完成立即生效。`,
      '确认支付',
      { type: 'warning', confirmButtonText: '确认支付并生效' }
    )
  } catch {
    return
  }
  orderSubmitting.value = true
  try {
    await createParkSeatOrder({
      parkId: parkId.value,
      orderType: orderForm.orderType,
      qty: orderForm.qty,
      unitPrice: orderForm.unitPrice
    })
    ElMessage.success(`已${orderForm.orderType === 'PURCHASE' ? '增购' : '续费'} ${orderForm.qty} 个席位，即时生效`)
    orderVisible.value = false
    await reloadAll()
  } catch (e: any) {
    ElMessage.error(e?.msg || '提交失败')
  } finally {
    orderSubmitting.value = false
  }
}

// ---------------- 格式化 ----------------
const pad = (n: number) => String(n).padStart(2, '0')

const fmtDate = (value: any) => {
  if (value === null || value === undefined || value === '') return '—'
  if (Array.isArray(value)) {
    const [y, m, d] = value
    return `${y}-${pad(Number(m))}-${pad(Number(d))}`
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

const fmtDateTime = (value: any) => {
  if (value === null || value === undefined || value === '') return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return `${fmtDate(date)} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

const money = (value: any) =>
  Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })

const memberStatusText = (status?: string) => {
  if (status === 'ACTIVE') return '生效中'
  if (status === 'EXPIRED') return '已过期'
  return '未开通'
}

const memberTagType = (status?: string) => {
  if (status === 'ACTIVE') return 'success'
  if (status === 'EXPIRED') return 'warning'
  return 'info'
}

const batchPercent = (row: any) => {
  const qty = Number(row?.qty || 0)
  if (!qty) return 0
  return Math.min(100, Math.round((Number(row?.usedQty || 0) / qty) * 100))
}

onMounted(async () => {
  await loadParks()
  await loadPlans()
  if (parkId.value) {
    await reloadAll()
  }
})
</script>

<style scoped>
.ps-page {
  padding-bottom: 12px;
}
.ps-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}
.ps-title {
  font-size: 18px;
  font-weight: 700;
  color: #20314d;
}
.ps-sub {
  margin-top: 4px;
  color: #8a99ae;
  font-size: 12px;
  line-height: 1.7;
}
.ps-head-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: none;
}
.ps-label {
  color: #8a99ae;
  font-size: 12px;
}
.ps-park-select {
  width: 220px;
}
.ps-notice {
  display: flex;
  gap: 9px;
  padding: 11px 13px;
  margin-bottom: 14px;
  border: 1px solid #dbe7f7;
  border-radius: 10px;
  background: linear-gradient(90deg, #f2f7ff, #fbfdff);
  color: #5b6f89;
  font-size: 12px;
  line-height: 1.75;
}
.ps-notice b {
  flex: none;
  height: 20px;
  padding: 0 6px;
  border-radius: 5px;
  background: #deebff;
  color: #1764c0;
  font-size: 10px;
  line-height: 20px;
}
.ps-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}
.ps-metric {
  position: relative;
  overflow: hidden;
  padding: 13px 14px;
  border: 1px solid #dfe7f2;
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 3px 12px rgba(28, 68, 128, 0.04);
}
.ps-metric::after {
  content: '';
  position: absolute;
  right: -26px;
  top: -30px;
  width: 82px;
  height: 82px;
  border-radius: 50%;
  background: #eef5ff;
}
.ps-metric span {
  position: relative;
  display: block;
  color: #8a99ae;
  font-size: 11px;
}
.ps-metric b {
  position: relative;
  display: block;
  margin-top: 3px;
  color: #263c5b;
  font-size: 22px;
  font-weight: 750;
}
.ps-metric b small {
  margin-left: 4px;
  color: #8b9aad;
  font-size: 10px;
  font-weight: 400;
}
.ps-metric em {
  position: relative;
  display: block;
  margin-top: 2px;
  color: #8f9daf;
  font-size: 10px;
  font-style: normal;
}
.ps-metric em.ok {
  color: #13956a;
}
.ps-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.ps-hint {
  color: #91a0b3;
  font-size: 11px;
}
.ps-expiring {
  margin-left: auto;
  padding: 3px 8px;
  border-radius: 6px;
  background: #fff4e5;
  color: #b76d14;
  font-size: 11px;
}
.ps-search {
  width: 260px;
}
.ps-user-cell {
  display: flex;
  align-items: center;
  gap: 9px;
}
.ps-user-cell b {
  color: #314762;
  font-size: 13px;
}
.ps-user-cell small {
  display: block;
  color: #94a2b4;
  font-size: 10px;
}
.ps-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  flex: none;
  border-radius: 50%;
  background: linear-gradient(135deg, #74a6ef, #2a6fd1);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
}
.ps-avatar.big {
  width: 38px;
  height: 38px;
  font-size: 14px;
}
.ps-muted {
  color: #94a2b4;
  font-size: 10px;
}
.ps-ml6 {
  margin-left: 6px;
}
.ps-ml-auto {
  margin-left: auto;
}
.ps-progress-text {
  color: #94a2b4;
  font-size: 10px;
}
.ps-dialog-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 0.9fr);
  gap: 14px;
}
.ps-dialog-left {
  min-width: 0;
}
.ps-search-row {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}
.ps-account-list {
  max-height: 430px;
  overflow: auto;
  padding-right: 4px;
}
.ps-account-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 11px;
  margin-bottom: 7px;
  border: 1px solid #e3ebf6;
  border-radius: 9px;
  background: #fff;
  cursor: pointer;
  transition: 0.15s;
}
.ps-account-item:hover {
  border-color: #aac5ef;
  background: #f8fbff;
}
.ps-account-item.active {
  border-color: #2a73dc;
  background: #f2f7ff;
  box-shadow: 0 0 0 3px rgba(23, 100, 192, 0.07);
}
.ps-account-item.blocked {
  background: #fffafa;
}
.ps-account-main {
  min-width: 0;
  flex: 1;
}
.ps-account-main b {
  display: block;
  color: #2f455f;
  font-size: 12.5px;
}
.ps-account-main small {
  display: block;
  overflow: hidden;
  margin-top: 2px;
  color: #94a2b4;
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ps-dialog-right {
  padding: 12px 13px;
  border: 1px solid #e3ebf6;
  border-radius: 10px;
  background: #fbfdff;
}
.ps-user-line {
  display: flex;
  align-items: center;
  gap: 10px;
}
.ps-user-line b {
  display: block;
  color: #2f455f;
  font-size: 13px;
}
.ps-user-line small {
  display: block;
  margin-top: 2px;
  color: #94a2b4;
  font-size: 10px;
}
.ps-field-label {
  margin: 14px 0 7px;
  color: #8291a5;
  font-size: 11px;
}
.ps-plan-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.ps-plan-item {
  padding: 9px 11px;
  border: 1px solid #dce8f8;
  border-radius: 9px;
  background: #fff;
  cursor: pointer;
  transition: 0.15s;
}
.ps-plan-item:hover {
  border-color: #aac5ef;
}
.ps-plan-item.active {
  border-color: #2a73dc;
  background: #f2f7ff;
  box-shadow: 0 0 0 3px rgba(23, 100, 192, 0.07);
}
.ps-plan-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #2f455f;
  font-size: 12px;
}
.ps-plan-head span {
  color: #1764c0;
  font-size: 11px;
  font-weight: 600;
}
.ps-plan-benefit {
  margin-top: 3px;
  color: #8494a8;
  font-size: 10px;
  line-height: 1.6;
}
.ps-kv {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-top: 11px;
}
.ps-kv > div {
  padding: 8px 9px;
  border-radius: 7px;
  background: #f3f7fc;
}
.ps-kv span {
  display: block;
  color: #96a4b6;
  font-size: 10px;
}
.ps-kv b {
  display: block;
  margin-top: 2px;
  color: #40566f;
  font-size: 11.5px;
  font-weight: 600;
}
.ps-block {
  padding: 10px 11px;
  margin-top: 12px;
  border: 1px solid #f0d0d0;
  border-radius: 8px;
  background: #fff6f6;
  color: #a84c4c;
  font-size: 11px;
  line-height: 1.7;
}
.ps-block-line {
  margin-top: 3px;
}
.ps-ok {
  padding: 10px 11px;
  margin-top: 12px;
  border: 1px solid #cfe9da;
  border-radius: 8px;
  background: #f5fcf8;
  color: #28795a;
  font-size: 11px;
  line-height: 1.7;
}
.ps-warn {
  padding: 10px 11px;
  margin-bottom: 14px;
  border: 1px solid #f0ddc1;
  border-radius: 8px;
  background: #fff9f0;
  color: #88683f;
  font-size: 11px;
  line-height: 1.65;
}
.ps-order-form {
  margin-top: 4px;
}
.ps-unit {
  margin-left: 8px;
  color: #94a2b4;
  font-size: 11px;
}
.ps-amount {
  color: #1764c0;
  font-size: 18px;
}
@media (max-width: 1100px) {
  .ps-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .ps-dialog-grid {
    grid-template-columns: 1fr;
  }
}
</style>
