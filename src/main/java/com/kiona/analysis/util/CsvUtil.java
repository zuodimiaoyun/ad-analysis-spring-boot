package com.kiona.analysis.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author yangshuaichao
 * @date 2022/08/15 13:53
 * @description TODO
 */
public class CsvUtil {

    private static final String COMMA_DELIMITER = ",";
    private static final String TAB_DELIMITER = "\t";

    public static List<List<String>> readRecords(InputStream inputStream) throws IOException {
        List<List<String>> records = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("unicode")))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(TAB_DELIMITER);
                records.add(Arrays.asList(values));
            }
        }
        return records;
    }
}
