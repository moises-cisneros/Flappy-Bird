package org.moises.core;

import org.moises.ui.MenuAction;


import org.lwjgl.glfw.GLFW;

/**
 * InputManager: encapsula el polling de GLFW y expone detección de flanco
 * ascendente (just-pressed) para cada tecla de interés.
 * <p>
 * Se usa un arreglo de estados anteriores para distinguir entre
 * "tecla recién presionada" (flanco) y "tecla mantenida".
 * <p>
 * Versión 2: añade teclas 1, 2, M, DOWN para el sistema de menús (REQ-07).
 */
public class InputManager {

    // -------------------------------------------------------------------------
    // Teclas monitoreadas (índices internos de este manager)
    // -------------------------------------------------------------------------

    /** Índice interno para SPACE (salto P1 / confirmar). */
    public static final int KEY_SPACE  = 0;
    /** Índice interno para W (salto P2 alternativo). */
    public static final int KEY_W      = 1;
    /** Índice interno para UP (salto P2 / nav menú arriba). */
    public static final int KEY_UP     = 2;
    /** Índice interno para R (restart). */
    public static final int KEY_R      = 3;
    /** Índice interno para ESC (salir / volver al menú). */
    public static final int KEY_ESC    = 4;
    /** Índice interno para tecla 1 (modo 1 jugador). */
    public static final int KEY_1      = 5;
    /** Índice interno para tecla 2 (modo 2 jugadores). */
    public static final int KEY_2      = 6;
    /** Índice interno para M (volver al menú principal). */
    public static final int KEY_M      = 7;
    /** Índice interno para DOWN (nav menú abajo). */
    public static final int KEY_DOWN   = 8;
    /** Índice interno para ENTER (confirmar selección). */
    public static final int KEY_ENTER  = 9;

    private static final int KEY_COUNT = 10;

    /** Mapa de índice interno → código GLFW. */
    private static final int[] GLFW_KEYS = {
            GLFW.GLFW_KEY_SPACE,
            GLFW.GLFW_KEY_W,
            GLFW.GLFW_KEY_UP,
            GLFW.GLFW_KEY_R,
            GLFW.GLFW_KEY_ESCAPE,
            GLFW.GLFW_KEY_1,
            GLFW.GLFW_KEY_2,
            GLFW.GLFW_KEY_M,
            GLFW.GLFW_KEY_DOWN,
            GLFW.GLFW_KEY_ENTER
    };

    // -------------------------------------------------------------------------
    // Estado
    // -------------------------------------------------------------------------

    /** Handle de la ventana GLFW para consultar el estado de teclas. */
    private final long window;

    /** Estado anterior de cada tecla (true = estaba presionada). */
    private final boolean[] prev = new boolean[KEY_COUNT];

    /** Estado actual de cada tecla en este frame. */
    private final boolean[] curr = new boolean[KEY_COUNT];

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Crea el InputManager ligado a la ventana dada.
     *
     * @param window handle de ventana GLFW.
     */
    public InputManager(long window) {
        this.window = window;
    }

    // -------------------------------------------------------------------------
    // Métodos públicos
    // -------------------------------------------------------------------------

    /**
     * Actualiza el estado interno consultando GLFW.
     * Debe llamarse una vez por frame ANTES de usar {@link #isJustPressed}.
     */
    public void poll() {
        for (int i = 0; i < KEY_COUNT; i++) {
            prev[i] = curr[i];
            curr[i] = GLFW.glfwGetKey(window, GLFW_KEYS[i]) == GLFW.GLFW_PRESS;
        }
    }

    /**
     * Devuelve {@code true} solo en el primer frame en que la tecla fue presionada
     * (detección de flanco ascendente).
     *
     * @param keyIndex una de las constantes {@code KEY_*} de esta clase.
     * @return {@code true} si la tecla pasó de no presionada a presionada en este frame.
     */
    public boolean isJustPressed(int keyIndex) {
        return curr[keyIndex] && !prev[keyIndex];
    }

    /**
     * Devuelve {@code true} mientras la tecla esté presionada (mantenida).
     *
     * @param keyIndex una de las constantes {@code KEY_*} de esta clase.
     * @return {@code true} si la tecla está presionada ahora mismo.
     */
    public boolean isHeld(int keyIndex) {
        return curr[keyIndex];
    }

    /**
     * Evalúa las teclas de navegación del menú principal y devuelve la acción
     * correspondiente. Las prioridades son: 1/2 directos → ENTER → NAV_UP/DOWN.
     *
     * @return la {@link MenuAction} detectada en este frame, o {@link MenuAction#NONE}.
     */
    public MenuAction getMenuAction() {
        if (isJustPressed(KEY_1))    return MenuAction.SELECT_1P;
        if (isJustPressed(KEY_2))    return MenuAction.SELECT_2P;
        if (isJustPressed(KEY_ENTER) || isJustPressed(KEY_SPACE))
                                     return MenuAction.SELECT_1P; // confirmar opción actual → se resuelve en Game
        if (isJustPressed(KEY_UP))   return MenuAction.NAV_UP;
        if (isJustPressed(KEY_DOWN)) return MenuAction.NAV_DOWN;
        return MenuAction.NONE;
    }
}
