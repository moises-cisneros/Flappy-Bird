package org.moises;

/**
 * TextRenderer: dibuja dígitos (0–9) y caracteres básicos usando segmentos
 * geométricos al estilo display de 7 segmentos. No requiere fuentes TTF.
 * <p>
 * Todos los caracteres se dibujan con {@link Renderer#drawRect} para los
 * segmentos horizontales/verticales, garantizando que el código sea 100%
 * explicable durante la defensa del examen.
 */
public class TextRenderer {

    // -------------------------------------------------------------------------
    // Dimensiones de un carácter (en NDC)
    // -------------------------------------------------------------------------

    /** Ancho total de un carácter. */
    private static final float CHAR_W  = 0.045f;
    /** Alto total de un carácter. */
    private static final float CHAR_H  = 0.075f;
    /** Separación entre caracteres. */
    private static final float CHAR_GAP = 0.012f;
    /** Grosor de cada segmento. */
    private static final float SEG_T   = 0.009f;

    /**
     * Mapa de segmentos activos por dígito (0–9).
     * Los 7 bits corresponden a los segmentos: top, top-left, top-right,
     * mid, bot-left, bot-right, bot (codificación estándar 7-seg).
     * Bit 6 = top, Bit 5 = top-left, Bit 4 = top-right,
     * Bit 3 = mid,  Bit 2 = bot-left, Bit 1 = bot-right, Bit 0 = bot.
     */
    private static final int[] DIGIT_SEGS = {
        0b1110111, // 0
        0b0010010, // 1
        0b1101101, // 2
        0b1011101, // 3
        0b0011110, // 4
        0b1011011, // 5
        0b1111011, // 6
        0b0010101, // 7
        0b1111111, // 8
        0b1011111  // 9
    };

    private final Renderer renderer;

    /**
     * Crea el TextRenderer ligado al renderer de la partida.
     *
     * @param renderer instancia del {@link Renderer} activo.
     */
    public TextRenderer(Renderer renderer) {
        this.renderer = renderer;
    }

    // -------------------------------------------------------------------------
    // API pública
    // -------------------------------------------------------------------------

    /**
     * Dibuja un número entero no negativo empezando en ({@code x}, {@code y}).
     *
     * @param value número a dibujar (≥ 0).
     * @param x     esquina izquierda del primer dígito.
     * @param y     centro vertical de los caracteres.
     * @param scale factor de escala (1.0 = tamaño base).
     * @param r     rojo [0,1].
     * @param g     verde [0,1].
     * @param b     azul [0,1].
     */
    public void drawNumber(int value, float x, float y, float scale,
                           float r, float g, float b) {
        String str = Integer.toString(value);
        for (char c : str.toCharArray()) {
            drawDigit(c - '0', x, y, scale, r, g, b);
            x += (CHAR_W + CHAR_GAP) * scale;
        }
    }

    /**
     * Devuelve el ancho total (en NDC) que ocupa un número con {@code digits} dígitos.
     *
     * @param digits cantidad de dígitos.
     * @param scale  factor de escala.
     * @return ancho total en NDC.
     */
    public float numberWidth(int digits, float scale) {
        return digits * (CHAR_W + CHAR_GAP) * scale - CHAR_GAP * scale;
    }

    // -------------------------------------------------------------------------
    // Dibujo de un dígito individual
    // -------------------------------------------------------------------------

    /**
     * Dibuja un único dígito (0–9) en la posición dada.
     */
    private void drawDigit(int digit, float x, float y, float scale,
                            float r, float g, float b) {
        if (digit < 0 || digit > 9) return;
        int segs = DIGIT_SEGS[digit];

        float w  = CHAR_W  * scale;
        float h  = CHAR_H  * scale;
        float t  = SEG_T   * scale;
        float hw = w * 0.5f;
        float hh = h * 0.5f;

        // Segmentos horizontales (top, mid, bot).
        // top
        if ((segs & 0b1000000) != 0)
            renderer.drawRect(x + hw, y + hh - t * 0.5f, w, t, r, g, b);
        // mid
        if ((segs & 0b0001000) != 0)
            renderer.drawRect(x + hw, y,                 w, t, r, g, b);
        // bot
        if ((segs & 0b0000001) != 0)
            renderer.drawRect(x + hw, y - hh + t * 0.5f, w, t, r, g, b);

        // Segmentos verticales.
        float topY  = y + hh * 0.5f;  // mitad superior
        float botY  = y - hh * 0.5f;  // mitad inferior
        float halfH = hh * 0.95f;     // altura del segmento vertical

        // top-left
        if ((segs & 0b0100000) != 0)
            renderer.drawRect(x + t * 0.5f, topY, t, halfH, r, g, b);
        // top-right
        if ((segs & 0b0010000) != 0)
            renderer.drawRect(x + w - t * 0.5f, topY, t, halfH, r, g, b);
        // bot-left
        if ((segs & 0b0000100) != 0)
            renderer.drawRect(x + t * 0.5f, botY, t, halfH, r, g, b);
        // bot-right
        if ((segs & 0b0000010) != 0)
            renderer.drawRect(x + w - t * 0.5f, botY, t, halfH, r, g, b);
    }
}
