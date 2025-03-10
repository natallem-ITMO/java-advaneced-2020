package ru.ifmo.rain.lemeshkova.bank.common;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Account extends Remote {
    /**
     * Returns account identifier.
     */
    String getId() throws RemoteException;

    /**
     * Returns amount of money at the account.
     */
    int getAmount() throws RemoteException;

    /**
     * Sets amount of money at the account.
     */
    void setAmount(int amount) throws RemoteException;

    /**
     * Add amount of money to account and return previous amount
     */
    int addAmount(int amount) throws RemoteException;
}