# Integración de energía con Ala Industrial (Fabric, MC 26.3)

## Lo que encontré
- **Repo (MIT):** https://github.com/AlaMine-Team-Org/AlaIndustrial (revisado el 23/10/2026, commit `dd61a7d`).
- **No tiene API propia ni artefacto Maven publicado.** No hace falta: en Fabric su energía "EU" es
  **Team Reborn Energy** (`teamreborn:energy:5.0.0`), que va embebida (JiJ) dentro de su jar.
  - Busca vecinos con `EnergyStorage.SIDED.find(level, pos, side)` (`FabricEnergyLookup.java`).
  - Expone sus máquinas con `EnergyStorage.SIDED.registerForBlockEntity(...)` (`ModBlockEntities.java`).
  - 1 EU = 1 unidad de TR Energy (los montos pasan sin conversión, ver `PortAsEnergyStorage.java`).
- No usa directamente la Fabric Transfer API para energía, pero TR Energy se apoya en sus transacciones
  (`Transaction`, `SnapshotParticipant`).
- **Versiones:** CurseForge solo muestra la 0.1.174 para 26.2, pero la rama `main` ya es para **26.3**
  y hay releases en GitHub/Modrinth `v0.1.202-mc26.3`. Para 26.3 usa Fabric Loader 0.19.5 y
  Fabric API `0.161.0+26.3`. En tu server hay que instalar el jar `-mc26.3+fabric`, no el de CurseForge 26.2.
- Sus cables tratan a un bloque de otro mod que acepte inserción como un "almacenamiento" y le
  empujan energía según el espacio libre que tenga. No encontré daño por sobrevoltaje a bloques ajenos.

## build.gradle
Fabric Loom ya añade `maven.fabricmc.net`, que es donde vive `teamreborn:energy`.

```groovy
dependencies {
    // Team Reborn Energy: la interfaz común que usa Ala Industrial (y Tech Reborn, etc.)
    include(implementation("teamreborn:energy:5.0.0"))

    // Opcional: Ala Industrial en el cliente de desarrollo para probar con sus cables/generadores
    // (verifica el número exacto en la pestaña Versions de Modrinth)
    // runtimeOnly("maven.modrinth:ala-industrial:0.1.202-mc26.3+fabric")
}
```
MC 26.x ya no está ofuscado y Loom 1.17 no tiene configuraciones `mod*`, por eso se usa
`implementation` normal (así lo hace Ala). Para el runtime de Ala añade el repo de Modrinth:
```groovy
repositories {
    maven { url = 'https://api.modrinth.com/maven'; content { includeGroup 'maven.modrinth' } }
}
```
En `fabric.mod.json` → `"depends": { "team_reborn_energy": "*" }` y, si lo quieres opcional,
`"suggests": { "alaindustrial": "*" }`. No dependas de Ala directamente: así tu quarry también
funciona con Tech Reborn y cualquier mod que use TR Energy.

## Block entity de la quarry
```java
public class QuarryBlockEntity extends BlockEntity {
    // capacidad, maxInsert por tick, maxExtract (0 = solo consume)
    public final SimpleEnergyStorage energy = new SimpleEnergyStorage(100_000, 512, 0) {
        @Override protected void onFinalCommit() { setChanged(); }
    };

    public static void tick(Level level, BlockPos pos, BlockState state, QuarryBlockEntity be) {
        long costPerBlock = be.costPerBlock(); // escala con tamaño/upgrades
        if (be.energy.amount >= costPerBlock) {
            be.energy.amount -= costPerBlock;   // fuera de transacción: se modifica directo
            be.setChanged();
            be.mineNextBlock();
        }
    }

    // Guardar/cargar energy.amount en saveAdditional/loadAdditional (ValueOutput/ValueInput en 26.x)
}
```
Registro en el `ModInitializer`:
```java
EnergyStorage.SIDED.registerForBlockEntity((be, side) -> be.energy, ModBlockEntities.QUARRY);
```
Con eso los cables de Ala Industrial la detectan sola. Si quieres que reciba solo por ciertas caras,
devuelve `null` en las caras que no aceptan energía.

## Ideas para la quarry escalable
- Coste por bloque = base × (1 + área/…) y `maxInsert` que suba con un upgrade de "voltaje" (LV 32,
  MV 128, HV 512 EU/t al estilo IC2) para que la quarry grande pida cables de tier más alto.
- Mostrar `energy.amount / energy.capacity` en la GUI con un `ContainerData`.
