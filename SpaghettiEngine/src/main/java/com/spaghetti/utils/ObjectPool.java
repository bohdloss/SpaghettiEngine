package com.spaghetti.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ObjectPool<T> {

    private static Map<Class<?>, ObjectPool<?>> pools = new HashMap<>();

    public static synchronized <T> ObjectPool<T> getOrCreate(Class<T> cls) {
        ObjectPool<T> pool = (ObjectPool<T>) pools.get(cls);
        if(pool == null) {
            pool = new ObjectPool(cls);
            pools.put(cls, pool);
        }
        return pool;
    }

    public static synchronized <T> T sget(Class<T> cls) {
        return getOrCreate(cls).get();
    }

    public static synchronized <T> void sdrop(Class<T> cls, T object) {
        getOrCreate(cls).drop(object);
    }

    protected final int poolSize;
    protected final List<ObjectEntry> objects;
    protected final Class<T> cls;
    protected int getPointer;
    protected int dropPointer;

    /**
     * Initializes a new Object pool for the given type.
     * Will only work on objects that properly implement hashCode()
     *
     * @param cls The class of the object
     */
    public ObjectPool(Class<T> cls) {
        this(10, cls);
    }

    public ObjectPool(int poolSize, Class<T> cls) {
        this.poolSize = poolSize;
        this.objects = new ArrayList<>(poolSize);
        this.cls = cls;
        for(int i = 0; i < poolSize; i++) {
            ObjectEntry entry = new ObjectEntry();
            try {
                entry.object = cls.newInstance();
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            this.objects.add(entry);
        }
    }

    /**
     * Attempts to borrow a temporary object of type T from the
     * object pool. In case all the objects are in use, this function will
     * return an object allocated with new
     *
     * @return The object
     */
    public synchronized T get() {
        int cycles = 0;
        while(true) {
            ObjectEntry entry = objects.get(getPointer++);
            if (getPointer == poolSize) {
                getPointer = 0;
            }

            if(!entry.inUse) {
                entry.inUse = true;
                return (T) entry.object;
            }

            cycles++;
            if(cycles >= poolSize) {
                try {
                    return cls.newInstance();
                } catch (InstantiationException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * Returns the object to the pool, for it to be used again.
     * If the object is not part of the object pool, nothing happens
     *
     * @param object The object to return
     */
    public synchronized void drop(T object) {
        int cycles = 0;
        while(true) {
            ObjectEntry entry = objects.get(dropPointer--);
            if (dropPointer == -1) {
                dropPointer = poolSize - 1;
            }

            if(entry.object == object) {
                entry.inUse = false;
                return;
            }

            cycles++;
            if(cycles >= poolSize) {
                return;
            }
        }
    }

    public int getPoolSize() {
        return poolSize;
    }

    private class ObjectEntry {
        Object object;
        boolean inUse;
    }

}
