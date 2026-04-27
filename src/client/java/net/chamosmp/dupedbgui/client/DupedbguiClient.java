package net.chamosmp.dupedbgui.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.chamosmp.dupedbgui.client.gui.DupeDBScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;

public class DupedbguiClient implements ClientModInitializer {

    private static KeyMapping openKey;

    @Override
    public void onInitializeClient() {
        openKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.dupedbgui.open",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_F6,
                KeyMapping.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new DupeDBScreen());
                }
            }
        });
    }
}
