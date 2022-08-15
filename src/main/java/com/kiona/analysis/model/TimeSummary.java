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
public class TimeSummary extends Summary {
    @ExcelProperty(value = "天", order = 1)
    private String day;
    @ExcelProperty(value = "周", order = 1)
    private String week;
    @ExcelProperty(value = "月", order = 1)
    private String month;
}
