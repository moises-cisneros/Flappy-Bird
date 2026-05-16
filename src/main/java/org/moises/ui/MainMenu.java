package org.moises.ui;

import org.moises.render.Renderer;
import org.moises.render.TextRenderer;


/**
 * MainMenu: renderiza la pantalla de menú principal usando solo primitivas del
 * {@link Renderer}. No requiere mouse ni librerías de UI.
 * <p>
 * Opciones navegables:
 * <ul>
 *   <li>Opción 0 → 1 Jugador (solo SPACE controla el pájaro único)</li>
 *   <li>Opción 1 → 2 Jugadores (SPACE para P1, W/↑ para P2)</li>
 * </ul>
 * La opción seleccionada se resalta con un rectángulo de color distinto.
 */
public class MainMenu {

    // -------------------------------------------------------------------------
    // Constantes visuales
    // -------------------------------------------------------------------------

    /**
     * Número de opciones disponibles.
     */
    public static final int OPT_COUNT = 2;
    /**
     * Índice de la opción "1 Jugador".
     */
    public static final int OPT_1P = 0;
    /**
     * Índice de la opción "2 Jugadores".
     */
    public static final int OPT_2P = 1;

    /**
     * Centro Y de la tarjeta de 1 jugador (NDC). Usado para hit-testing de ratón.
     */
    public static final float OPT_1P_Y = 0.02f;
    /**
     * Centro Y de la tarjeta de 2 jugadores (NDC).
     */
    public static final float OPT_2P_Y = -0.10f;
    /**
     * Ancho de cada tarjeta de opción (NDC).
     */
    public static final float CARD_W = 0.80f;
    /**
     * Alto de cada tarjeta de opción (NDC).
     */
    public static final float CARD_H = 0.10f;

    private static final float TITLE_Y = 0.30f;
    private static final float SUBTITLE_Y = 0.16f;
    private static final float INSTRUCTION_Y = -0.28f;

    // -------------------------------------------------------------------------
    // Estado
    // -------------------------------------------------------------------------

    /**
     * Índice de la opción actualmente resaltada (0 = 1P, 1 = 2P).
     */
    private int selectedOption = OPT_1P;

    private final Renderer renderer;
    private final TextRenderer text;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Crea el MainMenu ligado al renderer activo.
     *
     * @param renderer     instancia del {@link Renderer}.
     * @param textRenderer instancia del {@link TextRenderer}.
     */
    public MainMenu(Renderer renderer, TextRenderer textRenderer) {
        this.renderer = renderer;
        this.text = textRenderer;
    }

    // -------------------------------------------------------------------------
    // API pública
    // -------------------------------------------------------------------------

    /**
     * Avanza el cursor de selección hacia arriba (cíclico).
     */
    public void navigateUp() {
        selectedOption = (selectedOption - 1 + OPT_COUNT) % OPT_COUNT;
    }

    /**
     * Avanza el cursor de selección hacia abajo (cíclico).
     */
    public void navigateDown() {
        selectedOption = (selectedOption + 1) % OPT_COUNT;
    }

    /**
     * Devuelve el índice de la opción actualmente seleccionada.
     *
     * @return {@link #OPT_1P} o {@link #OPT_2P}.
     */
    public int getSelectedOption() {
        return selectedOption;
    }

    /**
     * Fuerza la selección a una opción concreta.
     *
     * @param option índice de la opción ({@link #OPT_1P} o {@link #OPT_2P}).
     */
    public void setSelectedOption(int option) {
        selectedOption = option;
    }

    /**
     * Renderiza la pantalla completa del menú principal.
     * <p>
     * Dibuja: overlay semitransparente, título decorativo, dos tarjetas de
     * selección con resaltado, y línea de ayuda inferior.
     */
    public void render() {
        // --- Overlay semitransparente de fondo ---
        renderer.setAlpha(0.72f);
        renderer.drawRect(0f, 0f, 2f, 2f, 0.04f, 0.06f, 0.12f);
        renderer.setAlpha(1.0f);

        // --- Título y Subtítulo ---
        text.drawTextClamped("FLAPPY BIRD", 0f, TITLE_Y, 1.4f, 1f, 0.8f, 0.2f, -0.6f, 0.6f, 0.5f);
        text.drawTextClamped("Selecciona modo de juego", 0f, SUBTITLE_Y, 0.5f, 0.8f, 0.8f, 0.8f, -0.6f, 0.6f, 0.3f);

        // --- Tarjeta opción 1 Jugador ---
        drawOptionCard(OPT_1P_Y, OPT_1P, "[1] Un Jugador", 1.0f, 0.20f);

        // --- Tarjeta opción 2 Jugadores ---
        drawOptionCard(OPT_2P_Y, OPT_2P, "[2] Dos Jugadores", 0.20f, 1.0f);

        // --- Instrucción inferior ---
        text.drawTextClamped("ENTER / numero para iniciar", 0f, INSTRUCTION_Y, 0.4f, 0.6f, 0.6f, 0.6f, -0.6f, 0.6f, 0.3f);

        // --- Línea de ayuda inferior ---
        renderer.drawRect(0f, -0.80f, 1.4f, 0.005f, 0.35f, 0.35f, 0.45f);
        renderer.drawRect(0f, -0.88f, 1.0f, 0.005f, 0.30f, 0.30f, 0.40f);
    }

    // -------------------------------------------------------------------------
    // Auxiliares de dibujo
    // -------------------------------------------------------------------------

    private void drawOptionCard(float cy, int optIndex, String label,
                                float cr, float cb) {
        boolean selected = (selectedOption == optIndex);

        // Fondo de la tarjeta.
        float bgR = selected ? cr * 0.25f : 0.08f;
        float bgG = selected ? (float) 0.85 * 0.25f : 0.10f;
        float bgB = selected ? cb * 0.25f : 0.16f;
        renderer.drawRect(0f, cy, CARD_W, CARD_H, bgR, bgG, bgB);

        // Borde izquierdo de color temático (más grueso si seleccionado).
        float borderW = selected ? 0.018f : 0.008f;
        renderer.drawRect(-CARD_W * 0.5f + borderW * 0.5f, cy,
                borderW, CARD_H, cr, (float) 0.85, cb);

        text.drawTextClamped(label, 0f, cy - 0.018f, 0.8f, 1f, 1f, 1f, -CARD_W / 2 + 0.1f, CARD_W / 2 - 0.1f, 0.4f);

        // Indicador ">" si está seleccionado.
        if (selected) {
            renderer.drawRect(CARD_W * 0.5f - 0.06f, cy,
                    0.012f, CARD_H * 0.6f, cr, (float) 0.85, cb);
        }
    }
}
