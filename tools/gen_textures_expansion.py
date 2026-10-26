"""Texturas de la expansión (brocas, generadores, drones, plataforma offshore...).

Uso:  python tools/gen_textures_expansion.py
Va aparte de gen_textures.py para no cambiar las texturas que ya existen (las dos usan semillas fijas).
"""
import math
import random

from PIL import Image, ImageDraw

import gen_textures as base
from gen_textures import noisy_fill, save, shade

base.rng = random.Random(1_1)
rng = base.rng

# Colores del acero de cada broca y de sus insertos
BIT_COLORS = {
    "iron": ((0x8E, 0x94, 0x9C), (0xD8, 0xDC, 0xE2)),
    "diamond": ((0x6A, 0x70, 0x78), (0x6F, 0xF0, 0xF0)),
    "netherite": ((0x3A, 0x34, 0x38), (0x8A, 0x78, 0x84)),
    "pdc": ((0x9C, 0xA4, 0xAD), (0xE8, 0xF4, 0xFF)),
}


def px(img, x, y, color):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), tuple(color) + (255,) if len(color) == 3 else color)


def bit_icon(name):
    """Broca vista de costado: pin roscado arriba, cuerpo, y abajo tres conos (o aletas PDC) con dientes."""
    steel, teeth = BIT_COLORS[name]
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Pin roscado
    for y in range(1, 5):
        d.line((6, y, 9, y), fill=shade(steel, 25 if y % 2 else -10) + (255,))
    d.rectangle((4, 5, 11, 6), fill=shade(steel, 10) + (255,))           # hombro
    d.rectangle((3, 7, 12, 9), fill=steel + (255,))                       # cuerpo
    d.line((3, 7, 12, 7), fill=shade(steel, 35) + (255,))
    if name == "pdc":
        for x in (3, 6, 9, 12):                                           # aletas
            d.rectangle((x, 10, x + 1, 13), fill=shade(steel, -30) + (255,))
            px(img, x, 14, teeth)
            px(img, x + 1, 14, teeth)
            px(img, x, 11, teeth)
        return img
    for cx in (4, 8, 12):                                                 # tres conos
        d.polygon([(cx - 2, 10), (cx + 1, 10), (cx, 14), (cx - 1, 14)], fill=shade(steel, -25) + (255,))
        px(img, cx - 2, 11, teeth)
        px(img, cx + 1, 11, teeth)
        px(img, cx - 1, 13, teeth)
        px(img, cx, 14, teeth)
    return img


