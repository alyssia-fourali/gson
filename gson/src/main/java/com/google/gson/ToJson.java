package com.google.gson;

import com.google.gson.internal.GsonBuildConfig;
import com.google.gson.internal.Streams;
import com.google.gson.internal.bind.JsonTreeWriter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Type;

public class ToJson{
    Gson gson;
    public ToJson(Gson gson){
        this.gson = gson;
    }
    /**
     * This method serializes the specified object into its equivalent JSON representation. This
     * method should be used when the specified object is not a generic type. This method uses {@link
     * Class#getClass()} to get the type for the specified object, but the {@code getClass()} loses
     * the generic type information because of the Type Erasure feature of Java. Note that this method
     * works fine if any of the object fields are of generic type, just the object itself should not
     * be of a generic type. If the object is of generic type, use {@link #toJson(Object, Type)}
     * instead. If you want to write out the object to a {@link Writer}, use {@link #toJson(Object,
     * Appendable)} instead.
     *
     * @param src the object for which JSON representation is to be created
     * @return JSON representation of {@code src}.
     * @see #toJson(Object, Appendable)
     * @see #toJson(Object, Type)
     */
    public String toJson(Object src) {
        if (src == null) {
            return toJson(JsonNull.INSTANCE);
        }
        return toJson(src, src.getClass());
    }

    /**
     * This method serializes the specified object, including those of generic types, into its
     * equivalent JSON representation. This method must be used if the specified object is a generic
     * type. For non-generic objects, use {@link #toJson(Object)} instead. If you want to write out
     * the object to a {@link Appendable}, use {@link #toJson(Object, Type, Appendable)} instead.
     *
     * @param src the object for which JSON representation is to be created
     * @param typeOfSrc The specific genericized type of src. You can obtain this type by using the
     *     {@link com.google.gson.reflect.TypeToken} class. For example, to get the type for {@code
     *     Collection<Foo>}, you should use:
     *     <pre>
     * Type typeOfSrc = new TypeToken&lt;Collection&lt;Foo&gt;&gt;(){}.getType();
     * </pre>
     *
     * @return JSON representation of {@code src}.
     * @see #toJson(Object, Type, Appendable)
     * @see #toJson(Object)
     */
    public String toJson(Object src, Type typeOfSrc) {
        StringWriter writer = new StringWriter();
        toJson(src, typeOfSrc, writer);
        return writer.toString();
    }

    /**
     * This method serializes the specified object into its equivalent JSON representation and writes
     * it to the writer. This method should be used when the specified object is not a generic type.
     * This method uses {@link Class#getClass()} to get the type for the specified object, but the
     * {@code getClass()} loses the generic type information because of the Type Erasure feature of
     * Java. Note that this method works fine if any of the object fields are of generic type, just
     * the object itself should not be of a generic type. If the object is of generic type, use {@link
     * #toJson(Object, Type, Appendable)} instead.
     *
     * @param src the object for which JSON representation is to be created
     * @param writer Writer to which the JSON representation needs to be written
     * @throws JsonIOException if there was a problem writing to the writer
     * @since 1.2
     * @see #toJson(Object)
     * @see #toJson(Object, Type, Appendable)
     */
    public void toJson(Object src, Appendable writer) throws JsonIOException {
        if (src != null) {
            toJson(src, src.getClass(), writer);
        } else {
            toJson(JsonNull.INSTANCE, writer);
        }
    }

    /**
     * This method serializes the specified object, including those of generic types, into its
     * equivalent JSON representation and writes it to the writer. This method must be used if the
     * specified object is a generic type. For non-generic objects, use {@link #toJson(Object,
     * Appendable)} instead.
     *
     * @param src the object for which JSON representation is to be created
     * @param typeOfSrc The specific genericized type of src. You can obtain this type by using the
     *     {@link com.google.gson.reflect.TypeToken} class. For example, to get the type for {@code
     *     Collection<Foo>}, you should use:
     *     <pre>
     * Type typeOfSrc = new TypeToken&lt;Collection&lt;Foo&gt;&gt;(){}.getType();
     * </pre>
     *
     * @param writer Writer to which the JSON representation of src needs to be written
     * @throws JsonIOException if there was a problem writing to the writer
     * @since 1.2
     * @see #toJson(Object, Type)
     * @see #toJson(Object, Appendable)
     */
    public void toJson(Object src, Type typeOfSrc, Appendable writer) throws JsonIOException {
        try {
            JsonWriter jsonWriter = gson.newJsonWriter(Streams.writerForAppendable(writer));
            toJson(src, typeOfSrc, jsonWriter);
        } catch (IOException e) {
            throw new JsonIOException(e);
        }
    }

