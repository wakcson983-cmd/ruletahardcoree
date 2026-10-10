package com.ruleta;

/** Los 8 colores de la ruleta, en orden horario empezando por el gajo naranja (como la imagen). */
public enum RuletaColor {
    NARANJA("naranja", 0xFF8800, 30.2f),
    ROJO("rojo", 0xFF2244, 75.2f),
    ROSA("rosa", 0xEE22BB, 120.2f),
    MORADO("morado", 0x9B30E5, 165.2f),
    AZUL("azul", 0x2238DD, 210.2f),
    CELESTE("celeste", 0x15D4E0, 255.2f),
    VERDE("verde", 0x38CC3C, 300.2f),
    AMARILLO("amarillo", 0xFFD000, 345.2f);

    public final String id;
    public final int rgb;
    /** Ángulo (grados, horario desde arriba) del centro del gajo en la imagen sin girar. */
    public final float center;

    RuletaColor(String id, int rgb, float center) {
        this.id = id;
        this.rgb = rgb;
        this.center = center;
    }

    public static RuletaColor byIndex(int i) {
        RuletaColor[] v = values();
        return i >= 0 && i < v.length ? v[i] : null;
    }
}
