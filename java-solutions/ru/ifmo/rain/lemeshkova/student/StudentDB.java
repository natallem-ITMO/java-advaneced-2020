package ru.ifmo.rain.lemeshkova.student;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;
import java.util.stream.*;

import info.kgeorgiy.java.advanced.student.AdvancedStudentGroupQuery;
import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.Student;

import static java.util.stream.Collectors.*;

public class StudentDB implements AdvancedStudentGroupQuery {

    private final Comparator<Map.Entry<String, List<Student>>> GROUP_SIZE_COMPARATOR = Comparator.comparingInt((Map.Entry<String, List<Student>> x) -> x.getValue().size()).
            reversed().thenComparing(Map.Entry::getKey);

    private final Comparator<AbstractMap.SimpleEntry<String, Long>> GROUP_DISTINCT_FIRST_NAME_SIZE_COMPARATOR =
            Comparator.comparingLong((ToLongFunction<AbstractMap.SimpleEntry<String, Long>>) AbstractMap.SimpleEntry::getValue).
                    reversed().thenComparing(AbstractMap.SimpleEntry::getKey);

    private final Comparator<Student> STUDENT_NAME_COMPARATOR = Comparator.comparing(Student::getLastName)
            .thenComparing(Student::getFirstName).thenComparing(Student::getId).thenComparing(Student::getGroup);

    @Override
    public List<Group> getGroupsByName(Collection<Student> students) {
        return sortStreamReturnAsList(createGroupStream(groupByGroup(students), STUDENT_NAME_COMPARATOR), Comparator.comparing(Group::getName));
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> students) {
        return sortStreamReturnAsList(createGroupStream(groupByGroup(students), Comparator.comparing(Student::getId)), Comparator.comparing(Group::getName));
    }

    @Override
    public String getLargestGroup(Collection<Student> students) {
        return getKeyFromOptional(groupByGroup(students).min(GROUP_SIZE_COMPARATOR));
    }

    @Override
    public String getLargestGroupFirstName(Collection<Student> students) {
        return groupByGroup(students).map
                (entry -> new AbstractMap.SimpleEntry<>(
                        entry.getKey(),
                        entry.getValue().stream().map(Student::getFirstName).distinct().count())).
                min(GROUP_DISTINCT_FIRST_NAME_SIZE_COMPARATOR).map(AbstractMap.SimpleEntry::getKey).orElse("");
    }

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return mapStudentsToStringAsList(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return mapStudentsToStringAsList(students, Student::getLastName);
    }

    @Override
    public List<String> getGroups(List<Student> students) {
        return mapStudentsToStringAsList(students, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return mapStudentsToStringAsList(students, this::getStudentFullName);
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return mapStudentsToStringsAsStream(students, Student::getFirstName).collect(toCollection(TreeSet::new));
    }

    @Override
    public String getMinStudentFirstName(List<Student> students) {
        return getValueOrDefault(streamOf(students).min(Comparator.comparingInt(Student::getId)), Student::getFirstName);
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return sortStreamReturnAsList(streamOf(students), Comparator.comparing(Student::getId));
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return sortStreamReturnAsList(streamOf(students), STUDENT_NAME_COMPARATOR);
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return sortStreamReturnAsList(filterStudentsAsStream(students, getPredicate(Student::getFirstName, name)), STUDENT_NAME_COMPARATOR);
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return sortStreamReturnAsList(filterStudentsAsStream(students, getPredicate(Student::getLastName, name)), STUDENT_NAME_COMPARATOR);
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, String group) {
        return sortStreamReturnAsList(filterStudentsAsStream(students, groupPredicate(group)), STUDENT_NAME_COMPARATOR);
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, String group) {
        return filterStudentsAsStream(students, groupPredicate(group)).collect(Collectors.toMap(Student::getLastName, Student::getFirstName, BinaryOperator.minBy(Comparator.naturalOrder())));
    }

    @Override
    public String getMostPopularName(Collection<Student> students) {
        return getKeyFromOptional(students.stream().collect(groupingBy(this::getStudentFullName,
                mapping(Student::getGroup, toSet()))).entrySet().stream().max(Comparator.comparing(
                (Map.Entry<String, Set<String>> x) -> x.getValue().size()).thenComparing(Map.Entry::getKey)));
    }

    @Override
    public List<String> getFirstNames(Collection<Student> students, int[] indices) {
        return getByIndices(new ArrayList<>(students), indices, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(Collection<Student> students, int[] indices) {
        return getByIndices(new ArrayList<>(students), indices, Student::getLastName);
    }

    @Override
    public List<String> getGroups(Collection<Student> students, int[] indices) {
        return getByIndices(new ArrayList<>(students), indices, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(Collection<Student> students, int[] indices) {
        return getByIndices(new ArrayList<>(students), indices, this::getStudentFullName);
    }

    private List<String> getByIndices(List<Student> students, int[] indices, Function<Student, String> functionToCollect) {
        return Arrays.stream(indices).mapToObj(i -> functionToCollect.apply(students.get(i))).collect(toList());
    }

    private String getStudentFullName(Student student) {
        return student.getFirstName() + " " + student.getLastName();
    }

    private Stream<Student> streamOf(Collection<Student> students) {
        return students.stream();
    }

    private Predicate<? super Student> getPredicate(Function<Student, String> studentMethod, String valueToCompare) {
        return (x -> studentMethod.apply(x).equals(valueToCompare));
    }

    private Predicate<? super Student> groupPredicate(String group) {
        return getPredicate(Student::getGroup, group);
    }

    private Stream<String> mapStudentsToStringsAsStream(Collection<Student> collection, Function<Student, String> function) {
        return collection.stream().map(function);
    }

    private List<String> mapStudentsToStringAsList(Collection<Student> collection, Function<Student, String> function) {
        return mapStudentsToStringsAsStream(collection, function).collect(Collectors.toList());
    }

    private Stream<Student> filterStudentsAsStream(Collection<Student> students, Predicate<? super Student> predicate) {
        return streamOf(students).filter(predicate);
    }

    private <T> List<T> sortStreamReturnAsList(Stream<T> stream, Comparator<T> comparator) {
        return stream.sorted(comparator).collect(toList());
    }

    private Stream<Map.Entry<String, List<Student>>> groupByGroup(Collection<Student> students) {
        return streamOf(students).collect(groupingBy(Student::getGroup)).entrySet().stream();
    }

    private Stream<Group> createGroupStream(Stream<Map.Entry<String, List<Student>>> stream, Comparator<Student> studentGroupComparator) {
        return stream.map((Map.Entry<String, List<Student>> x) ->
                new Group(x.getKey(), x.getValue().stream().sorted(studentGroupComparator).collect(toList())));
    }

    private <V> String getKeyFromOptional(Optional<Map.Entry<String, V>> optionalEntry) {
        return getValueOrDefault(optionalEntry, Map.Entry::getKey);
    }

    private <V> String getValueOrDefault(Optional<V> optional, Function<V, String> func) {
        return optional.map(func).orElse("");
    }
}