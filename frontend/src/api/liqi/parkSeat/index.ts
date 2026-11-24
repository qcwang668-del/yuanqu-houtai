import request from '@/config/axios'

export interface ParkSeatBatchVO {
  id: number
  batchNo: string
  source: string
  qty: number
  usedQty: number
  remainingQty: number
  startDate: any
  expireDate: any
  status: number
}

export interface ParkSeatOverviewVO {
  parkId: number
  parkName: string
  totalQty: number
  usedQty: number
  remainingQty: number
  expiringQty: number
  companyChangedQty: number
  batchCount: number
  batches: ParkSeatBatchVO[]
}

export interface ParkSeatAccountVO {
  userId: number
  userName: string
  phone: string
  company: string
  parkId: number
  parkName: string
  memberStatus: 'NONE' | 'ACTIVE' | 'EXPIRED'
  memberPlanName?: string
  memberExpireTime?: number
  memberPrice?: number
  companyChanged?: boolean
}

export interface MemberPlanVO {
  planCode: string
  planName: string
  price: number
  durationDays: number
  benefits: string
}

// 园区席位总览
export const getParkSeatOverview = (parkId: number) => {
  return request.get({ url: '/liqi/park-seat/overview', params: { parkId } })
}

// 本园区账号分页
export const getParkSeatAccountPage = (params: any) => {
  return request.get({ url: '/liqi/park-seat/account/page', params })
}

// 可指派套餐
export const getMemberPlanList = () => {
  return request.get({ url: '/liqi/park-seat/plan/list' })
}

// 指派会员
export const assignParkMember = (data: { parkId: number; userId: number; planCode: string }) => {
  return request.post({ url: '/liqi/park-seat/assign', data })
}

// 指派记录
export const getParkSeatLogPage = (params: any) => {
  return request.get({ url: '/liqi/park-seat/log/page', params })
}

// 增购/续费记录
export const getParkSeatOrderPage = (params: any) => {
  return request.get({ url: '/liqi/park-seat/order/page', params })
}

// 增购/续费（支付完成即生效）
export const createParkSeatOrder = (data: {
  parkId: number
  orderType: 'PURCHASE' | 'RENEW'
  qty: number
  unitPrice: number
}) => {
  return request.post({ url: '/liqi/park-seat/order/create', data })
}

// 园区下拉列表（园区席位权限）
export const getParkSeatParkList = () => {
  return request.get({ url: '/liqi/park-seat/park/list' })
}