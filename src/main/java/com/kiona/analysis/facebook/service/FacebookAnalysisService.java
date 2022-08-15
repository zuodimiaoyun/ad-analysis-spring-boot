package com.kiona.analysis.facebook.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.kiona.analysis.facebook.constant.FacebookSkanConstant;
import com.kiona.analysis.model.DayCampaignSummary;
import com.kiona.analysis.model.DayCreativeSummary;
import com.kiona.analysis.model.Summary;
import com.kiona.analysis.model.TimeSummary;
import com.kiona.analysis.util.DateFormatter;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalField;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 * @author yangshuaichao
 * @date 2022/08/15 14:40
 * @description TODO
 */
@Slf4j
@Service
public class FacebookAnalysisService {
    private static final WeekFields weekFields = WeekFields.of(Locale.getDefault());
    private static final TemporalField weekOfYear = weekFields.weekOfYear();
    private static final List<String> dayExcludeFields = new ArrayList<>();
    private static final List<String> weekExcludeFields = new ArrayList<>();
    private static final List<String> monthExcludeFields = new ArrayList<>();
    static {
        dayExcludeFields.add("week");
        dayExcludeFields.add("month");
        weekExcludeFields.add("day");
        weekExcludeFields.add("month");
        monthExcludeFields.add("week");
        monthExcludeFields.add("day");
    }


    public void analysis(MultipartFile file, HttpServletResponse response) throws IOException {
        checkFileType(file.getInputStream());
        export(parse(file), response);
    }

    private void checkFileType(InputStream inputStream) throws IOException {
        Tika tika = new Tika();
        String detect = tika.detect(inputStream);
        log.info(detect);
    }

    private List<Summary> parse(MultipartFile file) throws IOException {
        List<DayCreativeSummary> dayCreativeSummaries = EasyExcel.read(file.getInputStream()).head(DayCreativeSummary.class).doReadAllSync();
        if(!dayCreativeSummaries.isEmpty() && dayCreativeSummaries.get(0).getCreative() != null){
            return new ArrayList<>(dayCreativeSummaries);
        }else{
            return new ArrayList<>(EasyExcel.read(file.getInputStream()).head(DayCampaignSummary.class).doReadAllSync());
        }
    }

    private void export(List<Summary> summaries, HttpServletResponse response) {
        String fileName = "FacebookSkanResult";
        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        try {
            ExcelWriter excelWriter = EasyExcel
                    .write(response.getOutputStream())
                    .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                    .build();
            if (summaries.get(0) instanceof DayCampaignSummary) {
                List<DayCampaignSummary> list = summaries.stream().map(d -> (DayCampaignSummary) d).collect(Collectors.toList());
                List<TimeSummary> daySummaries = getDaySummaries(list);
                if (!daySummaries.isEmpty()) {
                    WriteSheet writeSheetDay = EasyExcel.writerSheet("按天").head(TimeSummary.class).build();
                    excelWriter.write(daySummaries, writeSheetDay);
                }

                List<DayCampaignSummary> dayCampaignSummaries = getDayCampaignSummaries(list);
                if (!dayCampaignSummaries.isEmpty()) {
                    WriteSheet writeSheetDayCampaign = EasyExcel.writerSheet("按Campaign天").head(DayCampaignSummary.class).build();
                    excelWriter.write(dayCampaignSummaries, writeSheetDayCampaign);
                }
            } else if (summaries.get(0) instanceof DayCreativeSummary) {
                List<DayCreativeSummary> list = summaries.stream().map(d -> (DayCreativeSummary) d).peek(d -> {
                    Matcher matcher = FacebookSkanConstant.creativePattern.matcher(d.getCreative());
                    if (matcher.matches()) {
                        d.setCreative(matcher.group(1));
                    }
                }).collect(Collectors.toList());
                List<DayCreativeSummary> dayCreativeSummaries = getDayCreativeSummaries(list);
                if (!dayCreativeSummaries.isEmpty()) {
                    WriteSheet writeSheetDayCreative = EasyExcel.writerSheet("按素材&天").excludeColumnFiledNames(dayExcludeFields).head(DayCreativeSummary.class).build();
                    excelWriter.write(dayCreativeSummaries, writeSheetDayCreative);
                    List<DayCreativeSummary> creativeSummariesGroupByWeek = getCreativeSummariesGroupByWeek(dayCreativeSummaries);
                    WriteSheet writeSheetWeekCreative = EasyExcel.writerSheet("按素材&周").excludeColumnFiledNames(weekExcludeFields).head(DayCreativeSummary.class).build();
                    excelWriter.write(creativeSummariesGroupByWeek, writeSheetWeekCreative);
                    List<DayCreativeSummary> creativeSummariesGroupByMonth = getCreativeSummariesGroupByMonth(dayCreativeSummaries);
                    WriteSheet writeSheetMonthCreative = EasyExcel.writerSheet("按素材&月").excludeColumnFiledNames(monthExcludeFields).head(DayCreativeSummary.class).build();
                    excelWriter.write(creativeSummariesGroupByMonth, writeSheetMonthCreative);
                }
            }
            excelWriter.finish();
        } catch (Exception e) {
            log.error("导出excel失败", e);
        }
    }

