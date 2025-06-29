package net.ramixin.mixson.util;

@FunctionalInterface
public interface QuintFunction<T, M, N, K, L, R> {

    R accept(T t, M m, N n, K k, L l);

}
