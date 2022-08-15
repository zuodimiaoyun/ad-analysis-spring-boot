package com.kiona.analysis.facebook.constant;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FacebookSkanConstant {
    public static Pattern creativePattern = Pattern.compile("^(.*)\\.mp4( \\(\\d*\\))*$");

    public static void main(String[] args) {
        String creative = "Actual in-game_KR_ROE_0322~0326_7_勇士出征多国版_16X9_白.mp4";
        String creative1 = "Actual in-game_KR_ROE_0322~0326_7_勇士出征多国版_16X9_白.mp4 (13223)";
        Matcher matcher = creativePattern.matcher(creative);
        Matcher matcher1 = creativePattern.matcher(creative1);
        if (matcher.matches()) {
            System.out.println(creative + "|" + matcher.group(1));
        }

        if (matcher1.matches()) {
            System.out.println(creative1 + "|" + matcher1.group(1));
        }
    }

}
