package info.kgeorgiy.ja.garipov.student;

import info.kgeorgiy.java.advanced.student.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements AdvancedQuery {

    private static final Comparator<Student> STUDENT_BY_NAME_ORDER = Comparator.comparing(Student::getLastName,
            Comparator.reverseOrder()).thenComparing(Student::getFirstName, Comparator.reverseOrder()).
            thenComparing(Student::getId);

    private static final String EMPTY_STRING = "";

    // :NOTE: Обобщить
    private static <T, U> Predicate<U> FieldEqualsPredicate(final T fieldValue, final Function<U, T> getField) {
        return (obj -> getField.apply(obj).equals(fieldValue));
    }

    private static <T> List<T> mapToList(final List<Student> students, final Function<Student, T> getter) {
        return students.stream().map(getter).collect(Collectors.toList());
    }

    @Override
    public List<String> getFirstNames(final List<Student> students) {
        return mapToList(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(final List<Student> students) {
        return mapToList(students, Student::getLastName);
    }

    @Override
    public List<GroupName> getGroups(final List<Student> students) {
        return mapToList(students, Student::getGroup);
    }


    private static String getFullName(final Student student) {
        return student.getFirstName() + " " + student.getLastName();
    }

    @Override
    public List<String> getFullNames(final List<Student> students) {
        return mapToList(students, StudentDB::getFullName);
    }

    @Override
    public Set<String> getDistinctFirstNames(final List<Student> students) {
        return students.stream().map(Student::getFirstName).collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public String getMaxStudentFirstName(final List<Student> students) {
        return students.stream().max(Student::compareTo).map(Student::getFirstName).orElse(EMPTY_STRING);
    }

    private static List<Student> sortStudents(final Collection<Student> students,
                                              final Comparator<? super Student> comparator) {
        return students.stream().sorted(comparator).collect(Collectors.toList());
    }

    @Override
    public List<Student> sortStudentsById(final Collection<Student> students) {
        return sortStudents(students, Student::compareTo);
    }

    @Override
    public List<Student> sortStudentsByName(final Collection<Student> students) {
        return sortStudents(students, STUDENT_BY_NAME_ORDER);
    }

    private <T> Stream<Student> filterStudentsByFieldValue(final Collection<Student> students,
                                                           final Function<Student, T> getter, final T filteringValue) {
        return students.stream().filter(FieldEqualsPredicate(filteringValue, getter));
    }

    private <T> List<Student> findStudentByFieldValue(final Collection<Student> students,
                                                      final Function<Student, T> getter, final T filteringValue) {
        return filterStudentsByFieldValue(students, getter, filteringValue)
                .sorted(STUDENT_BY_NAME_ORDER).collect(Collectors.toList());
    }

    @Override
    public List<Student> findStudentsByFirstName(final Collection<Student> students, final String name) {
        return findStudentByFieldValue(students, Student::getFirstName, name);
    }

    @Override
    public List<Student> findStudentsByLastName(final Collection<Student> students, final String name) {
        return findStudentByFieldValue(students, Student::getLastName, name);
    }

    @Override
    public List<Student> findStudentsByGroup(final Collection<Student> students, final GroupName group) {
        return findStudentByFieldValue(students, Student::getGroup, group);
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(final Collection<Student> students, final GroupName group) {
        return filterStudentsByFieldValue(students, Student::getGroup, group)
                .collect(Collectors.toMap(
                        Student::getLastName,
                        Student::getFirstName,
                        BinaryOperator.minBy(Comparator.naturalOrder())
                ));
    }

    private static List<Group> getGroupsBy(final Collection<Student> students, final Comparator<? super Student> comparator) {
        return students.stream().sorted(comparator).
                collect(Collectors.groupingBy(
                        Student::getGroup,
                        Collectors.toList()))
                .entrySet().stream().map(entry -> new Group(entry.getKey(), entry.getValue())
                ).sorted(Comparator.comparing(Group::getName)).collect(Collectors.toList());
    }

    @Override
    public List<Group> getGroupsByName(final Collection<Student> students) {
        return getGroupsBy(students, STUDENT_BY_NAME_ORDER);
    }

    @Override
    public List<Group> getGroupsById(final Collection<Student> students) {
        return getGroupsBy(students, Student::compareTo);
    }
    // :NOTE: Обобщить
    private static <T extends Comparable<? super T>, U, S> T getAbstractMax(
            final Collection<S> collection, final Function<S, T> groupingMapping,
            final Function<S, U> valueExtractor, final Comparator<Map.Entry<T, Long>> sizeComparator,
            final T orElseValue
    ) {
        return collection.stream()
                .collect(Collectors.groupingBy(
                        groupingMapping,
                        Collectors.mapping(
                                valueExtractor,
                                Collectors.collectingAndThen(Collectors.toCollection(HashSet::new), set -> (long) set.size()))
                )).entrySet().stream().max(
                        Map.Entry.<T, Long>comparingByValue().thenComparing(sizeComparator)
                ).map(Map.Entry::getKey).orElse(orElseValue);
    }

    private static <U> GroupName getLargestAbstractGroup(final Collection<Student> students, final Function<Student, U> valueExtractor,
                                                         final Comparator<Map.Entry<GroupName, Long>> isSizesEqualComparator) {
        return getAbstractMax(students, Student::getGroup, valueExtractor, isSizesEqualComparator, null);
    }

    @Override
    public GroupName getLargestGroup(final Collection<Student> students) {
        return getLargestAbstractGroup(students, Function.identity(), Map.Entry.comparingByKey());
    }

    @Override
    public GroupName getLargestGroupFirstName(final Collection<Student> students) {
        return getLargestAbstractGroup(students, Student::getFirstName,
                Map.Entry.<GroupName, Long>comparingByKey().reversed());
    }

    @Override
    public String getMostPopularName(final Collection<Student> students) {
        return getAbstractMax(students, Student::getFirstName, Student::getGroup, Map.Entry.comparingByKey(),
                EMPTY_STRING);
    }

    private static <T> List<T> getByIndices(final Collection<Student> students, final int[] indices, final Function<Student, T> valueExtractor) {
        return Arrays.stream(indices).mapToObj(List.copyOf(students)::get).map(valueExtractor)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getFirstNames(final Collection<Student> students, final int[] indices) {
        return getByIndices(students, indices, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(final Collection<Student> students, final int[] indices) {
        return getByIndices(students, indices, Student::getLastName);
    }

    @Override
    public List<GroupName> getGroups(final Collection<Student> students, final int[] indices) {
        return getByIndices(students, indices, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(final Collection<Student> students, final int[] indices) {
        return getByIndices(students, indices, StudentDB::getFullName);
    }
}
