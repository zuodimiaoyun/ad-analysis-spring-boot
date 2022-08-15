package com.kiona.analysis.google.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.kiona.analysis.google.constant.GoogleSkanConstant;
import com.kiona.analysis.google.constant.GoogleSkanPurchaseValue;
import com.kiona.analysis.model.DayCampaignSummary;
import com.kiona.analysis.model.Summary;
import com.kiona.analysis.model.TimeSummary;
import com.kiona.analysis.util.CsvUtil;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author yangshuaichao
 * @date 2022/08/15 13:36
 * @description TODO
 */

@Slf4j
@Service
public class GoogleAnalysisService {

    private static final int START_ANALYSIS_ROW = 3;

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
        List<Summary> summaries = new ArrayList<>();
        Header header = null;
        int currentRow = 0;
        for (List<String> row : CsvUtil.readRecords(file.getInputStream())) {
            currentRow++;
            if (currentRow < START_ANALYSIS_ROW) {
                continue;
            }
            if (currentRow == START_ANALYSIS_ROW) {
                header = parseHeader(row);
                continue;
            }
            summaries.add(parseRow(currentRow, row, header));
        }
        return summaries;
    }

    private void export(List<Summary> summaries, HttpServletResponse response) {
        String fileName = "GoogleSkanResult";
        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        try {
            ExcelWriter excelWriter = EasyExcel
                    .write(response.getOutputStream())
                    .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                    .build();
            List<TimeSummary> daySummaries = getDaySummaries(summaries);
            if (!daySummaries.isEmpty()) {
                WriteSheet writeSheetDay = EasyExcel
                        .writerSheet("按天")
                        .excludeColumnFiledNames(dayExcludeFields)
                        .head(TimeSummary.class)
                        .build();
                excelWriter.write(daySummaries, writeSheetDay);
            }

            List<DayCampaignSummary> dayCampaignSummaries = getDayCampaignSummaries(summaries);
            if (!dayCampaignSummaries.isEmpty()) {
                WriteSheet writeSheetDayCampaign = EasyExcel
                        .writerSheet("按Campaign天")
                        .excludeColumnFiledNames(dayExcludeFields)
                        .head(DayCampaignSummary.class)
                        .build();
                excelWriter.write(dayCampaignSummaries, writeSheetDayCampaign);
            }
            excelWriter.finish();
        } catch (Exception e) {
            log.error("导出excel失败", e);
        }
    }

    private List<TimeSummary> getDaySummaries(List<Summary> summaries) {
        return summaries.stream()
                .filter(s -> s.getClass() == TimeSummary.class)
                .map(s -> (TimeSummary) s)
                .sorted(Comparator.comparing(TimeSummary::getDay))
                .collect(Collectors.toList());
    }

    private List<DayCampaignSummary> getDayCampaignSummaries(List<Summary> summaries) {
        return summaries.stream()
                .filter(s -> s.getClass() == DayCampaignSummary.class)
                .map(s -> (DayCampaignSummary) s)
                .sorted(Comparator.comparing(DayCampaignSummary::getCampaign).thenComparing(DayCampaignSummary::getDay))
                .collect(Collectors.toList());

    }

    private int findHeaderIndex(List<String> headers, String headerPattern) {
        return IntStream.range(0, headers.size()).filter(index -> headers.get(index).matches(headerPattern)).findFirst().orElse(-1);
    }

    private Header parseHeader(List<String> headers) {
        int dayIndex = findHeaderIndex(headers, GoogleSkanConstant.HEADER_DAY);
        int campaignIndex = findHeaderIndex(headers, GoogleSkanConstant.HEADER_CAMPAIGN);
        int otherEventIndex = findHeaderIndex(headers, GoogleSkanConstant.HEADER_OTHER_EVENT);

        if (dayIndex < 0) {
            throw new RuntimeException("未解析到日期表头！");
        }
        if (otherEventIndex < 0) {
            throw new RuntimeException("未解析到未知事件表头！");
        }

        Map<Integer, Integer> indexToEventNo = new HashMap<>(86);
        IntStream.range(0, headers.size()).forEach(headerIndex -> {
            String header = headers.get(headerIndex);
            Matcher matcher = GoogleSkanConstant.EVENT_HEADER_PATTERN.matcher(header);
            if (matcher.matches()) {
                indexToEventNo.put(headerIndex, Integer.parseInt(matcher.group(1)));
            }
        });

        if (indexToEventNo.isEmpty()) {
            throw new RuntimeException("未解析到已知事件表头！");
        }

        return Header.builder()
                .dayIndex(dayIndex)
                .campaignIndex(campaignIndex)
                .otherEventIndex(otherEventIndex)
                .indexToEventNo(indexToEventNo)
                .build();
    }


    @Data
    @Builder
    public static class Header {
        private int dayIndex;
        private int campaignIndex;
        private int otherEventIndex;
        private Map<Integer, Integer> indexToEventNo;
    }

    private void checkColumns(int rowNum, int columnSize, Header headerIndex) {
        if (headerIndex.getDayIndex() >= columnSize) {
            throw new RuntimeException("行数：" + rowNum + ", 日期不可解析");

        }
        if (headerIndex.getOtherEventIndex() >= columnSize) {
            throw new RuntimeException("行数：" + rowNum + ", 未知事件不可解析");
        }

        List<Integer> fails = headerIndex.getIndexToEventNo().entrySet().stream().filter(e -> e.getKey() >= columnSize).map(Map.Entry::getValue).collect(Collectors.toList());
        if (!fails.isEmpty()) {
            throw new RuntimeException("行数：" + rowNum + ", 已知事件不可解析，事件id：" + fails);
        }
    }

    private Summary parseRow(int rowNum, List<String> rowData, Header header) {
        checkColumns(rowNum, rowData.size(), header);
        String day = rowData.get(header.getDayIndex());
        int otherEventCount = Integer.parseInt(rowData.get(header.getOtherEventIndex()));
        String campaign = header.getCampaignIndex() >= 0 ? rowData.get(header.getCampaignIndex()) : null;
        int[] eventCounts = parseEventCounts(rowNum, rowData, header);
        return summary(day, campaign, otherEventCount, eventCounts);
    }

    private int[] parseEventCounts(int rowNum, List<String> rowData, Header header) {
        int[] eventCounts = new int[64];
        for (Map.Entry<Integer, Integer> indexToEventNoEntry : header.getIndexToEventNo().entrySet()) {
            int eventIndex = indexToEventNoEntry.getKey();
            int columnNum = eventIndex + 1;
            int eventNo = indexToEventNoEntry.getValue();
            int eventCount = 0;
            try {
                eventCount = Integer.parseInt(rowData.get(eventIndex).replace("\"", "").replace(",", ""));
            } catch (NumberFormatException ex) {
                log.warn("第" + rowNum + "行，第" + columnNum + "列，存在不可解析数据，跳过！！！！！");
            }
            eventCounts[eventNo] = eventCount;
        }
        return eventCounts;
    }

    private Summary summary(String day, String campaign, int otherEventCount, int[] eventCounts) {
        int install = otherEventCount + Arrays.stream(eventCounts).sum();
        int purchase = getPurchaseCount(day, eventCounts);
        double purchaseValue = getPurchaseValue(day, eventCounts);
        Summary summary;
        if (campaign != null) {
            summary = DayCampaignSummary.builder().day(day).campaign(campaign).build();
        } else {
            summary = TimeSummary.builder().day(day).build();
        }
        summary.setInstall(install);
        summary.setPurchase(purchase);
        summary.setPurchaseValue(purchaseValue);
        return summary;
    }

    private int getPurchaseCount(String day, int[] eventCounts) {
        return IntStream.range(0, eventCounts.length)
                .filter(eventNo -> GoogleSkanPurchaseValue.isPurchaseEvent(eventNo, day))
                .map(eventNo -> eventCounts[eventNo])
                .sum();
    }

    private static double getPurchaseValue(String day, int[] eventCounts) {
        return IntStream.range(0, eventCounts.length)
                .filter(eventNo -> GoogleSkanPurchaseValue.isPurchaseEvent(eventNo, day))
                .mapToDouble(eventNo -> GoogleSkanPurchaseValue.getValue(eventNo, day) * eventCounts[eventNo])
                .sum();
    }


}
