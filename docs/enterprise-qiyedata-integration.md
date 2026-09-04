# 企业库对接「企业数据平台 · 企业基本信息」接口

> 接口：`GET https://daas.liqicloud.com/api/business/qiyedata/qiye-base-info`（第三步接口）
> **使用范围：仅授权本项目（力企 SaaS）调用**，凭据不得外泄，本系统也不提供对外透传/代理能力。

## 一、调用链

| 层 | 位置 |
|---|---|
| 配置 | `backend/yudao-module-liqi/.../service/enterprise/qiyedata/QiyeDataProperties.java` |
| 客户端（含签名） | `backend/yudao-module-liqi/.../service/enterprise/qiyedata/QiyeDataClient.java` |
| 响应体 | `backend/yudao-module-liqi/.../service/enterprise/qiyedata/QiyeBaseInfoDTO.java`（1:1 对齐 QiYeInfoNewVO） |
| 业务落库 | `LiqiEnterpriseServiceImpl#syncBaseInfo` / `#applyBaseInfo` |
| 后台接口 | `POST /admin-api/liqi/enterprise/sync-base-info?id={企业库编号}` |
| 前端 | `frontend/src/api/liqi/enterprise/index.ts#syncBaseInfo`，详情抽屉「同步企业基本信息」按钮 |

## 二、请求头签名规则

| 请求头 | 取值 |
|---|---|
| `appKey` | 平台分配（env `QIYEDATA_APP_KEY`） |
| `timestamp` | 当前毫秒时间戳，有效期 5 分钟 |
| `nonce` | 每次请求新的 UUID（`IdUtil.fastSimpleUUID()`） |
| `sign` | `MD5(appKey + timestamp + nonce + appSecret)` |
| `tenant-id` | 可选，env `QIYEDATA_TENANT_ID`；正式平台**不需要**该头，留空即可 |

`appSecret` 只在本地参与签名计算，**不随请求发送**。

## 三、仅限本项目使用的落地约束

1. 后台接口 `sync-base-info` **只接受企业库内部编号 `id`**，`entityId` 由库内 `entity_id`（缺失时回退 `credit_code`）推导，不支持按外部传入 `entityId` 直查 —— 避免本系统变成该接口的对外查询代理。
2. 接口受 `@PreAuthorize("@ss.hasPermission('liqi:enterprise:query')")` 与多租户隔离约束，外部匿名/跨租户无法调用。
3. `appKey` / `appSecret` / `base-url` 全部由启动进程环境变量注入（`QIYEDATA_*`），仓库内只保留占位符，禁止硬编码。
4. 平台未配置凭据时，客户端直接跳过调用并记录 warn，不会向任何外部地址发起请求。

## 四、字段映射（接口返回 → `liqi_enterprise`）

语义相同的既有列直接复用，其余落新增列。

| 接口字段 | 库列 | 说明 |
|---|---|---|
| `entityId` | `entity_id` + `credit_code` | 统一社会信用代码 / 唯一 id，`idx_entity_id` 索引 |
| `companyName` | `enterprise_name` | |
| `companyFormerName` | `former_name` | 曾用名 |
| `entityEnglishName` | `english_name` | 新增 |
| `zcbWeb` / `zcbEmail` | `website` / `email` | 新增 |
| `legalName` | `legal_person` | |
| `regCapital` / `regCapitalType` | `reg_capital_amount` / `reg_capital_type` | 同时格式化写入展示列 `registered_capital` |
| `entStatus` | `reg_status` | 经营状态 |
| `regAddress` | `register_address` | |
| `industryLv1~4` / `industryLv1~4Name` | `industry_lv1~4` / `industry_lv1~4_name` | 新增；同时拼「一级/末级」写入 `industry` |
| `regDate` | `establish_date` | 兼容 `yyyy-MM-dd` 与带时间格式 |
| `licenseNumber` | `reg_number` | 工商注册号 |
| `zcbComType` | `company_org_type` | 企业类型 |
| `opFrom` / `opTo` | `op_from` / `op_to` | 新增 |
| `regOrg` | `reg_institute` | 登记机关 |
| `checkDate` | `check_date` | 新增 |
| `opScope` | `business_scope` | 经营范围 |
| `regProvincesCode` / `regCityCode` / `regDistrictCode` | 同名下划线列 | 新增 |
| `subsidyTotalMoney` | `subsidy_total_money` | 新增 |
| `socialStaffNum` | `insured_count` + `staff_num_range` | 参保人数 |
| `organizationNumber` | `org_number` | 组织机构代码 |
| `companyScale` | `company_scale` | 1 微型 2 小型 3 中型 4 大型 |
| `finalShowInfo` | `final_show_info` + `tags` | 数组以 `;` 拼接，前端标签区复用 |

同步成功后写入 `enrich_status=1`、`enrich_time=now()`、`enrich_source='qiyedata'`；查无结果写 `enrich_status=2`。

## 五、表结构迁移

```bash
mysql --user=root --database='ruoyi-vue-pro' -e "source backend/sql/liqi/enterprise-qiyedata-schema.sql"
```

脚本幂等（列/索引存在即跳过），可重复执行。

## 六、环境变量

```bash
QIYEDATA_BASE_URL=https://<平台域名>
QIYEDATA_APP_KEY=<平台分配 appKey>
QIYEDATA_APP_SECRET=<平台分配 appSecret>
QIYEDATA_TENANT_ID=<可选，租户编号>
```

## 七、批量补全

