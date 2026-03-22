package net.ramixin.mixson.util;

import net.minecraft.resources.Identifier;

import java.util.Objects;

@SuppressWarnings("ClassCanBeRecord") // NO IT CANNOT
public final class Index implements Comparable<Index> {

    private final Identifier id;
    private final int ordinal;

    public Index(Identifier id, int ordinal) {
        this.id = id;
        if(ordinal < -1) throw new IllegalArgumentException("Ordinal must be greater than or equal to -1");
        this.ordinal = ordinal;
    }

    public Index(String stringId, int ordinal) {
        this(Identifier.parse(stringId), ordinal);
    }

    public Index(Identifier id) {
        this(id, 0);
    }

    public Index(String stringId) {
        this(Identifier.parse(stringId), 0);
    }

    public Index withSuffixedId(String suffix) {
        return new Index(id.withSuffix(suffix), ordinal);
    }

    @Override
    public String toString() {
        return "Index{namespace=" +
                id.getNamespace() +
                ", path=" +
                id.getPath() +
                ", ordinal=" +
                ordinal +
                '}';
    }

    @Override
    public int compareTo(Index other) {
        int idCompare = this.id.compareTo(other.id);
        if (idCompare != 0)
            return idCompare;
        return Integer.compare(this.ordinal, other.ordinal);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Index index)) return false;
        return ordinal == index.ordinal && Objects.equals(id, index.id);
    }

    public boolean idEquals(Index other) {
        return this.id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, ordinal);
    }

    public Index copy() {
        return new Index(Identifier.parse(id.toString()), ordinal);
    }

    public Identifier id() {
        return id;
    }

    public int ordinal() {
        return ordinal;
    }
}
