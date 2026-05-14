package org.moises;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.stb.STBTruetype;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class TextRenderer {

    private static final int BITMAP_W = 512;
    private static final int BITMAP_H = 512;
    private static final float FONT_HEIGHT = 48.0f;

    private int program;
    private int vao, vbo;
    private int textureId;
    private STBTTBakedChar.Buffer cdata;

    private int uProjection;
    private int uColor;

    private float[] orthoMatrix;

    public TextRenderer(Renderer renderer) {
        initShader();
        initFont();
        initGL();

        orthoMatrix = new float[16];
        ortho(0, 900, 700, 0, -1, 1, orthoMatrix);
    }

    private void initShader() {
        String vert = """
                #version 330 core
                layout (location = 0) in vec4 aPosTex;
                uniform mat4 uProjection;
                out vec2 TexCoords;
                void main() {
                    gl_Position = uProjection * vec4(aPosTex.xy, 0.0, 1.0);
                    TexCoords = aPosTex.zw;
                }
                """;
        String frag = """
                #version 330 core
                in vec2 TexCoords;
                out vec4 color;
                uniform sampler2D text;
                uniform vec3 uColor;
                void main() {
                    float alpha = texture(text, TexCoords).r;
                    color = vec4(uColor, alpha);
                }
                """;

        int vs = compileShader(vert, GL20.GL_VERTEX_SHADER);
        int fs = compileShader(frag, GL20.GL_FRAGMENT_SHADER);

        program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vs);
        GL20.glAttachShader(program, fs);
        GL20.glLinkProgram(program);

        uProjection = GL20.glGetUniformLocation(program, "uProjection");
        uColor = GL20.glGetUniformLocation(program, "uColor");
    }

    private int compileShader(String src, int type) {
        int s = GL20.glCreateShader(type);
        GL20.glShaderSource(s, src);
        GL20.glCompileShader(s);
        return s;
    }

    private void initFont() {
        try {
            InputStream is = TextRenderer.class.getResourceAsStream("/fonts/flappy.ttf");
            if (is == null) throw new RuntimeException("Font not found");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int read;
            while ((read = is.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }
            byte[] fontData = baos.toByteArray();
            ByteBuffer ttf = BufferUtils.createByteBuffer(fontData.length);
            ttf.put(fontData).flip();

            cdata = STBTTBakedChar.malloc(96);
            ByteBuffer bitmap = BufferUtils.createByteBuffer(BITMAP_W * BITMAP_H);

            STBTruetype.stbtt_BakeFontBitmap(ttf, FONT_HEIGHT, bitmap, BITMAP_W, BITMAP_H, 32, cdata);

            textureId = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RED, BITMAP_W, BITMAP_H, 0, GL11.GL_RED, GL11.GL_UNSIGNED_BYTE, bitmap);

            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initGL() {
        vao = GL30.glGenVertexArrays();
        vbo = GL15.glGenBuffers();

        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, 4 * 6 * Float.BYTES, GL15.GL_DYNAMIC_DRAW);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 4, GL11.GL_FLOAT, false, 4 * Float.BYTES, 0);

        GL30.glBindVertexArray(0);
    }

    public void drawNumber(int value, float ndcX, float ndcY, float scale, float r, float g, float b) {
        drawText(Integer.toString(value), ndcX, ndcY, scale, r, g, b);
    }

    public void drawText(String text, float ndcX, float ndcY, float scale, float r, float g, float b) {
        float px = (ndcX + 1.0f) * 0.5f * 900f;
        float py = (1.0f - ndcY) * 0.5f * 700f;
        float finalScale = scale * 0.55f; 

        GL20.glUseProgram(program);
        GL20.glUniform3f(uColor, r, g, b);

        FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);
        matrixBuffer.put(orthoMatrix).flip();
        GL20.glUniformMatrix4fv(uProjection, false, matrixBuffer);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL20.glUniform1i(GL20.glGetUniformLocation(program, "text"), 0);

        GL30.glBindVertexArray(vao);

        FloatBuffer xBuf = BufferUtils.createFloatBuffer(1);
        FloatBuffer yBuf = BufferUtils.createFloatBuffer(1);

        STBTTAlignedQuad q = STBTTAlignedQuad.malloc();

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        float currentX = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < 32 || c >= 128) continue;

            xBuf.put(0, currentX);
            yBuf.put(0, 0);
            
            STBTruetype.stbtt_GetBakedQuad(cdata, BITMAP_W, BITMAP_H, c - 32, xBuf, yBuf, q, true);

            float x0 = px + q.x0() * finalScale;
            float y0 = py + q.y0() * finalScale;
            float x1 = px + q.x1() * finalScale;
            float y1 = py + q.y1() * finalScale;

            float[] vertices = {
                x0, y0, q.s0(), q.t0(),
                x0, y1, q.s0(), q.t1(),
                x1, y1, q.s1(), q.t1(),

                x0, y0, q.s0(), q.t0(),
                x1, y1, q.s1(), q.t1(),
                x1, y0, q.s1(), q.t0()
            };

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, vertices);

            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
            
            currentX = xBuf.get(0);
        }

        q.free();
        GL30.glBindVertexArray(0);
    }

    public float numberWidth(int digits, float scale) {
        return digits * 0.05f * scale;
    }

    private void ortho(float left, float right, float bottom, float top, float zNear, float zFar, float[] dest) {
        dest[0] = 2.0f / (right - left);
        dest[5] = 2.0f / (top - bottom);
        dest[10] = -2.0f / (zFar - zNear);
        dest[12] = -(right + left) / (right - left);
        dest[13] = -(top + bottom) / (top - bottom);
        dest[14] = -(zFar + zNear) / (zFar - zNear);
        dest[15] = 1.0f;
    }
}
