package net.ramixin.mixson.inline;

import net.minecraft.server.packs.resources.Resource;
import net.ramixin.mixson.util.ResourceDeserializer;
import net.ramixin.mixson.util.ResourceExporter;
import net.ramixin.mixson.util.ResourceSerializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.function.Function;

public interface MixsonCodec<T> {

    T deserialize(Resource resource) throws IOException;

    Resource serialize(Resource associatedResource, T file);

    ByteArrayOutputStream serializeOutputFile(T file) throws IOException;

    String extensionAndDot();

    @Deprecated(forRemoval = true)
    static <T> MixsonCodec<T> of(String extension, ResourceDeserializer<T> deserializer, ResourceSerializer<T> serializer, Function<T, String> outputFileSerializer) {
        return create(extension, deserializer, serializer, (item) -> {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                baos.write(outputFileSerializer.apply(item).getBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return baos;
        });
    }

    static <T> MixsonCodec<T> create(String extension, ResourceDeserializer<T> deserializer, ResourceSerializer<T> serializer, ResourceExporter<T> outputFileSerializer) {
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
            public ByteArrayOutputStream serializeOutputFile(T file) throws IOException {
                return outputFileSerializer.export(file);
            }

            @Override
            public String extensionAndDot() {
                return '.'+extension;
            }
        };
    }
}
