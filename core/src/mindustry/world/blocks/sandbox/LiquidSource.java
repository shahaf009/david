package mindustry.world.blocks.sandbox;

import arc.*;
import arc.graphics.g2d.*;
import arc.scene.ui.layout.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.ctype.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;

import static mindustry.Vars.content;

public class LiquidSource extends Block{
    public static Liquid lastLiquid;

    public LiquidSource(String name){
        super(name);
        update = true;
        solid = true;
        hasLiquids = true;
        liquidCapacity = 100f;
        configurable = true;
        outputsLiquid = true;
        hasThroughput = true;
        config(Liquid.class, (tile, l) -> ((LiquidSourceEntity)tile).source = l);
        configClear(tile -> ((LiquidSourceEntity)tile).source = null);
    }

    @Override
    public void setBars(){
        super.setBars();

        bars.remove("liquid");
    }

    @Override
    public void drawRequestConfig(BuildRequest req, Eachable<BuildRequest> list){
        drawRequestConfigCenter(req, (Content)req.config, "center");
    }

    public class LiquidSourceEntity extends TileEntity{
        public @Nullable Liquid source = null;

        @Override
        public void updateTile(){
            if(source == null){
                liquids.clear();
            }else{
                liquids.add(source, liquidCapacity);
                throughput.i += liquids().currentAmount();
                dumpLiquid(source);
                throughput.i -= liquids().currentAmount();
            }
        }

        @Override
        public void draw(){
            super.draw();

            if(source != null){
                Draw.color(source.color);
                Draw.rect("center", x, y);
                Draw.color();
            }
        }

        @Override
        public void buildConfiguration(Table table){
            ItemSelection.buildTable(table, content.liquids(), () -> source, liquid -> tile.configure(lastLiquid = liquid));
        }

        @Override
        public void playerPlaced(){
            if(lastLiquid != null){
                Core.app.post(() -> tile.configure(lastLiquid));
            }
        }

        @Override
        public Liquid config(){
            return source;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.b(source == null ? -1 : source.id);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            byte id = read.b();
            source = id == -1 ? null : content.liquid(id);
        }
    }
}
