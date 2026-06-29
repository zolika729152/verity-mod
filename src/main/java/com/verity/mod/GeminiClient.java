package com.verity.mod;

import com.google.gson.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Handles all communication with the Google Gemini API.
 * Runs on a background thread so it never freezes the game.
 */
public class GeminiClient {

    private static final String API_BASE = "https://generativelanguage.googleapis.com/v1beta/models/";

    private static final String SYSTEM_PROMPT =
        "You are Verity, a cheerful and knowledgeable personal helper friend inside Minecraft Java Edition 1.20.1.\n" +
        "You live inside the game and help players with anything they need.\n\n" +
        "Your personality:\n" +
        "- Friendly, enthusiastic, and encouraging\n" +
        "- Speak in short, clear sentences (chat window is small!)\n" +
        "- Use Minecraft-style language naturally\n" +
        "- Add a light emoji or two when appropriate\n\n" +
        "What you CAN help with:\n" +
        "- WHERE to find diamonds: Y-level -58 to -59 is best in 1.20.1, use Fortune III pickaxe\n" +
        "- Item giving: when asked for items, include GIVE_ITEM:<item_id>:<amount> in your response\n" +
        "  Examples: GIVE_ITEM:diamond:5 | GIVE_ITEM:iron_sword:1 | GIVE_ITEM:cooked_beef:16\n" +
        "- Village teleportation: when asked, include TELEPORT_STRUCTURE:minecraft:village\n" +
        "- Crafting recipes, biomes, mobs, tips, strategies\n" +
        "- General Minecraft 1.20.1 knowledge\n\n" +
        "IMPORTANT: Keep responses under 180 characters when possible. " +
        "If longer, put the most important info first.\n" +
        "Item IDs must be valid Minecraft Java Edition 1.20.1 item IDs (e.g. diamond, iron_sword, cooked_beef).";

    // Per-player conversation history
    private final Map<String, List<Map<String, Object>>> histories = new HashMap<>();

    public interface Callback {
        void onResponse(String response);
        void onError(String error);
    }

    /**
     * Ask Verity a question. Runs async, calls callback on the calling thread pool.
     */
    public void ask(String playerName, String question, Callback callback) {
        Thread thread = new Thread(() -> {
            try {
                String response = callGemini(playerName, question);
                callback.onResponse(response);
            } catch (Exception e) {
                VerityMod.LOGGER.error("[Verity] Gemini API error: " + e.getMessage());
                callback.onError(e.getMessage());
            }
        }, "Verity-AI-Thread");
        thread.setDaemon(true);
        thread.start();
    }

    private String callGemini(String playerName, String question) throws Exception {
        String apiKey = VerityConfig.GEMINI_API_KEY.get();
        String model  = VerityConfig.GEMINI_MODEL.get();

        if (apiKey.equals("PUT_YOUR_GEMINI_API_KEY_HERE") || apiKey.isBlank()) {
            throw new Exception("NO_API_KEY");
        }

        // Build conversation history for this player
        histories.computeIfAbsent(playerName, k -> new ArrayList<>());
        List<Map<String, Object>> history = histories.get(playerName);

        // Add user message
        Map<String, Object> userMsg = new LinkedHashMap<>();
        userMsg.put("role", "user");
        userMsg.put("parts", List.of(Map.of("text", question)));
        history.add(userMsg);

        // Keep max 10 turns
        if (history.size() > 10) {
            history.subList(0, history.size() - 10).clear();
        }

        // Build JSON body
        JsonObject body = new JsonObject();

        // System instruction
        JsonObject sysInstruction = new JsonObject();
        JsonArray sysParts = new JsonArray();
        JsonObject sysText = new JsonObject();
        sysText.addProperty("text", SYSTEM_PROMPT);
        sysParts.add(sysText);
        sysInstruction.add("parts", sysParts);
        body.add("system_instruction", sysInstruction);

        // Contents (conversation history)
        JsonArray contents = new Gson().toJsonTree(history).getAsJsonArray();
        body.add("contents", contents);

        // Generation config
        JsonObject genConfig = new JsonObject();
        genConfig.addProperty("maxOutputTokens", 300);
        genConfig.addProperty("temperature", 0.7);
        body.add("generationConfig", genConfig);

        String jsonBody = body.toString();

        // HTTP request
        String urlStr = API_BASE + model + ":generateContent?key=" + apiKey;
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        InputStream is = (status == 200) ? conn.getInputStream() : conn.getErrorStream();
        String responseBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);

        if (status != 200) {
            throw new Exception("HTTP " + status + ": " + responseBody.substring(0, Math.min(200, responseBody.length())));
        }

        // Parse response
        JsonObject resp = JsonParser.parseString(responseBody).getAsJsonObject();
        String aiText = resp
            .getAsJsonArray("candidates")
            .get(0).getAsJsonObject()
            .getAsJsonObject("content")
            .getAsJsonArray("parts")
            .get(0).getAsJsonObject()
            .get("text").getAsString();

        // Save assistant reply to history
        Map<String, Object> assistantMsg = new LinkedHashMap<>();
        assistantMsg.put("role", "model");
        assistantMsg.put("parts", List.of(Map.of("text", aiText)));
        history.add(assistantMsg);

        return aiText;
    }

    public void clearHistory(String playerName) {
        histories.remove(playerName);
    }
}
