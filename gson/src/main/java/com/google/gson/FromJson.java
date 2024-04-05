package com.google.gson;

import com.google.gson.internal.GsonBuildConfig;
import com.google.gson.internal.Primitives;
import com.google.gson.internal.bind.JsonTreeReader;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.MalformedJsonException;

import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Type;

public class FromJson {
    Gson gson;
    public FromJson(Gson gson){
        this.gson = gson;
    }



    /**
     * This method deserializes the specified JSON into an object of the specified class. It is not
     * suitable to use if the specified class is a generic type since it will not have the generic
     * type information because of the Type Erasure feature of Java. Therefore, this method should not
     * be used if the desired type is a generic type. Note that this method works fine if any of the
     * fields of the specified object are generics, just the object itself should not be a generic
     * type. For the cases when the object is of generic type, invoke {@link #fromJson(String,
     * TypeToken)}. If you have the JSON in a {@link Reader} instead of a String, use {@link
     * #fromJson(Reader, Class)} instead.
     *
     * <p>An exception is thrown if the JSON string has multiple top-level JSON elements, or if there
     * is trailing data. Use {@link #fromJson(JsonReader, Type)} if this behavior is not desired.
     *
     * @param <T> the type of the desired object
     * @param json the string from which the object is to be deserialized
     * @param classOfT the class of T
     * @return an object of type T from the string. Returns {@code null} if {@code json} is {@code
     *     null} or if {@code json} is empty.
     * @throws JsonSyntaxException if json is not a valid representation for an object of type
     *     classOfT
     * @see #fromJson(Reader, Class)
     * @see #fromJson(String, TypeToken)
     */
    public <T> T fromJson(String json, Class<T> classOfT) throws JsonSyntaxException {
        T object = fromJson(json, TypeToken.get(classOfT));
        return Primitives.wrap(classOfT).cast(object);
    }

    /**
     * This method deserializes the specified JSON into an object of the specified type. This method
     * is useful if the specified object is a generic type. For non-generic objects, use {@link
     * #fromJson(String, Class)} instead. If you have the JSON in a {@link Reader} instead of a
     * String, use {@link #fromJson(Reader, Type)} instead.
     *
     * <p>Since {@code Type} is not parameterized by T, this method is not type-safe and should be
     * used carefully. If you are creating the {@code Type} from a {@link TypeToken}, prefer using
     * {@link #fromJson(String, TypeToken)} instead since its return type is based on the {@code
     * TypeToken} and is therefore more type-safe.
     *
     * <p>An exception is thrown if the JSON string has multiple top-level JSON elements, or if there
     * is trailing data. Use {@link #fromJson(JsonReader, Type)} if this behavior is not desired.
     *
     * @param <T> the type of the desired object
     * @param json the string from which the object is to be deserialized
     * @param typeOfT The specific genericized type of src
     * @return an object of type T from the string. Returns {@code null} if {@code json} is {@code
     *     null} or if {@code json} is empty.
     * @throws JsonSyntaxException if json is not a valid representation for an object of type typeOfT
     * @see #fromJson(Reader, Type)
     * @see #fromJson(String, Class)
     * @see #fromJson(String, TypeToken)
     */
    @SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals"})
    public <T> T fromJson(String json, Type typeOfT) throws JsonSyntaxException {
        return (T) fromJson(json, TypeToken.get(typeOfT));
    }

