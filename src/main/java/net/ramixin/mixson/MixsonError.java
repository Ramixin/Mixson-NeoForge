package net.ramixin.mixson;

public class MixsonError extends RuntimeException {

  public MixsonError(String message, Object... args) {
        super(String.format(message, args));
  }

  public MixsonError(Throwable cause) {
      super(cause);
  }

}
