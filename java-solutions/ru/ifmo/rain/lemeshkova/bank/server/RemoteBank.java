package ru.ifmo.rain.lemeshkova.bank.server;


import ru.ifmo.rain.lemeshkova.bank.common.*;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class RemoteBank implements Bank {
    private final int port;
    private final Map<String, Account> accounts = new ConcurrentHashMap<>();
    private final Map<String, RemotePerson> persons = new ConcurrentHashMap<>();
    public static final String loggerSuffix = "{RemoteBank}";

    public RemoteBank(final int port) {
        this.port = port;
        Logger.info(loggerSuffix, "Created bank on port " + port);
    }

    public Account createAccount(final String accountId, final String passportId) throws RemoteException, ServerBankException {
        Objects.requireNonNull(accountId);
        synchronized (persons) {
            if (!persons.containsKey(passportId)) {
                throw new ServerBankException("Cannot create account: person with passport id " + passportId + " not exists.");
            }
            final Account account = new CommonAccount(accountId);
            synchronized (accounts) {
                if (accounts.putIfAbsent(accountId, account) == null) {
                    UnicastRemoteObject.exportObject(account, port);
                    Logger.info(loggerSuffix, "Successfully created new account. Id : " + accountId + " for person " + passportId);
                    return account;
                } else {
                    Logger.info(loggerSuffix, "Such account is already exists. Id : " + accountId + "; person " + passportId);
                    return getAccount(accountId);
                }
            }
        }
    }

    public Account getAccount(final String id) {
        Logger.info(loggerSuffix, "Return account. Id: " + id);
        return accounts.get(id);
    }

    @Override
    public Person registerPerson(String name, String surname, String passportId) throws RemoteException, ServerBankException {
        Objects.requireNonNull(name);
        Objects.requireNonNull(surname);
        Objects.requireNonNull(passportId);
        final RemotePerson person = new RemotePerson(name, surname, passportId);
        Person result = persons.putIfAbsent(passportId, person);
        if (result == null) {
            UnicastRemoteObject.exportObject(person, port);
            Logger.info(loggerSuffix, String.format("Register %s", person));
            return person;
        } else {
            Person remotePerson = getRemotePerson(passportId);
            if (!(remotePerson.getName().equals(name) && remotePerson.getSurname().equals(surname))) {
                throw new ServerBankException("Person already exists, but full names are different");
            }
            Logger.info(loggerSuffix, String.format("This registered person is already exists. Return %s", result));
            return getRemotePerson(passportId);
        }
    }

    @Override
    public Person getLocalPerson(String passportId) throws RemoteException {
        RemotePerson result = persons.get(passportId);
        if (result != null) {
            LocalPerson toReturn = result.getLocalCopy();
            Logger.info(loggerSuffix, String.format("Getting %s", toReturn));
            return toReturn;
        }
        return null;
    }

    @Override
    public Person getRemotePerson(String passportId) {
        RemotePerson result = persons.get(passportId);
        if (result != null) {
            Logger.info(loggerSuffix, String.format("Getting %s", result));
        }
        return result;
    }
}
