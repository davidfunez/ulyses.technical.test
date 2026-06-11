package com.septeo.ulyses.technical.test.service.bestselling;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.ToLongFunction;

/**
 * Selects the top-K elements of a collection without using any built-in
 * sorting facility (no {@code Comparator}, no {@code Collections.sort},
 * no {@code Stream.sorted}).
 *
 * <p>Internally keeps a small sorted buffer of size {@code k}. Each candidate
 * is fast-rejected against the current minimum (threshold) once the buffer
 * is full, so the algorithm scales linearly with the input size for small
 * {@code k} values, which is the expected scenario (top 5).
 */
@UtilityClass
public final class TopKSelector {

    public static <T> List<T> of(Collection<T> items, int k, ToLongFunction<? super T> valueExtractor) {
        if (items == null || items.isEmpty() || k <= 0) {
            return List.of();
        }

        final RankedBuffer<T> buffer = new RankedBuffer<>(Math.min(k, items.size()));
        items.forEach(item -> buffer.offer(item, valueExtractor.applyAsLong(item)));
        return buffer.toList();
    }

    private static final class RankedBuffer<T> {

        private final Object[] items;
        private final long[] values;
        private final int capacity;
        private int size;
        private long threshold;

        RankedBuffer(int capacity) {
            this.capacity = capacity;
            this.items = new Object[capacity];
            this.values = new long[capacity];
            this.size = 0;
            this.threshold = Long.MIN_VALUE;
        }

        void offer(T item, long value) {
            if (isRejected(value)) {
                return;
            }
            final int position = findInsertPosition(value);
            shiftRight(position);
            place(item, value, position);
        }

        private boolean isRejected(long value) {
            return size == capacity && value <= threshold;
        }

        private int findInsertPosition(long value) {
            int pos = lastWritableIndex();
            while (pos > 0 && values[pos - 1] < value) {
                pos--;
            }
            return pos;
        }

        private void shiftRight(int fromPosition) {
            final int length = lastWritableIndex() - fromPosition;
            if (length > 0) {
                System.arraycopy(values, fromPosition, values, fromPosition + 1, length);
                System.arraycopy(items, fromPosition, items, fromPosition + 1, length);
            }
        }

        private void place(T item, long value, int position) {
            items[position] = item;
            values[position] = value;
            if (size < capacity) {
                size++;
            }
            if (size == capacity) {
                threshold = values[capacity - 1];
            }
        }

        private int lastWritableIndex() {
            return (size < capacity) ? size : capacity - 1;
        }

        @SuppressWarnings("unchecked")
        List<T> toList() {
            final List<T> result = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                result.add((T) items[i]);
            }
            return result;
        }
    }
}
