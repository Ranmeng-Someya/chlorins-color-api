package com.chlorins.colorapi.command;

import com.chlorins.colorapi.ChlorinsColors;
import com.chlorins.colorapi.client.DebugState;
import com.chlorins.colorapi.compat.ModernUICompat;
import com.chlorins.colorapi.compat.bls.BetterLoadingScreenCompat;
import com.chlorins.colorapi.mc.ColorText;
import com.chlorins.colorapi.parser.ColorParser;
import com.chlorins.colorapi.render.TextPipeline;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Client-side commands:
 *
 * <pre>
 *   /color                     show the help page
 *   /color help                same
 *   /color preview &lt;prefix&gt; &lt;text&gt;   render a preview line in chat
 * </pre>
 *
 * <p>Minecraft command literals are case-sensitive and lower case, so the command is registered as
 * {@code /color}; {@code /Color} will not match.</p>
 */
public final class ColorCommand {

    public static final String[] PREFIXES = {"%", "&", "@", "#"};

    private ColorCommand() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> register(dispatcher));
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("color")
                .executes(context -> help(context.getSource()))
                .then(ClientCommandManager.literal("help")
                        .executes(context -> help(context.getSource())))
                .then(ClientCommandManager.literal("list")
                        .executes(context -> list(context.getSource())))
                .then(ClientCommandManager.literal("test")
                        .executes(context -> test(context.getSource())))
                .then(ClientCommandManager.literal("debug")
                        .executes(context -> toggleDebug(context.getSource())))
                .then(ClientCommandManager.literal("reload")
                        .executes(context -> reload(context.getSource())))
                .then(ClientCommandManager.literal("anim")
                        .then(ClientCommandManager.argument("name", StringArgumentType.word())
                                .executes(context -> anim(context.getSource(),
                                        StringArgumentType.getString(context, "name")))))
                .then(ClientCommandManager.literal("preview")
                        .then(ClientCommandManager.argument("prefix", StringArgumentType.word())
                                .then(ClientCommandManager.argument("text", StringArgumentType.greedyString())
                                        .executes(context -> preview(context.getSource(),
                                                StringArgumentType.getString(context, "prefix"),
                                                StringArgumentType.getString(context, "text")))))));
    }

    // ------------------------------------------------------------------ help

    private static int help(FabricClientCommandSource source) {
        source.sendFeedback(line("help.title", Formatting.AQUA));
        source.sendFeedback(line("help.prefix", Formatting.GRAY));
        source.sendFeedback(line("help.solid", Formatting.GRAY));
        source.sendFeedback(line("help.gradient", Formatting.GRAY));
        source.sendFeedback(line("help.multistop", Formatting.GRAY));
        source.sendFeedback(line("help.animate", Formatting.GRAY));
        source.sendFeedback(line("help.animate2", Formatting.GRAY));
        source.sendFeedback(line("help.outline", Formatting.GRAY));
        source.sendFeedback(line("help.decorate", Formatting.GRAY));
        source.sendFeedback(line("help.alpha", Formatting.GRAY));
        source.sendFeedback(line("help.reset", Formatting.GRAY));
        source.sendFeedback(line("help.key", Formatting.GRAY));
        source.sendFeedback(line("help.more", Formatting.GRAY));
        source.sendFeedback(line("help.preview", Formatting.GRAY));
        source.sendFeedback(compatLine());
        return 1;
    }

    private static Text line(String key, Formatting fallback) {
        return Text.translatable("chlorins_color_api.command." + key).formatted(fallback);
    }

    /** Reports which optional render pipelines are active, straight from the lang file. */
    private static Text compatLine() {
        boolean modernui = ModernUICompat.present();
        boolean bls = BetterLoadingScreenCompat.active();
        if (modernui && bls) {
            return Text.translatable("chlorins_color_api.command.help.compat.modernui")
                    .append(Text.literal("  "))
                    .append(Text.translatable("chlorins_color_api.command.help.compat.bls"));
        }
        if (modernui) {
            return Text.translatable("chlorins_color_api.command.help.compat.modernui");
        }
        if (bls) {
            return Text.translatable("chlorins_color_api.command.help.compat.bls");
        }
        return Text.translatable("chlorins_color_api.command.help.compat.none");
    }

    // ------------------------------------------------------------------ list / test / debug / reload / anim

    private static int list(FabricClientCommandSource source) {
        source.sendFeedback(line("list.animations", Formatting.GRAY));
        source.sendFeedback(line("list.animations2", Formatting.GRAY));
        source.sendFeedback(line("list.decorations", Formatting.GRAY));
        source.sendFeedback(line("list.misc", Formatting.GRAY));
        source.sendFeedback(line("list.commands", Formatting.GRAY));
        return 1;
    }

    private static int test(FabricClientCommandSource source) {
        source.sendFeedback(line("test.title", Formatting.AQUA));
        source.sendFeedback(Text.translatable("chlorins_color_api.debug.sample"));
        source.sendFeedback(Text.translatable("chlorins_color_api.demo.solid"));
        source.sendFeedback(Text.translatable("chlorins_color_api.demo.gradient"));
        source.sendFeedback(Text.translatable("chlorins_color_api.demo.decorate"));
        return 1;
    }

    private static int toggleDebug(FabricClientCommandSource source) {
        DebugState.toggle();
        source.sendFeedback(Text.translatable(DebugState.enabled()
                ? "chlorins_color_api.command.debug.on"
                : "chlorins_color_api.command.debug.off"));
        return 1;
    }

    private static int reload(FabricClientCommandSource source) {
        ChlorinsColors.clearCache();
        TextPipeline.clearCache();
        source.sendFeedback(Text.translatable("chlorins_color_api.command.reload.done")
                .formatted(Formatting.GREEN));
        return 1;
    }

    private static int anim(FabricClientCommandSource source, String name) {
        String key = ANIMATIONS.get(name.toLowerCase());
        if (key == null) {
            source.sendFeedback(Text.translatable("chlorins_color_api.command.anim.unknown", name)
                    .formatted(Formatting.RED));
            return 0;
        }
        source.sendFeedback(Text.translatable("chlorins_color_api.command.anim.header", name)
                .formatted(Formatting.DARK_GRAY));
        source.sendFeedback(Text.translatable(key));
        return 1;
    }

    /** name -> lang key, so every sample lives in the language files. */
    private static final java.util.Map<String, String> ANIMATIONS = java.util.Map.ofEntries(
            java.util.Map.entry("rainbow", "chlorins_color_api.command.anim.rainbow"),
            java.util.Map.entry("wave", "chlorins_color_api.command.anim.wave"),
            java.util.Map.entry("pulse", "chlorins_color_api.command.anim.pulse"),
            java.util.Map.entry("flow", "chlorins_color_api.command.anim.flow"),
            java.util.Map.entry("blink", "chlorins_color_api.command.anim.blink"),
            java.util.Map.entry("fade", "chlorins_color_api.command.anim.fade"),
            java.util.Map.entry("bounce", "chlorins_color_api.command.anim.bounce"),
            java.util.Map.entry("jitter", "chlorins_color_api.command.anim.jitter"));

    // ------------------------------------------------------------------ preview

    private static int preview(FabricClientCommandSource source, String prefix, String text) {
        String chosen = normalizePrefix(prefix);
        if (chosen == null) {
            source.sendFeedback(Text.translatable("chlorins_color_api.command.preview.bad_prefix", prefix)
                    .formatted(Formatting.RED));
            return 0;
        }
        String raw = chosen + text;
        MutableText rendered = ColorText.fromParsed(ChlorinsColors.parse(raw));
        source.sendFeedback(Text.translatable("chlorins_color_api.command.preview.header",
                Text.literal(raw).formatted(Formatting.WHITE)).formatted(Formatting.DARK_GRAY));
        source.sendFeedback(rendered);
        if (ColorParser.hasMarkers(raw)) {
            source.sendFeedback(Text.translatable("chlorins_color_api.command.preview.plain",
                    Text.literal(ChlorinsColors.parse(raw).plainText()).formatted(Formatting.GRAY))
                    .formatted(Formatting.DARK_GRAY));
        }
        return 1;
    }

    /** Accepts {@code %}, {@code &}, {@code @}, {@code #} or their names. */
    static String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return null;
        }
        for (String candidate : PREFIXES) {
            if (candidate.equals(prefix)) {
                return candidate;
            }
        }
        return switch (prefix.toLowerCase()) {
            case "percent", "pct" -> "%";
            case "amp", "and" -> "&";
            case "at" -> "@";
            case "hash" -> "#";
            default -> null;
        };
    }
}
