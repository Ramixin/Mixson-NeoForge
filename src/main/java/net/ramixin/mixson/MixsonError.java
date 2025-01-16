package net.ramixin.mixson;

public class MixsonError extends RuntimeException {

    public MixsonError(String message) {
        super(message);
    }

    public MixsonError(Throwable cause) {
        super(cause);
    }

}
