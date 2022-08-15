package com.kiona.analysis.model;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
public class DayCampaignSummary extends TimeSummary {
    @ExcelProperty(value = "广告系列名称", order = 0)
    private String campaign;
}
