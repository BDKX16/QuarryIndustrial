package org.example.pz.quarryindustrial.item;

import net.minecraft.world.item.Item;

/** Upgrade de quarry: se instala haciendo clic derecho sobre la quarry con el item en la mano. */
public class UpgradeItem extends Item {
    private final UpgradeType type;

    public UpgradeItem(UpgradeType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public UpgradeType type() {
        return type;
    }
}
