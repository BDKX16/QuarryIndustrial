"""Genera las recetas del mod en dos variantes por item.

Uso:  python tools/gen_recipes.py
- <item>.json          se carga solo si está Ala Industrial (usa sus circuitos, carcasas y placas);
- <item>_vanilla.json  se carga solo si NO está Ala (equivalentes vanilla, para jugar el mod solo).
Las condiciones son las de Fabric Resource Conditions ("fabric:load_conditions").
"""
import json
from pathlib import Path

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/data/quarryindustrial/recipe"
WITH_ALA = [{"condition": "fabric:all_mods_loaded", "values": ["alaindustrial"]}]
WITHOUT_ALA = [{"condition": "fabric:not", "value": WITH_ALA[0]}]

Q = "quarryindustrial:"
A = "alaindustrial:"

# item: (cantidad, patrón, claves con Ala, claves vanilla)
RECIPES = {
    "quarry_lv": (1, ["PDP", "CMC", "PGP"],
                  {"P": "#c:plates/iron", "D": "minecraft:diamond_pickaxe", "C": A + "electronic_circuit",
                   "M": A + "machine_casing", "G": A + "iron_gear"},
                  {"P": "#c:ingots/iron", "D": "minecraft:diamond_pickaxe", "C": "minecraft:comparator",
                   "M": "minecraft:iron_block", "G": "minecraft:piston"}),
    "quarry_mv": (1, ["TAT", "GQG", "TCT"],
                  {"T": "#c:plates/tempered_iron", "A": A + "advanced_circuit", "G": A + "gold_gear",
                   "Q": Q + "quarry_lv", "C": A + "copper_coil"},
                  {"T": "#c:ingots/gold", "A": "minecraft:comparator", "G": "minecraft:redstone_block",
                   "Q": Q + "quarry_lv", "C": "minecraft:observer"}),
    "quarry_hv": (1, ["EAE", "DQD", "EXE"],
                  {"E": "#c:plates/electrum", "A": A + "advanced_circuit", "D": A + "electric_drill_diamond_tip",
                   "Q": Q + "quarry_mv", "X": A + "advanced_machine_casing"},
                  {"E": "minecraft:diamond", "A": "minecraft:comparator", "D": "minecraft:diamond_pickaxe",
                   "Q": Q + "quarry_mv", "X": "minecraft:diamond_block"}),
    "quarry_core": (1, ["SRS", "NHN", "SXS"],
                    {"S": "#c:plates/shielding_alloy", "R": A + "reactor_circuit", "N": A + "netherite_drill_head",
                     "H": Q + "quarry_hv", "X": A + "advanced_machine_casing"},
                    {"S": "minecraft:netherite_ingot", "R": "minecraft:nether_star", "N": "minecraft:netherite_pickaxe",
                     "H": Q + "quarry_hv", "X": "minecraft:diamond_block"}),
    "quarry_casing": (4, ["TST", "SIS", "TST"],
                      {"T": "#c:plates/tempered_iron", "S": "#c:plates/shielding_alloy", "I": "#c:ingots/iron"},
                      {"T": "#c:ingots/iron", "S": "minecraft:obsidian", "I": "minecraft:iron_block"}),
    "energy_port": (1, [" C ", "CXC", " A "],
                    {"C": A + "insulated_electrum_cable", "X": Q + "quarry_casing", "A": A + "advanced_circuit"},
                    {"C": "minecraft:redstone_block", "X": Q + "quarry_casing", "A": "minecraft:comparator"}),
    "pumpjack": (1, ["TTT", "GPG", "MCM"],
                 {"T": "#c:plates/tempered_iron", "G": A + "iron_gear", "P": A + "pump", "M": A + "machine_casing",
                  "C": A + "electronic_circuit"},
                 {"T": "#c:ingots/iron", "G": "minecraft:piston", "P": "minecraft:bucket", "M": "minecraft:iron_block",
                  "C": "minecraft:comparator"}),
    "upgrade_area": (1, ["RCR", "CBC", "RCR"],
                     {"R": "minecraft:redstone", "C": "minecraft:compass", "B": A + "module_blank"},
                     {"R": "minecraft:redstone", "C": "minecraft:compass", "B": "minecraft:redstone_block"}),
    "upgrade_speed": (1, [" O ", "RBR", " O "],
                      {"O": A + "overclocker_chip_i", "R": "minecraft:redstone", "B": A + "module_blank"},
                      {"O": "minecraft:sugar", "R": "minecraft:redstone", "B": "minecraft:redstone_block"}),
    "upgrade_fortune": (1, ["LDL", "DBD", "LDL"],
                        {"L": "minecraft:lapis_block", "D": "minecraft:diamond", "B": A + "module_blank"},
                        {"L": "minecraft:lapis_block", "D": "minecraft:diamond", "B": "minecraft:redstone_block"}),
    "upgrade_silk_touch": (1, ["SES", "EBE", "SES"],
                           {"S": "minecraft:string", "E": "minecraft:emerald", "B": A + "module_blank"},
                           {"S": "minecraft:string", "E": "minecraft:emerald", "B": "minecraft:redstone_block"}),
    "upgrade_void": (1, ["OPO", "PBP", "OPO"],
                     {"O": "minecraft:obsidian", "P": "minecraft:ender_pearl", "B": A + "module_blank"},
                     {"O": "minecraft:obsidian", "P": "minecraft:ender_pearl", "B": "minecraft:redstone_block"}),
}


def recipe(item, count, pattern, key, conditions):
    result = {"id": Q + item}
    if count > 1:
        result["count"] = count
    return {
        "fabric:load_conditions": conditions,
        "type": "minecraft:crafting_shaped",
        "category": "misc" if item.startswith("upgrade") else "redstone",
        "pattern": pattern,
        "key": key,
        "result": result,
    }


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for item, (count, pattern, ala, vanilla) in RECIPES.items():
        for suffix, key, conditions in (("", ala, WITH_ALA), ("_vanilla", vanilla, WITHOUT_ALA)):
            path = OUT / f"{item}{suffix}.json"
            path.write_text(json.dumps(recipe(item, count, pattern, key, conditions), indent=2) + "\n",
                            encoding="utf-8")
    print(f"{len(RECIPES) * 2} recetas en {OUT}")


if __name__ == "__main__":
    main()
