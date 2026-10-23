"""Blockstates, modelos, loot tables y tag de pico para los bloques de la expansión.

Uso:  python tools/gen_blocks.py
"""
import json
from pathlib import Path

RES = Path(__file__).resolve().parent.parent / "src/main/resources"
ASSETS = RES / "assets/quarryindustrial"
DATA = RES / "data"
NS = "quarryindustrial"

# Orientables con estado encendido/apagado (frente animado cuando LIT=true)
LIT_MACHINES = ["diesel_generator", "steam_turbine"]
# Bloques comunes de una textura por cara (top/side), sin orientación
SIMPLE = []
# Bloques que sueltan su item al romperse
DROPS_SELF = LIT_MACHINES + SIMPLE


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def lit_machine(name):
    for lit in (False, True):
        suffix = "_on" if lit else ""
        write(ASSETS / f"models/block/{name}{suffix}.json", {
            "parent": "minecraft:block/orientable",
            "textures": {"top": f"{NS}:block/{name}_top", "front": f"{NS}:block/{name}_front{suffix}",
                         "side": f"{NS}:block/{name}_side"}})
    variants = {}
    for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for lit in ("false", "true"):
            model = {"model": f"{NS}:block/{name}" + ("_on" if lit == "true" else "")}
            if y:
                model["y"] = y
            variants[f"facing={facing},lit={lit}"] = model
    write(ASSETS / f"blockstates/{name}.json", {"variants": variants})
    write(ASSETS / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": f"{NS}:block/{name}"}})


def drops_self(name):
    write(DATA / f"{NS}/loot_table/blocks/{name}.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1.0, "entries": [{"type": "minecraft:item", "name": f"{NS}:{name}"}],
                   "conditions": [{"condition": "minecraft:survives_explosion"}]}]})


def offshore():
    """Cubierta (rejilla), controlador (mesa rotativa arriba) y pata (columna con agua alrededor)."""
    write(ASSETS / "models/block/offshore_deck.json", {
        "parent": "minecraft:block/cube_all", "textures": {"all": f"{NS}:block/offshore_deck"}})
    write(ASSETS / "blockstates/offshore_deck.json", {"variants": {"": {"model": f"{NS}:block/offshore_deck"}}})
    write(ASSETS / "models/block/offshore_rig.json", {
        "parent": "minecraft:block/cube_bottom_top",
        "textures": {"top": f"{NS}:block/offshore_rig_top", "bottom": f"{NS}:block/offshore_deck",
                     "side": f"{NS}:block/offshore_deck"}})
    write(ASSETS / "blockstates/offshore_rig.json", {"variants": {"": {"model": f"{NS}:block/offshore_rig"}}})
    write(ASSETS / "models/block/offshore_leg.json", {
        "parent": "minecraft:block/block",
        "textures": {"particle": f"{NS}:block/offshore_leg", "side": f"{NS}:block/offshore_leg"},
        "elements": [{"from": [2, 0, 2], "to": [14, 16, 14], "faces": {
            face: {"texture": "#side", "uv": [2, 0, 14, 16]} | ({"cullface": face} if face in ("up", "down") else {})
            for face in ("north", "south", "east", "west", "up", "down")}}]})
    write(ASSETS / "blockstates/offshore_leg.json", {"variants": {
        "waterlogged=false": {"model": f"{NS}:block/offshore_leg"},
        "waterlogged=true": {"model": f"{NS}:block/offshore_leg"}}})
    for name in ("offshore_deck", "offshore_leg"):
        write(ASSETS / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": f"{NS}:block/{name}"}})
    write(ASSETS / "items/offshore_rig.json", {"model": {"type": "minecraft:model", "model": f"{NS}:item/offshore_rig"}})
    write(ASSETS / "models/item/offshore_rig.json", {
        "parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/offshore_rig"}})
    drops_self("offshore_rig")


def drone_pad():
    """Plataforma de 4 px de alto con la H arriba."""
    write(ASSETS / "models/block/drone_pad.json", {
        "parent": "minecraft:block/block",
        "textures": {"particle": f"{NS}:block/drone_pad_side", "top": f"{NS}:block/drone_pad_top",
                     "side": f"{NS}:block/drone_pad_side"},
        "elements": [{"from": [0, 0, 0], "to": [16, 4, 16], "faces": {
            "up": {"texture": "#top"},
            "down": {"texture": "#side", "cullface": "down"},
            **{face: {"texture": "#side", "uv": [0, 12, 16, 16], "cullface": face}
               for face in ("north", "south", "east", "west")}}}]})
    write(ASSETS / "blockstates/drone_pad.json", {"variants": {"": {"model": f"{NS}:block/drone_pad"}}})
    write(ASSETS / "items/drone_pad.json", {"model": {"type": "minecraft:model", "model": f"{NS}:block/drone_pad"}})
    drops_self("drone_pad")


def main():
    drone_pad()
    for name in LIT_MACHINES:
        lit_machine(name)
    offshore()
    for name in DROPS_SELF:
        drops_self(name)
    tag = DATA / "minecraft/tags/block/mineable/pickaxe.json"
    values = json.loads(tag.read_text(encoding="utf-8"))["values"]
    for name in DROPS_SELF + ["offshore_rig", "offshore_deck", "drone_pad"]:
        if f"{NS}:{name}" not in values:
            values.append(f"{NS}:{name}")
    write(tag, {"values": values})
    print(f"{len(DROPS_SELF)} bloques")


if __name__ == "__main__":
    main()