    /**
     * Writes the JSON representation of {@code src} of type {@code typeOfSrc} to {@code writer}.
     *
     * <p>If the {@code Gson} instance has an {@linkplain GsonBuilder#setStrictness(Strictness)
     * explicit strictness setting}, this setting will be used for writing the JSON regardless of the
     * {@linkplain JsonWriter#getStrictness() strictness} of the provided {@link JsonWriter}. For
     * legacy reasons, if the {@code Gson} instance has no explicit strictness setting and the writer
     * does not have the strictness {@link Strictness#STRICT}, the JSON will be written in {@link
     * Strictness#LENIENT} mode.<br>
     * Note that in all cases the old strictness setting of the writer will be restored when this
     * method returns.
     *
     * <p>The 'HTML-safe' and 'serialize {@code null}' settings of this {@code Gson} instance
     * (configured by the {@link GsonBuilder}) are applied, and the original settings of the writer
     * are restored once this method returns.
     *
     * @param src the object for which JSON representation is to be created
     * @param typeOfSrc the type of the object to be written
     * @param writer Writer to which the JSON representation of src needs to be written
     * @throws JsonIOException if there was a problem writing to the writer
     */
    public void toJson(Object src, Type typeOfSrc, JsonWriter writer) throws JsonIOException {
        @SuppressWarnings("unchecked")
        TypeAdapter<Object> adapter = (TypeAdapter<Object>) gson.getAdapter(TypeToken.get(typeOfSrc));

        Strictness oldStrictness = writer.getStrictness();
        if (gson.strictness != null) {
            writer.setStrictness(gson.strictness);
        } else if (writer.getStrictness() == Strictness.LEGACY_STRICT) {
            // For backward compatibility change to LENIENT if writer has default strictness LEGACY_STRICT
            writer.setStrictness(Strictness.LENIENT);
        }

        boolean oldHtmlSafe = writer.isHtmlSafe();
        boolean oldSerializeNulls = writer.getSerializeNulls();

        writer.setHtmlSafe(gson.htmlSafe);
        writer.setSerializeNulls(gson.serializeNulls);
        try {
            adapter.write(writer, src);
        } catch (IOException e) {
            throw new JsonIOException(e);
        } catch (AssertionError e) {
            throw new AssertionError(
                    "AssertionError (GSON " + GsonBuildConfig.VERSION + "): " + e.getMessage(), e);
        } finally {
            writer.setStrictness(oldStrictness);
            writer.setHtmlSafe(oldHtmlSafe);
            writer.setSerializeNulls(oldSerializeNulls);
        }
    }

    /**
     * Converts a tree of {@link JsonElement}s into its equivalent JSON representation.
     *
     * @param jsonElement root of a tree of {@link JsonElement}s
     * @return JSON String representation of the tree.
     * @since 1.4
     */
    public String toJson(JsonElement jsonElement) {
        StringWriter writer = new StringWriter();
        toJson(jsonElement, writer);
        return writer.toString();
    }

    /**
     * Writes out the equivalent JSON for a tree of {@link JsonElement}s.
     *
     * @param jsonElement root of a tree of {@link JsonElement}s
     * @param writer Writer to which the JSON representation needs to be written
     * @throws JsonIOException if there was a problem writing to the writer
     * @since 1.4
     */
    public void toJson(JsonElement jsonElement, Appendable writer) throws JsonIOException {
        try {
            JsonWriter jsonWriter = gson.newJsonWriter(Streams.writerForAppendable(writer));
            toJson(jsonElement, jsonWriter);
        } catch (IOException e) {
            throw new JsonIOException(e);
        }
    }

