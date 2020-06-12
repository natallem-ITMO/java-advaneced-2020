package ru.ifmo.rain.lemeshkova.bank.common;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Person extends Remote {
    /**
     * Getter for {@code name} field
     *
     * @return name of person
     * @throws RemoteException if any error occurs
     */
    String getName() throws RemoteException;

    /**
     * Getter for {@code surname} field
     *
     * @return surname of person
     * @throws RemoteException if any error occurs
     */
    String getSurname() throws RemoteException;

    /**
     * Getter for {@code passportId} field
     *
     * @return passportId of person
     * @throws RemoteException if any error occurs
     */
    String getPassportId() throws RemoteException;

    /**
     * Getter for account by specified subId
     *
     * @return account by subId of person if such account exists else return null
     * @throws RemoteException if any error occurs
     */
    Account getAccount(String subId) throws RemoteException;

    /**
     * Creating account with specified subId
     *
     * @param subId of created account
     * @return created account
     * @throws RemoteException if any error while exporting object occurs or bank cannot validate user
     */
    Account createAccount(String subId) throws RemoteException;

}