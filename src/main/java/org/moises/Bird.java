package org.moises;

/**
 * Bird: datos y lógica de un pájaro jugador.
 * <p>
 * Encapsula posición, velocidad, estado (vivo/muerto), puntuación, color e ID
 * de jugador. También expone métodos para saltar, actualizar física y resetear.
 */
public class Bird {

    // -------------------------------------------------------------------------
    // Constantes de física y geometría
    // -------------------------------------------------------------------------

    /** Aceleración de gravedad (NDC/s²). */
    public static final float GRAVEDAD = -1.9f;

    /** Impulso vertical al saltar (NDC/s). */
    public static final float IMPULSO_SALTO = 0.85f;

    /** Velocidad máxima de caída (NDC/s). */
    public static final float VELOCIDAD_MAX_CAIDA = -1.8f;

    /** Ancho del pájaro en NDC. */
    public static final float ANCHO = 0.10f;

    /** Alto del pájaro en NDC. */
    public static final float ALTO = 0.09f;

    /** Ángulo máximo de inclinación en radianes (~30°). */
    private static final float MAX_TILT = (float) Math.toRadians(30.0);

    /** Velocidad de animación del ala (rad/s). */
    private static final float FLAP_SPEED = 8.0f;

    /** Amplitud de oscilación del ala en NDC. */
    private static final float WING_AMP = 0.025f;

    // -------------------------------------------------------------------------
    // Estado del pájaro
    // -------------------------------------------------------------------------

    /** Nombre del jugador (P1 / P2). */
    public final String name;

    /** Posición horizontal fija en NDC. */
    public final float x;

    /** Posición vertical actual en NDC. */
    public float y;

    /** Velocidad vertical actual (NDC/s). */
    public float velY;

    /** {@code true} si el pájaro sigue vivo. */
    public boolean alive;

    /** Puntuación acumulada de este jugador. */
    public int score;

    /** Color RGB del pájaro (componentes [0,1]). */
    public final float[] color;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Crea un pájaro para el jugador indicado.
     *
     * @param name  nombre del jugador (p. ej. {@code "P1"}).
     * @param x     posición horizontal fija en NDC.
     * @param r     componente roja del color [0,1].
     * @param g     componente verde del color [0,1].
     * @param b     componente azul del color [0,1].
     */
    public Bird(String name, float x, float r, float g, float b) {
        this.name  = name;
        this.x     = x;
        this.color = new float[]{r, g, b};
        reset();
    }

    // -------------------------------------------------------------------------
    // Métodos públicos
    // -------------------------------------------------------------------------

    /**
     * Aplica el impulso de salto si el pájaro está vivo.
     */
    public void jump() {
        if (alive) {
            velY = IMPULSO_SALTO;
        }
    }

    /**
     * Actualiza la física vertical del pájaro.
     * Si el pájaro golpea el techo o el suelo, muere.
     *
     * @param dt delta de tiempo en segundos.
     */
    public void update(float dt) {
        if (!alive) return;

        velY += GRAVEDAD * dt;
        if (velY < VELOCIDAD_MAX_CAIDA) velY = VELOCIDAD_MAX_CAIDA;
        y += velY * dt;

        // Límites de pantalla (NDC -1 … +1).
        if (y + ALTO * 0.5f >= 1.0f || y - ALTO * 0.5f <= -0.88f) {
            alive = false;
        }
    }

    /**
     * Resetea el pájaro a su estado inicial para empezar una nueva partida.
     */
    public void reset() {
        y     = 0.0f;
        velY  = 0.0f;
        alive = true;
        score = 0;
    }

    /**
     * Renderiza el pájaro compuesto (cuerpo, pico, ojo, cola, ala animada).
     * La inclinación se calcula a partir de {@code velY}.
     *
     * @param renderer instancia del {@link Renderer} activo.
     * @param time     tiempo de aplicación en segundos (para animación del ala).
     */
    public void render(Renderer renderer, float time) {
        if (!alive) return;

        float tilt = clamp(velY / Math.abs(IMPULSO_SALTO), -1.0f, 1.0f) * MAX_TILT;
        float wingOffset = (float) Math.sin(time * FLAP_SPEED) * WING_AMP;

        float r = color[0], g = color[1], b = color[2];

        // Cuerpo principal.
        renderer.drawRect(x, y, ANCHO, ALTO, tilt, r, g, b);

        // Cola (triángulo apuntando a la izquierda).
        renderer.drawTriangle(
                x - 0.06f, y,           // punta izquierda
                x - 0.02f, y + 0.03f,   // arriba
                x - 0.02f, y - 0.03f,   // abajo
                tilt,
                r * 0.75f, g * 0.75f, b * 0.75f
        );

        // Pico (triángulo apuntando a la derecha).
        renderer.drawTriangle(
                x + 0.07f, y,           // punta derecha
                x + 0.03f, y + 0.02f,   // arriba
                x + 0.03f, y - 0.02f,   // abajo
                tilt,
                0.98f, 0.65f, 0.10f
        );

        // Ala (rect animado con sin).
        renderer.drawRect(x - 0.01f, y + wingOffset, 0.06f, 0.025f, tilt,
                r * 0.80f, g * 0.80f, b * 0.80f);

        // Ojo blanco.
        renderer.drawCircle(x + 0.025f, y + 0.022f, 0.018f, 16, tilt, 1f, 1f, 1f);

        // Pupila negra.
        renderer.drawCircle(x + 0.030f, y + 0.022f, 0.010f, 12, tilt, 0.05f, 0.05f, 0.05f);
    }

    // -------------------------------------------------------------------------
    // Auxiliar
    // -------------------------------------------------------------------------

    /** Clampa un valor entre {@code min} y {@code max}. */
    private static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }
}
