# Ruleta Hardcore (Fabric 1.21.1)

- Item `ruleta` (sin crafteo): `/give @a ruleta`. Click derecho en **creativo** abre el menú (color, título, texto).
- Comandos (operador): `/ruleta rojo`, `azul`, `amarillo`, `morado`, `rosa`, `naranja`, `verde`, `celeste` (opcional: texto para el chat).
- Solo el rojo dispara la animación de la cara roja (filtro rojo, rayos, risa y canción). Los demás colores solo muestran la ruleta y el mensaje en el chat.
- Cuando muere un jugador sale la misma animación a todos (sin filtro ni música) con su nombre y la causa.
- Requiere Fabric Loader 0.16+ y Fabric API para 1.21.1, Java 21. Va en cliente y servidor.

## Sacar el .jar con GitHub
1. Sube TODO el contenido de esta carpeta a tu repositorio (Add file > Upload files, arrastrando las carpetas; incluye `.github`).
2. En la pestaña **Actions** se ejecuta `build`. Al terminar, entra a la ejecución y descarga `ruleta-hardcore-jar` (adentro está el .jar).
