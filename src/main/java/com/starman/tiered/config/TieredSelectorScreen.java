package com.starman.tiered.config;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.fabricmc.loader.api.FabricLoader;

public class TieredSelectorScreen extends Screen {
    private final Screen parent;
    private final Component screenTitle = Component.translatable("gui.tiered.selector.title");

    public TieredSelectorScreen(Screen parent) {
        super(Component.translatable("gui.tiered.selector.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2 - 100;
        int startY = this.height / 2 - 60;

        addConfigButton("Cloth Config", "cloth", "cloth-config2", centerX, startY);
        addConfigButton("YetAnotherConfigLib", "yacl", "yet_another_config_lib_v3", centerX, startY + 24);
        addConfigButton("Vanilla", "vanilla", "", centerX, startY + 48);

        this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> this.minecraft.setScreen(parent))
                .pos(centerX, startY + 84).size(200, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(graphics, mouseX, mouseY, delta);
        super.render(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(this.font, this.screenTitle, this.width / 2, 20, 0xFFFFFF);
    }

    private void addConfigButton(String displayName, String type, String modId, int x, int y) {
        boolean available = modId.isEmpty() || FabricLoader.getInstance().isModLoaded(modId);

        Component buttonText = Component.translatable("config.tiered.type." + type);

        Button button = Button.builder(buttonText, b -> {
            TieredConfig.selectedConfigType = type;
            TieredConfig.save();
            this.minecraft.setScreen(TieredConfig.getPreferredScreen(this.parent));
        }).pos(x, y).size(200, 20).build();

        button.active = available;
        if (!available) {
            button.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                    Component.translatable("config.tiered.tooltip.mod_required", modId)
            ));
        }

        this.addRenderableWidget(button);
    }
}