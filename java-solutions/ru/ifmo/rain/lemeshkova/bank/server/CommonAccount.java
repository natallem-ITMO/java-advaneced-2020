package ru.ifmo.rain.lemeshkova.bank.server;

import ru.ifmo.rain.lemeshkova.bank.common.Account;
import ru.ifmo.rain.lemeshkova.bank.common.Logger;

import java.io.Serializable;
import java.rmi.RemoteException;

public class CommonAccount implements Serializable, Account {

    private static final String loggerSuffix = "{CommonAccount}";
    private final String id;
    private int amount;

    public CommonAccount(final String id) {
        Logger.additionalInfo(loggerSuffix, "Creating account. Id: " + id);
        this.id = id;
        amount = 0;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public synchronized int getAmount() {
        Logger.additionalInfo(loggerSuffix, "Getting amount of money for accountId " + id);
        return amount;
    }

    @Override
    public synchronized void setAmount(final int amount) {
        Logger.additionalInfo(loggerSuffix, "Setting amount of money for accountId " + id);
        this.amount = amount;
    }
}