def ore_seeker_icon():
    """Placa de circuito (como los otros upgrades) con una lupa sobre una veta de mineral."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle((1, 1, 14, 13), fill=(0x1F, 0x6B, 0x3A, 255), outline=(0x10, 0x3A, 0x20, 255))
    for x in range(3, 14, 2):
        d.line((x, 13, x, 14), fill=(0xE8, 0xC0, 0x40, 255))
    for p in [(4, 9), (5, 10), (6, 9), (5, 8)]:
        px(img, *p, (0x5A, 0xE0, 0xF0))
    d.ellipse((6, 3, 11, 8), outline=(0xF0, 0xF0, 0xF0, 255))
    d.line((6, 8, 4, 11), fill=(0xC0, 0x90, 0x40, 255))
    px(img, 8, 5, (0xFF, 0xD0, 0x40))
    px(img, 9, 6, (0xFF, 0xD0, 0x40))
    return img


ENGINE_GREEN = (0x2F, 0x5E, 0x3A)
MV_ORANGE = (0xFF, 0x8A, 0x3D)


def save_animated(frames, rel, frametime):
    """Tira vertical de cuadros + .mcmeta, para las texturas animadas de los bloques encendidos."""
    strip = Image.new("RGBA", (16, 16 * len(frames)))
    for i, frame in enumerate(frames):
        strip.paste(frame, (0, 16 * i))
    save(strip, rel)
    path = base.ROOT / (rel + ".mcmeta")
    path.write_text('{\n  "animation": {\n    "frametime": %d,\n    "interpolate": false\n  }\n}\n' % frametime,
                    encoding="utf-8")


def diesel_face(kind, frame=None):
    """Generador diésel: carcasa verde de motor. Frente con el volante de inercia (gira al estar encendido)."""
    img = base.casing(MV_ORANGE, "plain")
    noisy_fill(img, (1, 1, 15, 15), ENGINE_GREEN, 6)
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, 15, 15), outline=(0x1E, 0x3A, 0x26, 255))
    if kind == "front":
        d.ellipse((3, 3, 12, 12), fill=(0x22, 0x25, 0x28, 255), outline=(0x9C, 0xA4, 0xAD, 255))
        angle = 0 if frame is None else frame
        # Rayo del volante: 4 posiciones de giro
        spokes = [((7, 4), (8, 11)), ((4, 7), (11, 8)), ((5, 5), (10, 10)), ((10, 5), (5, 10))]
        for a, b in (spokes[angle % 4], spokes[(angle + 2) % 4]):
            d.line((a, b), fill=(0xC8, 0xCE, 0xD4, 255))
        d.rectangle((7, 7, 8, 8), fill=(0xE6, 0xAE, 0x12, 255))
        glow = (0xFF, 0x9A, 0x30, 255) if frame is not None else (0x55, 0x30, 0x20, 255)
        d.rectangle((2, 13, 4, 13), fill=glow)
        d.rectangle((11, 13, 13, 13), fill=MV_ORANGE + (255,))
    elif kind == "top":
        d.ellipse((9, 2, 13, 6), fill=(0x1A, 0x1A, 0x1C, 255), outline=(0x5A, 0x5E, 0x64, 255))
        for y in range(9, 14, 2):
            d.line((2, y, 13, y), fill=(0x1A, 0x2E, 0x20, 255))
    else:
        for y in range(3, 12, 2):
            d.line((3, y, 12, y), fill=(0x1A, 0x2E, 0x20, 255))
        d.rectangle((1, 13, 14, 14), fill=MV_ORANGE + (255,))
    return img


def turbine_face(kind, frame=None):
    """Turbina de vapor: acero pulido. Frente con el rotor de álabes, que gira al estar encendida."""
    img = Image.new("RGBA", (16, 16))
    noisy_fill(img, (0, 0, 16, 16), (0x9C, 0xA4, 0xAD), 8, streak=True)
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, 15, 15), outline=(0x4A, 0x4F, 0x55, 255))
    if kind == "front":
        d.ellipse((2, 2, 13, 13), fill=(0x2A, 0x2D, 0x31, 255), outline=(0x5A, 0x5E, 0x64, 255))
        offset = 0 if frame is None else frame * math.pi / 16
        for i in range(8):
            a = offset + i * math.pi / 4
            x = 7.5 + math.cos(a) * 4.5
            y = 7.5 + math.sin(a) * 4.5
            d.line((7.5, 7.5, x, y), fill=(0xD8, 0xDC, 0xE2, 255))
        d.ellipse((6, 6, 9, 9), fill=(0xE6, 0xAE, 0x12, 255))
    elif kind == "top":
        d.ellipse((4, 4, 11, 11), fill=(0x6A, 0x70, 0x78, 255), outline=(0x3E, 0x43, 0x49, 255))
        d.ellipse((6, 6, 9, 9), fill=(0x1A, 0x1C, 0x1F, 255))
    else:
        d.rectangle((1, 6, 14, 9), fill=(0xB8, 0x73, 0x33, 255))       # caño de cobre
        d.line((1, 6, 14, 6), fill=(0xD8, 0x93, 0x53, 255))
        for x in (3, 12):
            d.rectangle((x, 5, x + 1, 10), fill=(0x6A, 0x70, 0x78, 255))
    return img


SAFETY_YELLOW = (0xE6, 0xAE, 0x12)


def offshore_deck():
    """Rejilla de acero galvanizado: barras cada 3 px con huecos transparentes (se ve el mar debajo)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            frame = x in (0, 15) or y in (0, 15)
            bar = x % 3 == 0 or y % 4 == 0
            if frame:
                img.putpixel((x, y), shade(SAFETY_YELLOW, rng.randint(-10, 10)) + (255,))
            elif bar:
                img.putpixel((x, y), shade((0x8E, 0x96, 0x9E), rng.randint(-12, 12)) + (255,))
    return img


