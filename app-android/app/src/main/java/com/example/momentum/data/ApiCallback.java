package com.example.momentum.data;

// How results come back from the backend.
// onSuccess gets the result, onError gets a message to show the user.
public interface ApiCallback<T> {
    void onSuccess(T result);
    void onError(String message);
}
