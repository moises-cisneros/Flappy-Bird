package org.moises.ui;

import org.moises.core.Game;
import org.moises.core.InputManager;


/**
 * MenuAction: resultado de la entrada del usuario en el menú principal.
 * Devuelto por {@link InputManager#getMenuAction()} para desacoplar
 * la lógica de navegación de la clase {@link Game}.
 */
public enum MenuAction {
    /**
     * Sin acción este frame.
     */
    NONE,
    /**
     * El usuario eligió modo 1 jugador.
     */
    SELECT_1P,
    /**
     * El usuario eligió modo 2 jugadores.
     */
    SELECT_2P,
    /**
     * El usuario movió el cursor hacia arriba.
     */
    NAV_UP,
    /**
     * El usuario movió el cursor hacia abajo.
     */
    NAV_DOWN
}
