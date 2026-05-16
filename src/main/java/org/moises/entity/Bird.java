package org.moises.entity;

import org.moises.render.Renderer;

/**
 * Bird: datos y lógica de un pájaro jugador.
 * <p>
 * Encapsula posición, velocidad, estado (vivo/muerto), puntuación, color e ID
 * de jugador. También expone métodos para saltar, actualizar física y resetear.
 */
public final class Bird {

    // -------------------------------------------------------------------------
    // Constantes de física y geometría
    // -------------------------------------------------------------------------

    /**
     * Aceleración de gravedad (NDC/s²).
     */
    public static final float GRAVEDAD = -1.9f;

    /**
     * Impulso vertical al saltar (NDC/s).
     */
    public static final float IMPULSO_SALTO = 0.85f;

    /**
     * Velocidad máxima de caída (NDC/s).
     */
    public static final float VELOCIDAD_MAX_CAIDA = -1.8f;

    /**
     * Ancho del pájaro en NDC.
     */
    public static final float ANCHO = 0.10f;

    /**
     * Alto del pájaro en NDC.
     */
    public static final float ALTO = 0.09f;

    /**
     * Ángulo máximo de inclinación en radianes (~30°).
     */
    private static final float MAX_TILT = (float) Math.toRadians(30.0);

    /**
     * Velocidad de animación del ala (rad/s).
     */
    private static final float FLAP_SPEED = 8.0f;

    /**
     * Amplitud de oscilación del ala en NDC.
     */
    private static final float WING_AMP = 0.025f;

    /**
     * Puntos necesarios para que la gravedad se invierta.
     */
    private static final int PTOS_GRAVEDAD_INVERTIDA = 1;

    // -------------------------------------------------------------------------
    // Estado del pájaro
    // -------------------------------------------------------------------------

    /**
     * Nombre del jugador (P1 / P2).
     */
    public final String name;

    /**
     * Posición horizontal fija en NDC.
     */
    public final float x;

    /**
     * Posición vertical actual en NDC.
     */
    public float y;

    /**
     * Velocidad vertical actual (NDC/s).
     */
    public float velY;

    /**
     * {@code true} si el pájaro sigue vivo.
     */
    public boolean alive;

    /**
     * Tiempo en ms en que murió.
     */
    public long timeDeath;

    /**
     * Puntuación acumulada de este jugador.
     */
    public int score;

    /**
     * Color RGB del pájaro (componentes [0,1]).
     */
    public final float[] color;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Crea un pájaro para el jugador indicado.
     *
     * @param name nombre del jugador (p. ej. {@code "P1"}).
     * @param x    posición horizontal fija en NDC.
     * @param r    componente roja del color [0,1].
     * @param g    componente verde del color [0,1].
     * @param b    componente azul del color [0,1].
     */
    public Bird(String name, float x, float r, float g, float b) {
        this.name = name;
        this.x = x;
        this.color = new float[] { r, g, b };
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
            velY = (score >= 15) ? -IMPULSO_SALTO : IMPULSO_SALTO;
        }
    }

    /**
     * Actualiza la física vertical del pájaro.
     * Si el pájaro golpea el techo o el suelo, muere.
     *
     * @param dt delta de tiempo en segundos.
     */
    public void update(float dt) {
        if (!alive)
            return;

        float currentGravity = (score >= PTOS_GRAVEDAD_INVERTIDA) ? -GRAVEDAD : GRAVEDAD;
        float currentMaxFallSpeed = (score >= PTOS_GRAVEDAD_INVERTIDA) ? -VELOCIDAD_MAX_CAIDA : VELOCIDAD_MAX_CAIDA;

        velY += currentGravity * dt;

        if (score >= PTOS_GRAVEDAD_INVERTIDA) {
            if (velY > currentMaxFallSpeed)
                velY = currentMaxFallSpeed;
        } else {
            if (velY < currentMaxFallSpeed)
                velY = currentMaxFallSpeed;
        }

        y += velY * dt;

        // Límites de pantalla (NDC -1 … +1).
        if (y + ALTO * 0.5f >= 1.0f || y - ALTO * 0.5f <= -0.88f) {
            die();
        }
    }

    public void die() {
        if (alive) {
            alive = false;
            timeDeath = System.currentTimeMillis();
        }
    }

    /**
     * Resetea el pájaro a su estado inicial para empezar una nueva partida.
     */
    public void reset() {
        y = 0.0f;
        velY = 0.0f;
        alive = true;
        timeDeath = 0;
        score = 0;
    }

    /**
     * Renderiza el pájaro compuesto (cuerpo, pico, ojo, cola, ala animada).
     * <p>
     * Todas las partes se definen en <b>espacio local</b> (relativas al centro
     * del pájaro). La rotación 2D se aplica en CPU antes de trasladar al mundo,
     * garantizando que el tilt inclina TODAS las partes de forma cohesiva (M9-T2).
     *
     * @param renderer instancia del {@link Renderer} activo.
     * @param time     tiempo de aplicación en segundos (para animación del ala).
     */
    public void render(Renderer renderer, float time) {
        if (!alive) {
            if (System.currentTimeMillis() < timeDeath + 1500) {
                if ((System.currentTimeMillis() / 200) % 2 == 0) {
                    renderer.drawExplosion(x, y, ANCHO * 2.5f, ALTO * 2.5f);
                }
            }
            return;
        }

        float theta = clamp(velY / Math.abs(IMPULSO_SALTO)) * MAX_TILT;
        float wing = (float) Math.sin(time * FLAP_SPEED) * WING_AMP;

        float r = color[0], g = color[1], b = color[2];

        renderer.glPushMatrix();
        renderer.glTranslatef(x, y);
        renderer.glRotatef(theta);

        // Cola — triángulo apuntando a la izquierda (dibujado antes para quedar detrás)
        renderer.drawTriangle(
                -0.06f, 0f, -0.02f, 0.03f, -0.02f, -0.03f, 0f,
                r * 0.75f, g * 0.75f, b * 0.75f);

        // Cuerpo — dibujado en el centro local
        renderer.drawRect(0f, 0f, ANCHO, ALTO, 0f, r, g, b);

        // Pico — triángulo apuntando a la derecha
        renderer.drawTriangle(
                0.07f, 0f, 0.03f, 0.02f, 0.03f, -0.02f, 0f,
                0.98f, 0.65f, 0.10f);

        // Ala — rect animado (Restaurado a la versión antigua de traslación en Y)
        renderer.drawRect(-0.01f, wing, 0.06f, 0.025f, 0f,
                r * 0.80f, g * 0.80f, b * 0.80f);

        // Ojo blanco y pupila
        renderer.drawCircle(0.025f, 0.022f, 0.018f, 16, 0f, 1f, 1f, 1f);
        renderer.drawCircle(0.030f, 0.022f, 0.010f, 12, 0f, 0.05f, 0.05f, 0.05f);

        renderer.glPopMatrix();
    }

    /**
     * Clampa un valor entre {@code min} y {@code max}.
     */
    private static float clamp(float val) {
        return Math.max((float) -1.0, Math.min((float) 1.0, val));
    }
}