def offshore_rig_top():
    """Mesa rotativa en el moonpool: plato gris con el buje del kelly y la rejilla alrededor."""
    img = offshore_deck()
    d = ImageDraw.Draw(img)
    d.ellipse((1, 1, 14, 14), fill=(0x3E, 0x43, 0x49, 255), outline=(0x9C, 0xA4, 0xAD, 255))
    d.ellipse((4, 4, 11, 11), fill=(0x5A, 0x5E, 0x64, 255))
    d.rectangle((6, 6, 9, 9), fill=(0x1A, 0x1C, 0x1F, 255))
    for p in [(2, 7), (13, 8), (7, 2), (8, 13)]:
        px(img, *p, SAFETY_YELLOW)
    return img


def offshore_leg():
    """Pata tubular amarilla con franjas de marea (verdín abajo) y soldaduras."""
    img = Image.new("RGBA", (16, 16))
    noisy_fill(img, (0, 0, 16, 16), SAFETY_YELLOW, 10, streak=True)
    d = ImageDraw.Draw(img)
    d.line((0, 5, 15, 5), fill=(0x8A, 0x6A, 0x10, 255))
    for x in range(0, 16, 2):
        px(img, x, 12 + rng.randint(0, 3), (0x3A, 0x6A, 0x4A))
    return img