    /**
     * Writes the JSON for {@code jsonElement} to {@code writer}.
     *
     * <p>If the {@code Gson} instance has an {@linkplain GsonBuilder#setStrictness(Strictness)
     * explicit strictness setting}, this setting will be used for writing the JSON regardless of the
     * {@linkplain JsonWriter#getStrictness() strictness} of the provided {@link JsonWriter}. For
     * legacy reasons, if the {@code Gson} instance has no explicit strictness setting and the writer
     * does not have the strictness {@link Strictness#STRICT}, the JSON will be written in {@link
     * Strictness#LENIENT} mode.<br>
     * Note that in all cases the old strictness setting of the writer will be restored when this
     * method returns.
     *
     * <p>The 'HTML-safe' and 'serialize {@code null}' settings of this {@code Gson} instance
     * (configured by the {@link GsonBuilder}) are applied, and the original settings of the writer
     * are restored once this method returns.
     *
     * @param jsonElement the JSON element to be written
     * @param writer the JSON writer to which the provided element will be written
     * @throws JsonIOException if there was a problem writing to the writer
     */
    public void toJson(JsonElement jsonElement, JsonWriter writer) throws JsonIOException {
        Strictness oldStrictness = writer.getStrictness();
        boolean oldHtmlSafe = writer.isHtmlSafe();
        boolean oldSerializeNulls = writer.getSerializeNulls();

        writer.setHtmlSafe(gson.htmlSafe);
        writer.setSerializeNulls(gson.serializeNulls);

        if (gson.strictness != null) {
            writer.setStrictness(gson.strictness);
        } else if (writer.getStrictness() == Strictness.LEGACY_STRICT) {
            // For backward compatibility change to LENIENT if writer has default strictness LEGACY_STRICT
            writer.setStrictness(Strictness.LENIENT);
        }

        try {
            Streams.write(jsonElement, writer);
        } catch (IOException e) {
            throw new JsonIOException(e);
        } catch (AssertionError e) {
            throw new AssertionError(
                    "AssertionError (GSON " + GsonBuildConfig.VERSION + "): " + e.getMessage(), e);
        } finally {
            writer.setStrictness(oldStrictness);
            writer.setHtmlSafe(oldHtmlSafe);
            writer.setSerializeNulls(oldSerializeNulls);
        }
    }


    /**
     * This method serializes the specified object into its equivalent representation as a tree of
     * {@link JsonElement}s. This method should be used when the specified object is not a generic
     * type. This method uses {@link Class#getClass()} to get the type for the specified object, but
     * the {@code getClass()} loses the generic type information because of the Type Erasure feature
     * of Java. Note that this method works fine if any of the object fields are of generic type, just
     * the object itself should not be of a generic type. If the object is of generic type, use {@link
     * #toJsonTree(Object, Type)} instead.
     *
     * @param src the object for which JSON representation is to be created
     * @return JSON representation of {@code src}.
     * @since 1.4
     * @see #toJsonTree(Object, Type)
     */
    public JsonElement toJsonTree(Object src) {
        if (src == null) {
            return JsonNull.INSTANCE;
        }
        return toJsonTree(src, src.getClass());
    }

    /**
     * This method serializes the specified object, including those of generic types, into its
     * equivalent representation as a tree of {@link JsonElement}s. This method must be used if the
     * specified object is a generic type. For non-generic objects, use {@link #toJsonTree(Object)}
     * instead.
     *
     * @param src the object for which JSON representation is to be created
     * @param typeOfSrc The specific genericized type of src. You can obtain this type by using the
     *     {@link com.google.gson.reflect.TypeToken} class. For example, to get the type for {@code
     *     Collection<Foo>}, you should use:
     *     <pre>
     * Type typeOfSrc = new TypeToken&lt;Collection&lt;Foo&gt;&gt;(){}.getType();
     * </pre>
     *
     * @return JSON representation of {@code src}.
     * @since 1.4
     * @see #toJsonTree(Object)
     */
    public JsonElement toJsonTree(Object src, Type typeOfSrc) {
        JsonTreeWriter writer = new JsonTreeWriter();
        toJson(src, typeOfSrc, writer);
        return writer.get();
    }

}
