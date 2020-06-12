package ru.ifmo.rain.lemeshkova.bank.common;

import ru.ifmo.rain.lemeshkova.bank.server.ServerBankException;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Bank extends Remote {
    /**
     * Creates account in bank database. Checking if person with passportId exists
     *
     * @param id         full id of creating account in format {@code passportId:subId}
     * @param passportId passport id of person, which account is register
     * @return remote account if person with such passportId exits, else null
     * @throws RemoteException if cannot export account object
     */
    Account createAccount(final String id, final String passportId) throws RemoteException, ServerBankException;

    /**
     * Return remote account by given full account id
     *
     * @param id full id of returning account in format {@code passportId:subId}
     * @return CommonAccount with specified id
     * @throws RemoteException if any Remote error happens
     */
    Account getAccount(String id) throws RemoteException;

    /**
     * Create remote person, register this person in bank database.
     * <p>
     * Check if person with given passportId is already exists and passed identifiers are equal to exist one.
     *
     * @param name       of registering person
     * @param surname    of registering person
     * @param passportId unique of registering person
     * @return created remote person
     * @throws RemoteException     if cannot export created person object
     * @throws ServerBankException if person with given passportId is already exists, his but name and surname not equals to passed
     */
    Person registerPerson(String name, String surname, String passportId) throws RemoteException, ServerBankException;

    /**
     * Return locale person by given passportId
     *
     * @param passportId of person to return
     * @return local copy of remote person with given id if such passportId is registered, else null
     * @throws RemoteException if any error while copying occurs
     */
    Person getLocalPerson(String passportId) throws RemoteException;

    /**
     * Return remote link for person by given passportId
     *
     * @param passportId of person to return
     * @return local copy of remote person with given id if such passportId is registered, else null
     * @throws RemoteException if any error while copying occurs
     */
    Person getRemotePerson(String passportId) throws RemoteException;
}
