package mindustry.world.meta;

/** Stores special flags of blocks for easy querying. */
public enum BlockFlag{
    /** Enemy core; primary target for all units. */
    core,
    /** Rally point for units.*/
    rally,
    /** Producer of important goods. */
    producer,
    /** A turret. */
    turret,
    /** Only the command center block.*/
    comandCenter,
    /** Repair point. */
    repair,
    /** Upgrade pad. */
    mechPad,
    /** Scalables. */
    scalable,
    /** Upgradables. */
    upgradable,
    /** Graphite. */
    yoinkable,
    junction;

    public final static BlockFlag[] all = values();
}
