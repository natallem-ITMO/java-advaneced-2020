/*
package ru.ifmo.rain.lemeshkova.bank.tests;

import org.junit.*;
import ru.ifmo.rain.lemeshkova.bank.server.LocalPerson;
import ru.ifmo.rain.lemeshkova.bank.server.RemotePerson;
import ru.ifmo.rain.lemeshkova.bank.server.Server;
import ru.ifmo.rain.lemeshkova.bank.common.Account;
import ru.ifmo.rain.lemeshkova.bank.common.Bank;
import ru.ifmo.rain.lemeshkova.bank.common.Person;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static junit.framework.TestCase.*;

public class ClientTests {

    private static final String SURNAME_PATTERN = "surname";
    private static final String NAME_PATTERN = "name";
    private static final String ID_PATTERN = "id";
    private static Bank bank;

    private final String defaultName = "defaultName";
    private final String defaultSurname = "defaultSurname";
    private final String defaultPassportId = "defaultPassportId";

    private static final Random RANDOM = new Random(45454532049583L);

    @BeforeClass
    public static void before() throws Exception {
        Server.startServer();
        bank = (Bank) Naming.lookup("//localhost/bank");
    }

    @AfterClass
    public static void after() throws Exception {
        Server.stop();
    }

    @Test
    public void test_emptyBank() throws RemoteException {
        assertNotNull("bank is not created", bank);
        for (Integer x : RANDOM.ints().distinct().limit(100).boxed().collect(Collectors.toList())) {
            assertNull(bank.getRemotePerson(x + "$"));
            assertNull(bank.getLocalPerson(x + "$"));
        }
    }

    @Test
    public void test_registration() throws RemoteException {
        assertNotNull("bank is not created", bank);
        List<Integer> randomDistinctIntList = getRandomDistinctIntList(100);
        registeredId.addAll(randomDistinctIntList);
        test_createPersons(randomDistinctIntList);
    }

    private void test_createPersons(List<Integer> ids) throws RemoteException {
        for (Integer id : ids) {
            test_getPerson(id);
        }
    }

    private List<Integer> getRandomDistinctIntList(int size) {
        return RANDOM.ints().distinct().limit(size).filter(l -> l > 100000).boxed().collect(Collectors.toList());
    }

    private Person test_getPerson(Integer id) throws RemoteException {
        String name = NAME_PATTERN + id;
        String surname = SURNAME_PATTERN + id;
        String passportId = Integer.toString(id);
        assertNull(bank.getRemotePerson(passportId));
        PersonalIdentifiers expected = new PersonalIdentifiers(name, surname, passportId);
        Person actual = bank.registerPerson(name, surname, passportId);
        assertTrue("Registered person not equals to passed identifiers", new PersonalIdentifiers(actual).equals(expected));
        return actual;
    }

    @Test
    public void test_getter() throws RemoteException {
        for (var id : registeredId) {
            test_getOnePerson(id);
        }
    }

    private void test_getOnePerson(Integer id) throws RemoteException {
        String name = NAME_PATTERN + id;
        String surname = SURNAME_PATTERN + id;
        String passportId = Integer.toString(id);
        Person actual = bank.getRemotePerson(passportId);
        assertNotNull(actual);
        PersonalIdentifiers expected = new PersonalIdentifiers(name, surname, passportId);
        assertTrue("Returned person not equals to passed identifiers", new PersonalIdentifiers(actual).equals(expected));
    }

    @Test
    public void testMultithreadingRegistration() throws RemoteException {
        int threadCount = 100;
        int registrationThreadCount = 10;
        ExecutorService pool = new ThreadPoolExecutor(threadCount, threadCount, 0, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>());
        for (int i = 0; i < threadCount; i++) {
            int finalI = i;
            pool.submit(() -> {
                try {
                    registerInOneThread(finalI, registrationThreadCount);
                } catch (RemoteException e) {
                    Assert.fail("Not expected RemoteException: " + e.getMessage());
                }
            });
        }
        pool.shutdown();
        try {
            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignored) {
        }
        for (int i = 0; i < threadCount; i++) {
            for (int j = 0; j < registrationThreadCount; j++) {
                String name = NAME_PATTERN + i + "_" + j;
                String surname = SURNAME_PATTERN + i + "_" + j;
                String id = ID_PATTERN + i + "_" + j;
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

    private void registerInOneThread(int threadNumber, int registrationThreadCount) throws RemoteException {
        String name = NAME_PATTERN + threadNumber;
        String surname = SURNAME_PATTERN + threadNumber;
        String id = ID_PATTERN + threadNumber;
        for (int i = 0; i < registrationThreadCount; i++) {
            bank.registerPerson(name + "_" + i, surname + "_" + i, id + "_" + i);
        }
    }

    @Test
    public void testAccountActions() throws RemoteException {
        Person person = registerDefaultPerson();
        List<Integer> accountIds = getRandomDistinctIntList(100);
        for (var subId : accountIds) {

            createAccountAndCheck(person.getPassportId(), "1234");
            createAccountAndCheck(person.getPassportId(), "345");
        }
    }

    private static <T> T random(List<T> valuesArgs) {
        List<T> values = valuesArgs[RANDOM.nextInt(valuesArgs.length)];
        return values.get(RANDOM.nextInt(values.size()));
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

    @Test
    public void testMultithreadingAccountCreation() throws RemoteException {
        int personCount = 10;
        registerInOneThread(0, personCount);//creating 10 persons
        String id = "id0_";
        String subId = "subId_";
        int threadCount = 100;
        ExecutorService pool = new ThreadPoolExecutor(threadCount, threadCount, 0, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>());
        for (int i = 0; i < threadCount; i++) {
            int finalI = i;
            pool.submit(() -> {
                try {
                    createAccountsInOneThread(finalI, personCount, id, subId);
                } catch (RemoteException e) {
                    Assert.fail("Not expected RemoteException: " + e.getMessage());
                }
            });
        }
        pool.shutdown();
        try {
            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException ignored) {
        }
        for (int i = 0; i < personCount; i++) {
            Person person = bank.getRemotePerson(id + i);
            for (int j = 0; j < threadCount; j++) {
                Account actualAccount = person.getAccount(subId + j);
                assertNotNull(actualAccount);
            }
        }
    }

    void createAccountsInOneThread(int threadNumber, int personCount, String passportIdPattern, String prefixSubId) throws RemoteException {
        for (int i = 0; i < personCount; i++) {
            bank.getRemotePerson(passportIdPattern + i).createAccount(prefixSubId + threadNumber);
        }
    }

    @Test
    public void testRemoteAndLocalChanges() throws RemoteException {
        String accId = "accountId";
        int remoteAccountMoney = 100;
        int localAccountMoney = 300;

        Person person = registerDefaultPerson();
        Account remoteAccount1 = person.createAccount(accId);
        remoteAccount1.setAmount(remoteAccountMoney);
        Account localAccount = bank.getLocalPerson(defaultPassportId).getAccount(accId);
        localAccount.setAmount(localAccountMoney);
        Account remoteAccount2 = person.getAccount(accId);

        assertEquals(localAccountMoney, localAccount.getAmount());
        assertEquals(remoteAccountMoney, remoteAccount2.getAmount());
        assertEquals(remoteAccount1.getAmount(), remoteAccount2.getAmount());

        localAccount.setAmount(localAccountMoney * 2);
        remoteAccount2.setAmount(remoteAccountMoney * 2);

        assertEquals(2 * localAccountMoney, localAccount.getAmount());
        assertEquals(2 * remoteAccountMoney, remoteAccount2.getAmount());
        assertEquals(remoteAccount1.getAmount(), remoteAccount2.getAmount());
    }


    @Test
    public void testClient() throws RemoteException {
        int initMoney = 100;
        String defaultSubId = "123";
        Client.main(defaultName, defaultSurname, defaultPassportId, defaultSubId, Integer.toString(initMoney));
        Person expectedRemotePerson = new RemotePerson(defaultName, defaultSurname, defaultPassportId);
        Person actualRemotePerson = bank.getRemotePerson(defaultPassportId);
        assertEquals(expectedRemotePerson, actualRemotePerson);
        assertEquals(actualRemotePerson.getAccount(defaultSubId).getAmount(), initMoney);

        Client.main(defaultName, defaultSurname, defaultPassportId, defaultSubId, Integer.toString(initMoney * 2));
        actualRemotePerson = bank.getRemotePerson(defaultPassportId);
        assertEquals(expectedRemotePerson, actualRemotePerson);
        assertEquals(actualRemotePerson.getAccount(defaultSubId).getAmount(), 3 * initMoney);
    }


    private LocalPerson getDefaultLocalPerson() {
        return new LocalPerson(defaultName, defaultSurname, defaultPassportId, new ConcurrentHashMap<>());
    }


    private Person registerDefaultPerson() throws RemoteException {
        return bank.registerPerson(defaultName, defaultSurname, defaultPassportId);
    }


    private class PersonalIdentifiers {
        String name;
        String surname;
        String passportId;


        public PersonalIdentifiers(String name, String surname, String passportId) {
            this.name = name;
            this.surname = surname;
            this.passportId = passportId;
        }

        public PersonalIdentifiers(Person person) throws RemoteException {
            this.name = person.getName();
            this.surname = person.getSurname();
            this.passportId = person.getPassportId();
        }

        public String getName() {
            return name;
        }

        public String getSurname() {
            return surname;
        }

        public String getPassportId() {
            return passportId;
        }

        public List<String> getAllIdentifiers() {
            return List.of(name, surname, passportId);
        }

        @Override
        public String toString() {
            return "PersonalIdentifiers for " + name + surname + passportId;
        }

        public boolean equals(PersonalIdentifiers anotherIdentifiers) {
            return (getAllIdentifiers().equals(anotherIdentifiers.getAllIdentifiers()));
        }


        @Override
        public int hashCode() {
            return Objects.hash(name.hashCode(), surname.hashCode(), passportId);
        }
    }
}
*/
