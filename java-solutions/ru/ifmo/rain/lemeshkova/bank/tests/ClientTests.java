package ru.ifmo.rain.lemeshkova.bank.tests;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.ifmo.rain.lemeshkova.bank.client.Client;
import ru.ifmo.rain.lemeshkova.bank.common.Account;
import ru.ifmo.rain.lemeshkova.bank.common.Bank;
import ru.ifmo.rain.lemeshkova.bank.common.Person;
import ru.ifmo.rain.lemeshkova.bank.server.Server;
import ru.ifmo.rain.lemeshkova.bank.server.ServerBankException;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static junit.framework.TestCase.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ClientTests extends BaseTests {

    private static final String NAME_PATTERN = "name";
    private static final String SURNAME_PATTERN = "surname";
    private static final String PASSPORT_ID_PATTERN = "id";
    private static final String ACCOUNT_SUBID_PATTERN = "subid";

    private static Bank bank;

    @BeforeEach
    public void before() throws Exception {
        Server.startServer();
        bank = (Bank) Naming.lookup("//localhost/bank");
    }

    @AfterEach
    public void after() throws Exception {
        Server.stop();
    }

    @Test
    public void test_emptyArgs() {
        if (!Client.CAN_USE_DEFAULT_ARGUMENTS) {
            assertThrows(IllegalArgumentException.class, () -> Client.main((String[]) null));
            assertThrows(IllegalArgumentException.class, Client::main);
        }
        List<String> args = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            args.add("1");
            assertThrows(IllegalArgumentException.class, () -> Client.main(args.toArray(new String[0])));
        }
        assertThrows(IllegalArgumentException.class, () -> Client.main("a", "a", "a", "a", "not_number"));
    }

    @Test
    public void test_registrationWithClient() throws RemoteException, ServerBankException {
        for (Integer id : getRandomDistinctIntList(50)) {
            test_registerOnePersonInClient(false, NAME_PATTERN + id, SURNAME_PATTERN + id,
                    PASSPORT_ID_PATTERN + id, ACCOUNT_SUBID_PATTERN + id, 100, 100);
        }
    }

    @Test
    public void test_ClientAddMoney() throws RemoteException, ServerBankException {
        int initMoney = 100;
        Integer curSum = initMoney;
        test_registerOnePersonInClient(false, NAME_PATTERN, SURNAME_PATTERN, PASSPORT_ID_PATTERN, ACCOUNT_SUBID_PATTERN, initMoney, curSum);
        List<Integer> moneyOperation = RANDOM.ints().distinct().limit(1000).filter(l -> l > 0 && l < 1000000).boxed().collect(Collectors.toList());
        for (var v : moneyOperation) {
            curSum += v;
            test_registerOnePersonInClient(true, NAME_PATTERN, SURNAME_PATTERN, PASSPORT_ID_PATTERN, ACCOUNT_SUBID_PATTERN, v, curSum);
        }
    }

    private void test_registerOnePersonInClient(boolean exists, String name, String surname, String passportId, String subId, int sumChange, int expectedSum) throws RemoteException, ServerBankException {
        Person actual = bank.getRemotePerson(passportId);
        if (!exists) {
            assertNull(actual);
        } else {
            assertNotNull(actual);
        }
        PersonalIdentifiers expected = new PersonalIdentifiers(name, surname, passportId);
        Client.main(name, surname, passportId, subId, Integer.toString(sumChange));
        if (!exists) actual = bank.getRemotePerson(passportId);
        assertTrue("Registered person not equals to passed identifiers", new PersonalIdentifiers(actual).equals(expected));
        Account account = actual.getAccount(subId);
        assertNotNull(account);
        assertEquals(account.getAmount(), expectedSum);
    }
}
