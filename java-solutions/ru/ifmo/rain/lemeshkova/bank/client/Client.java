package ru.ifmo.rain.lemeshkova.bank.client;

import ru.ifmo.rain.lemeshkova.bank.common.Account;
import ru.ifmo.rain.lemeshkova.bank.common.Bank;
import ru.ifmo.rain.lemeshkova.bank.common.Logger;
import ru.ifmo.rain.lemeshkova.bank.common.Person;
import ru.ifmo.rain.lemeshkova.bank.server.ServerBankException;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class Client {

    private static final String loggerSuffix = "{Client}";
    private static final String[] defaultArgs = {"defaultName", "defaultSurname", "defaultPassportId", "defaultSubId", "1000"};
    private static final String USAGE_LINE = "Usage: Client <name> <surname> <passportId> <subId> <changeSumInteger>";
    public static final boolean CAN_USE_DEFAULT_ARGUMENTS = true;

    public static void main(final String... args) throws RemoteException, ServerBankException {
        final Bank bank;

        try {
            try {
                bank = (Bank) Naming.lookup("//localhost/bank");
            } catch (final NotBoundException | MalformedURLException e) {
                throw new RemoteException("Cannot find bank in registry", e);
            }

            String[] usingArgs = args;
            if (args == null) {
                throw new IllegalArgumentException("Args for client cannot be null\n" + USAGE_LINE);
            }
            if (usingArgs.length < 5) {
                if (usingArgs.length==0 && CAN_USE_DEFAULT_ARGUMENTS) {
                    Logger.info("Using default arguments.");
                    usingArgs = defaultArgs;
                } else {
                    throw new IllegalArgumentException("Incorrect args number for Client\n" + USAGE_LINE);
                }
            }

            String name = usingArgs[0];
            String surname = usingArgs[1];
            String passportId = usingArgs[2];
            String subId = usingArgs[3];
            int sumChange;
            try {
                sumChange = Integer.parseInt(usingArgs[4]);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Incorrect 5th argument. Expected: integer, found:" + usingArgs[3], e);
            }

            Person person = tryToRegister(bank, name, surname, passportId);
            Account account = getAccountFrom(person, subId);
            addMoneyAndShowResult(sumChange, person, account);
        } catch (IllegalArgumentException | RemoteException | ServerBankException e) {
            Logger.error(e);
            throw e;
        }
    }

    private static Person tryToRegister(Bank bank, String name, String surname, String passportId) throws RemoteException, ServerBankException {
        Person person = bank.getRemotePerson(passportId);
        if (person == null) {
            person = bank.registerPerson(name, surname, passportId);
            Logger.info(loggerSuffix, String.format("Registered new person with passportId %s. Full name: %s %s",
                    person.getPassportId(), person.getName(), person.getSurname()));
        } else if (!person.getName().equals(name) || !person.getSurname().equals(surname)) {
            throw new IllegalArgumentException("Person with passportId " + passportId + " is already exists. " +
                    "Args name doesn't match remote person name");
        }
        return person;
    }

    private static Account getAccountFrom(Person person, String subId) throws RemoteException, ServerBankException {
        Account account = person.getAccount(subId);
        if (account == null) {
            account = person.createAccount(subId);
            Logger.info(loggerSuffix, String.format("Created account for person: %s %s. AccountId: %s with sum %d",
                    person.getName(), person.getSurname(), account.getId(), account.getAmount()));
        }
        return account;
    }

    public static void addMoneyAndShowResult(int addSum, Person person, Account account) throws RemoteException {
        int previousAmount = account.getAmount();
        account.setAmount(account.getAmount() + addSum);
        Logger.info(loggerSuffix, String.format("Change money amount for person with full name: %s %s. Account id: %s with sum: %d(previous: %d)",
                person.getName(), person.getSurname(), account.getId(), account.getAmount(), previousAmount));
    }
}
