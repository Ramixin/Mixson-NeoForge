package net.ramixin.mixson;

public record HexRecord<A,B,C,D,E,F>(A first, B second, C third, D fourth, E fifth, F sixth) {

    public <R> R apply(HexFunction<A,B,C,D,E,F, R> function) {
        return function.apply(first, second, third, fourth, fifth, sixth);
    }

    @FunctionalInterface
    public interface HexFunction<A, B, C, D, E, F, R> {

        R apply(A a, B b, C c, D d, E e, F f);
    }
}
