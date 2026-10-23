"""Termina las capturas del showcase para el README y CurseForge.

Uso:  python tools/finish_showcase.py   (después de QUARRY_SHOWCASE=1 ./gradlew runClientGameTest)
- Pasa las capturas de 1920×1080 a JPG (calidad 90): pesan ~10 veces menos que el PNG y no se nota.
- Recorta la GUI de 09_gui_raw.png y la amplía ×2 sin suavizado (queda en PNG: es pixel art).
"""
from pathlib import Path

from PIL import Image

SHOTS = Path(__file__).resolve().parent.parent / "docs/screenshots"


def crop_gui():
    raw_path = SHOTS / "09_gui_raw.png"
    if not raw_path.exists():
        return
    raw = Image.open(raw_path).convert("RGB")
    w, h = raw.size
    # El panel de la GUI es gris claro (#C6C6C6): su caja es lo que se recorta
    xs, ys = [], []
    for y in range(h):
        for x in range(w):
            if raw.getpixel((x, y)) == (0xC6, 0xC6, 0xC6):
                xs.append(x)
                ys.append(y)
    margin = 12
    box = (max(0, min(xs) - margin), max(0, min(ys) - margin), min(w, max(xs) + margin), min(h, max(ys) + margin))
    gui = raw.crop(box)
    gui.resize((gui.width * 2, gui.height * 2), Image.NEAREST).save(SHOTS / "09_gui.png")
    raw_path.unlink()
    print("GUI recortada:", SHOTS / "09_gui.png")


def to_jpeg():
    for png in sorted(SHOTS.glob("0[1-8]_*.png")):
        jpg = png.with_suffix(".jpg")
        Image.open(png).convert("RGB").save(jpg, quality=90, optimize=True, progressive=True)
        png.unlink()
        print(f"{jpg.name}: {jpg.stat().st_size // 1024} KB")


if __name__ == "__main__":
    crop_gui()
    to_jpeg()
