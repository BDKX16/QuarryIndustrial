package org.example.pz.quarryindustrial.item;

/** Qué hace cada upgrade y cuántos se pueden instalar a la vez en una quarry. */
public enum UpgradeType {
    AREA(3),
    SPEED(4),
    FORTUNE(3),
    SILK_TOUCH(1),
    VOID(1),
    /** Buscavetas: el cabezal solo rompe minerales (#c:ores) y deja la roca en su lugar. */
    ORE_SEEKER(1);

    private final int maxStack;

    UpgradeType(int maxStack) {
        this.maxStack = maxStack;
    }

    public int maxInstalled() {
        return maxStack;
    }

    /** Fortuna y seda se excluyen, igual que en un pico. */
    public boolean conflictsWith(UpgradeType other) {
        return (this == FORTUNE && other == SILK_TOUCH) || (this == SILK_TOUCH && other == FORTUNE);
    }
}
