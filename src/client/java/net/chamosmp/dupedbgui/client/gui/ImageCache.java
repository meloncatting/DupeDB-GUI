package net.chamosmp.dupedbgui.client.gui;

import com.mojang.blaze3d.platform.NativeImage;
import net.chamosmp.dupedbgui.client.api.DupeDBApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ImageCache {
    public static class TexInfo {
        public final Identifier id;
        public final int width;
        public final int height;

        TexInfo(Identifier id, int width, int height) {
            this.id = id;
            this.width = width;
            this.height = height;
        }
    }

    private static final Map<String, TexInfo> CACHE = new HashMap<>();
    private static final Set<String> LOADING = ConcurrentHashMap.newKeySet();
    private static int counter = 0;

    public static TexInfo get(String url) {
        return CACHE.get(url);
    }

    public static void load(String url) {
        if (url == null || CACHE.containsKey(url) || LOADING.contains(url)) return;
        LOADING.add(url);

        DupeDBApi.fetchBytes(url).thenAccept(bytes -> {
            if (bytes == null) { LOADING.remove(url); return; }
            try {
                NativeImage image = decode(bytes);
                if (image == null) { LOADING.remove(url); return; }
                int w = image.getWidth();
                int h = image.getHeight();
                int idx = counter++;
                Minecraft.getInstance().execute(() -> {
                    try {
                        DynamicTexture tex = new DynamicTexture(() -> "dupedbgui_img_" + idx, image);
                        Identifier texId = Identifier.fromNamespaceAndPath("dupedbgui", "dynamic/img" + idx);
                        Minecraft.getInstance().getTextureManager().register(texId, tex);
                        CACHE.put(url, new TexInfo(texId, w, h));
                    } finally {
                        LOADING.remove(url);
                    }
                });
            } catch (Exception e) {
                LOADING.remove(url);
            }
        });
    }

    private static NativeImage decode(byte[] bytes) {
        try {
            return NativeImage.read(new ByteArrayInputStream(bytes));
        } catch (Exception ex) {
            try {
                BufferedImage bi = ImageIO.read(new ByteArrayInputStream(bytes));
                if (bi == null) return null;
                BufferedImage rgba = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_ARGB);
                rgba.getGraphics().drawImage(bi, 0, 0, null);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(rgba, "PNG", baos);
                return NativeImage.read(new ByteArrayInputStream(baos.toByteArray()));
            } catch (Exception e2) {
                return null;
            }
        }
    }

    public static boolean isLoading(String url) {
        return url != null && LOADING.contains(url);
    }
}
