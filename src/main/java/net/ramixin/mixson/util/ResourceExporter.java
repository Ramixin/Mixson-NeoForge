package net.ramixin.mixson.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@FunctionalInterface
public interface ResourceExporter<T> {

    ByteArrayOutputStream export(T resource) throws IOException;

}
