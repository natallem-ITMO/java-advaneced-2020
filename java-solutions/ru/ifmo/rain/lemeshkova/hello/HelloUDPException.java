package ru.ifmo.rain.lemeshkova.hello;

public class HelloUDPException extends RuntimeException {

    public HelloUDPException(String message) {
        super(message);
    }

    public HelloUDPException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
