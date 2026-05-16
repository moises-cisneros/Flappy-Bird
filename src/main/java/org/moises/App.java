package org.moises;

import org.moises.core.Game;

/**
 * App: entry point de la aplicación.
 * Instancia {@link Game} y delega la ejecución completa en él.
 */
public class App {

    public static void main(String[] args) {
        Game game = new Game();
        game.run();
    }
}
