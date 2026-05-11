package org.moises;

import java.io.BufferedInputStream;
import java.io.InputStream;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.AudioInputStream;

/**
 * SoundManager: carga y reproduce clips de audio WAV usando {@code javax.sound.sampled}.
 * <p>
 * Los recursos se leen desde el classpath (Maven coloca {@code src/main/resources} en él).
 * Si un archivo no existe o falla al cargar, se degrada silenciosamente sin abortar el juego.
 * <p>
 * Uso típico:
 * <pre>
 *   Clip jump = SoundManager.loadClip("/sounds/jump.wav");
 *   SoundManager.play(jump);
 * </pre>
 */
public class SoundManager {

    /** Instancia única (patrón singleton). */
    private static final SoundManager INSTANCE = new SoundManager();

    // -------------------------------------------------------------------------
    // Clips precargados
    // -------------------------------------------------------------------------

    private final Clip clipJump;
    private final Clip clipPoint;
    private final Clip clipGameOver;

    // -------------------------------------------------------------------------
    // Constructor privado
    // -------------------------------------------------------------------------

    private SoundManager() {
        clipJump     = loadClip("/sounds/jump.wav");
        clipPoint    = loadClip("/sounds/point.wav");
        clipGameOver = loadClip("/sounds/gameover.wav");
    }

    // -------------------------------------------------------------------------
    // API pública estática
    // -------------------------------------------------------------------------

    /**
     * Reproduce el sonido de salto.
     */
    public static void playJump() {
        play(INSTANCE.clipJump);
    }

    /**
     * Reproduce el sonido de punto conseguido.
     */
    public static void playPoint() {
        play(INSTANCE.clipPoint);
    }

    /**
     * Reproduce el sonido de game over.
     */
    public static void playGameOver() {
        play(INSTANCE.clipGameOver);
    }

    // -------------------------------------------------------------------------
    // Auxiliares
    // -------------------------------------------------------------------------

    /**
     * Carga un clip WAV desde el classpath.
     *
     * @param classpathPath ruta con barra inicial (p. ej. {@code "/sounds/jump.wav"}).
     * @return el {@link Clip} cargado, o {@code null} si no se pudo cargar.
     */
    public static Clip loadClip(String classpathPath) {
        try (InputStream is = SoundManager.class.getResourceAsStream(classpathPath)) {
            if (is == null) {
                System.err.println("[WARN] Recurso de audio no encontrado: " + classpathPath);
                return null;
            }
            try (AudioInputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(is))) {
                Clip clip = AudioSystem.getClip();
                clip.open(ais);
                return clip;
            }
        } catch (Exception e) {
            System.err.println("[WARN] No se pudo cargar: " + classpathPath + " — " + e.getMessage());
            return null;
        }
    }

    /**
     * Reproduce un clip desde el inicio.
     * Si {@code clip} es {@code null} no hace nada (degradación silenciosa).
     *
     * @param clip clip a reproducir.
     */
    public static void play(Clip clip) {
        if (clip == null) return;
        clip.setFramePosition(0);
        clip.start();
    }
}
