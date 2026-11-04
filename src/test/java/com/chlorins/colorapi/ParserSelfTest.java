package com.chlorins.colorapi;

import com.chlorins.colorapi.parser.Animation;
import com.chlorins.colorapi.parser.ColorParser;
import com.chlorins.colorapi.parser.ParsedText;
import com.chlorins.colorapi.parser.Run;
import com.chlorins.colorapi.parser.TextStyle;
import com.chlorins.colorapi.parser.TranslationTemplate;

import java.util.List;

/**
 * Dependency-free self test (no JUnit download needed). Run with {@code java com.chlorins.colorapi.ParserSelfTest}.
 * Exits with status 1 on the first failure.
 */
public final class ParserSelfTest {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        // ---- solid ------------------------------------------------------------
        ParsedText solid = ColorParser.parse("%#FF4500传说之刃%r");
        expectRuns(solid, List.of(
                new Run("传说之刃", new TextStyle(0xFF4500, false, false, false, false, false))));
        expect("solid: no leftover marker", "传说之刃", solid.plainText());

        // ---- gradient ---------------------------------------------------------
        ParsedText gradient = ColorParser.parse("%#FF0000-#00FF00红绿渐变%r");
        expect("gradient: one run per code point", 4, gradient.runs().size());
        expect("gradient: first colour", 0xFF0000, gradient.runs().get(0).style().rgb());
        expect("gradient: last colour", 0x00FF00, gradient.runs().get(3).style().rgb());
        expect("gradient: text kept", "红绿渐变", gradient.plainText());

        // ---- multi ------------------------------------------------------------
        ParsedText multi = ColorParser.parse("%#FF0000红%r%#00FF00绿%r");
        expect("multi: two runs", 2, multi.runs().size());
        expect("multi: red then green", "FF0000/00FF00",
                hex(multi.runs().get(0)) + "/" + hex(multi.runs().get(1)));

        // ---- escape -----------------------------------------------------------
        ParsedText escape = ColorParser.parse("%%#FF0000不是颜色");
        expect("escape: literal percent kept", "%#FF0000不是颜色", escape.plainText());
        expect("escape: no colour applied", true, escape.isPlain());

        // ---- invalid ----------------------------------------------------------
        String[] invalid = {"%#ZZZZZZ无效保留", "%#FF00", "&#GG0000x", "@#12345", "%#12345"} ;
        expect("dash without hex stays a solid colour", 0xFF0000, ColorParser.parse("%#FF0000-zzz").runs().get(0).style().rgb());
        for (String s : invalid) {
            ParsedText p = ColorParser.parse(s);
            expect("invalid kept verbatim: " + s, true, p.plainText().indexOf('%') >= 0 || p.isPlain());
        }
        expect("invalid hex: text unchanged", "%#ZZZZZZ无效保留", ColorParser.parse("%#ZZZZZZ无效保留").plainText());

        // ---- legacy -----------------------------------------------------------
        ParsedText legacySection = ColorParser.parse("§c红色 §l加粗");
        expect("legacy §c", 0xFF5555, legacySection.runs().get(0).style().rgb());
        expect("legacy §l", true, legacySection.runs().get(1).style().bold());
        ParsedText legacyAmp = ColorParser.parse("&a绿色");
        expect("legacy &a", 0x55FF55, legacyAmp.runs().get(0).style().rgb());

        // ---- mixing markers and legacy codes ----------------------------------
        ParsedText mixed = ColorParser.parse("%#FF0000红§l粗%r");
        expect("mixed: bold inside marker", true, mixed.runs().get(mixed.runs().size() - 1).style().bold());

        // ---- code points (no surrogate splitting) -----------------------------
        ParsedText emoji = ColorParser.parse("%#FF0000-#0000FF😀😀");
        expect("emoji: 2 runs", 2, emoji.runs().size());
        expect("emoji: first run is full code point", "😀", emoji.runs().get(0).text());
        expect("emoji: 1 char count", 1, emoji.runs().get(0).text().length() / 2);

        // ---- single character gradient must not be black/NaN ------------------
        ParsedText single = ColorParser.parse("%#FF0000-#0000FFX");
        expect("single char gradient = start colour", 0xFF0000, single.runs().get(0).style().rgb());

        // ---- old MatrixColorAPI tag syntax ------------------------------------
        ParsedText oldTag = ColorParser.parse("<SOLID:#00FF00>Hi</SOLID>");
        expect("legacy <SOLID> tag", 0x00FF00, oldTag.runs().get(0).style().rgb());
        ParsedText oldGradient = ColorParser.parse("<GRADIENT:#FF0000>AB</GRADIENT:#0000FF>");
        expect("legacy <GRADIENT> tag: 2 runs", 2, oldGradient.runs().size());
        expect("legacy <GRADIENT> tag: end colour", 0x0000FF, oldGradient.runs().get(1).style().rgb());

