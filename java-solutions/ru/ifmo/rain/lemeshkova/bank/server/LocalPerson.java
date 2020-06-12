package ru.ifmo.rain.lemeshkova.bank.server;

import ru.ifmo.rain.lemeshkova.bank.common.Account;

import java.util.Map;

public class LocalPerson extends AbstractPerson {

    Map<String, Account> accounts;

    public LocalPerson(String name, String surname, String passportId) {
        super(name, surname, passportId);
    }

    public LocalPerson(String name, String surname, String passportId, Map<String, Account> accounts) {
        super(name, surname, passportId);
        this.accounts = accounts;
    }

    @Override
    public Account getAccount(String subId) {
        return accounts.get(subId);
    }

    @Override
    public Account createAccount(String subId) {
        return accounts.computeIfAbsent(subId, (x) -> new CommonAccount(subId));
    }

    @Override
    protected String getTypeString() {
        return "local";
    }

    @Override
    protected String getAccountsString() {
        return getAccountsStringFromSet(accounts.keySet());
    }
}