| 接口 | 说明 |
|---|---|
| `POST /admin-api/liqi/enterprise/sync-base-info-batch?limit=2000&interval=200` | 批量补全，取 `enrich_status=0` 且有 entityId/信用代码的企业逐个调接口 |
| `GET /admin-api/liqi/enterprise/enrich-progress` | 进度统计：total / enriched / notFound / pending / syncable |

特性：

- **断点续跑**：成功的记录 `enrich_status` 被改写后不再命中，分多次调用即可跑完全量；
- **失败不中断**：单家异常记入 `failures` 数组继续下一家，返回统计 `picked/succeed/notFound/failed`；
- **限流保护**：`interval` 为每次调用间隔毫秒，默认 200ms，建议对生产接口不小于 200ms；
- **单批上限** 2000 家（`limit` 超限自动截断）。

## 八、企业清单数据源

`backend/sql/liqi/enterprise-shenzhenwan-data.sql` —— 由 `注册地深圳湾企业列表(1).xlsx` 生成，7183 家深圳湾注册地企业。

表格提供 4 列：`entity_id`（平台数字 id）、`entity_name`、`register_address`、`us_credit_code`（18 位统一社会信用代码）。
导入时额外生成 `short_name`（企业简称，去除行政区/企业类型后缀取 4 字）与 `logo_color`（展示色），
其余工商字段留空由接口补全，`enrich_status` 初始为 0。

> 注意：`entity_id` 与 `credit_code` 是两个不同的值 —— 平台 entityId 为内部数字 id。
> 因此 `applyBaseInfo` 中只在返回的 `entityId` 为 18 位信用代码格式时才回写 `credit_code`，
> 避免用内部 id 覆盖真实信用代码（`isCreditCode()` 校验）。

## 九、本地验证记录（2026-09-04）

用本地 mock 平台（校验 appKey/timestamp/nonce/sign）对全量 7183 家企业验证：

- 单家同步：`synced=true`，全部字段正确落库（industry 拼合、companyScale 映射、finalShowInfo → tags）；
- 无 `entityId`/信用代码的企业 → `synced=false`，**不发起外部请求**；
- **批量全量跑通**：7183 家分 5 批处理完毕，6809 家成功 / 374 家查无结果（mock 模拟）/ **0 失败**，签名零错误（`bad=0`），耗时约 30 秒；
- 字段完整性核对：`has_legal`/`has_scale`/`has_ind1`/`has_cap`/`has_scope`/`has_en`/`has_check` 均 = 6809，与成功数一致；
- `credit_code` 18 位校验全部通过（7183/7183），未被平台内部 id 覆盖。

> 验证后已将企业库还原为纯净的表格原始数据（7183 家，`enrich_status=0` 待补全），mock 数据未留存。
> 备份文件：`backup-liqi-enterprise-20260904.sql`（导入前的旧数据）。

## 十、正式平台接入参数（已确认并实测通过）

| 项 | 值 |
|---|---|
| `QIYEDATA_BASE_URL` | `https://daas.liqicloud.com` |
| 接口路径 | `/api/business/qiyedata/qiye-base-info`（**注意**：需求文档写的 `/app-api/...` 在该域名下会返回前端 SPA 页面，实际网关前缀为 `/api`） |
| 成功码 | 平台返回 `code=200`（文档示例写 `0`），客户端已同时兼容 `0` 与 `200` |
| `tenant-id` 头 | 不需要，未配置 `QIYEDATA_TENANT_ID` 即可正常调用 |

> 上述差异已固化在 `QiyeDataProperties#path` 默认值与 `QiyeDataClient` 成功码判断中，
> 配置文件 `application-local.yaml` / `application-dev.yaml` 的 `liqi.qiyedata.path` 默认值同步修正。


## 十一、真实接口全量补全结果（2026-09-04）

对 7183 家深圳湾企业按 `limit=300 & interval=150ms` 分 11 批跑完全量：

| 指标 | 数量 |
|---|---|
| 总数 | 7183 |
| 补全成功 `enrich_status=1` | 7132（99.29%） |
| 平台查无结果 `enrich_status=2` | 51（平台返回 `data:null`，非失败） |
| 失败 `enrich_status=3` | 0 |
| `pending` | 0 |

字段完整度（成功记录内）：`reg_status`/`check_date` 7132、`legal_person` 7130、`industry_lv1` 6974、
`reg_capital_amount` 6717、`company_scale` 5255；`credit_code` 18 位校验 7183/7183 全部通过。

数据分布合理性抽查：经营状态以「存续」为主（7083），行业 Top3 为信息传输/软件和信息技术服务业（1805）、
批发和零售业（1528）、租赁和商务服务业（1443）；企业规模微型 2454、小型 1257、中型 1305、大型 239。

### 本轮修复

1. **列长度溢出**：合伙企业的 `legalName` 可能是长机构名，`varchar(32)` 溢出导致 1 家失败。
   已在 `enterprise-qiyedata-schema.sql` 中放宽 `legal_person`(255)、`reg_status`(64)、`company_org_type`(128)、
   `reg_capital_type`(64)、`registered_capital`(64)、`register_address`(512)、`reg_institute`(255)、
   `enterprise_name`(255)、`short_name`(32)。
2. **注册资本展示文本**：原 `formatCapital` 直接拼成 `300人民币(单位：万元)`，
   现改为拆出币种与单位拼成 `300万元人民币`；已落库的历史数据用 SQL 同步修正。

### 批量补全脚本

`run-enrich.ps1`（部署目录，非仓库产物）：每批前重新登录取 token，循环调用
`sync-base-info-batch` 直到 `pending=0`，日志写 `%TEMP%\enrich-run.log`。
接口天然支持断点续跑，中断后重新执行即可。
