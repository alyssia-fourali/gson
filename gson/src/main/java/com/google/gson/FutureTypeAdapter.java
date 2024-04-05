package com.google.gson;

import com.google.gson.internal.bind.SerializationDelegatingTypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

/**
 * Proxy type adapter for cyclic type graphs.
 *
 * <p><b>Important:</b> Setting the delegate adapter is not thread-safe; instances of {@code
 * FutureTypeAdapter} must only be published to other threads after the delegate has been set.
 *
 * @see Gson#threadLocalAdapterResults
 */

public class FutureTypeAdapter<T> extends SerializationDelegatingTypeAdapter<T> {
    private TypeAdapter<T> delegate = null;

    public void setDelegate(TypeAdapter<T> typeAdapter) {
        if (delegate != null) {
            throw new AssertionError("Delegate is already set");
        }
        delegate = typeAdapter;
    }

    public TypeAdapter<T> delegate() {
        TypeAdapter<T> delegate = this.delegate;
        if (delegate == null) {
            // Can occur when adapter is leaked to other thread or when adapter is used for
            // (de-)serialization
            // directly within the TypeAdapterFactory which requested it
            throw new IllegalStateException(
                    "Adapter for type with cyclic dependency has been used"
                            + " before dependency has been resolved");
        }
        return delegate;
    }

    @Override
    public TypeAdapter<T> getSerializationDelegate() {
        return delegate();
    }

    @Override
    public T read(JsonReader in) throws IOException {
        return delegate().read(in);
    }

    @Override
    public void write(JsonWriter out, T value) throws IOException {
        delegate().write(out, value);
    }
}
