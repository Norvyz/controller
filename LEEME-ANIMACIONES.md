# Controller 1.3.0 - SocioLandia

## 1. Compilar
`./mvnw package` (o `mvnw.cmd package`) -> `target/Controller.jar` -> carpeta `plugins/`. Requiere Spigot/Paper 1.21.x.
**Recomendado: Paper.** En Paper los titulos con imagen se envian limpio; en Spigot puro funcionan pero la consola
imprime "Showing new title..." por cada cuadro de la animacion.

## 2. IMPORTANTE: el resource pack (la imagen en pantalla)
La imagen se dibuja dentro del titulo de Minecraft como un caracter de una fuente propia, y eso solo se puede
con un resource pack. Al iniciar, el plugin genera `plugins/Controller/SocioLandia-pack.zip` y calcula su SHA-1.
Debes ponerlo en internet (elige una opcion en `config.yml`, seccion `resource-pack`):
- **A)** Sube el .zip a un hosting (GitHub Releases, tu hosting, etc.) y pega el enlace DIRECTO en `url`.
- **B)** `host.enabled: true`, `host.address: <IP o dominio>` y abre el puerto TCP `host.port` (8123): el servidor lo sirve solo.

Cada jugador lo recibe al entrar. Quien lo rechace, o si no hay URL, ve la version solo texto (nunca se rompe).
El enlace debe ser directo al .zip (no una pagina de descarga). Si cambias algo del pack, reinicia el servidor.

## 3. Que hace
**Bienvenida** (`/mensaje preview`): el logo entra de golpe en el centro de la pantalla, respira, y debajo se escribe
la primera linea de tu mensaje con degradado dorado y destello; la segunda linea aparece en la barra de accion.
**Muerte** (`/mensaje preview muerte`): mensaje burlon en el chat para todos; al reaparecer, la calavera cae de golpe,
tiembla y rebota con "JAJAJAJA" debajo, una burla en la barra de accion y 5 carcajadas (risa de bruja de Minecraft;
una risa propia requiere meter el audio en el pack y poner su nombre en `death.laugh-sound`).
**PvP** (`/pvp on 30m`, `/pvp off`, `/pvp preview`): titulo con >> << que se juntan, espadas 3D que chocan,
barra de accion animada con cuenta regresiva. Sin emojis ni simbolos raros en ningun lado.

## 4. Si la imagen sale muy grande, chica, arriba o abajo
Un titulo de Minecraft no puede ocupar literalmente toda la pantalla: la imagen mide unos 176 px de GUI de alto
(aprox. la mitad de la pantalla). Para cambiarlo edita `FINAL_H` y `CENTER` en `tools/generate_font.py`, ejecuta
`python3 tools/generate_font.py`, recompila y reinicia (el plugin reconstruye el pack solo).
Para otro logo/calavera: reemplaza los PNG cuadrados (512x512, fondo transparente) en
`src/main/resources/pack/assets/sociolandia/textures/font/`.

## 5. Problema "preview muerte" al entrar
Pasaba porque con la version anterior del plugin `/mensaje preview muerte` guardaba ese texto como mensaje de bienvenida.
Ya no pasa (las palabras preview/reload/clear nunca se guardan como mensaje), pero el texto malo sigue guardado en TU
servidor: ejecuta `/mensaje &6&lBIENVENIDO\nDisfruta tu estancia` o edita `join-message.text` en `plugins/Controller/config.yml`.

## Otros
- `/mensaje reload` recarga la config. `/mensaje clear` desactiva el mensaje de bienvenida.
- Cada animacion se apaga con `enabled: false` en su seccion.
