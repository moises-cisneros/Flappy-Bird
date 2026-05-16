package org.moises.ui;

import org.moises.core.GameState;
import org.moises.render.Renderer;
import org.moises.render.TextRenderer;

/**
 * GameOverMenu: renderiza la pantalla de fin de partida mostrando los scores
 * finales, el ganador y las opciones "Retry" / "Menú Principal".
 * <p>
 * Todo se dibuja con primitivas del {@link Renderer}, sin librerías externas.
 * <p>
 * Opciones disponibles (M7-T4 / REQ-07.8 / REQ-07.9):
 * <ul>
 * <li>R / SPACE → Retry (reinicia la partida en el mismo modo)</li>
 * <li>M / ESC → Menú Principal (vuelve a {@link GameState#MAIN_MENU})</li>
 * </ul>
 */
public class GameOverMenu {

    // -------------------------------------------------------------------------
    // Constantes de posición
    // -------------------------------------------------------------------------
    private static final float PANEL_Y = 0.00f;
    private static final float PANEL_W = 1.40f;
    private static final float PANEL_H = 0.75f;

    private static final float TITLE_Y = 0.28f;
    private static final float SCORE_Y = 0.10f;
    private static final float WINNER_Y = -0.04f;
    private static final float RETRY_Y = -0.20f;
    private static final float MENU_Y = -0.32f;