        // ---- templates --------------------------------------------------------
        List<TranslationTemplate.Part> parts = TranslationTemplate.split("%#FF0000%s 点伤害%r");
        expect("template: 3 parts", 3, parts.size());
        expect("template: arg index", 0, parts.get(1).argIndex());
        expect("template: literal kept", "%#FF0000", parts.get(0).literal());

        List<TranslationTemplate.Part> indexed = TranslationTemplate.split("%2$s and %1$s");
        expect("template: indexed arg order", 1, indexed.get(0).argIndex());
        expect("template: indexed arg order 2", 0, indexed.get(2).argIndex());

        // ---- multi-stop gradient ---------------------------------------------
        ParsedText rainbow = ColorParser.parse("%#FF0000-#FFFF00-#00FF00红黄绿%r");
        expect("multi-stop: one run per char", 3, rainbow.runs().size());
        expect("multi-stop: start", 0xFF0000, rainbow.runs().get(0).style().rgb());
        expect("multi-stop: middle", 0xFFFF00, rainbow.runs().get(1).style().rgb());
        expect("multi-stop: end", 0x00FF00, rainbow.runs().get(2).style().rgb());

        ParsedText fire = ColorParser.parse("%#FF0000-#FFAA00-#FFFF00-#FFFFFF火焰之光%r");
        expect("four stops: 4 runs", 4, fire.runs().size());
        expect("four stops: first", 0xFF0000, fire.runs().get(0).style().rgb());
        expect("four stops: last", 0xFFFFFF, fire.runs().get(3).style().rgb());

        // ---- animations --------------------------------------------------------
        ParsedText rainbowAnim = ColorParser.parse("%rainbow[abc]");
        expect("rainbow: 3 runs", 3, rainbowAnim.runs().size());
        expect("rainbow: kind", Animation.Kind.RAINBOW, rainbowAnim.runs().get(0).animation().kind());
        expect("rainbow: index", 1, rainbowAnim.runs().get(1).animation().index());
        expect("rainbow: count", 3, rainbowAnim.runs().get(1).animation().count());
        expect("rainbow: text", "abc", rainbowAnim.plainText());

        ParsedText mixedAnim = ColorParser.parse("开始%wave[波浪]结束");
        expect("wave: text", "开始波浪结束", mixedAnim.plainText());
        expect("wave: flag", true, ColorParser.hasAnimation("%wave[x]"));
        expect("no animation", false, ColorParser.hasAnimation("%#FF0000abc"));

        // ---- overlay (render path) --------------------------------------------
        com.chlorins.colorapi.render.Overlay overlay = com.chlorins.colorapi.render.OverlayScanner.scan("%#FF0000ab%r");
        expect("overlay: parsed", true, overlay != null);
        expect("overlay: marker skipped", true, overlay.isSkipped(0));
        expect("overlay: first char colour", 0xFF0000, overlay.color(9));
        expect("overlay: plain text untouched", false, com.chlorins.colorapi.render.OverlayScanner.hasMarkers("普通文字"));

        com.chlorins.colorapi.render.Overlay animOverlay = com.chlorins.colorapi.render.OverlayScanner.scan("%pulse[ab]");
        expect("overlay: pulse detected", true, animOverlay != null && animOverlay.hasAnimation());
        expect("overlay: pulse not wave", false, animOverlay.hasWave());

        com.chlorins.colorapi.render.Overlay waveOverlay = com.chlorins.colorapi.render.OverlayScanner.scan("%wave[ab]");
        expect("overlay: wave detected", true, waveOverlay != null && waveOverlay.hasWave());

        // ---- %flow: dynamic gradient ------------------------------------------
        ParsedText flow = ColorParser.parse("%flow[流光]");
        expect("flow: 2 runs", 2, flow.runs().size());
        expect("flow: kind", Animation.Kind.FLOW, flow.runs().get(0).animation().kind());
        expect("flow: default ramp", Animation.DEFAULT_FLOW.length, flow.runs().get(0).animation().stops().length);

        ParsedText customFlow = ColorParser.parse("%flow[#FF0000-#00FF00|绿红]");
        expect("flow: custom ramp size", 2, customFlow.runs().get(0).animation().stops().length);
        expect("flow: custom ramp first", 0xFF0000, customFlow.runs().get(0).animation().stops()[0]);
        expect("flow: text kept", "绿红", customFlow.plainText());

