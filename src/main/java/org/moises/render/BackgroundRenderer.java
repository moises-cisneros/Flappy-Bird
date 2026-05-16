package org.moises.render;

/**
 * BackgroundRenderer: dibuja el fondo utilizando formas geométricas básicas.
 * Incluye cielo, montañas, nubes, árboles con parallax, y un Easter Egg de un
 * avión.
 */
public final class BackgroundRenderer {

    // =========================================================================
    // Datos de los elementos (Geometría estática y parallax)
    // =========================================================================
    private static final float[][] MOUNTAINS = {
            { -1.2f, -0.70f, 0.9f, 0.6f }, { -0.4f, -0.70f, 0.7f, 0.45f },
            { 0.5f, -0.70f, 1.0f, 0.7f }, { 1.3f, -0.70f, 0.8f, 0.5f }
    };

    private static final float[][] CLOUDS = {
            { -1.0f, 0.72f }, { -0.3f, 0.80f }, { 0.4f, 0.68f }, { 1.1f, 0.78f }
    };

    private static final float[][] TREES = {
            { -1.3f, -0.85f }, { -0.8f, -0.80f }, { -0.3f, -0.88f },
            { 0.2f, -0.75f }, { 0.7f, -0.82f }, { 1.2f, -0.85f }
    };

    public BackgroundRenderer() {
        // Inicialización vacía
    }

    /**
     * Renderiza el fondo multicapa con efecto parallax.
     *
     * @param renderer Renderer activo usado para dibujar figuras geométricas.
     * @param time     Tiempo de la simulación para la animación continua.
     */
    public void render(Renderer renderer, float time) {

        // 1. CIELO (Estático)
        renderer.drawRect(0f, 0.5f, 2f, 1.0f, 0.40f, 0.65f, 0.90f); // Superior
        renderer.drawRect(0f, -0.2f, 2f, 0.6f, 0.55f, 0.78f, 0.95f); // Inferior

        // 2. MONTAÑAS (Velocidad muy lenta)
        float mountainSpeed = 0.015f;
        for (float[] m : MOUNTAINS) {
            float cx = wrapX(m[0], time, mountainSpeed, -1.8f, 3.6f);
            drawMountain(renderer, cx, m[1], m[2], m[3]);
        }

        // 3. NUBES (Velocidad media)
        float cloudSpeed = 0.03f;
        for (float[] c : CLOUDS) {
            float cx = wrapX(c[0], time, cloudSpeed, -1.8f, 3.6f);
            drawCloud(renderer, cx, c[1]);
        }

        // 4. ÁRBOLES (Velocidad más rápida)
        float treeSpeed = 0.08f;
        for (float[] t : TREES) {
            float cx = wrapX(t[0], time, treeSpeed, -1.8f, 3.6f);
            drawTree(renderer, cx, t[1]);
        }

        // 5. EASTER EGG (Avión con Bandera)
        // Se dibuja al final para que esté frente a los elementos de fondo
        drawEasterEggPlane(renderer, time);
    }

    // =========================================================================
    // Métodos de Dibujo: Elementos de Fondo
    // =========================================================================

    private void drawMountain(Renderer renderer, float cx, float base_y, float w, float h) {
        float r = 0.20f, g = 0.25f, b = 0.35f;
        renderer.drawTriangle(
                cx - w / 2, base_y, cx + w / 2, base_y, cx, base_y + h,
                0f, r, g, b);
    }

    private void drawCloud(Renderer renderer, float cx, float cy) {
        renderer.drawRect(cx, cy, 0.18f, 0.06f, 0.95f, 0.97f, 1.0f);
        renderer.drawRect(cx - 0.06f, cy + 0.02f, 0.10f, 0.05f, 0.95f, 0.97f, 1.0f);
        renderer.drawRect(cx + 0.05f, cy + 0.03f, 0.09f, 0.04f, 0.95f, 0.97f, 1.0f);
    }

    private void drawTree(Renderer renderer, float cx, float base_y) {
        renderer.drawRect(cx, base_y + 0.05f, 0.035f, 0.10f, 0.35f, 0.20f, 0.10f); // Tronco
        renderer.drawTriangle(
                cx - 0.12f, base_y + 0.10f, cx + 0.12f, base_y + 0.10f, cx, base_y + 0.35f, // Hojas
                0f, 0.15f, 0.40f, 0.20f);
    }

    // =========================================================================
    // Métodos de Dibujo: EASTER EGG (Avión)
    // =========================================================================

