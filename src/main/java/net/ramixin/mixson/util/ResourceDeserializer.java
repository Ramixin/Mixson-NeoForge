package net.ramixin.mixson.util;

import net.minecraft.server.packs.resources.Resource;

import java.io.IOException;

public interface ResourceDeserializer<T> {

    T deserialize(Resource resource) throws IOException;

}
