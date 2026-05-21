# Flappy Bird — OpenGL 2D (Java + LWJGL)

![Java](https://img.shields.io/badge/Java-17-orange.svg)
![LWJGL](https://img.shields.io/badge/LWJGL-3.3.3-black.svg)
![OpenGL](https://img.shields.io/badge/OpenGL-3.3%20Core-blue.svg)
![License](https://img.shields.io/badge/License-MIT-green.svg)

[⬇️ Descargar archivo .jar (Ejecutable)](bin/Flappy%20Bird.jar)

![Flappy Bird Gameplay](assets/demo.gif)

*Gameplay mostrando el modo de 2 jugadores simultáneos y el escalado de dificultad.*

## Descripción

Juego 2D estilo Flappy Bird construido con Java 17 y LWJGL 3.3.3 sobre OpenGL 3.3 core profile. El proyecto emplea un quad base reutilizable con uniforms de transformación para renderizar todos los elementos en pantalla, prescindiendo del uso de motores gráficos externos o funciones del pipeline fijo. El renderizado y el manejo de transformaciones de los elementos se realiza mediante operaciones matriciales directamente en CPU para ser inyectadas en los shaders.

## Características

- Pájaro compuesto por figuras geométricas (cuerpo, pico, cola, ojo con pupila, ala animada oscilando con `sin(t)`).
- Inclinación del pájaro proporcional a la velocidad vertical para mayor dinamismo.
- Modo un jugador, dos jugadores y tres jugadores simultáneos en la misma ventana, con controles independientes.
- Dificultad progresiva: la velocidad de las tuberías y la frecuencia de aparición aumentan por nivel (Nivel 0: base, Nivel 1: medio, Nivel 2: avanzado, Nivel 3: máximo/techo).
- Menú principal con selección interactiva de modo de juego.
- Configuración previa a la partida para activar/desactivar gravedad invertida y ajustar el umbral de puntos.
- Pantalla de Game Over con puntuaciones individuales y declaración clara del ganador o empate.
- HUD superior con el puntaje en tiempo real de cada jugador.
- Fondo completo con degradado armónico, suelo interactivo y elementos decorativos dibujados con primitivas puras de OpenGL.
- Tuberías renderizadas con capitel decorativo.
- Escalado dinámico de ventana responsivo mediante técnica de letterbox/pillarbox para asegurar que el aspect ratio se mantenga en todo momento.
- Detección precisa de colisiones mediante cajas delimitadoras AABB (Axis-Aligned Bounding Box).
- Audio inmersivo de salto, punto conseguido y estado de game over utilizando `javax.sound.sampled`.
- Sistema tipográfico robusto para renderizado de texto escalable y centrado automáticamente en menús.
- **Multiplataforma**: Soporta Windows y macOS (incluyendo Apple Silicon M1/M2/M3).

## Requisitos del sistema

- Java 17 o superior ([Descargar](https://adoptium.net/))
- Maven 3.8+ (solo necesario para compilar desde código fuente)
- GPU con soporte para OpenGL 3.3 (cualquier GPU de la última década)
- **Sistemas operativos soportados:**
  - Windows 10 / Windows 11 (x64)
  - macOS 11 (Big Sur) o superior (Intel y Apple Silicon)

## Instalación y ejecución

### Opción 1: Ejecutar el JAR precompilado (Recomendado)

1. Asegúrate de tener Java 17 instalado. Verifica con:

   ```bash
   java -version
   ```

2. Descarga el archivo `FlappyBird.jar` desde la carpeta `bin/` del repositorio.

3. Abre una terminal en la carpeta donde descargaste el archivo y ejecuta:

   **En Windows:**

   ```bash
   java -jar FlappyBird.jar
   ```

   **En macOS:**

   ```bash
   java -XstartOnFirstThread -jar FlappyBird.jar
   ```

### Opción 2: Compilar desde el código fuente

```bash
# Clonar el repositorio
git clone https://github.com/moises-cisneros/Flappy-Bird
cd Flappy-Bird

# Compilar el proyecto
mvn compile

# Ejecutar el juego
mvn exec:exec

# Generar JAR ejecutable (incluye todas las dependencias)
mvn clean package

# Ejecutar el JAR generado
# En Windows:
java -jar target/FlappyBird-1.0-SNAPSHOT.jar

# En macOS:
java -XstartOnFirstThread -jar target/FlappyBird-1.0-SNAPSHOT.jar
```

### Opción 3: Importar en IDE (IntelliJ IDEA / Eclipse)

1. Abre tu IDE y selecciona "Importar proyecto Maven".
2. Selecciona la carpeta del proyecto.
3. Espera a que se descarguen las dependencias.
4. Ejecuta la clase `org.moises.App`.

## Controles

| Jugador / Contexto | Acción | Tecla |
| --- | --- | --- |
| **Jugador 1** | Saltar | `SPACE` |
| **Jugador 2** | Saltar | `W` |
| **Jugador 3** | Saltar | `↑` (Flecha arriba) |
| **Menú Principal** | Navegar Arriba | `↑` |
| **Menú Principal** | Navegar Abajo | `↓` |
| **Menú Principal** | Modo 1 Jugador Directo | `1` |
| **Menú Principal** | Modo 2 Jugadores Directo | `2` |
| **Menú Principal** | Modo 3 Jugadores Directo | `3` |
| **Menú Principal** | Confirmar Selección | `ENTER` / `SPACE` |
| **Menú Principal** | Toggle Gravedad Invertida | Click en botón `ON/OFF` |
| **Menú Principal** | Ajustar Umbral Gravedad Invertida | Click en `-` / `+` |
| **Game Over** | Volver a Intentar (Retry) | `R` / `SPACE` |
| **Game Over** | Menú Principal | `M` / `ESC` |

## Estructura del proyecto

```text
src/main/java/org/moises/
├── App.java                    ← Punto de entrada que inicializa y arranca la aplicación
├── core/
│   ├── Game.java               ← Bucle principal, control del viewport y máquina de estados
│   ├── GameState.java          ← Enumerador para los estados MAIN_MENU, PLAYING y GAME_OVER
│   └── InputManager.java       ← Polling nativo de GLFW y detección de flancos para teclas
├── entity/
│   ├── Bird.java               ← Datos anatómicos, física y renderizado compuesto de cada pájaro
│   └── Pipe.java               ← Estructura de tuberías y hitboxes AABB
├── render/
│   ├── Renderer.java           ← Abstracción para drawRect, drawTriangle y transformaciones matriciales
│   ├── BackgroundRenderer.java ← Dibuja el fondo multicapa con primitivas y parallax
│   └── TextRenderer.java       ← Lógica de medición, escalado y dibujo geométrico de tipografía
├── ui/
│   ├── MainMenu.java           ← Dibujado y control de la pantalla de inicio
│   ├── GameOverMenu.java       ← Dibujado de resultados y botones de reintento
│   └── MenuAction.java         ← Enumerador para selección en menús (SELECT_1P, SELECT_2P, NONE)
└── audio/
    └── SoundManager.java       ← Sistema de Object Pooling y playback de pistas WAV
```

## Solución de problemas

### Error en macOS: "Failed to locate library: liblwjgl.dylib"

**Solución:** Asegúrate de usar el flag `-XstartOnFirstThread`:

```bash
java -XstartOnFirstThread -jar FlappyBird.jar
```

### Error: "mvn no se reconoce como comando"

**Solución:** Instala Maven o usa el wrapper incluido:

- Windows: Descargar Maven desde [apache.org](https://maven.apache.org/download.cgi)
- macOS: `brew install maven`
- O usa `./mvnw` (Maven Wrapper) si está incluido en el proyecto

### Error: "UnsupportedClassVersionError"

**Solución:** Actualiza tu versión de Java a Java 17 o superior:

```bash
# Verificar versión actual
java -version

# Descargar Java 17 desde:
# https://openjdk.org/projects/jdk/17/
```

### El juego no inicia o se cierra inmediatamente

**Posibles causas:**

- GPU no soporta OpenGL 3.3 (actualiza drivers)
- Falta el flag `-XstartOnFirstThread` en macOS
- El JAR está corrupto (re-descargar o recompilar)

## Créditos y Recursos

- **Desarrollo y Arquitectura:** Moises Cisneros - [GitHub](https://github.com/moises-cisneros) | [X (Twitter)](https://x.com/cisn3ronauta)
- **Motor Gráfico y Binding:** Construido sobre [LWJGL 3](https://www.lwjgl.org/) (Licencia BSD)
- **Tipografía:** [Press Start 2P](https://fonts.google.com/specimen/Press+Start+2P) por CodeMan38 (SIL Open Font License)
- **Audio:** Efectos de sonido generados vía [jsfxr](https://sfxr.me) y/o biblioteca Kenney (CC0)
- **Texturas:** Imagen de explosión del pack [Kenney Particle Pack](https://kenney.nl/assets/particle-pack) (CC0)

## Licencia

Este proyecto se distribuye bajo la licencia **MIT**. Eres libre de utilizar, modificar y distribuir este código para fines personales o educativos. Consulta el archivo `LICENSE` para más detalles.

---

## 📞 Contacto

¿Preguntas o sugerencias? Abre un [Issue](https://github.com/moises-cisneros/Flappy-Bird/issues) en GitHub o contacta al desarrollador.
