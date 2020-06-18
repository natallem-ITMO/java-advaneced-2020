package ru.ifmo.rain.lemeshkova.hello;

public class HelloUPDException extends RuntimeException {
    public HelloUPDException(String message) {
        super(message);
    }

    public HelloUPDException(String message, Throwable cause) {
        super(message, cause);
    }
}