    /**
     * This method deserializes the specified JSON into an object of the specified type. This method
     * is useful if the specified object is a generic type. For non-generic objects, use {@link
     * #fromJson(String, Class)} instead. If you have the JSON in a {@link Reader} instead of a
     * String, use {@link #fromJson(Reader, TypeToken)} instead.
     *
     * <p>An exception is thrown if the JSON string has multiple top-level JSON elements, or if there
     * is trailing data. Use {@link #fromJson(JsonReader, TypeToken)} if this behavior is not desired.
     *
     * @param <T> the type of the desired object
     * @param json the string from which the object is to be deserialized
     * @param typeOfT The specific genericized type of src. You should create an anonymous subclass of
     *     {@code TypeToken} with the specific generic type arguments. For example, to get the type
     *     for {@code Collection<Foo>}, you should use:
     *     <pre>
     * new TypeToken&lt;Collection&lt;Foo&gt;&gt;(){}
     * </pre>
     *
     * @return an object of type T from the string. Returns {@code null} if {@code json} is {@code
     *     null} or if {@code json} is empty.
     * @throws JsonSyntaxException if json is not a valid representation for an object of the type
     *     typeOfT
     * @see #fromJson(Reader, TypeToken)
     * @see #fromJson(String, Class)
     * @since 2.10
     */
    public <T> T fromJson(String json, TypeToken<T> typeOfT) throws JsonSyntaxException {
        if (json == null) {
            return null;
        }
        StringReader reader = new StringReader(json);
        return fromJson(reader, typeOfT);
    }

    /**
     * This method deserializes the JSON read from the specified reader into an object of the
     * specified class. It is not suitable to use if the specified class is a generic type since it
     * will not have the generic type information because of the Type Erasure feature of Java.
     * Therefore, this method should not be used if the desired type is a generic type. Note that this
     * method works fine if any of the fields of the specified object are generics, just the object
     * itself should not be a generic type. For the cases when the object is of generic type, invoke
     * {@link #fromJson(Reader, TypeToken)}. If you have the JSON in a String form instead of a {@link
     * Reader}, use {@link #fromJson(String, Class)} instead.
     *
     * <p>An exception is thrown if the JSON data has multiple top-level JSON elements, or if there is
     * trailing data. Use {@link #fromJson(JsonReader, Type)} if this behavior is not desired.
     *
     * @param <T> the type of the desired object
     * @param json the reader producing the JSON from which the object is to be deserialized.
     * @param classOfT the class of T
     * @return an object of type T from the Reader. Returns {@code null} if {@code json} is at EOF.
     * @throws JsonIOException if there was a problem reading from the Reader
     * @throws JsonSyntaxException if json is not a valid representation for an object of type typeOfT
     * @since 1.2
     * @see #fromJson(String, Class)
     * @see #fromJson(Reader, TypeToken)
     */
    public <T> T fromJson(Reader json, Class<T> classOfT)
            throws JsonSyntaxException, JsonIOException {
        T object = fromJson(json, TypeToken.get(classOfT));
        return Primitives.wrap(classOfT).cast(object);
    }

    /**
     * This method deserializes the JSON read from the specified reader into an object of the
     * specified type. This method is useful if the specified object is a generic type. For
     * non-generic objects, use {@link #fromJson(Reader, Class)} instead. If you have the JSON in a
     * String form instead of a {@link Reader}, use {@link #fromJson(String, Type)} instead.
     *
     * <p>Since {@code Type} is not parameterized by T, this method is not type-safe and should be
     * used carefully. If you are creating the {@code Type} from a {@link TypeToken}, prefer using
     * {@link #fromJson(Reader, TypeToken)} instead since its return type is based on the {@code
     * TypeToken} and is therefore more type-safe.
     *
     * <p>An exception is thrown if the JSON data has multiple top-level JSON elements, or if there is
     * trailing data. Use {@link #fromJson(JsonReader, Type)} if this behavior is not desired.
     *
     * @param <T> the type of the desired object
     * @param json the reader producing JSON from which the object is to be deserialized
     * @param typeOfT The specific genericized type of src
     * @return an object of type T from the Reader. Returns {@code null} if {@code json} is at EOF.
     * @throws JsonIOException if there was a problem reading from the Reader
     * @throws JsonSyntaxException if json is not a valid representation for an object of type typeOfT
     * @since 1.2
     * @see #fromJson(String, Type)
     * @see #fromJson(Reader, Class)
     * @see #fromJson(Reader, TypeToken)
     */
    @SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals"})
    public <T> T fromJson(Reader json, Type typeOfT) throws JsonIOException, JsonSyntaxException {
        return (T) fromJson(json, TypeToken.get(typeOfT));
    }

