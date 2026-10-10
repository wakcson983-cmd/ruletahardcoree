package com.ruleta.client;

import com.ruleta.RuletaColor;
import com.ruleta.net.SelectPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/** Menú que se abre al hacer click con la ruleta: elegir color, escribir título y texto. */
public class RuletaScreen extends Screen {
    private static final int PANEL_W = 380;
    private static final int PANEL_H = 190;
    private static final int SWATCH = 20;
    private static final int GAP = 4;

    private int selected = RuletaColor.ROJO.ordinal();
    private TextFieldWidget titleField;
    private TextFieldWidget textField;

    private int px, py, swatchX, swatchY;

    public RuletaScreen() {
        super(Text.literal("Ruleta"));
    }

    @Override
    protected void init() {
        px = (width - PANEL_W) / 2;
        py = (height - PANEL_H) / 2;
        int fx = px + 150;
        int fw = PANEL_W - 150 - 14;

        swatchX = fx;
        swatchY = py + 38;

        titleField = new TextFieldWidget(textRenderer, fx, py + 80, fw, 20, Text.literal("Título"));
        titleField.setMaxLength(48);
        addDrawableChild(titleField);

        textField = new TextFieldWidget(textRenderer, fx, py + 120, fw, 20, Text.literal("Texto"));
        textField.setMaxLength(200);
        addDrawableChild(textField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Girar ruleta"), b -> enviar())
                .dimensions(fx, py + 150, fw, 22).build());

        setInitialFocus(titleField);
    }

    private void enviar() {
        ClientPlayNetworking.send(new SelectPayload(selected, titleField.getText(), textField.getText()));
        close();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);

        ctx.fill(px - 2, py - 2, px + PANEL_W + 2, py + PANEL_H + 2, 0xFF0B0B0D);
        ctx.fill(px, py, px + PANEL_W, py + PANEL_H, 0xF0232327);
        // Los widgets se dibujan de nuevo encima del panel.
        titleField.render(ctx, mouseX, mouseY, delta);
        textField.render(ctx, mouseX, mouseY, delta);
        for (var child : children()) {
            if (child instanceof ButtonWidget bw) bw.render(ctx, mouseX, mouseY, delta);
        }

        ctx.drawTexture(Anim.WHEEL, px + 10, py + 30, 130, 130, 0f, 0f, 512, 512, 512, 512);
        ctx.drawTextWithShadow(textRenderer, Text.literal("RULETA"), px + 10, py + 10, 0xFFFFFFFF);

        int fx = px + 150;
        ctx.drawTextWithShadow(textRenderer, Text.literal("Color que va a salir"), fx, py + 26, 0xFFD8D8E0);
        RuletaColor[] cols = RuletaColor.values();
        for (int i = 0; i < cols.length; i++) {
            int x = swatchX + i * (SWATCH + GAP);
            int y = swatchY;
            if (i == selected) {
                ctx.fill(x - 2, y - 2, x + SWATCH + 2, y + SWATCH + 2, 0xFFFFFFFF);
            } else {
                ctx.fill(x - 1, y - 1, x + SWATCH + 1, y + SWATCH + 1, 0xFF0B0B0D);
            }
            ctx.fill(x, y, x + SWATCH, y + SWATCH, 0xFF000000 | cols[i].rgb);
        }
        ctx.drawTextWithShadow(textRenderer, Text.literal("Título"), fx, py + 68, 0xFFD8D8E0);
        ctx.drawTextWithShadow(textRenderer, Text.literal("Texto del chat"), fx, py + 108, 0xFFD8D8E0);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        RuletaColor[] cols = RuletaColor.values();
        for (int i = 0; i < cols.length; i++) {
            int x = swatchX + i * (SWATCH + GAP);
            if (mouseX >= x && mouseX <= x + SWATCH && mouseY >= swatchY && mouseY <= swatchY + SWATCH) {
                selected = i;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
