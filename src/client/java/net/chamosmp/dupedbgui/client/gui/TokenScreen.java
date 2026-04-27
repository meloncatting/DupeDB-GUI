package net.chamosmp.dupedbgui.client.gui;

import net.chamosmp.dupedbgui.client.ModConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class TokenScreen extends Screen {
    private static final int BG = 0xFF0D0D0D;

    private final Screen parent;
    private EditBox tokenField;

    public TokenScreen(Screen parent) {
        super(Component.literal("DupeDB Token"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int cy = height / 2;

        tokenField = new EditBox(font, cx - 160, cy - 10, 320, 20, Component.empty());
        tokenField.setHint(Component.literal("dupe_..."));
        tokenField.setMaxLength(80);
        String existing = ModConfig.getToken();
        if (existing != null) tokenField.setValue(existing);
        addRenderableWidget(tokenField);

        addRenderableWidget(
            Button.builder(Component.literal("Save"), b -> {
                ModConfig.setToken(tokenField.getValue());
                minecraft.setScreen(parent);
            }).bounds(cx - 82, cy + 18, 78, 20).build()
        );
        addRenderableWidget(
            Button.builder(Component.literal("Clear"), b -> {
                ModConfig.setToken(null);
                tokenField.setValue("");
            }).bounds(cx + 4, cy + 18, 78, 20).build()
        );
        addRenderableWidget(
            Button.builder(Component.literal("< Back"), b -> minecraft.setScreen(parent))
                  .bounds(6, 6, 55, 18).build()
        );
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        g.fill(0, 0, width, height, BG);
        int cx = width / 2;
        int cy = height / 2;

        g.drawCenteredString(font, "§l§aDupeDB Token", cx, cy - 38, 0xFFFFFFFF);
        g.drawCenteredString(font, "§7Paste your §fdupe_...§7 token to access full exploit steps.", cx, cy - 24, 0xFFFFFFFF);
        g.drawCenteredString(font, "§7Get a token by connecting an OAuth app at §9dupedb.net§7.", cx, cy - 14, 0xFFFFFFFF);

        super.render(g, mx, my, delta);

        boolean hasToken = ModConfig.getToken() != null;
        String status = hasToken ? "§aToken set" : "§cNo token — steps will be partial";
        g.drawCenteredString(font, status, cx, cy + 44, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
