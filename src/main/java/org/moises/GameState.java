package org.moises;

/**
 * GameState: enum que representa el estado global del juego.
 * <ul>
 *   <li>{@link #MAIN_MENU} — pantalla de inicio; selección de modo 1P / 2P.</li>
 *   <li>{@link #WAITING}   — esperando que los jugadores presionen sus teclas.</li>
 *   <li>{@link #PLAYING}   — partida en curso.</li>
 *   <li>{@link #GAME_OVER} — ambos pájaros han muerto; pantalla de resultado.</li>
 * </ul>
 */
public enum GameState {
    /** Pantalla de menú principal. Selección de modo 1 o 2 jugadores. */
    MAIN_MENU,
    /** Pantalla de espera. Instrucciones de controles antes de empezar. */
    WAITING,
    /** Partida activa. La lógica de física/colisiones está habilitada. */
    PLAYING,
    /** Fin de partida. Muestra puntuaciones finales y opciones. */
    GAME_OVER
}
