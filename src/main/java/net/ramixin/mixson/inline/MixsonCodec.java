package net.ramixin.mixson.inline;

import net.minecraft.server.packs.resources.Resource;
import net.ramixin.mixson.util.ResourceDeserializer;
import net.ramixin.mixson.util.ResourceSerializer;

import java.io.IOException;
import java.util.function.Function;

public interface MixsonCodec<T> {

    T deserialize(Resource resource) throws IOException;

    Resource serialize(Resource associatedResource, T file);

    String serializeOutputFile(T file);

    String extensionAndDot();


    static <T> MixsonCodec<T> of(String extension, ResourceDeserializer<T> deserializer, ResourceSerializer<T> serializer, Function<T, String> outputFileSerializer) {
        return new MixsonCodec<>() {
            @Override
            public T deserialize(Resource resource) throws IOException {
                return deserializer.deserialize(resource);
            }

            @Override
            public Resource serialize(Resource associatedResource, T file) {
                return serializer.serialize(associatedResource, file);
            }

            @Override
            public String serializeOutputFile(T file) {
                return outputFileSerializer.apply(file);
            }

            @Override
            public String extensionAndDot() {
                return '.'+extension;
            }
        };
    }
}
