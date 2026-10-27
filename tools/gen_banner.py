"""Banner, encabezados de sección y marcos de capturas de la página de CurseForge. Pixel art sin suavizado.

Uso:  python tools/gen_banner.py [ruta al minecraft-clientOnly.jar]
Salida (todo en docs/curseforge/):
- banner.png          960×240 (320×80 escalado ×3)
- headers/<clave>.png 800×64  (200×16 escalado ×4): una barra por sección de la descripción
- divider.png         800×16
- shots/<nombre>      las capturas de docs/screenshots/ con marco (JPG las fotos, PNG las interfaces)

Colorimetría 60/30/10: 60 % neutro (grafito azulado), 30 % principal (azul acero, el del mapa de la centralita),
10 % acento (ámbar de peligro, el de las franjas del pórtico). El texto sale de la tipografía de Minecraft, que se
lee del jar del cliente (no se versiona).
"""
import io
import random
import sys
import zipfile
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "docs" / "curseforge"
JAR = ROOT / ".gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-clientOnly-1b0498e74c/26.3/minecraft-clientOnly-1b0498e74c-26.3.jar"

# 60 % neutro
N0 = (0x0B, 0x0E, 0x14)
N1 = (0x11, 0x16, 0x1F)
N2 = (0x18, 0x1F, 0x2B)
N3 = (0x22, 0x2B, 0x3A)
N4 = (0x30, 0x3B, 0x4D)
# 30 % principal
B0 = (0x12, 0x35, 0x5C)
B1 = (0x1C, 0x52, 0x88)
B2 = (0x2C, 0x78, 0xC0)
B3 = (0x58, 0xA0, 0xE0)
B4 = (0x9C, 0xCB, 0xF2)
# 10 % acento
A0 = (0xA8, 0x74, 0x08)
A1 = (0xF2, 0xB8, 0x16)
A2 = (0xFF, 0xDE, 0x7A)
LIGHT = (0xE6, 0xEE, 0xF7)


# ── Tipografía ──