def offshore_icon():
    """Ícono del item: torre en celosía sobre la cubierta, patas en el agua y la llama de la antorcha."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    water = (0x2A, 0x5B, 0xD8, 255)
    d.rectangle((0, 13, 15, 15), fill=water)
    d.line((0, 13, 15, 13), fill=(0x6F, 0xB8, 0xFF, 255))
    for x in (2, 12):
        d.line((x, 10, x, 15), fill=SAFETY_YELLOW + (255,))
    d.rectangle((1, 9, 13, 10), fill=(0x8E, 0x96, 0x9E, 255))
    d.line((5, 8, 7, 1), fill=SAFETY_YELLOW + (255,))
    d.line((10, 8, 8, 1), fill=SAFETY_YELLOW + (255,))
    d.line((5, 6, 10, 6), fill=SAFETY_YELLOW + (255,))
    d.line((6, 3, 9, 3), fill=SAFETY_YELLOW + (255,))
    d.line((6, 6, 9, 3), fill=(0x9C, 0xA4, 0xAD, 255))
    d.line((7, 0, 8, 0), fill=(0x3E, 0x43, 0x49, 255))
    d.line((13, 9, 15, 5), fill=SAFETY_YELLOW + (255,))
    px(img, 15, 4, (0xFF, 0x9A, 0x30))
    px(img, 15, 3, (0xFF, 0xE0, 0x40))
    px(img, 14, 3, (0xFF, 0x6A, 0x20))
    return img


DRONE_COLORS = {"cargo": (0xE6, 0xAE, 0x12), "tanker": (0x2A, 0x5B, 0xD8), "power": (0xFF, 0x3D, 0x5A),
                "maintenance": (0x3C, 0xE8, 0x7A), "survey": (0xB0, 0x50, 0xFF)}


# Dron en 3/4, letra por pixel ('.' = transparente). La carga se dibuja encima, debajo del cuerpo.
DRONE_FRAME = [
    "................",
    ".rRBr......rBRr.",
    "rRRRRr....rRRRRr",
    ".rrMr......rMrr.",
    "...m.oooooo.m...",
    "...aoCWCCCkoa...",
    "...aoCCCCkkoa...",
    "....ohhhhllo....",
    "..aaodddGgdoaa..",
    ".La..oddddo..aN.",
    "rrMRr.oooo.rRMrr",
    "RRBRRr....rRRBRR",
    ".rrrr......rrrr.",
    "................",
    "................",
    "................",
]

DRONE_PAYLOAD = {
    "cargo": [None]*11 + [
        "......w..w......",
        ".....xxxxxx.....",
        ".....xYCYYx.....",
        ".....xyCyyx.....",
        ".....xxxxxx.....",
    ],
    "tanker": [None]*11 + [
        "......w..w......",
        ".....xTTTTx.....",
        "....xTCCCCTx....",
        "....xttttttx....",
        ".....xxxxxx.....",
    ],
    "power": [None]*11 + [
        ".......QQ.......",
        ".....xxxxxx.....",
        ".....xCCZZx.....",
        ".....xZZCCx.....",
        ".....xxxxxx.....",
    ],
    "maintenance": [None]*11 + [
        ".....wwwwww.....",
        ".....C.ss.C.....",
        ".....CsssSC.....",
        "......CssC......",
        ".......S........",
    ],
    "survey": [None]*11 + [
        "......xwwx......",
        ".....xCCCCx.....",
        ".....xgGVgx.....",
        ".....xggggx.....",
        "......xxxx......",
    ],
}


def drone_icon(kind):
    """Cuadricóptero en 3/4: hélices girando, carcasa del color del tipo, lente, luces de navegación y la
    carga que lleva colgando (caja, tanque, batería, garra con broca o cámara)."""
    color = DRONE_COLORS[kind]
    pal = {
        "o": (0x16, 0x18, 0x1C, 255),
        "d": (0x23, 0x27, 0x2E, 255),
        "b": (0x34, 0x39, 0x41, 255),
        "l": (0x4C, 0x53, 0x5D, 255),
        "h": (0x76, 0x7F, 0x8A, 255),
        "a": (0x8A, 0x92, 0x9C, 255),
        "m": (0x3E, 0x43, 0x49, 255),
        "M": (0xC8, 0xD0, 0xD8, 255),
        "r": (0xB4, 0xC4, 0xD4, 90),
        "R": (0xE4, 0xEE, 0xF8, 150),
        "B": (0x50, 0x58, 0x62, 210),
        "W": shade(color, 80) + (255,),
        "C": color + (255,),
        "k": shade(color, -70) + (255,),
        "G": (0x8F, 0xE8, 0xFF, 255),
        "g": (0x0C, 0x1C, 0x2C, 255),
        "w": (0x5A, 0x60, 0x68, 255),
        "x": (0x1E, 0x1F, 0x22, 255),
        "y": (0x7A, 0x52, 0x2A, 255),
        "Y": (0xB8, 0x84, 0x48, 255),
        "L": (0xFF, 0x40, 0x40, 255),
        "N": (0x50, 0xFF, 0x70, 255),
        "t": (0x4A, 0x52, 0x5C, 255),
        "T": (0xB8, 0xC2, 0xCC, 255),
        "Q": (0xC8, 0xD0, 0xD8, 255),
        "Z": (0xFF, 0xF0, 0x60, 255),
        "s": (0x9C, 0xA4, 0xAD, 255),
        "S": (0xE8, 0xF4, 0xFF, 255),
        "V": color + (255,),
    }
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for layer in (DRONE_FRAME, DRONE_PAYLOAD[kind]):
        for y, row in enumerate(layer):
            if row is None:
                continue
            assert len(row) == 16, row
            for x, ch in enumerate(row):
                if ch != ".":
                    img.putpixel((x, y), pal[ch])
    # Luces: punta delantera del color del tipo, con un brillo
    return img


def drone_pad_top():
    """Helipuerto: placa gris con borde de peligro y una H amarilla."""
    img = Image.new("RGBA", (16, 16))
    noisy_fill(img, (0, 0, 16, 16), (0x4A, 0x4F, 0x55), 5)
    for x in range(16):
        for y in range(16):
            if x in (0, 15) or y in (0, 15):
                img.putpixel((x, y), SAFETY_YELLOW + (255,) if (x + y) % 4 < 2 else (0x22, 0x22, 0x24, 255))
    d = ImageDraw.Draw(img)
    d.rectangle((4, 4, 5, 11), fill=SAFETY_YELLOW + (255,))
    d.rectangle((10, 4, 11, 11), fill=SAFETY_YELLOW + (255,))
    d.rectangle((6, 7, 9, 8), fill=SAFETY_YELLOW + (255,))
    for p in ((2, 2), (13, 2), (2, 13), (13, 13)):
        px(img, *p, (0x3C, 0xE8, 0x7A))
    return img


def drone_pad_side():
    img = Image.new("RGBA", (16, 16))
    noisy_fill(img, (0, 0, 16, 16), (0x3E, 0x43, 0x49), 5)
    d = ImageDraw.Draw(img)
    d.line((0, 12, 15, 12), fill=SAFETY_YELLOW + (255,))
    for x in range(1, 16, 4):
        px(img, x, 14, (0x3C, 0xE8, 0x7A))
    return img


CONSOLE_STEEL = (0x3E, 0x43, 0x49)
RADAR_GREEN = (0x4A, 0xCF, 0x8A)


def console_face(kind, frame=0):
    """Centralita de drones. Frente: pantalla de radar (el barrido gira en 8 cuadros) con blips de pads, un dron y
    un géiser, y abajo el teclado con sus luces. Costados con rejilla y franja de peligro; arriba, la antena."""
    img = Image.new("RGBA", (16, 16))
    noisy_fill(img, (0, 0, 16, 16), CONSOLE_STEEL, 5, streak=True)
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, 15, 15), outline=(0x22, 0x24, 0x28, 255))
    if kind == "front":
        d.rectangle((2, 1, 13, 10), fill=(0x16, 0x18, 0x1C, 255))
        d.rectangle((3, 2, 12, 9), fill=(0x07, 0x13, 0x21, 255))
        for gx in range(3, 13, 3):
            for gy in range(2, 10, 3):
                px(img, gx, gy, (0x17, 0x3A, 0x5E))
        cx, cy = 7.5, 5.5
        for i in range(24):
            a = i * math.pi / 12
            x, y = round(cx + math.cos(a) * 3.6), round(cy + math.sin(a) * 3.6)
            if 3 <= x <= 12 and 2 <= y <= 9:
                px(img, x, y, (0x1F, 0x5A, 0x40))
        # Barrido con estela: el cuadro actual brillante, los dos anteriores más apagados
        for trail, color in ((2, (0x1C, 0x50, 0x38)), (1, (0x2C, 0x84, 0x5A)), (0, RADAR_GREEN)):
            a = (frame - trail) * math.pi / 4
            for r in (1, 2, 3, 4):
                x, y = round(cx + math.cos(a) * r), round(cy + math.sin(a) * r)
                if 3 <= x <= 12 and 2 <= y <= 9:
                    px(img, x, y, color)
        px(img, 5, 4, (0xFF, 0xD2, 0x4A))      # pad
        px(img, 10, 7, (0xB0, 0x50, 0xFF))     # géiser
        px(img, 10, 3, (0x6F, 0xD0, 0xFF))     # dron
        px(img, 7, 5, (0xE6, 0xF0, 0xFF))      # la centralita
        # Teclado y luces
        d.rectangle((2, 12, 13, 13), fill=(0x22, 0x25, 0x29, 255))
        for x in range(3, 13, 2):
            px(img, x, 12, (0x8A, 0x92, 0x9C))
        px(img, 3, 14, RADAR_GREEN)
        px(img, 5, 14, (0xFF, 0xD2, 0x4A))
        px(img, 12, 14, (0xFF, 0x3D, 0x5A) if frame % 2 else (0x60, 0x20, 0x28))
    elif kind == "top":
        d.ellipse((4, 4, 11, 11), fill=(0x2A, 0x2D, 0x31, 255), outline=(0x9C, 0xA4, 0xAD, 255))
        d.ellipse((6, 6, 9, 9), fill=(0x5A, 0x60, 0x68, 255))
        px(img, 7, 7, (0x6F, 0xD0, 0xFF))
        for x in (1, 14):
            d.line((x, 2, x, 13), fill=(0x22, 0x25, 0x29, 255))
    else:
        for y in range(3, 10, 2):
            d.line((3, y, 12, y), fill=(0x1E, 0x20, 0x24, 255))
        for x in range(1, 15):
            px(img, x, 13, SAFETY_YELLOW if x % 4 < 2 else (0x22, 0x22, 0x24))
        px(img, 2, 2, RADAR_GREEN)
    return img


def linker_icon():
    """Vinculador: un control remoto con antena y dos luces."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle((4, 5, 11, 14), fill=(0x3E, 0x43, 0x49, 255), outline=(0x1E, 0x1F, 0x22, 255))
    d.rectangle((5, 6, 10, 9), fill=(0x0C, 0x1A, 0x12, 255))
    px(img, 6, 7, (0x3C, 0xE8, 0x7A))
    px(img, 8, 8, (0x3C, 0xE8, 0x7A))
    d.line((10, 5, 13, 1), fill=(0x9C, 0xA4, 0xAD, 255))
    px(img, 13, 1, (0xFF, 0x3D, 0x5A))
    px(img, 6, 12, SAFETY_YELLOW)
    px(img, 9, 12, (0xFF, 0x3D, 0x5A))
    return img