        // ---- %aHH alpha --------------------------------------------------------
        com.chlorins.colorapi.render.Overlay alphaOverlay =
                com.chlorins.colorapi.render.OverlayScanner.scan("%a80淡出文字");
        expect("alpha: has alpha", true, alphaOverlay != null && alphaOverlay.hasAlpha());
        expect("alpha: value", 0x80, alphaOverlay.uniformAlpha());
        expect("alpha: marker skipped", true, alphaOverlay.isSkipped(0));

        // ---- §k obfuscated through the overlay --------------------------------
        com.chlorins.colorapi.render.Overlay obfOverlay =
                com.chlorins.colorapi.render.OverlayScanner.scan("§k这是乱码");
        expect("§k: detected", true, obfOverlay != null && obfOverlay.hasDecorations());
        expect("§k: code chars skipped", true, obfOverlay.isSkipped(0) && obfOverlay.isSkipped(1));
        expect("§k: obfuscated flag set", true,
                (obfOverlay.decorationsOn(2) & com.chlorins.colorapi.render.Overlay.OBFUSCATED) != 0);

        com.chlorins.colorapi.render.Overlay ampObf = com.chlorins.colorapi.render.OverlayScanner.scan("&k乱码&r正常");
        expect("&k: obfuscated", true, (ampObf.decorationsOn(2) & com.chlorins.colorapi.render.Overlay.OBFUSCATED) != 0);
        expect("&r: cleared", true, (ampObf.decorationsOff(6) & com.chlorins.colorapi.render.Overlay.OBFUSCATED) != 0);

        // ---- legacy colour through the overlay --------------------------------
        com.chlorins.colorapi.render.Overlay legacyColour = com.chlorins.colorapi.render.OverlayScanner.scan("§c红色");
        expect("§c: colour applied", 0xFF5555, legacyColour.color(2));

        // ---- decorations -------------------------------------------------------
        ParsedText bold = ColorParser.parse("%b粗体%r普通");
        expect("bold: first run bold", true, bold.runs().get(0).style().bold());
        expect("bold: reset after %r", false, bold.runs().get(bold.runs().size() - 1).style().bold());
        expect("bold: text", "粗体普通", bold.plainText());

        ParsedText italic = ColorParser.parse("%i斜体");
        expect("italic", true, italic.runs().get(0).style().italic());
        ParsedText underline = ColorParser.parse("%u下划线");
        expect("underline", true, underline.runs().get(0).style().underlined());
        ParsedText strike = ColorParser.parse("%m删除线");
        expect("strike", true, strike.runs().get(0).style().strikethrough());
        ParsedText obf = ColorParser.parse("%k乱码");
        expect("obfuscated", true, obf.runs().get(0).style().obfuscated());

        expect("alias %l", true, ColorParser.parse("%l粗").runs().get(0).style().bold());
        expect("alias %o", true, ColorParser.parse("%o斜").runs().get(0).style().italic());
        expect("alias %n", true, ColorParser.parse("%n线").runs().get(0).style().underlined());

        // scoped form: only the bracketed text is affected
        ParsedText scoped = ColorParser.parse("前%b[粗]后");
        expect("scoped: 3 runs", 3, scoped.runs().size());
        expect("scoped: outside plain", false, scoped.runs().get(0).style().bold());
        expect("scoped: inside bold", true, scoped.runs().get(1).style().bold());
        expect("scoped: after plain", false, scoped.runs().get(2).style().bold());
        expect("scoped: text", "前粗后", scoped.plainText());

        // decorations stack with colour
        ParsedText combo = ColorParser.parse("%#FF0000%b红粗%r");
        expect("combo: colour", 0xFF0000, combo.runs().get(0).style().rgb());
        expect("combo: bold", true, combo.runs().get(0).style().bold());

        // ---- decorations through the render overlay ---------------------------
        com.chlorins.colorapi.render.Overlay decoOverlay =
                com.chlorins.colorapi.render.OverlayScanner.scan("%b[粗]细");
        expect("overlay deco: marker skipped", true, decoOverlay.isSkipped(0));
        expect("overlay deco: inside bold", true,
                (decoOverlay.decorationsOn(3) & com.chlorins.colorapi.render.Overlay.BOLD) != 0);
        expect("overlay deco: after not bold", true,
                (decoOverlay.decorationsOn(5) & com.chlorins.colorapi.render.Overlay.BOLD) == 0);

        // ---- %outline: the border follows the gradient ------------------------
        ParsedText outlined = ColorParser.parse("前%outline[描边]后");
        expect("outline: tags removed", "前描边后", outlined.plainText());
        expect("outline: one merged run", 1, outlined.runs().size());

