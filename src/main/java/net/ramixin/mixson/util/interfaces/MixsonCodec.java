package net.ramixin.mixson.util.interfaces;

import net.minecraft.server.packs.resources.Resource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public interface MixsonCodec<T> {

    String extensionAndDot();

    T deserialize(Resource resource) throws IOException;

    Resource serialize(Resource associatedResource, T elem) throws IOException;

    ByteArrayOutputStream export(T resource) throws IOException;
}
