package org.moises.render;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * Renderer: abstracción sobre OpenGL que expone métodos de dibujo 2D
 * primitivos.
 * <p>
 * Maneja internamente dos VAO/VBO: uno para el quad unitario (drawRect) y otro
 * para geometría dinámica (drawTriangle, drawCircle). El shader soporta offset,
 * escala y rotación (uniforms uOffset, uScale, uAngle, uColor).
 */
public class Renderer {

    // -------------------------------------------------------------------------
    // Uniforms del shader
    // -------------------------------------------------------------------------
    private final int program;
    private final int uModel;
    private final int uColor;

    // -------------------------------------------------------------------------
    // VAO / VBO
    // -------------------------------------------------------------------------
    /**
     * VAO del quad unitario (-0.5 … +0.5), reutilizado para todos los rects.
     */
    private final int quadVao;
    private final int quadVbo;

    /**
     * VAO dinámico para triángulos y círculos.
     */
    private final int dynVao;
    private final int dynVbo;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Inicializa el Renderer con el programa de shaders ya compilado y enlazado.
     *
     * @param program ID del programa OpenGL (vertex + fragment enlazados).
     */
    public Renderer(int program) {
        this.program = program;
        this.uModel = GL20.glGetUniformLocation(program, "uModel");
        this.uColor = GL20.glGetUniformLocation(program, "uColor");

        // Crear quad base.
        int[] quadIds = createStaticVao(new float[] {
                -0.5f, -0.5f, 0f,
                0.5f, -0.5f, 0f,
                0.5f, 0.5f, 0f,
                -0.5f, -0.5f, 0f,
                0.5f, 0.5f, 0f,
                -0.5f, 0.5f, 0f
        });
        quadVao = quadIds[0];
        quadVbo = quadIds[1];

        // Crear VAO dinámico (sin datos por ahora; se llenan en cada llamada).
        dynVao = GL30.glGenVertexArrays();
        dynVbo = GL15.glGenBuffers();
        GL30.glBindVertexArray(dynVao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, dynVbo);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
    }

    // -------------------------------------------------------------------------
    // Matriz de Transformación
    // -------------------------------------------------------------------------

    private final float[] cmat = { 1, 0, 0, 0, 1, 0, 0, 0, 1 };
    private final float[] stack = new float[64 * 9];
    private int stackPtr = 0;

    public void glPushMatrix() {
        System.arraycopy(cmat, 0, stack, stackPtr, 9);
        stackPtr += 9;
    }

    public void glPopMatrix() {
        stackPtr -= 9;
        System.arraycopy(stack, stackPtr, cmat, 0, 9);
    }

    public void glTranslatef(float tx, float ty) {
        cmat[6] += cmat[0] * tx + cmat[3] * ty;
        cmat[7] += cmat[1] * tx + cmat[4] * ty;
    }

    public void glRotatef(float angle) {
        float c = (float) Math.cos(angle);
        float s = (float) Math.sin(angle);
        float m00 = cmat[0], m10 = cmat[1], m01 = cmat[3], m11 = cmat[4];
        cmat[0] = m00 * c + m01 * s;
        cmat[1] = m10 * c + m11 * s;
        cmat[3] = m00 * -s + m01 * c;
        cmat[4] = m10 * -s + m11 * c;
    }

    public void glScalef(float sx, float sy) {
        cmat[0] *= sx;
        cmat[1] *= sx;
        cmat[3] *= sy;
        cmat[4] *= sy;
    }

    // -------------------------------------------------------------------------
    // Métodos de dibujo públicos
    // -------------------------------------------------------------------------

    public void drawRect(float cx, float cy, float w, float h,
            float angle, float r, float g, float b) {
        glPushMatrix();
        glTranslatef(cx, cy);
        glRotatef(angle);
        glScalef(w, h);

        GL20.glUseProgram(program);
        FloatBuffer fb = BufferUtils.createFloatBuffer(9);
        fb.put(cmat).flip();
        GL20.glUniformMatrix3fv(uModel, false, fb);
        GL20.glUniform3f(uColor, r, g, b);
        GL30.glBindVertexArray(quadVao);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
        GL30.glBindVertexArray(0);

        glPopMatrix();
    }

    /**
     * Sobrecarga sin rotación (angle = 0).
     *
     * @see #drawRect(float, float, float, float, float, float, float, float)
     */
    public void drawRect(float cx, float cy, float w, float h,
            float r, float g, float b) {
        drawRect(cx, cy, w, h, 0f, r, g, b);
    }

    /**
     * Dibuja un triángulo definido por tres vértices en NDC,
     * rotados {@code angle} radianes alrededor del centroide.
     *
     * @param x1    vértice 1 X.
     * @param y1    vértice 1 Y.
     * @param x2    vértice 2 X.
     * @param y2    vértice 2 Y.
     * @param x3    vértice 3 X.
     * @param y3    vértice 3 Y.
     * @param angle rotación adicional en radianes.
     * @param r     rojo [0,1].
     * @param g     verde [0,1].
     * @param b     azul [0,1].
     */
    public void drawTriangle(float x1, float y1, float x2, float y2,
            float x3, float y3, float angle,
            float r, float g, float b) {
        float[] verts = {
                x1, y1, 0f,
                x2, y2, 0f,
                x3, y3, 0f
        };
        glPushMatrix();
        glRotatef(angle);
        uploadAndDraw(verts, GL11.GL_TRIANGLES, 3, r, g, b);
        glPopMatrix();
    }

