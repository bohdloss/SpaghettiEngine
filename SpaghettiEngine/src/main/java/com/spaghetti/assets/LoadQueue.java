package com.spaghetti.assets;

import com.spaghetti.utils.Logger;
import com.spaghetti.utils.ThreadUtil;

import java.util.ArrayList;
import java.util.List;

public class LoadQueue {

    protected static final long WAIT_BEFORE_QUIT = 10000;
    protected static final long CAN_STILL_SEND = WAIT_BEFORE_QUIT / 2;

    protected static final long SLEEP_TIME = 100;

    protected final AssetManager manager;
    protected LoaderThread thread;

    public LoadQueue(AssetManager manager) {
        this.manager = manager;
        thread = null;
    }

    public boolean isRunning() {
        return thread != null && thread.isAlive();
    }

    public synchronized void queueAsset(AssetEntry entry) {
        if(thread == null || !thread.isAlive() || thread.getRemainingTime() < CAN_STILL_SEND) {
            killThread();
            thread = new LoaderThread(manager);
            thread.queueAsset(entry);
            thread.start();
        } else {
            thread.queueAsset(entry);
        }
    }

    public void killThread() {
        if(thread == null) {
            return;
        }
        while(!thread.isIdle()) {
            ThreadUtil.sleep(1);
        }
        thread.setRemainingTime(0);
        while(thread.isAlive()) {
            ThreadUtil.sleep(1);
        }
    }

    private static class LoaderThread extends Thread {

        private final AssetManager manager;
        private final List<AssetEntry> toLoad = new ArrayList<>();
        private long remaining = WAIT_BEFORE_QUIT;
        private boolean idle;

        public LoaderThread(AssetManager manager) {
            this.manager = manager;
            setName("ASSET LOADER " + hashCode());
            manager.getGame().registerThread(this);
        }

        public boolean isIdle() {
            return idle;
        }

        public void queueAsset(AssetEntry entry) {
            Logger.debug("[loadAssetLazy] " + entry.name + " " + hashCode());
            toLoad.add(entry);
        }

        public long getRemainingTime() {
            return remaining;
        }

        public void setRemainingTime(long time) {
            this.remaining = time;
        }

        @Override
        public void run() {
            Logger.debug("Started");
            // We make sure the thread stays active for a while
            // even after finishing all the tasks
            // in case new assets are queued to it
            while(remaining > 0) {
                long time = System.currentTimeMillis();
                ThreadUtil.sleep(SLEEP_TIME);
                long delta = System.currentTimeMillis() - time;
                remaining -= delta;

                if (toLoad.size() > 0) {
                    idle = false;
                    doLoad();
                    remaining = WAIT_BEFORE_QUIT;
                } else {
                    idle = true;
                }
            }
            Logger.debug("Exiting");
            idle = true;
            manager.getGame().unregisterThread(this);
        }

        private void doLoad() {
            //ThreadUtil.sleep(1500);
            AssetEntry asset = toLoad.remove(0);

            synchronized (asset) {
                try {
                    // Fill asset
                    manager.fillAsset(asset);

                    // Perform native loading
                    manager.getGame().getPrimaryDispatcher().quickQueue(() -> {
                        asset.asset.load();
                        return null;
                    });
                    Logger.debug("[loadAssetLazy] " + asset.type + " loaded " + asset.name);
                } catch (Throwable t) {
                    Logger.error("[loadAssetLazy] Error loading " + asset.type + " " + asset.name, t);
                } finally {
                    asset.loading = false;
                }
            }
        }

    }

}
