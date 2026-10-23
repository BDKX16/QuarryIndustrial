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
