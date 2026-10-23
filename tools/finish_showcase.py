"""Termina las capturas del showcase para el README y CurseForge.

Uso:  python tools/finish_showcase.py   (después de QUARRY_SHOWCASE=1 / QUARRY_EXPANSION=1 ./gradlew runClientGameTest)
- Pasa las capturas de 1920×1080 a JPG (calidad 90): pesan ~10 veces menos que el PNG y no se nota.
- Recorta las pantallas (la GUI de la quarry y el manual) y las amplía ×2 sin suavizado (quedan en PNG: son
  pixel art). La captura de Jade pasa entera a JPG.
"""
from pathlib import Path

from PIL import Image

SHOTS = Path(__file__).resolve().parent.parent / "docs/screenshots"

# captura cruda: (color que marca la caja a recortar, nombre final)
SCREENS = {
    "09_gui_raw.png": ((0xC6, 0xC6, 0xC6), "09_gui.png"),        # panel gris de la GUI
    "19_manual_raw.png": ((0x06, 0x11, 0x1F), "19_manual.png"),  # marco del papel del manual
}


def crop_screen(raw_name, color, out_name):
    raw_path = SHOTS / raw_name
    if not raw_path.exists():
        return
    raw = Image.open(raw_path).convert("RGB")
    w, h = raw.size
    xs, ys = [], []
    for y in range(0, h):
        for x in range(0, w):
            if raw.getpixel((x, y)) == color:
                xs.append(x)
                ys.append(y)
    if not xs:
        print("no se encontró la pantalla en", raw_name)
        return
    margin = 12
    box = (max(0, min(xs) - margin), max(0, min(ys) - margin), min(w, max(xs) + margin), min(h, max(ys) + margin))
    screen = raw.crop(box)
    screen.resize((screen.width * 2, screen.height * 2), Image.NEAREST).save(SHOTS / out_name)
    raw_path.unlink()
    print("recortada:", SHOTS / out_name)


def to_jpeg():
    for png in sorted(SHOTS.glob("[0-2][0-9]_*.png")):
        if png.name in ("09_gui.png", "19_manual.png") or png.name in SCREENS:
            continue
        name = png.stem.removesuffix("_raw")
        jpg = png.with_name(name + ".jpg")
        Image.open(png).convert("RGB").save(jpg, quality=90, optimize=True, progressive=True)
        png.unlink()
        print(f"{jpg.name}: {jpg.stat().st_size // 1024} KB")


if __name__ == "__main__":
    for raw_name, (color, out_name) in SCREENS.items():
        crop_screen(raw_name, color, out_name)
    to_jpeg()