    // -------------------------------------------------------------------------
    // Dependencias
    // -------------------------------------------------------------------------
    private final Renderer renderer;
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
        this.text = textRenderer;
    }

    // -------------------------------------------------------------------------
    // API pública
    // -------------------------------------------------------------------------

    /**
     * Renderiza la pantalla de game over completa.
     *
     * @param scoreP1     puntuación final del jugador 1.
     * @param scoreP2     puntuación final del jugador 2.
     * @param scoreP3     puntuación final del jugador 3.
     * @param colorP1     color RGB del pájaro P1 (array de 3 floats [0,1]).
     * @param colorP2     color RGB del pájaro P2 (array de 3 floats [0,1]).
     * @param colorP3     color RGB del pájaro P3 (array de 3 floats [0,1]).
     * @param playerCount cantidad de jugadores en la partida (1, 2, o 3).
     */
    public void render(int scoreP1, int scoreP2, int scoreP3,
            float[] colorP1, float[] colorP2, float[] colorP3,
            int playerCount) {
        // --- Overlay semitransparente ---
        renderer.setAlpha(0.70f);
        renderer.drawRect(0f, 0f, 2f, 2f, 0.08f, 0.02f, 0.06f);
        renderer.setAlpha(1.0f);

        // --- Panel central ---
        renderer.drawRect(0f, PANEL_Y, PANEL_W, PANEL_H, 0.10f, 0.04f, 0.10f);
        text.drawTextClamped("GAME OVER", 0f, TITLE_Y, 1.2f, 1f, 1f, 1f, -0.65f, 0.65f, 0.5f);

        // Borde superior del panel (rojo oscuro).
        renderer.drawRect(0f, PANEL_Y + PANEL_H * 0.5f, PANEL_W, 0.018f,
                0.80f, 0.10f, 0.15f);
        renderer.drawRect(0f, PANEL_Y - PANEL_H * 0.5f, PANEL_W, 0.018f,
                0.80f, 0.10f, 0.15f);

        // --- Scores finales ---
        if (playerCount == 3) {
            drawScores3P(scoreP1, scoreP2, scoreP3);
            drawWinnerIndicator3P(scoreP1, scoreP2, scoreP3, colorP1, colorP2, colorP3);
        } else if (playerCount == 2) {
            drawScores(scoreP1, scoreP2);
            drawWinnerIndicator(scoreP1, scoreP2, colorP1, colorP2);
        } else {
            // Modo 1 jugador: solo muestra el score de P1 centrado.
            drawSingleScore(scoreP1, colorP1);
        }

        // --- Botones de opción ---
        drawRetryButton();
        drawMenuButton();
    }

    // -------------------------------------------------------------------------
    // Auxiliares privados
    // -------------------------------------------------------------------------

    /**
     * Dibuja los marcadores finales de P1 y P2 lado a lado.
     */
    private void drawScores(int scoreP1, int scoreP2) {
        String s = "P1: " + scoreP1 + "  |  P2: " + scoreP2;
        text.drawTextClamped(s, 0f, SCORE_Y, 0.8f, 1f, 1f, 1f, -0.65f, 0.65f, 0.4f);
    }

    /**
     * Dibuja los marcadores finales de P1, P2 y P3 lado a lado.
     */
    private void drawScores3P(int scoreP1, int scoreP2, int scoreP3) {
        String s = "P1: " + scoreP1 + " | P2: " + scoreP2 + " | P3: " + scoreP3;
        text.drawTextClamped(s, 0f, SCORE_Y, 0.7f, 1f, 1f, 1f, -0.65f, 0.65f, 0.4f);
    }

    /**
     * Dibuja el score único en modo 1 jugador (centrado).
     */
    private void drawSingleScore(int score, float[] color) {
        text.drawTextClamped("Score: " + score, 0f, SCORE_Y, 0.8f, color[0], color[1], color[2], -0.65f, 0.65f, 0.4f);
    }

    /**
     * Dibuja una barra de "ganador" debajo del score más alto.
     */
    private void drawWinnerIndicator(int s1, int s2, float[] c1, float[] c2) {
        if (s1 > s2) {
            text.drawTextClamped("Ganador: P1", 0f, WINNER_Y, 0.7f, c1[0], c1[1], c1[2], -0.65f, 0.65f, 0.4f);
        } else if (s2 > s1) {
            text.drawTextClamped("Ganador: P2", 0f, WINNER_Y, 0.7f, c2[0], c2[1], c2[2], -0.65f, 0.65f, 0.4f);
        } else {
            text.drawTextClamped("Empate", 0f, WINNER_Y, 0.7f, 1f, 1f, 1f, -0.65f, 0.65f, 0.4f);
        }
    }

    /**
     * Dibuja ganador entre 3 jugadores.
     */
    private void drawWinnerIndicator3P(int s1, int s2, int s3, float[] c1, float[] c2, float[] c3) {
        int max = Math.max(s1, Math.max(s2, s3));
        int count = 0;
        if (s1 == max)
            count++;
        if (s2 == max)
            count++;
        if (s3 == max)
            count++;

        if (count > 1) {
            text.drawTextClamped("Empate", 0f, WINNER_Y, 0.7f, 1f, 1f, 1f, -0.65f, 0.65f, 0.4f);
        } else if (s1 == max) {
            text.drawTextClamped("Ganador: P1", 0f, WINNER_Y, 0.7f, c1[0], c1[1], c1[2], -0.65f, 0.65f, 0.4f);
        } else if (s2 == max) {
            text.drawTextClamped("Ganador: P2", 0f, WINNER_Y, 0.7f, c2[0], c2[1], c2[2], -0.65f, 0.65f, 0.4f);
        } else {
            text.drawTextClamped("Ganador: P3", 0f, WINNER_Y, 0.7f, c3[0], c3[1], c3[2], -0.65f, 0.65f, 0.4f);
        }
    }

    /**
     * Dibuja el botón "Retry" (R / SPACE).
     */
    private void drawRetryButton() {
        renderer.drawRect(0f, RETRY_Y, 1.0f, 0.10f, 0.15f, 0.30f, 0.15f);
        text.drawTextClamped("[R] Volver a intentar", 0f, RETRY_Y - 0.018f, 0.6f, 1f, 1f, 1f, -0.45f, 0.45f, 0.3f);
    }

    /**
     * Dibuja el botón "Menú" (M / ESC).
     */
    private void drawMenuButton() {
        renderer.drawRect(0f, MENU_Y, 1.0f, 0.10f, 0.25f, 0.12f, 0.28f);
        text.drawTextClamped("[M] Menu principal", 0f, MENU_Y - 0.018f, 0.6f, 1f, 1f, 1f, -0.45f, 0.45f, 0.3f);
    }
}
