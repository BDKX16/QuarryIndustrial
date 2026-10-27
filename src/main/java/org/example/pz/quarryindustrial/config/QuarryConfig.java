package org.example.pz.quarryindustrial.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;
import org.example.pz.quarryindustrial.Quarryindustrial;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Todos los números de balance en {@code config/quarryindustrial.json}. Se lee una vez al arrancar;
 * los campos que falten en el archivo toman el valor por defecto y el archivo se reescribe completo,
 * así una actualización del mod agrega las opciones nuevas sin pisar las que el admin cambió.
 */
public final class QuarryConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static QuarryConfig instance = new QuarryConfig();

    // ── Coste por bloque: base × (1 + dureza) × multiplicadores de upgrades ──
    public long baseCostPerBlock = 50;
    public double speedUpgradeCostMultiplier = 1.25;
    public double fortuneCostMultiplierPerLevel = 0.5;
    public double silkTouchCostMultiplier = 2.0;

    // ── Tiers: EU/t de entrada, buffer, lado base/máximo del área, cabezales, bloques por segundo máx. ──
    public Tier lv = new Tier(32, 20_000, 9, 16, 1, 5);
    public Tier mv = new Tier(128, 80_000, 16, 48, 1, 20);
    public Tier hv = new Tier(512, 320_000, 32, 80, 2, 60);
    /**
     * Quarry nuclear (multibloque). maxInsert es por puerto de energía: con N puertos entra N × 512 EU/t,
     * uno por reactor de Ala Industrial (que tampoco pasa de 512 EU/t).
     */
    public Tier nuclear = new Tier(512, 2_000_000, 64, 144, 4, 400);
    /** Buffer de cada puerto de energía de la quarry nuclear. */
    public long energyPortCapacity = 4096;
    /** Máximo de puertos que cuenta el core (los demás quedan como carcasa). */
    public int maxEnergyPorts = 8;

    /** Cada upgrade de velocidad suma este porcentaje al tope de bloques por segundo. */
    public int speedUpgradePercent = 50;
    /** Cuánto crece el lado del área con cada upgrade de área (hasta el máximo del tier). */
    public int areaUpgradeStep = 16;

    /** Altura del pórtico sobre la quarry, en bloques. */
    public int frameHeight = 4;
    /** EU por cada bloque de marco que la quarry coloca antes de empezar a minar. */
    public long frameCostPerBlock = 100;
    /** Tope de bloques de marco colocados por tick. */
    public int frameBlocksPerTick = 2;

    /** Mantiene cargados el chunk de la quarry y los chunks donde están los cabezales. */
    public boolean chunkLoading = true;
    /** Si es true, al minar un cofre/barril su contenido va al buffer; si es false, la quarry lo salta. */
    public boolean mineContainers = true;
    /** Tope de posiciones vacías (aire, bedrock...) que un cabezal salta por tick. */
    public int maxSkipsPerTick = 256;

    // ── Brocas, refrigerante y calor ──

    public Bits bits = new Bits();
    /** Tanque de agua de la quarry (refrigerante / lodo de perforación), en cubos. */
    public int coolantTankBuckets = 8;
    /** mB de agua por bloque minado. Con agua: menos desgaste y más velocidad. */
    public int coolantPerBlockMb = 1;
    /** Bonus de velocidad con refrigerante, en porcentaje. */
    public int coolantSpeedBonusPercent = 25;
    /** Multiplicador del desgaste de la broca con refrigerante. */
    public double coolantWearMultiplier = 0.5;
    /** Quarry nuclear: calor por bloque minado sin refrigerante (el calor va de 0 a 1000). */
    public double nuclearHeatPerBlock = 1.2;
    /** Calor que se disipa solo, por tick. */
    public double heatDissipationPerTick = 6;
    /** Desde este calor la quarry nuclear empieza a frenar; en 1000 va al 20 % de velocidad. */
    public double heatThrottleStart = 700;

    public static final class Bits {
        /** Sin broca la quarry no mina (el marco lo arma igual). */
        public boolean requireDrillBit = true;
        /** Desgaste por bloque = 1 + dureza / esto (piedra 1, pizarra profunda 2, obsidiana 17). */
        public double hardnessPerWearPoint = 3;
        public Bit iron = new Bit(6_000, 1.0);
        public Bit diamond = new Bit(24_000, 1.3);
        public Bit netherite = new Bit(80_000, 1.6);
        public Bit pdc = new Bit(200_000, 2.0);
    }

    public static final class Bit {
        /** Vida en puntos de desgaste. Se lee al arrancar (es la durabilidad del item). */
        public int durability;
        /** Multiplica el tope de bloques por segundo de la quarry. */
        public double speedMultiplier;

        Bit(int durability, double speedMultiplier) {
            this.durability = durability;
            this.speedMultiplier = speedMultiplier;
        }
    }

    // ── Generadores (diésel y turbina de vapor) ──

    public Generators generators = new Generators();

    public static final class Generators {
        /** EU por mB de cada combustible (la columna de Ala saca 700 de diésel y 200 de fuel oil por cubo de crudo). */
        public long dieselEuPerMb = 12;
        public long fuelOilEuPerMb = 8;
        public long biofuelEuPerMb = 8;
        /** Generador diésel: EU/t que produce mientras tenga combustible y lugar en el buffer (MV). */
        public long dieselGeneratorEuPerTick = 128;
        public long dieselGeneratorCapacity = 40_000;
        public int dieselGeneratorTankBuckets = 8;
        /** Turbina de vapor: EU por mB de vapor y tope de vapor que traga por tick. */
        public long turbineEuPerMbSteam = 2;
        public long turbineMaxSteamMbPerTick = 512;
        /** Tope de salida de la turbina por tick (repartido entre los vecinos). */
        public long turbineMaxOutputPerTick = 1024;
        public long turbineCapacity = 100_000;
        public int turbineTankBuckets = 16;
    }

    // ── Cigüeña de petróleo (pumpjack) ──

    public Pumpjack pumpjack = new Pumpjack();

    public static final class Pumpjack {
        /** Caudal infinito una vez vaciado el domo, en mB por tick, sin upgrades. */
        public int baseRateMbPerTick = 10;
        /** Consumo en ese régimen, en EU/t, sin upgrades. */
        public long baseEuPerTick = 32;
        /** Cada upgrade de velocidad suma este porcentaje al caudal y al consumo. */
        public int speedUpgradePercent = 50;
        /** Mientras el domo tiene oil de verdad: un cubo cada tantos ticks... */
        public int drainTicksPerBucket = 20;
        /** ...y tantos EU por cubo. */
        public long drainEuPerBucket = 500;
        public long energyCapacity = 20_000;
        /** Entrada máxima: MV, para que alcance con todos los upgrades. */
        public long maxInsert = 128;
        public int tankBuckets = 16;

        // Detección del pozo de un géiser (ver OilWellValidator)
        /** Largo mínimo del caño de 1×1 entre la superficie y el domo. Ala genera al menos 24. */
        public int minShaftLength = 16;
        /** Radio mínimo de la cavidad del domo. Ala genera de 8 a 11. */
        public int minCavityRadius = 6;
        /** El centro del domo tiene que estar por debajo de esta Y. Ala lo pone en -45. */
        public int maxCavityCenterY = -20;
        /** Hasta dónde baja el escaneo buscando el domo. */
        public int maxScanDepth = 160;
        /** Celdas que recorre como máximo la búsqueda de oil para vaciar el domo, por escaneo. */
        public int drainScanMaxVisited = 8192;
        /** Motor diésel: tanque de combustible (entra por la fila del motor, atrás de todo). */
        public int fuelTankBuckets = 8;
        /**
         * Probabilidad por tick de que salga un Oil Slime de Ala del pozo mientras la cigüeña bombea
         * (1 en N; 0 = nunca). Con 12000, uno cada ~10 minutos.
         */
        public int oilSlimeChance = 12_000;
    }

    // ── Drones ──

    public Drones drones = new Drones();

    public static final class Drones {
        /** Distancia máxima entre dos pads vinculados. */
        public int maxLinkDistance = 512;
        /** Bloques por tick en crucero (0.6 = 12 bloques por segundo). */
        public double speed = 0.6;
        /** Altura de crucero sobre el más alto de los dos extremos. */
        public int cruiseAltitude = 16;
        /** EU que cobra el pad por viaje: fijo + por bloque de distancia (ida). */
        public long tripBaseEu = 1_000;
        public long tripEuPerBlock = 8;
        public long padEnergyCapacity = 500_000;
        public long padMaxTransfer = 2_048;
        public int padTankBuckets = 16;
        /** El dron de carga sale con tantos stacks o cuando lo que hay lleva {@code launchIdleTicks} esperando. */
        public int cargoLaunchStacks = 4;
        public int launchIdleTicks = 200;
        /** Cisterna: capacidad y cuánto espera juntar antes de salir. */
        public int tankerCapacityBuckets = 16;
        public int tankerLaunchBuckets = 8;
        /** Dron de energía: capacidad de su batería y desde cuánto sale. */
        public long powerCapacity = 400_000;
        public long powerLaunchEu = 100_000;
        /** Mantenimiento: radio de búsqueda y desde qué desgaste cambia la broca. */
        public int maintenanceRadius = 48;
        public double maintenanceWear = 0.9;
        /** Mantenimiento: desde qué ensuciamiento (0-100) limpia una columna de destilación de Ala Industrial. */
        public int maintenanceFouling = 50;
        /** Explorador: radio del vuelo de reconocimiento y ancho de la franja que mira por pasada. */
        public int surveyRadius = 128;
        public int surveyScanRadius = 6;
        /** Ticks sin noticias de un dron en vuelo para darlo por perdido (y devolverlo al pad, sin la carga). */
        public int lostAfterTicks = 6_000;
        /** Centralita: hasta qué distancia (horizontal) ve pads, drones y géiseres. */
        public int consoleRange = 1_024;
        /** Radar repetidor: hasta dónde ve a su alrededor, si él mismo está dentro de lo que ya ve la centralita. */
        public int radarRange = 1_024;
        /** Mejora de alcance del explorador: bloques de radio que suma cada una y cuántas entran en un pad. */
        public int surveyRangeBonus = 64;
        public int surveyRangeMax = 4;
    }

    // ── Plataforma offshore ──

    public Offshore offshore = new Offshore();

    public static final class Offshore {
        /** Solo en biomas de océano (#minecraft:is_ocean). */
        public boolean requireOceanBiome = true;
        /** Agua mínima debajo de la cubierta, en bloques. */
        public int minWaterDepth = 8;
        /** Tope de profundidad hasta donde bajan las patas buscando el fondo. */
        public int maxWaterDepth = 64;
        /** Metros a perforar debajo del lecho marino antes de llegar al reservorio. */
        public int drillDepth = 24;
        /** Ticks y EU por metro perforado; la broca se gasta tantos puntos por metro. */
        public int drillTicksPerMeter = 40;
        public long drillEuPerTick = 256;
        public int bitWearPerMeter = 50;
        /** Producción infinita una vez perforado, en mB/t sin upgrades, y su consumo. */
        public int baseRateMbPerTick = 25;
        public long baseEuPerTick = 96;
        public int speedUpgradePercent = 50;
        public long energyCapacity = 100_000;
        /** Entrada máxima por bloque de la cubierta (HV). */
        public long maxInsert = 512;
        public int tankBuckets = 64;
        public int fuelTankBuckets = 16;
        /** Como la cigüeña: 1 en N por tick de que suba un Oil Slime (0 = nunca). */
        public int oilSlimeChance = 9_000;
    }

    public static final class Tier {
        public long maxInsert;
        public long capacity;
        public int baseSide;
        public int maxSide;
        public int heads;
        public int maxBlocksPerSecond;

        Tier(long maxInsert, long capacity, int baseSide, int maxSide, int heads, int maxBlocksPerSecond) {
            this.maxInsert = maxInsert;
            this.capacity = capacity;
            this.baseSide = baseSide;
            this.maxSide = maxSide;
            this.heads = heads;
            this.maxBlocksPerSecond = maxBlocksPerSecond;
        }
    }

    public static QuarryConfig get() {
        return instance;
    }

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(Quarryindustrial.MOD_ID + ".json");
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                QuarryConfig read = GSON.fromJson(reader, QuarryConfig.class);
                if (read != null) {
                    instance = read;
                }
            } catch (IOException | JsonParseException e) {
                Quarryindustrial.LOGGER.error("No se pudo leer {}, uso los valores por defecto", path, e);
            }
        }
        try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(instance, writer);
        } catch (IOException e) {
            Quarryindustrial.LOGGER.warn("No se pudo escribir {}", path, e);
        }
    }

    private QuarryConfig() {
    }
}
