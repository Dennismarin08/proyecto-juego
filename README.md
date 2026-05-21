# Fútbol VS Zombies

## Descripción
Juego de defensa táctica con jugadores de fútbol contra oleadas de zombies. El proyecto incluye:
- Landing page profesional en `index.html`
- Juego principal en `futbol_vs_zombies_v2.html`
- Lobby con selección de modalidad y botón de continuar
- Backend Java que guarda el progreso en SQL

## Cómo ejecutar
1. Asegúrate de tener instalado:
   - Java 17 o superior
   - Maven
   - XAMPP o servidor local Apache para servir `http://localhost/JUEGO`

2. Copia la carpeta `JUEGO` dentro de la carpeta de tu servidor local, por ejemplo `htdocs`.

3. Inicia el backend Java:
   - Abre terminal en `c:\xampp\htdocs\JUEGO\backend`
   - Ejecuta:
     ```bash
     mvn compile exec:java
     ```

4. Abre el juego en el navegador:
   - `http://localhost/JUEGO/index.html`

5. Para jugar directamente:
   - Haz clic en "JUGAR AHORA"
   - El backend debe estar corriendo para que el guardado funcione correctamente.

## Guardado de progreso
El juego guarda automáticamente el avance en:
- nivel
- oleada
- score
- energía

El guardado se realiza en un backend Java usando H2 SQL. Si el profesor quiere revisar, puede abrir `backend/ProgressServer.java`.
