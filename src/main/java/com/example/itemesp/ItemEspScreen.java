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
        int hw = 98;
        int lx = cx - 100;
        int rx = cx + 2;
        int step = 22;
        int y = Math.max(24, height / 2 - 80);
        topY = y;

        addDrawableChild(ButtonWidget.builder(enabledText(c), b -> {
            c.enabled = !c.enabled;
            c.save();
            b.setMessage(enabledText(c));
        }).dimensions(lx, y, hw, 20).build());
        addDrawableChild(ButtonWidget.builder(tracerText(c), b -> {
            c.tracers = !c.tracers;
            c.save();
            b.setMessage(tracerText(c));
        }).dimensions(rx, y, hw, 20).build());
        y += step;

        addDrawableChild(ButtonWidget.builder(displayText(c), b -> {
            c.mode = (c.mode + 1) % 3;
            c.save();
            b.setMessage(displayText(c));
        }).dimensions(lx, y, hw, 20).build());
        addDrawableChild(ButtonWidget.builder(modeText(c), b -> {
            c.onlySome = !c.onlySome;
            c.save();
            b.setMessage(modeText(c));
        }).dimensions(rx, y, hw, 20).build());
        y += step;

        addDrawableChild(ButtonWidget.builder(colorModeText(c), b -> {
            c.dynamicColor = !c.dynamicColor;
            c.save();
            b.setMessage(colorModeText(c));
        }).dimensions(lx, y, hw, 20).build());
        TextFieldWidget colorField = new TextFieldWidget(textRenderer, rx, y, hw, 20, Text.empty());
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
        y += step;

        addDrawableChild(new ValueSlider(lx, y, hw, 20, "Tầm xa", 256, c.maxDistance, v -> {
            c.maxDistance = v;
            c.save();
        }));
        addDrawableChild(new ValueSlider(rx, y, hw, 20, "Mờ từ", 256, c.fadeDistance, v -> {
            c.fadeDistance = v;
            c.save();
        }));
        y += step;

        idField = new TextFieldWidget(textRenderer, lx, y, 140, 20, Text.empty());
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
        y += step;

        listY = y;
        y += 14;

        addDrawableChild(ButtonWidget.builder(Text.literal("Xong"), b -> close())
                .dimensions(lx, y, 200, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int cx = width / 2;
        context.drawCenteredTextWithShadow(textRenderer, title, cx, topY - 12, 0xFFFFFF);
        String line = "Danh sách: " + ItemEspClient.config.items.size() + " item"
                + (status.isEmpty() ? "" : " | " + status);
        context.drawCenteredTextWithShadow(textRenderer, line, cx, listY + 3, 0xAAAAAA);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    private static Text enabledText(ItemEspConfig c) {
        return Text.literal("ItemESP: " + (c.enabled ? "BẬT" : "TẮT"));
    }

    private static Text tracerText(ItemEspConfig c) {
        return Text.literal("Tracer: " + (c.tracers ? "BẬT" : "TẮT"));
    }

    private static Text displayText(ItemEspConfig c) {
        String[] names = {"Viền", "Name tag", "Cả hai"};
        return Text.literal("Kiểu: " + names[Math.floorMod(c.mode, 3)]);
    }

    private static Text modeText(ItemEspConfig c) {
        return Text.literal("Lọc: " + (c.onlySome ? "Danh sách" : "Tất cả"));
    }

    private static Text colorModeText(ItemEspConfig c) {
        return Text.literal("Màu: " + (c.dynamicColor ? "Động" : "Tĩnh"));
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
