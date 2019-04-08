package io.anuke.mindustry.game;

import io.anuke.arc.Core;
import io.anuke.arc.Events;
import io.anuke.arc.collection.*;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.game.EventType.UnlockEvent;
import io.anuke.mindustry.type.*;

import static io.anuke.mindustry.Vars.content;
import static io.anuke.mindustry.Vars.state;

/** Stores player unlocks. Clientside only. */
public class GlobalData{
    private ObjectMap<ContentType, ObjectSet<String>> unlocked = new ObjectMap<>();
    private ObjectIntMap<Item> items = new ObjectIntMap<>();
    private boolean modified;

    public GlobalData(){
        Core.settings.setSerializer(ContentType.class, (stream, t) -> stream.writeInt(t.ordinal()), stream -> ContentType.values()[stream.readInt()]);
        Core.settings.setSerializer(Item.class, (stream, t) -> stream.writeUTF(t.name), stream -> content.getByName(ContentType.item, stream.readUTF()));

        Core.settings.setSerializer(ItemStack.class, (stream, t) -> {
            stream.writeUTF(t.item.name);
            stream.writeInt(t.amount);
        }, stream -> {
            String name = stream.readUTF();
            int amount = stream.readInt();
            return new ItemStack(content.getByName(ContentType.item, name), amount);
        });
    }

    public void modified(){
        modified = true;
    }

    public int getItem(Item item){
        return items.get(item, 0);
    }

    public void addItem(Item item, int amount){
        unlockContent(item);
        modified = true;
        items.getAndIncrement(item, 0, amount);
        state.stats.itemsDelivered.getAndIncrement(item, 0, amount);
    }

    public boolean hasItems(ItemStack[] stacks){
        for(ItemStack stack : stacks){
            if(items.get(stack.item, 0) < stack.amount){
                return false;
            }
        }
        return true;
    }

    public void removeItems(ItemStack[] stacks){
        for(ItemStack stack : stacks){
            items.getAndIncrement(stack.item, 0, -stack.amount);
        }
        modified = true;
    }

    public boolean has(Item item, int amount){
        return items.get(item, 0) >= amount;
    }

    public ObjectIntMap<Item> items(){
        return items;
    }

    /** Returns whether or not this piece of content is unlocked yet. */
    public boolean isUnlocked(UnlockableContent content){
        return content.alwaysUnlocked() || unlocked.getOr(content.getContentType(), ObjectSet::new).contains(content.name);
    }

    /**
     * Makes this piece of content 'unlocked', if possible.
     * If this piece of content is already unlocked or cannot be unlocked due to dependencies, nothing changes.
     * Results are not saved until you call {@link #save()}.
     */
    public void unlockContent(UnlockableContent content){
        if(content.alwaysUnlocked()) return;

        //fire unlock event so other classes can use it
        if(unlocked.getOr(content.getContentType(), ObjectSet::new).add(content.name)){
            modified = true;
            content.onUnlock();
            Events.fire(new UnlockEvent(content));
        }
    }

    /** Clears all unlocked content. Automatically saves. */
    public void reset(){
        save();
    }

    public void checkSave(){
        if(modified){
            save();
            modified = false;
        }
    }

    @SuppressWarnings("unchecked")
    public void load(){
        unlocked = Core.settings.getObject("unlocks", ObjectMap.class, ObjectMap::new);
        for(Item item : Vars.content.items()){
            items.put(item, Core.settings.getInt("item-" + item.name, 0));
        }

        //set up default values
        if(!Core.settings.has("item-" + Items.copper.name)){
            addItem(Items.copper, 50);
        }
    }

    public void save(){
        Core.settings.putObject("unlocks", unlocked);
        for(Item item : Vars.content.items()){
            Core.settings.put("item-" + item.name, items.get(item, 0));
        }
        Core.settings.save();
    }

}
