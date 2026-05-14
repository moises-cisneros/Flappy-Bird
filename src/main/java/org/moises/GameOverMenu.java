package org.moises;

/**
 * GameOverMenu: renderiza la pantalla de fin de partida mostrando los scores
 * finales, el ganador y las opciones "Retry" / "Menú Principal".
 * <p>
 * Todo se dibuja con primitivas del {@link Renderer}, sin librerías externas.
 * <p>
 * Opciones disponibles (M7-T4 / REQ-07.8 / REQ-07.9):
 * <ul>
 *   <li>R / SPACE → Retry (reinicia la partida en el mismo modo)</li>
 *   <li>M / ESC   → Menú Principal (vuelve a {@link GameState#MAIN_MENU})</li>
 * </ul>
 */
public class GameOverMenu {

    // -------------------------------------------------------------------------
    // Constantes de posición
    // -------------------------------------------------------------------------
    private static final float PANEL_Y    =  0.10f;
    private static final float PANEL_W    =  1.40f;
    private static final float PANEL_H    =  0.90f;
    private static final float SCORE_Y    =  0.35f;
    private static final float WINNER_Y   =  0.10f;

    /** Centro Y de los botones Retry/Menú (NDC). Expuesto para hit-testing de ratón. */
    public static final float BTN_Y       = -0.25f;
    /** Ancho de cada botón (NDC). */
    public static final float BTN_W       =  0.44f;
    /** Alto de cada botón (NDC). */
    public static final float BTN_H       =  0.10f;


    // -------------------------------------------------------------------------
    // Dependencias
    // -------------------------------------------------------------------------
    private final Renderer     renderer;
    private final TextRenderer text;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Crea el GameOverMenu ligado al renderer activo.
     *
     * @param renderer     instancia del {@link Renderer}.
     * @param textRenderer instancia del {@link TextRenderer}.
     */
    public GameOverMenu(Renderer renderer, TextRenderer textRenderer) {
        this.renderer = renderer;
        this.text     = textRenderer;
    }

    // -------------------------------------------------------------------------
    // API pública
    // -------------------------------------------------------------------------

    /**
     * Renderiza la pantalla de game over completa.
     *
     * @param scoreP1      puntuación final del jugador 1.
     * @param scoreP2      puntuación final del jugador 2.
     * @param colorP1      color RGB del pájaro P1 (array de 3 floats [0,1]).
     * @param colorP2      color RGB del pájaro P2 (array de 3 floats [0,1]).
     * @param twoPlayerMode {@code true} si se jugó en modo 2 jugadores.
     */
    public void render(int scoreP1, int scoreP2,
                       float[] colorP1, float[] colorP2,
                       boolean twoPlayerMode) {
        // --- Overlay semitransparente ---
        renderer.setAlpha(0.70f);
        renderer.drawRect(0f, 0f, 2f, 2f, 0.08f, 0.02f, 0.06f);
        renderer.setAlpha(1.0f);

        // --- Panel central ---
        renderer.drawRect(0f, PANEL_Y, PANEL_W, PANEL_H, 0.10f, 0.04f, 0.10f);
        text.drawText("Fin del Juego", -0.28f, PANEL_Y + PANEL_H * 0.35f, 1.2f, 1f, 1f, 1f);

        // Borde superior del panel (rojo oscuro).
        renderer.drawRect(0f, PANEL_Y + PANEL_H * 0.5f, PANEL_W, 0.018f,
                0.80f, 0.10f, 0.15f);
        renderer.drawRect(0f, PANEL_Y - PANEL_H * 0.5f, PANEL_W, 0.018f,
                0.80f, 0.10f, 0.15f);

        // --- Scores finales ---
        if (twoPlayerMode) {
            drawScores(scoreP1, scoreP2, colorP1, colorP2);
            drawWinnerIndicator(scoreP1, scoreP2, colorP1, colorP2);
        } else {
            // Modo 1 jugador: solo muestra el score de P1 centrado.
            drawSingleScore(scoreP1, colorP1);
        }

        // --- Botones de opción ---
        drawRetryButton();
        drawMenuButton();

        // --- Separador ---
        renderer.drawRect(0f, BTN_Y + 0.08f, PANEL_W * 0.8f, 0.004f,
                0.35f, 0.15f, 0.35f);
    }