    private List<TimeSummary> getDaySummaries(List<DayCampaignSummary> summaries) {
        return summaries
                .stream()
                .map(TimeSummary::getDay)
                .distinct()
                .map(day -> getSummaryByDay(day, summaries))
                .sorted(Comparator.comparing(TimeSummary::getDay))
                .collect(Collectors.toList());
    }

    private List<DayCampaignSummary> getDayCampaignSummaries(List<DayCampaignSummary> summaries) {
        return summaries
                .stream()
                .map(s -> DayCampaignSummary.builder().campaign(s.getCampaign()).day(s.getDay()).build())
                .distinct()
                .map(d -> getSummaryByDayAndCampaign(d.getDay(), d.getCampaign(), summaries))
                .sorted(Comparator.comparing(DayCampaignSummary::getCampaign).thenComparing(DayCampaignSummary::getDay))
                .collect(Collectors.toList());

    }

    private List<DayCreativeSummary> getDayCreativeSummaries(List<DayCreativeSummary> summaries) {
        Map<String, Map<Date, List<DayCreativeSummary>>> creativeMap = summaries.stream().collect(Collectors.groupingBy(DayCreativeSummary::getCreative, Collectors.groupingBy(s -> DateFormatter.tryParse(s.getDay()), Collectors.toList())));
        return creativeMap
                .entrySet()
                .stream()
                .flatMap(cItem -> cItem.getValue().entrySet()
                        .stream()
                        .map(dItem -> DayCreativeSummary.builder()
                                .creative(cItem.getKey())
                                .day(DateFormatter.format(dItem.getKey()))
                                .click(dItem.getValue().stream().mapToInt(DayCreativeSummary::getClick).sum())
                                .display(dItem.getValue().stream().mapToInt(DayCreativeSummary::getDisplay).sum())
                                .costMoney(dItem.getValue().stream().mapToDouble(Summary::getCostMoney).sum())
                                .install(cItem.getValue().getOrDefault(Date.from(dItem.getKey().toInstant().plus(1, ChronoUnit.DAYS)), Collections.emptyList()).stream().mapToInt(Summary::getInstall).sum())
                                .purchase(cItem.getValue().getOrDefault(Date.from(dItem.getKey().toInstant().plus(2, ChronoUnit.DAYS)), Collections.emptyList()).stream().mapToInt(Summary::getPurchase).sum())
                                .purchaseValue(cItem.getValue().getOrDefault(Date.from(dItem.getKey().toInstant().plus(2, ChronoUnit.DAYS)), Collections.emptyList()).stream().mapToDouble(Summary::getPurchaseValue).sum())
                                .build()
                        )
                ).sorted(Comparator.comparing(DayCreativeSummary::getCreative).thenComparing(DayCreativeSummary::getDay))
                .collect(Collectors.toList());
    }

    private List<DayCreativeSummary> getCreativeSummariesGroupByMonth(List<DayCreativeSummary> summaries) {
        Map<Integer, Map<String, List<DayCreativeSummary>>> groupData = summaries.stream().collect(Collectors.groupingBy(s -> DateFormatter.tryParseLocalDate(s.getDay()).getMonthValue(), Collectors.groupingBy(DayCreativeSummary::getCreative, Collectors.toList())));
        return groupData.entrySet().stream().flatMap(mItem ->
                        mItem.getValue().entrySet().stream().map(cItem ->
                                DayCreativeSummary.builder()
                                        .month(mItem.getKey() + "月")
                                        .creative(cItem.getKey())
                                        .click(cItem.getValue().stream().mapToInt(DayCreativeSummary::getClick).sum())
                                        .display(cItem.getValue().stream().mapToInt(DayCreativeSummary::getDisplay).sum())
                                        .install(cItem.getValue().stream().mapToInt(DayCreativeSummary::getInstall).sum())
                                        .purchase(cItem.getValue().stream().mapToInt(DayCreativeSummary::getPurchase).sum())
                                        .purchaseValue(cItem.getValue().stream().mapToDouble(DayCreativeSummary::getPurchaseValue).sum())
                                        .costMoney(cItem.getValue().stream().mapToDouble(DayCreativeSummary::getCostMoney).sum())
                                        .build())
                ).sorted(Comparator.comparing(DayCreativeSummary::getCreative).thenComparing(DayCreativeSummary::getMonth))
                .collect(Collectors.toList());
    }


