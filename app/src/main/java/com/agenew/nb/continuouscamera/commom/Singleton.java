package com.agenew.nb.continuouscamera.commom;

public abstract class Singleton<T> {

    protected T instance;

    public abstract T create();

    public final T getInstance() {
        if (instance == null) {
            synchronized (this) {
                if (instance == null) {
                    instance = create();
                }
            }
        }
        return instance;
    }
}
