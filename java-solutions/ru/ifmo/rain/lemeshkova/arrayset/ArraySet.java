package ru.ifmo.rain.lemeshkova.arrayset;

import java.util.*;

public class ArraySet<T> extends AbstractSet<T> implements NavigableSet<T> {

    private ArraySet<T> descendingSet = null;
    private final Comparator<? super T> comparator;
    private final List<T> list;

    private ArraySet(List<T> list, Comparator<? super T> comparator) {
        this.comparator = comparator;
        this.list = list;
    }

    public ArraySet(Collection<? extends T> collection, Comparator<? super T> comparator) {
        this.comparator = comparator;
        if (isSorted(collection)) {
            list = List.copyOf(collection);
        } else {
            SortedSet<T> sortedSet = new TreeSet<>(comparator);
            sortedSet.addAll(collection);
            list = List.copyOf(sortedSet);
        }
    }

    public ArraySet(Collection<? extends T> collection) {
        this(collection, null);
    }

    public ArraySet() {
        this(Collections.emptyList());
    }

    @Override
    public T lower(T t) {
        return getValue(getIndex(t, true, true));
    }

    @Override
    public T floor(T t) {
        return getValue(getIndex(t, true, false));
    }

    @Override
    public T ceiling(T t) {
        return getValue(getIndex(t, false, false));
    }

    @Override
    public T higher(T t) {
        return getValue(getIndex(t, false, true));
    }

    @Override
    public NavigableSet<T> subSet(T fromElement, boolean fromInclusive, T toElement, boolean toInclusive) {
        if (compare(fromElement, toElement) > 0) throw new IllegalArgumentException();
        return new ArraySet<>(getSubList(getIndex(fromElement, false, !fromInclusive),
                getIndex(toElement, true, !toInclusive) + 1), comparator);
    }

    @Override
    public NavigableSet<T> headSet(T toElement, boolean toInclusive) {
        return new ArraySet<>(getSubList(0, getIndex(toElement, true, !toInclusive) + 1), comparator);
    }

    @Override
    public NavigableSet<T> tailSet(T fromElement, boolean toInclusive) {
        return new ArraySet<>(getSubList(getIndex(fromElement, false, !toInclusive), size()), comparator);
    }

    @Override
    public SortedSet<T> subSet(T fromElement, T toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<T> headSet(T toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<T> tailSet(T fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public Comparator<? super T> comparator() {
        return comparator;
    }

    @Override
    public Iterator<T> iterator() {
        return list.iterator();
    }

    @Override
    public T first() {
        if (isEmpty()) throw new NoSuchElementException("ArraySet is empty");
        return list.get(0);
    }

    @Override
    public T last() {
        if (isEmpty()) throw new NoSuchElementException("ArraySet is empty");
        return list.get(size() - 1);
    }

    @Override
    public NavigableSet<T> descendingSet() {
        if (descendingSet == null) {
            descendingSet = new ArraySet<>(new ReversedList<>(list), Collections.reverseOrder(comparator));
        }
        return descendingSet;
    }

    private int getIndex(T t) {
        return Collections.binarySearch(list, t, comparator);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean contains(Object o) {
        return getIndex((T) o) >= 0;
    }

    @Override
    public Iterator<T> descendingIterator() {
        return descendingSet().iterator();
    }

    private static class ReversedList<T> extends AbstractList<T> {

        private final List<T> reversedList;

        private boolean reversed;

        ReversedList(List<T> otherList) {
            if (otherList.getClass() == ReversedList.class) {
                ReversedList<T> els = (ReversedList<T>) otherList;
                this.reversedList = els.reversedList;
                this.reversed = !els.reversed;
            } else {
                this.reversedList = otherList;
                this.reversed = true;
            }
        }

        @Override
        public T get(int index) {
            if (reversed) {
                return reversedList.get(size() - index - 1);
            }
            return reversedList.get(index);
        }

        @Override
        public int size() {
            return reversedList.size();
        }


    }

    /*Unsupperted Operations*/

    @Override
    public T pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public T pollLast() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    private int getIndex(T t, boolean less, boolean strictly) {
        if (less && strictly) return getIndex(t, -1, -1);
        if (less) return getIndex(t, 0, -1);
        if (strictly) return getIndex(t, 1, 0);
        return getIndex(t, 0, 0);
    }

    private int getIndex(T t, int foundAdd, int notFoundAdd) {
        if (t == null) throw new NullPointerException();
        int index = getIndex(t);
        if (index < 0) {
            index = -index - 1 + notFoundAdd;
        } else {
            index += foundAdd;
        }
        return index;
    }

    private T getValue(int index) {
        if (inRange(index)) return list.get(index);
        return null;
    }

    private boolean inRange(int index) {
        return (index >= 0 && index < size());
    }

    private List<T> getSubList(int begin, int end) {//begin include, end exclude
        if (begin >= end || begin == -1 || end == -1) return List.of();
        return list.subList(begin, end);
    }

    @SuppressWarnings("unchecked")
    private int compare(T e1, T e2) {
        return comparator == null ? ((Comparable<? super T>) e1).compareTo(e2) : comparator.compare(e1, e2);
    }

    private boolean isSorted(Collection<? extends T> collection) {
        Iterator<? extends T> iterator = collection.iterator();
        if (iterator.hasNext()) {
            T previous;
            previous = iterator.next();
            while (iterator.hasNext()) {
                T current = iterator.next();
                if (compare(previous, current) >= 0) return false;
                previous = current;
            }
        }
        return true;
    }
}