package org.example.pz.quarryindustrial.block.quarry;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.base.FilteringStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.example.pz.quarryindustrial.config.QuarryConfig;
import org.example.pz.quarryindustrial.item.UpgradeItem;
import org.example.pz.quarryindustrial.item.UpgradeType;
import org.example.pz.quarryindustrial.registry.ModBlockEntities;
import org.example.pz.quarryindustrial.registry.ModBlocks;
import org.example.pz.quarryindustrial.registry.ModItems;
import org.example.pz.quarryindustrial.registry.ModTags;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.base.SimpleEnergyStorage;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Quarry por tiers. El área es un cuadrado detrás del bloque (del lado opuesto a su frente), rodeado por
 * un pórtico ({@link QuarryArea}). Primero construye el marco; después mina desde debajo del pórtico hasta
 * el fondo del mundo, repartiendo el área en franjas entre los cabezales.
 *
 * <p>La velocidad sale de la energía: cada tick suma su cuota de bloques al presupuesto (hasta el tope
 * del tier) y mina mientras quede presupuesto y el buffer pueda pagar el coste de cada bloque. Una quarry
 * mal alimentada simplemente mina más lento.
 *
 * <p>Del lado del cliente el block entity solo guarda lo que necesita el renderer: el área, los cursores
 * de los cabezales (sincronizados cada pocos ticks) y una posición visual interpolada para cada uno.
 */
public class QuarryBlockEntity extends BlockEntity implements ExtendedMenuProvider<BlockPos> {
    public static final int BUFFER_SLOTS = 27;
    /** Cuántos tipos de upgrade distintos entran a la vez (los "slots"). */
    public static final int UPGRADE_SLOTS = 4;
    private static final int PUSH_INTERVAL = 4;
    private static final int CHUNK_REFRESH_INTERVAL = 20;
    private static final int SYNC_INTERVAL = 4;
    private static final int STRUCTURE_SCAN_INTERVAL = 20;
    private static final Direction[] FLUID_CHECK = {Direction.NORTH, Direction.SOUTH, Direction.EAST,
            Direction.WEST, Direction.UP};

    /** Lo engancha el cliente para el sonido del motor; en el servidor queda vacío. */
    public static Consumer<QuarryBlockEntity> clientTickHook = quarry -> {
    };

    private final QuarryTier tier;
    public final SimpleEnergyStorage energy;
    private final SimpleContainer buffer = new SimpleContainer(BUFFER_SLOTS) {
        @Override
        public void setChanged() {
            super.setChanged();
            QuarryBlockEntity.this.setChanged();
        }
    };
    private final Storage<ItemVariant> bufferStorage = ContainerStorage.of(buffer, null);
    /** Solo extracción: las tuberías pueden sacar items, pero no meterlos en la quarry. */
    private final Storage<ItemVariant> exposedStorage = FilteringStorage.extractOnlyOf(bufferStorage);
    /** Drops que no entraron en el buffer. Mientras haya algo acá la quarry espera. */
    private final List<ItemStack> overflow = new ArrayList<>();
    /** Cantidad instalada de cada tipo, recalculada desde {@link #upgradeSlots} cada vez que cambia. */
    private final Map<UpgradeType, Integer> upgrades = new EnumMap<>(UpgradeType.class);
    /**
     * Los 4 slots de upgrades: cada slot tiene un solo tipo, apilado hasta su máximo
     * ({@link UpgradeType#maxInstalled()}). Se llenan desde la GUI o con clic derecho.
     */
    private final SimpleContainer upgradeSlots = new SimpleContainer(UPGRADE_SLOTS) {
        @Override
        public void setChanged() {
            super.setChanged();
            onUpgradesChanged();
        }
    };
    private final List<DrillHead> heads = new ArrayList<>();
    private final Set<Long> forcedChunks = new HashSet<>();
    /** Solo multibloque: puertos de energía encontrados en el último escaneo del cubo. */
    private List<BlockPos> energyPorts = List.of();
    private boolean structureFormed;
    /** EU/t que entran (por cable o por los puertos), promedio móvil de ~1 segundo. */
    private double averageInput;
    /** Solo multibloque: lo mismo, por puerto (en el orden de {@link #energyPorts}). */
    private double[] portAverages = new double[0];
    /** Energía al final del tick anterior: lo que haya de más al empezar el tick entró por cable. */
    private long energyAfterLastTick;

    private QuarryStatus status = QuarryStatus.IDLE;
    private @Nullable QuarryArea area;
    /** Cuántas posiciones de {@link QuarryArea#framePositions()} ya están resueltas. */
    private int frameProgress;
    private boolean frameBuilt;
    private double blockBudget;
    private boolean started;
    private @Nullable UUID owner;
    private int tickCounter;
    private boolean needsSync;