    /**
     * This method deserializes the JSON read from the specified reader into an object of the
     * specified type. This method is useful if the specified object is a generic type. For
     * non-generic objects, use {@link #fromJson(Reader, Class)} instead. If you have the JSON in a
     * String form instead of a {@link Reader}, use {@link #fromJson(String, TypeToken)} instead.
     *
     * <p>An exception is thrown if the JSON data has multiple top-level JSON elements, or if there is
     * trailing data. Use {@link #fromJson(JsonReader, TypeToken)} if this behavior is not desired.
     *
     * @param <T> the type of the desired object
     * @param json the reader producing JSON from which the object is to be deserialized
     * @param typeOfT The specific genericized type of src. You should create an anonymous subclass of
     *     {@code TypeToken} with the specific generic type arguments. For example, to get the type
     *     for {@code Collection<Foo>}, you should use:
     *     <pre>
     * new TypeToken&lt;Collection&lt;Foo&gt;&gt;(){}
     * </pre>
     *
     * @return an object of type T from the Reader. Returns {@code null} if {@code json} is at EOF.
     * @throws JsonIOException if there was a problem reading from the Reader
     * @throws JsonSyntaxException if json is not a valid representation for an object of type of
     *     typeOfT
     * @see #fromJson(String, TypeToken)
     * @see #fromJson(Reader, Class)
     * @since 2.10
     */
    public <T> T fromJson(Reader json, TypeToken<T> typeOfT)
            throws JsonIOException, JsonSyntaxException {
        JsonReader jsonReader = gson.newJsonReader(json);
        T object = fromJson(jsonReader, typeOfT);
        assertFullConsumption(object, jsonReader);
        return object;
    }

    // fromJson(JsonReader, Class) is unfortunately missing and cannot be added now without breaking
    // source compatibility in certain cases, see
    // https://github.com/google/gson/pull/1700#discussion_r973764414

    /**
     * Reads the next JSON value from {@code reader} and converts it to an object of type {@code
     * typeOfT}. Returns {@code null}, if the {@code reader} is at EOF.
     *
     * <p>Since {@code Type} is not parameterized by T, this method is not type-safe and should be
     * used carefully. If you are creating the {@code Type} from a {@link TypeToken}, prefer using
     * {@link #fromJson(JsonReader, TypeToken)} instead since its return type is based on the {@code
     * TypeToken} and is therefore more type-safe. If the provided type is a {@code Class} the {@code
     * TypeToken} can be created with {@link TypeToken#get(Class)}.
     *
     * <p>Unlike the other {@code fromJson} methods, no exception is thrown if the JSON data has
     * multiple top-level JSON elements, or if there is trailing data.
     *
     * <p>If the {@code Gson} instance has an {@linkplain GsonBuilder#setStrictness(Strictness)
     * explicit strictness setting}, this setting will be used for reading the JSON regardless of the
     * {@linkplain JsonReader#getStrictness() strictness} of the provided {@link JsonReader}. For
     * legacy reasons, if the {@code Gson} instance has no explicit strictness setting and the reader
     * does not have the strictness {@link Strictness#STRICT}, the JSON will be written in {@link
     * Strictness#LENIENT} mode.<br>
     * Note that in all cases the old strictness setting of the reader will be restored when this
     * method returns.
     *
     * @param <T> the type of the desired object
     * @param reader the reader whose next JSON value should be deserialized
     * @param typeOfT The specific genericized type of src
     * @return an object of type T from the JsonReader. Returns {@code null} if {@code reader} is at
     *     EOF.
     * @throws JsonIOException if there was a problem reading from the JsonReader
     * @throws JsonSyntaxException if json is not a valid representation for an object of type typeOfT
     * @see #fromJson(Reader, Type)
     * @see #fromJson(JsonReader, TypeToken)
     */
    @SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals"})
    public <T> T fromJson(JsonReader reader, Type typeOfT)
            throws JsonIOException, JsonSyntaxException {
        return (T) fromJson(reader, TypeToken.get(typeOfT));
    }

