package info.kgeorgiy.ja.garipov.arrayset;

import java.util.*;

public class ArraySet<T> extends AbstractSet<T> implements NavigableSet<T> {
    private static class ReversibleArrayList<T> extends AbstractList<T> implements RandomAccess {
        private final boolean reversed;
        private final List<T> array;

        public boolean isReversed() {
            return reversed;
        }

        public ReversibleArrayList() {
            reversed = false;
            array = new ArrayList<>();
        }

        public ReversibleArrayList(List<T> list) {
            reversed = false;
            array = list;
        }

        public ReversibleArrayList(ReversibleArrayList<T> reversibleArrayList, boolean reversed) {
            this.array = reversibleArrayList.array;
            this.reversed = reversed;
        }

        @Override
        public T get(int i) {
            return array.get(reversed ? size() - i - 1 : i);
        }

        @Override
        public int size() {
            return array.size();
        }
    }

    private final ReversibleArrayList<T> array;
    private final Comparator<? super T> comparator;

    public ArraySet() {
        array = new ReversibleArrayList<>();
        comparator = null;
    }

    public ArraySet(Comparator<? super T> comparator) {
        array = new ReversibleArrayList<>();
        this.comparator = comparator;
    }

    private ArraySet(ReversibleArrayList<T> reversibleArrayList, Comparator<? super T> comparator){
        this.array = reversibleArrayList;
        this.comparator = comparator;
    }

    public ArraySet(Collection<? extends T> collection) {
        this(collection, null);
    }

    public ArraySet(Collection<? extends T> collection, Comparator<? super T> comparator) {
        TreeSet<T> treeSet = new TreeSet<>(comparator);
        treeSet.addAll(collection);
        array = new ReversibleArrayList<>(new ArrayList<>(treeSet));
        this.comparator = comparator;
    }

    private int binarySearch(T t, boolean equalityRequired, boolean greater) {
        int index = Collections.binarySearch(array, t, comparator);
        if (index >= 0) {
            int addition = greater ? 1 : -1;
            index += equalityRequired ? 0 : addition;
            return index;
        } else {
            int addition = greater ? 0 : -1;
            index = -index - 1;
            index += addition;
        }
        return index;
    }

    private boolean checkIndexInBounds(int index) {
        return 0 <= index && index < array.size();
    }

    // max < t or null
    @Override
    public T lower(T t) {
        int index = binarySearch(t, false, false);
        return checkIndexInBounds(index) ? (array.get(index)) : null;
    }

    // max <= t or null
    @Override
    public T floor(T t) {
        int index = binarySearch(t, true, false);
        return checkIndexInBounds(index) ? (array.get(index)) : null;
    }

    // min > t or null
    @Override
    public T higher(T t) {
        int index = binarySearch(t, false, true);
        return checkIndexInBounds(index) ? (array.get(index)) : null;
    }

    // min >= t or null
    @Override
    public T ceiling(T t) {
        int index = binarySearch(t, true, true);
        return checkIndexInBounds(index) ? (array.get(index)) : null;
    }

    @Override
    public T pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public T pollLast() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<T> iterator() {
        return Collections.unmodifiableList(array).iterator();
    }

    @Override
    public ArraySet<T> descendingSet() {
        return new ArraySet<>(new ReversibleArrayList<>(array, !array.isReversed()), Collections.reverseOrder(comparator));
    }

    @Override
    public Iterator<T> descendingIterator() {
        return descendingSet().iterator();
    }

    @SuppressWarnings("unchecked")
    private int compare(T e1, T e2) {
        if (comparator == null) {
            return ((Comparable<T>) e1).compareTo(e2);
        } else {
            return comparator.compare(e1, e2);
        }
    }

    @Override
    public NavigableSet<T> subSet(T fromElement, boolean fromInclusive, T toElement, boolean toInclusive) {
        if (compare(fromElement, toElement) > 0) {
            throw new IllegalArgumentException();
        }
        if (compare(fromElement, toElement) == 0 && !(fromInclusive && toInclusive)) {
            return new ArraySet<>(comparator);
        } else {
            int fromIndex = Math.max(binarySearch(fromElement, fromInclusive, true), 0);
            int toIndex = Math.min(binarySearch(toElement, toInclusive, false) + 1, size());
            return new ArraySet<>(new ReversibleArrayList<>(array.array.subList(fromIndex, toIndex)), comparator);
        }
    }

    private NavigableSet<T> headOrTailSet(T element, boolean inclusive, boolean headSet) {
        if (size() == 0) {
            return new ArraySet<>(comparator);
        }
        int compare = headSet ? compare(first(), element) : compare(element, last());
        if (compare > 0 && inclusive || compare >= 0 && !inclusive) {
            return new ArraySet<>(comparator);
        } else {
            return headSet ? subSet(first(), true, element, inclusive) :
                    subSet(element, inclusive, last(), true);
        }
    }

    public NavigableSet<T> headSet(T toElement, boolean inclusive) {
        return headOrTailSet(toElement, inclusive, true);
    }

    public NavigableSet<T> tailSet(T fromElement, boolean inclusive) {
        return headOrTailSet(fromElement, inclusive, false);
    }

    @Override
    public Comparator<? super T> comparator() {
        return comparator;
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
    public T first() {
        if (size() == 0) {
            throw new NoSuchElementException("ArraySet is empty");
        }
        return array.get(0);
    }

    @Override
    public T last() {
        if (size() == 0) {
            throw new NoSuchElementException("ArraySet is empty");
        }
        return array.get(size() - 1);
    }

    @Override
    public int size() {
        return array.size();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(Object o) {
        return Collections.binarySearch(array, (T) o, comparator) >= 0;
    }

    // :NOTE-2: no need to override this
    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object cur : c) {
            if (!contains(cur)) {
                return false;
            }
        }
        return true;
    }
}
