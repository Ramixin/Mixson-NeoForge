package net.ramixin.mixson.util;

import net.minecraft.server.packs.resources.Resource;

import java.io.IOException;

public interface ResourceSerializer<T> {

    Resource serialize(Resource associatedResource, T elem) throws IOException;

}
