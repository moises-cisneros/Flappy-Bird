package org.moises;

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
        System.out.println("Flappy Bird - Demo");

        Game game = new Game();
        game.run();
    }
}