    // Solo cliente: posición visual de cada cabezal (x, y, z por cabezal) y giro del taladro
    private double[] visual = new double[0];
    private double[] previousVisual = new double[0];
    private float drillAngle;
    private float previousDrillAngle;

    public QuarryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.QUARRY, pos, state);
        this.tier = state.getBlock() instanceof QuarryBlock quarry ? quarry.tier() : QuarryTier.LV;
        QuarryConfig.Tier values = tier.settings();
        // capacidad, maxInsert, maxExtract (0 = solo consume, nadie le puede sacar energía)
        this.energy = new SimpleEnergyStorage(values.capacity, values.maxInsert, 0) {
            @Override
            protected void onFinalCommit() {
                setChanged();
            }
        };
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, QuarryBlockEntity quarry) {
        if (level instanceof ServerLevel serverLevel) {
            quarry.tick(serverLevel, pos, state);
        }
    }

    private void tick(ServerLevel level, BlockPos pos, BlockState state) {
        tickCounter++;
        if (!tier.multiblock()) {
            trackInput(Math.max(0, energy.amount - energyAfterLastTick));
        }
        try {
            tickBody(level, pos, state);
        } finally {
            energyAfterLastTick = energy.amount;
        }
    }

    private void trackInput(long received) {
        averageInput += (received - averageInput) * 0.05;
    }

    private void tickBody(ServerLevel level, BlockPos pos, BlockState state) {
        if (!started) {
            layout(pos, state);
        }
        if (tickCounter % PUSH_INTERVAL == 0) {
            pushToNeighbours(level, pos);
        }
        if (needsSync && tickCounter % SYNC_INTERVAL == 0) {
            needsSync = false;
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
        // Forzar los chunks desde el primer tick: si el jugador se aleja enseguida, la quarry no se queda dormida
        boolean working = !heads.isEmpty() && !heads.stream().allMatch(DrillHead::isFinished);
        if (working && (tickCounter % CHUNK_REFRESH_INTERVAL == 0 || forcedChunks.isEmpty())) {
            refreshChunks(level, pos);
        }
        if (tier.multiblock() && !tickStructure(level, pos, state)) {
            setStatus(QuarryStatus.STRUCTURE_INCOMPLETE);
            return;
        }

        if (heads.stream().allMatch(DrillHead::isFinished)) {
            setStatus(QuarryStatus.FINISHED);
            releaseChunks(level);
            return;
        }
        if (!flushOverflow()) {
            setStatus(QuarryStatus.OUTPUT_FULL);
            return;
        }
        if (!frameBuilt) {
            buildFrame(level);
            return;
        }

        QuarryConfig config = QuarryConfig.get();
        double perTick = maxBlocksPerSecond() / 20.0;
        // Sin acumular más de dos ticks: una quarry parada no tiene que "ponerse al día" de golpe
        blockBudget = Math.min(blockBudget + perTick, Math.max(1.0, perTick * 2));

        int mined = 0;
        int skips = 0;
        boolean starved = false;
        MiningContext context = null;

        int maxSkips = config.maxSkipsPerTick * Math.max(1, heads.size());
        // Ronda entre cabezales para que todos avancen parejo
        mining:
        while (blockBudget >= 1.0 && skips < maxSkips) {
            boolean progressed = false;
            for (DrillHead head : heads) {
                if (head.isFinished() || blockBudget < 1.0) {
                    continue;
                }
                BlockPos target = head.cursor();
                BlockState targetState = level.getBlockState(target);
                sealFluidsAround(level, target);
                if (!isMineable(level, target, targetState)) {
                    clearFluid(level, target, targetState);
                    head.advance(level.getMinY());
                    skips++;
                    progressed = true;
                    continue;
                }
                long cost = costFor(level, target, targetState);
                if (energy.amount < cost) {
                    starved = true;
                    break mining;
                }
                if (context == null) {
                    context = new MiningContext(fakePlayer(level), miningTool(level));
                }
                if (!tryBreak(level, target, targetState, context, cost, mined == 0)) {
                    // Un mod de claims dijo que no: lo saltamos sin cobrar
                    head.advance(level.getMinY());
                    skips++;
                    progressed = true;
                    continue;
                }
                head.advance(level.getMinY());
                blockBudget -= 1.0;
                mined++;
                progressed = true;
                if (!overflow.isEmpty()) {
                    break mining;
                }
            }
            if (!progressed) {
                break;
            }
        }

        if (mined > 0 || skips > 0) {
            needsSync = true;
        }
        if (mined > 0) {
            setStatus(QuarryStatus.MINING);
        } else if (starved) {
            setStatus(QuarryStatus.NO_POWER);
        }
        setChanged();
    }

    private record MiningContext(FakePlayer player, ItemStack tool) {
    }

    // ── Multibloque (quarry nuclear) ──

    /**
     * Revisa el cubo cada tanto y pasa la energía de los puertos al buffer del core. Los puertos siguen
     * cargándose aunque falte un bloque; lo único que se frena es el minado.
     */
    private boolean tickStructure(ServerLevel level, BlockPos pos, BlockState state) {
        QuarryConfig config = QuarryConfig.get();
        if (tickCounter % STRUCTURE_SCAN_INTERVAL == 1 || energyPorts.isEmpty()) {
            NuclearStructure.Scan scan = NuclearStructure.scan(level, pos, state.getValue(QuarryBlock.FACING));
            boolean formed = scan.formed(config.maxEnergyPorts);
            energyPorts = scan.ports();
            if (formed != structureFormed) {
                structureFormed = formed;
                needsSync = true;
            }
        }
        if (portAverages.length != energyPorts.size()) {
            portAverages = new double[energyPorts.size()];
        }
        long received = 0;
        for (int i = 0; i < energyPorts.size(); i++) {
            long taken = 0;
            long space = energy.getCapacity() - energy.amount;
            if (space > 0 && level.getBlockEntity(energyPorts.get(i)) instanceof EnergyPortBlockEntity port) {
                taken = port.drain(space);
                energy.amount += taken;
                received += taken;
            }
            portAverages[i] += (taken - portAverages[i]) * 0.05;
        }
        trackInput(received);
        return structureFormed;
    }

    public int energyPortCount() {
        return energyPorts.size();
    }

    public boolean structureFormed() {
        return structureFormed;
    }

    /** EU/t que están entrando (promedio). */
    public long averageInput() {
        return Math.round(averageInput);
    }

    public long portAverageInput(int port) {
        return port < portAverages.length ? Math.round(portAverages[port]) : 0;
    }

    /** Avance total de 0 a 1000: capas terminadas sobre el total desde el pórtico hasta el fondo del mundo. */
    public int progressPermille() {
        if (area == null || level == null || heads.isEmpty()) {
            return 0;
        }
        if (heads.stream().allMatch(DrillHead::isFinished)) {
            return 1000;
        }
        int total = area.startY() - level.getMinY() + 1;
        int done = area.startY() - currentY();
        return Math.clamp(done * 1000L / Math.max(1, total), 0, 1000);
    }

    // ── GUI ──

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayer player) {
        return worldPosition;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new QuarryMenu(containerId, inventory, this, menuData);
    }

    public SimpleContainer buffer() {
        return buffer;
    }

    /** Lo que ve la GUI. Los valores van como short por la red, así que los long viajan en mitades de 16 bits. */
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case QuarryMenu.ENERGY, QuarryMenu.ENERGY + 1 -> half(energy.amount, index - QuarryMenu.ENERGY);
                case QuarryMenu.CAPACITY, QuarryMenu.CAPACITY + 1 -> half(energy.getCapacity(), index - QuarryMenu.CAPACITY);
                case QuarryMenu.INPUT, QuarryMenu.INPUT + 1 -> half(averageInput(), index - QuarryMenu.INPUT);
                case QuarryMenu.STATUS -> status.ordinal();
                case QuarryMenu.LAYER -> currentY();
                case QuarryMenu.PROGRESS -> progressPermille();
                case QuarryMenu.PORTS -> energyPorts.size();
                case QuarryMenu.SIDE -> side();
                default -> index >= QuarryMenu.PORT_INPUTS && index < QuarryMenu.DATA_COUNT
                        ? (int) Math.min(0x7FFF, portAverageInput(index - QuarryMenu.PORT_INPUTS)) : 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Solo lectura del lado del servidor
        }

        @Override
        public int getCount() {
            return QuarryMenu.DATA_COUNT;
        }
    };

    private static int half(long value, int which) {
        return (int) ((value >>> (16 * which)) & 0xFFFF);
    }

    // ── Área, marco y cabezales ──

    /** Lado del cuadrado: base del tier, más un escalón por upgrade de área, sin pasar el máximo. */
    public int side() {
        QuarryConfig.Tier values = tier.settings();
        int side = values.baseSide + upgradeCount(UpgradeType.AREA) * QuarryConfig.get().areaUpgradeStep;
        return Math.min(side, values.maxSide);
    }

    private void layout(BlockPos pos, BlockState state) {
        heads.clear();
        int side = side();
        area = QuarryArea.behind(pos, state.getValue(QuarryBlock.FACING), side, QuarryConfig.get().frameHeight,
                tier.areaOffset());
        frameProgress = 0;
        frameBuilt = false;

        int count = Math.max(1, Math.min(tier.settings().heads, side));
        int stripWidth = side / count;
        for (int i = 0; i < count; i++) {
            int stripMin = area.minX() + i * stripWidth;
            int stripMax = i == count - 1 ? area.maxX() : stripMin + stripWidth - 1;
            heads.add(new DrillHead(stripMin, stripMax, area.minZ(), area.maxZ(), area.startY()));
        }
        started = true;
        needsSync = true;
        setChanged();
    }

    /**
     * Coloca los bloques de marco que falten, unos pocos por tick. Si una posición está ocupada por algo
     * que se puede minar, lo mina primero (cobrando como un bloque normal); si es irrompible, la salta.
     */
    private void buildFrame(ServerLevel level) {
        if (area == null) {
            return;
        }
        setStatus(QuarryStatus.BUILDING_FRAME);
        List<BlockPos> positions = area.framePositions();
        QuarryConfig config = QuarryConfig.get();
        int placed = 0;
        MiningContext context = null;
        while (frameProgress < positions.size() && placed < config.frameBlocksPerTick) {
            BlockPos target = positions.get(frameProgress);
            BlockState current = level.getBlockState(target);
            if (current.is(ModBlocks.QUARRY_FRAME)) {
                frameProgress++;
                continue;
            }
            long cost = config.frameCostPerBlock;
            boolean obstructed = !current.isAir() && !current.canBeReplaced();
            if (obstructed && !isMineable(level, target, current)) {
                frameProgress++;
                continue;
            }
            if (obstructed) {
                cost += costFor(level, target, current);
            }
            if (energy.amount < cost) {
                setStatus(QuarryStatus.NO_POWER);
                return;
            }
            if (obstructed) {
                if (context == null) {
                    context = new MiningContext(fakePlayer(level), miningTool(level));
                }
                if (!tryBreak(level, target, current, context, cost - config.frameCostPerBlock, placed == 0)) {
                    frameProgress++;
                    continue;
                }
            }
            energy.amount -= config.frameCostPerBlock;
            level.setBlock(target, ModBlocks.QUARRY_FRAME.connectedState(level, target), Block.UPDATE_ALL);
            frameProgress++;
            placed++;
        }
        if (frameProgress >= positions.size()) {
            frameBuilt = true;
            needsSync = true;
        }
        setChanged();
    }

    /** Saca el marco que esta quarry construyó (al romperla o al cambiar el tamaño del área). */
    private void removeFrame(Level level) {
        if (area == null) {
            return;
        }
        for (BlockPos framePos : area.framePositions()) {
            if (level.getBlockState(framePos).is(ModBlocks.QUARRY_FRAME)) {
                level.setBlock(framePos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    /** Vuelve a empezar desde arriba, por ejemplo al cambiar el tamaño del área. El aire se salta gratis. */
    public void restart() {
        if (level != null && !level.isClientSide()) {
            removeFrame(level);
        }
        started = false;
        heads.clear();
        area = null;
        setStatus(QuarryStatus.IDLE);
        needsSync = true;
        setChanged();
    }

    // ── Minado ──

    private boolean isMineable(ServerLevel level, BlockPos target, BlockState state) {
        if (state.isAir() || state.liquid()) {
            return false;
        }
        // Bedrock y bloques irrompibles tienen dureza negativa
        if (state.getDestroySpeed(level, target) < 0 || state.is(ModTags.QUARRY_BLACKLIST)) {
            return false;
        }
        if (target.equals(worldPosition)) {
            return false;
        }
        if (state.hasBlockEntity() && !QuarryConfig.get().mineContainers
                && level.getBlockEntity(target) instanceof Container) {
            return false;
        }
        return true;
    }

    /**
     * Los fluidos se borran sin actualizar vecinos, así no se pone a correr el resto del lago
     * mientras el cabezal pasa. No cuesta energía.
     */
    private void clearFluid(ServerLevel level, BlockPos target, BlockState state) {
        if (state.liquid()) {
            level.setBlock(target, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /**
     * Antes de tocar un bloque, mira sus vecinos de los costados y de arriba: el fluido que esté adentro del
     * área se borra ya (si no, correría hacia lo recién minado), y el que esté afuera se tapa con un dique
     * de adoquín. Sin esto, una quarry junto a un lago o al mar se inunda entera.
     */
    private void sealFluidsAround(ServerLevel level, BlockPos target) {
        if (area == null) {
            return;
        }
        for (Direction direction : FLUID_CHECK) {
            BlockPos neighbour = target.relative(direction);
            if (!level.isLoaded(neighbour) || level.getFluidState(neighbour).isEmpty()) {
                continue;
            }
            boolean inside = neighbour.getX() >= area.minX() && neighbour.getX() <= area.maxX()
                    && neighbour.getZ() >= area.minZ() && neighbour.getZ() <= area.maxZ();
            if (inside) {
                level.setBlock(neighbour, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            } else {
                level.setBlock(neighbour, Blocks.COBBLESTONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    public long costFor(ServerLevel level, BlockPos target, BlockState state) {
        QuarryConfig config = QuarryConfig.get();
        double hardness = Math.max(0, state.getDestroySpeed(level, target));
        double multiplier = Math.pow(config.speedUpgradeCostMultiplier, upgradeCount(UpgradeType.SPEED))
                * (1 + config.fortuneCostMultiplierPerLevel * upgradeCount(UpgradeType.FORTUNE))
                * (hasUpgrade(UpgradeType.SILK_TOUCH) ? config.silkTouchCostMultiplier : 1);
        return Math.max(1, Math.round(config.baseCostPerBlock * (1 + hardness) * multiplier));
    }

    /**
     * Rompe el bloque si los mods de claims lo permiten, cobra {@code cost} y guarda los drops.
     * Devuelve false (sin cobrar) si el evento lo canceló.
     */
    private boolean tryBreak(ServerLevel level, BlockPos target, BlockState state, MiningContext context,
                             long cost, boolean effects) {
        BlockEntity targetBe = level.getBlockEntity(target);
        if (!PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, context.player(), target, state,
                targetBe)) {
            return false;
        }
        energy.amount -= cost;
        List<ItemStack> drops = new ArrayList<>(Block.getDrops(state, level, target, targetBe, context.player(),
                context.tool()));
        if (targetBe instanceof Container container) {
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.removeItemNoUpdate(i);
                if (!stack.isEmpty()) {
                    drops.add(stack);
                }
            }
        }
        if (effects) {
            // Partículas y sonido de rotura, solo para el primer bloque del tick
            level.levelEvent(2001, target, Block.getId(state));
        }
        level.setBlock(target, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);

        boolean voiding = hasUpgrade(UpgradeType.VOID);
        for (ItemStack drop : drops) {
            if (voiding && drop.is(ModTags.VOID_FILTER)) {
                continue;
            }
            ItemStack rest = buffer.addItem(drop);
            if (!rest.isEmpty()) {
                overflow.add(rest);
            }
        }
        return true;
    }

    /** Pico virtual con los encantamientos de los upgrades instalados, para que las loot tables apliquen. */
    private ItemStack miningTool(ServerLevel level) {
        ItemStack tool = new ItemStack(Items.NETHERITE_PICKAXE);
        var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        int fortune = upgradeCount(UpgradeType.FORTUNE);
        if (fortune > 0) {
            tool.enchant(enchantments.getOrThrow(Enchantments.FORTUNE), fortune);
        } else if (hasUpgrade(UpgradeType.SILK_TOUCH)) {
            tool.enchant(enchantments.getOrThrow(Enchantments.SILK_TOUCH), 1);
        }
        return tool;
    }

    /** Rompemos como un jugador falso con el UUID del dueño, así los mods de claims pueden decidir. */
    private FakePlayer fakePlayer(ServerLevel level) {
        if (owner == null) {
            return FakePlayer.get(level);
        }
        return FakePlayer.get(level, new GameProfile(owner, "[Quarry]"));
    }

    private double maxBlocksPerSecond() {
        int speed = upgradeCount(UpgradeType.SPEED);
        return tier.settings().maxBlocksPerSecond * (1 + speed * QuarryConfig.get().speedUpgradePercent / 100.0);
    }

    // ── Salida de items ──

    /** Intenta meter el overflow en el buffer. Devuelve true si quedó vacío. */
    private boolean flushOverflow() {
        if (overflow.isEmpty()) {
            return true;
        }
        overflow.replaceAll(buffer::addItem);
        overflow.removeIf(ItemStack::isEmpty);
        setChanged();
        return overflow.isEmpty();
    }

    private void pushToNeighbours(ServerLevel level, BlockPos pos) {
        if (buffer.isEmpty()) {
            return;
        }
        for (Direction direction : Direction.values()) {
            Storage<ItemVariant> target = ItemStorage.SIDED.find(level, pos.relative(direction), direction.getOpposite());
            if (target != null && target.supportsInsertion()) {
                StorageUtil.move(bufferStorage, target, variant -> true, Long.MAX_VALUE, null);
                if (buffer.isEmpty()) {
                    return;
                }
            }
        }
    }

    public Storage<ItemVariant> itemStorage() {
        return exposedStorage;
    }

    /** Lo que hay que soltar al romper la quarry: el buffer y el overflow. */
    public List<ItemStack> contentsToDrop() {
        List<ItemStack> all = new ArrayList<>(buffer.removeAllItems());
        all.addAll(upgradeSlots.removeAllItems());
        all.addAll(overflow);
        overflow.clear();
        return all;
    }

    // ── Chunk loading ──

    private void refreshChunks(ServerLevel level, BlockPos pos) {
        if (!QuarryConfig.get().chunkLoading) {
            releaseChunks(level);
            return;
        }
        Set<Long> wanted = new HashSet<>();
        wanted.add(ChunkPos.pack(pos));
        for (DrillHead head : heads) {
            if (!head.isFinished()) {
                wanted.add(ChunkPos.pack(head.cursor()));
            }
        }
        for (Long chunk : Set.copyOf(forcedChunks)) {
            if (!wanted.contains(chunk)) {
                ChunkPos chunkPos = ChunkPos.unpack(chunk);
                level.setChunkForced(chunkPos.x(), chunkPos.z(), false);
                forcedChunks.remove(chunk);
            }
        }
        for (Long chunk : wanted) {
            if (forcedChunks.add(chunk)) {
                ChunkPos chunkPos = ChunkPos.unpack(chunk);
                level.setChunkForced(chunkPos.x(), chunkPos.z(), true);
            }
        }
        setChanged();
    }

    public void releaseChunks(ServerLevel level) {
        if (forcedChunks.isEmpty()) {
            return;
        }
        for (Long chunk : forcedChunks) {
            ChunkPos chunkPos = ChunkPos.unpack(chunk);
            level.setChunkForced(chunkPos.x(), chunkPos.z(), false);
        }
        forcedChunks.clear();
        setChanged();
    }

    // ── Upgrades ──

    public int upgradeCount(UpgradeType type) {
        return upgrades.getOrDefault(type, 0);
    }

    public boolean hasUpgrade(UpgradeType type) {
        return upgradeCount(type) > 0;
    }

    public SimpleContainer upgradeSlots() {
        return upgradeSlots;
    }

    /** Recalcula los conteos; si cambió el área, el marco viejo se saca y se vuelve a empezar. */
    private void onUpgradesChanged() {
        int previousArea = upgradeCount(UpgradeType.AREA);
        upgrades.clear();
        for (ItemStack stack : upgradeSlots.getItems()) {
            if (stack.getItem() instanceof UpgradeItem upgrade) {
                upgrades.merge(upgrade.type(), stack.getCount(), Integer::sum);
            }
        }
        if (upgradeCount(UpgradeType.AREA) != previousArea && started) {
            // restart() usa el área guardada, o sea la vieja: saca el marco correcto
            restart();
        }
        setChanged();
    }

    /**
     * Si {@code stack} puede ir en el slot {@code slot}: tiene que ser un upgrade que no esté ya en otro slot
     * y que no choque con los instalados (fortuna y seda se excluyen).
     */
    public boolean canPlaceUpgrade(int slot, ItemStack stack) {
        return canPlaceUpgrade(upgradeSlots, slot, stack);
    }

    /** Lo mismo sobre cualquier contenedor de 4 slots (el menú del cliente tiene su propia copia). */
    public static boolean canPlaceUpgrade(Container slots, int slot, ItemStack stack) {
        if (!(stack.getItem() instanceof UpgradeItem upgrade)) {
            return false;
        }
        for (int i = 0; i < UPGRADE_SLOTS; i++) {
            if (i == slot || !(slots.getItem(i).getItem() instanceof UpgradeItem other)) {
                continue;
            }
            if (other.type() == upgrade.type() || upgrade.type().conflictsWith(other.type())) {
                return false;
            }
        }
        return true;
    }

    /** Instala un upgrade si entra. Devuelve false si ya está al máximo, choca con otro o no hay slot libre. */
    public boolean installUpgrade(UpgradeType type) {
        ItemStack single = new ItemStack(ModItems.upgrade(type));
        for (int i = 0; i < UPGRADE_SLOTS; i++) {
            ItemStack current = upgradeSlots.getItem(i);
            if (current.is(single.getItem())) {
                if (current.getCount() >= type.maxInstalled()) {
                    return false;
                }
                current.grow(1);
                upgradeSlots.setChanged();
                return true;
            }
        }
        for (int i = 0; i < UPGRADE_SLOTS; i++) {
            if (upgradeSlots.getItem(i).isEmpty() && canPlaceUpgrade(i, single)) {
                upgradeSlots.setItem(i, single);
                return true;
            }
        }
        return false;
    }

    /** Saca un upgrade del último slot ocupado. Devuelve su tipo o null si no había. */
    public @Nullable UpgradeType removeLastUpgrade() {
        for (int i = UPGRADE_SLOTS - 1; i >= 0; i--) {
            ItemStack stack = upgradeSlots.getItem(i);
            if (stack.getItem() instanceof UpgradeItem upgrade) {
                stack.shrink(1);
                upgradeSlots.setChanged();
                return upgrade.type();
            }
        }
        return null;
    }

    public Map<UpgradeType, Integer> upgrades() {
        return Map.copyOf(upgrades);
    }

    // ── Estado ──

    private void setStatus(QuarryStatus status) {
        if (this.status != status) {
            this.status = status;
            needsSync = true;
            setChanged();
        }
    }

    public QuarryStatus status() {
        return status;
    }

    public QuarryTier tier() {
        return tier;
    }

    public @Nullable QuarryArea area() {
        return area;
    }

    public boolean frameBuilt() {
        return frameBuilt;
    }

    public List<DrillHead> heads() {
        return heads;
    }

    /** Capa más alta que todavía están minando los cabezales (para el mensaje de estado). */
    public int currentY() {
        return heads.stream().filter(head -> !head.isFinished()).mapToInt(DrillHead::y).max()
                .orElse(worldPosition.getY());
    }

    public void setOwner(@Nullable UUID owner) {
        this.owner = owner;
        setChanged();
    }

    // ── Cliente: interpolación para el renderer ──

    public static void clientTick(Level level, BlockPos pos, BlockState state, QuarryBlockEntity quarry) {
        quarry.tickVisuals();
        clientTickHook.accept(quarry);
    }

    /**
     * Acerca la posición visual de cada cabezal a su cursor. El servidor manda el cursor cada pocos ticks y
     * a veces salta varias filas de golpe; acá se recorre con una velocidad que crece con la distancia,
     * así el carro se ve deslizarse en vez de teletransportarse.
     */
    private void tickVisuals() {
        int n = heads.size();
        if (visual.length != n * 3) {
            visual = new double[n * 3];
            previousVisual = new double[n * 3];
            for (int i = 0; i < n; i++) {
                snapTo(i);
            }
        }
        System.arraycopy(visual, 0, previousVisual, 0, visual.length);
        for (int i = 0; i < n; i++) {
            DrillHead head = heads.get(i);
            double tx = head.cursor().getX() + 0.5;
            double ty = head.isFinished() ? head.y() + 1 : head.y();
            double tz = head.cursor().getZ() + 0.5;
            double dx = tx - visual[i * 3];
            double dy = ty - visual[i * 3 + 1];
            double dz = tz - visual[i * 3 + 2];
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > 48) {
                snapTo(i);
                System.arraycopy(visual, i * 3, previousVisual, i * 3, 3);
                continue;
            }
            double step = Math.max(0.15, dist * 0.25);
            if (dist > step) {
                visual[i * 3] += dx / dist * step;
                visual[i * 3 + 1] += dy / dist * step;
                visual[i * 3 + 2] += dz / dist * step;
            } else {
                visual[i * 3] = tx;
                visual[i * 3 + 1] = ty;
                visual[i * 3 + 2] = tz;
            }
        }
        previousDrillAngle = drillAngle;
        if (status.drilling()) {
            drillAngle += 0.45f;
            if (drillAngle > Math.PI * 2) {
                drillAngle -= (float) (Math.PI * 2);
                previousDrillAngle -= (float) (Math.PI * 2);
            }
        }
    }

    private void snapTo(int i) {
        DrillHead head = heads.get(i);
        visual[i * 3] = head.cursor().getX() + 0.5;
        visual[i * 3 + 1] = head.y();
        visual[i * 3 + 2] = head.cursor().getZ() + 0.5;
    }

    /** Posición visual del cabezal {@code i} (coordenadas de mundo), interpolada entre ticks. */
    public double visualX(int i, float partialTick) {
        return lerp(i * 3, partialTick);
    }

    public double visualY(int i, float partialTick) {
        return lerp(i * 3 + 1, partialTick);
    }

    public double visualZ(int i, float partialTick) {
        return lerp(i * 3 + 2, partialTick);
    }

    private double lerp(int index, float partialTick) {
        if (index >= visual.length) {
            return 0;
        }
        return previousVisual[index] + (visual[index] - previousVisual[index]) * partialTick;
    }

    public float drillAngle(float partialTick) {
        return previousDrillAngle + (drillAngle - previousDrillAngle) * partialTick;
    }

    public int visualHeadCount() {
        return visual.length / 3;
    }

    // ── Sincronización con el cliente ──

    /** Lo que el renderer necesita. Va en el paquete de actualización y también en el guardado normal. */
    private void writeSyncData(ValueOutput output) {
        output.putString("Status", status.name());
        output.putBoolean("Started", started);
        output.putBoolean("FrameBuilt", frameBuilt);
        if (area != null) {
            area.save(output);
        }
        ValueOutput.ValueOutputList headList = output.childrenList("Heads");
        for (DrillHead head : heads) {
            head.save(headList.addChild());
        }
    }

    private void readSyncData(ValueInput input) {
        try {
            status = QuarryStatus.valueOf(input.getStringOr("Status", QuarryStatus.IDLE.name()));
        } catch (IllegalArgumentException e) {
            status = QuarryStatus.IDLE;
        }
        started = input.getBooleanOr("Started", false);
        frameBuilt = input.getBooleanOr("FrameBuilt", false);
        area = QuarryArea.load(input);
        heads.clear();
        input.childrenListOrEmpty("Heads").stream().map(DrillHead::load).forEach(heads::add);
        if (heads.isEmpty() || area == null) {
            started = false;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        writeSyncData(output);
        return output.buildResult();
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Al romper la quarry: soltar el buffer, desarmar el marco y dejar de forzar chunks. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel serverLevel) {
            releaseChunks(serverLevel);
            removeFrame(serverLevel);
            Containers.dropContents(level, pos, NonNullList.of(ItemStack.EMPTY,
                    contentsToDrop().toArray(ItemStack[]::new)));
        }
        super.preRemoveSideEffects(pos, state);
    }

    // ── Persistencia ──

    private void loadUpgrades(ValueInput input) {
        List<ItemStack> slots = input.read("UpgradeSlots", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());
        for (int i = 0; i < UPGRADE_SLOTS; i++) {
            upgradeSlots.getItems().set(i, i < slots.size() ? slots.get(i) : ItemStack.EMPTY);
        }
        if (slots.isEmpty()) {
            // Mundos guardados antes de la GUI: los upgrades eran solo conteos por tipo
            ValueInput legacy = input.childOrEmpty("Upgrades");
            int slot = 0;
            for (UpgradeType type : UpgradeType.values()) {
                int count = Math.min(legacy.getIntOr(type.name(), 0), type.maxInstalled());
                if (count > 0 && slot < UPGRADE_SLOTS) {
                    upgradeSlots.getItems().set(slot++, new ItemStack(ModItems.upgrade(type), count));
                }
            }
        }
        // Sin pasar por setChanged(): cargar no tiene que reiniciar la quarry
        upgrades.clear();
        for (ItemStack stack : upgradeSlots.getItems()) {
            if (stack.getItem() instanceof UpgradeItem upgrade) {
                upgrades.merge(upgrade.type(), stack.getCount(), Integer::sum);
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        writeSyncData(output);
        output.putLong("Energy", energy.amount);
        output.putInt("FrameProgress", frameProgress);
        output.putDouble("Budget", blockBudget);
        if (owner != null) {
            output.store("Owner", UUIDUtil.CODEC, owner);
        }
        output.store("UpgradeSlots", ItemStack.OPTIONAL_CODEC.listOf(), upgradeSlots.getItems());
        output.store("Buffer", ItemStack.OPTIONAL_CODEC.listOf(), buffer.getItems());
        output.store("Overflow", ItemStack.CODEC.listOf(), List.copyOf(overflow));
        output.store("ForcedChunks", Codec.LONG.listOf(), List.copyOf(forcedChunks));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        readSyncData(input);
        energy.amount = Math.min(input.getLongOr("Energy", 0L), energy.getCapacity());
        frameProgress = input.getIntOr("FrameProgress", 0);
        blockBudget = input.getDoubleOr("Budget", 0);
        owner = input.read("Owner", UUIDUtil.CODEC).orElse(null);

        loadUpgrades(input);

        buffer.clearContent();
        List<ItemStack> items = input.read("Buffer", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());
        for (int i = 0; i < Math.min(items.size(), BUFFER_SLOTS); i++) {
            buffer.setItem(i, items.get(i));
        }
        overflow.clear();
        overflow.addAll(input.read("Overflow", ItemStack.CODEC.listOf()).orElse(List.of()));
        forcedChunks.clear();
        forcedChunks.addAll(input.read("ForcedChunks", Codec.LONG.listOf()).orElse(List.of()));
    }
}