        com.chlorins.colorapi.render.Overlay outlineOverlay =
                com.chlorins.colorapi.render.OverlayScanner.scan("%#FF0000-#0000FF%outline[边框]");
        expect("outline: detected", true, outlineOverlay != null && outlineOverlay.hasOutline());
        int firstContent = "%#FF0000-#0000FF%outline[".length();
        expect("outline: first char outlined", true, outlineOverlay.isOutlined(firstContent));
        expect("outline: colour behind it", true, outlineOverlay.color(firstContent) != com.chlorins.colorapi.render.Overlay.NO_COLOR);
        expect("outline: no outline outside", false, outlineOverlay.isOutlined(0));

        expect("outline: hasMarkers", true, com.chlorins.colorapi.render.OverlayScanner.hasMarkers("%outline[x]"));

        com.chlorins.colorapi.render.Overlay gradientOutline =
                com.chlorins.colorapi.render.OverlayScanner.scan("%#FF0000-#0000FF%outline[边框]");
        expect("outline after gradient: detected", true,
                gradientOutline != null && gradientOutline.hasOutline());

        // ---- new animations ----------------------------------------------------
        expect("blink kind", Animation.Kind.BLINK, ColorParser.parse("%blink[闪]").runs().get(0).animation().kind());
        expect("fade kind", Animation.Kind.FADE, ColorParser.parse("%fade[淡]").runs().get(0).animation().kind());
        expect("bounce kind", Animation.Kind.BOUNCE, ColorParser.parse("%bounce[跳]").runs().get(0).animation().kind());
        expect("jitter kind", Animation.Kind.JITTER, ColorParser.parse("%jitter[抖]").runs().get(0).animation().kind());

        com.chlorins.colorapi.render.Overlay bounceOverlay =
                com.chlorins.colorapi.render.OverlayScanner.scan("%bounce[ab]");
        expect("bounce: position animated", true, bounceOverlay.hasPosition());
        com.chlorins.colorapi.render.Overlay jitterOverlay =
                com.chlorins.colorapi.render.OverlayScanner.scan("%jitter[ab]");
        expect("jitter: position animated", true, jitterOverlay.hasPosition());
        com.chlorins.colorapi.render.Overlay fadeOverlay =
                com.chlorins.colorapi.render.OverlayScanner.scan("%fade[ab]");
        expect("fade: alpha animated", true, fadeOverlay.hasAnimatedAlpha());
        expect("fade: not position", false, fadeOverlay.hasPosition());

        // deterministic maths
        expect("bounce is upward", true,
                com.chlorins.colorapi.render.AnimationEngine.bounce(0, 123) <= 0.0f);
        expect("jitter deterministic", com.chlorins.colorapi.render.AnimationEngine.jitterX(3, 500),
                com.chlorins.colorapi.render.AnimationEngine.jitterX(3, 500));
        expect("jitter varies with index", true,
                com.chlorins.colorapi.render.AnimationEngine.jitterX(3, 500)
                        != com.chlorins.colorapi.render.AnimationEngine.jitterX(4, 500));
        expect("blink toggles", true,
                com.chlorins.colorapi.render.AnimationEngine.blink(0, 0)
                        != com.chlorins.colorapi.render.AnimationEngine.blink(0, 200));

        // ---- plain text is untouched -----------------------------------------
        String plain = "普通文本 with $ and \\ and & nothing";
        expect("plain text identity", plain, ColorParser.parse(plain).plainText());

        // ---- null / empty -----------------------------------------------------
        expect("null is empty", true, ColorParser.parse(null).isEmpty());
        expect("empty is empty", true, ColorParser.parse("").isEmpty());

        // ---- legacy renderer --------------------------------------------------
        expect("toLegacy solid", "§x§F§F§4§5§0§0传说之刃", ColorParser.parse("%#FF4500传说之刃%r").toLegacy());

        System.out.println();
        System.out.println("passed = " + passed + ", failed = " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static String hex(Run run) {
        return String.format("%06X", run.style().rgb());
    }

    private static void expectRuns(ParsedText actual, List<Run> expected) {
        expect("run count", expected.size(), actual.runs().size());
        for (int i = 0; i < Math.min(expected.size(), actual.runs().size()); i++) {
            expect("run " + i + " text", expected.get(i).text(), actual.runs().get(i).text());
            expect("run " + i + " style", expected.get(i).style(), actual.runs().get(i).style());
        }
    }

    private static void expect(String what, Object expected, Object actual) {
        boolean ok = expected == null ? actual == null : expected.equals(actual);
        if (ok) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL " + what + "\n     expected: " + expected + "\n     actual  : " + actual);
        }
    }

    private static void expect(String what, boolean condition) {
        expect(what, Boolean.TRUE, condition);
    }
}
