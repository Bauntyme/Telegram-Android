package org.telegram.messenger;

import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.util.SparseLongArray;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Thread-safe dynamic per-account instance holder with lock-free reads and per-slot synchronized creation.
 */
public final class AccountInstanceMap<T> implements Iterable<T> {

    public interface IntFactory<T> {
        T create(int account);
    }

    private volatile AtomicReferenceArray<Object> items;
    private volatile AtomicReferenceArray<Object> locks;

    public AccountInstanceMap() {
        this(4);
    }

    public AccountInstanceMap(int initialCapacity) {
        int cap = Math.max(4, initialCapacity);
        items = new AtomicReferenceArray<>(cap);
        AtomicReferenceArray<Object> initLocks = new AtomicReferenceArray<>(cap);
        for (int i = 0; i < cap; i++) {
            initLocks.set(i, new Object());
        }
        locks = initLocks;
    }

    @SuppressWarnings("unchecked")
    public T getIfExists(int num) {
        if (num < 0) {
            return null;
        }
        AtomicReferenceArray<Object> arr = items;
        return num < arr.length() ? (T) arr.get(num) : null;
    }

    @SuppressWarnings("unchecked")
    public T getOrCreate(int num, IntFactory<T> factory) {
        if (num < 0) {
            num = 0;
        }
        AtomicReferenceArray<Object> arr = items;
        if (num < arr.length()) {
            Object existing = arr.get(num);
            if (existing != null) {
                return (T) existing;
            }
        }
        Object lock = getLock(num);
        synchronized (lock) {
            arr = items;
            if (num < arr.length()) {
                Object existing = arr.get(num);
                if (existing != null) {
                    return (T) existing;
                }
            }
            T created = factory.create(num);
            storeItem(num, created);
            return created;
        }
    }

    private synchronized Object getLock(int num) {
        AtomicReferenceArray<Object> l = locks;
        if (num >= l.length()) {
            int newCap = Math.max(num + 1, l.length() * 2);
            AtomicReferenceArray<Object> next = new AtomicReferenceArray<>(newCap);
            for (int i = 0; i < l.length(); i++) {
                next.set(i, l.get(i));
            }
            for (int i = l.length(); i < newCap; i++) {
                next.set(i, new Object());
            }
            locks = next;
            l = next;
        }
        Object lock = l.get(num);
        if (lock == null) {
            lock = new Object();
            l.set(num, lock);
        }
        return lock;
    }

    private synchronized void storeItem(int num, T value) {
        AtomicReferenceArray<Object> arr = items;
        if (num >= arr.length()) {
            int newCap = Math.max(num + 1, arr.length() * 2);
            AtomicReferenceArray<Object> next = new AtomicReferenceArray<>(newCap);
            for (int i = 0; i < arr.length(); i++) {
                next.set(i, arr.get(i));
            }
            next.set(num, value);
            items = next;
        } else {
            arr.set(num, value);
        }
    }

    /** Removes the instance at {@code num} only if it still references {@code expected}. */
    public boolean removeIfMatch(int num, T expected) {
        if (num < 0) {
            return false;
        }
        Object lock = getLock(num);
        synchronized (lock) {
            AtomicReferenceArray<Object> arr = items;
            if (num < arr.length() && arr.get(num) == expected) {
                arr.set(num, null);
                return true;
            }
            return false;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iterator<T> iterator() {
        AtomicReferenceArray<Object> arr = items;
        ArrayList<T> snapshot = new ArrayList<>(arr.length());
        for (int i = 0; i < arr.length(); i++) {
            T item = (T) arr.get(i);
            if (item != null) {
                snapshot.add(item);
            }
        }
        return snapshot.iterator();
    }

    public static final class BoolArray {
        private final SparseBooleanArray map = new SparseBooleanArray();

        public synchronized boolean get(int account) {
            return map.get(account, false);
        }

        public synchronized void set(int account, boolean value) {
            if (!value) {
                map.delete(account);
            } else {
                map.put(account, true);
            }
        }
    }

    public static final class IntArray {
        private final SparseIntArray map = new SparseIntArray();

        public synchronized int get(int account) {
            return map.get(account, 0);
        }

        public synchronized void set(int account, int value) {
            map.put(account, value);
        }
    }

    public static final class LongArray {
        private final SparseLongArray map = new SparseLongArray();

        public synchronized long get(int account) {
            return map.get(account, 0L);
        }

        public synchronized void set(int account, long value) {
            map.put(account, value);
        }
    }
}