    /**
     * Dibuja el Easter Egg: un avión rojo que aparece raramente cruzando el cielo
     * arrastrando una bandera flameando.
     */
    private void drawEasterEggPlane(Renderer renderer, float time) {
        // Lógica de aparición: Aparece cada 45 segundos durante 12 segundos.
        float eggPeriod = 45.0f;
        float eggDuration = 12.0f;

        float currentTimeInPeriod = time % eggPeriod;
        if (currentTimeInPeriod > eggDuration)
            return;

        // Trayectoria: Vuela de izquierda a derecha en el cielo superior
        float t_norm = currentTimeInPeriod / eggDuration;
        float cx = -1.3f + t_norm * 2.6f;
        float cy = 0.6f; // Altura de vuelo

        // 5a. Dibujar el Avión (Biplano Rojo Geométrico)
        float planeR = 0.85f, planeG = 0.10f, planeB = 0.10f; // Rojo intenso

        // Cuerpo y cola
        renderer.drawRect(cx, cy, 0.22f, 0.08f, planeR, planeG, planeB); // Fuselaje
        renderer.drawTriangle(
                cx - 0.11f, cy, cx - 0.11f, cy + 0.06f, cx - 0.17f, cy + 0.06f, // Timón de cola
                0f, planeR * 0.9f, planeG, planeB);
        // Alas
        renderer.drawRect(cx + 0.04f, cy + 0.08f, 0.18f, 0.02f, planeR, planeG, planeB); // Ala superior
        renderer.drawRect(cx + 0.04f, cy - 0.08f, 0.18f, 0.02f, planeR, planeG, planeB); // Ala inferior

        // Cuerdas que sujetan la bandera (usando rectángulos muy finos)
        float stringThickness = 0.005f;
        float stringColor = 1.0f; // Blanco/Gris
        renderer.drawRect(cx - 0.11f, cy + 0.02f, 0.1f, stringThickness, stringColor, stringColor, stringColor);
        renderer.drawRect(cx - 0.11f, cy - 0.02f, 0.1f, stringThickness, stringColor, stringColor, stringColor);

        // 5b. Dibujar la Bandera Flameando
        drawWavingFlag(renderer, cx - 0.16f, cy, time);
    }

    /**
     * Dibuja una bandera blanca segmentada que flamea matemáticamente.
     * Incluye una simulación de firma "@cisn3ronauta".
     */
    private void drawWavingFlag(Renderer renderer, float mastilX, float base_y, float time) {
        float flagColorR = 1.0f, flagColorG = 1.0f, flagColorB = 1.0f; // Blanca

        // Propiedades de la bandera
        int segments = 15; // Cuantas tiras verticales usar (más = más suave)
        float flagWidth = 0.45f; // Longitud horizontal total
        float flagHeight = 0.15f; // Altura vertical
        float segmentW = flagWidth / segments;

        // Propiedades de la onda
        float waveAmp = 0.025f; // Intensidad del movimiento vertical
        float waveFrequency = 15.0f; // Cuantas ondas hay a lo largo de la bandera
        float waveSpeed = 8.0f; // Velocidad de flameo

        // Dibujar cada tira de la bandera
        for (int i = 0; i < segments; i++) {
            // Posición X de la tira actual (se dibuja hacia atrás/izquierda del mástil)
            float currentSegX = mastilX - i * segmentW;

            // Calculamos el desfase de la onda seno para flamear.
            // La onda depende del tiempo Y de la distancia horizontal al mástil (i).
            float waveOffset = (float) Math.sin(time * waveSpeed - i * (waveFrequency / segments)) * waveAmp;

            // Dibujamos la tira vertical como un rectángulo estrecho
            renderer.drawRect(
                    currentSegX,
                    base_y + waveOffset, // La altura Y central de la tira sigue la onda
                    segmentW,
                    flagHeight,
                    flagColorR, flagColorG, flagColorB);

            // SIMULACIÓN DE TEXTO (@cisn3ronauta)
            // Dibujamos pequeños "garabatos" negros geométricos
            // solo en las tiras centrales de la bandera para simular el nombre.
            if (i > 3 && i < segments - 2) {
                float garabatoThickness = 0.006f;
                // Dibujamos dos líneas cruzadas abstractas dentro de la tira actual
                renderer.drawRect(
                        currentSegX,
                        base_y + waveOffset + 0.02f, // Ligeramente arriba
                        segmentW * 1.1f, garabatoThickness,
                        0f, 0f, 0f // Negro
                );
                renderer.drawRect(
                        currentSegX,
                        base_y + waveOffset - 0.01f, // Ligeramente abajo
                        segmentW * 0.8f, garabatoThickness * 1.5f,
                        0f, 0f, 0f // Negro
                );
            }
        }
    }

    // =========================================================================
    // Lógica Matemática
    // =========================================================================

    /**
     * Calcula la posición X infinita continua.
     */
    private float wrapX(float startX, float time, float speed, float minX, float span) {
        float x = startX - (time * speed);
        float dist = x - minX;
        float mod = (dist % span + span) % span;
        return minX + mod;
    }

    public void cleanup() {
        // Nada que limpiar
    }
}