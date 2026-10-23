"""Genera las texturas del mod (pixel art procedural) con Pillow.

Uso:  python tools/gen_textures.py
Escribe en src/main/resources/assets/quarryindustrial/textures/. Es determinista (semilla fija),
así que correrlo de nuevo da los mismos archivos; para retocar a mano, editar los PNG y no volver a correrlo.
"""
import random
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/quarryindustrial/textures"
rng = random.Random(26_3)

# Colores de tier de Ala Industrial (EnergyTier.color)
TIER_COLORS = {"lv": (0xFF, 0xD2, 0x4A), "mv": (0xFF, 0x8A, 0x3D), "hv": (0xFF, 0x3D, 0x5A)}


def clamp(v):
    return max(0, min(255, int(v)))


def shade(color, amount):
    return tuple(clamp(c + amount) for c in color)


def noisy_fill(img, box, base, spread, streak=False):
    """Rellena un rectángulo con ruido; con streak el ruido corre en franjas verticales (metal cepillado)."""
    x0, y0, x1, y1 = box
    column = {x: rng.randint(-spread, spread) for x in range(x0, x1)}
    for y in range(y0, y1):
        for x in range(x0, x1):
            n = rng.randint(-spread, spread)
            if streak:
                n = n // 2 + column[x]
            img.putpixel((x, y), shade(base, n) + (255,))


def rig_texture():
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    yellow = (0xE6, 0xAE, 0x12)
    noisy_fill(img, (0, 0, 128, 32), yellow, 10)
    # Desgaste: manchas de óxido/hollín y remaches cada 8 px
    for _ in range(60):
        x, y = rng.randrange(128), rng.randrange(32)
        img.putpixel((x, y), shade((0x8A, 0x5A, 0x10), rng.randint(-15, 15)) + (255,))
    for y in range(3, 32, 8):
        for x in range(3, 128, 8):
            img.putpixel((x, y), (0x6B, 0x55, 0x1A, 255))
            img.putpixel((x + 1, y), (0xF6, 0xD0, 0x50, 255))

    noisy_fill(img, (0, 32, 128, 64), (0x9C, 0xA4, 0xAD), 12, streak=True)   # acero de la tubería
    noisy_fill(img, (0, 64, 128, 80), (0x3E, 0x43, 0x49), 8)                  # acero oscuro
    noisy_fill(img, (0, 80, 128, 96), (0xCF, 0xD3, 0xD8), 14)                 # carburo de tungsteno
    noisy_fill(img, (0, 96, 128, 112), (0x86, 0x80, 0x78), 12)                # conos (acero forjado)
    for y in range(96, 112, 2):                                               # surcos del cono
        for x in range(0, 128):
            if (x + y) % 3 == 0:
                img.putpixel((x, y), (0x5C, 0x57, 0x51, 255))
    for y in range(112, 128):                                                 # baliza naranja
        for x in range(128):
            t = (y - 112) / 16
            img.putpixel((x, y), (255, clamp(140 - 80 * t), clamp(40 - 30 * t), 255))
    return img


def frame_texture():
    """Viga del marco: franjas de peligro amarillo/negro en diagonal con borde oscuro."""
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            stripe = ((x + y) // 4) % 2 == 0
            base = (0xE6, 0xAE, 0x12) if stripe else (0x22, 0x22, 0x24)
            img.putpixel((x, y), shade(base, rng.randint(-8, 8)) + (255,))
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, 15, 15), outline=(0x30, 0x2C, 0x22, 255))
    for p in [(2, 2), (13, 2), (2, 13), (13, 13)]:
        img.putpixel(p, (0x9A, 0x9A, 0x9A, 255))
    return img


