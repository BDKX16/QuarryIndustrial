"""Escribe items/<id>.json y models/item/<id>.json para los items planos (un sprite) del mod.

Uso:  python tools/gen_item_models.py
Los bloques tienen sus propios modelos, hechos a mano.
"""
import json
from pathlib import Path

ASSETS = Path(__file__).resolve().parent.parent / "src/main/resources/assets/quarryindustrial"

FLAT_ITEMS = [
    "drill_bit_iron", "drill_bit_diamond", "drill_bit_netherite", "drill_bit_pdc",
    "upgrade_ore_seeker",
    "cargo_drone", "tanker_drone", "power_drone", "maintenance_drone", "survey_drone",
    "drone_linker", "survey_report", "manual",
]
# Herramientas: se sostienen como una espada
HANDHELD = {"drone_linker"}


def main():
    for item in FLAT_ITEMS:
        parent = "minecraft:item/handheld" if item in HANDHELD else "minecraft:item/generated"
        (ASSETS / "models/item").mkdir(parents=True, exist_ok=True)
        (ASSETS / "items").mkdir(parents=True, exist_ok=True)
        (ASSETS / f"models/item/{item}.json").write_text(json.dumps(
            {"parent": parent, "textures": {"layer0": f"quarryindustrial:item/{item}"}}, indent=2) + "\n",
            encoding="utf-8")
        (ASSETS / f"items/{item}.json").write_text(json.dumps(
            {"model": {"type": "minecraft:model", "model": f"quarryindustrial:item/{item}"}}, indent=2) + "\n",
            encoding="utf-8")
    print(f"{len(FLAT_ITEMS)} items")


if __name__ == "__main__":
    main()
