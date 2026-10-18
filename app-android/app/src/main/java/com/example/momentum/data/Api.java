package com.example.momentum.data;

// The one place that decides which backend the app talks to.
public class Api {

    private static final MomentumApi INSTANCE = new RealMomentumApi();

    public static MomentumApi get() {
        return INSTANCE;
    }
}
