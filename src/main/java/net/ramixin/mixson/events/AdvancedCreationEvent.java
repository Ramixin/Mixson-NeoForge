package net.ramixin.mixson.events;

import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.ramixin.mixson.BuiltResourceReference;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

@FunctionalInterface
public interface AdvancedCreationEvent extends MixsonEventTypes.Creation {

    @Nullable JsonElement run(HashMap<ResourceLocation, BuiltResourceReference> references);

    @Override
    default String getName() {
        return "Advanced Creation Event";
    }
}
