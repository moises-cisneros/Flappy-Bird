package org.moises;

/**
 * GameState: enum que representa el estado global del juego.
 * <ul>
 *   <li>{@link #WAITING}   — pantalla de inicio, esperando que los jugadores presionen sus teclas.</li>
 *   <li>{@link #PLAYING}   — partida en curso.</li>
 *   <li>{@link #GAME_OVER} — ambos pájaros han muerto; se muestra la pantalla de resultado.</li>
 * </ul>
 */
public enum GameState {
    /** Pantalla de inicio. Muestra instrucciones de controles. */
    WAITING,
    /** Partida activa. La lógica de física/colisiones está habilitada. */
    PLAYING,
    /** Fin de partida. Muestra puntuaciones finales y ganador. */
    GAME_OVER
}
