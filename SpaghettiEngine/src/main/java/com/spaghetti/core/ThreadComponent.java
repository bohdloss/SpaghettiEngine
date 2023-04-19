package com.spaghetti.core;

public interface ThreadComponent {

    void initialize(Game game) throws Throwable;
    void postInitialize() throws Throwable;
    void loop(float delta) throws Throwable;
    void preTerminate() throws Throwable;
    void terminate() throws Throwable;

}
