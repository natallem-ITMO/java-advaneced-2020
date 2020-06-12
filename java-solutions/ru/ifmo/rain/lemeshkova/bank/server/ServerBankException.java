package ru.ifmo.rain.lemeshkova.bank.server;

public class ServerBankException extends Exception {
    public ServerBankException() {
        super();
    }

    public ServerBankException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServerBankException(String message) {
        super(message);
    }
}
