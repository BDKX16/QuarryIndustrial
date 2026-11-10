# Release 1.3.0 para CurseForge

Todo listo para copiar en el formulario de «Upload file» (ver el resto de los pasos en `SUBIR.md`).

| Campo | Valor |
|---|---|
| **Archivo** | `build/libs/QuarryIndustrial-1.3.0.jar` (no el `-sources.jar`) |
| **Display name** | `Quarry Industrial 1.3.0 (26.3, Fabric)` |
| **Release type** | Beta (el filtro todavía no se jugó en partidas reales; pasar a Release cuando lo esté) |
| **Game version** | 26.3 |
| **Mod loader** | Fabric |
| **Java** | 25 |
| **Relaciones** | Fabric API → *Required dependency*; Ala Industrial → *Optional dependency* |
| **Changelog** | El de abajo (Markdown) |

---

## Changelog (pegar tal cual)

## 1.3.0 — Pipe Filter

### New

- **Pipe Filter** (needs Ala Industrial). Right-click it onto the arm of an Ala Industrial item pipe (basic or
  advanced) that touches an inventory, and that side only lets the items you mark go through, both when the pipe
  pulls from the inventory and when it pushes into it. It uses no energy. Right-click it with an empty hand to open
  its screen; sneak + right-click takes it off with its settings, and it drops as an item if the pipe breaks.
  - **You can see it.** A filtered side shows an iron sleeve with a cyan band on the pipe's arm.
  - **One item, or five.** Out of the box it filters a single item: put an item in the slot to mark it (nothing is
    consumed). The **Pipe Filter Expansion**, which goes in its own panel on the right of the screen (like the
    quarry's upgrades), unlocks all five slots.
  - **Allow or deny.** With one slot it reads *Allow* / *Deny*; with the expansion it becomes an *Allow list* / *Deny
    list*. An empty allow list lets nothing through and an empty deny list lets everything through.
  - **By item or by category.** *By item* matches only the exact item. *By category* matches every item that shares
    a tag with the marked one: mark an oak log and every kind of log goes through. An item can belong to several
    categories, so click its slot to switch which one is used. A panel on the right lists every item in the chosen
    category (scroll to see more), and each button explains itself in a tooltip.
  - Crafted with an Ala **Empty Chip** (the expansion needs one too). Without Ala Industrial the recipes use a
    comparator instead.

### Changes

- The in-game manual gets a Pipe Filter chapter, an Energy Distributor page and a long-range surveying page (range
  upgrades, the console's Travel button), and the console page now covers lakes, range rings and Radar Repeaters.
- Shared drawing code for the mod's screens (quarry, machine info screens and the pipe filter), so their panels,
  slots and colours stay consistent.

### Notes

- The Pipe Filter only does something with Ala Industrial installed; without it the two items still exist but have
  no pipe to attach to.
- It plugs into Ala's item pipes through mixins, so a future Ala update that renames its internals could stop the
  filter from acting until this mod is updated.
