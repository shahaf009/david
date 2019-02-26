package io.anuke.mindustry.content;

import io.anuke.arc.graphics.Color;
import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemType;

public class Items implements ContentList{
    public static Item scrap, copper, lead, graphite, coal, titanium, thorium, silicon, plastanium, phasefabric, surgealloy,
    sporePod, sand, blastCompound, pyratite, metaglass;

    @Override
    public void load(){
        copper = new Item("copper", Color.valueOf("d99d73")){{
            type = ItemType.material;
            hardness = 1;
            cost = 0.6f;
            genOre = true;
            alwaysUnlocked = true;
        }};

        lead = new Item("lead", Color.valueOf("8c7fa9")){{
            type = ItemType.material;
            hardness = 1;
            cost = 0.9f;
            genOre = true;
        }};

        metaglass = new Item("metaglass", Color.valueOf("ebeef5")){{
            type = ItemType.material;
            cost = 2f;
        }};

        graphite = new Item("graphite", Color.valueOf("b2c6d2")){{
            type = ItemType.material;
            cost = 1.3f;
        }};

        coal = new Item("coal", Color.valueOf("272727")){{
            explosiveness = 0.4f;
            flammability = 1f;
            hardness = 2;
            genOre = true;
        }};

        titanium = new Item("titanium", Color.valueOf("8da1e3")){{
            type = ItemType.material;
            hardness = 3;
            cost = 1.1f;
            genOre = true;
        }};

        thorium = new Item("thorium", Color.valueOf("f9a3c7")){{
            type = ItemType.material;
            explosiveness = 0.2f;
            hardness = 4;
            radioactivity = 1f;
            cost = 1.4f;
            genOre = true;
        }};

        scrap = new Item("scrap", Color.valueOf("777777")){{
            genOre = true;
        }};

        silicon = new Item("silicon", Color.valueOf("53565c")){{
            type = ItemType.material;
            cost = 0.9f;
        }};

        plastanium = new Item("plastanium", Color.valueOf("cbd97f")){{
            type = ItemType.material;
            flammability = 0.2f;
            explosiveness = 0.2f;
            cost = 1.6f;
        }};

        phasefabric = new Item("phase-fabric", Color.valueOf("f4ba6e")){{
            type = ItemType.material;
            cost = 1.5f;
            radioactivity = 0.6f;
        }};

        surgealloy = new Item("surge-alloy", Color.valueOf("f3e979")){{
            type = ItemType.material;
        }};

        sporePod = new Item("spore-pod", Color.valueOf("7457ce")){{
            flammability = 1.05f;
        }};

        sand = new Item("sand", Color.valueOf("e3d39e")){{

        }};

        blastCompound = new Item("blast-compound", Color.valueOf("ff795e")){{
            flammability = 0.4f;
            explosiveness = 1.2f;
        }};

        pyratite = new Item("pyratite", Color.valueOf("ffaa5f")){{
            flammability = 1.4f;
            explosiveness = 0.4f;
        }};
    }
}
