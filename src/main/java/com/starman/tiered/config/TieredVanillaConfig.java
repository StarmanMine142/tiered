package com.starman.tiered.config;

import java.util.*;

import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.*;

public class TieredVanillaConfig extends Screen {
    private final Screen parent;
    private final Component screenTitle = Component.translatable("gui.tiered.config");

    protected TieredVanillaConfig(Screen parent) {
        super(Component.translatable("gui.tiered.config"));
        this.parent = parent;
    }

    public static Screen create(Screen parent) {
        return new TieredVanillaConfig(parent);
    }

    @Override
    protected void init() {
        int centerX = this.width / 2 - 100;
        int centerY = this.height / 2 - 60;

        updateToggleButton(centerX, centerY);
        addSelectorButton(centerX, centerY + 24);

        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> {
            TieredConfig.save();
            this.minecraft.setScreen(parent);
        }).pos(centerX, centerY + 72).size(200, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(graphics, mouseX, mouseY, delta);
        super.render(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(this.font, this.screenTitle, this.width / 2, 20, 0xFFFFFF);
    }

    private void updateToggleButton(int x, int y) {
        Component status = TieredConfig.enableReforgeExpCost
                ? Component.translatable("options.on")
                : Component.translatable("options.off");

        Component text = Component.translatable("config.tiered.enable_reforge_experience_cost")
                .append(": ")
                .append(status);

        Button button = Button.builder(text, b -> {
            TieredConfig.enableReforgeExpCost = !TieredConfig.enableReforgeExpCost;
            TieredConfig.save();
            this.clearWidgets();
            this.init();
        }).pos(x, y).size(200, 20).build();

        button.setTooltip(Tooltip.create(
                Component.translatable("config.tiered.enable_reforge_experience_cost.comment")
        ));

        this.addRenderableWidget(button);
    }

    private void addSelectorButton(int x, int y) {
        String currentType = TieredConfig.selectedConfigType;
        if (currentType == null || currentType.isEmpty()) {
            currentType = "none";
        }

        List<String> options = new ArrayList<>();
        options.add("vanilla");
        if (FabricLoader.getInstance().isModLoaded("cloth-config2")) {
            options.add("cloth");
        }
        if (FabricLoader.getInstance().isModLoaded("yet_another_config_lib_v3")) {
            options.add("yacl");
        }
        options.add("none");

        if (!options.contains(currentType)) {
            currentType = "none";
            TieredConfig.resetSelection();
        }

        CycleButton<String> selectorButton = CycleButton.builder((String value) ->
                        Component.translatable("config.tiered.type." + value)
                )
                .withValues(options)
                .withInitialValue(currentType)
                .withTooltip((String value) -> Tooltip.create(Component.translatable("config.tiered.selector_mode.tooltip")))
                .create(
                        x, y, 200, 20,
                        Component.translatable("config.tiered.selector_mode"),
                        (CycleButton<String> button, String newValue) -> {
                            TieredConfig.selectedConfigType = newValue;
                            if ("none".equals(newValue)) {
                                TieredConfig.resetSelection();
                            } else {
                                TieredConfig.save();
                            }
                        }
                );

        this.addRenderableWidget(selectorButton);
    }

    @Override
    public void onClose() {
        TieredConfig.save();
        this.minecraft.setScreen(parent);
    }
}