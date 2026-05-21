package org.moises.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.moises.audio.SoundManager;
import org.moises.entity.Bird;
import org.moises.entity.Pipe;
import org.moises.render.BackgroundRenderer;
import org.moises.render.Renderer;
import org.moises.render.TextRenderer;
import org.moises.ui.GameOverMenu;
import org.moises.ui.MainMenu;
import org.moises.ui.MenuAction;

/**
 * Game: bucle principal del Flappy Bird para dos jugadores.
 * <p>
 * Delega la lógica de cada pájaro en {@link Bird}, el dibujo en
 * {@link Renderer},
 * las teclas en {@link InputManager} y el audio en {@link SoundManager}.
 * La dificultad crece con el puntaje según la tabla de niveles del plan.
 */
public class Game {

    // =========================================================================
    // Constantes de ventana
    // =========================================================================
    private static final int WIN_W = 900;
    private static final int WIN_H = 700;

    // =========================================================================
    // Constantes de tuberías
    // =========================================================================
    private static final float PIPE_W = Pipe.PIPE_W;
    private static final float PIPE_CAP_EXTRA = Pipe.CAPITAL_EXTRA_WIDTH; // capitel más ancho
    private static final float GAP_H = Pipe.GAP_H;
    private static final float GAP_MIN_Y = -0.40f;
    private static final float GAP_MAX_Y = 0.40f;
    private static final float GROUND_TOP = -1.00f; // límite inferior (suelo)

    // =========================================================================
    // Constantes de dificultad progresiva
    // =========================================================================
    private static final float BASE_SPEED = 0.62f;
    private static final float SPEED_INC = 0.145f; // incremento por nivel
    private static final float MAX_SPEED = 1.05f;
    private static final float BASE_SPAWN = 1.5f;
    private static final float SPAWN_DEC = 0.2f; // reducción por nivel
    private static final float MIN_SPAWN = 0.8f;
    private static final int PTS_PER_LEVEL = 5;
    private static final int MAX_LEVEL = 8;

    // =========================================================================
    // Constantes de viewport (M8)
    // =========================================================================
    private static final float TARGET_ASPECT = (float) WIN_W / WIN_H;

    // =========================================================================
    // Recursos OpenGL
    // =========================================================================
    private long window;
    private int program;
    private Renderer renderer;
    private TextRenderer textRenderer;
    private BackgroundRenderer background;
    private InputManager input;
    private MainMenu mainMenu;
    private GameOverMenu gameOverMenu;

    // =========================================================================
    // Viewport actual (para conversión mouse → NDC)
    // =========================================================================
    private int vpX, vpY, vpW, vpH;

    // =========================================================================
    // Estado de ratón
    // =========================================================================
    private double cursorX, cursorY;
    private boolean mouseClicked;

    // =========================================================================
    // Estado de partida
    // =========================================================================
    private Bird player1;
    private Bird player2;
    private Bird player3;
    private GameState state = GameState.MAIN_MENU;
    private int playerCount = 1;
    private boolean antiGravityEnabled = true;
    private int antiGravityThreshold = 1;
    private final List<Pipe> pipes = new ArrayList<>();
    private final Random rng = new Random();
    private float timerSpawn;

    // =========================================================================
    // Punto de entrada
    // =========================================================================

