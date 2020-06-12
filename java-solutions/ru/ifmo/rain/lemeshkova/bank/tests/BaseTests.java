package ru.ifmo.rain.lemeshkova.bank.tests;

import ru.ifmo.rain.lemeshkova.bank.common.Person;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

abstract public class BaseTests {

    protected static final Random RANDOM = new Random(45454532049583L);

    protected static <T> T random(List<T> values) {
        return values.get(RANDOM.nextInt(values.size()));
    }

    static List<Integer> getRandomDistinctIntList(int size) {
        return RANDOM.ints().distinct().limit(size).filter(l -> l > 0).boxed().collect(Collectors.toList());
    }

    protected static class PersonalIdentifiers {

        private final String name;
        private final String surname;
        private final String passportId;

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
