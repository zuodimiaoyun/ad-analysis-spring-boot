package com.kiona.analysis.google.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.kiona.analysis.google.constant.GoogleSkanConstant;
import com.kiona.analysis.model.DayCampaignSummary;
import com.kiona.analysis.model.Summary;
import com.kiona.analysis.model.TimeSummary;
import com.kiona.analysis.util.CsvUtil;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
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

    private final GoogleEventService googleEventService;

    private static final int START_ANALYSIS_ROW = 3;
    private static final List<String> supportMediaTypes = Arrays.asList("text/csv");

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

    public GoogleAnalysisService(GoogleEventService googleEventService) {this.googleEventService = googleEventService;}

    public void analysis(MultipartFile file, String analysisType, HttpServletResponse response) throws IOException {
        checkFileType(file);
        export(parse(file, analysisType), response);
    }

    private void checkFileType(MultipartFile file) throws IOException {
        Metadata metadata = new Metadata();
        metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, file.getOriginalFilename());
        String detect = new Tika().detect(file.getInputStream(), metadata);
        log.info("????????????" + file.getOriginalFilename() + "??????????????????????????????" + detect);
        if(!supportMediaTypes.contains(detect)){
            throw new RuntimeException("?????????????????????" + detect + "???????????????????????????" + supportMediaTypes);
        }
    }

    private List<Summary> parse(MultipartFile file, String analysisType) throws IOException {
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
            summaries.add(parseRow(currentRow, row, header, analysisType));
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
            List<String> excludeFields = new ArrayList<>(dayExcludeFields);
            excludeFields.add("costMoney");
            if (!daySummaries.isEmpty()) {
                WriteSheet writeSheetDay = EasyExcel
                        .writerSheet("??????")
                        .excludeColumnFiledNames(excludeFields)
                        .head(TimeSummary.class)
                        .build();
                excelWriter.write(daySummaries, writeSheetDay);
            }

            List<DayCampaignSummary> dayCampaignSummaries = getDayCampaignSummaries(summaries);
            if (!dayCampaignSummaries.isEmpty()) {
                WriteSheet writeSheetDayCampaign = EasyExcel
                        .writerSheet("???Campaign???")
                        .excludeColumnFiledNames(excludeFields)
                        .head(DayCampaignSummary.class)
                        .build();
                excelWriter.write(dayCampaignSummaries, writeSheetDayCampaign);
            }
            excelWriter.finish();
        } catch (Exception e) {
            log.error("??????excel??????", e);
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
        return IntStream.range(0, headers.size()).filter(index -> headers.get(index).toLowerCase().matches(headerPattern.toLowerCase())).findFirst().orElse(-1);
    }

    private Header parseHeader(List<String> headers) {
        int dayIndex = findHeaderIndex(headers, GoogleSkanConstant.HEADER_DAY);
        int campaignIndex = findHeaderIndex(headers, GoogleSkanConstant.HEADER_CAMPAIGN);
        int otherEventIndex = findHeaderIndex(headers, GoogleSkanConstant.HEADER_OTHER_EVENT);

        if (dayIndex < 0) {
            throw new RuntimeException("???????????????????????????");
        }
        if (otherEventIndex < 0) {
            throw new RuntimeException("?????????????????????????????????");
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
            throw new RuntimeException("?????????????????????????????????");
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
            throw new RuntimeException("?????????" + rowNum + ", ??????????????????");

        }
        if (headerIndex.getOtherEventIndex() >= columnSize) {
            throw new RuntimeException("?????????" + rowNum + ", ????????????????????????");
        }

        List<Integer> fails = headerIndex.getIndexToEventNo().entrySet().stream().filter(e -> e.getKey() >= columnSize).map(Map.Entry::getValue).collect(Collectors.toList());
        if (!fails.isEmpty()) {
            throw new RuntimeException("?????????" + rowNum + ", ?????????????????????????????????id???" + fails);
        }
    }

    private Summary parseRow(int rowNum, List<String> rowData, Header header, String analysisType) {
        checkColumns(rowNum, rowData.size(), header);
        String day = rowData.get(header.getDayIndex());
        int otherEventCount = csvNumberToInteger(rowData.get(header.getOtherEventIndex()));
        String campaign = header.getCampaignIndex() >= 0 ? rowData.get(header.getCampaignIndex()) : null;
        int[] eventCounts = parseEventCounts(rowNum, rowData, header);
        return summary(day, campaign, otherEventCount, eventCounts, analysisType);
    }

    private int csvNumberToInteger(String csvNum){
        return Integer.parseInt(csvNum.replace("\"", "").replace(",", ""));
    }

    private int[] parseEventCounts(int rowNum, List<String> rowData, Header header) {
        int[] eventCounts = new int[64];
        for (Map.Entry<Integer, Integer> indexToEventNoEntry : header.getIndexToEventNo().entrySet()) {
            int eventIndex = indexToEventNoEntry.getKey();
            int columnNum = eventIndex + 1;
            int eventNo = indexToEventNoEntry.getValue();
            int eventCount = 0;
            try {
                eventCount = csvNumberToInteger(rowData.get(eventIndex));
            } catch (NumberFormatException ex) {
                log.warn("???" + rowNum + "?????????" + columnNum + "??????????????????????????????????????????????????????");
            }
            eventCounts[eventNo] = eventCount;
        }
        return eventCounts;
    }

    private Summary summary(String day, String campaign, int otherEventCount, int[] eventCounts, String analysisType) {
        int install = otherEventCount + Arrays.stream(eventCounts).sum();
        int purchase = googleEventService.getPurchaseCount(analysisType, eventCounts);
        double purchaseValue = googleEventService.getPurchaseValue(analysisType, eventCounts);
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

}
