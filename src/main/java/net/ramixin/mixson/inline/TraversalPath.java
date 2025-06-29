package net.ramixin.mixson.inline;

import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class TraversalPath {

    private final List<Function<JsonElement, JsonElement>> nodes = new ArrayList<>();

    public JsonElement apply(JsonElement root) {
        JsonElement val = root;
        for (Function<JsonElement, JsonElement> node : nodes) val = node.apply(val);
        return val;
    }

    public static TraversalPath create(String... path) {
        TraversalPath traversalPath = new TraversalPath();
        for(String val : path) {
            boolean object = val.startsWith("{");
            if(!object && !val.startsWith("[")) throw new IllegalArgumentException("Invalid traversal node: " + val+". Node must start with '{' or '['");
            String key = val.substring(1);
            if(object) {
                traversalPath.intoObject(key);
                continue;
            }
            try {
                int index = Integer.parseInt(key);
                traversalPath.intoArray(index);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid traversal node: " + val+". Array node must contain numbers >= 0 only");
            }
        }
        return traversalPath;
    }

    public TraversalPath intoObject(String objectKey) {
        nodes.add(root -> root.getAsJsonObject().get(objectKey));
        return this;
    }

    public TraversalPath intoArray(int index) {
        nodes.add(root -> root.getAsJsonArray().get(index));
        return this;
    }
}