def load_font(jar):
    atlas = Image.open(io.BytesIO(zipfile.ZipFile(jar).read("assets/minecraft/textures/font/ascii.png"))).convert("RGBA")
    glyphs = {}
    for code in range(32, 127):
        cx, cy = (code % 16) * 8, (code // 16) * 8
        cell = atlas.crop((cx, cy, cx + 8, cy + 8))
        xs = [x for x in range(8) for y in range(8) if cell.getpixel((x, y))[3] > 0]
        width = (max(xs) + 1) if xs else 3
        glyphs[chr(code)] = (cell, width)
    return glyphs


def text_width(glyphs, text, scale=1):
    return sum(glyphs[c][1] + 1 for c in text) * scale - scale


def draw_text(img, glyphs, x, y, text, color, scale=1, shadow=None):
    """Dibuja el texto con la tipografía de Minecraft; con sombra opcional un píxel abajo y a la derecha."""
    def stamp(ox, oy, col):
        cursor = x + ox
        for c in text:
            cell, width = glyphs[c]
            for gy in range(8):
                for gx in range(width):
                    if cell.getpixel((gx, gy))[3] > 0:
                        for sy in range(scale):
                            for sx in range(scale):
                                px, py = cursor + gx * scale + sx, y + oy + gy * scale + sy
                                if 0 <= px < img.width and 0 <= py < img.height:
                                    img.putpixel((px, py), col)
            cursor += (width + 1) * scale

    if shadow:
        stamp(scale, scale, shadow)
    stamp(0, 0, color)


def upscale(img, factor):
    return img.resize((img.width * factor, img.height * factor), Image.NEAREST)


# ── Banner ──

def banner(glyphs):
    w, h = 320, 80
    img = Image.new("RGB", (w, h), N1)
    d = ImageDraw.Draw(img)
    rng = random.Random(26)

    # Cielo: degradé en bandas con tramado, más oscuro arriba
    for y in range(h):
        t = y / (h - 1)
        base = N0 if t < 0.28 else N1 if t < 0.62 else N2
        d.line([(0, y), (w, y)], fill=base)
        if 0.24 < t < 0.32 or 0.58 < t < 0.66:
            nxt = N1 if t < 0.5 else N2
            for x in range(y % 2, w, 2):
                img.putpixel((x, y), nxt)
    for _ in range(46):
        x, y = rng.randrange(w), rng.randrange(0, 40)
        img.putpixel((x, y), B1 if rng.random() < 0.7 else B3)

    # Estratos: piedra grafito con vetas de mineral azul (y una sola ámbar)
    ground = 62
    for y in range(ground, h):
        depth = (y - ground) / (h - ground)
        col = N3 if depth < 0.3 else N2 if depth < 0.65 else N1
        d.line([(0, y), (w, y)], fill=col)
    for x in range(w):
        top = ground + (1 if (x // 3) % 5 == 0 else 0) + (1 if (x // 7) % 3 == 0 else 0)
        d.line([(x, top - 1), (x, top)], fill=N4)
    for _ in range(70):
        x, y = rng.randrange(w), rng.randrange(ground + 3, h)
        img.putpixel((x, y), N4 if rng.random() < 0.5 else N0)
    for x, y in [(34, 70), (35, 70), (35, 71), (96, 66), (97, 66), (150, 73), (151, 73), (151, 72), (212, 69),
                 (213, 69), (262, 74), (263, 74), (288, 67), (289, 67), (289, 68), (60, 75), (61, 75)]:
        img.putpixel((x, y), B2)
    for x, y in [(36, 70), (98, 66), (152, 72), (264, 74)]:
        img.putpixel((x, y), B4)
    d.rectangle([(124, 71), (125, 72)], fill=A1)

    # Derrick de la derecha: torre de celosía azul con antorcha de gas (acento) y un dron al lado
    cx, base_y, top_y = 291, ground, 12
    for y in range(top_y, base_y):
        half = 2 + (y - top_y) * 11 // (base_y - top_y)
        d.point((cx - half, y), fill=B2)
        d.point((cx + half, y), fill=B2)
        if (y - top_y) % 6 == 0:
            d.line([(cx - half, y), (cx + half, y)], fill=B1)
        step = (y - top_y) // 6
        diag = cx - half + ((y - top_y) % 6) * (2 * half) // 6
        d.point((diag if step % 2 == 0 else 2 * cx - diag, y), fill=B1)
    d.rectangle([(cx - 3, top_y - 3), (cx + 3, top_y)], fill=B3)
    d.rectangle([(cx - 1, top_y - 5), (cx + 1, top_y - 3)], fill=B4)
    # Antorcha
    d.line([(cx + 7, top_y + 6), (cx + 12, top_y + 6)], fill=B2)
    d.rectangle([(cx + 12, top_y - 1), (cx + 12, top_y + 6)], fill=B2)
    for (fx, fy, col) in [(12, -2, A1), (12, -3, A1), (11, -3, A2), (13, -4, A2), (12, -5, A1), (12, -6, A2),
                          (11, -4, A0), (13, -3, A0)]:
        img.putpixel((cx + fx, top_y + fy), col)
    # Dron: cuerpo azul, hélices y una luz de navegación ámbar
    dx, dy = 66, 35
    d.rectangle([(dx - 3, dy), (dx + 3, dy + 3)], fill=B3)
    d.rectangle([(dx - 2, dy + 1), (dx + 2, dy + 2)], fill=B4)
    d.line([(dx - 8, dy - 2), (dx - 3, dy)], fill=B2)
    d.line([(dx + 3, dy), (dx + 8, dy - 2)], fill=B2)
    d.line([(dx - 10, dy - 3), (dx - 6, dy - 3)], fill=B4)
    d.line([(dx + 6, dy - 3), (dx + 10, dy - 3)], fill=B4)
    img.putpixel((dx, dy + 4), A1)
    img.putpixel((dx, dy + 5), A0)
    d.rectangle([(dx - 1, dy + 6), (dx + 1, dy + 8)], fill=N4)

    # Pórtico de la quarry a la izquierda: dos torres, puente, carro y sarta con broca que entra en la roca
    gx0, gx1, gy = 12, 52, 26
    for gx in (gx0, gx1):
        d.rectangle([(gx - 2, gy), (gx + 2, ground)], fill=B1)
        d.line([(gx - 2, gy), (gx - 2, ground)], fill=B2)
        for y in range(gy + 4, ground, 6):
            d.line([(gx - 2, y), (gx + 2, y + 4)], fill=B0)
    d.rectangle([(gx0 - 4, gy - 3), (gx1 + 4, gy + 1)], fill=B2)
    d.line([(gx0 - 4, gy - 3), (gx1 + 4, gy - 3)], fill=B4)
    # Franjas de peligro sobre el puente (acento)
    for x in range(gx0 - 4, gx1 + 5):
        if (x // 3) % 2 == 0:
            d.line([(x, gy - 1), (x, gy + 1)], fill=A1)
    carriage = 32
    d.rectangle([(carriage - 5, gy + 2), (carriage + 5, gy + 8)], fill=B3)
    d.rectangle([(carriage - 4, gy + 3), (carriage + 4, gy + 4)], fill=B4)
    d.rectangle([(carriage - 1, gy + 8), (carriage + 1, ground + 8)], fill=N4)
    for y in range(gy + 10, ground + 8, 5):
        d.line([(carriage - 2, y), (carriage + 2, y)], fill=B2)
    # Broca tricónica
    d.rectangle([(carriage - 3, ground + 8), (carriage + 3, ground + 11)], fill=B3)
    d.rectangle([(carriage - 2, ground + 12), (carriage + 2, ground + 13)], fill=B2)
    for x, y in [(carriage - 5, ground + 11), (carriage + 5, ground + 11), (carriage - 4, ground + 10),
                 (carriage + 4, ground + 10)]:
        img.putpixel((x, y), A1)
    img.putpixel((carriage, ground + 14), A2)
    # Bloques saltando de la broca
    for x, y in [(carriage - 8, ground + 4), (carriage + 9, ground + 2), (carriage + 12, ground + 5)]:
        d.rectangle([(x, y), (x + 1, y + 1)], fill=N4)

    # Franja de peligro en el borde inferior
    for x in range(w):
        d.line([(x, h - 2), (x, h - 1)], fill=A1 if (x // 4) % 2 == 0 else N0)

    # Texto: título grande con sombra azul, subtítulo con puntos de acento
    title = "QUARRY INDUSTRIAL"
    tw = text_width(glyphs, title, 2)
    tx = (w - tw) // 2
    draw_text(img, glyphs, tx, 15, title, LIGHT, scale=2, shadow=B0)
    sub = ["QUARRIES", "OIL", "DRONES"]
    parts_w = sum(text_width(glyphs, s) for s in sub) + 2 * 11
    sx = (w - parts_w) // 2
    for i, s in enumerate(sub):
        draw_text(img, glyphs, sx, 37, s, B4, shadow=N0)
        sx += text_width(glyphs, s)
        if i < len(sub) - 1:
            d.rectangle([(sx + 4, 40), (sx + 6, 42)], fill=A1)
            sx += 11
    tag = "FOR ALA INDUSTRIAL  -  FABRIC 26.3"
    draw_text(img, glyphs, (w - text_width(glyphs, tag)) // 2, 50, tag, B3, shadow=N0)

    # Marco del banner
    d.rectangle([(0, 0), (w - 1, h - 1)], outline=B1)
    d.rectangle([(1, 1), (w - 2, h - 2)], outline=N0)
    return upscale(img, 3)


# ── Encabezados ──

ICONS = {
    # '.' vacío, 'b' azul claro, 'w' blanco, 'a' acento, 'd' azul oscuro
    "quarry": ["....ww...", "...wwww..", "..bbbbbb.", ".b..ww..b", "....bb...", "....bb...", "....bb...", "...aaaa..", "....aa..."],
    "nuclear": ["..bbbbb..", ".b..w..b.", "b..bwb..b", "b.wwawwb.", "b..bwb..b", ".b..w..b.", "..bbbbb..", ".........", "........."],
    "oil": ["....a....", "...aaa...", "..aaaaa..", ".aawaaaa.", ".aaaaaaa.", ".aaaaaaa.", "..aaaaa..", "...aaa...", "........."],
    "bit": ["..bbbbb..", ".bwwbwwb.", ".bbbbbbb.", "..bbwbb..", "..bbbbb..", "...bwb...", "...bbb...", "....a....", "....a...."],
    "offshore": ["....bb...", "...b..b..", "...bbbb..", "..b.bb.b.", "..bbbbbb.", ".........", "wwdwwdwwd", "dwwdwwdww", "ddddddddd"],
    "drone": ["b.......b", "bbb.....b", ".bbbwbbb.", "..bwwwb..", "..bbbbb..", "...bab...", "....a....", ".........", "........."],
    "bolt": ["....aaa..", "...aaa...", "..aaa....", ".aaaaaa..", "...aaa...", "..aaa....", "..aa.....", ".a.......", "........."],
    "ala": ["...bbb...", "..bwwwb..", ".bwdddwb.", ".bwdadwb.", ".bwdddwb.", "..bwwwb..", "...bbb...", "...b.b...", "..bb.bb.."],
    "manual": ["..bbbbbb.", ".bwwwwwwb", ".bwbbbwwb", ".bwwwwwwb", ".bwbbwwwb", ".bwwwwwwb", ".bwwwwwwb", ".bbbbbbbb", "..aaaaaa."],
    "upgrade": ["...aaa...", "..aaaaa..", ".aaaaaaa.", "...bbb...", "...bbb...", "...bbb...", "..bbbbb..", ".bbbbbbb.", "........."],
    "package": ["..bbbbbb.", ".bwwbwwwb", "bbbbbbbbb", "bwwwbwwwb", "bwwwbwwwb", "bwwwbwwwb", "bbbbbbbbb", ".........", "........."],
    "flag": ["bb.......", "bbwwwww..", "bbwwwwwww", "bbaaaaaaa", "bbaaaaaa.", "bb.......", "bb.......", "bb.......", "bbb......"],
    "console": ["bbbbbbbbb", "bdddddddb", "bd.dwd.db", "bdwdbdwdb", "bd.dwd.db", "bdddddddb", "bbbbbbbbb", "...bbb...", "..bbbbb.."],
    "new": ["...a.....", "..aaa..a.", ".aaaaa.aa", "..aaa..a.", "...a.....", ".....a...", "....aaa..", ".....a...", "........."],
}
ICON_COLORS = {"b": B4, "w": LIGHT, "a": A1, "d": B1}

HEADERS = {
    "quarries": ("quarry", "QUARRIES THAT SCALE UP"),
    "nuclear": ("nuclear", "THE NUCLEAR QUARRY"),
    "pumpjacks": ("oil", "OIL PUMPJACKS"),
    "bits": ("bit", "DRILL BITS THAT WEAR OUT"),
    "offshore": ("offshore", "OFFSHORE DRILLING RIG"),
    "drones": ("drone", "DRONES"),
    "console": ("console", "DRONE CONTROL STATION"),
    "ala": ("ala", "ALA INDUSTRIAL, ALL THE WAY"),
    "manual": ("manual", "MANUAL, SOUNDS, ADVANCEMENTS"),
    "upgrades": ("upgrade", "UPGRADES AND GUI"),
    "requirements": ("package", "REQUIREMENTS"),
    "spanish": ("flag", "EN ESPANOL"),
    "new": ("new", "WHAT'S NEW"),
}


def header(glyphs, icon, label):
    w, h = 200, 16
    img = Image.new("RGB", (w, h), N2)
    d = ImageDraw.Draw(img)
    d.rectangle([(0, 0), (w - 1, h - 1)], outline=N3)
    # Pestaña azul con el ícono
    d.rectangle([(0, 0), (h - 1, h - 1)], fill=B1)
    d.rectangle([(0, 0), (h - 1, 0)], fill=B2)
    d.rectangle([(h - 1, 0), (h - 1, h - 1)], fill=B0)
    for y, row in enumerate(ICONS[icon]):
        for x, ch in enumerate(row):
            if ch in ICON_COLORS:
                img.putpixel((3 + x, 3 + y), ICON_COLORS[ch])
    draw_text(img, glyphs, h + 5, 4, label, LIGHT, shadow=N0)
    # Remate de acento a la derecha: tres tramos de franja de peligro
    for i in range(10):
        if (i // 2) % 2 == 0:
            d.line([(w - 16 + i, h - 3), (w - 16 + i, h - 2)], fill=A1)
    d.line([(h, h - 1), (w - 1, h - 1)], fill=B0)
    return upscale(img, 4)


def divider():
    w, h = 200, 4
    img = Image.new("RGB", (w, h), N1)
    d = ImageDraw.Draw(img)
    d.line([(0, 1), (w, 1)], fill=N3)
    d.line([(0, 2), (w, 2)], fill=B0)
    for x in range(w // 2 - 12, w // 2 + 12):
        d.line([(x, 0), (x, 3)], fill=A1 if (x // 3) % 2 == 0 else N0)
    return upscale(img, 4)


# ── Marcos de capturas ──

def framed(path, max_width=1280):
    shot = Image.open(path).convert("RGB")
    if shot.width > max_width:
        shot = shot.resize((max_width, round(shot.height * max_width / shot.width)), Image.LANCZOS)
    pad = 10
    img = Image.new("RGB", (shot.width + pad * 2, shot.height + pad * 2), N2)
    d = ImageDraw.Draw(img)
    d.rectangle([(0, 0), (img.width - 1, img.height - 1)], outline=B1, width=2)
    d.rectangle([(4, 4), (img.width - 5, img.height - 5)], outline=N0, width=1)
    img.paste(shot, (pad, pad))
    # Esquinas de acento
    for (x, y) in [(0, 0), (img.width - 16, 0), (0, img.height - 5), (img.width - 16, img.height - 5)]:
        d.rectangle([(x, y), (x + 15, y + 4)], fill=A1)
    return img


def main():
    jar = Path(sys.argv[1]) if len(sys.argv) > 1 else JAR
    glyphs = load_font(jar)
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / "headers").mkdir(exist_ok=True)
    banner(glyphs).save(OUT / "banner.png")
    for key, (icon, label) in HEADERS.items():
        header(glyphs, icon, label).save(OUT / "headers" / f"{key}.png")
    divider().save(OUT / "divider.png")
    shots = OUT / "shots"
    shots.mkdir(exist_ok=True)
    for source in sorted((ROOT / "docs" / "screenshots").glob("*")):
        if source.suffix.lower() in (".jpg", ".png"):
            if source.suffix.lower() == ".png":
                framed(source).save(shots / (source.stem + ".png"))  # interfaces: sin artefactos de JPEG
            else:
                framed(source).save(shots / (source.stem + ".jpg"), quality=90)
    print("listo:", OUT)


if __name__ == "__main__":
    main()
