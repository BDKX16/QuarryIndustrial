"""Genera las recetas del mod en dos variantes por item.

Uso:  python tools/gen_recipes.py
- <item>.json          se carga solo si está Ala Industrial y registra todos los items suyos que usa la receta
                       (una versión de Ala sin alguno de ellos haría fallar la carga de datapacks del server);
- <item>_vanilla.json  se carga en cualquier otro caso (equivalentes vanilla, para jugar el mod solo).
Las condiciones son las de Fabric Resource Conditions ("fabric:load_conditions").
"""
import json
from pathlib import Path

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/data/quarryindustrial/recipe"
ALA_LOADED = {"condition": "fabric:all_mods_loaded", "values": ["alaindustrial"]}

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
    # ── Brocas: cada una se arma con la anterior (que puede estar gastada) ──
    "drill_bit_iron": (1, [" P ", "PGP", "I I"],
                       {"P": "#c:plates/iron", "G": A + "iron_gear", "I": "#c:ingots/iron"},
                       {"P": "#c:ingots/iron", "G": "minecraft:iron_block", "I": "#c:ingots/iron"}),
    "drill_bit_diamond": (1, [" D ", "DBD", " D "],
                          {"D": "minecraft:diamond", "B": Q + "drill_bit_iron"},
                          {"D": "minecraft:diamond", "B": Q + "drill_bit_iron"}),
    "drill_bit_netherite": (1, ["N", "B"],
                            {"N": A + "netherite_drill_head", "B": Q + "drill_bit_diamond"},
                            {"N": "minecraft:netherite_ingot", "B": Q + "drill_bit_diamond"}),
    "drill_bit_pdc": (1, ["DSD", "DBD", "DSD"],
                      {"D": "minecraft:diamond", "S": "#c:plates/shielding_alloy", "B": Q + "drill_bit_netherite"},
                      {"D": "minecraft:diamond", "S": "minecraft:netherite_scrap", "B": Q + "drill_bit_netherite"}),
    "upgrade_ore_seeker": (1, [" R ", "LBL", " C "],
                           {"R": A + "magnet_filter_module", "L": "minecraft:spyglass", "B": A + "module_blank",
                            "C": A + "drill_column_module"},
                           {"R": "minecraft:compass", "L": "minecraft:spyglass", "B": "minecraft:redstone_block",
                            "C": "minecraft:diamond_pickaxe"}),
    # ── Generadores ──
    "diesel_generator": (1, [" T ", "GXG", "PCP"],
                         {"T": A + "fluid_tank", "G": A + "iron_gear", "X": A + "generator", "P": "#c:plates/iron",
                          "C": A + "electronic_circuit"},
                         {"T": "minecraft:bucket", "G": "minecraft:piston", "X": "minecraft:blast_furnace",
                          "P": "#c:ingots/iron", "C": "minecraft:redstone"}),
    "steam_turbine": (1, ["PBP", "CXC", "PSP"],
                      {"P": "#c:plates/tempered_iron", "B": A + "iron_gear", "C": A + "copper_coil",
                       "X": A + "machine_casing", "S": A + "steam_pipe"},
                      {"P": "#c:ingots/iron", "B": "minecraft:iron_bars", "C": "minecraft:copper_block",
                       "X": "minecraft:iron_block", "S": "minecraft:bucket"}),
    "offshore_rig": (1, ["SPS", "BJB", "SCS"],
                     {"S": "#c:plates/tempered_iron", "P": A + "pump", "B": Q + "drill_bit_diamond",
                      "J": Q + "pumpjack", "C": A + "advanced_circuit"},
                     {"S": "minecraft:iron_block", "P": "minecraft:bucket", "B": Q + "drill_bit_diamond",
                      "J": Q + "pumpjack", "C": "minecraft:comparator"}),
    # ── Drones: el chasis es el dron de jardín de Ala (sin Ala, un bloque de cobre) y la carga depende del tipo ──
    **{f"{kind}_drone": (1, ["M M", " G ", "CXC"],
                         {"M": "minecraft:phantom_membrane", "G": A + "garden_drone", "C": A + "electronic_circuit",
                          "X": ala_part},
                         {"M": "minecraft:phantom_membrane", "G": "minecraft:copper_block", "C": "minecraft:redstone",
                          "X": vanilla_part})
       for kind, ala_part, vanilla_part in (
           ("cargo", "minecraft:chest", "minecraft:chest"),
           ("tanker", A + "fluid_tank", "minecraft:bucket"),
           ("power", A + "battery_box", "minecraft:redstone_block"),
           ("maintenance", A + "wrench", "minecraft:anvil"),
           ("survey", "minecraft:spyglass", "minecraft:spyglass"))},
    "drone_pad": (2, ["YYY", "CXC"],
                  {"Y": "minecraft:yellow_concrete", "C": A + "electronic_circuit", "X": A + "machine_casing"},
                  {"Y": "minecraft:yellow_concrete", "C": "minecraft:redstone", "X": "minecraft:iron_block"}),
    "drone_console": (1, [" A ", "GMG", "CXC"],
                      {"A": "minecraft:lightning_rod", "G": "minecraft:glass_pane", "M": "minecraft:map",
                       "C": A + "advanced_circuit", "X": A + "machine_casing"},
                      {"A": "minecraft:lightning_rod", "G": "minecraft:glass_pane", "M": "minecraft:map",
                       "C": "minecraft:comparator", "X": "minecraft:iron_block"}),
    "drone_linker": (1, [" A ", "IRI", "III"],
                     {"A": "minecraft:lightning_rod", "I": "#c:plates/iron", "R": A + "electronic_circuit"},
                     {"A": "minecraft:lightning_rod", "I": "#c:ingots/iron", "R": "minecraft:redstone"}),
}


def category(item):
    if item.startswith("upgrade"):
        return "misc"
    if item.startswith("drill_bit"):
        return "equipment"
    return "redstone"


def recipe(item, count, pattern, key, conditions):
    result = {"id": Q + item}
    if count > 1:
        result["count"] = count
    return {
        "fabric:load_conditions": conditions,
        "type": "minecraft:crafting_shaped",
        "category": category(item),
        "pattern": pattern,
        "key": key,
        "result": result,
    }


def ala_conditions(key):
    """Ala cargado y, si la receta usa items de Ala, que existan en la versión instalada."""
    items = sorted({v for v in key.values() if v.startswith(A)})
    conditions = [ALA_LOADED]
    if items:
        conditions.append({"condition": "fabric:registry_contains", "registry": "minecraft:item", "values": items})
    return conditions


def vanilla_conditions(key):
    return [{"condition": "fabric:not", "value": {"condition": "fabric:and", "values": ala_conditions(key)}}]


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for item, (count, pattern, ala, vanilla) in RECIPES.items():
        for suffix, key, conditions in (("", ala, ala_conditions(ala)), ("_vanilla", vanilla, vanilla_conditions(ala))):
            path = OUT / f"{item}{suffix}.json"
            path.write_text(json.dumps(recipe(item, count, pattern, key, conditions), indent=2) + "\n",
                            encoding="utf-8")
    print(f"{len(RECIPES) * 2} recetas en {OUT}")


if __name__ == "__main__":
    main()
