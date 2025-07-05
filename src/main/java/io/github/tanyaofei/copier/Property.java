package io.github.tanyaofei.copier;

import jakarta.annotation.Nonnull;
import net.sf.cglib.core.ReflectUtils;
import net.sf.cglib.core.Signature;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * @param name   Property Name
 * @param type   Property Type
 * @param method Getter/Setter
 * @author tanyaofei
 * @since 2025/6/19
 **/
record Property(

        @Nonnull
        String name,

        @Nonnull
        PropertyType type,

        @Nonnull
        Method method


) {

    public @Nonnull Class<?> propertyType() {
        return type.type();
    }

    public @Nonnull java.lang.reflect.Type propertyGenericType() {
        return type.genericType();
    }

    public @Nonnull Type propertyAsmType() {
        return type.asmType();
    }

    public @Nonnull Signature signature() {
        return new Signature(
                this.method.getName(),
                Type.getReturnType(this.method),
                Type.getArgumentTypes(this.method)
        );
    }

    @SuppressWarnings("unchecked")
    public static Property[] forClassReaders(@Nonnull Class<?> type) {
        if (type.isRecord()) {
            return forRecordReaders((Class<? extends Record>) type);
        } else {
            return forBeanReaders(type);
        }
    }

    @SuppressWarnings("unchecked")
    public static Property[] forClassWriters(@Nonnull Class<?> type) {
        if (type.isRecord()) {
            return forRecordWriters((Class<? extends Record>) type);
        } else {
            return forBeanWriter(type);
        }
    }

    @SuppressWarnings("unchecked")
    public static Property[] forClassConstructors(@Nonnull Class<?> type) {
        if (!type.isRecord()) {
            throw new UnsupportedOperationException("cannot find constructor properties for non-record class: " + type.getName());
        }

        return forRecordReaders((Class<? extends Record>) type);
    }

    private static Property[] forRecordReaders(@Nonnull Class<? extends Record> type) {
        var components = type.getRecordComponents();
        int size = components.length;
        var properties = new Property[size];
        for (int i = 0; i < size; i++) {
            var component = components[i];
            properties[i] = new Property(
                    component.getName(),
                    new PropertyType(component.getType(), component.getGenericType()),
                    component.getAccessor()
            );
        }
        return properties;
    }

    private static Property[] forRecordWriters(@Nonnull Class<? extends Record> type) {
        throw new UnsupportedOperationException("record has no writers");
    }

    private static Property[] forBeanReaders(@Nonnull Class<?> type) {
        var descriptors = ReflectUtils.getBeanGetters(type);
        int size = descriptors.length;
        var properties = new Property[size];
        for (int i = 0; i < size; i++) {
            var descriptor = descriptors[i];
            var name = descriptor.getName();
            var reader = descriptor.getReadMethod();
            var genericType = reader.getGenericReturnType();
            properties[i] = new Property(
                    name,
                    new PropertyType(descriptor.getPropertyType(), genericType),
                    reader
            );
        }
        return properties;
    }

    static Property[] forBeanWriter(@Nonnull Class<?> type) {
        var descriptors = ReflectUtils.getBeanProperties(type);
        int size = descriptors.length;
        var properties = new ArrayList<Property>(size);
        for (var descriptor : descriptors) {
            var name = descriptor.getName();
            var writer = descriptor.getWriteMethod();

            if (writer == null) {
                // lombok @Accessor(chain=true)
                var writerName = "set" + name.substring(0, 1).toUpperCase() + (name.length() > 1 ? name.substring(1) : "");
                try {
                    writer = type.getMethod(writerName, descriptor.getPropertyType());
                } catch (NoSuchMethodException ignore) {

                }
            }


            if (writer == null) {
                // java bean, uAge -> setuAge()
                if (name.length() > 1 && Character.isLowerCase(name.charAt(0)) && Character.isUpperCase(name.charAt(1))) {
                    var writerName = "set" + name;
                    try {
                        writer = type.getMethod(writerName, descriptor.getPropertyType());
                    } catch (NoSuchMethodException ignore1) {

                    }
                }
            }

            if (writer == null) {
                continue;
            }

            var genericType = writer.getGenericParameterTypes()[0];
            properties.add(new Property(
                    name,
                    new PropertyType(descriptor.getPropertyType(), genericType),
                    writer
            ));
        }
        return properties.toArray(new Property[0]);
    }


}
