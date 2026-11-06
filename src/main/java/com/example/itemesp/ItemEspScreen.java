package com.example.itemesp;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.function.DoubleConsumer;

public class ItemEspScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget idField;
    private int topY;
    private int colorY;
    private int listY;
    private String status = "";

    public ItemEspScreen(Screen parent) {
        super(Text.literal("ItemESP"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ItemEspConfig c = ItemEspClient.config;
        int cx = width / 2;
        int w = 200;
        int y = Math.max(34, height / 2 - 98);
        topY = y;

        addDrawableChild(ButtonWidget.builder(enabledText(c), b -> {
            c.enabled = !c.enabled;
            c.save();
            b.setMessage(enabledText(c));
        }).dimensions(cx - w / 2, y, w, 20).build());
        y += 24;

        addDrawableChild(ButtonWidget.builder(modeText(c), b -> {
            c.onlySome = !c.onlySome;
            c.save();
            b.setMessage(modeText(c));
        }).dimensions(cx - w / 2, y, w, 20).build());
        y += 24;

        addDrawableChild(ButtonWidget.builder(colorModeText(c), b -> {
            c.dynamicColor = !c.dynamicColor;
            c.save();
            b.setMessage(colorModeText(c));
        }).dimensions(cx - w / 2, y, w, 20).build());
        y += 24;

        colorY = y;
        TextFieldWidget colorField = new TextFieldWidget(textRenderer, cx + 4, y, w / 2 - 4, 20, Text.empty());
        colorField.setMaxLength(7);
        colorField.setText(String.format("%06X", c.color));
        colorField.setChangedListener(s -> {
            try {
                c.color = Integer.parseInt(s.replace("#", ""), 16) & 0xFFFFFF;
                c.save();
            } catch (NumberFormatException ignored) {
            }
        });
        addDrawableChild(colorField);
        y += 24;

        addDrawableChild(new ValueSlider(cx - w / 2, y, w, 20, "Tầm tối đa (0 = vô hạn)", 256, c.maxDistance, v -> {
            c.maxDistance = v;
            c.save();
        }));
        y += 24;

        addDrawableChild(new ValueSlider(cx - w / 2, y, w, 20, "Bắt đầu mờ", 256, c.fadeDistance, v -> {
            c.fadeDistance = v;
            c.save();
        }));
        y += 24;

        idField = new TextFieldWidget(textRenderer, cx - w / 2, y, 140, 20, Text.empty());
        idField.setMaxLength(100);
        addDrawableChild(idField);
        addDrawableChild(ButtonWidget.builder(Text.literal("+"), b -> {
            status = ItemEspClient.addItem(idField.getText());
            idField.setText("");
        }).dimensions(cx + 44, y, 26, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("-"), b -> {
            status = ItemEspClient.removeItem(idField.getText());
            idField.setText("");
        }).dimensions(cx + 74, y, 26, 20).build());
        y += 24;
        listY = y;
        y += 24;

        addDrawableChild(ButtonWidget.builder(Text.literal("Xong"), b -> close())
                .dimensions(cx - w / 2, y, w, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int cx = width / 2;
        context.drawCenteredTextWithShadow(textRenderer, title, cx, topY - 16, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, "Màu tĩnh (hex):", cx - 100, colorY + 6, 0xFFFFFF);
        String line = "Danh sách: " + ItemEspClient.config.items.size() + " item"
                + (status.isEmpty() ? "" : " | " + status);
        context.drawCenteredTextWithShadow(textRenderer, line, cx, listY + 6, 0xAAAAAA);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    private static Text enabledText(ItemEspConfig c) {
        return Text.literal("ItemESP: " + (c.enabled ? "BẬT" : "TẮT"));
    }

    private static Text modeText(ItemEspConfig c) {
        return Text.literal("Lọc: " + (c.onlySome ? "Theo danh sách" : "Tất cả"));
    }

    private static Text colorModeText(ItemEspConfig c) {
        return Text.literal("Màu: " + (c.dynamicColor ? "Động (theo item)" : "Tĩnh"));
    }

    private static class ValueSlider extends SliderWidget {
        private final String name;
        private final double max;
        private final DoubleConsumer onChange;

        ValueSlider(int x, int y, int w, int h, String name, double max, double value, DoubleConsumer onChange) {
            super(x, y, w, h, Text.empty(), Math.min(1.0, value / max));
            this.name = name;
            this.max = max;
            this.onChange = onChange;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.literal(name + ": " + (int) Math.round(value * max)));
        }

        @Override
        protected void applyValue() {
            onChange.accept(Math.round(value * max));
        }
    }
}
