package org.moises;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

/**
 * Game: bucle principal del Flappy Bird para dos jugadores.
 * <p>
 * Delega la lógica de cada pájaro en {@link Bird}, el dibujo en {@link Renderer},
 * las teclas en {@link InputManager} y el audio en {@link SoundManager}.
 * La dificultad crece con el puntaje según la tabla de niveles del plan.
 */
public class Game {

    // =========================================================================
    // Constantes de ventana
    // =========================================================================
    private static final int   WIN_W = 900;
    private static final int   WIN_H = 700;

    // =========================================================================
    // Constantes de tuberías
    // =========================================================================
    private static final float PIPE_W          = 0.18f;
    private static final float PIPE_CAP_EXTRA  = 0.04f;  // capitel más ancho
    private static final float PIPE_CAP_H      = 0.04f;
    private static final float GAP_H           = 0.46f;
    private static final float GAP_MIN_Y       = -0.40f;
    private static final float GAP_MAX_Y       =  0.40f;
    private static final float GROUND_TOP      = -0.88f; // límite inferior (suelo)

    // =========================================================================
    // Constantes de dificultad progresiva
    // =========================================================================
    private static final float BASE_SPEED      = 0.62f;
    private static final float SPEED_INC       = 0.145f; // incremento por nivel
    private static final float MAX_SPEED       = 1.05f;
    private static final float BASE_SPAWN      = 1.5f;
    private static final float SPAWN_DEC       = 0.2f;   // reducción por nivel
    private static final float MIN_SPAWN       = 0.8f;
    private static final int   PTS_PER_LEVEL   = 5;

    // =========================================================================
    // Recursos OpenGL
    // =========================================================================
    private long         window;
    private int          program;
    private Renderer     renderer;
    private TextRenderer textRenderer;
    private InputManager input;

    // =========================================================================
    // Estado de partida
    // =========================================================================
    private Bird         player1;
    private Bird         player2;
    private GameState    state;
    private final List<Pipe> pipes  = new ArrayList<>();
    private final Random      rng   = new Random();
    private float             timerSpawn;

    // =========================================================================
    // Constantes de nube (posiciones fijas)
    // =========================================================================
    private static final float[][] CLOUDS = {
        {-0.70f, 0.72f}, {-0.20f, 0.80f}, {0.30f, 0.68f},
        {0.65f, 0.78f},  {0.00f, 0.60f}
    };

    // =========================================================================
    // Punto de entrada
    // =========================================================================

    /** Inicializa, ejecuta y limpia el juego. */
    public void run() {
        init();
        resetGame();
        loop();
        cleanup();
    }

    // =========================================================================
    // Inicialización
    // =========================================================================

    private void init() {
        if (!GLFW.glfwInit()) throw new IllegalStateException("No se pudo iniciar GLFW");

        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE,               GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE,             GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE,        GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);

        window = GLFW.glfwCreateWindow(WIN_W, WIN_H, "Flappy Bird 2P — OpenGL", 0, 0);
        if (window == 0) throw new RuntimeException("No se pudo crear la ventana");

        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwSwapInterval(1);
        GLFW.glfwShowWindow(window);
        GL.createCapabilities();

        // Habilitar blending para overlays semitransparentes.
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        program      = buildShaderProgram();
        renderer     = new Renderer(program);
        textRenderer = new TextRenderer(renderer);
        input        = new InputManager(window);

