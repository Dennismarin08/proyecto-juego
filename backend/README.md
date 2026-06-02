# Java Progress Backend

Este backend guarda y recupera el progreso de la partida usando SQL (H2) y Java.

## Requisitos
- Java 17 o superior
- Maven

## Ejecutar el servidor
1. Abre un terminal en `c:\xampp\htdocs\JUEGO\backend`
2. Ejecuta:

```bash
mvn compile exec:java
```

El servidor quedará escuchando en `http://localhost:8080/api/progress`.

## Endpoints
- `GET /api/progress?userId=player-default`
- `POST /api/progress` con JSON:
  - `userId`
  - `level`
  - `subWave`
  - `score`
  - `energy`

## Notas
- El archivo de base de datos se crea automáticamente en `c:\xampp\htdocs\JUEGO\backend`.
- El juego en `futbol_vs_zombies_v2.html` carga el progreso al iniciar y muestra un botón de "Continuar partida" cuando existe un guardado.
