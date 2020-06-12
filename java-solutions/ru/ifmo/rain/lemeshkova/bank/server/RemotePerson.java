package ru.ifmo.rain.lemeshkova.bank.server;

import ru.ifmo.rain.lemeshkova.bank.common.Account;
import ru.ifmo.rain.lemeshkova.bank.common.Bank;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RemotePerson extends AbstractPerson {

    private final Map<String, String> accountsSubId;//use map instead of ConcurrentHashSet
    private Bank bank = null;

    public RemotePerson(String name, String surname, String passportId) {
        super(name, surname, passportId);
        accountsSubId = new ConcurrentHashMap<>();
    }

    @Override
    public Account getAccount(String subId) throws RemoteException {
        return getCachedBank().getAccount(resolveAccountId(subId));
    }

    @Override
    public synchronized Account createAccount(String subId) throws RemoteException {
        Account account = getAccount(subId);
        if (account == null) {
            try {
                account = getCachedBank().createAccount(resolveAccountId(subId), getPassportId());
            } catch (ServerBankException e) {
                throw new RemoteException("Cannot create account for " + this + ", because such person not registered in bank");
            }
            accountsSubId.put(subId, subId);
        }
        return account;
    }

    public synchronized LocalPerson getLocalCopy() throws RemoteException {
        HashMap<String, Account> localAccounts = new HashMap<>();
        RemoteException exception = new RemoteException("Exception while creating copy of accounts");
        accountsSubId.forEach((subId, _subId) -> {
            try {
                Account account = new CommonAccount(subId);
                account.setAmount(getAccount(subId).getAmount());
                localAccounts.put(subId, account);
            } catch (RemoteException e) {
                exception.addSuppressed(e);
            }
        });
        if (exception.getSuppressed().length != 0) {
            throw exception;
        }
        return new LocalPerson(getName(), getSurname(), getPassportId(), localAccounts);
    }


    private Bank getCachedBank() throws RemoteException {
        if (bank == null) {
            try {
                bank = (Bank) Naming.lookup("//localhost/bank");
            } catch (NotBoundException | MalformedURLException cause) {
                RemoteException exception = new RemoteException();
                exception.addSuppressed(cause);
                throw exception;
            }
        }
        return bank;
    }

    @Override
    protected String getTypeString() {
        return "remote";
    }

    @Override
    protected String getAccountsString() {
        return getAccountsStringFromSet(accountsSubId.keySet());
    }

    private String resolveAccountId(String subId) {
        return getPassportId() + ":" + subId;
    }
}