    private List<DayCreativeSummary> getCreativeSummariesGroupByWeek(List<DayCreativeSummary> summaries) {

        Map<Integer, Map<String, List<DayCreativeSummary>>> groupData = summaries.stream().collect(Collectors.groupingBy(s -> DateFormatter.tryParseLocalDate(s.getDay()).get(weekOfYear), Collectors.groupingBy(DayCreativeSummary::getCreative, Collectors.toList())));
        return groupData.entrySet().stream().flatMap(wItem ->
                        wItem.getValue().entrySet().stream().map(cItem ->
                                DayCreativeSummary.builder()
                                        .week(wItem.getKey() + "周（开始日期：" + LocalDate.now().with(weekFields.weekOfYear(), wItem.getKey()).with(weekFields.dayOfWeek(), 2) + "）")
                                        .creative(cItem.getKey())
                                        .click(cItem.getValue().stream().mapToInt(DayCreativeSummary::getClick).sum())
                                        .display(cItem.getValue().stream().mapToInt(DayCreativeSummary::getDisplay).sum())
                                        .install(cItem.getValue().stream().mapToInt(DayCreativeSummary::getInstall).sum())
                                        .purchase(cItem.getValue().stream().mapToInt(DayCreativeSummary::getPurchase).sum())
                                        .purchaseValue(cItem.getValue().stream().mapToDouble(DayCreativeSummary::getPurchaseValue).sum())
                                        .costMoney(cItem.getValue().stream().mapToDouble(DayCreativeSummary::getCostMoney).sum())
                                        .build())
                ).sorted(Comparator.comparing(DayCreativeSummary::getCreative).thenComparing(DayCreativeSummary::getWeek))
                .collect(Collectors.toList());
    }

    private TimeSummary getSummaryByDay(String day, List<? extends TimeSummary> summaries) {
        Date summaryDay = DateFormatter.tryParse(day);
        Date installDay = Date.from(summaryDay.toInstant().plus(1, ChronoUnit.DAYS));
        Date purchaseDay = Date.from(summaryDay.toInstant().plus(2, ChronoUnit.DAYS));
        return TimeSummary.builder()
                .day(day)
                .costMoney(summaries.stream().filter(s -> Objects.equals(s.getDay(), day)).mapToDouble(Summary::getCostMoney).sum())
                .install(summaries.stream().filter(s -> Objects.equals(s.getDay(), DateFormatter.format(installDay))).mapToInt(Summary::getInstall).sum())
                .purchase(summaries.stream().filter(s -> Objects.equals(s.getDay(), DateFormatter.format(purchaseDay))).mapToInt(Summary::getPurchase).sum())
                .purchaseValue(summaries.stream().filter(s -> Objects.equals(s.getDay(), DateFormatter.format(purchaseDay))).mapToDouble(Summary::getPurchaseValue).sum())
                .build();
    }

    private DayCampaignSummary getSummaryByDayAndCampaign(String day, String campaign, List<? extends DayCampaignSummary> summaries) {
        Date summaryDay = DateFormatter.tryParse(day);
        Date installDay = Date.from(summaryDay.toInstant().plus(1, ChronoUnit.DAYS));
        Date purchaseDay = Date.from(summaryDay.toInstant().plus(2, ChronoUnit.DAYS));
        return DayCampaignSummary.builder()
                .day(day)
                .campaign(campaign)
                .costMoney(summaries.stream().filter(s -> Objects.equals(s.getDay(), day) && Objects.equals(s.getCampaign(), campaign)).mapToDouble(Summary::getCostMoney).sum())
                .install(summaries.stream().filter(s -> Objects.equals(s.getDay(), DateFormatter.format(installDay)) && Objects.equals(s.getCampaign(), campaign)).mapToInt(Summary::getInstall).sum())
                .purchase(summaries.stream().filter(s -> Objects.equals(s.getDay(), DateFormatter.format(purchaseDay)) && Objects.equals(s.getCampaign(), campaign)).mapToInt(Summary::getPurchase).sum())
                .purchaseValue(summaries.stream().filter(s -> Objects.equals(s.getDay(), DateFormatter.format(purchaseDay)) && Objects.equals(s.getCampaign(), campaign)).mapToDouble(Summary::getPurchaseValue).sum())
                .build();
    }
}
