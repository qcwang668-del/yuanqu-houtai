import request from '@/config/axios'

// 力企 - 企业库（深圳湾生态园）
export const getEnterprisePage = (params: any) => {
  return request.get({ url: '/liqi/enterprise/page', params })
}

// 企业详情（主表全字段 + 股东/联系人/知产/风险/项目子表）
export const getEnterpriseDetail = (id: number | string) => {
  return request.get({ url: '/liqi/enterprise/get', params: { id } })
}

// 按需实时调天眼查回填工商信息 + 股东
export const enrichEnterprise = (id: number | string) => {
  return request.post({ url: '/liqi/enterprise/enrich', params: { id } })
}

// 企业数据平台「企业基本信息」同步（仅本项目内部使用）
export const syncBaseInfo = (id: number | string) => {
  return request.post({ url: '/liqi/enterprise/sync-base-info', params: { id } })
}

// 企业统计（总数/行业分布/规模分布/状态分布/年份趋势/榜单）
export const getEnterpriseStats = () => {
  return request.get({ url: '/liqi/enterprise/stats' })
}