    // -------------------------------------------------------------------------
    // Auxiliares privados
    // -------------------------------------------------------------------------

    /**
     * Dibuja los marcadores finales de P1 y P2 lado a lado.
     */
    private void drawScores(int scoreP1, int scoreP2,
                             float[] c1, float[] c2) {
        // P1 (izquierda).
        text.drawNumber(scoreP1, -0.58f, SCORE_Y, 2.8f, c1[0], c1[1], c1[2]);

        // Separador central.
        renderer.drawRect(0f, SCORE_Y, 0.008f, 0.14f, 0.50f, 0.20f, 0.50f);

        // P2 (derecha).
        int digits2 = Math.max(1, String.valueOf(scoreP2).length());
        float w2 = text.numberWidth(digits2, 2.8f);
        text.drawNumber(scoreP2, 0.58f - w2, SCORE_Y, 2.8f, c2[0], c2[1], c2[2]);
    }

    /**
     * Dibuja el score único en modo 1 jugador (centrado).
     */
    private void drawSingleScore(int score, float[] color) {
        int digits = Math.max(1, String.valueOf(score).length());
        float w = text.numberWidth(digits, 3.0f);
        text.drawNumber(score, -w * 0.5f, SCORE_Y, 3.0f,
                color[0], color[1], color[2]);
    }

    /**
     * Dibuja una barra de "ganador" debajo del score más alto.
     */
    private void drawWinnerIndicator(int s1, int s2, float[] c1, float[] c2) {
        if (s1 >= s2) {
            // Ganó P1 o empate → barra amarilla a la izquierda.
            renderer.drawRect(-0.30f, WINNER_Y, 0.48f, 0.010f, c1[0], c1[1], c1[2]);
        } else {
            // Ganó P2 → barra cian a la derecha.
            renderer.drawRect(0.30f, WINNER_Y, 0.48f, 0.010f, c2[0], c2[1], c2[2]);
        }
    }

    /**
     * Dibuja el botón "Retry" (R / SPACE) a la izquierda.
     */
    private void drawRetryButton() {
        // Fondo del botón.
        renderer.drawRect(-0.34f, BTN_Y, BTN_W, BTN_H, 0.15f, 0.30f, 0.15f);
        renderer.drawRect(-0.34f, BTN_Y + BTN_H * 0.5f,
                BTN_W, 0.007f, 0.25f, 0.75f, 0.25f);
        // Indicador visual: cuadrado verde como ícono de "play".
        renderer.drawTriangle(
                -0.42f, BTN_Y + 0.025f,
                -0.42f, BTN_Y - 0.025f,
                -0.34f, BTN_Y,
                0f, 0.25f, 0.90f, 0.25f);
        
        text.drawText("Iniciar Juego", -0.50f, BTN_Y - 0.018f, 0.6f, 1f, 1f, 1f);
    }

    /**
     * Dibuja el botón "Menú" (M / ESC) a la derecha.
     */
    private void drawMenuButton() {
        // Fondo del botón.
        renderer.drawRect(0.34f, BTN_Y, BTN_W, BTN_H, 0.25f, 0.12f, 0.28f);
        renderer.drawRect(0.34f, BTN_Y + BTN_H * 0.5f,
                BTN_W, 0.007f, 0.70f, 0.25f, 0.75f);
        // Ícono de "casa" (triángulo sobre un rect).
        renderer.drawTriangle(
                0.25f, BTN_Y + 0.035f,
                0.35f, BTN_Y + 0.010f,
                0.45f, BTN_Y + 0.035f,
                0f, 0.85f, 0.40f, 0.90f);
        renderer.drawRect(0.35f, BTN_Y - 0.015f, 0.07f, 0.04f,
                0.75f, 0.35f, 0.80f);

        text.drawText("Volver al Menu Principal", 0.15f, BTN_Y - 0.018f, 0.45f, 1f, 1f, 1f);
    }
}