def survey_report_icon():
    """Informe: un mapa con una gota de petróleo marcada."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle((2, 2, 13, 13), fill=(0xE8, 0xDC, 0xB8, 255), outline=(0x8A, 0x70, 0x48, 255))
    for y in (5, 8, 11):
        d.line((3, y, 12, y), fill=(0xC8, 0xB8, 0x90, 255))
    d.line((4, 10, 7, 6), fill=(0x6A, 0x9A, 0x4A, 255))
    d.ellipse((8, 4, 11, 8), fill=(0x1E, 0x1F, 0x22, 255))
    px(img, 9, 3, (0x1E, 0x1F, 0x22))
    px(img, 10, 5, (0x6A, 0x70, 0x78))
    return img


def manual_icon():
    """Manual del operador: libro de tapa azul plano con un taladro amarillo en la tapa."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle((3, 1, 13, 14), fill=(0x1F, 0x4A, 0x75, 255), outline=(0x0F, 0x2A, 0x47, 255))
    d.rectangle((3, 1, 4, 14), fill=(0x0F, 0x2A, 0x47, 255))
    d.line((13, 2, 13, 14), fill=(0xF0, 0xF0, 0xE8, 255))
    d.line((8, 3, 8, 8), fill=SAFETY_YELLOW + (255,))
    d.polygon([(6, 8), (10, 8), (8, 12)], fill=SAFETY_YELLOW + (255,))
    d.line((6, 3, 10, 3), fill=(0x9C, 0xA4, 0xAD, 255))
    return img


