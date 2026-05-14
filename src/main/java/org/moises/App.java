package org.moises;

import org.moises.core.Game;

/**
 * App: entry point de la aplicación.
 * Instancia {@link Game} y delega la ejecución completa en él.
 */
public class App {

    /**
     * Punto de entrada JVM.
     *
     * @param args argumentos de línea de comandos (no usados).
     */
    public static void main(String[] args) {
        Game game = new Game();
        game.run();
    }
}