    /**
     * Dibuja un círculo usando un triangle fan.
     *
     * @param cx       centro X en NDC.
     * @param cy       centro Y en NDC.
     * @param radius   radio en NDC.
     * @param segments número de segmentos (≥ 8 para suavidad aceptable).
     * @param angle    rotación adicional en radianes.
     * @param r        rojo [0,1].
     * @param g        verde [0,1].
     * @param b        azul [0,1].
     */
    public void drawCircle(float cx, float cy, float radius, int segments,
            float angle, float r, float g, float b) {
        int count = segments + 2; // centro + periferia + cierre
        float[] verts = new float[count * 3];
        // Centro.
        verts[0] = cx;
        verts[1] = cy;
        verts[2] = 0f;
        for (int i = 0; i <= segments; i++) {
            double theta = 2.0 * Math.PI * i / segments;
            verts[(i + 1) * 3] = cx + radius * (float) Math.cos(theta);
            verts[(i + 1) * 3 + 1] = cy + radius * (float) Math.sin(theta);
            verts[(i + 1) * 3 + 2] = 0f;
        }
        glPushMatrix();
        glRotatef(angle);
        uploadAndDraw(verts, GL11.GL_TRIANGLE_FAN, count, r, g, b);
        glPopMatrix();
    }

    // -------------------------------------------------------------------------
    // Transparencia (alpha)
    // -------------------------------------------------------------------------

    /**
     * Establece el valor del uniform {@code uAlpha} del shader activo.
     * Llamar con 1.0f para dibujos opacos, < 1.0f para overlays translúcidos.
     *
     * @param alpha valor de opacidad [0,1].
     */
    public void setAlpha(float alpha) {
        GL20.glUseProgram(program);
        GL20.glUniform1f(GL20.glGetUniformLocation(program, "uAlpha"), alpha);
    }

    // -------------------------------------------------------------------------
    // Limpieza
    // -------------------------------------------------------------------------

    /**
     * Libera los recursos OpenGL del renderer.
     */
    public void cleanup() {
        GL30.glDeleteVertexArrays(quadVao);
        GL15.glDeleteBuffers(quadVbo);
        GL30.glDeleteVertexArrays(dynVao);
        GL15.glDeleteBuffers(dynVbo);
    }

    // -------------------------------------------------------------------------
    // Auxiliares privados
    // -------------------------------------------------------------------------

    /**
     * Sube vértices al VAO dinámico y lanza la llamada de dibujo.
     * La rotación se aplica en el shader con uAngle, sin pivot (se pasa 0,0
     * porque los vértices ya están en coordenadas mundo).
     */
    private void uploadAndDraw(float[] verts, int mode, int count,
            float r, float g, float b) {
        FloatBuffer buf = BufferUtils.createFloatBuffer(verts.length);
        buf.put(verts).flip();

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, dynVbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buf, GL15.GL_DYNAMIC_DRAW);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        GL20.glUseProgram(program);
        FloatBuffer fb = BufferUtils.createFloatBuffer(9);
        fb.put(cmat).flip();
        GL20.glUniformMatrix3fv(uModel, false, fb);
        GL20.glUniform3f(uColor, r, g, b);

        GL30.glBindVertexArray(dynVao);
        GL11.glDrawArrays(mode, 0, count);
        GL30.glBindVertexArray(0);
    }

    /**
     * Crea un VAO + VBO estático con los vértices dados y devuelve sus IDs.
     *
     * @param vertices arreglo de floats (x, y, z por vértice).
     * @return {@code int[]{vaoId, vboId}}.
     */
    private static int[] createStaticVao(float[] vertices) {
        int vao = GL30.glGenVertexArrays();
        int vbo = GL15.glGenBuffers();
        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        FloatBuffer buf = BufferUtils.createFloatBuffer(vertices.length);
        buf.put(vertices).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buf, GL15.GL_STATIC_DRAW);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
        return new int[] { vao, vbo };
    }

    public void drawExplosion(float cx, float cy, float w, float h) {
        float radius = Math.min(w, h) * 0.25f;
        drawCircle(cx, cy, radius, 18, 0f, 1.0f, 0.65f, 0.12f);

        float inner = radius * 0.65f;
        float outer = radius * 1.45f;
        float spread = 0.35f;
        for (int i = 0; i < 6; i++) {
            float angle = (float) (i * Math.PI / 3.0);
            float ax = cx + (float) Math.cos(angle) * outer;
            float ay = cy + (float) Math.sin(angle) * outer;
            float bx = cx + (float) Math.cos(angle - spread) * inner;
            float by = cy + (float) Math.sin(angle - spread) * inner;
            float cx2 = cx + (float) Math.cos(angle + spread) * inner;
            float cy2 = cy + (float) Math.sin(angle + spread) * inner;
            drawTriangle(ax, ay, bx, by, cx2, cy2, 0f, 1.0f, 0.55f, 0.05f);
        }
    }
}
