package io.github.tanyaofei.copier;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * Extra properties
 *
 * @author tanyaofei
 * @since 2025/6/19
 **/
public class Properties implements Map<String, Object> {

    private final static Properties EMPTY = new Properties(Collections.emptyMap());

    private final Map<String, Object> delegate;

    private Properties(@Nonnull Map<String, Object> delegate) {
        this.delegate = delegate;
    }

    @Nonnull
    public static Properties of() {
        return EMPTY;
    }

    @Nonnull
    public static Properties of(@Nonnull String k, @Nullable Object v) {
        return ofN(k, v);
    }

    @Nonnull
    public static Properties of(@Nonnull String k1, @Nullable Object v1, @Nonnull String k2, @Nullable Object v2) {
        return ofN(k1, v1, k2, v2);
    }

    @Nonnull
    public static Properties of(@Nonnull String k1, @Nullable Object v1, @Nonnull String k2, @Nullable Object v2, @Nonnull String k3, @Nullable Object v3) {
        return ofN(k1, v1, k2, v2, k3, v3);
    }

    @Nonnull
    public static Properties of(@Nonnull String k1, @Nullable Object v1, @Nonnull String k2, @Nullable Object v2, @Nonnull String k3, @Nullable Object v3, @Nonnull String k4, @Nullable Object v4) {
        return ofN(k1, v1, k2, v2, k3, v3, k4, v4);
    }

    @Nonnull
    public static Properties of(@Nonnull String k1, @Nullable Object v1, @Nonnull String k2, @Nullable Object v2, @Nonnull String k3, @Nullable Object v3, @Nonnull String k4, @Nullable Object v4, @Nonnull String k5, @Nullable Object v5) {
        return ofN(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5);
    }

    @Nonnull
    public static Properties of(@Nonnull String k1, @Nullable Object v1, @Nonnull String k2, @Nullable Object v2, @Nonnull String k3, @Nullable Object v3, @Nonnull String k4, @Nullable Object v4, @Nonnull String k5, @Nullable Object v5, @Nonnull String k6, @Nullable Object v6) {
        return ofN(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6);
    }

    @Nonnull
    public static Properties of(@Nonnull String k1, @Nullable Object v1, @Nonnull String k2, @Nullable Object v2, @Nonnull String k3, @Nullable Object v3, @Nonnull String k4, @Nullable Object v4, @Nonnull String k5, @Nullable Object v5, @Nonnull String k6, @Nullable Object v6, @Nonnull String k7, @Nullable Object v7) {
        return ofN(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7);
    }

    @Nonnull
    public static Properties of(@Nonnull String k1, @Nullable Object v1, @Nonnull String k2, @Nullable Object v2, @Nonnull String k3, @Nullable Object v3, @Nonnull String k4, @Nullable Object v4, @Nonnull String k5, @Nullable Object v5, @Nonnull String k6, @Nullable Object v6, @Nonnull String k7, @Nullable Object v7, @Nonnull String k8, @Nullable Object v8) {
        return ofN(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8);
    }

    @Nonnull
    public static Properties of(@Nonnull String k1, @Nullable Object v1, @Nonnull String k2, @Nullable Object v2, @Nonnull String k3, @Nullable Object v3, @Nonnull String k4, @Nullable Object v4, @Nonnull String k5, @Nullable Object v5, @Nonnull String k6, @Nullable Object v6, @Nonnull String k7, @Nullable Object v7, @Nonnull String k8, @Nullable Object v8, @Nonnull String k9, @Nullable Object v9) {
        return ofN(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9);
    }

    @Nonnull
    public static Properties of(@Nonnull String k1, @Nullable Object v1, @Nonnull String k2, @Nullable Object v2, @Nonnull String k3, @Nullable Object v3, @Nonnull String k4, @Nullable Object v4, @Nonnull String k5, @Nullable Object v5, @Nonnull String k6, @Nullable Object v6, @Nonnull String k7, @Nullable Object v7, @Nonnull String k8, @Nullable Object v8, @Nonnull String k9, @Nullable Object v9, @Nonnull String k10, @Nullable Object v10) {
        return ofN(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9, k10, v10);
    }

    @Nonnull
    public static Properties ofProperties(
            @Nonnull Property... kvs
    ) {
        if (kvs.length == 0) {
            return Properties.of();
        }

        var delegate = new HashMap<String, Object>(kvs.length / 2);
        for (var kv : kvs) {
            delegate.put(Objects.requireNonNull(kv.key(), "key"), kv.value());
        }
        return new Properties(Collections.unmodifiableMap(delegate));
    }

    @Nonnull
    private static Properties ofN(@Nonnull Object... kv) {
        if (kv.length == 0) {
            return Properties.of();
        }

        int size = kv.length;
        var delegate = new HashMap<String, Object>(size / 2);
        for (int i = 0; i < size; i += 2) {
            var k = Objects.requireNonNull((String) kv[i], "key");
            var v = kv[i + 1];
            delegate.put(k, v);
        }
        return new Properties(Collections.unmodifiableMap(delegate));
    }

    public @Nonnull PropertiesConverter converter() {
        return new PropertiesConverter();
    }

    @Nonnull
    @Override
    public Set<Entry<String, Object>> entrySet() {
        return this.delegate.entrySet();
    }

    @Override
    public Object get(Object key) {
        return this.delegate.get(key);
    }

    @Override
    public Object put(String key, Object value) {
        return this.delegate.put(key, value);
    }

    @Override
    public Object remove(Object key) {
        return this.delegate.remove(key);
    }

    @Override
    public void putAll(@Nonnull Map<? extends String, ?> m) {
        this.delegate.putAll(m);
    }

    @Override
    public void clear() {
        this.delegate.clear();
    }

    @Override
    public Object getOrDefault(Object key, Object defaultValue) {
        return this.delegate.getOrDefault(key, defaultValue);
    }

    @Override
    @Nonnull
    public Collection<Object> values() {
        return this.delegate.values();
    }

    @Override
    @Nonnull
    public Set<String> keySet() {
        return this.delegate.keySet();
    }

    @Override
    public boolean containsValue(Object value) {
        return this.delegate.containsValue(value);
    }

    @Override
    public int size() {
        return this.delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return this.delegate.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return this.delegate.containsKey(key);
    }

    @Override
    public void forEach(BiConsumer<? super String, ? super Object> action) {
        this.delegate.forEach(action);
    }

    public record Property(

            @Nonnull
            String key,

            @Nullable
            Object value

    ) {
    }

    public final class PropertiesConverter implements Converter {

        @Nullable
        @Override
        public Object convert(@Nullable Object value, @Nonnull String property, @Nonnull Class<?> propertyType, boolean assignable) {
            if (assignable) {
                return value;
            }
            return null;
        }

        @Override
        public Object provide(@Nullable Object source, @Nonnull String property, @Nonnull Class<?> propertyType) {
            var value = Properties.this.get(property);
            if (value == null) {
                return null;
            }

            if (propertyType.isAssignableFrom(value.getClass())) {
                return value;
            }

            return null;
        }

    }


}
