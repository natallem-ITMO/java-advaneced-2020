package ru.ifmo.rain.lemeshkova.bank.tests;

import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.ifmo.rain.lemeshkova.bank.common.Account;
import ru.ifmo.rain.lemeshkova.bank.common.Bank;
import ru.ifmo.rain.lemeshkova.bank.common.Person;
import ru.ifmo.rain.lemeshkova.bank.server.Server;
import ru.ifmo.rain.lemeshkova.bank.server.ServerBankException;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static junit.framework.TestCase.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ServerTests extends BaseTests {

    private static final String SURNAME_PATTERN = "surname";
    private static final String NAME_PATTERN = "name";
    private static final String PASSPORT_ID_PATTERN = "id";
    private static final String ACCOUNT_SUB_ID_PATTERN = "subId";

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
    public void test_emptyBank() throws RemoteException {
        assertNotNull("bank is not created", bank);
        for (Integer x : getRandomDistinctIntList(100)) {
            assertNull(bank.getRemotePerson(x + "$"));
            assertNull(bank.getLocalPerson(x + "$"));
        }
    }

    @Test
    public void test_nullInput() {
        assertThrows(NullPointerException.class, (() -> bank.createAccount(null, null)));
        assertThrows(NullPointerException.class, (() -> bank.createAccount("not_null", null)));
        assertThrows(NullPointerException.class, (() -> bank.createAccount(null, "not_null")));

        assertThrows(NullPointerException.class, (() -> bank.registerPerson(null, null, null)));
        assertThrows(NullPointerException.class, (() -> bank.registerPerson("not_null", null, null)));
        assertThrows(NullPointerException.class, (() -> bank.registerPerson(null, null, "not_null")));
        assertThrows(NullPointerException.class, (() -> bank.registerPerson(null, "not_null", null)));
    }

    @Test
    public void test_registration() throws RemoteException, ServerBankException {
        assertNotNull("bank is not created", bank);
        test_createPersons(getRandomDistinctIntList(50));
    }

    @Test
    public void test_registrationRewrite() throws RemoteException, ServerBankException {
        final int size = 100;
        List<Integer> randomDistinctIntList = getRandomDistinctIntList(size);
        test_createPersons(randomDistinctIntList);
        Stream.generate(() -> random(randomDistinctIntList)).limit(size).forEach((id) -> {
                    assertThrows(ServerBankException.class, () -> test_createPersonNoRewrite(
                            NAME_PATTERN + id, SURNAME_PATTERN + id,
                            NAME_PATTERN + (id + 1), SURNAME_PATTERN + (id + 1),
                            PASSPORT_ID_PATTERN + id));
                }
        );
    }

    @Test
    public void test_createAccounts() throws RemoteException, ServerBankException {
        final int personCount = 100;
        List<Integer> personIdentifierSeed = getRandomDistinctIntList(personCount);

        Stream.generate(() -> random(personIdentifierSeed)).limit(personCount).distinct().forEach((id) -> {
                    List<Integer> accountIdentifierSeeds = getRandomDistinctIntList(20);
                    for (var i : accountIdentifierSeeds) {
                        assertThrows(ServerBankException.class, () -> bank.createAccount(PASSPORT_ID_PATTERN + id, ACCOUNT_SUB_ID_PATTERN + i));
                    }
                }
        );

        test_createPersons(personIdentifierSeed);
        Stream.generate(() -> random(personIdentifierSeed)).limit(personCount).distinct().forEach((id) -> {
                    List<Integer> accountIdentifierSeeds = getRandomDistinctIntList(20);
                    for (var i : accountIdentifierSeeds) {
                        try {
                            createAccountAndCheck(PASSPORT_ID_PATTERN + id, ACCOUNT_SUB_ID_PATTERN + i);
                        } catch (RemoteException e) {
                            Assert.fail("Not expected RemoteException: " + e.getMessage());
                        }
                    }
                }
        );
    }

    @Test
    public void test_getter() throws RemoteException, ServerBankException {
        List<Integer> personIdentifierSeed = getRandomDistinctIntList(100);
        test_createPersons(personIdentifierSeed);
        for (var id : personIdentifierSeed) {
            test_getOnePerson(NAME_PATTERN + id, SURNAME_PATTERN + id, PASSPORT_ID_PATTERN + id);
        }
    }

    @Test
    public void test_MultithreadingRegistrationAndGetter() throws RemoteException {
        int threadCount = 100;
        int registrationThreadCount = 10;
        ExecutorService pool = new ThreadPoolExecutor(threadCount, threadCount, 0, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>());
        for (int i = 0; i < threadCount; i++) {
            int finalI = i;
            pool.submit(() -> {
                try {
                    for (int j = 0; j < registrationThreadCount; j++) {
                        test_registerOnePerson(NAME_PATTERN + finalI + "_" + j, SURNAME_PATTERN + finalI + "_" + j, PASSPORT_ID_PATTERN + finalI + "_" + j);
                    }
                } catch (RemoteException e) {
                    Assert.fail("Not expected RemoteException: " + e.getMessage());
                } catch (ServerBankException e) {
                    Assert.fail("Not expected SeverBankException: " + e.getMessage());
                }
            });
        }
        pool.shutdown();
        try {
            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Assert.fail("Not expected InterruptedException: " + e.getMessage());
        }
        for (int i = 0; i < threadCount; i++) {
            for (int j = 0; j < registrationThreadCount; j++) {
                String name = NAME_PATTERN + i + "_" + j;
                String surname = SURNAME_PATTERN + i + "_" + j;
                String id = PASSPORT_ID_PATTERN + i + "_" + j;
                Person actualLocalPerson = bank.getLocalPerson(id);
                Person actualRemotePerson = bank.getRemotePerson(id);
                PersonalIdentifiers expected = new PersonalIdentifiers(name, surname, id);
                assertTrue("Expected and actual local person not the same",
                        expected.equals(new PersonalIdentifiers(actualLocalPerson)));
                assertTrue("Expected and actual remote person not the same",
                        expected.equals(new PersonalIdentifiers(actualRemotePerson)));
            }
        }
    }

    @Test
    public void test_remoteAndLocalChangesIndependence() throws RemoteException, ServerBankException {
        int remoteAccountMoney = 100;

        Person person = test_registerOnePerson(NAME_PATTERN, SURNAME_PATTERN, PASSPORT_ID_PATTERN);
        Account remoteAccount1 = person.createAccount(ACCOUNT_SUB_ID_PATTERN);
        remoteAccount1.setAmount(remoteAccountMoney);

        List<Integer> moneyOperation = RANDOM.ints().distinct().limit(1000).filter(l -> l > 0 && l < 1000000).boxed().collect(Collectors.toList());

        for (var changeSum : moneyOperation) {
            Person remote = bank.getRemotePerson(PASSPORT_ID_PATTERN);
            Person local = bank.getLocalPerson(PASSPORT_ID_PATTERN);
            assertNotNull(remote);
            assertNotNull(remote);
            Account remoteAccount = remote.getAccount(ACCOUNT_SUB_ID_PATTERN);
            Account localAccount = local.getAccount(ACCOUNT_SUB_ID_PATTERN);
            assertNotNull(remoteAccount);
            assertNotNull(localAccount);

            remoteAccountMoney = test_remoteLocalAccountIndependence(remoteAccount, localAccount, changeSum, remoteAccountMoney);
        }
    }

    private Person test_registerOnePerson(String name, String surname, String passportId) throws RemoteException, ServerBankException {
        assertNull(bank.getRemotePerson(passportId));
        PersonalIdentifiers expected = new PersonalIdentifiers(name, surname, passportId);
        Person actual = bank.registerPerson(name, surname, passportId);
        assertTrue("Registered person not equals to passed identifiers", new PersonalIdentifiers(actual).equals(expected));
        return actual;
    }

    private void test_createPersons(List<Integer> ids) throws RemoteException, ServerBankException {
        for (Integer id : ids) {
            test_registerOnePerson(NAME_PATTERN + id, SURNAME_PATTERN + id, PASSPORT_ID_PATTERN + id);
        }
    }

    private void test_createPersonNoRewrite(final String prevName, final String prevSurname,
                                            final String newName, final String newSurname,
                                            final String passportId) throws RemoteException, ServerBankException {
        test_getOnePerson(prevName, prevSurname, passportId);
        bank.registerPerson(newName, newSurname, passportId);
        test_getOnePerson(prevName, prevSurname, passportId);
    }

    private void test_getOnePerson(String name, String surname, String passportId) throws RemoteException {
        Person actual = bank.getRemotePerson(passportId);
        assertNotNull(actual);
        PersonalIdentifiers expected = new PersonalIdentifiers(name, surname, passportId);
        assertTrue("Returned person not equals to passed identifiers", new PersonalIdentifiers(actual).equals(expected));
    }

    private void createAccountAndCheck(String passportId, String accountId) throws RemoteException {
        Person person = bank.getRemotePerson(passportId);
        assertNotNull(person);
        Account account = person.createAccount(accountId);
        checkAccountOperations(account);
        checkAccountOperations(bank.getLocalPerson(person.getPassportId()).getAccount(accountId));
    }

    private void checkAccountOperations(Account account) throws RemoteException {
        int initialSum = account.getAmount();
        List<Integer> moneyChanges = List.of(100, 200, -10, 400, -400, 0, 10, -300, 45467575, -40000000 * 2, 40000000);
        for (var i : moneyChanges) {
            account.setAmount(account.getAmount() + i);
            initialSum += i;
            assertEquals(account.getAmount(), initialSum);
        }
    }

    private int test_remoteLocalAccountIndependence(Account remoteAccount, Account localAccount, int changeSum, int remoteAccountMoney) throws RemoteException {
        assertEquals(remoteAccount.getAmount(), remoteAccountMoney);
        localAccount.setAmount(changeSum);
        assertEquals(localAccount.getAmount(), changeSum);
        assertEquals(remoteAccount.getAmount(), remoteAccountMoney);
        remoteAccount.setAmount(changeSum);
        remoteAccountMoney = changeSum;
        assertEquals(remoteAccount.getAmount(), remoteAccountMoney);
        return changeSum;
    }
}
