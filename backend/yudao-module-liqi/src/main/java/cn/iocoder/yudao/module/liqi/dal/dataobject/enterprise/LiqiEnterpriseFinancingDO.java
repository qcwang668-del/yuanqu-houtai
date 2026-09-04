package cn.iocoder.yudao.module.liqi.dal.dataobject.enterprise;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 力企 - 企业库 · 融资信息 DO（外部投融资数据导入）
 */
@TableName("liqi_enterprise_financing")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiqiEnterpriseFinancingDO extends TenantBaseDO {

    @TableId
    private Long id;
    /** 企业编号 */
    private Long enterpriseId;
    /** 外部 entity_id（冗余，用于溯源） */
    private String entityId;
    /** 外部融资记录 id（幂等去重用） */
    private Long sourceId;
    /** 融资时间 */
    private LocalDateTime rzTime;
    /** 融资金额（原始文本，如「数千万人民币」「未披露」） */
    private String rzAmt;
    /** 融资金额（数值·元，可解析时填入） */
    private BigDecimal rzAmtNum;
    /** 融资轮次 */
    private String rzRound;
    /** 投资方信息 JSON 数组 [{"name":"...","id":"..."}] */
    private String investorInfo;

}
