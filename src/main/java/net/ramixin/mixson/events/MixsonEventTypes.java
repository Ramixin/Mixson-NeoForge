package net.ramixin.mixson.events;

import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.ramixin.mixson.AssociatedMixsonEvent;
import net.ramixin.mixson.BuiltResourceReference;
import net.ramixin.mixson.MixsonError;

import java.util.HashMap;

public interface MixsonEventTypes {

    interface BaseEvent<T> {

        default int ordinal() {
            return -1;
        }

        String getName();

        T runEvent(AssociatedMixsonEvent event, JsonElement elem, HashMap<ResourceLocation, BuiltResourceReference> references);

    }

    interface Creation extends BaseEvent<JsonElement> {

        @Override
        default JsonElement runEvent(AssociatedMixsonEvent event, JsonElement elem, HashMap<ResourceLocation, BuiltResourceReference> references) {
            if(event.referenceIds().length == 0) {
                if(event.event() instanceof CreationEvent simpleEvent) return simpleEvent.run();
                else throw new MixsonError("Creation Events with no resource references must be of type CreationEvent");
            } else {
                if(event.event() instanceof AdvancedCreationEvent advancedEvent) {
                    return advancedEvent.run(references);
                }
                else throw new MixsonError("Creation Events with resource references must be of type AdvancedCreationEvent");
            }
        }

    }

    interface Deletion extends BaseEvent<Boolean> {

        @Override
        default Boolean runEvent(AssociatedMixsonEvent event, JsonElement elem, HashMap<ResourceLocation, BuiltResourceReference> references) {
            if(event.referenceIds().length == 0) {
                if(event.event() instanceof DeletionEvent simpleEvent) return simpleEvent.run();
                else throw new MixsonError("Deletion Events with no resource references must be of type DeletionEvent");
            } else {
                if(event.event() instanceof AdvancedDeletionEvent advancedEvent) {
                    return advancedEvent.run(references);
                }
                else throw new MixsonError("Deletion Events with resource references must be of type AdvancedDeletionEvent");
            }
        }
    }

    interface Modification extends BaseEvent<JsonElement> {

        @Override
        default JsonElement runEvent(AssociatedMixsonEvent event, JsonElement elem, HashMap<ResourceLocation, BuiltResourceReference> references) {
            if(event.referenceIds().length == 0) {
                if(event.event() instanceof ModificationEvent simpleEvent) return simpleEvent.run(elem);
                else throw new MixsonError("Modification Events with no resource references must be of type ModificationEvent");
            } else {
                if(event.event() instanceof AdvancedModificationEvent advancedEvent) {
                    return advancedEvent.run(elem, references);
                }
                else throw new MixsonError("Modification Events with resource references must be of type AdvancedModificationEvent");
            }
        }
    }


}
