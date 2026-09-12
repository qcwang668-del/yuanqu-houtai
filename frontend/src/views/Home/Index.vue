<template>
  <div>
    <!-- 欢迎条 -->
    <el-card shadow="never">
      <div class="flex items-center justify-between lt-sm:flex-col lt-sm:items-start">
        <div>
          <div class="text-20px">你好 {{ username }}，祝你开心每一天！</div>
          <div class="mt-8px text-14px text-gray-500">{{ today }}</div>
        </div>
        <div class="flex items-center lt-sm:mt-10px">
          <div class="px-20px text-center">
            <div class="mb-8px text-14px text-gray-400">园区企业</div>
            <div class="text-22px">{{ stats.total.toLocaleString() }}</div>
          </div>
          <el-divider direction="vertical" />
          <div class="px-20px text-center">
            <div class="mb-8px text-14px text-gray-400">已回填</div>
            <div class="text-22px">{{ stats.enriched.toLocaleString() }}</div>
          </div>
          <el-divider direction="vertical" />
          <div class="px-20px text-center">
            <div class="mb-8px text-14px text-gray-400">行业赛道</div>
            <div class="text-22px">{{ stats.industryCount }}</div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 图表 + 快捷入口 -->
    <el-row :gutter="16" class="mt-16px">
      <el-col :xl="9" :lg="9" :md="24" :sm="24" :xs="24">
        <el-card shadow="never">
          <template #header>
            <div class="flex items-center justify-between">
              <span>行业赛道分布</span>
              <span class="text-12px text-gray-400">企业库一级行业 TOP 8</span>
            </div>
          </template>
          <el-skeleton :loading="loading" animated>
            <Echart v-if="industryData.length" :height="320" :options="industryOptions" />
            <el-empty v-else description="暂无行业数据" :image-size="80" />
          </el-skeleton>
        </el-card>
      </el-col>
      <el-col :xl="9" :lg="9" :md="24" :sm="24" :xs="24">
        <el-card shadow="never">
          <template #header>
            <div class="flex items-center justify-between">
              <span>企业规模分布</span>
              <span class="text-12px text-gray-400">按企业画像规模</span>
            </div>
          </template>
          <el-skeleton :loading="loading" animated>
            <Echart v-if="scaleData.length" :height="320" :options="scaleOptions" />
            <el-empty v-else description="暂无规模数据" :image-size="80" />
          </el-skeleton>
        </el-card>
      </el-col>
      <el-col :xl="6" :lg="6" :md="24" :sm="24" :xs="24">
        <el-card shadow="never">
          <template #header>
            <span>快捷入口</span>
          </template>
          <div
            v-for="item in shortcuts"
            :key="item.path"
            class="flex cursor-pointer items-center rounded-6px px-10px py-12px hover:bg-[var(--el-fill-color-light)]"
            @click="go(item.path)"
          >
            <Icon :icon="item.icon" :size="20" :color="item.color" class="mr-10px" />
            <span class="text-14px">{{ item.name }}</span>
            <Icon icon="ep:arrow-right" :size="12" class="ml-auto text-gray-400" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 通知公告 -->
    <el-row v-if="notices.length" :gutter="16" class="mt-16px">
      <el-col :span="24">
        <el-card shadow="never">
          <template #header>
            <span>通知公告</span>
          </template>
          <div
            v-for="item in notices"
            :key="item.id"
            class="flex items-center justify-between border-0 border-b-1px border-gray-200 border-solid py-10px last:border-b-0"
          >
            <span class="text-14px">{{ item.title }}</span>
            <span class="text-12px text-gray-400">{{ formatTime(item.createTime, 'yyyy-MM-dd') }}</span>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script lang="ts" setup>
import { EChartsOption } from 'echarts'
import { formatTime } from '@/utils'
import { useUserStore } from '@/store/modules/user'
import { useRouter } from 'vue-router'
import request from '@/config/axios'

defineOptions({ name: 'Index' })

const router = useRouter()
const userStore = useUserStore()
const username = userStore.getUser.nickname

const weekDays = ['日', '一', '二', '三', '四', '五', '六']
const now = new Date()
const today = `${formatTime(now, 'yyyy年MM月dd日')} 星期${weekDays[now.getDay()]}`

const loading = ref(true)
const stats = reactive({ total: 0, enriched: 0, industryCount: 0 })
const industryData = ref<{ name: string; value: number }[]>([])
const scaleData = ref<{ name: string; value: number }[]>([])

const shortcuts = [
  { name: '园区可视化大屏', path: '/park-screen', icon: 'ep:monitor', color: '#3b82f6' },
  { name: '园区客户管理', path: '/enterpriseManage/memberMgSys', icon: 'ep:office-building', color: '#22d3ee' },
  { name: '榜单招商', path: '/investment/rankingList', icon: 'ep:trophy', color: '#fbbf24' },
  { name: '园区政策发布', path: '/liqi-ops/policy-publish', icon: 'ep:document', color: '#34d399' }
]

const go = (path: string) => router.push(path)

const COLORS = ['#3b82f6', '#22d3ee', '#34d399', '#fbbf24', '#f472b6', '#a78bfa', '#f87171', '#94a3b8']

const industryOptions = computed<EChartsOption>(() => ({
  color: COLORS,
  tooltip: { trigger: 'item', formatter: '{b}<br/>{c} 家（{d}%）' },
  legend: { bottom: 0, type: 'scroll', textStyle: { fontSize: 12 } },
  series: [
    {
      type: 'pie',
      radius: ['42%', '68%'],
      center: ['50%', '44%'],
      itemStyle: { borderRadius: 4, borderWidth: 1 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 14, formatter: '{b}\n{c} 家' } },
      data: industryData.value
    }
  ]
}))

const scaleOptions = computed<EChartsOption>(() => ({
  color: ['#3b82f6'],
  tooltip: { trigger: 'axis', formatter: '{b}企业：{c} 家' },
  grid: { left: 50, right: 20, top: 30, bottom: 30 },
  xAxis: {
    type: 'category',
    data: scaleData.value.map((x) => x.name),
    axisTick: { show: false }
  },
  yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { opacity: 0.3 } } },
  series: [
    {
      type: 'bar',
      barWidth: 40,
      itemStyle: { borderRadius: [4, 4, 0, 0] },
      label: { show: true, position: 'top' },
      data: scaleData.value.map((x) => x.value)
    }
  ]
}))

const notices = ref<{ id: number; title: string; createTime: number }[]>([])

const load = async () => {
  try {
    const data = await request.get({ url: '/liqi/enterprise/stats' })
    if (data) {
      stats.total = Number(data.total) || 0
      stats.enriched = Number(data.enriched) || 0
      const lv1 = Array.isArray(data.industryLv1) ? data.industryLv1 : []
      stats.industryCount = lv1.filter((x: any) => x.name).length
      industryData.value = lv1
        .filter((x: any) => x.name)
        .slice(0, 8)
        .map((x: any) => ({ name: x.name, value: Number(x.value) || 0 }))
      scaleData.value = (Array.isArray(data.scales) ? data.scales : [])
        .filter((x: any) => x.name && x.name !== '未知')
        .map((x: any) => ({ name: x.name, value: Number(x.value) || 0 }))
    }
  } finally {
    loading.value = false
  }
}

const loadNotices = async () => {
  try {
    const data = await request.get({ url: '/system/notice/page', params: { pageNo: 1, pageSize: 5 } })
    notices.value = (data && data.list) || []
  } catch {
    notices.value = []
  }
}

onMounted(() => {
  load()
  loadNotices()
})
</script>