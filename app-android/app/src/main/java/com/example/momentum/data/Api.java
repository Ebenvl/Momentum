package com.example.momentum.data;

// The one place that decides which backend the app talks to.
// RIGHT NOW IT IS FAKE: all data in the app comes from FakeMomentumApi.
// When the real backend is ready, only this line changes.
public class Api {

    private static final MomentumApi INSTANCE = new FakeMomentumApi();

    public static MomentumApi get() {
        return INSTANCE;
    }
}