    /**
     * Reads the next JSON value from {@code reader} and converts it to an object of type {@code
     * typeOfT}. Returns {@code null}, if the {@code reader} is at EOF. This method is useful if the
     * specified object is a generic type. For non-generic objects, {@link #fromJson(JsonReader,
     * Type)} can be called, or {@link TypeToken#get(Class)} can be used to create the type token.
     *
     * <p>Unlike the other {@code fromJson} methods, no exception is thrown if the JSON data has
     * multiple top-level JSON elements, or if there is trailing data.
     *
     * <p>If the {@code Gson} instance has an {@linkplain GsonBuilder#setStrictness(Strictness)
     * explicit strictness setting}, this setting will be used for reading the JSON regardless of the
     * {@linkplain JsonReader#getStrictness() strictness} of the provided {@link JsonReader}. For
     * legacy reasons, if the {@code Gson} instance has no explicit strictness setting and the reader
     * does not have the strictness {@link Strictness#STRICT}, the JSON will be written in {@link
     * Strictness#LENIENT} mode.<br>
     * Note that in all cases the old strictness setting of the reader will be restored when this
     * method returns.
     *
     * @param <T> the type of the desired object
     * @param reader the reader whose next JSON value should be deserialized
     * @param typeOfT The specific genericized type of src. You should create an anonymous subclass of
     *     {@code TypeToken} with the specific generic type arguments. For example, to get the type
     *     for {@code Collection<Foo>}, you should use:
     *     <pre>
     * new TypeToken&lt;Collection&lt;Foo&gt;&gt;(){}
     * </pre>
     *
     * @return an object of type T from the JsonReader. Returns {@code null} if {@code reader} is at
     *     EOF.
     * @throws JsonIOException if there was a problem reading from the JsonReader
     * @throws JsonSyntaxException if json is not a valid representation for an object of the type
     *     typeOfT
     * @see #fromJson(Reader, TypeToken)
     * @see #fromJson(JsonReader, Type)
     * @since 2.10
     */
    public <T> T fromJson(JsonReader reader, TypeToken<T> typeOfT)
            throws JsonIOException, JsonSyntaxException {
        boolean isEmpty = true;
        Strictness oldStrictness = reader.getStrictness();

        if (gson.strictness != null) {
            reader.setStrictness(gson.strictness);
        } else if (reader.getStrictness() == Strictness.LEGACY_STRICT) {
            // For backward compatibility change to LENIENT if reader has default strictness LEGACY_STRICT
            reader.setStrictness(Strictness.LENIENT);
        }

        try {
            JsonToken unused = reader.peek();
            isEmpty = false;
            TypeAdapter<T> typeAdapter = gson.getAdapter(typeOfT);
            return typeAdapter.read(reader);
        } catch (EOFException e) {
            /*
             * For compatibility with JSON 1.5 and earlier, we return null for empty
             * documents instead of throwing.
             */
            if (isEmpty) {
                return null;
            }
            throw new JsonSyntaxException(e);
        } catch (IllegalStateException e) {
            throw new JsonSyntaxException(e);
        } catch (IOException e) {
            // TODO(inder): Figure out whether it is indeed right to rethrow this as JsonSyntaxException
            throw new JsonSyntaxException(e);
        } catch (AssertionError e) {
            throw new AssertionError(
                    "AssertionError (GSON " + GsonBuildConfig.VERSION + "): " + e.getMessage(), e);
        } finally {
            reader.setStrictness(oldStrictness);
        }
    }

