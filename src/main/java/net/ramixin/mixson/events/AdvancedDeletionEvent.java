package net.ramixin.mixson.events;

import net.minecraft.resources.ResourceLocation;
import net.ramixin.mixson.BuiltResourceReference;

import java.util.HashMap;

@FunctionalInterface
public interface AdvancedDeletionEvent extends MixsonEventTypes.Deletion {
    
    boolean run(HashMap<ResourceLocation, BuiltResourceReference> references);

    @Override
    default String getName() {
        return "Advanced Deletion Event";
    }
}
