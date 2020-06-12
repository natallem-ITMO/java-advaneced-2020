package ru.ifmo.rain.lemeshkova.bank.server;

import ru.ifmo.rain.lemeshkova.bank.common.Person;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Set;

abstract public class AbstractPerson implements Serializable, Person {

    private final String name;
    private final String surname;
    private final String passportId;

    public AbstractPerson(String name, String surname, String passportId) {
        this.name = name;
        this.surname = surname;
        this.passportId = passportId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getSurname() {
        return surname;
    }

    @Override
    public String getPassportId() {
        return passportId;
    }

    @Override
    public String toString() {
        return String.format("%s person. Name: %s; Surname: %s; PassportId: %s; Accounts: %s", getTypeString(), name, surname, passportId, getAccountsString());
    }

    protected abstract String getAccountsString();

    protected abstract String getTypeString();

    protected String getAccountsStringFromSet(Set<String> accounts) {
        if (accounts.isEmpty()) return "none";
        return String.join(", ", accounts);
    }
}
