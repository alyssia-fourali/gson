/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.gson;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.internal.ConstructorConstructor;
import com.google.gson.internal.Excluder;
import com.google.gson.internal.GsonBuildConfig;
import com.google.gson.internal.LazilyParsedNumber;
import com.google.gson.internal.Primitives;
import com.google.gson.internal.Streams;
import com.google.gson.internal.bind.ArrayTypeAdapter;
import com.google.gson.internal.bind.CollectionTypeAdapterFactory;
import com.google.gson.internal.bind.DefaultDateTypeAdapter;
import com.google.gson.internal.bind.JsonAdapterAnnotationTypeAdapterFactory;
import com.google.gson.internal.bind.JsonTreeReader;
import com.google.gson.internal.bind.JsonTreeWriter;
import com.google.gson.internal.bind.MapTypeAdapterFactory;
import com.google.gson.internal.bind.NumberTypeAdapter;
import com.google.gson.internal.bind.ObjectTypeAdapter;
import com.google.gson.internal.bind.ReflectiveTypeAdapterFactory;
import com.google.gson.internal.bind.SerializationDelegatingTypeAdapter;
import com.google.gson.internal.bind.TypeAdapters;
import com.google.gson.internal.sql.SqlTypesSupport;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.google.gson.stream.MalformedJsonException;
import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * This is the main class for using Gson. Gson is typically used by first constructing a Gson
 * instance and then invoking {@link #toJson(Object)} or {@link #fromJson(String, Class)} methods on
 * it. Gson instances are Thread-safe so you can reuse them freely across multiple threads.
 *
 * <p>You can create a Gson instance by invoking {@code new Gson()} if the default configuration is
 * all you need. You can also use {@link GsonBuilder} to build a Gson instance with various
 * configuration options such as versioning support, pretty printing, custom newline, custom indent,
 * custom {@link JsonSerializer}s, {@link JsonDeserializer}s, and {@link InstanceCreator}s.
 *
 * <p>Here is an example of how Gson is used for a simple Class:
 *
 * <pre>
 * Gson gson = new Gson(); // Or use new GsonBuilder().create();
 * MyType target = new MyType();
 * String json = gson.toJson(target); // serializes target to JSON
 * MyType target2 = gson.fromJson(json, MyType.class); // deserializes json into target2
 * </pre>
 *
 * <p>If the type of the object that you are converting is a {@code ParameterizedType} (i.e. has at
 * least one type argument, for example {@code List<MyType>}) then for deserialization you must use
 * a {@code fromJson} method with {@link Type} or {@link TypeToken} parameter to specify the
 * parameterized type. For serialization specifying a {@code Type} or {@code TypeToken} is optional,
 * otherwise Gson will use the runtime type of the object. {@link TypeToken} is a class provided by
 * Gson which helps creating parameterized types. Here is an example showing how this can be done:
 *
 * <pre>
 * TypeToken&lt;List&lt;MyType&gt;&gt; listType = new TypeToken&lt;List&lt;MyType&gt;&gt;() {};
 * List&lt;MyType&gt; target = new LinkedList&lt;MyType&gt;();
 * target.add(new MyType(1, "abc"));
 *
 * Gson gson = new Gson();
 * // For serialization you normally do not have to specify the type, Gson will use
 * // the runtime type of the objects, however you can also specify it explicitly
 * String json = gson.toJson(target, listType.getType());
 *
 * // But for deserialization you have to specify the type
 * List&lt;MyType&gt; target2 = gson.fromJson(json, listType);
 * </pre>
 *
 * <p>See the <a href="https://github.com/google/gson/blob/main/UserGuide.md">Gson User Guide</a>
 * for a more complete set of examples.
 *
 * <h2 id="default-lenient">JSON Strictness handling</h2>
 *
 * For legacy reasons most of the {@code Gson} methods allow JSON data which does not comply with
 * the JSON specification when no explicit {@linkplain Strictness strictness} is set (the default).
 * To specify the strictness of a {@code Gson} instance, you should set it through {@link
 * GsonBuilder#setStrictness(Strictness)}.
 *
 * <p>For older Gson versions, which don't have the strictness mode API, the following workarounds
 * can be used:
 *
 * <h3>Serialization</h3>
 *
 * <ol>
 *   <li>Use {@link #getAdapter(Class)} to obtain the adapter for the type to be serialized
 *   <li>When using an existing {@code JsonWriter}, manually apply the writer settings of this
 *       {@code Gson} instance listed by {@link #newJsonWriter(Writer)}.<br>
 *       Otherwise, when not using an existing {@code JsonWriter}, use {@link
 *       #newJsonWriter(Writer)} to construct one.
 *   <li>Call {@link TypeAdapter#write(JsonWriter, Object)}
 * </ol>
 *
 * <h3>Deserialization</h3>
 *
 * <ol>
 *   <li>Use {@link #getAdapter(Class)} to obtain the adapter for the type to be deserialized
 *   <li>When using an existing {@code JsonReader}, manually apply the reader settings of this
 *       {@code Gson} instance listed by {@link #newJsonReader(Reader)}.<br>
 *       Otherwise, when not using an existing {@code JsonReader}, use {@link
 *       #newJsonReader(Reader)} to construct one.
 *   <li>Call {@link TypeAdapter#read(JsonReader)}
 *   <li>Call {@link JsonReader#peek()} and verify that the result is {@link JsonToken#END_DOCUMENT}
 *       to make sure there is no trailing data
 * </ol>
 *
 * Note that the {@code JsonReader} created this way is only 'legacy strict', it mostly adheres to
 * the JSON specification but allows small deviations. See {@link
 * JsonReader#setStrictness(Strictness)} for details.
 *
 * @see TypeToken
 * @author Inderjeet Singh
 * @author Joel Leitch
 * @author Jesse Wilson
 */
public final class Gson {

  public ToJson toJson = new ToJson(this);
  public FromJson fromJson = new FromJson(this);
  static final boolean DEFAULT_JSON_NON_EXECUTABLE = false;
  // Strictness of `null` is the legacy mode where some Gson APIs are always lenient
  static final Strictness DEFAULT_STRICTNESS = null;
  static final FormattingStyle DEFAULT_FORMATTING_STYLE = FormattingStyle.COMPACT;
  static final boolean DEFAULT_ESCAPE_HTML = true;
  static final boolean DEFAULT_SERIALIZE_NULLS = false;
  static final boolean DEFAULT_COMPLEX_MAP_KEYS = false;
  static final boolean DEFAULT_SPECIALIZE_FLOAT_VALUES = false;
  static final boolean DEFAULT_USE_JDK_UNSAFE = true;
  static final String DEFAULT_DATE_PATTERN = null;
  static final FieldNamingStrategy DEFAULT_FIELD_NAMING_STRATEGY = FieldNamingPolicy.IDENTITY;
  static final ToNumberStrategy DEFAULT_OBJECT_TO_NUMBER_STRATEGY = ToNumberPolicy.DOUBLE;
  static final ToNumberStrategy DEFAULT_NUMBER_TO_NUMBER_STRATEGY =
      ToNumberPolicy.LAZILY_PARSED_NUMBER;

  private static final String JSON_NON_EXECUTABLE_PREFIX = ")]}'\n";

  /**
   * This thread local guards against reentrant calls to {@link #getAdapter(TypeToken)}. In certain
   * object graphs, creating an adapter for a type may recursively require an adapter for the same
   * type! Without intervention, the recursive lookup would stack overflow. We cheat by returning a
   * proxy type adapter, {@link FutureTypeAdapter}, which is wired up once the initial adapter has
   * been created.
   *
   * <p>The map stores the type adapters for ongoing {@code getAdapter} calls, with the type token
   * provided to {@code getAdapter} as key and either {@code FutureTypeAdapter} or a regular {@code
   * TypeAdapter} as value.
   */
  @SuppressWarnings("ThreadLocalUsage")
  private final ThreadLocal<Map<TypeToken<?>, TypeAdapter<?>>> threadLocalAdapterResults =
      new ThreadLocal<>();

  private final ConcurrentMap<TypeToken<?>, TypeAdapter<?>> typeTokenCache =
      new ConcurrentHashMap<>();

  private final ConstructorConstructor constructorConstructor;
  private final JsonAdapterAnnotationTypeAdapterFactory jsonAdapterFactory;

  final List<TypeAdapterFactory> factories;

  final Excluder excluder;
  final FieldNamingStrategy fieldNamingStrategy;
  final Map<Type, InstanceCreator<?>> instanceCreators;
  final boolean serializeNulls;
  final boolean complexMapKeySerialization;
  final boolean generateNonExecutableJson;
  final boolean htmlSafe;
  final FormattingStyle formattingStyle;
  final Strictness strictness;
  final boolean serializeSpecialFloatingPointValues;
  final boolean useJdkUnsafe;
  final String datePattern;
  final int dateStyle;
  final int timeStyle;
  final LongSerializationPolicy longSerializationPolicy;
  final List<TypeAdapterFactory> builderFactories;
  final List<TypeAdapterFactory> builderHierarchyFactories;
  final ToNumberStrategy objectToNumberStrategy;
  final ToNumberStrategy numberToNumberStrategy;
  final List<ReflectionAccessFilter> reflectionFilters;

  /**
   * Constructs a Gson object with default configuration. The default configuration has the
   * following settings:
   *
   * <ul>
   *   <li>The JSON generated by {@code toJson} methods is in compact representation. This means
   *       that all the unneeded white-space is removed. You can change this behavior with {@link
   *       GsonBuilder#setPrettyPrinting()}.
   *   <li>When the JSON generated contains more than one line, the kind of newline and indent to
   *       use can be configured with {@link GsonBuilder#setFormattingStyle(FormattingStyle)}.
   *   <li>The generated JSON omits all the fields that are null. Note that nulls in arrays are kept
   *       as is since an array is an ordered list. Moreover, if a field is not null, but its
   *       generated JSON is empty, the field is kept. You can configure Gson to serialize null
   *       values by setting {@link GsonBuilder#serializeNulls()}.
   *   <li>Gson provides default serialization and deserialization for Enums, {@link Map}, {@link
   *       java.net.URL}, {@link java.net.URI}, {@link java.util.Locale}, {@link java.util.Date},
   *       {@link java.math.BigDecimal}, and {@link java.math.BigInteger} classes. If you would
   *       prefer to change the default representation, you can do so by registering a type adapter
   *       through {@link GsonBuilder#registerTypeAdapter(Type, Object)}.
   *   <li>The default Date format is same as {@link java.text.DateFormat#DEFAULT}. This format
   *       ignores the millisecond portion of the date during serialization. You can change this by
   *       invoking {@link GsonBuilder#setDateFormat(int, int)} or {@link
   *       GsonBuilder#setDateFormat(String)}.
   *   <li>By default, Gson ignores the {@link com.google.gson.annotations.Expose} annotation. You
   *       can enable Gson to serialize/deserialize only those fields marked with this annotation
   *       through {@link GsonBuilder#excludeFieldsWithoutExposeAnnotation()}.
   *   <li>By default, Gson ignores the {@link com.google.gson.annotations.Since} annotation. You
   *       can enable Gson to use this annotation through {@link GsonBuilder#setVersion(double)}.
   *   <li>The default field naming policy for the output JSON is same as in Java. So, a Java class
   *       field {@code versionNumber} will be output as {@code "versionNumber"} in JSON. The same
   *       rules are applied for mapping incoming JSON to the Java classes. You can change this
   *       policy through {@link GsonBuilder#setFieldNamingPolicy(FieldNamingPolicy)}.
   *   <li>By default, Gson excludes {@code transient} or {@code static} fields from consideration
   *       for serialization and deserialization. You can change this behavior through {@link
   *       GsonBuilder#excludeFieldsWithModifiers(int...)}.
   *   <li>No explicit strictness is set. You can change this by calling {@link
   *       GsonBuilder#setStrictness(Strictness)}.
   * </ul>
   */
  public Gson() {
    this(
        Excluder.DEFAULT,
        DEFAULT_FIELD_NAMING_STRATEGY,
        Collections.<Type, InstanceCreator<?>>emptyMap(),
        DEFAULT_SERIALIZE_NULLS,
        DEFAULT_COMPLEX_MAP_KEYS,
        DEFAULT_JSON_NON_EXECUTABLE,
        DEFAULT_ESCAPE_HTML,
        DEFAULT_FORMATTING_STYLE,
        DEFAULT_STRICTNESS,
        DEFAULT_SPECIALIZE_FLOAT_VALUES,
        DEFAULT_USE_JDK_UNSAFE,
        LongSerializationPolicy.DEFAULT,
        DEFAULT_DATE_PATTERN,
        DateFormat.DEFAULT,
        DateFormat.DEFAULT,
        Collections.<TypeAdapterFactory>emptyList(),
        Collections.<TypeAdapterFactory>emptyList(),
        Collections.<TypeAdapterFactory>emptyList(),
        DEFAULT_OBJECT_TO_NUMBER_STRATEGY,
        DEFAULT_NUMBER_TO_NUMBER_STRATEGY,
        Collections.<ReflectionAccessFilter>emptyList());
  }

  Gson(
      Excluder excluder,
      FieldNamingStrategy fieldNamingStrategy,
      Map<Type, InstanceCreator<?>> instanceCreators,
      boolean serializeNulls,
      boolean complexMapKeySerialization,
      boolean generateNonExecutableGson,
      boolean htmlSafe,
      FormattingStyle formattingStyle,
      Strictness strictness,
      boolean serializeSpecialFloatingPointValues,
      boolean useJdkUnsafe,
      LongSerializationPolicy longSerializationPolicy,
      String datePattern,
      int dateStyle,
      int timeStyle,
      List<TypeAdapterFactory> builderFactories,
      List<TypeAdapterFactory> builderHierarchyFactories,
      List<TypeAdapterFactory> factoriesToBeAdded,
      ToNumberStrategy objectToNumberStrategy,
      ToNumberStrategy numberToNumberStrategy,
      List<ReflectionAccessFilter> reflectionFilters) {
    this.excluder = excluder;
    this.fieldNamingStrategy = fieldNamingStrategy;
    this.instanceCreators = instanceCreators;
    this.constructorConstructor =
        new ConstructorConstructor(instanceCreators, useJdkUnsafe, reflectionFilters);
    this.serializeNulls = serializeNulls;
    this.complexMapKeySerialization = complexMapKeySerialization;
    this.generateNonExecutableJson = generateNonExecutableGson;
    this.htmlSafe = htmlSafe;
    this.formattingStyle = formattingStyle;
    this.strictness = strictness;
    this.serializeSpecialFloatingPointValues = serializeSpecialFloatingPointValues;
    this.useJdkUnsafe = useJdkUnsafe;
    this.longSerializationPolicy = longSerializationPolicy;
    this.datePattern = datePattern;
    this.dateStyle = dateStyle;
    this.timeStyle = timeStyle;
    this.builderFactories = builderFactories;
    this.builderHierarchyFactories = builderHierarchyFactories;
    this.objectToNumberStrategy = objectToNumberStrategy;
    this.numberToNumberStrategy = numberToNumberStrategy;
    this.reflectionFilters = reflectionFilters;

    List<TypeAdapterFactory> factories = new ArrayList<>();

    // built-in type adapters that cannot be overridden
    factories.add(TypeAdapters.JSON_ELEMENT_FACTORY);
    factories.add(ObjectTypeAdapter.getFactory(objectToNumberStrategy));

    // the excluder must precede all adapters that handle user-defined types
    factories.add(excluder);

    // users' type adapters
    factories.addAll(factoriesToBeAdded);

    // type adapters for basic platform types
    factories.add(TypeAdapters.STRING_FACTORY);
    factories.add(TypeAdapters.INTEGER_FACTORY);
    factories.add(TypeAdapters.BOOLEAN_FACTORY);
    factories.add(TypeAdapters.BYTE_FACTORY);
    factories.add(TypeAdapters.SHORT_FACTORY);
    TypeAdapter<Number> longAdapter = longAdapter(longSerializationPolicy);
    factories.add(TypeAdapters.newFactory(long.class, Long.class, longAdapter));
    factories.add(
        TypeAdapters.newFactory(
            double.class, Double.class, doubleAdapter(serializeSpecialFloatingPointValues)));
    factories.add(
        TypeAdapters.newFactory(
            float.class, Float.class, floatAdapter(serializeSpecialFloatingPointValues)));
    factories.add(NumberTypeAdapter.getFactory(numberToNumberStrategy));
    factories.add(TypeAdapters.ATOMIC_INTEGER_FACTORY);
    factories.add(TypeAdapters.ATOMIC_BOOLEAN_FACTORY);
    factories.add(TypeAdapters.newFactory(AtomicLong.class, atomicLongAdapter(longAdapter)));
    factories.add(
        TypeAdapters.newFactory(AtomicLongArray.class, atomicLongArrayAdapter(longAdapter)));
    factories.add(TypeAdapters.ATOMIC_INTEGER_ARRAY_FACTORY);
    factories.add(TypeAdapters.CHARACTER_FACTORY);
    factories.add(TypeAdapters.STRING_BUILDER_FACTORY);
    factories.add(TypeAdapters.STRING_BUFFER_FACTORY);
    factories.add(TypeAdapters.newFactory(BigDecimal.class, TypeAdapters.BIG_DECIMAL));
    factories.add(TypeAdapters.newFactory(BigInteger.class, TypeAdapters.BIG_INTEGER));
    // Add adapter for LazilyParsedNumber because user can obtain it from Gson and then try to
    // serialize it again
    factories.add(
        TypeAdapters.newFactory(LazilyParsedNumber.class, TypeAdapters.LAZILY_PARSED_NUMBER));
    factories.add(TypeAdapters.URL_FACTORY);
    factories.add(TypeAdapters.URI_FACTORY);
    factories.add(TypeAdapters.UUID_FACTORY);
    factories.add(TypeAdapters.CURRENCY_FACTORY);
    factories.add(TypeAdapters.LOCALE_FACTORY);
    factories.add(TypeAdapters.INET_ADDRESS_FACTORY);
    factories.add(TypeAdapters.BIT_SET_FACTORY);
    factories.add(DefaultDateTypeAdapter.DEFAULT_STYLE_FACTORY);
    factories.add(TypeAdapters.CALENDAR_FACTORY);

    if (SqlTypesSupport.SUPPORTS_SQL_TYPES) {
      factories.add(SqlTypesSupport.TIME_FACTORY);
      factories.add(SqlTypesSupport.DATE_FACTORY);
      factories.add(SqlTypesSupport.TIMESTAMP_FACTORY);
    }

    factories.add(ArrayTypeAdapter.FACTORY);
    factories.add(TypeAdapters.CLASS_FACTORY);

    // type adapters for composite and user-defined types
    factories.add(new CollectionTypeAdapterFactory(constructorConstructor));
    factories.add(new MapTypeAdapterFactory(constructorConstructor, complexMapKeySerialization));
    this.jsonAdapterFactory = new JsonAdapterAnnotationTypeAdapterFactory(constructorConstructor);
    factories.add(jsonAdapterFactory);
    factories.add(TypeAdapters.ENUM_FACTORY);
    factories.add(
        new ReflectiveTypeAdapterFactory(
            constructorConstructor,
            fieldNamingStrategy,
            excluder,
            jsonAdapterFactory,
            reflectionFilters));

    this.factories = Collections.unmodifiableList(factories);
  }

  /**
   * Returns a new GsonBuilder containing all custom factories and configuration used by the current
   * instance.
   *
   * @return a GsonBuilder instance.
   * @since 2.8.3
   */
  public GsonBuilder newBuilder() {
    return new GsonBuilder(this);
  }

  /**
   * @deprecated This method by accident exposes an internal Gson class; it might be removed in a
   *     future version.
   */
  @Deprecated
  public Excluder excluder() {
    return excluder;
  }

  /**
   * Returns the field naming strategy used by this Gson instance.
   *
   * @see GsonBuilder#setFieldNamingStrategy(FieldNamingStrategy)
   */
  public FieldNamingStrategy fieldNamingStrategy() {
    return fieldNamingStrategy;
  }

  /**
   * Returns whether this Gson instance is serializing JSON object properties with {@code null}
   * values, or just omits them.
   *
   * @see GsonBuilder#serializeNulls()
   */
  public boolean serializeNulls() {
    return serializeNulls;
  }

  /**
   * Returns whether this Gson instance produces JSON output which is HTML-safe, that means all HTML
   * characters are escaped.
   *
   * @see GsonBuilder#disableHtmlEscaping()
   */
  public boolean htmlSafe() {
    return htmlSafe;
  }

  private TypeAdapter<Number> doubleAdapter(boolean serializeSpecialFloatingPointValues) {
    if (serializeSpecialFloatingPointValues) {
      return TypeAdapters.DOUBLE;
    }
    return new TypeAdapter<Number>() {
      @Override
      public Double read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
          in.nextNull();
          return null;
        }
        return in.nextDouble();
      }

      @Override
      public void write(JsonWriter out, Number value) throws IOException {
        if (value == null) {
          out.nullValue();
          return;
        }
        double doubleValue = value.doubleValue();
        checkValidFloatingPoint(doubleValue);
        out.value(doubleValue);
      }
    };
  }

  private TypeAdapter<Number> floatAdapter(boolean serializeSpecialFloatingPointValues) {
    if (serializeSpecialFloatingPointValues) {
      return TypeAdapters.FLOAT;
    }
    return new TypeAdapter<Number>() {
      @Override
      public Float read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
          in.nextNull();
          return null;
        }
        return (float) in.nextDouble();
      }

      @Override
      public void write(JsonWriter out, Number value) throws IOException {
        if (value == null) {
          out.nullValue();
          return;
        }
        float floatValue = value.floatValue();
        checkValidFloatingPoint(floatValue);
        // For backward compatibility don't call `JsonWriter.value(float)` because that method has
        // been newly added and not all custom JsonWriter implementations might override it yet
        Number floatNumber = value instanceof Float ? value : floatValue;
        out.value(floatNumber);
      }
    };
  }

  static void checkValidFloatingPoint(double value) {
    if (Double.isNaN(value) || Double.isInfinite(value)) {
      throw new IllegalArgumentException(
          value
              + " is not a valid double value as per JSON specification. To override this"
              + " behavior, use GsonBuilder.serializeSpecialFloatingPointValues() method.");
    }
  }

  private static TypeAdapter<Number> longAdapter(LongSerializationPolicy longSerializationPolicy) {
    if (longSerializationPolicy == LongSerializationPolicy.DEFAULT) {
      return TypeAdapters.LONG;
    }
    return new TypeAdapter<Number>() {
      @Override
      public Number read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
          in.nextNull();
          return null;
        }
        return in.nextLong();
      }

      @Override
      public void write(JsonWriter out, Number value) throws IOException {
        if (value == null) {
          out.nullValue();
          return;
        }
        out.value(value.toString());
      }
    };
  }

  private static TypeAdapter<AtomicLong> atomicLongAdapter(final TypeAdapter<Number> longAdapter) {
    return new TypeAdapter<AtomicLong>() {
      @Override
      public void write(JsonWriter out, AtomicLong value) throws IOException {
        longAdapter.write(out, value.get());
      }

      @Override
      public AtomicLong read(JsonReader in) throws IOException {
        Number value = longAdapter.read(in);
        return new AtomicLong(value.longValue());
      }
    }.nullSafe();
  }

  private static TypeAdapter<AtomicLongArray> atomicLongArrayAdapter(
      final TypeAdapter<Number> longAdapter) {
    return new TypeAdapter<AtomicLongArray>() {
      @Override
      public void write(JsonWriter out, AtomicLongArray value) throws IOException {
        out.beginArray();
        for (int i = 0, length = value.length(); i < length; i++) {
          longAdapter.write(out, value.get(i));
        }
        out.endArray();
      }

      @Override
      public AtomicLongArray read(JsonReader in) throws IOException {
        List<Long> list = new ArrayList<>();
        in.beginArray();
        while (in.hasNext()) {
          long value = longAdapter.read(in).longValue();
          list.add(value);
        }
        in.endArray();
        int length = list.size();
        AtomicLongArray array = new AtomicLongArray(length);
        for (int i = 0; i < length; ++i) {
          array.set(i, list.get(i));
        }
        return array;
      }
    }.nullSafe();
  }

  /**
   * Returns the type adapter for {@code type}.
   *
   * <p>When calling this method concurrently from multiple threads and requesting an adapter for
   * the same type this method may return different {@code TypeAdapter} instances. However, that
   * should normally not be an issue because {@code TypeAdapter} implementations are supposed to be
   * stateless.
   *
   * @throws IllegalArgumentException if this Gson instance cannot serialize and deserialize {@code
   *     type}.
   */
  public <T> TypeAdapter<T> getAdapter(TypeToken<T> type) {
    Objects.requireNonNull(type, "type must not be null");
    TypeAdapter<?> cached = typeTokenCache.get(type);
    if (cached != null) {
      @SuppressWarnings("unchecked")
      TypeAdapter<T> adapter = (TypeAdapter<T>) cached;
      return adapter;
    }

    Map<TypeToken<?>, TypeAdapter<?>> threadCalls = threadLocalAdapterResults.get();
    boolean isInitialAdapterRequest = false;
    if (threadCalls == null) {
      threadCalls = new HashMap<>();
      threadLocalAdapterResults.set(threadCalls);
      isInitialAdapterRequest = true;
    } else {
      // the key and value type parameters always agree
      @SuppressWarnings("unchecked")
      TypeAdapter<T> ongoingCall = (TypeAdapter<T>) threadCalls.get(type);
      if (ongoingCall != null) {
        return ongoingCall;
      }
    }

    TypeAdapter<T> candidate = null;
    try {
      FutureTypeAdapter<T> call = new FutureTypeAdapter<>();
      threadCalls.put(type, call);

      for (TypeAdapterFactory factory : factories) {
        candidate = factory.create(this, type);
        if (candidate != null) {
          call.setDelegate(candidate);
          // Replace future adapter with actual adapter
          threadCalls.put(type, candidate);
          break;
        }
      }
    } finally {
      if (isInitialAdapterRequest) {
        threadLocalAdapterResults.remove();
      }
    }

    if (candidate == null) {
      throw new IllegalArgumentException(
          "GSON (" + GsonBuildConfig.VERSION + ") cannot handle " + type);
    }

    if (isInitialAdapterRequest) {
      /*
       * Publish resolved adapters to all threads
       * Can only do this for the initial request because cyclic dependency TypeA -> TypeB -> TypeA
       * would otherwise publish adapter for TypeB which uses not yet resolved adapter for TypeA
       * See https://github.com/google/gson/issues/625
       */
      typeTokenCache.putAll(threadCalls);
    }
    return candidate;
  }

  /**
   * Returns the type adapter for {@code type}.
   *
   * @throws IllegalArgumentException if this Gson instance cannot serialize and deserialize {@code
   *     type}.
   */
  public <T> TypeAdapter<T> getAdapter(Class<T> type) {
    return getAdapter(TypeToken.get(type));
  }

  /**
   * This method is used to get an alternate type adapter for the specified type. This is used to
   * access a type adapter that is overridden by a {@link TypeAdapterFactory} that you may have
   * registered. This feature is typically used when you want to register a type adapter that does a
   * little bit of work but then delegates further processing to the Gson default type adapter. Here
   * is an example:
   *
   * <p>Let's say we want to write a type adapter that counts the number of objects being read from
   * or written to JSON. We can achieve this by writing a type adapter factory that uses the {@code
   * getDelegateAdapter} method:
   *
   * <pre>{@code
   * class StatsTypeAdapterFactory implements TypeAdapterFactory {
   *   public int numReads = 0;
   *   public int numWrites = 0;
   *   public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
   *     final TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
   *     return new TypeAdapter<T>() {
   *       public void write(JsonWriter out, T value) throws IOException {
   *         ++numWrites;
   *         delegate.write(out, value);
   *       }
   *       public T read(JsonReader in) throws IOException {
   *         ++numReads;
   *         return delegate.read(in);
   *       }
   *     };
   *   }
   * }
   * }</pre>
   *
   * This factory can now be used like this:
   *
   * <pre>{@code
   * StatsTypeAdapterFactory stats = new StatsTypeAdapterFactory();
   * Gson gson = new GsonBuilder().registerTypeAdapterFactory(stats).create();
   * // Call gson.toJson() and fromJson methods on objects
   * System.out.println("Num JSON reads: " + stats.numReads);
   * System.out.println("Num JSON writes: " + stats.numWrites);
   * }</pre>
   *
   * Note that this call will skip all factories registered before {@code skipPast}. In case of
   * multiple TypeAdapterFactories registered it is up to the caller of this function to ensure that
   * the order of registration does not prevent this method from reaching a factory they would
   * expect to reply from this call. Note that since you can not override the type adapter factories
   * for some types, see {@link GsonBuilder#registerTypeAdapter(Type, Object)}, our stats factory
   * will not count the number of instances of those types that will be read or written.
   *
   * <p>If {@code skipPast} is a factory which has neither been registered on the {@link
   * GsonBuilder} nor specified with the {@link JsonAdapter @JsonAdapter} annotation on a class,
   * then this method behaves as if {@link #getAdapter(TypeToken)} had been called. This also means
   * that for fields with {@code @JsonAdapter} annotation this method behaves normally like {@code
   * getAdapter} (except for corner cases where a custom {@link InstanceCreator} is used to create
   * an instance of the factory).
   *
   * @param skipPast The type adapter factory that needs to be skipped while searching for a
   *     matching type adapter. In most cases, you should just pass <i>this</i> (the type adapter
   *     factory from where {@code getDelegateAdapter} method is being invoked).
   * @param type Type for which the delegate adapter is being searched for.
   * @since 2.2
   */
  public <T> TypeAdapter<T> getDelegateAdapter(TypeAdapterFactory skipPast, TypeToken<T> type) {
    Objects.requireNonNull(skipPast, "skipPast must not be null");
    Objects.requireNonNull(type, "type must not be null");

    if (jsonAdapterFactory.isClassJsonAdapterFactory(type, skipPast)) {
      skipPast = jsonAdapterFactory;
    }

    boolean skipPastFound = false;
    for (TypeAdapterFactory factory : factories) {
      if (!skipPastFound) {
        if (factory == skipPast) {
          skipPastFound = true;
        }
        continue;
      }

      TypeAdapter<T> candidate = factory.create(this, type);
      if (candidate != null) {
        return candidate;
      }
    }

    if (skipPastFound) {
      throw new IllegalArgumentException("GSON cannot serialize or deserialize " + type);
    } else {
      // Probably a factory from @JsonAdapter on a field
      return getAdapter(type);
    }
  }




  /**
   * Returns a new JSON writer configured for the settings on this Gson instance.
   *
   * <p>The following settings are considered:
   *
   * <ul>
   *   <li>{@link GsonBuilder#disableHtmlEscaping()}
   *   <li>{@link GsonBuilder#generateNonExecutableJson()}
   *   <li>{@link GsonBuilder#serializeNulls()}
   *   <li>{@link GsonBuilder#setStrictness(Strictness)}. If no {@linkplain
   *       GsonBuilder#setStrictness(Strictness) explicit strictness has been set} the created
   *       writer will have a strictness of {@link Strictness#LEGACY_STRICT}. Otherwise, the
   *       strictness of the {@code Gson} instance will be used for the created writer.
   *   <li>{@link GsonBuilder#setPrettyPrinting()}
   *   <li>{@link GsonBuilder#setFormattingStyle(FormattingStyle)}
   * </ul>
   */
  public JsonWriter newJsonWriter(Writer writer) throws IOException {
    if (generateNonExecutableJson) {
      writer.write(JSON_NON_EXECUTABLE_PREFIX);
    }
    JsonWriter jsonWriter = new JsonWriter(writer);
    jsonWriter.setFormattingStyle(formattingStyle);
    jsonWriter.setHtmlSafe(htmlSafe);
    jsonWriter.setStrictness(strictness == null ? Strictness.LEGACY_STRICT : strictness);
    jsonWriter.setSerializeNulls(serializeNulls);
    return jsonWriter;
  }




  /**
   * Returns a new JSON reader configured for the settings on this Gson instance.
   *
   * <p>The following settings are considered:
   *
   * <ul>
   *   <li>{@link GsonBuilder#setStrictness(Strictness)}. If no {@linkplain
   *       GsonBuilder#setStrictness(Strictness) explicit strictness has been set} the created
   *       reader will have a strictness of {@link Strictness#LEGACY_STRICT}. Otherwise, the
   *       strictness of the {@code Gson} instance will be used for the created reader.
   * </ul>
   */
  public JsonReader newJsonReader(Reader reader) {
    JsonReader jsonReader = new JsonReader(reader);
    jsonReader.setStrictness(this.strictness == null ? Strictness.LEGACY_STRICT : this.strictness);
    return jsonReader;
  }


  @Override
  public String toString() {
    return "{serializeNulls:"
        + serializeNulls
        + ",factories:"
        + factories
        + ",instanceCreators:"
        + constructorConstructor
        + "}";
  }
}
