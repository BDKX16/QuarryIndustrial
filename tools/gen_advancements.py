"""Genera los logros del mod (pestaña propia) y sus textos.

Uso:  python tools/gen_advancements.py   (después, python tools/gen_lang.py no los pisa: van en otro bloque)
Los que dependen de lo que hace una máquina usan el trigger minecraft:impossible y los otorga el código
(ver ModAdvancements); los de items, minecraft:inventory_changed.
"""
import json
from pathlib import Path

from lang_es import to_tu

RES = Path(__file__).resolve().parent.parent / "src/main/resources"
OUT = RES / "data/quarryindustrial/advancement"
LANG = RES / "assets/quarryindustrial/lang"
NS = "quarryindustrial"

# id: (padre, ícono, marco, criterio, título en, descripción en, título es, descripción es)
# criterio: None = impossible (lo otorga el código), o el id del item a conseguir
ADVANCEMENTS = {
    "root": (None, f"{NS}:quarry_hv", "task", f"{NS}:quarry_lv",
             "Quarry Industrial", "Craft your first quarry (the Operator's Manual comes with it)",
             "Quarry Industrial", "Fabricá tu primera quarry (viene con el Manual del operador)"),
    "frame_built": ("root", f"{NS}:quarry_lv", "task", None,
                    "Raise the Gantry", "Watch a quarry build its frame",
                    "Arriba el pórtico", "Mirá cómo una quarry arma su marco"),
    "bit_broken": ("frame_built", f"{NS}:drill_bit_iron", "task", None,
                   "Worn to the Shank", "Wear a drill bit down until it breaks",
                   "Gastada hasta el vástago", "Usá una broca hasta romperla"),
    "pdc_bit": ("bit_broken", f"{NS}:drill_bit_pdc", "goal", f"{NS}:drill_bit_pdc",
                "Diamond Cutters", "Get a PDC bit, the endgame drill bit",
                "Cortadores de diamante", "Conseguí una broca PDC, la mejor de todas"),
    "rock_bottom": ("frame_built", "minecraft:bedrock", "challenge", None,
                    "Rock Bottom", "Let a quarry dig all the way down to the bottom of the world",
                    "Tocando fondo", "Dejá que una quarry excave hasta el fondo del mundo"),
    "nuclear_eight": ("frame_built", f"{NS}:quarry_core", "challenge", None,
                      "Eight Reactors, One Hole", "Form a Nuclear Quarry with all 8 energy ports",
                      "Ocho reactores, un pozo", "Armá una quarry nuclear con los 8 puertos de energía"),
    "black_gold": ("root", f"{NS}:pumpjack", "task", None,
                   "Black Gold", "Start pumping oil from a geyser well",
                   "Oro negro", "Empezá a sacar petróleo de un géiser"),
    "bottomless": ("black_gold", f"{NS}:pumpjack", "goal", None,
                   "Bottomless Well", "Drain a geyser dome dry: from now on it pumps forever",
                   "Pozo sin fondo", "Vaciá el domo de un géiser: de ahí en más bombea para siempre"),
    "self_sufficient": ("black_gold", f"{NS}:diesel_generator", "task", None,
                        "Self-Sufficient", "Run a pumpjack on diesel",
                        "Autosuficiente", "Hacé andar una cigüeña a diésel"),
    "into_the_deep": ("black_gold", f"{NS}:offshore_rig", "goal", None,
                      "Into the Deep", "Drill an offshore rig down to its reservoir",
                      "Mar adentro", "Perforá con una plataforma offshore hasta el reservorio"),
    "waste_not": ("root", f"{NS}:steam_turbine", "task", None,
                  "Waste Not", "Turn a reactor's steam into power with a Steam Turbine",
                  "Nada se pierde", "Convertí el vapor de un reactor en energía con una turbina"),
    "air_mail": ("root", f"{NS}:cargo_drone", "task", None,
                 "Air Mail", "Have a drone deliver something to another pad",
                 "Correo aéreo", "Hacé que un dron entregue algo en otro pad"),
    "pit_crew": ("air_mail", f"{NS}:maintenance_drone", "task", None,
                 "Pit Crew", "Let a maintenance drone swap a drill bit for you",
                 "Equipo de boxes", "Dejá que un dron de mantenimiento te cambie una broca"),
    "prospector": ("air_mail", f"{NS}:survey_drone", "goal", None,
                   "Prospector", "Find an oil geyser with a survey drone",
                   "Buscador de petróleo", "Encontrá un géiser de petróleo con un dron explorador"),
}


def advancement(name, parent, icon, frame, item):
    if item is None:
        criteria = {"done": {"trigger": "minecraft:impossible"}}
        requirement = "done"
    else:
        criteria = {"has_item": {"trigger": "minecraft:inventory_changed",
                                 "conditions": {"items": [{"items": item}]}}}
        requirement = "has_item"
    display = {
        "icon": {"id": icon},
        "title": {"translate": f"advancements.{NS}.{name}.title"},
        "description": {"translate": f"advancements.{NS}.{name}.description"},
        "frame": frame,
    }
    data = {}
    if parent is None:
        display["background"] = "minecraft:gui/advancements/backgrounds/stone"
        display["show_toast"] = False
        display["announce_to_chat"] = False
        data["rewards"] = {"loot": [f"{NS}:grants/manual"]}
    else:
        data["parent"] = f"{NS}:{parent}"
    data["criteria"] = criteria
    data["display"] = display
    data["requirements"] = [[requirement]]
    return data


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for name, (parent, icon, frame, item, *_texts) in ADVANCEMENTS.items():
        (OUT / f"{name}.json").write_text(json.dumps(advancement(name, parent, icon, frame, item), indent=2) + "\n",
                                          encoding="utf-8")
    grants = RES / f"data/{NS}/loot_table/grants"
    grants.mkdir(parents=True, exist_ok=True)
    (grants / "manual.json").write_text(json.dumps({
        "type": "minecraft:advancement_reward",
        "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{NS}:manual"}]}]}, indent=2) + "\n",
        encoding="utf-8")
    for code, offset in (("en_us", 0), ("es_ar", 2), ("es_es", 2)):
        path = LANG / f"{code}.json"
        lang = json.loads(path.read_text(encoding="utf-8"))
        for name, (_p, _i, _f, _it, *texts) in ADVANCEMENTS.items():
            convert = to_tu if code == "es_es" else str
            lang[f"advancements.{NS}.{name}.title"] = convert(texts[offset])
            lang[f"advancements.{NS}.{name}.description"] = convert(texts[offset + 1])
        path.write_text(json.dumps(lang, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"{len(ADVANCEMENTS)} logros")


if __name__ == "__main__":
    main()
