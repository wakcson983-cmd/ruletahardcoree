package com.ruleta.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.ruleta.RuletaColor;
import com.ruleta.RuletaMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.RotationAxis;

import java.util.Random;

/** Animación de la ruleta (flash, giro, pointer fijo, gajes que cambian de color) y animación de la cara roja. */
public final class Anim {
    private Anim() {}

    public static final Identifier WHEEL = tex("wheel");
    private static final Identifier POINTER = tex("pointer");
    private static final Identifier GEM = tex("gem");
    private static final Identifier SKULL = tex("skull");
    private static final Identifier[] WHEEL_COLOR = new Identifier[RuletaColor.values().length];

    static {
        for (RuletaColor c : RuletaColor.values()) WHEEL_COLOR[c.ordinal()] = tex("wheel_" + c.id);
    }

    private static Identifier tex(String name) {
        return Identifier.of("ruleta", "textures/gui/" + name + ".png");
    }

    // ---------- estado ruleta ----------
    private static boolean wheelOn;
    private static long wheelStart;
    private static RuletaColor wheelColor = RuletaColor.ROJO;

    // tiempos de la ruleta (ms)
    private static final float W_SPIN_START = 350f;
    private static final float W_LAND = 4000f;     // coincide con 80 ticks del servidor
    private static final float W_FILL_START = 4500f;
    private static final float W_FILL_END = 5700f;
    private static final float W_END = 6400f;

    // ---------- estado animación roja ----------
    private static boolean redOn;
    private static long redStart;
    private static boolean redDeath;
    private static String redName = "";
    private static Text redCause = Text.empty();
    private static float redDur;

    public static void startWheel(int colorIndex) {
        RuletaColor c = RuletaColor.byIndex(colorIndex);
        if (c == null) return;
        wheelColor = c;
        wheelStart = Util.getMeasuringTimeMs();
        wheelOn = true;
    }

