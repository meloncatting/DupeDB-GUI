package net.chamosmp.dupedbgui.client.gui;

import net.chamosmp.dupedbgui.client.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class TokenScreen extends Screen {
    private static final int    BG       = 0xFF0D0D0D;
    private static final String AUTH_URL =
        "https://dupedb.net/api/oauth/authorize?app_id=dupedb-gui-mod&redirect_uri=http%3A%2F%2Flocalhost%3A25585%2Fcallback";

    private final Screen parent;
    private EditBox tokenField;

    private enum LoginState { IDLE, WAITING, SUCCESS, ERROR }
    private volatile LoginState loginState = LoginState.IDLE;
    private Thread listenerThread;

    public TokenScreen(Screen parent) {
        super(Component.literal("DupeDB Token"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int cy = height / 2;

        tokenField = new EditBox(font, cx - 160, cy - 10, 320, 20, Component.empty());
        tokenField.setHint(Component.literal("dupe_...  (or use Login button)"));
        tokenField.setMaxLength(80);
        String existing = ModConfig.getToken();
        if (existing != null) tokenField.setValue(existing);
        addRenderableWidget(tokenField);

        addRenderableWidget(
            Button.builder(Component.literal("Login with DupeDB"), b -> startOAuth())
                  .bounds(cx - 160, cy + 14, 150, 20).build()
        );
        addRenderableWidget(
            Button.builder(Component.literal("Save token"), b -> {
                ModConfig.setToken(tokenField.getValue());
                minecraft.setScreen(parent);
            }).bounds(cx - 4, cy + 14, 74, 20).build()
        );
        addRenderableWidget(
            Button.builder(Component.literal("Clear"), b -> {
                ModConfig.setToken(null);
                tokenField.setValue("");
                loginState = LoginState.IDLE;
            }).bounds(cx + 76, cy + 14, 84, 20).build()
        );
        addRenderableWidget(
            Button.builder(Component.literal("< Back"), b -> {
                stopListener();
                minecraft.setScreen(parent);
            }).bounds(6, 6, 55, 18).build()
        );
    }

    private void startOAuth() {
        if (loginState == LoginState.WAITING) return;
        loginState = LoginState.WAITING;
        stopListener();

        listenerThread = new Thread(() -> {
            try (ServerSocket server = new ServerSocket(25585)) {
                server.setSoTimeout(120_000);
                Minecraft.getInstance().execute(() ->
                    Util.getPlatform().openUri(AUTH_URL)
                );
                try (Socket client = server.accept()) {
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
                    String requestLine = reader.readLine();

                    String token = extractToken(requestLine);

                    String body;
                    if (token != null) {
                        body = "<html><body style='background:#0d0d0d;color:#1BD96A;font-family:monospace;text-align:center;padding-top:80px'>"
                             + "<h2>Token saved!</h2><p>You can close this tab and return to Minecraft.</p></body></html>";
                    } else {
                        body = "<html><body style='background:#0d0d0d;color:#e05555;font-family:monospace;text-align:center;padding-top:80px'>"
                             + "<h2>No token found.</h2><p>Try again.</p></body></html>";
                    }

                    PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                    out.println("HTTP/1.1 200 OK");
                    out.println("Content-Type: text/html; charset=UTF-8");
                    out.println("Content-Length: " + body.getBytes(StandardCharsets.UTF_8).length);
                    out.println("Connection: close");
                    out.println();
                    out.print(body);
                    out.flush();

                    if (token != null) {
                        final String t = token;
                        Minecraft.getInstance().execute(() -> {
                            ModConfig.setToken(t);
                            tokenField.setValue(t);
                            loginState = LoginState.SUCCESS;
                        });
                    } else {
                        Minecraft.getInstance().execute(() -> loginState = LoginState.ERROR);
                    }
                }
            } catch (Exception e) {
                Minecraft.getInstance().execute(() -> loginState = LoginState.ERROR);
            }
        }, "dupedb-oauth-listener");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private String extractToken(String requestLine) {
        if (requestLine == null) return null;
        // GET /callback?code=dupe_abc123... HTTP/1.1
        int q = requestLine.indexOf("?code=");
        if (q < 0) return null;
        int end = requestLine.indexOf(' ', q);
        String raw = end < 0 ? requestLine.substring(q + 6) : requestLine.substring(q + 6, end);
        try { raw = URLDecoder.decode(raw, StandardCharsets.UTF_8); } catch (Exception ignored) {}
        return raw.startsWith("dupe_") ? raw : null;
    }

    private void stopListener() {
        if (listenerThread != null && listenerThread.isAlive()) {
            listenerThread.interrupt();
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        g.fill(0, 0, width, height, BG);
        int cx = width / 2;
        int cy = height / 2;

        g.drawCenteredString(font, "§l§aDupeDB Token", cx, cy - 44, 0xFFFFFFFF);
        g.drawCenteredString(font, "§7Click §fLogin with DupeDB§7 to authorize automatically,", cx, cy - 30, 0xFFFFFFFF);
        g.drawCenteredString(font, "§7or paste a §fdupe_...§7 token manually and click §fSave token§7.", cx, cy - 20, 0xFFFFFFFF);

        super.render(g, mx, my, delta);

        String status = switch (loginState) {
            case IDLE    -> ModConfig.getToken() != null ? "§aToken set" : "§cNo token — steps will be partial";
            case WAITING -> "§eWaiting for browser authorization...";
            case SUCCESS -> "§aLogged in successfully!";
            case ERROR   -> "§cAuthorization failed or timed out. Try again.";
        };
        g.drawCenteredString(font, status, cx, cy + 40, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        stopListener();
        minecraft.setScreen(parent);
    }
}
