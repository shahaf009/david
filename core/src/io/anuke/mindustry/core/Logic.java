package io.anuke.mindustry.core;

import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.arc.ApplicationListener;
import io.anuke.arc.Events;
import io.anuke.arc.collection.ObjectSet;
import io.anuke.arc.collection.ObjectSet.ObjectSetIterator;
import io.anuke.arc.util.Structs;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.*;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.game.Teams.TeamData;
import io.anuke.mindustry.gen.BrokenBlock;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.BuildBlock;
import io.anuke.mindustry.world.blocks.BuildBlock.BuildEntity;
import io.anuke.mindustry.world.blocks.distribution.ItemsEater;

import static io.anuke.mindustry.Vars.*;

/**
 * Logic module.
 * Handles all logic for entities and waves.
 * Handles game state events.
 * Does not store any game state itself.
 * <p>
 * This class should <i>not</i> call any outside methods to change state of modules, but instead fire events.
 */
public class Logic implements ApplicationListener{

    public Logic(){
        Events.on(WaveEvent.class, event -> {
            if(world.isZone()){
                world.getZone().updateWave(state.wave);
            }
            for (Player p : playerGroup.all()) {
                p.respawns = state.rules.respawns;
            }
        });

        Events.on(BlockDestroyEvent.class, event -> {
            //blocks that get broken are appended to the team's broken block queue
            Tile tile = event.tile;
            Block block = tile.block();
            if(block instanceof BuildBlock){
                BuildEntity entity = tile.entity();

                //update block to reflect the fact that something was being constructed
                if(entity.cblock != null && entity.cblock.synthetic()){
                    block = entity.cblock;
                }else{
                    //otherwise this was a deconstruction that was interrupted, don't want to rebuild that
                    return;
                }
            }

            TeamData data = state.teams.get(tile.getTeam());
            data.brokenBlocks.addFirst(BrokenBlock.get(tile.x, tile.y, tile.rotation(), block.id));
        });
    }

    /** Handles the event of content being used by either the player or some block. */
    public void handleContent(UnlockableContent content){
        if(!headless){
            data.unlockContent(content);
        }
    }

    public void play(){
        state.set(State.playing);
        state.wavetime = state.rules.waveSpacing * 2; //grace period of 2x wave time before game starts
        Events.fire(new PlayEvent());

        //add starting items
        if(!world.isZone()){
            for(Team team : Team.all){
                if(state.teams.isActive(team)){
                    for(Tile core : state.teams.get(team).cores){
                        core.entity.items.add(Items.copper, 200);
                    }
                }
            }
        }
    }

    public void reset(){
        state.wave = 1;
        state.wavetime = state.rules.waveSpacing;
        state.gameOver = state.launched = false;
        state.teams = new Teams();
        state.rules = new Rules();
        state.stats = new Stats();
        state.eliminationtime = state.rules.eliminationTime;
        state.round = 1;
        state.pointsThreshold = state.rules.firstThreshold;
        Blocks.itemsEater.buildRequirements = ItemsEater.requirementsInRound[0];

        Time.clear();
        Entities.clear();
        TileEntity.sleepingEntities = 0;

        Events.fire(new ResetEvent());
    }

    public void runWave(){
        world.spawner.spawnEnemies();
        state.wave++;
        state.wavetime = world.isZone() && world.getZone().isBossWave(state.wave) ? state.rules.waveSpacing * state.rules.bossWaveMultiplier :
        world.isZone() && world.getZone().isLaunchWave(state.wave) ? state.rules.waveSpacing * state.rules.launchWaveMultiplier : state.rules.waveSpacing;

        Events.fire(new WaveEvent());
    }

    public void eliminateWeakest(){
        state.eliminationtime = state.rules.eliminationTime;
        state.round++;

        calcPoints();

        Call.eliminateTeam(state.getWeakest().ordinal());

        Call.onRound();
    }

    public void calcPoints(){
        for(Team team : Team.all){
            Teams.TeamData teamData = state.teams.get(team);
            int points = -1;
            if(teamData.cores.size!=0 || Structs.filter(Player.class, playerGroup.all().toArray(), (p)->p.getTeam()==team).length!=0){
                points = 0;
                for(Tile t : teamData.eaters){
                    points += calcPoints(t);
                }
            }
            state.points[team.ordinal()] = points;
        }
    }

    public int calcPoints(Tile t){
        int points = 0;
        for(int i=0; i<content.items().size; i++){
            points += (int)(t.entity.items.get(content.items().get(i)) * itemsValues[i]);
        }
        return points;
    }

