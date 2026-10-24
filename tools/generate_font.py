#!/usr/bin/env python3
"""Regenera assets/sociolandia/font/image.json del resource pack.

Uso:  python3 tools/generate_font.py
Despues borra plugins/Controller/pack.contentsha (o el zip) en el servidor para que el plugin
reconstruya el pack con el nuevo font, y vuelve a subir el zip si lo alojas fuera.

La imagen se dibuja como un "caracter" dentro del TITULO de Minecraft (que se escala x4).
  height = alto del dibujo (en unidades del titulo; en pantalla = height * 4 px de GUI)
  ascent = cuanto sube sobre la linea base (debe ser <= height)
Para que la imagen quede centrada en una altura fija se usa  ascent = height/2 + CENTER.
Si la ves muy arriba/abajo o muy grande/chica, ajusta FINAL_H y CENTER y regenera.
"""
import json, os

FINAL_H = 44          # tamano final de la imagen (44 -> ~176 px de GUI)
CENTER = 22           # mayor = la imagen sube. Con 22 y FINAL_H=44 la base queda justo encima del subtitulo
SLAM = [84, 72, 62, 54, 48, FINAL_H, 46]   # entrada "golpe": grande -> tamano final; el ultimo es el "respiro"
SHIFT = 6             # desplazamiento horizontal para el temblor (unidades; x4 px)
OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "pack",
                   "assets", "sociolandia", "font", "image.json")

def asc(h):
    return min(h, int(round(h / 2 + CENTER)))

def bitmap(tex, char, h, ascent=None):
    return {"type": "bitmap", "file": f"sociolandia:font/{tex}.png",
            "ascent": asc(h) if ascent is None else ascent, "height": h, "chars": [char]}

providers = []
for i, h in enumerate(SLAM):
    providers.append(bitmap("logo", chr(0xE000 + i), h))
for i, h in enumerate(SLAM):
    providers.append(bitmap("calavera", chr(0xE010 + i), h))
# Calavera de tamano final en tres alturas distintas (para rebotar al reirse)
providers.append(bitmap("calavera", chr(0xE020), FINAL_H, asc(FINAL_H) - 4))
providers.append(bitmap("calavera", chr(0xE021), FINAL_H, asc(FINAL_H) - 8))
providers.append(bitmap("calavera", chr(0xE022), FINAL_H, asc(FINAL_H) - 2))
# Espacios para mover la imagen a los lados
providers.append({"type": "space", "advances": {chr(0xE100): SHIFT, chr(0xE101): -SHIFT}})

os.makedirs(os.path.dirname(OUT), exist_ok=True)
with open(OUT, "w") as f:
    json.dump({"providers": providers}, f, indent=2)
print("escrito", os.path.normpath(OUT), len(providers), "proveedores")