    /**
     * This method deserializes the JSON read from the specified parse tree into an object of the
     * specified type. It is not suitable to use if the specified class is a generic type since it
     * will not have the generic type information because of the Type Erasure feature of Java.
     * Therefore, this method should not be used if the desired type is a generic type. Note that this
     * method works fine if any of the fields of the specified object are generics, just the object
     * itself should not be a generic type. For the cases when the object is of generic type, invoke
     * {@link #fromJson(JsonElement, TypeToken)}.
     *
     * @param <T> the type of the desired object
     * @param json the root of the parse tree of {@link JsonElement}s from which the object is to be
     *     deserialized
     * @param classOfT The class of T
     * @return an object of type T from the JSON. Returns {@code null} if {@code json} is {@code null}
     *     or if {@code json} is empty.
     * @throws JsonSyntaxException if json is not a valid representation for an object of type
     *     classOfT
     * @since 1.3
     * @see #fromJson(Reader, Class)
     * @see #fromJson(JsonElement, TypeToken)
     */
    public <T> T fromJson(JsonElement json, Class<T> classOfT) throws JsonSyntaxException {
        T object = fromJson(json, TypeToken.get(classOfT));
        return Primitives.wrap(classOfT).cast(object);
    }

    /**
     * This method deserializes the JSON read from the specified parse tree into an object of the
     * specified type. This method is useful if the specified object is a generic type. For
     * non-generic objects, use {@link #fromJson(JsonElement, Class)} instead.
     *
     * <p>Since {@code Type} is not parameterized by T, this method is not type-safe and should be
     * used carefully. If you are creating the {@code Type} from a {@link TypeToken}, prefer using
     * {@link #fromJson(JsonElement, TypeToken)} instead since its return type is based on the {@code
     * TypeToken} and is therefore more type-safe.
     *
     * @param <T> the type of the desired object
     * @param json the root of the parse tree of {@link JsonElement}s from which the object is to be
     *     deserialized
     * @param typeOfT The specific genericized type of src
     * @return an object of type T from the JSON. Returns {@code null} if {@code json} is {@code null}
     *     or if {@code json} is empty.
     * @throws JsonSyntaxException if json is not a valid representation for an object of type typeOfT
     * @since 1.3
     * @see #fromJson(Reader, Type)
     * @see #fromJson(JsonElement, Class)
     * @see #fromJson(JsonElement, TypeToken)
     */
    @SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals"})
    public <T> T fromJson(JsonElement json, Type typeOfT) throws JsonSyntaxException {
        return (T) fromJson(json, TypeToken.get(typeOfT));
    }

    /**
     * This method deserializes the JSON read from the specified parse tree into an object of the
     * specified type. This method is useful if the specified object is a generic type. For
     * non-generic objects, use {@link #fromJson(JsonElement, Class)} instead.
     *
     * @param <T> the type of the desired object
     * @param json the root of the parse tree of {@link JsonElement}s from which the object is to be
     *     deserialized
     * @param typeOfT The specific genericized type of src. You should create an anonymous subclass of
     *     {@code TypeToken} with the specific generic type arguments. For example, to get the type
     *     for {@code Collection<Foo>}, you should use:
     *     <pre>
     * new TypeToken&lt;Collection&lt;Foo&gt;&gt;(){}
     * </pre>
     *
     * @return an object of type T from the JSON. Returns {@code null} if {@code json} is {@code null}
     *     or if {@code json} is empty.
     * @throws JsonSyntaxException if json is not a valid representation for an object of type typeOfT
     * @see #fromJson(Reader, TypeToken)
     * @see #fromJson(JsonElement, Class)
     * @since 2.10
     */
    public <T> T fromJson(JsonElement json, TypeToken<T> typeOfT) throws JsonSyntaxException {
        if (json == null) {
            return null;
        }
        return fromJson(new JsonTreeReader(json), typeOfT);
    }

    private static void assertFullConsumption(Object obj, JsonReader reader) {
        try {
            if (obj != null && reader.peek() != JsonToken.END_DOCUMENT) {
                throw new JsonSyntaxException("JSON document was not fully consumed.");
            }
        } catch (MalformedJsonException e) {
            throw new JsonSyntaxException(e);
        } catch (IOException e) {
            throw new JsonIOException(e);
        }
    }
}