    @Remote(called = Loc.both)
    public static void onRound(){
        //bump requirements
        Blocks.itemsEater.buildRequirements = ItemsEater.requirementsInRound[(state.round > ItemsEater.requirementsInRound.length) ?
                ItemsEater.requirementsInRound.length-1 : state.round -1];
        player.respawns = state.rules.respawns;
        state.pointsThreshold += state.rules.bumpThreshold;
    }

    @Remote(called = Loc.both)
    public static void eliminateTeam(int team){
        Team t = Team.all[team];
        //We need to copy set because when Core is destroyed it wants to remove itself from the original set and will occur error
        ObjectSet<Tile> cores = new ObjectSet<>(state.teams.get(t).cores);
        for(Tile tile : cores){
            world.tile(tile.pos()).block().onDestroyed(tile);
            world.removeBlock(tile);
        }
        if(player.getTeam() == t){
            for(Player p : playerGroup.all()){
                if(p.getTeam() == t){
                    p.kill();
                }
            }
        }

        Events.fire(new TeamEliminatedEvent(t));
    }

    private void checkGameOver(){
        if(!state.rules.attackMode && state.teams.get(defaultTeam).cores.size == 0 && !state.gameOver){
            state.gameOver = true;
            Events.fire(new GameOverEvent(waveTeam));
        }else if(state.rules.attackMode){
            Team alive = null;

            for(Team team : Team.all){
                if(state.teams.get(team).cores.size > 0){
                    if(alive != null){
                        return;
                    }
                    alive = team;
                }
            }

            if(alive != null && !state.gameOver){
                state.gameOver = true;
                Events.fire(new GameOverEvent(alive));
            }
        }
    }

    @Remote(called = Loc.both)
    public static void launchZone(){
        if(!headless){
            ui.hudfrag.showLaunch();
        }

        for(Tile tile : new ObjectSetIterator<>(state.teams.get(defaultTeam).cores)){
            Effects.effect(Fx.launch, tile);
        }

        Time.runTask(30f, () -> {
            for(Tile tile : new ObjectSetIterator<>(state.teams.get(defaultTeam).cores)){
                for(Item item : content.items()){
                    data.addItem(item, tile.entity.items.get(item));
                }
                world.removeBlock(tile);
            }
            state.launched = true;
        });
    }

    @Remote(called = Loc.both)
    public static void onGameOver(Team winner){
        state.stats.wavesLasted = state.wave;
        ui.restart.show(winner);
        netClient.setQuiet();
    }

    @Override
    public void update(){

        if(!state.is(State.menu)){

            if(!state.isPaused()){
                Time.update();

                if(state.rules.waves && state.rules.waveTimer && !state.gameOver){
                    if(!state.rules.waitForWaveToEnd || unitGroups[waveTeam.ordinal()].size() == 0){
                        state.wavetime = Math.max(state.wavetime - Time.delta(), 0);
                    }
                }

                if(state.rules.resourcesWar && !state.rules.rushGame && !state.gameOver){
                    state.eliminationtime = Math.max(state.eliminationtime - Time.delta(), 0);
                }

                if(!Net.client() && state.rules.resourcesWar){
                    calcPoints();
                }

                if(!Net.client() && state.rules.resourcesWar && state.rules.rushGame){
                    for(int i=0; i<state.points.length; i++){
                        if(state.points[i] >= state.pointsThreshold){
                            eliminateWeakest();
                        }
                    }
                }

                if(!Net.client() && state.wavetime <= 0 && state.rules.waves){
                    runWave();
                }

                if(!Net.client() && state.eliminationtime <=0 && state.rules.resourcesWar && !state.rules.rushGame){
                    eliminateWeakest();
                }

                if(!headless){
                    Entities.update(effectGroup);
                    Entities.update(groundEffectGroup);
                }

                if(!state.isEditor()){
                    for(EntityGroup group : unitGroups){
                        Entities.update(group);
                    }

                    Entities.update(puddleGroup);
                    Entities.update(shieldGroup);
                    Entities.update(bulletGroup);
                    Entities.update(tileGroup);
                    Entities.update(fireGroup);
                }else{
                    for(EntityGroup<?> group : unitGroups){
                        group.updateEvents();
                        collisions.updatePhysics(group);
                    }
                }


                Entities.update(playerGroup);

                //effect group only contains item transfers in the headless version, update it!
                if(headless){
                    Entities.update(effectGroup);
                }

                if(!state.isEditor()){

                    for(EntityGroup group : unitGroups){
                        if(group.isEmpty()) continue;
                        collisions.collideGroups(bulletGroup, group);
                    }

                    collisions.collideGroups(bulletGroup, playerGroup);
                }

                world.pathfinder.update();
            }

            if(!Net.client() && !world.isInvalidMap() && !state.isEditor()){
                checkGameOver();
            }
        }
    }
}
