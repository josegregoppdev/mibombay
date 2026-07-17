package com.mibombay.sistemaresurante.config;

import java.util.Map;

public record TemaPreset(
    String fondo, String fondoRgb,
    String surface, String surface2, String surfaceHover,
    String text, String textSecondary, String textMuted, String textFaint,
    String border, String borderStrong,
    String sidebarBg, String sidebarRgb,
    String sidebarText, String sidebarTextMuted, String sidebarTextFaint,
    String accent, String accentRgb,
    String secondary, String boton, String botonRgb
) {
    private static final TemaPreset OSCURO = new TemaPreset(
        "#0b0b0f", "11, 11, 15",
        "#131317", "#19191f", "#1e1e26",
        "#f0efed", "#a09e9c", "#6a6865", "#4a4855",
        "rgba(255,255,255,0.05)", "rgba(255,255,255,0.09)",
        "#0c0c10", "12, 12, 16",
        "#e8e6e3", "#8a8680", "#4a4855",
        "#d4a853", "212, 168, 83",
        "#8b7355", "#d4a853", "212, 168, 83"
    );

    private static final TemaPreset TIERRA = new TemaPreset(
        "#1c1814", "28, 24, 20",
        "#24201c", "#2c2824", "#322e2a",
        "#e8ddd0", "#b0a090", "#7a6a5a", "#5a4a3a",
        "rgba(255,255,255,0.05)", "rgba(255,255,255,0.09)",
        "#14100c", "20, 16, 12",
        "#e8ddd0", "#8a7a6a", "#5a4a3a",
        "#c9a84c", "201, 168, 76",
        "#8b7355", "#c9a84c", "201, 168, 76"
    );

    private static final TemaPreset CLARO = new TemaPreset(
        "#f5f0e8", "245, 240, 232",
        "#ffffff", "#f0ebe3", "#faf5ed",
        "#2d2418", "#6b5d4f", "#9a8a7a", "#b8a898",
        "rgba(45,36,24,0.08)", "rgba(45,36,24,0.15)",
        "#1a1410", "26, 20, 16",
        "#f5f0e8", "#8a7a6a", "#4a4038",
        "#d4a853", "212, 168, 83",
        "#e07a5f", "#d4a853", "212, 168, 83"
    );

    public static final Map<String, TemaPreset> TODOS = Map.of(
        "OSCURO", OSCURO,
        "TIERRA", TIERRA,
        "CLARO", CLARO
    );
}