def casing(tier_color, kind):
    """Carcasa de máquina 16×16: panel gris oscuro biselado, remaches y detalle según la cara."""
    img = Image.new("RGBA", (16, 16))
    noisy_fill(img, (0, 0, 16, 16), (0x4A, 0x4F, 0x55), 6)
    d = ImageDraw.Draw(img)
    d.line((0, 0, 15, 0), fill=(0x6E, 0x74, 0x7B, 255))
    d.line((0, 0, 0, 15), fill=(0x6E, 0x74, 0x7B, 255))
    d.line((0, 15, 15, 15), fill=(0x26, 0x29, 0x2D, 255))
    d.line((15, 0, 15, 15), fill=(0x26, 0x29, 0x2D, 255))
    for p in [(1, 1), (14, 1), (1, 14), (14, 14)]:
        img.putpixel(p, (0x9C, 0xA4, 0xAD, 255))
    if kind == "front":
        # Pantalla con la "lectura" de profundidad y una barra del color del tier
        d.rectangle((3, 3, 12, 8), fill=(0x0C, 0x1A, 0x12, 255), outline=(0x1E, 0x22, 0x26, 255))
        for i, h in enumerate([2, 3, 4, 3, 4, 2, 3]):
            d.line((4 + i, 7, 4 + i, 7 - h + 1), fill=(0x3C, 0xE8, 0x7A, 255))
        d.rectangle((3, 10, 12, 11), fill=tier_color + (255,))
        img.putpixel((4, 13), (0xFF, 0x40, 0x30, 255))
        img.putpixel((6, 13), (0x40, 0xFF, 0x60, 255))
        d.rectangle((9, 13, 12, 13), fill=(0x2A, 0x2D, 0x31, 255))
    elif kind == "top":
        # Rejilla de ventilación del motor
        for y in range(3, 13, 2):
            d.line((3, y, 12, y), fill=(0x1A, 0x1C, 0x1F, 255))
        d.rectangle((2, 2, 13, 13), outline=tier_color + (255,))
    elif kind == "plain":
        pass
    else:
        # Banda de peligro abajo y franja del tier arriba
        for x in range(1, 15):
            for y in range(11, 14):
                stripe = ((x + y) // 2) % 2 == 0
                img.putpixel((x, y), (0xE6, 0xAE, 0x12, 255) if stripe else (0x22, 0x22, 0x24, 255))
        d.rectangle((1, 3, 14, 4), fill=tier_color + (255,))
        d.rectangle((5, 6, 10, 9), fill=(0x35, 0x39, 0x3E, 255), outline=(0x26, 0x29, 0x2D, 255))
    return img


ICONS = {
    # 8×8 sobre la placa, '#' = color del ícono, '+' = brillo
    "area": ["#..##..#", "##....##", "........", "#......#", "#......#", "........", "##....##", "#..##..#"],
    "speed": ["#...#...", ".#...#..", "..#...#.", "...#...#", "..#...#.", ".#...#..", "#...#...", "........"],
    "fortune": ["...##...", "..#++#..", ".#++###.", "#+#####.", ".######.", "..####..", "...##...", "........"],
    "silk_touch": ["......##", ".....#+#", "....#+#.", "...#+#..", "..#+#...", ".#+#....", "##.#....", "#......."],
    "void": ["..####..", ".#....#.", "#..##..#", "#.#++#.#", "#.#++#.#", "#..##..#", ".#....#.", "..####.."],
}
ICON_COLORS = {"area": (0x7C, 0xD8, 0xFF), "speed": (0xFF, 0xE0, 0x40), "fortune": (0x3A, 0x6E, 0xFF),
               "silk_touch": (0xF0, 0xF0, 0xF0), "void": (0xB0, 0x50, 0xFF)}


def upgrade_icon(name):
    """Placa de circuito verde con conectores dorados abajo y el ícono del upgrade en el centro."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle((1, 1, 14, 13), fill=(0x1F, 0x6B, 0x3A, 255), outline=(0x10, 0x3A, 0x20, 255))
    for x in range(3, 14, 2):
        d.line((x, 13, x, 14), fill=(0xE8, 0xC0, 0x40, 255))
    for _ in range(10):
        x, y = rng.randrange(2, 14), rng.randrange(2, 12)
        img.putpixel((x, y), (0x2E, 0x8A, 0x4E, 255))
    color = ICON_COLORS[name]
    for row, line in enumerate(ICONS[name]):
        for col, ch in enumerate(line):
            if ch == "#":
                img.putpixel((4 + col, 3 + row), color + (255,))
            elif ch == "+":
                img.putpixel((4 + col, 3 + row), shade(color, 70) + (255,))
    return img


def pumpjack_texture():
    """Cigüeña, 128×256 en franjas: pintura bordó, amarillo, acero oscuro, acero, negro y verde del motor."""
    img = Image.new("RGBA", (128, 256), (0, 0, 0, 0))
    noisy_fill(img, (0, 0, 128, 96), (0x7E, 0x2A, 0x22), 9)            # pintura bordó (balancín, poste)
    for _ in range(140):                                                  # pintura saltada / óxido
        x, y = rng.randrange(128), rng.randrange(96)
        img.putpixel((x, y), shade((0x5A, 0x3A, 0x2A), rng.randint(-10, 10)) + (255,))
    noisy_fill(img, (0, 96, 128, 128), (0xE6, 0xAE, 0x12), 10)          # amarillo (cabeza de caballo)
    noisy_fill(img, (0, 128, 128, 160), (0x3E, 0x43, 0x49), 8)          # acero oscuro
    noisy_fill(img, (0, 160, 128, 192), (0xB4, 0xBA, 0xC2), 10, streak=True)  # acero pulido
    noisy_fill(img, (0, 192, 128, 224), (0x1E, 0x1F, 0x22), 6)          # contrapesos
    for y in range(192, 224):                                             # franjas amarillas de los contrapesos
        for x in range(128):
            if (x // 3 + y // 3) % 6 == 0:
                img.putpixel((x, y), (0xE6, 0xAE, 0x12, 255))
    noisy_fill(img, (0, 224, 128, 256), (0x2F, 0x5E, 0x3A), 8)          # motor verde
    return img


def pumpjack_icon():
    """Silueta de cigüeña para el item: patín, poste, balancín con cabeza de caballo y contrapeso."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    red, yellow, dark, black = (0x9A, 0x34, 0x28, 255), (0xE6, 0xAE, 0x12, 255), (0x3E, 0x43, 0x49, 255), (0x1E, 0x1F, 0x22, 255)
    d.line((1, 14, 14, 14), fill=dark)
    d.line((6, 13, 8, 5), fill=red)
    d.line((10, 13, 8, 5), fill=red)
    d.line((2, 4, 13, 6), fill=red)
    d.rectangle((1, 2, 2, 6), fill=yellow)
    d.line((2, 7, 2, 13), fill=(0xB4, 0xBA, 0xC2, 255))
    d.rectangle((11, 9, 14, 12), fill=black)
    d.line((13, 6, 12, 9), fill=red)
    d.point((1, 13), fill=dark)
    d.point((3, 13), fill=dark)
    return img


HV_RED = (0xFF, 0x3D, 0x5A)


def nuclear_casing():
    """Carcasa del multibloque: placa de acero oscuro con bisel, remaches y esquinas de peligro."""
    img = casing((0x30, 0x34, 0x38), "plain")
    d = ImageDraw.Draw(img)
    for (x, y) in [(1, 1), (12, 1), (1, 12), (12, 12)]:
        for i in range(3):
            for j in range(3):
                stripe = (i + j) % 2 == 0
                img.putpixel((x + i, y + j), (0xE6, 0xAE, 0x12, 255) if stripe else (0x22, 0x22, 0x24, 255))
    d.rectangle((5, 5, 10, 10), outline=(0x26, 0x29, 0x2D, 255))
    return img


def energy_port():
    """Puerto HV: la carcasa con un enchufe rojo (color HV de Ala) y un rayo."""
    img = nuclear_casing()
    d = ImageDraw.Draw(img)
    d.ellipse((3, 3, 12, 12), fill=(0x1A, 0x1C, 0x1F, 255), outline=HV_RED + (255,))
    d.ellipse((4, 4, 11, 11), outline=shade(HV_RED, -60) + (255,))
    bolt = [(8, 4), (6, 8), (8, 8), (7, 11), (10, 7), (8, 7), (9, 4)]
    d.polygon(bolt, fill=(0xFF, 0xE0, 0x40, 255))
    return img


def core_face(kind):
    """Core de la quarry nuclear: pantalla con el trébol de radiación al frente; rejilla arriba."""
    if kind == "front":
        img = nuclear_casing()
        d = ImageDraw.Draw(img)
        d.rectangle((3, 3, 12, 12), fill=(0x0C, 0x1A, 0x12, 255), outline=(0x1E, 0x22, 0x26, 255))
        glow = (0x3C, 0xE8, 0x7A, 255)
        d.ellipse((6, 6, 9, 9), fill=glow)
        for (x0, y0, x1, y1) in [(5, 3, 10, 5), (3, 9, 6, 12), (9, 9, 12, 12)]:
            d.rectangle((x0 + 1, y0 + 1, x1 - 1, y1 - 1), fill=glow)
        return img
    if kind == "top":
        img = casing(HV_RED, "top")
        return img
    return casing((0x3C, 0xE8, 0x7A), "side")


def save(img, rel):
    path = ROOT / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)
    print("escrito", path.relative_to(ROOT.parent.parent.parent.parent.parent))


def main():
    save(rig_texture(), "entity/quarry_rig.png")
    save(frame_texture(), "block/quarry_frame.png")
    for tier, color in TIER_COLORS.items():
        for kind in ("front", "side", "top"):
            save(casing(color, kind), f"block/quarry_{tier}_{kind}.png")
    for name in ICONS:
        save(upgrade_icon(name), f"item/upgrade_{name}.png")
    save(pumpjack_texture(), "entity/pumpjack.png")
    save(nuclear_casing(), "block/quarry_casing.png")
    save(energy_port(), "block/energy_port.png")
    for kind in ("front", "side", "top"):
        save(core_face(kind), f"block/quarry_core_{kind}.png")
    save(pumpjack_icon(), "item/pumpjack.png")


if __name__ == "__main__":
    main()