    /**
     * Inicializa, ejecuta y limpia el juego.
     */
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
        if (!GLFW.glfwInit())
            throw new IllegalStateException("No se pudo iniciar GLFW");

        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);

        window = GLFW.glfwCreateWindow(WIN_W, WIN_H, "Flappy Bird 2P — OpenGL", 0, 0);
        if (window == 0)
            throw new RuntimeException("No se pudo crear la ventana");

        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwSwapInterval(1);
        GLFW.glfwShowWindow(window);
        GL.createCapabilities();

        // Habilitar blending para overlays semitransparentes.
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);

        // Registrar callback de viewport responsivo (letterbox/pillarbox).
        GLFW.glfwSetFramebufferSizeCallback(window, (win, w, h) -> adjustViewport(w, h));
        adjustViewport(WIN_W, WIN_H);

        program = buildShaderProgram();
        renderer = new Renderer(program);
        background = new BackgroundRenderer();
        textRenderer = new TextRenderer(renderer);
        input = new InputManager(window);
        mainMenu = new MainMenu(renderer, textRenderer);
        gameOverMenu = new GameOverMenu(renderer, textRenderer);

        // Mouse: rastrear cursor y clics para interacción con menús.
        GLFW.glfwSetCursorPosCallback(window, (win, x, y) -> {
            cursorX = x;
            cursorY = y;
        });
        GLFW.glfwSetMouseButtonCallback(window, (win, btn, action, mods) -> {
            if (btn == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_PRESS)
                mouseClicked = true;
        });

        // Pájaros: P1 amarillo a la izquierda, P2 cian a la derecha, P3 magenta al
        // centro.
        player1 = new Bird("P1", -0.45f, 0.98f, 0.85f, 0.20f);
        player2 = new Bird("P2", -0.25f, 0.20f, 0.85f, 0.98f);
        player3 = new Bird("P3", -0.05f, 1.00f, 0.20f, 0.80f);

        mainMenu.setAntiGravityEnabled(antiGravityEnabled);
        mainMenu.setAntiGravityThreshold(antiGravityThreshold);
    }

    /**
     * Ajusta el viewport con lógica letterbox/pillarbox para preservar el
     * aspect ratio 900x700 independientemente del tamaño de la ventana.
     * Guarda los parámetros del viewport para la conversión de coordenadas de
     * mouse.
     *
     * @param fbW ancho del framebuffer en píxeles.
     * @param fbH alto del framebuffer en píxeles.
     */
    private void adjustViewport(int fbW, int fbH) {
        float windowAspect = (float) fbW / fbH;
        if (windowAspect >= TARGET_ASPECT) {
            vpH = fbH;
            vpW = (int) (fbH * TARGET_ASPECT);
            vpX = (fbW - vpW) / 2;
            vpY = 0;
        } else {
            vpW = fbW;
            vpH = (int) (fbW / TARGET_ASPECT);
            vpX = 0;
            vpY = (fbH - vpH) / 2;
        }
        GL11.glViewport(vpX, vpY, vpW, vpH);
    }

    /**
     * Convierte coordenadas de pantalla (píxeles GLFW) a NDC [-1, +1].
     * Tiene en cuenta el viewport letterboxed actual.
     *
     * @param sx píxel X del cursor.
     * @param sy píxel Y del cursor (origen arriba-izquierda en GLFW).
     * @return {@code float[]{ndcX, ndcY}}.
     */
    private float[] screenToNdc(double sx, double sy) {
        float ndcX = 2.0f * (float) (sx - vpX) / vpW - 1.0f;
        float ndcY = -2.0f * (float) (sy - vpY) / vpH + 1.0f; // GLFW Y invertido
        return new float[] { ndcX, ndcY };
    }

    /**
     * Devuelve {@code true} si el punto NDC (nx, ny) está dentro del rect centrado.
     */
    private static boolean hitTest(float nx, float ny,
            float cx, float cy, float w, float h) {
        return Math.abs(nx - cx) <= w * 0.5f && Math.abs(ny - cy) <= h * 0.5f;
    }

    private static boolean hitTest(float nx, float ny,
            float cy, float w, float h) {
        return hitTest(nx, ny, 0.0f, cy, w, h);
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
                uniform mat3 uModel;
                void main() {
                    vec3 pos = uModel * vec3(aPos.xy, 1.0);
                    gl_Position = vec4(pos.xy, aPos.z, 1.0);
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

        int vs = compileShader(vert, GL20.GL_VERTEX_SHADER, "Vertex");
        int fs = compileShader(frag, GL20.GL_FRAGMENT_SHADER, "Fragment");

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
     * Reinicia el estado completo de la partida y pasa a MAIN_MENU.
     */
    private void resetGame() {
        player1.reset();
        player2.reset();
        player3.reset();
        pipes.clear();
        timerSpawn = 0f;
        state = GameState.MAIN_MENU;
        updateTitle();
    }

    /**
     * Inicia una partida en el modo indicado desde MAIN_MENU.
     *
     * @param players cantidad de jugadores.
     */
    private void startGame(int players) {
        this.playerCount = players;
        applyAntiGravitySettings();
        player1.reset();
        player2.reset();
        player3.reset();
        pipes.clear();
        timerSpawn = 0f;
        state = GameState.WAITING;
        updateTitle();
    }

    // =========================================================================
    // Dificultad progresiva
    // =========================================================================

    /**
     * Nivel actual basado en el mejor puntaje de los dos jugadores.
     */
    private int currentLevel() {
        int maxScore = player1.score;
        if (playerCount >= 2)
            maxScore = Math.max(maxScore, player2.score);
        if (playerCount == 3)
            maxScore = Math.max(maxScore, player3.score);
        return Math.min(MAX_LEVEL, maxScore / PTS_PER_LEVEL);
    }

    /**
     * Velocidad de desplazamiento de tuberías según nivel.
     */
    private float currentSpeed() {
        return Math.min(MAX_SPEED, BASE_SPEED + SPEED_INC * currentLevel());
    }

    /**
     * Intervalo de spawn de tuberías según nivel.
     */
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
            float dt = Math.min(now - lastTime, 0.033f);
            lastTime = now;

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
        // ESC siempre disponible: desde el juego vuelve al menú; desde menú cierra.
        if (input.isJustPressed(InputManager.KEY_ESC)) {
            if (state == GameState.MAIN_MENU) {
                GLFW.glfwSetWindowShouldClose(window, true);
            } else {
                resetGame(); // vuelve a MAIN_MENU
            }
            return;
        }

        // MAIN_MENU — navegación por teclado y ratón.
        if (state == GameState.MAIN_MENU) {
            MenuAction action = input.getMenuAction();
            switch (action) {
                case SELECT_1P -> startGame(1);
                case SELECT_2P -> startGame(2);
                case SELECT_3P -> startGame(3);
                case NAV_UP -> mainMenu.navigateUp();
                case NAV_DOWN -> mainMenu.navigateDown();
                case NONE -> {
                    if (input.isJustPressed(InputManager.KEY_ENTER) ||
                            input.isJustPressed(InputManager.KEY_SPACE)) {
                        startGame(mainMenu.getSelectedOption() + 1);
                    }
                }
            }
            // --- Soporte de ratón en menú principal ---
            if (mouseClicked) {
                float[] ndc = screenToNdc(cursorX, cursorY);
                if (hitTest(ndc[0], ndc[1], MainMenu.TOGGLE_X, MainMenu.CONFIG_TOGGLE_Y,
                        MainMenu.TOGGLE_W, MainMenu.TOGGLE_H)) {
                    mainMenu.toggleAntiGravity();
                    antiGravityEnabled = mainMenu.isAntiGravityEnabled();
                } else if (hitTest(ndc[0], ndc[1], MainMenu.THRESH_DEC_X, MainMenu.CONFIG_THRESHOLD_Y,
                        MainMenu.THRESH_BTN_W, MainMenu.THRESH_BTN_H)) {
                    mainMenu.decrementAntiGravityThreshold();
                    antiGravityThreshold = mainMenu.getAntiGravityThreshold();
                } else if (hitTest(ndc[0], ndc[1], MainMenu.THRESH_INC_X, MainMenu.CONFIG_THRESHOLD_Y,
                        MainMenu.THRESH_BTN_W, MainMenu.THRESH_BTN_H)) {
                    mainMenu.incrementAntiGravityThreshold();
                    antiGravityThreshold = mainMenu.getAntiGravityThreshold();
                } else if (hitTest(ndc[0], ndc[1], MainMenu.OPT_1P_Y, MainMenu.CARD_W, MainMenu.CARD_H)) {
                    startGame(1);
                } else if (hitTest(ndc[0], ndc[1], MainMenu.OPT_2P_Y, MainMenu.CARD_W, MainMenu.CARD_H)) {
                    startGame(2);
                } else if (hitTest(ndc[0], ndc[1], MainMenu.OPT_3P_Y, MainMenu.CARD_W, MainMenu.CARD_H)) {
                    startGame(3);
                }
                mouseClicked = false;
            }
            return;
        }

        if (state == GameState.WAITING) {
            if (input.isJustPressed(InputManager.KEY_SPACE)) {
                state = GameState.PLAYING;
                player1.jump();
                SoundManager.playJump();
            }
            if (playerCount >= 2 && input.isJustPressed(InputManager.KEY_W)) {
                state = GameState.PLAYING;
                player2.jump();
                SoundManager.playJump();
            }
            if (playerCount == 3 && input.isJustPressed(InputManager.KEY_UP)) {
                state = GameState.PLAYING;
                player3.jump();
                SoundManager.playJump();
            }
            return;
        }

        // GAME_OVER — Retry (R/SPACE) o Menú Principal (M/ESC).
        if (state == GameState.GAME_OVER) {
            if (input.isJustPressed(InputManager.KEY_R) ||
                    input.isJustPressed(InputManager.KEY_SPACE)) {
                startGame(playerCount);
            }
            if (input.isJustPressed(InputManager.KEY_M)) {
                resetGame();
            }
            // --- Soporte de ratón en menú game over ---
            if (mouseClicked) {
                float[] ndc = screenToNdc(cursorX, cursorY);
                if (hitTest(ndc[0], ndc[1], -0.20f, 1.0f, 0.10f))
                    startGame(playerCount); // Retry
                else if (hitTest(ndc[0], ndc[1], -0.32f, 1.0f, 0.10f))
                    resetGame(); // Menú Principal
                mouseClicked = false;
            }
            return;
        }

        // PLAYING.
        if (input.isJustPressed(InputManager.KEY_SPACE)) {
            player1.jump();
            SoundManager.playJump();
        }
        if (playerCount >= 2 && input.isJustPressed(InputManager.KEY_W)) {
            player2.jump();
            SoundManager.playJump();
        }
        if (playerCount == 3 && input.isJustPressed(InputManager.KEY_UP)) {
            player3.jump();
            SoundManager.playJump();
        }
    }

    // =========================================================================
    // Update
    // =========================================================================

    private void update(float dt) {
        if (state != GameState.PLAYING)
            return;

        boolean p1WasAlive = player1.alive;
        boolean p2WasAlive = player2.alive;
        boolean p3WasAlive = player3.alive;

        player1.update(dt);
        if (playerCount >= 2)
            player2.update(dt);
        if (playerCount == 3)
            player3.update(dt);

        if (p1WasAlive && !player1.alive)
            SoundManager.playGameOver();
        if (playerCount >= 2 && p2WasAlive && !player2.alive)
            SoundManager.playGameOver();
        if (playerCount == 3 && p3WasAlive && !player3.alive)
            SoundManager.playGameOver();

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
            if (playerCount >= 2 && player2.alive && !p.scoredP2 && pipeRight < player2.x) {
                p.scoredP2 = true;
                player2.score++;
                scoredThisFrame = true;
            }
            if (playerCount == 3 && player3.alive && !p.scoredP3 && pipeRight < player3.x) {
                p.scoredP3 = true;
                player3.score++;
                scoredThisFrame = true;
            }

            // Colisiones.
            if (player1.alive && p.collides(player1)) {
                player1.die();
                SoundManager.playGameOver();
            }
            if (playerCount >= 2 && player2.alive && p.collides(player2)) {
                player2.die();
                SoundManager.playGameOver();
            }
            if (playerCount == 3 && player3.alive && p.collides(player3)) {
                player3.die();
                SoundManager.playGameOver();
            }

            // Eliminar tuberías fuera de pantalla.
            if (p.x + PIPE_W * 0.5f < -1.3f)
                it.remove();
        }

        if (scoredThisFrame) {
            SoundManager.playPoint();
            updateTitle();
        }

        // Game over cuando todos los pájaros en juego han muerto.
        boolean gameOver = false;
        if (playerCount == 1)
            gameOver = !player1.alive;
        else if (playerCount == 2)
            gameOver = !player1.alive && !player2.alive;
        else if (playerCount == 3)
            gameOver = !player1.alive && !player2.alive && !player3.alive;

        if (gameOver && state != GameState.GAME_OVER) {
            state = GameState.GAME_OVER;
            updateTitle();
        }
    }

    private void spawnPipe() {
        float gap = GAP_MIN_Y + rng.nextFloat() * (GAP_MAX_Y - GAP_MIN_Y);
        pipes.add(new Pipe(1.25f, gap));
    }

    // =========================================================================
    // Render
    // =========================================================================

    private void render(float time) {
        GL11.glClearColor(0.10f, 0.17f, 0.30f, 1f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        GL20.glUseProgram(program);
        setAlpha(1.0f);

        // Dispatch de renderizado por estado.
        switch (state) {
            case MAIN_MENU -> {
                drawBackground();
                mainMenu.render();
            }
            case WAITING, PLAYING -> {
                drawBackground();
                drawPipes();
                player1.render(renderer, time);
                if (playerCount >= 2)
                    player2.render(renderer, time);
                if (playerCount == 3)
                    player3.render(renderer, time);
                drawHUD();
                if (state == GameState.WAITING)
                    drawWaitingScreen();
            }
            case GAME_OVER -> {
                drawBackground();
                drawPipes();
                player1.render(renderer, time);
                if (playerCount >= 2)
                    player2.render(renderer, time);
                if (playerCount == 3)
                    player3.render(renderer, time);
                drawHUD();
                gameOverMenu.render(player1.score, player2.score, player3.score,
                        player1.color, player2.color, player3.color, playerCount);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Fondo y ambiente
    // -------------------------------------------------------------------------

    /**
     * Dibuja el fondo geométrico.
     */
    private void drawBackground() {
        background.render(renderer, (float) GLFW.glfwGetTime());
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
                renderer.drawRect(p.x, gapTop - Pipe.PIPE_CAP_H * 0.5f,
                        PIPE_W + PIPE_CAP_EXTRA, Pipe.PIPE_CAP_H, 0.18f, 0.70f, 0.22f);
            }

            // Tramo inferior.
            float hInf = gapBot - GROUND_TOP;
            if (hInf > 0) {
                float yCenInf = GROUND_TOP + hInf * 0.5f;
                renderer.drawRect(p.x, yCenInf, PIPE_W, hInf, 0.15f, 0.60f, 0.20f);
                // Capitel inferior.
                renderer.drawRect(p.x, gapBot + Pipe.PIPE_CAP_H * 0.5f,
                        PIPE_W + PIPE_CAP_EXTRA, Pipe.PIPE_CAP_H, 0.18f, 0.70f, 0.22f);
            }
        }
    }

    // -------------------------------------------------------------------------
    // HUD
    // -------------------------------------------------------------------------

    private void drawHUD() {
        // Marcador P1 (amarillo, lado izquierdo).
        textRenderer.drawNumber(player1.score, -0.95f, 0.87f, 1.8f,
                player1.color[0], player1.color[1], player1.color[2]);

        if (playerCount >= 2) {
            // Marcador P2 (cian, lado derecho).
            String s2 = Integer.toString(player2.score);
            float w2 = textRenderer.numberWidth(s2.length(), 1.8f);
            textRenderer.drawNumber(player2.score, 0.95f - w2, 0.87f, 1.8f,
                    player2.color[0], player2.color[1], player2.color[2]);
        }

        if (playerCount == 3) {
            // Marcador P3 (magenta, centro).
            String s3 = Integer.toString(player3.score);
            float w3 = textRenderer.numberWidth(s3.length(), 1.8f);
            textRenderer.drawNumber(player3.score, -w3 * 0.5f, 0.87f, 1.8f,
                    player3.color[0], player3.color[1], player3.color[2]);
        }

        // Barra de nivel (esquina inferior izquierda).
        drawLevelBar();
    }

    /**
     * Barra de progreso geométrica que indica el nivel de dificultad actual.
     */
    private void drawLevelBar() {
        float barX = -0.90f;
        float barY = -0.93f;
        float barW = 0.30f;
        float barH = 0.018f;
        int level = currentLevel();

        // Marco vacío.
        renderer.drawRect(barX + barW * 0.5f, barY, barW, barH, 0.3f, 0.3f, 0.3f);

        // Relleno dividido en segmentos equitativos
        float segmentW = barW / MAX_LEVEL; // división equitativa (ancho / nivelesTotales)
        for (int i = 0; i < level; i++) {
            renderer.drawRect(barX + (i * segmentW) + (segmentW * 0.5f), barY,
                    segmentW - 0.005f, barH - 0.004f,
                    0.98f, 0.70f, 0.10f);
        }
    }

    // -------------------------------------------------------------------------
    // Pantallas overlay
    // -------------------------------------------------------------------------

    /**
     * Pantalla de espera con instrucciones de controles.
     */
    private void drawWaitingScreen() {
        setAlpha(0.55f);
        renderer.drawRect(0f, 0f, 2f, 2f, 0.05f, 0.08f, 0.15f);
        setAlpha(1.0f);

        // P1: SPACE (amarillo).
        textRenderer.drawNumber(1, -0.55f, 0.22f, 2.5f,
                player1.color[0], player1.color[1], player1.color[2]);
        if (playerCount >= 2) {
            // P2: W (cian).
            textRenderer.drawNumber(2, 0.10f, 0.22f, 2.5f,
                    player2.color[0], player2.color[1], player2.color[2]);
        }
        if (playerCount == 3) {
            // P3: UP (magenta).
            textRenderer.drawNumber(3, 0.75f, 0.22f, 2.5f,
                    player3.color[0], player3.color[1], player3.color[2]);
        }
        renderer.drawRect(0f, 0.00f, 1.2f, 0.006f, 0.5f, 0.5f, 0.6f);
        renderer.drawRect(0f, -0.10f, 1.0f, 0.006f, 0.5f, 0.5f, 0.6f);
    }

    private void applyAntiGravitySettings() {
        player1.setAntiGravityEnabled(antiGravityEnabled);
        player1.setAntiGravityThreshold(antiGravityThreshold);
        player2.setAntiGravityEnabled(antiGravityEnabled);
        player2.setAntiGravityThreshold(antiGravityThreshold);
        player3.setAntiGravityEnabled(antiGravityEnabled);
        player3.setAntiGravityThreshold(antiGravityThreshold);
    }

    // =========================================================================
    // Auxiliares
    // =========================================================================

    /**
     * Actualiza el título de la ventana con nivel y puntuaciones.
     */
    private void updateTitle() {
        String title = switch (state) {
            case MAIN_MENU -> "Flappy Bird | 1=Un Jugador  2=Dos Jugadores  3=Tres Jugadores";
            case WAITING -> switch (playerCount) {
                case 1 -> "Flappy Bird 1P | SPACE para empezar";
                case 2 -> "Flappy Bird 2P | SPACE / W para empezar";
                default -> "Flappy Bird 3P | SPACE / W / UP para empezar";
            };
            case PLAYING -> switch (playerCount) {
                case 1 -> String.format("Nivel: %d | Score: %d",
                        currentLevel(), player1.score);
                case 2 -> String.format("Nivel: %d | P1: %d | P2: %d",
                        currentLevel(), player1.score, player2.score);
                default -> String.format("Nivel: %d | P1: %d | P2: %d | P3: %d",
                        currentLevel(), player1.score, player2.score, player3.score);
            };
            case GAME_OVER -> switch (playerCount) {
                case 1 -> String.format("GAME OVER | Score:%d | R=Retry M=Menú",
                        player1.score);
                case 2 -> String.format("GAME OVER | P1:%d P2:%d | R=Retry M=Menú",
                        player1.score, player2.score);
                default -> String.format("GAME OVER | P1:%d P2:%d P3:%d | R=Retry M=Menú",
                        player1.score, player2.score, player3.score);
            };
        };
        GLFW.glfwSetWindowTitle(window, title);
    }

    /**
     * Cambia el valor del uniform uAlpha del shader activo.
     */
    private void setAlpha(float alpha) {
        GL20.glUniform1f(GL20.glGetUniformLocation(program, "uAlpha"), alpha);
    }

    // =========================================================================
    // Limpieza
    // =========================================================================

    private void cleanup() {
        background.cleanup();
        renderer.cleanup();
        GL20.glDeleteProgram(program);
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }
}