        // Pájaros: P1 amarillo a la izquierda, P2 cian a la derecha.
        player1 = new Bird("P1", -0.45f, 0.98f, 0.85f, 0.20f);
        player2 = new Bird("P2", -0.25f, 0.20f, 0.85f, 0.98f);
    }

    // =========================================================================
    // Shader
    // =========================================================================

    /**
     * Compila y enlaza el vertex + fragment shader.
     * El vertex shader soporta offset, escala y rotación 2D alrededor de un pivot.
     *
     * @return ID del programa OpenGL.
     */
    private int buildShaderProgram() {
        String vert = """
                #version 330 core
                layout (location = 0) in vec3 aPos;
                uniform vec2 uOffset;
                uniform vec2 uScale;
                uniform float uAngle;
                uniform vec2 uPivot;
                void main() {
                    vec2 p = aPos.xy * uScale;
                    float c = cos(uAngle);
                    float s = sin(uAngle);
                    vec2 rotated = vec2(c*p.x - s*p.y, s*p.x + c*p.y);
                    gl_Position = vec4(rotated + uOffset + uPivot, aPos.z, 1.0);
                }
                """;

        String frag = """
                #version 330 core
                uniform vec3 uColor;
                uniform float uAlpha;
                out vec4 fragColor;
                void main() {
                    fragColor = vec4(uColor, uAlpha);
                }
                """;

        int vs = compileShader(vert, GL20.GL_VERTEX_SHADER,   "Vertex");
        int fs = compileShader(frag, GL20.GL_FRAGMENT_SHADER,  "Fragment");

        int prog = GL20.glCreateProgram();
        GL20.glAttachShader(prog, vs);
        GL20.glAttachShader(prog, fs);
        GL20.glLinkProgram(prog);
        if (GL20.glGetProgrami(prog, GL20.GL_LINK_STATUS) == GL11.GL_FALSE)
            throw new RuntimeException("Link error: " + GL20.glGetProgramInfoLog(prog));

        GL20.glDeleteShader(vs);
        GL20.glDeleteShader(fs);

        // Valor por defecto de alpha = 1 (opaco).
        GL20.glUseProgram(prog);
        GL20.glUniform1f(GL20.glGetUniformLocation(prog, "uAlpha"), 1.0f);

        return prog;
    }

    private static int compileShader(String src, int type, String name) {
        int s = GL20.glCreateShader(type);
        GL20.glShaderSource(s, src);
        GL20.glCompileShader(s);
        if (GL20.glGetShaderi(s, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE)
            throw new RuntimeException(name + " shader: " + GL20.glGetShaderInfoLog(s));
        return s;
    }

    // =========================================================================
    // Reset
    // =========================================================================

    /**
     * Reinicia el estado completo de la partida y pasa a WAITING.
     */
    private void resetGame() {
        player1.reset();
        player2.reset();
        pipes.clear();
        timerSpawn = 0f;
        state      = GameState.WAITING;
        updateTitle();
    }

    // =========================================================================
    // Dificultad progresiva
    // =========================================================================

    /** Nivel actual basado en el mejor puntaje de los dos jugadores. */
    private int currentLevel() {
        return Math.min(3, Math.max(player1.score, player2.score) / PTS_PER_LEVEL);
    }

    /** Velocidad de desplazamiento de tuberías según nivel. */
    private float currentSpeed() {
        return Math.min(MAX_SPEED, BASE_SPEED + SPEED_INC * currentLevel());
    }

    /** Intervalo de spawn de tuberías según nivel. */
    private float currentSpawnInterval() {
        return Math.max(MIN_SPAWN, BASE_SPAWN - SPAWN_DEC * currentLevel());
    }

    // =========================================================================
    // Bucle principal
    // =========================================================================

    private void loop() {
        float lastTime = (float) GLFW.glfwGetTime();
        while (!GLFW.glfwWindowShouldClose(window)) {
            float now = (float) GLFW.glfwGetTime();
            float dt  = Math.min(now - lastTime, 0.033f);
            lastTime  = now;

            input.poll();
            processInput();
            update(dt);
            render(now);

            GLFW.glfwSwapBuffers(window);
            GLFW.glfwPollEvents();
        }
    }

    // =========================================================================
    // Input
    // =========================================================================

    private void processInput() {
        if (input.isJustPressed(InputManager.KEY_ESC)) {
            GLFW.glfwSetWindowShouldClose(window, true);
            return;
        }

        if (state == GameState.WAITING) {
            if (input.isJustPressed(InputManager.KEY_SPACE)) {
                state = GameState.PLAYING;
                player1.jump();
                SoundManager.playJump();
            }
            if (input.isJustPressed(InputManager.KEY_W) ||
                input.isJustPressed(InputManager.KEY_UP)) {
                state = GameState.PLAYING;
                player2.jump();
                SoundManager.playJump();
            }
            return;
        }

        if (state == GameState.GAME_OVER) {
            if (input.isJustPressed(InputManager.KEY_SPACE) ||
                input.isJustPressed(InputManager.KEY_R)) {
                resetGame();
            }
            return;
        }

        // PLAYING.
        if (input.isJustPressed(InputManager.KEY_SPACE)) {
            player1.jump();
            SoundManager.playJump();
        }
        if (input.isJustPressed(InputManager.KEY_W) ||
            input.isJustPressed(InputManager.KEY_UP)) {
            player2.jump();
            SoundManager.playJump();
        }
    }

    // =========================================================================
    // Update
    // =========================================================================

    private void update(float dt) {
        if (state != GameState.PLAYING) return;

        player1.update(dt);
        player2.update(dt);

        // Spawn de tuberías.
        timerSpawn += dt;
        if (timerSpawn >= currentSpawnInterval()) {
            timerSpawn = 0f;
            spawnPipe();
        }

        boolean scoredThisFrame = false;
        Iterator<Pipe> it = pipes.iterator();
        while (it.hasNext()) {
            Pipe p = it.next();
            p.x -= currentSpeed() * dt;

            // Puntaje independiente por jugador.
            float pipeRight = p.x + PIPE_W * 0.5f;
            if (player1.alive && !p.scoredP1 && pipeRight < player1.x) {
                p.scoredP1 = true;
                player1.score++;
                scoredThisFrame = true;
            }
            if (player2.alive && !p.scoredP2 && pipeRight < player2.x) {
                p.scoredP2 = true;
                player2.score++;
                scoredThisFrame = true;
            }

            // Colisiones.
            if (player1.alive && collides(player1, p)) {
                player1.alive = false;
            }
            if (player2.alive && collides(player2, p)) {
                player2.alive = false;
            }

            // Eliminar tuberías fuera de pantalla.
            if (p.x + PIPE_W * 0.5f < -1.3f) it.remove();
        }

        if (scoredThisFrame) {
            SoundManager.playPoint();
            updateTitle();
        }

        // Game over cuando ambos pájaros han muerto.
        if (!player1.alive && !player2.alive) {
            state = GameState.GAME_OVER;
            SoundManager.playGameOver();
            updateTitle();
        }
    }

    private void spawnPipe() {
        float gap = GAP_MIN_Y + rng.nextFloat() * (GAP_MAX_Y - GAP_MIN_Y);
        pipes.add(new Pipe(1.25f, gap));
    }

    /**
     * Colisión AABB simplificada: overlap horizontal + pájaro fuera del gap.
     */
    private boolean collides(Bird b, Pipe p) {
        float bL = b.x - Bird.ANCHO * 0.5f;
        float bR = b.x + Bird.ANCHO * 0.5f;
        float bT = b.y + Bird.ALTO  * 0.5f;
        float bB = b.y - Bird.ALTO  * 0.5f;

        float pL = p.x - PIPE_W * 0.5f;
        float pR = p.x + PIPE_W * 0.5f;
        if (bR <= pL || bL >= pR) return false;

        float gapTop = p.gapCenterY + GAP_H * 0.5f;
        float gapBot = p.gapCenterY - GAP_H * 0.5f;
        return bT > gapTop || bB < gapBot;
    }

    // =========================================================================
    // Render
    // =========================================================================

    private void render(float time) {
        GL11.glClearColor(0.10f, 0.17f, 0.30f, 1f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        GL20.glUseProgram(program);
        setAlpha(1.0f);

        drawBackground(time);
        drawPipes();

        // Pájaros compuestos.
        player1.render(renderer, time);
        player2.render(renderer, time);

        drawHUD(time);
        drawOverlay();
    }

    // -------------------------------------------------------------------------
    // Fondo y ambiente
    // -------------------------------------------------------------------------

    /**
     * Dibuja el degradado de cielo (dos grandes triángulos con colores distintos),
     * nubes y suelo.
     */
    private void drawBackground(float time) {
        // Cielo superior (azul claro).
        renderer.drawRect(0f,  0.5f, 2f, 1.0f, 0.40f, 0.65f, 0.90f);
        // Cielo inferior (más cálido).
        renderer.drawRect(0f, -0.2f, 2f, 0.6f, 0.55f, 0.78f, 0.95f);

        // Nubes (parallax lento: se mueven con el tiempo).
        float cloudSpeed = 0.03f;
        for (float[] c : CLOUDS) {
            float cx = (c[0] - (float)(time * cloudSpeed)) % 2.2f;
            if (cx < -1.2f) cx += 2.4f;
            drawCloud(cx, c[1]);
        }

        // Suelo verde oscuro.
        float groundH = 1.0f - Math.abs(GROUND_TOP);
        renderer.drawRect(0f, -1f + groundH * 0.5f, 2f, groundH,
                0.15f, 0.52f, 0.18f);
        // Franja de césped encima del suelo.
        renderer.drawRect(0f, GROUND_TOP + 0.01f, 2f, 0.025f,
                0.25f, 0.72f, 0.28f);
    }

    /** Dibuja una nube como conjunto de 3 rectángulos solapados. */
    private void drawCloud(float cx, float cy) {
        renderer.drawRect(cx,         cy,        0.18f, 0.06f, 0.95f, 0.97f, 1.0f);
        renderer.drawRect(cx - 0.06f, cy + 0.02f, 0.10f, 0.05f, 0.95f, 0.97f, 1.0f);
        renderer.drawRect(cx + 0.05f, cy + 0.03f, 0.09f, 0.04f, 0.95f, 0.97f, 1.0f);
    }

    // -------------------------------------------------------------------------
    // Tuberías con capitel
    // -------------------------------------------------------------------------

    private void drawPipes() {
        for (Pipe p : pipes) {
            float gapTop = p.gapCenterY + GAP_H * 0.5f;
            float gapBot = p.gapCenterY - GAP_H * 0.5f;

            // Tramo superior.
            float hSup = 1.0f - gapTop;
            if (hSup > 0) {
                float yCenSup = gapTop + hSup * 0.5f;
                renderer.drawRect(p.x, yCenSup, PIPE_W, hSup, 0.15f, 0.60f, 0.20f);
                // Capitel superior (borde más ancho en la punta del gap).
                renderer.drawRect(p.x, gapTop - PIPE_CAP_H * 0.5f,
                        PIPE_W + PIPE_CAP_EXTRA, PIPE_CAP_H, 0.18f, 0.70f, 0.22f);
            }

            // Tramo inferior.
            float hInf = gapBot - GROUND_TOP;
            if (hInf > 0) {
                float yCenInf = GROUND_TOP + hInf * 0.5f;
                renderer.drawRect(p.x, yCenInf, PIPE_W, hInf, 0.15f, 0.60f, 0.20f);
                // Capitel inferior.
                renderer.drawRect(p.x, gapBot + PIPE_CAP_H * 0.5f,
                        PIPE_W + PIPE_CAP_EXTRA, PIPE_CAP_H, 0.18f, 0.70f, 0.22f);
            }
        }
    }

    // -------------------------------------------------------------------------
    // HUD
    // -------------------------------------------------------------------------

    private void drawHUD(float time) {
        // Marcador P1 (amarillo, lado izquierdo).
        textRenderer.drawNumber(player1.score, -0.95f, 0.87f, 1.8f,
                player1.color[0], player1.color[1], player1.color[2]);

        // Marcador P2 (cian, lado derecho).
        String s2 = Integer.toString(player2.score);
        float w2  = textRenderer.numberWidth(s2.length(), 1.8f);
        textRenderer.drawNumber(player2.score, 0.95f - w2, 0.87f, 1.8f,
                player2.color[0], player2.color[1], player2.color[2]);

        // Barra de nivel (esquina inferior izquierda).
        drawLevelBar();
    }

    /** Barra de progreso geométrica que indica el nivel de dificultad actual. */
    private void drawLevelBar() {
        float barX  = -0.90f;
        float barY  = -0.93f;
        float barW  = 0.30f;
        float barH  = 0.018f;
        int   level = currentLevel();
        float fill  = level / 3.0f;

        // Marco vacío.
        renderer.drawRect(barX + barW * 0.5f, barY, barW, barH, 0.3f, 0.3f, 0.3f);
        // Relleno proporcional al nivel.
        if (fill > 0) {
            renderer.drawRect(barX + barW * fill * 0.5f, barY,
                    barW * fill, barH - 0.004f,
                    0.98f, 0.70f, 0.10f);
        }
    }

    // -------------------------------------------------------------------------
    // Pantallas overlay
    // -------------------------------------------------------------------------

    private void drawOverlay() {
        if (state == GameState.WAITING) drawWaitingScreen();
        else if (state == GameState.GAME_OVER) drawGameOverScreen();
    }

    /** Pantalla de inicio con instrucciones de controles. */
    private void drawWaitingScreen() {
        setAlpha(0.55f);
        renderer.drawRect(0f, 0f, 2f, 2f, 0.05f, 0.08f, 0.15f);
        setAlpha(1.0f);

        // "P1: SPACE" (amarillo).
        textRenderer.drawNumber(1, -0.55f, 0.22f, 2.5f,
                player1.color[0], player1.color[1], player1.color[2]);
        // "P2: W / UP" (cian).
        textRenderer.drawNumber(2, 0.10f, 0.22f, 2.5f,
                player2.color[0], player2.color[1], player2.color[2]);

        // Línea decorativa.
        renderer.drawRect(0f, 0.00f, 1.2f, 0.006f, 0.5f, 0.5f, 0.6f);
        renderer.drawRect(0f, -0.10f, 1.0f, 0.006f, 0.5f, 0.5f, 0.6f);
    }

    /** Pantalla de game over con puntuaciones finales y ganador. */
    private void drawGameOverScreen() {
        setAlpha(0.60f);
        renderer.drawRect(0f, 0f, 2f, 2f, 0.10f, 0.03f, 0.08f);
        setAlpha(1.0f);

        // Mostrar scores finales.
        textRenderer.drawNumber(player1.score, -0.55f, 0.30f, 2.5f,
                player1.color[0], player1.color[1], player1.color[2]);
        textRenderer.drawNumber(player2.score,  0.10f, 0.30f, 2.5f,
                player2.color[0], player2.color[1], player2.color[2]);

        // Indicador del ganador (barra debajo del marcador mayor).
        if (player1.score >= player2.score) {
            renderer.drawRect(-0.35f, 0.18f, 0.40f, 0.010f,
                    player1.color[0], player1.color[1], player1.color[2]);
        } else {
            renderer.drawRect( 0.30f, 0.18f, 0.40f, 0.010f,
                    player2.color[0], player2.color[1], player2.color[2]);
        }

        // Líneas decorativas (guía para "SPACE = restart").
        renderer.drawRect(0f, -0.05f, 1.0f, 0.006f, 0.5f, 0.5f, 0.6f);
        renderer.drawRect(0f, -0.18f, 0.8f, 0.006f, 0.5f, 0.5f, 0.6f);
    }

    // =========================================================================
    // Auxiliares
    // =========================================================================

    /** Actualiza el título de la ventana con nivel y puntuaciones. */
    private void updateTitle() {
        String title = switch (state) {
            case WAITING   -> "Flappy Bird 2P | SPACE / W para empezar";
            case PLAYING   -> String.format(
                    "Nivel: %d | P1: %d | P2: %d", currentLevel(),
                    player1.score, player2.score);
            case GAME_OVER -> String.format(
                    "GAME OVER | P1: %d | P2: %d | SPACE o R = reiniciar",
                    player1.score, player2.score);
        };
        GLFW.glfwSetWindowTitle(window, title);
    }

    /** Cambia el valor del uniform uAlpha del shader activo. */
    private void setAlpha(float alpha) {
        GL20.glUniform1f(GL20.glGetUniformLocation(program, "uAlpha"), alpha);
    }

    // =========================================================================
    // Limpieza
    // =========================================================================

    private void cleanup() {
        renderer.cleanup();
        GL20.glDeleteProgram(program);
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }
}
