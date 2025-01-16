package net.ramixin.mixson.atp;

import oshi.util.tuples.Pair;

import java.util.ArrayList;
import java.util.List;

public class GeneratorContext {

    List<String> resourceIds = new ArrayList<>();
    List<String> eventIds = new ArrayList<>();

    public GeneratorContext() {}

    public void addFile(String resourceId, String eventId) {
        resourceIds.add(resourceId);
        eventIds.add(eventId);
    }

    public Pair<String[], String[]> collect() {
        return new Pair<>(resourceIds.toArray(String[]::new), eventIds.toArray(String[]::new));
    }
}