def main():
    for name in BIT_COLORS:
        save(bit_icon(name), f"item/drill_bit_{name}.png")
    save(ore_seeker_icon(), "item/upgrade_ore_seeker.png")
    for kind in ("front", "side", "top"):
        save(diesel_face(kind), f"block/diesel_generator_{kind}.png")
        save(turbine_face(kind), f"block/steam_turbine_{kind}.png")
    save_animated([diesel_face("front", f) for f in range(4)], "block/diesel_generator_front_on.png", 1)
    save_animated([turbine_face("front", f) for f in range(4)], "block/steam_turbine_front_on.png", 1)
    save(offshore_deck(), "block/offshore_deck.png")
    save(offshore_rig_top(), "block/offshore_rig_top.png")
    save(offshore_leg(), "block/offshore_leg.png")
    save(offshore_icon(), "item/offshore_rig.png")
    for kind in DRONE_COLORS:
        save(drone_icon(kind), f"item/{kind}_drone.png")
    save(drone_pad_top(), "block/drone_pad_top.png")
    save(drone_pad_side(), "block/drone_pad_side.png")
    save(linker_icon(), "item/drone_linker.png")
    save_animated([console_face("front", f) for f in range(8)], "block/drone_console_front.png", 3)
    save(console_face("side"), "block/drone_console_side.png")
    save(console_face("top"), "block/drone_console_top.png")
    save(survey_report_icon(), "item/survey_report.png")
    save(manual_icon(), "item/manual.png")


if __name__ == "__main__":
    main()
