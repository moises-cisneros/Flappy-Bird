package org.moises.entity;

/**
 * Pipe (Tubería): modelo de datos de un obstáculo.
 * <p>
 * Las tuberías se comparten entre los dos jugadores (un solo stream).
 * Cada tubería lleva flags independientes para saber si ya otorgó punto
 * a P1 y a P2.
 */
public class Pipe {

    /**
     * Posición horizontal del centro de la tubería en NDC.
     */
    public float x;

    /**
     * Centro vertical del hueco (gap) en NDC.
     */
    public float gapCenterY;

    /**
     * Indica si P1 ya recibió punto por esta tubería.
     */
    public boolean scoredP1;

    /**
     * Indica si P2 ya recibió punto por esta tubería.
     */
    public boolean scoredP2;

    /**
     * Indica si P3 ya recibió punto por esta tubería.
     */
    public boolean scoredP3;

    public static final float PIPE_W = 0.18f;
    public static final float CAPITAL_EXTRA_WIDTH = 0.04f;
    public static final float GAP_H = 0.46f;
    public static final float PIPE_CAP_H = 0.04f;

    public Pipe(float f, float gap) {
        this.x = f;
        this.gapCenterY = gap;
        this.scoredP1 = false;
        this.scoredP2 = false;
        this.scoredP3 = false;
    }

    /**
     * Crea una nueva tubería.
     *
     * @param x          posición horizontal inicial (normalmente ≥ 1.2).
     * @param gapCenterY centro vertical del hueco en NDC.
     *                   public Pipe(float x, float gapCenterY) {
     *                   this.x = x;
     *                   this.gapCenterY = gapCenterY;
     *                   this.scoredP1 = false;
     *                   this.scoredP2 = false;
     *                   }
     * 
     *                   /**
     *                   Evalúa el AABB contra DOS rectángulos lógicos:
     *                   - El tronco (más angosto).
     *                   - El capitel/boquilla (más ancho y ubicado en el extremo).
     */
    public boolean collides(Bird b) {
        float bL = b.x - Bird.ANCHO * 0.5f;
        float bR = b.x + Bird.ANCHO * 0.5f;
        float bT = b.y + Bird.ALTO * 0.5f;
        float bB = b.y - Bird.ALTO * 0.5f;

        float gapTop = this.gapCenterY + GAP_H * 0.5f;
        float gapBot = this.gapCenterY - GAP_H * 0.5f;

        // Colisión con el tronco de la tubería
        float tL = this.x - PIPE_W * 0.5f;
        float tR = this.x + PIPE_W * 0.5f;
        boolean hitTrunk = (bR > tL && bL < tR) && (bT > gapTop || bB < gapBot);

        // Colisión con los capiteles sobresalientes
        float maxPipeW = PIPE_W + CAPITAL_EXTRA_WIDTH;
        float cL = this.x - maxPipeW * 0.5f;
        float cR = this.x + maxPipeW * 0.5f;

        boolean hitTopCap = (bR > cL && bL < cR) && (bB < gapTop + PIPE_CAP_H && bT > gapTop);
        boolean hitBotCap = (bR > cL && bL < cR) && (bB < gapBot && bT > gapBot - PIPE_CAP_H);

        return hitTrunk || hitTopCap || hitBotCap;
    }
}
