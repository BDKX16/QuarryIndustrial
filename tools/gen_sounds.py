"""Sintetiza los sonidos del mod (motores, golpes, alarmas) y los pasa a OGG con ffmpeg.

Uso:  python tools/gen_sounds.py      (necesita ffmpeg con libvorbis en el PATH)
Todo es procedural y determinista: ruido filtrado, armónicos y envolventes. Los loops cierran en fase para
que no hagan "clic" al repetirse.
"""
import math
import random
import struct
import subprocess
import tempfile
import wave
from pathlib import Path

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/quarryindustrial/sounds"
RATE = 22050
rng = random.Random(4_4)


def lowpass(samples, alpha):
    out, y = [], 0.0
    for s in samples:
        y += alpha * (s - y)
        out.append(y)
    return out


def noise(n):
    return [rng.uniform(-1, 1) for _ in range(n)]


def tone(n, freq, phase=0.0):
    """Senoidal con un número entero de ciclos en el largo, así el loop cierra."""
    cycles = max(1, round(freq * n / RATE))
    return [math.sin(2 * math.pi * cycles * i / n + phase) for i in range(n)]


def mix(*tracks):
    n = min(len(t) for t, _ in tracks)
    return [sum(t[i] * gain for t, gain in tracks) for i in range(n)]


def normalize(samples, peak=0.85):
    top = max(abs(s) for s in samples) or 1
    return [s / top * peak for s in samples]


def crossfade_loop(samples, fade):
    """Funde el final con el principio para que el loop no se note."""
    body = samples[:-fade]
    tail = samples[-fade:]
    for i in range(fade):
        t = i / fade
        body[i] = body[i] * t + tail[i] * (1 - t)
    return body


def drill_loop():
    """Motor eléctrico grave + el rechinar de la broca contra la roca."""
    n = int(RATE * 2.0)
    hum = mix((tone(n, 55), 0.5), (tone(n, 110), 0.3), (tone(n, 165), 0.12))
    grind = lowpass(noise(n), 0.08)
    wobble = tone(n, 3)
    grind = [g * (0.7 + 0.3 * w) for g, w in zip(grind, wobble)]
    clatter = lowpass([s if rng.random() < 0.02 else 0 for s in noise(n)], 0.3)
    return crossfade_loop(normalize(mix((hum, 0.6), (grind, 1.6), (clatter, 2.5))), 2000)


def diesel_loop():
    """Motor diésel: pulsos de combustión a ~12 Hz (4 cilindros lentos) con ruido de escape."""
    n = int(RATE * 1.5)
    out = [0.0] * n
    period = RATE // 12
    for start in range(0, n, period):
        accent = 1.0 if (start // period) % 4 == 0 else 0.7
        for i in range(min(period, n - start)):
            env = math.exp(-i / (RATE * 0.012))
            out[start + i] += accent * env * rng.uniform(-1, 1)
    out = lowpass(out, 0.12)
    body = mix((out, 1.0), (tone(n, 48), 0.25), (lowpass(noise(n), 0.03), 0.4))
    return crossfade_loop(normalize(body), 1500)


def drone_loop():
    """Hélices: zumbido a ~180 Hz modulado por el paso de las palas, más aire."""
    n = int(RATE * 1.0)
    buzz = mix((tone(n, 180), 0.5), (tone(n, 360), 0.25), (tone(n, 540), 0.1))
    blades = tone(n, 30)
    buzz = [b * (0.75 + 0.25 * m) for b, m in zip(buzz, blades)]
    air = lowpass(noise(n), 0.25)
    return crossfade_loop(normalize(mix((buzz, 1.0), (air, 0.35))), 1200)


def turbine_loop():
    """Turbina de vapor: silbido agudo de los álabes sobre un rumor de vapor."""
    n = int(RATE * 2.0)
    whine = mix((tone(n, 880), 0.25), (tone(n, 1320), 0.12), (tone(n, 440), 0.2))
    steam = lowpass(noise(n), 0.5)
    rumble = lowpass(noise(n), 0.02)
    return crossfade_loop(normalize(mix((whine, 0.8), (steam, 0.5), (rumble, 1.2))), 2000)


def pumpjack_thump():
    """Golpe metálico del vástago en el fondo de la carrera, con un crujido del balancín."""
    n = int(RATE * 0.9)
    out = []
    for i in range(n):
        t = i / RATE
        thump = math.sin(2 * math.pi * 70 * t) * math.exp(-t * 14)
        clank = math.sin(2 * math.pi * 410 * t) * math.exp(-t * 20) * 0.4
        creak = 0.0
        if 0.25 < t < 0.7:
            u = (t - 0.25) / 0.45
            creak = math.sin(2 * math.pi * (240 + 60 * u) * t) * math.sin(math.pi * u) * 0.18
        out.append(thump + clank + creak)
    return normalize(mix((out, 1.0), (lowpass(noise(n), 0.2), 0.02)))


def alarm():
    """Dos tonos cortos, como la alarma de una máquina que se paró."""
    out = []
    for freq in (880, 660):
        n = int(RATE * 0.18)
        for i in range(n):
            env = min(1.0, i / 200) * min(1.0, (n - i) / 400)
            out.append(env * (math.sin(2 * math.pi * freq * i / RATE) + 0.3 * math.sin(4 * math.pi * freq * i / RATE)))
        out.extend([0.0] * int(RATE * 0.05))
    return normalize(out, 0.6)


def write_ogg(name, samples):
    OUT.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        wav = Path(tmp) / f"{name}.wav"
        with wave.open(str(wav), "wb") as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(RATE)
            w.writeframes(b"".join(struct.pack("<h", int(max(-1, min(1, s)) * 32767)) for s in samples))
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", str(wav), "-c:a", "libvorbis", "-q:a", "3",
                        str(OUT / f"{name}.ogg")], check=True)
    print("escrito", name)


def main():
    write_ogg("drill_loop", drill_loop())
    write_ogg("diesel_loop", diesel_loop())
    write_ogg("drone_loop", drone_loop())
    write_ogg("turbine_loop", turbine_loop())
    write_ogg("pumpjack_thump", pumpjack_thump())
    write_ogg("alarm", alarm())


if __name__ == "__main__":
    main()
