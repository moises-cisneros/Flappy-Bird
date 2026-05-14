package org.moises.entity;

/**
 * Pipe (Tubería): modelo de datos de un obstáculo.
 * <p>
 * Las tuberías se comparten entre los dos jugadores (un solo stream).
 * Cada tubería lleva flags independientes para saber si ya otorgó punto
 * a P1 y a P2.
 */
public class Pipe {

    /** Posición horizontal del centro de la tubería en NDC. */
    public float x;

    /** Centro vertical del hueco (gap) en NDC. */
    public float gapCenterY;

    /** Indica si P1 ya recibió punto por esta tubería. */
    public boolean scoredP1;

    /** Indica si P2 ya recibió punto por esta tubería. */
    public boolean scoredP2;

    /**
     * Crea una nueva tubería.
     *
     * @param x          posición horizontal inicial (normalmente ≥ 1.2).
     * @param gapCenterY centro vertical del hueco en NDC.
     */
    public Pipe(float x, float gapCenterY) {
        this.x = x;
        this.gapCenterY = gapCenterY;
        this.scoredP1 = false;
        this.scoredP2 = false;
    }
}
