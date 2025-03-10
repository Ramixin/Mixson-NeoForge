package net.ramixin.mixson.util;

@FunctionalInterface
public interface QuintConsumer<T, M, N, K, L> {

    void accept(T t, M m, N n, K k, L l);

}
