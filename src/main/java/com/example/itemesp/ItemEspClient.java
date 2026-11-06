package com.example.itemesp;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

public class ItemEspClient implements ClientModInitializer {
    public static ItemEspConfig config = new ItemEspConfig();
    private static KeyBinding openKey;
    private static final Map<Item, Integer> DYNAMIC_CACHE = new HashMap<>();

    @Override
    public void onInitializeClient() {
        config = ItemEspConfig.load();
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.itemesp.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_CONTROL, "category.itemesp"));
        ClientTickEvents.END_CLIENT_TICK.register(ItemEspClient::tick);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommandManager.literal("itemesp")
                        .executes(ctx -> {
                            MinecraftClient mc = MinecraftClient.getInstance();
                            mc.send(() -> mc.setScreen(new ItemEspScreen(null)));
                            return 1;
                        })
                        .then(ClientCommandManager.literal("toggle").executes(ctx -> {
                            config.enabled = !config.enabled;
                            config.save();
                            ctx.getSource().sendFeedback(Text.literal("ItemESP: " + (config.enabled ? "BẬT" : "TẮT")));
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("add")
                                .then(ClientCommandManager.argument("id", StringArgumentType.word()).executes(ctx -> {
                                    String msg = addItem(StringArgumentType.getString(ctx, "id"));
                                    ctx.getSource().sendFeedback(Text.literal(msg));
                                    return 1;
                                })))
                        .then(ClientCommandManager.literal("remove")
                                .then(ClientCommandManager.argument("id", StringArgumentType.word()).executes(ctx -> {
                                    String msg = removeItem(StringArgumentType.getString(ctx, "id"));
                                    ctx.getSource().sendFeedback(Text.literal(msg));
                                    return 1;
                                })))
                        .then(ClientCommandManager.literal("list").executes(ctx -> {
                            ctx.getSource().sendFeedback(Text.literal(
                                    config.items.isEmpty() ? "Danh sách trống." : "Danh sách: " + String.join(", ", config.items)));
                            return 1;
                        }))
        ));
    }

    private static void tick(MinecraftClient mc) {
        if (mc.world == null || mc.player == null) {
            return;
        }
        while (openKey.wasPressed()) {
            mc.setScreen(new ItemEspScreen(null));
        }
        for (Entity e : mc.world.getEntities()) {
            if (e instanceof ItemEntity ie) {
                boolean want = shouldGlow(mc, ie);
                if (ie.isGlowing() != want) {
                    ie.setGlowing(want);
                }
            }
        }
    }

    private static boolean shouldGlow(MinecraftClient mc, ItemEntity ie) {
        ItemEspConfig c = config;
        if (!c.enabled) {
            return false;
        }
        if (c.maxDistance > 0 && mc.player.distanceTo(ie) > c.maxDistance) {
            return false;
        }
        if (c.onlySome) {
            String id = Registries.ITEM.getId(ie.getStack().getItem()).toString();
            return c.items.contains(id);
        }
        return true;
    }

    public static Integer colorFor(ItemEntity ie) {
        ItemEspConfig c = config;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (c == null || !c.enabled || mc.player == null) {
            return null;
        }
        int rgb = c.dynamicColor
                ? DYNAMIC_CACHE.computeIfAbsent(ie.getStack().getItem(), ItemEspClient::hashColor)
                : c.color;
        if (c.maxDistance > 0 && c.fadeDistance > 0 && c.fadeDistance < c.maxDistance) {
            double d = mc.player.distanceTo(ie);
            if (d > c.fadeDistance) {
                double t = Math.min(1.0, (d - c.fadeDistance) / (c.maxDistance - c.fadeDistance));
                double f = 1.0 - 0.75 * t;
                int r = (int) (((rgb >> 16) & 255) * f);
                int g = (int) (((rgb >> 8) & 255) * f);
                int b = (int) ((rgb & 255) * f);
                rgb = (r << 16) | (g << 8) | b;
            }
        }
        return rgb;
    }

    private static int hashColor(Item item) {
        int h = Registries.ITEM.getId(item).toString().hashCode();
        float hue = ((h & 0x7FFFFFFF) % 360) / 360f;
        return MathHelper.hsvToRgb(hue, 0.85f, 1.0f) & 0xFFFFFF;
    }

    public static Identifier parseItem(String s) {
        String t = s.trim().toLowerCase();
        if (t.isEmpty()) {
            return null;
        }
        if (!t.contains(":")) {
            t = "minecraft:" + t;
        }
        Identifier id = Identifier.tryParse(t);
        if (id == null || !Registries.ITEM.containsId(id)) {
            return null;
        }
        return id;
    }

    public static String addItem(String s) {
        Identifier id = parseItem(s);
        if (id == null) {
            return "Item không hợp lệ: " + s;
        }
        config.items.add(id.toString());
        config.save();
        return "Đã thêm " + id;
    }

    public static String removeItem(String s) {
        Identifier id = parseItem(s);
        if (id == null) {
            return "Item không hợp lệ: " + s;
        }
        boolean removed = config.items.remove(id.toString());
        config.save();
        return removed ? "Đã xoá " + id : id + " không có trong danh sách";
    }
          }
