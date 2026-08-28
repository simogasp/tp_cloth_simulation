import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetFramebufferSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glBufferSubData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_LINK_STATUS;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glGetProgramInfoLog;
import static org.lwjgl.opengl.GL20.glGetProgrami;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20.glGetShaderi;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glLinkProgram;
import static org.lwjgl.opengl.GL20.glShaderSource;
import static org.lwjgl.opengl.GL20.glUniform2f;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

/** Displays a Node grid as a white OpenGL wireframe. */
public final class ClothRenderer implements AutoCloseable {

    private static final int INITIAL_WIDTH = 900;
    private static final int INITIAL_HEIGHT = 700;

    private static final String VERTEX_SHADER = """
            #version 330 core
            layout (location = 0) in vec3 aPosition;

            uniform vec2 uCenter;
            uniform vec2 uScale;

            void main() {
                vec2 screenPosition = (aPosition.xy - uCenter) * uScale;
                gl_Position = vec4(screenPosition, 0.0, 1.0);
            }
            """;

    private static final String FRAGMENT_SHADER = """
            #version 330 core
            out vec4 fragmentColor;

            void main() {
                fragmentColor = vec4(1.0, 1.0, 1.0, 1.0);
            }
            """;

    private final Node[][] cloth;
    private final int rowCount;
    private final int columnCount;
    private final int indexCount;
    private final FloatBuffer positions;
    private final GLFWErrorCallback errorCallback;

    private long window;
    private boolean glfwInitialized;
    private boolean openGlInitialized;
    private int vertexArray;
    private int vertexBuffer;
    private int indexBuffer;
    private int shaderProgram;
    private int centerUniform;
    private int scaleUniform;
    private int framebufferWidth = INITIAL_WIDTH;
    private int framebufferHeight = INITIAL_HEIGHT;

    private final float viewCenterX;
    private final float viewCenterY;
    private final float viewWidth;
    private final float viewHeight;

    public ClothRenderer(Node[][] cloth, float zoomOutFactor) {
        validateCloth(cloth);
        this.cloth = cloth;
        this.rowCount = cloth.length;
        this.columnCount = cloth[0].length;
        this.indexCount = 2 * (rowCount * (columnCount - 1)
                + (rowCount - 1) * columnCount);
        this.positions = BufferUtils.createFloatBuffer(rowCount * columnCount * 3);

        float minX = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        for (Node[] row : cloth) {
            for (Node node : row) {
                minX = Math.min(minX, (float) node.getX());
                maxX = Math.max(maxX, (float) node.getX());
                minY = Math.min(minY, (float) node.getY());
                maxY = Math.max(maxY, (float) node.getY());
            }
        }
        this.viewCenterX = (minX + maxX) * 0.5f;
        this.viewCenterY = (minY + maxY) * 0.5f;
        this.viewWidth = Math.max(maxX - minX, 1.0f) * 1.2f * zoomOutFactor;
        this.viewHeight = Math.max(maxY - minY, 1.0f) * 1.2f * zoomOutFactor;

        errorCallback = GLFWErrorCallback.createPrint(System.err).set();
        try {
            initializeWindow();
            initializeOpenGlObjects();
        } catch (RuntimeException exception) {
            close();
            throw exception;
        }
    }

    private static void validateCloth(Node[][] cloth) {
        if (cloth == null || cloth.length == 0 || cloth[0] == null
                || cloth[0].length == 0) {
            throw new IllegalArgumentException("The cloth must contain at least one node");
        }

        int expectedColumns = cloth[0].length;
        for (Node[] row : cloth) {
            if (row == null || row.length != expectedColumns) {
                throw new IllegalArgumentException("The cloth must be a rectangular array");
            }
            for (Node node : row) {
                if (node == null) {
                    throw new IllegalArgumentException("The cloth cannot contain null nodes");
                }
            }
        }
    }