    public static void startRed(boolean death, String name, Text cause) {
        redDeath = death;
        redName = name == null ? "" : name;
        redCause = cause == null ? Text.empty() : cause;
        redDur = death ? 5000f : 7000f;
        redStart = Util.getMeasuringTimeMs();
        redOn = true;

        if (!death) {
            MinecraftClient mc = MinecraftClient.getInstance();
            mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1.0f));
            mc.getSoundManager().play(PositionedSoundInstance.master(RuletaMod.RISA, 1.0f, 1.0f));
            mc.getSoundManager().play(PositionedSoundInstance.master(RuletaMod.CANCION, 1.0f, 1.0f));
        }
    }

    public static void render(DrawContext ctx) {
        long now = Util.getMeasuringTimeMs();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        renderWheel(ctx, now);
        renderRed(ctx, now);
    }

    // =====================================================================
    //                              RULETA
    // =====================================================================

    private static float clamp01(float v) {
        return v < 0 ? 0 : Math.min(v, 1f);
    }

    private static float spinProgress(float t) {
        float x = clamp01((t - W_SPIN_START) / (W_LAND - W_SPIN_START));
        return 1f - (float) Math.pow(1f - x, 3.5);
    }

    private static void renderWheel(DrawContext ctx, long now) {
        if (!wheelOn) return;
        float t = now - wheelStart;
        if (t > W_END) {
            wheelOn = false;
            return;
        }
        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();
        int size = (int) (h * 0.5f);
        float cx = w / 2f;
        float cy = h * 0.40f;

        float alpha = clamp01((t - 150f) / 500f) * (1f - clamp01((t - 5800f) / 500f));

        float total = 360f * 6f + (360f - wheelColor.center);
        float angle = total * spinProgress(t);
        float prev = total * spinProgress(t - 45f);
        float vel = angle - prev;                 // grados por ~45 ms

        // destello blanco inicial
        if (t < W_SPIN_START + 150f) {
            float k = clamp01(t / 300f);
            float a = (1f - clamp01((t - 150f) / 350f));
            fillCircle(ctx, cx, cy, size * 0.5f * (0.2f + 0.8f * k), ((int) (230 * a) << 24) | 0xFFFFFF);
        }

        // motion blur muy leve (copias fantasma)
        float blur = Math.min(vel, 40f);
        for (int i = 3; i >= 1; i--) {
            drawRot(ctx, WHEEL, cx, cy, size, angle - blur * 0.35f * i, alpha * 0.16f);
        }
        drawRot(ctx, WHEEL, cx, cy, size, angle, alpha);

        // gajes que se vuelven del color ganador
        float fill = clamp01((t - W_FILL_START) / (W_FILL_END - W_FILL_START));
        if (fill > 0f) {
            drawRot(ctx, WHEEL_COLOR[wheelColor.ordinal()], cx, cy, size, angle, alpha * fill);
        }

        // puntero fijo arriba, con la gema del color que está debajo
        float theta = ((-angle % 360f) + 360f) % 360f;
        int sector = (int) Math.floor(((theta - 7.7f + 360f) % 360f) / 45f) % 8;
        RuletaColor under = RuletaColor.values()[sector];
        if (t >= W_LAND) under = wheelColor;
        drawPointer(ctx, cx, cy - size / 2f + size * 0.05f, size, under.rgb, alpha);

        // destellitos al terminar
        if (t > W_LAND) {
            Random r = new Random((long) (t / 110f) * 977L + 13L);
            float k = alpha * 0.9f;
            for (int i = 0; i < 14; i++) {
                double ang = r.nextDouble() * Math.PI * 2;
                double rad = r.nextDouble() * size * 0.42;
                int sx = (int) (cx + Math.cos(ang) * rad);
                int sy = (int) (cy + Math.sin(ang) * rad);
                int len = 2 + r.nextInt(3);
                int c = ((int) (255 * k) << 24) | 0xFFFFFF;
                ctx.fill(sx - len, sy, sx + len + 1, sy + 1, c);
                ctx.fill(sx, sy - len, sx + 1, sy + len + 1, c);
            }
        }

        // destello blanco final y desaparece
        if (t > 5600f) {
            float k = clamp01((t - 5600f) / 350f);
            float a = k * (1f - clamp01((t - 5950f) / 450f));
            fillCircle(ctx, cx, cy, size * 0.52f, ((int) (235 * a) << 24) | 0xFFFFFF);
        }
    }

    private static void drawRot(DrawContext ctx, Identifier tex, float cx, float cy, int size, float angle, float alpha) {
        if (alpha <= 0.01f) return;
        MatrixStack m = ctx.getMatrices();
        m.push();
        m.translate(cx, cy, 0);
        m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle));
        ctx.setShaderColor(1f, 1f, 1f, alpha);
        ctx.drawTexture(tex, -size / 2, -size / 2, size, size, 0f, 0f, 512, 512, 512, 512);
        ctx.setShaderColor(1f, 1f, 1f, 1f);
        m.pop();
    }

    private static void drawPointer(DrawContext ctx, float cx, float tipY, int size, int gemRgb, float alpha) {
        if (alpha <= 0.01f) return;
        float ph = size * 0.22f;
        float pw = ph * 0.8f;
        float scale = pw / 128f;
        MatrixStack m = ctx.getMatrices();
        m.push();
        m.translate(cx - pw / 2f, tipY - ph, 0);
        ctx.setShaderColor(1f, 1f, 1f, alpha);
        ctx.drawTexture(POINTER, 0, 0, (int) pw, (int) ph, 0f, 0f, 128, 160, 128, 160);
        float gs = 68f * scale;
        float gx = pw / 2f - gs / 2f;
        float gy = 58f * scale - gs / 2f;
        ctx.setShaderColor(((gemRgb >> 16) & 255) / 255f, ((gemRgb >> 8) & 255) / 255f, (gemRgb & 255) / 255f, alpha);
        ctx.drawTexture(GEM, (int) gx, (int) gy, (int) gs, (int) gs, 0f, 0f, 68, 68, 68, 68);
        ctx.setShaderColor(1f, 1f, 1f, 1f);
        m.pop();
    }

    private static void fillCircle(DrawContext ctx, float cx, float cy, float r, int argb) {
        if ((argb >>> 24) <= 2) return;
        int ir = (int) r;
        for (int dy = -ir; dy <= ir; dy++) {
            int hw = (int) Math.sqrt(Math.max(0f, r * r - (float) dy * dy));
            ctx.fill((int) cx - hw, (int) cy + dy, (int) cx + hw, (int) cy + dy + 1, argb);
        }
    }

    // =====================================================================
    //                         ANIMACIÓN ROJA
    // =====================================================================

    private static void renderRed(DrawContext ctx, long now) {
        if (!redOn) return;
        float t = now - redStart;
        if (t > redDur) {
            redOn = false;
            return;
        }
        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();
        float in = clamp01(t / 700f);
        float out = clamp01((redDur - t) / 700f);
        float a = Math.min(in, out);
        boolean edge = t < 700f || t > redDur - 700f;      // entra y sale con muchos rayos
        float ts = t / 1000f;

        // filtro rojo en pantalla (solo cuando cae rojo en la ruleta, no en la muerte)
        if (!redDeath) {
            float f = 0.30f + 0.08f * (float) Math.sin(t * 0.012f);
            ctx.fill(0, 0, w, h, ((int) (255 * f * a) << 24) | 0xC00000);
        }

        // destellos rojos rápidos
        Random fr = new Random((long) (t / 80f) * 31L + 7L);
        if (edge && fr.nextFloat() < 0.35f) {
            ctx.fill(0, 0, w, h, ((int) (110 * a) << 24) | 0xFF3030);
        }

        float cx = w / 2f;
        float cy = redDeath ? h * 0.40f : h * 0.50f;
        float dh = h * (redDeath ? 0.48f : 0.62f);
        float dw = dh * 480f / 336f;

        // rayos rojos alrededor
        int bolts = edge ? 16 : 8;
        Random br = new Random((long) (t / 70f) * 7919L + 3L);
        for (int i = 0; i < bolts; i++) {
            bolt(ctx, br, cx, cy, w, h, dw, dh, a);
        }

        // cara: muy movida, con aretes y hojas moviéndose
        drawSkull(ctx, cx, cy, dw, dh, a, ts, t);

        // texto debajo (solo en la muerte)
        if (redDeath) {
            TextRenderer tr = MinecraftClient.getInstance().textRenderer;
            float ty = cy + dh * 0.5f + 12f;
            int ca = Math.max(4, (int) (255 * a));
            MatrixStack m = ctx.getMatrices();
            m.push();
            m.translate(cx, ty, 0);
            m.scale(2f, 2f, 1f);
            ctx.drawCenteredTextWithShadow(tr,
                    Text.literal(redName + " murió").formatted(Formatting.BOLD),
                    0, 0, (ca << 24) | 0xFFFFFF);
            m.pop();
            ctx.drawCenteredTextWithShadow(tr, redCause, (int) cx, (int) (ty + 26), (ca << 24) | 0xFF7F8F);
        }
    }

    private static void drawSkull(DrawContext ctx, float cx, float cy, float dw, float dh, float a, float ts, float t) {
        final int cols = 24, rows = 24;
        float tw = dw / cols, th = dh / rows;
        float amp = dw * 0.020f;

        Random gr = new Random((long) (t / 110f) * 4421L + 5L);
        int glRow = gr.nextInt(rows - 2);
        float glDx = (gr.nextFloat() - 0.5f) * dw * 0.10f * (gr.nextFloat() < 0.5f ? 1f : 0f);

        float shakeX = (float) Math.sin(ts * 31f) * dw * 0.006f + (gr.nextFloat() - 0.5f) * dw * 0.008f;
        float shakeY = (float) Math.cos(ts * 27f) * dh * 0.006f + (gr.nextFloat() - 0.5f) * dh * 0.008f;
        float scale = (0.85f + 0.15f * a) * (1f + 0.035f * (float) Math.sin(ts * 9f));
        float rot = 2.2f * (float) Math.sin(ts * 6.5f);

        MatrixStack m = ctx.getMatrices();
        m.push();
        m.translate(cx + shakeX, cy + shakeY, 0);
        m.scale(scale, scale, 1f);
        m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rot));
        ctx.setShaderColor(1f, 1f, 1f, a);
        int drawW = (int) Math.ceil(tw) + 1;
        int drawH = (int) Math.ceil(th) + 1;
        for (int row = 0; row < rows; row++) {
            float ny = (row + 0.5f) / rows * 2f - 1f;
            for (int col = 0; col < cols; col++) {
                float nx = (col + 0.5f) / cols * 2f - 1f;
                // los costados y la parte baja (hojas, aretes, garras) se mueven más
                float edgeF = (float) Math.pow(Math.abs(nx), 1.6) * (0.45f + 0.55f * (ny + 1f) / 2f);
                float ph = ts * 8.5f + row * 0.35f + col * 0.12f;
                float ox = (float) Math.sin(ph) * amp * edgeF * (nx > 0 ? 1f : -1f)
                        + (float) Math.sin(ts * 3f + row) * amp * 0.15f;
                float oy = (float) Math.cos(ph * 0.9f) * amp * 0.8f * edgeF;
                if (row == glRow || row == glRow + 1) ox += glDx;
                m.push();
                m.translate(-dw / 2f + col * tw + ox, -dh / 2f + row * th + oy, 0);
                ctx.drawTexture(SKULL, 0, 0, drawW, drawH, col * 20f, row * 14f, 20, 14, 480, 336);
                m.pop();
            }
        }
        ctx.setShaderColor(1f, 1f, 1f, 1f);
        m.pop();
    }

    /** Un rayo rojo que va desde fuera de la pantalla hasta cerca de la cara. */
    private static void bolt(DrawContext ctx, Random r, float cx, float cy, int w, int h, float dw, float dh, float a) {
        double ang = r.nextDouble() * Math.PI * 2;
        float sx = cx + (float) Math.cos(ang) * w * 0.62f;
        float sy = cy + (float) Math.sin(ang) * h * 0.66f;
        float ex = cx + (float) Math.cos(ang) * dw * (0.50f + r.nextFloat() * 0.12f);
        float ey = cy + (float) Math.sin(ang) * dh * (0.52f + r.nextFloat() * 0.12f);

        final int n = 9;
        float[] xs = new float[n + 1];
        float[] ys = new float[n + 1];
        float dx = ex - sx, dy = ey - sy;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        float px = -dy / Math.max(len, 1f), py = dx / Math.max(len, 1f);
        for (int i = 0; i <= n; i++) {
            float k = i / (float) n;
            float j = (i == 0 || i == n) ? 0f : (r.nextFloat() - 0.5f) * len * 0.16f;
            xs[i] = sx + dx * k + px * j;
            ys[i] = sy + dy * k + py * j;
        }
        int glow = ((int) (60 * a) << 24) | 0xFF0010;
        int mid = ((int) (170 * a) << 24) | 0xFF2030;
        int core = ((int) (255 * a) << 24) | 0xFFD8D8;
        for (int i = 0; i < n; i++) {
            segment(ctx, xs[i], ys[i], xs[i + 1], ys[i + 1], 7, glow);
            segment(ctx, xs[i], ys[i], xs[i + 1], ys[i + 1], 4, mid);
            segment(ctx, xs[i], ys[i], xs[i + 1], ys[i + 1], 2, core);
        }
    }

    private static void segment(DrawContext ctx, float x1, float y1, float x2, float y2, int th, int argb) {
        float dx = x2 - x1, dy = y2 - y1;
        int steps = Math.max(1, (int) (Math.max(Math.abs(dx), Math.abs(dy)) / 2f));
        int half = th / 2;
        for (int i = 0; i <= steps; i++) {
            float k = i / (float) steps;
            int x = (int) (x1 + dx * k);
            int y = (int) (y1 + dy * k);
            ctx.fill(x - half, y - half, x - half + th, y - half + th, argb);
        }
    }
}
