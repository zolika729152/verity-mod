package com.verity.mod;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class VerityConfig {

    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // ─── API Key ───────────────────────────────────────────────────
    public static final ForgeConfigSpec.ConfigValue<String> GEMINI_API_KEY;
    public static final ForgeConfigSpec.ConfigValue<String> GEMINI_MODEL;
    public static final ForgeConfigSpec.ConfigValue<String> COMMAND_PREFIX;
    public static final ForgeConfigSpec.BooleanValue SHOW_WELCOME_MESSAGE;
    public static final ForgeConfigSpec.BooleanValue ALLOW_ITEM_GIVING;
    public static final ForgeConfigSpec.BooleanValue ALLOW_TELEPORT;

    static {
        BUILDER.comment("Verity - AI Helper Friend Configuration").push("verity");

        BUILDER.comment("=== Google Gemini API ===").push("api");
        GEMINI_API_KEY = BUILDER
            .comment("Your Google Gemini API key.",
                     "Get a FREE key at: https://aistudio.google.com/app/apikey")
            .define("geminiApiKey", "PUT_YOUR_GEMINI_API_KEY_HERE");

        GEMINI_MODEL = BUILDER
            .comment("Gemini model to use. Free options: gemini-3.5-flash, gemini-2.5-flash")
            .define("geminiModel", "gemini-3.5-flash");
        BUILDER.pop();

        BUILDER.comment("=== Verity Behavior ===").push("behavior");
        COMMAND_PREFIX = BUILDER
            .comment("The prefix players use to talk to Verity in chat.")
            .define("commandPrefix", "!verity");

        SHOW_WELCOME_MESSAGE = BUILDER
            .comment("Show Verity's welcome message when a player joins.")
            .define("showWelcomeMessage", true);

        ALLOW_ITEM_GIVING = BUILDER
            .comment("Allow Verity to give items to players when asked.")
            .define("allowItemGiving", true);

        ALLOW_TELEPORT = BUILDER
            .comment("Allow Verity to teleport players to structures (villages, etc).")
            .define("allowTeleport", true);
        BUILDER.pop();

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, "verity.toml");
    }
}
