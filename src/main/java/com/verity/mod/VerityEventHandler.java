package com.verity.mod;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mod.EventBusSubscriber(modid = VerityMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class VerityEventHandler {

    private static final GeminiClient geminiClient = new GeminiClient();

    // Track welcomed players (reset on server restart)
    private static final Set<String> welcomedPlayers = new HashSet<>();

    // Patterns for special AI commands
    private static final Pattern GIVE_PATTERN =
        Pattern.compile("GIVE_ITEM:([a-z_0-9:]+):(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern TELEPORT_PATTERN =
        Pattern.compile("TELEPORT_STRUCTURE:([a-z_0-9:/]+)", Pattern.CASE_INSENSITIVE);

    // ─── Chat formatting helpers ─────────────────────────────────

    private static MutableComponent verityPrefix() {
        return Component.literal("[Verity] ")
            .withStyle(ChatFormatting.AQUA);
    }

    private static void sendVerity(ServerPlayer player, String message) {
        // Split long messages into chunks
        int maxLen = 180;
        while (message.length() > maxLen) {
            int splitAt = message.lastIndexOf(' ', maxLen);
            if (splitAt == -1) splitAt = maxLen;
            String chunk = message.substring(0, splitAt);
            message = message.substring(splitAt).trim();
            player.sendSystemMessage(
                verityPrefix().append(
                    Component.literal(chunk).withStyle(ChatFormatting.WHITE)
                )
            );
        }
        if (!message.isBlank()) {
            player.sendSystemMessage(
                verityPrefix().append(
                    Component.literal(message).withStyle(ChatFormatting.WHITE)
                )
            );
        }
    }

    // ─── Player Join Event ───────────────────────────────────────

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!VerityConfig.SHOW_WELCOME_MESSAGE.get()) return;

        String playerName = player.getName().getString();
        if (welcomedPlayers.contains(playerName)) return;
        welcomedPlayers.add(playerName);

        // Broadcast Verity joining to everyone
        player.getServer().getPlayerList().broadcastSystemMessage(
            Component.literal("✨ Verity ").withStyle(ChatFormatting.AQUA)
                .append(Component.literal("has joined the world!").withStyle(ChatFormatting.GRAY)),
            false
        );

        // Delay personal welcome by 2 seconds (40 ticks)
        // We use a server tick timer trick via a simple thread sleep
        Thread welcomeThread = new Thread(() -> {
            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            player.getServer().execute(() -> {
                sendVerity(player, "Hi! I'm Verity, your personal Helper Friend! 🌟");
                sendVerity(player, "Ask me anything — I know everything about Minecraft!");
                player.sendSystemMessage(
                    Component.literal("Type ").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal("!verity <question>").withStyle(ChatFormatting.AQUA))
                        .append(Component.literal(" to talk to me!").withStyle(ChatFormatting.GRAY))
                );
                player.sendSystemMessage(
                    Component.literal("E.g: ").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal("!verity where is diamond").withStyle(ChatFormatting.AQUA))
                        .append(Component.literal(" | ").withStyle(ChatFormatting.DARK_GRAY))
                        .append(Component.literal("!verity give me 5 diamonds").withStyle(ChatFormatting.AQUA))
                );
            });
        }, "Verity-Welcome");
        welcomeThread.setDaemon(true);
        welcomeThread.start();
    }

    // ─── Chat Event (listen for !verity command) ─────────────────

    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        String message = event.getMessage().getString();
        String prefix = VerityConfig.COMMAND_PREFIX.get();

        if (!message.toLowerCase().startsWith(prefix.toLowerCase())) return;

        // Cancel the original chat message so it doesn't appear publicly
        event.setCanceled(true);

        String question = message.substring(prefix.length()).trim();
        if (question.isBlank()) {
            sendVerity(player, "Ask me something! 😊 E.g.: !verity where do I find diamonds?");
            return;
        }

        // Show the question to everyone in a styled way
        player.getServer().getPlayerList().broadcastSystemMessage(
            Component.literal("[" + player.getName().getString() + " → Verity]: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(question).withStyle(ChatFormatting.WHITE)),
            false
        );

        sendVerity(player, "💭 Thinking...");

        // Call Gemini API async
        geminiClient.ask(player.getName().getString(), question, new GeminiClient.Callback() {
            @Override
            public void onResponse(String response) {
                // Back on server thread
                player.getServer().execute(() -> {
                    String cleaned = processSpecialCommands(player, response);
                    if (!cleaned.isBlank()) {
                        sendVerity(player, cleaned.trim());
                    }
                });
            }

            @Override
            public void onError(String error) {
                player.getServer().execute(() -> {
                    if ("NO_API_KEY".equals(error)) {
                        sendVerity(player, "⚠️ No API key! Open config/verity.toml and add your Gemini API key.");
                        sendVerity(player, "Free key: https://aistudio.google.com/app/apikey");
                    } else {
                        sendVerity(player, "Oops! I couldn't connect to Gemini. Check your internet and API key!");
                        VerityMod.LOGGER.error("[Verity] Error: " + error);
                    }
                });
            }
        });
    }

    // ─── Process special AI commands ─────────────────────────────

    private static String processSpecialCommands(ServerPlayer player, String aiResponse) {
        String cleaned = aiResponse;

        // ── Give items ──────────────────────────────────────────
        if (VerityConfig.ALLOW_ITEM_GIVING.get()) {
            Matcher giveMatcher = GIVE_PATTERN.matcher(aiResponse);
            StringBuffer sb = new StringBuffer();
            while (giveMatcher.find()) {
                String itemId = giveMatcher.group(1).toLowerCase();
                int amount = Math.min(Integer.parseInt(giveMatcher.group(2)), 64);

                // Handle both "diamond" and "minecraft:diamond" formats
                String fullId = itemId.contains(":") ? itemId : "minecraft:" + itemId;
                ResourceLocation rl = new ResourceLocation(fullId);
                Item item = ForgeRegistries.ITEMS.getValue(rl);

                if (item != null && !item.equals(net.minecraft.world.item.Items.AIR)) {
                    ItemStack stack = new ItemStack(item, amount);
                    boolean added = player.getInventory().add(stack);
                    if (!added) {
                        // Drop at player feet if inventory full
                        player.drop(stack, false);
                    }
                    String displayName = itemId.replace("minecraft:", "").replace("_", " ");
                    sendVerity(player, "✨ Here you go! I gave you " + amount + "x " + displayName + "!");
                } else {
                    sendVerity(player, "Hmm, I don't know that item. Try a valid Minecraft item name!");
                }
                giveMatcher.appendReplacement(sb, "");
            }
            giveMatcher.appendTail(sb);
            cleaned = sb.toString().trim();
        }

        // ── Teleport to structure ───────────────────────────────
        if (VerityConfig.ALLOW_TELEPORT.get()) {
            Matcher tpMatcher = TELEPORT_PATTERN.matcher(cleaned);
            if (tpMatcher.find()) {
                String structureId = tpMatcher.group(1);
                cleaned = tpMatcher.replaceAll("").trim();
                teleportToStructure(player, structureId);
            }
        }

        return cleaned;
    }

    private static void teleportToStructure(ServerPlayer player, String structureId) {
        try {
            ServerLevel level = player.serverLevel();
            CommandSourceStack source = player.createCommandSourceStack().withSuppressedOutput().withMaximumPermission(4);

            // Use the /locate command internally
            String locateCmd = "locate structure " + structureId;
            player.getServer().getCommands().performPrefixedCommand(source, locateCmd);

            sendVerity(player, "🏘️ I'm searching for the nearest " +
                structureId.replace("minecraft:", "").replace("_", " ") + "...");
            sendVerity(player, "Check the chat above for coordinates, then use /tp to get there!");

        } catch (Exception e) {
            sendVerity(player, "I couldn't locate that structure. Make sure cheats are enabled!");
            VerityMod.LOGGER.error("[Verity] Teleport error: " + e.getMessage());
        }
    }

    // ─── Player Leave ─────────────────────────────────────────────

    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Clear conversation history when player leaves to save memory
            geminiClient.clearHistory(player.getName().getString());
        }
    }
}
