"""Ícono del mod: pixel art de 32×32 escalado sin suavizado.

Uso:  python tools/gen_icon.py
- src/main/resources/assets/quarryindustrial/icon.png  (128×128, el que muestra Mod Menu / Fabric)
- docs/curseforge/icon.png                             (512×512, avatar del proyecto en CurseForge)
Escena: el pórtico amarillo con franjas de peligro, el carro, la sarta de perforación y la broca tricónica
mordiendo capas de tierra y piedra con vetas de mineral.
"""
import random
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
rng = random.Random(7)

YELLOW = (0xF2, 0xB8, 0x16)
YELLOW_DARK = (0xB0, 0x80, 0x0C)
BLACK = (0x1E, 0x1E, 0x20)
STEEL = (0xB4, 0xBA, 0xC2)
STEEL_DARK = (0x6E, 0x74, 0x7B)
CARBIDE = (0xF0, 0xF2, 0xF4)
CONE = (0x86, 0x80, 0x78)
DIRT = (0x7A, 0x55, 0x36)
GRASS = (0x5B, 0x9E, 0x3A)
STONE = (0x7E, 0x7E, 0x80)
DEEPSLATE = (0x4A, 0x4A, 0x52)


def shade(c, d):
    return tuple(max(0, min(255, v + d)) for v in c)


def build():
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    # Fondo: cielo de atardecer en degradé, con esquinas redondeadas (las recorta la máscara al final)
    for y in range(32):
        t = y / 31
        sky = (int(0x2C + 0x60 * t), int(0x3E + 0x30 * t), int(0x70 - 0x10 * t))
        d.line((0, y, 31, y), fill=sky + (255,))

    # Terreno: pasto, tierra, piedra y pizarra profunda, con un pozo excavado al centro
    for y in range(19, 32):
        for x in range(32):
            if y == 19:
                c = GRASS
            elif y < 22:
                c = DIRT
            elif y < 27:
                c = STONE
            else:
                c = DEEPSLATE
            img.putpixel((x, y), shade(c, rng.randint(-10, 10)) + (255,))
    for y in range(19, 30):
        for x in range(10, 22):
            img.putpixel((x, y), shade((0x2A, 0x24, 0x20), rng.randint(-6, 6)) + (255,))
    # Vetas de mineral en las paredes del pozo: diamante, oro y redstone
    for (x, y, c) in [(8, 24, (0x5A, 0xE6, 0xE0)), (24, 23, (0xF6, 0xD0, 0x40)), (7, 28, (0xE0, 0x30, 0x30)),
                      (25, 28, (0x5A, 0xE6, 0xE0)), (23, 26, (0xE0, 0x30, 0x30))]:
        img.putpixel((x, y), c + (255,))
        img.putpixel((x + 1, y), shade(c, -40) + (255,))

    # Patas del pórtico y viga superior con franjas de peligro
    for x0 in (4, 26):
        for y in range(6, 20):
            for x in range(x0, x0 + 2):
                stripe = ((x + y) // 2) % 2 == 0
                img.putpixel((x, y), (YELLOW if stripe else BLACK) + (255,))
    for y in range(5, 8):
        for x in range(3, 29):
            stripe = ((x + y) // 2) % 2 == 0
            img.putpixel((x, y), (YELLOW if stripe else BLACK) + (255,))
    d.line((3, 8, 28, 8), fill=YELLOW_DARK + (255,))

    # Carro con motor y baliza
    d.rectangle((12, 3, 19, 9), fill=YELLOW + (255,))
    d.rectangle((12, 9, 19, 9), fill=YELLOW_DARK + (255,))
    d.rectangle((14, 1, 17, 3), fill=(0x3E, 0x43, 0x49, 255))
    img.putpixel((15, 0), (0xFF, 0x7A, 0x20, 255))
    img.putpixel((16, 0), (0xFF, 0x7A, 0x20, 255))

    # Sarta de perforación con tool joints
    for y in range(10, 24):
        img.putpixel((15, y), STEEL + (255,))
        img.putpixel((16, y), STEEL_DARK + (255,))
    for y in (13, 17, 21):
        d.line((14, y, 17, y), fill=CARBIDE + (255,))

    # Broca tricónica: cuerpo, tres conos con insertos de carburo
    d.rectangle((13, 24, 18, 25), fill=(0x3E, 0x43, 0x49, 255))
    for (x, y) in [(12, 26), (15, 27), (18, 26)]:
        d.rectangle((x, y, x + 1, y + 1), fill=CONE + (255,))
        img.putpixel((x, y + 2), CARBIDE + (255,))
        img.putpixel((x + 1, y + 2), CARBIDE + (255,))
    # Chispas y polvo donde muerde la broca
    for (x, y) in [(11, 29), (20, 29), (13, 30), (18, 30)]:
        img.putpixel((x, y), (0xFF, 0xD2, 0x4A, 255))
    return img


def rounded(img, size):
    big = img.resize((size, size), Image.NEAREST)
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, size - 1, size - 1), radius=size // 8, fill=255)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.paste(big, (0, 0), mask)
    return out


def main():
    icon = build()
    targets = {
        ROOT / "src/main/resources/assets/quarryindustrial/icon.png": 128,
        ROOT / "docs/curseforge/icon.png": 512,
    }
    for path, size in targets.items():
        path.parent.mkdir(parents=True, exist_ok=True)
        rounded(icon, size).save(path)
        print("escrito", path.relative_to(ROOT))


if __name__ == "__main__":
    main()
