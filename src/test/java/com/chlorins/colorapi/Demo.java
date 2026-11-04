package com.chlorins.colorapi;

import com.chlorins.colorapi.parser.ColorParser;
import com.chlorins.colorapi.parser.ParsedText;
import com.chlorins.colorapi.parser.Run;

/** Prints what the parser does for the acceptance cases; handy for eyeballing gradients. */
public final class Demo {

    public static void main(String[] args) {
        show("%#FF0000纯红%r");
        show("%#FF0000-#00FF00红绿渐变%r");
        show("%#FF0000-#0000FF红到蓝渐变%r");
        show("%#FF0000红%r%#00FF00绿%r");
        show("%%#FF0000字面百分号");
        show("%#ZZZZZZ无效保留");
        show("§c原版红色 §l加粗");
        show("&a绿色 &e金色");
        show("<GRADIENT:#FF0000>旧标签</GRADIENT:#0000FF>");
        show("%@#00FF00@前缀也认%r");
        show("@#0000FF蓝色%r");
        show("😀%#FF0000-#0000FF表情也安全%r😀");
    }

    private static void show(String input) {
        ParsedText parsed = ColorParser.parse(input);
        System.out.println("IN   " + input);
        StringBuilder sb = new StringBuilder("RUNS ");
        for (Run run : parsed.runs()) {
            sb.append('[').append(run.text()).append(" #")
              .append(run.style().rgb() == null ? "------" : String.format("%06X", run.style().rgb()));
            if (run.style().bold()) sb.append(" bold");
            sb.append("] ");
        }
        System.out.println(sb.toString().trim());
        System.out.println("TEXT " + parsed.plainText());
        System.out.println();
    }
}
