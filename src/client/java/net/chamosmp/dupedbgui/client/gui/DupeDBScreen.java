package net.chamosmp.dupedbgui.client.gui;

import net.chamosmp.dupedbgui.client.api.DupeDBApi;
import net.chamosmp.dupedbgui.client.api.Exploit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class DupeDBScreen extends Screen {
    private static final int BG         = 0xFF0D0D0D;
    private static final int CARD_BG    = 0xFF1A1A1A;
    private static final int CARD_HOVER = 0xFF242424;
    private static final int ACCENT     = 0xFF1BD96A;
    private static final int VERIFIED_C = 0xFF1BD96A;
    private static final int PATCHED_C  = 0xFFE05555;
    private static final int UNVERIF_C  = 0xFFAAAAAA;
    private static final int HEADER_H   = 58;
    private static final int CARD_H     = 56;
    private static final int CARD_GAP   = 3;
    private static final int THUMB_W    = 80;
    private static final int THUMB_H    = 45;
    private static final int LIST_MAX_W = 620;

    private List<Exploit> allExploits      = new ArrayList<>();
    private List<Exploit> filteredExploits = new ArrayList<>();
    private EditBox searchField;

    private boolean loading     = true;
    private boolean loadingMore = false;
    private String  loadError   = null;
    private double  scrollY     = 0;
    private int     maxScroll   = 0;
    private int     currentPage = 1;
    private int     totalPages  = 1;
    private String  sortMode    = "date_submitted";
    private String  sortOrder   = "desc";

    private static final String[] SORT_LABELS = {"Newest", "Oldest", "Top Rated", "A-Z"};

    public DupeDBScreen() {
        super(Component.literal("DupeDB"));
    }

    @Override
    protected void init() {
        int cx = width / 2;

        searchField = new EditBox(font, cx - 140, 8, 280, 18, Component.empty());
        searchField.setHint(Component.literal("Search exploits..."));
        searchField.setResponder(q -> {
            applyFilter(q);
        });
        addRenderableWidget(searchField);

        int btnW = 64;
        int totalBW = SORT_LABELS.length * btnW + (SORT_LABELS.length - 1) * 3;
        int bx = cx - totalBW / 2;
        for (int i = 0; i < SORT_LABELS.length; i++) {
            final int idx = i;
            addRenderableWidget(
                Button.builder(Component.literal(SORT_LABELS[i]), b -> applySort(idx))
                      .bounds(bx + i * (btnW + 3), 30, btnW, 16)
                      .build()
            );
        }

        addRenderableWidget(
            Button.builder(Component.literal("Token"), b -> minecraft.setScreen(new TokenScreen(this)))
                  .bounds(8, 30, 50, 16).build()
        );

        if (allExploits.isEmpty()) fetchPage(1, false);
    }

    private void applySort(int idx) {
        switch (idx) {
            case 0 -> { sortMode = "date_submitted"; sortOrder = "desc"; }
            case 1 -> { sortMode = "date_submitted"; sortOrder = "asc"; }
            case 2 -> { sortMode = "upvotes";        sortOrder = "desc"; }
            case 3 -> { sortMode = "name";           sortOrder = "asc"; }
        }
        allExploits.clear();
        filteredExploits.clear();
        scrollY = 0;
        currentPage = 1;
        loading = true;
        loadError = null;
        fetchPage(1, false);
    }

    private void fetchPage(int page, boolean append) {
        DupeDBApi.fetchExploits(page, sortMode, sortOrder).thenAccept(res -> {
            Minecraft.getInstance().execute(() -> {
                loading = false;
                loadingMore = false;
                if (res == null) {
                    if (!append) loadError = "Failed to connect to DupeDB";
                    return;
                }
                if (!append) allExploits.clear();
                allExploits.addAll(res.exploits);
                currentPage = res.page;
                totalPages  = res.totalPages;

                for (Exploit e : res.exploits) {
                    String url = e.getThumbnailUrl();
                    if (url != null) ImageCache.load(url);
                    String ytUrl = e.getYoutubeThumbnailUrl();
                    if (ytUrl != null) ImageCache.load(ytUrl);
                }

                applyFilter(searchField != null ? searchField.getValue() : "");
            });
        });
    }

    private void applyFilter(String raw) {
        String q = raw.toLowerCase(Locale.ROOT).trim();
        if (q.isEmpty()) {
            filteredExploits = new ArrayList<>(allExploits);
        } else {
            filteredExploits = allExploits.stream()
                    .filter(e -> e.getDisplayName().toLowerCase(Locale.ROOT).contains(q)
                              || (e.author != null && e.author.toLowerCase(Locale.ROOT).contains(q))
                              || (e.type   != null && e.type.toLowerCase(Locale.ROOT).contains(q)))
                    .collect(Collectors.toList());
        }
        recalcScroll();
    }

    private void recalcScroll() {
        int listH    = height - HEADER_H;
        int contentH = filteredExploits.size() * (CARD_H + CARD_GAP);
        maxScroll = Math.max(0, contentH - listH);
        scrollY   = Math.min(scrollY, maxScroll);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        g.fill(0, 0, width, height, BG);
        g.fill(0, HEADER_H - 1, width, HEADER_H, 0xFF2A2A2A);

        super.render(g, mx, my, delta);

        g.drawString(font, "§a§lDupeDB", 8, 12, ACCENT, true);

        if (loading && allExploits.isEmpty()) {
            g.drawCenteredString(font, "§7Loading exploits...", width / 2, height / 2, 0xFFFFFFFF);
            return;
        }
        if (loadError != null && allExploits.isEmpty()) {
            g.drawCenteredString(font, "§c" + loadError, width / 2, height / 2, 0xFFFFFFFF);
            return;
        }
        if (filteredExploits.isEmpty()) {
            g.drawCenteredString(font, "§7No exploits found", width / 2, height / 2, 0xFFFFFFFF);
            return;
        }

        int listW = Math.min(width - 16, LIST_MAX_W);
        int listX = (width - listW) / 2;

        g.enableScissor(0, HEADER_H, width, height);

        int baseY = HEADER_H - (int) scrollY;
        for (int i = 0; i < filteredExploits.size(); i++) {
            Exploit e  = filteredExploits.get(i);
            int     cy = baseY + i * (CARD_H + CARD_GAP);
            if (cy + CARD_H < HEADER_H || cy > height) continue;
            renderCard(g, e, listX, cy, listW, mx, my);
        }

        g.disableScissor();

        if (maxScroll > 0) {
            int sbH  = height - HEADER_H;
            int barH = Math.max(20, (int) ((double) sbH / (sbH + maxScroll) * sbH));
            int barY = HEADER_H + (int) (scrollY / maxScroll * (sbH - barH));
            g.fill(width - 5, HEADER_H, width - 1, height, 0xFF1A1A1A);
            g.fill(width - 5, barY, width - 1, barY + barH, 0xFF555555);
        }

        if (loadingMore) {
            g.drawCenteredString(font, "§7Loading more...", width / 2, height - 10, 0xFFFFFFFF);
        }
    }

    private void renderCard(GuiGraphics g, Exploit e, int x, int y, int w, int mx, int my) {
        boolean hovered = mx >= x && mx < x + w && my >= y && my < y + CARD_H;
        g.fill(x, y, x + w, y + CARD_H, hovered ? CARD_HOVER : CARD_BG);
        g.fill(x, y + CARD_H - 1, x + w, y + CARD_H, 0xFF222222);

        int thumbX = x + 8;
        int thumbY = y + (CARD_H - THUMB_H) / 2;

        String thumbUrl = e.getThumbnailUrl();
        if (thumbUrl == null) thumbUrl = e.getYoutubeThumbnailUrl();
        boolean thumbFullyVisible = isRectFullyVisible(thumbX, thumbY, THUMB_W, THUMB_H);
        if (thumbUrl != null && thumbFullyVisible) {
            ImageCache.load(thumbUrl);
            ImageCache.TexInfo info = ImageCache.get(thumbUrl);
            if (info != null) {
                g.blit(info.id, thumbX, thumbY, THUMB_W, THUMB_H, 0f, 0f, 1f, 1f);
            } else {
                g.fill(thumbX, thumbY, thumbX + THUMB_W, thumbY + THUMB_H, 0xFF161616);
            }
        } else if (thumbUrl == null) {
            g.fill(thumbX, thumbY, thumbX + THUMB_W, thumbY + THUMB_H, 0xFF161616);
        }

        int tx = thumbX + THUMB_W + 10;
        int ty = y + 8;

        String name = e.getDisplayName();
        int maxNameW = w - (tx - x) - 55;
        if (font.width(name) > maxNameW) name = truncate(name, maxNameW);
        g.drawString(font, name, tx, ty, 0xFFFFFFFF, true);

        int sc = switch (e.status != null ? e.status : "") {
            case "verified" -> VERIFIED_C;
            case "patched"  -> PATCHED_C;
            default         -> UNVERIF_C;
        };
        String typeStr   = e.getTypeLabel();
        String statusStr = e.getStatusLabel();
        int    typeW     = font.width(typeStr);
        g.drawString(font, typeStr,   tx,                              ty + 13, 0xFF888888, false);
        g.drawString(font, " • ",     tx + typeW,                      ty + 13, 0xFF444444, false);
        g.drawString(font, statusStr, tx + typeW + font.width(" • "),  ty + 13, sc,         false);

        String meta = "by " + (e.author != null ? e.author : "?") + "  " + fmtDate(e.dateSubmitted);
        g.drawString(font, meta, tx, ty + 25, 0xFF555555, false);

        String votes = "▲ " + e.upvotes;
        g.drawString(font, votes, x + w - font.width(votes) - 10, y + (CARD_H - 8) / 2, 0xFF3D8A3D, false);
    }

    private boolean isRectFullyVisible(int x, int y, int w, int h) {
        return x >= 0 && x + w <= width && y >= HEADER_H && y + h <= height;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmt, double vAmt) {
        if (my > HEADER_H) {
            scrollY = Math.max(0, Math.min(maxScroll, scrollY - vAmt * 14));
            if (!loadingMore && currentPage < totalPages && scrollY >= maxScroll - 60) {
                loadingMore = true;
                fetchPage(currentPage + 1, true);
            }
            return true;
        }
        return super.mouseScrolled(mx, my, hAmt, vAmt);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean inGUI) {
        double mx = event.x();
        double my = event.y();
        if (my > HEADER_H) {
            int listW = Math.min(width - 16, LIST_MAX_W);
            int listX = (width - listW) / 2;
            int baseY = HEADER_H - (int) scrollY;

            for (int i = 0; i < filteredExploits.size(); i++) {
                int cy = baseY + i * (CARD_H + CARD_GAP);
                if (mx >= listX && mx < listX + listW && my >= cy && my < cy + CARD_H) {
                    minecraft.setScreen(new ExploitDetailScreen(filteredExploits.get(i), this));
                    return true;
                }
            }
        }
        return super.mouseClicked(event, inGUI);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256) { // ESC
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(null);
    }

    private String fmtDate(String iso) {
        if (iso == null || iso.length() < 10) return "?";
        return iso.substring(0, 10);
    }

    private String truncate(String text, int maxPx) {
        if (font.width(text) <= maxPx) return text;
        while (text.length() > 0 && font.width(text + "...") > maxPx) {
            text = text.substring(0, text.length() - 1);
        }
        return text + "...";
    }
}
