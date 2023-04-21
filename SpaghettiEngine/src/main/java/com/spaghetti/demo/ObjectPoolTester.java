package com.spaghetti.demo;

import com.spaghetti.utils.Logger;
import com.spaghetti.utils.ObjectPool;
import com.spaghetti.utils.ThreadUtil;
import org.joml.Vector3f;

public class ObjectPoolTester {

    private static final int times = 100000000;

    public static void main(String[] args) {
        //useNew();
        //useLocalOnce();
        //useLocalAll();
        //useGlobalOnce();
        //useGlobalOnce();

        // For comparing memory usage
        while(true) {
            ThreadUtil.sleep(1);
        }
    }

    private static void useNew() {
        long time = 0;
        System.out.print("Use new: ");
        time = System.currentTimeMillis();
        for(int i = 0; i < times; i++) {
            Vector3f vec = new Vector3f();
            vec.zero();
        }
        printTime(System.currentTimeMillis() - time);
    }

    private static void useLocalOnce() {
        long time = 0;
        System.out.print("(LocalPool) Allocate 1 and deallocate 1: ");
        ObjectPool<Vector3f> localPool = new ObjectPool<>(Vector3f.class);
        time = System.currentTimeMillis();
        for(int i = 0; i < times; i++) {
            Vector3f vec = localPool.get();
            vec.zero();
            localPool.drop(vec);
        }
        printTime(System.currentTimeMillis() - time);
    }

    private static void useLocalAll() {
        long time = 0;
        System.out.print("(LocalPool) Allocate all and deallocate all: ");
        ObjectPool<Vector3f> localPool = new ObjectPool<>(Vector3f.class);
        Vector3f[] vecs = new Vector3f[localPool.getPoolSize()];
        time = System.currentTimeMillis();
        int poolCounter = 0;
        while(poolCounter < times) {
            for(int i = 0; i < localPool.getPoolSize(); i++) {
                vecs[i] = localPool.get();
                vecs[i].zero();
            }
            for(int i = localPool.getPoolSize() - 1; i > -1; i--) {
                localPool.drop(vecs[i]);
            }
            poolCounter += localPool.getPoolSize();
        }
        printTime(System.currentTimeMillis() - time);
    }

    public static void useGlobalAll() {
        long time = 0;
        System.out.print("(GlobalPool) Allocate 1 and deallocate 1: ");
        time = System.currentTimeMillis();
        for(int i = 0; i < times; i++) {
            Vector3f vec = ObjectPool.sget(Vector3f.class);
            vec.zero();
            ObjectPool.sdrop(Vector3f.class, vec);
        }
        printTime(System.currentTimeMillis() - time);
    }

    public static void useGlobalOnce() {
        long time = 0;
        System.out.print("(GlobalPool) Allocate all and deallocate all: ");
        int poolSize = ObjectPool.getOrCreate(Vector3f.class).getPoolSize();
        Vector3f[] vecs = new Vector3f[poolSize];
        time = System.currentTimeMillis();
        int poolCounter = 0;
        while(poolCounter < times) {
            for(int i = 0; i < poolSize; i++) {
                vecs[i] = ObjectPool.sget(Vector3f.class);
                vecs[i].zero();
            }
            for(int i = 0; i < poolSize; i++) {
                ObjectPool.sdrop(Vector3f.class, vecs[i]);
            }
            poolCounter += poolSize;
        }
        printTime(System.currentTimeMillis() - time);

    }

    private static void printTime(long time) {
        System.out.println("Took " + time + " ms (" + String.format("%.20f", ((float) time / (float) times)) + " ms avg)");
    }

}
