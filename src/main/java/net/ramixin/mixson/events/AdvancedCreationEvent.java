package net.ramixin.mixson.events;

import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.ramixin.mixson.BuiltResourceReference;

import java.util.HashMap;

@FunctionalInterface
public interface AdvancedCreationEvent extends MixsonEventTypes.Creation {

    JsonElement run(HashMap<ResourceLocation, BuiltResourceReference> references);

}
