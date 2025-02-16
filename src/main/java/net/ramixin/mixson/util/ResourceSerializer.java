package net.ramixin.mixson.util;

import net.minecraft.server.packs.resources.Resource;

public interface ResourceSerializer<T> {

    Resource serialize(Resource associatedResource, T elem);

}