    private void initializeWindow() {
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }
        glfwInitialized = true;

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, 0);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        window = glfwCreateWindow(INITIAL_WIDTH, INITIAL_HEIGHT,
                "Mass-spring cloth simulation", NULL, NULL);
        if (window == NULL) {
            throw new IllegalStateException("Unable to create the GLFW window");
        }

        /* OpenGL must be initialized before any callback can call glViewport.
         * In particular, Wayland may fire the framebuffer callback as soon as
         * the window is shown. */
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        GL.createCapabilities();
        openGlInitialized = true;
        glViewport(0, 0, framebufferWidth, framebufferHeight);

        glfwSetKeyCallback(window, (handle, key, scancode, action, modifiers) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) {
                glfwSetWindowShouldClose(handle, true);
            }
        });
        glfwSetFramebufferSizeCallback(window, (handle, width, height) -> {
            framebufferWidth = Math.max(width, 1);
            framebufferHeight = Math.max(height, 1);
            glViewport(0, 0, width, height);
        });

        glfwShowWindow(window);
    }

    private void initializeOpenGlObjects() {
        shaderProgram = createShaderProgram();
        centerUniform = glGetUniformLocation(shaderProgram, "uCenter");
        scaleUniform = glGetUniformLocation(shaderProgram, "uScale");

        vertexArray = glGenVertexArrays();
        vertexBuffer = glGenBuffers();
        indexBuffer = glGenBuffers();

        glBindVertexArray(vertexArray);

        glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer);
        glBufferData(GL_ARRAY_BUFFER, (long) positions.capacity() * Float.BYTES,
                GL_DYNAMIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Float.BYTES, 0L);
        glEnableVertexAttribArray(0);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBuffer);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, createLineIndices(), GL_STATIC_DRAW);

        glBindVertexArray(0);
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
    }

    private IntBuffer createLineIndices() {
        IntBuffer indices = BufferUtils.createIntBuffer(indexCount);
        for (int row = 0; row < rowCount; row++) {
            for (int column = 0; column < columnCount - 1; column++) {
                int current = row * columnCount + column;
                indices.put(current).put(current + 1);
            }
        }
        for (int row = 0; row < rowCount - 1; row++) {
            for (int column = 0; column < columnCount; column++) {
                int current = row * columnCount + column;
                indices.put(current).put(current + columnCount);
            }
        }
        return indices.flip();
    }

    /** Uploads current CPU node positions and draws one frame. */
    public void render() {
        positions.clear();
        for (Node[] row : cloth) {
            for (Node node : row) {
                positions.put((float) node.getX());
                positions.put((float) node.getY());
                positions.put((float) node.getZ());
            }
        }
        positions.flip();

        glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer);
        glBufferSubData(GL_ARRAY_BUFFER, 0L, positions);

        float aspectRatio = (float) framebufferWidth / framebufferHeight;
        float visibleHeight = Math.max(viewHeight, viewWidth / aspectRatio);
        float visibleWidth = visibleHeight * aspectRatio;

        glClear(GL_COLOR_BUFFER_BIT);
        glUseProgram(shaderProgram);
        glUniform2f(centerUniform, viewCenterX, viewCenterY);
        glUniform2f(scaleUniform, 2.0f / visibleWidth, 2.0f / visibleHeight);
        glBindVertexArray(vertexArray);
        glDrawElements(GL_LINES, indexCount, GL_UNSIGNED_INT, 0L);

        glfwSwapBuffers(window);
        glfwPollEvents();
    }

    public boolean shouldClose() {
        return glfwWindowShouldClose(window);
    }

    private static int createShaderProgram() {
        int vertexShader = compileShader(GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragmentShader = compileShader(GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
        int program = glCreateProgram();
        glAttachShader(program, vertexShader);
        glAttachShader(program, fragmentShader);
        glLinkProgram(program);

        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);

        if (glGetProgrami(program, GL_LINK_STATUS) == 0) {
            String log = glGetProgramInfoLog(program);
            glDeleteProgram(program);
            throw new IllegalStateException("Could not link shaders:\n" + log);
        }
        return program;
    }

    private static int compileShader(int type, String source) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == 0) {
            String log = glGetShaderInfoLog(shader);
            glDeleteShader(shader);
            throw new IllegalStateException("Could not compile shader:\n" + log);
        }
        return shader;
    }

    @Override
    public void close() {
        if (window != NULL) {
            if (openGlInitialized) {
                glDeleteProgram(shaderProgram);
                glDeleteBuffers(vertexBuffer);
                glDeleteBuffers(indexBuffer);
                glDeleteVertexArrays(vertexArray);
            }
            glfwFreeCallbacks(window);
            glfwDestroyWindow(window);
            window = NULL;
        }
        if (glfwInitialized) {
            glfwTerminate();
            glfwInitialized = false;
        }
        glfwSetErrorCallback(null);
        errorCallback.free();
    }
}
