package com.example.itemesp;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

public class ItemEspClient implements ClientModInitializer {
    public static ItemEspConfig config = new ItemEspConfig();
    private static KeyBinding openKey;
    private static final Map<Item, Integer> DYNAMIC_CACHE = new HashMap<>();
    private static final Map<Integer, String> LABELED = new HashMap<>();
    private static ClientWorld lastWorld;

    @Override
    public void onInitializeClient() {
        config = ItemEspConfig.load();
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.itemesp.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_CONTROL, "category.itemesp"));
        ClientTickEvents.END_CLIENT_TICK.register(ItemEspClient::tick);
        HudRenderCallback.EVENT.register((ctx, tickCounter) -> renderTracers(ctx));

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
        if (mc.world != lastWorld) {
            LABELED.clear();
            lastWorld = mc.world;
        }
        while (openKey.wasPressed()) {
            mc.setScreen(new ItemEspScreen(null));
        }
        ItemEspConfig c = config;
        for (Entity e : mc.world.getEntities()) {
            if (e instanceof ItemEntity ie) {
                boolean want = shouldShow(mc, ie);
                boolean glow = want && (c.mode == 0 || c.mode == 2);
                if (ie.isGlowing() != glow) {
                    ie.setGlowing(glow);
                }
                boolean tag = want && (c.mode == 1 || c.mode == 2);
                updateTag(ie, tag);
            }
        }
    }

    private static void updateTag(ItemEntity ie, boolean tag) {
        int id = ie.getId();
        if (tag) {
            int rgb = baseColor(ie);
            ItemStack stack = ie.getStack();
            String text = stack.getName().getString() + (stack.getCount() > 1 ? " x" + stack.getCount() : "");
            String key = text + "#" + rgb;
            if (!key.equals(LABELED.get(id))) {
                ie.setCustomName(Text.literal(text).styled(st -> st.withColor(rgb)));
                ie.setCustomNameVisible(true);
                LABELED.put(id, key);
            }
        } else if (LABELED.remove(id) != null) {
            ie.setCustomName(null);
            ie.setCustomNameVisible(false);
        }
    }

    private static void renderTracers(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ItemEspConfig c = config;
        if (!c.enabled || !c.tracers || mc.world == null || mc.player == null) {
            return;
        }
        Camera cam = mc.gameRenderer.getCamera();
        Vec3d cp = cam.getPos();
        double yaw = Math.toRadians(cam.getYaw());
        double pitch = Math.toRadians(cam.getPitch());
        double sy = Math.sin(yaw);
        double cy = Math.cos(yaw);
        double sp = Math.sin(pitch);
        double cpi = Math.cos(pitch);
        double fx = -sy * cpi;
        double fy = -sp;
        double fz = cy * cpi;
        double rx = -cy;
        double rz = -sy;
        double ux = -sy * sp;
        double uy = cpi;
        double uz = cy * sp;

        int w = mc.getWindow().getScaledWidth();
        int h = mc.getWindow().getScaledHeight();
        float centerX = w / 2f;
        float centerY = h / 2f;
        double tanHalf = Math.tan(Math.toRadians(mc.options.getFov().getValue()) / 2.0);
        double aspect = (double) w / h;

        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof ItemEntity ie) || !shouldShow(mc, ie)) {
                continue;
            }
            double dx = ie.getX() - cp.x;
            double dy = ie.getY() + ie.getHeight() / 2.0 - cp.y;
            double dz = ie.getZ() - cp.z;
            double z = dx * fx + dy * fy + dz * fz;
            double x = dx * rx + dz * rz;
            double y = dx * ux + dy * uy + dz * uz;

            float ex;
            float ey;
            if (z > 0.05) {
                ex = (float) (centerX + (x / z) / (tanHalf * aspect) * (w / 2.0));
                ey = (float) (centerY - (y / z) / tanHalf * (h / 2.0));
            } else {
                double len = Math.sqrt(x * x + y * y);
                double nx = len < 1e-6 ? 0 : x / len;
                double ny = len < 1e-6 ? 1 : -y / len;
                ex = (float) (centerX + nx * 3000);
                ey = (float) (centerY + ny * 3000);
            }
            float vx = ex - centerX;
            float vy = ey - centerY;
            float m = Math.max(Math.abs(vx), Math.abs(vy));
            if (m > 3000f) {
                ex = centerX + vx * 3000f / m;
                ey = centerY + vy * 3000f / m;
            }
            drawLine(ctx, centerX, centerY, ex, ey, 0xFF000000 | baseColor(ie));
        }
    }

    private static void drawLine(DrawContext ctx, float x1, float y1, float x2, float y2, int argb) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1f) {
            return;
        }
        MatrixStack ms = ctx.getMatrices();
        ms.push();
        ms.translate(x1, y1, 0f);
        ms.multiply(RotationAxis.POSITIVE_Z.rotation((float) Math.atan2(dy, dx)));
        ctx.fill(0, 0, (int) Math.ceil(len), 1, argb);
        ms.pop();
    }

    private static boolean shouldShow(MinecraftClient mc, ItemEntity ie) {
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

    private static int baseColor(ItemEntity ie) {
        ItemEspConfig c = config;
        MinecraftClient mc = MinecraftClient.getInstance();
        int rgb = c.dynamicColor
                ? DYNAMIC_CACHE.computeIfAbsent(ie.getStack().getItem(), ItemEspClient::hashColor)
                : c.color;
        if (mc.player != null && c.maxDistance > 0 && c.fadeDistance > 0 && c.fadeDistance < c.maxDistance) {
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

    public static Integer colorFor(ItemEntity ie) {
        ItemEspConfig c = config;
        if (c == null || !c.enabled || c.mode == 1 || MinecraftClient.getInstance().player == null) {
            return null;
        }
        return baseColor(ie);
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
