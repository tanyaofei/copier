package io.github.tanyaofei.copier;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import net.sf.cglib.core.ClassEmitter;
import net.sf.cglib.core.EmitUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.invoke.MethodHandles;
import java.lang.ref.ReferenceQueue;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author tanyaofei
 * @since 2025/7/1
 **/
abstract class Generator {

    private final static String DEBUG_LOCATION = System.getProperty("copier.debugLocation");

    private final static Set<String> RESERVED_CLASS_NAMES = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final static ReferenceQueue<Class<?>> REFERENCE_QUEUE = new ReferenceQueue<>();

    final static Map<CacheKey, IdentifierWeakReference<CacheKey, Class<?>>> CACHE = new ConcurrentHashMap<>();

    static {
        var cleaner = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    var ref = (IdentifierWeakReference<CacheKey, ?>) REFERENCE_QUEUE.remove();
                    CACHE.remove(ref.getIdentifier());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        cleaner.setName("Copier Class Cleaner");
        cleaner.setDaemon(true);
        cleaner.start();
    }

    /**
     * source class
     */
    @Nonnull
    protected final Class<?> source;

    /**
     * target class
     */
    @Nonnull
    protected final Class<?> target;

    /**
     * Whether if using converter
     */
    protected final boolean useConverter;

    /**
     * class name without hashcode
     */
    @Nonnull
    private final String classNamePrefix;

    /**
     * Lookup instance for defining Copier class
     */
    @Nonnull
    private final MethodHandles.Lookup lookup;

    public Generator(@Nonnull Class<?> source, @Nonnull Class<?> target, boolean useConverter, @Nonnull MethodHandles.Lookup lookup) {
        this.source = source;
        this.target = target;
        this.useConverter = useConverter;
        this.lookup = lookup;
        this.classNamePrefix = generateClassNamePrefix(source, target);
    }

    protected void generateClass(@Nonnull ClassVisitor v, @Nonnull String className) {
        var ce = new ClassEmitter(v);
        ce.begin_class(Constants.V17,
                       Constants.ACC_PUBLIC | Constants.ACC_FINAL,
                       className,
                       Constants.TYPE_COPIER,
                       null,
                       Constants.SOURCE_FILE
        );

        EmitUtils.null_constructor(ce);
        this.generateCopyMethod(ce);
        this.generateCopyIntoMethod(ce);
        ce.end_class();
    }

    /**
     * Generate copy method
     *
     * @param ce ClassEmitter
     * @see Copier#copy(Object, Converter)
     */
    protected abstract void generateCopyMethod(@Nonnull ClassEmitter ce);

    /**
     * Generate copyInto method
     *
     * @param ce ClassEmitter
     * @see Copier#copyInto(Object, Object, Converter)
     */
    protected abstract void generateCopyIntoMethod(@Nonnull ClassEmitter ce);

    /**
     * Generate class and instance
     *
     * @return Copier instance
     */
    @Nonnull
    public Copier create() {
        var key = new CacheKey(this.source, this.target, this.useConverter);

        var clz = getClassFromCache(key);

        if (clz == null) {
            synchronized (CACHE) {
                clz = getClassFromCache(key);
                if (clz == null) {
                    clz = this.generate(key);
                }
                CACHE.put(key, new IdentifierWeakReference<>(key, clz, REFERENCE_QUEUE));
            }
        }

        try {
            return (Copier) clz.getConstructor().newInstance();
        } catch (Throwable e) {
            throw new CopierException("Error instantiate a Copier", e);
        }
    }

    /**
     * Generate class
     *
     * @param key cache key
     * @return Copier class
     */
    @Nonnull
    protected Class<?> generate(@Nonnull CacheKey key) {
        byte[] bytecode;
        try {
            var cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            var className = generateClassName(this.classNamePrefix, key, new DefaultNamingPolicy());
            this.generateClass(cw, className);

            bytecode = cw.toByteArray();
            if (DEBUG_LOCATION != null && !DEBUG_LOCATION.isEmpty()) {
                dumpClassFile(bytecode);
            }
        } catch (Exception e) {
            throw CopierException.wrap("Error generating Copier bytecode", e);
        }

        try {
            return Reflections.defineClass(this.lookup, bytecode);
        } catch (Exception e) {
            throw CopierException.wrap("Error loading Copier class", e);
        }
    }

    @Nonnull
    private static String generateClassNamePrefix(@Nonnull Class<?> source, @Nonnull Class<?> target) {
        var pkg = target.getPackageName();
        var name = new StringBuilder();
        if (!pkg.isEmpty()) {
            name.append(pkg).append(".");
        }
        name.append(target.getSimpleName()).append("Copier$$").append(source.getSimpleName());
        return name.toString();
    }


    @Nonnull
    private static String generateClassName(@Nonnull String prefix, @Nonnull Object key, @Nonnull NamingPolicy policy) {
        return policy.getName(prefix, key, RESERVED_CLASS_NAMES::add);
    }

    protected boolean isAssignable(@Nonnull PropertyType source, @Nonnull PropertyType target) {
        if (Collection.class.isAssignableFrom(source.type())) {
            return TypeUtils.isAssignable(source.genericType(), target.genericType());
        } else if (Optional.class == source.type()) {
            return TypeUtils.isAssignable(source.genericType(), target.genericType());
        }
        return target.type().isAssignableFrom(source.type());
    }

    @Nonnull
    protected Type getBoxType(@Nonnull Type type) {
        return switch (type.getSort()) {
            case Type.BOOLEAN -> Constants.TYPE_BOOLEAN;
            case Type.BYTE -> Constants.TYPE_BYTE;
            case Type.CHAR -> Constants.TYPE_CHARACTER;
            case Type.SHORT -> Constants.TYPE_SHORT;
            case Type.INT -> Constants.TYPE_INTEGER;
            case Type.LONG -> Constants.TYPE_LONG;
            case Type.FLOAT -> Constants.TYPE_FLOAT;
            case Type.DOUBLE -> Constants.TYPE_DOUBLE;
            default -> type;
        };
    }

    /**
     * Dump bytecode to a *.class file
     *
     * @param bytecode Java bytecode
     */
    private static void dumpClassFile(@Nonnull byte[] bytecode) {
        var reader = new ClassReader(bytecode);
        var className = reader.getClassName();

        var filepath = DEBUG_LOCATION + File.separatorChar + className + ".class";

        var folder = new File(filepath).getParentFile();
        if (!folder.exists() && !folder.mkdirs()) {
            throw new CopierException("Failed to create debug location folder: " + folder.getPath());
        }

        try (var out = new BufferedOutputStream(new FileOutputStream(filepath))) {
            out.write(bytecode);
        } catch (Exception e) {
            throw new CopierException("Error dumping bytecode to disk", e);
        }
    }

    @Nullable
    private static Class<?> getClassFromCache(@Nonnull CacheKey key) {
        var ref = CACHE.get(key);
        if (ref == null) {
            return null;
        }
        return ref.get();
    }


}
