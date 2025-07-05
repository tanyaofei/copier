
package io.github.tanyaofei.copier;

import jakarta.annotation.Nonnull;
import net.sf.cglib.core.ClassEmitter;
import org.objectweb.asm.Type;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author tanyaofei
 * @since 2025/6/19
 **/
class NoArgsConstructorGenerator extends Generator {

    public NoArgsConstructorGenerator(@Nonnull Class<?> source, @Nonnull Class<?> target, boolean useConverter, @Nonnull MethodHandles.Lookup lookup) {
        super(source, target, useConverter, lookup);
    }

    @Override
    protected void generateCopyMethod(@Nonnull ClassEmitter ce) {
        var e = ce.begin_method(Constants.ACC_PUBLIC | Constants.ACC_FINAL, Constants.SIGNATURE_COPIER$copy, null);

        var thisType = ce.getClassType();
        var targetType = Type.getType(this.target);

        var target = e.make_local();
        e.new_instance(targetType);
        e.dup();
        e.invoke_constructor(targetType);
        e.store_local(target);

        e.load_this();          // this
        e.load_arg(0);    // source
        e.load_local(target);   // target
        e.load_arg(1);    // properties
        e.invoke_virtual(thisType, Constants.SIGNATURE_COPIER$copyInto);        // this.copyInto(source, target, properties)

        e.load_local(target);
        e.return_value();
        e.end_method();
    }

    @Override
    @SuppressWarnings("DuplicatedCode")
    protected void generateCopyIntoMethod(@Nonnull ClassEmitter ce) {
        var e = ce.begin_method(Constants.ACC_PUBLIC | Constants.ACC_FINAL, Constants.SIGNATURE_COPIER$copyInto, null);

        var sourceType = Type.getType(this.source);
        var targetType = Type.getType(this.target);

        var sourceProps = Arrays.stream(Property.forClassReaders(this.source)).collect(Collectors.toMap(Property::name, Function.identity()));
        var targetProps = Property.forClassWriters(this.target);

        var source = e.make_local();
        e.load_arg(0);
        e.checkcast(sourceType);
        e.store_local(source);

        var target = e.make_local();
        e.load_arg(1);
        e.checkcast(targetType);
        e.store_local(target);

        for (var targetProp : targetProps) {
            var sourceProp = sourceProps.get(targetProp.name());
            if (sourceProp == null) {
                // 字段不存在, 使用 Converter.provide()
                if (!this.useConverter) {
                    continue;
                }

                e.load_local(target);
                e.load_arg(2);
                e.load_arg(0);
                e.push(targetProp.name());
                e.visitLdcInsn(this.getBoxType(targetProp.propertyAsmType()));
                e.invoke_interface(Constants.TYPE_CONVERTER, Constants.SIGNATURE_CONVERTER$provide);
                e.unbox_or_zero(targetProp.propertyAsmType());
            } else {
                if (this.useConverter) {
                    e.load_local(target);
                    e.load_arg(2);
                    e.load_local(source);
                    e.invoke_virtual(sourceType, sourceProp.signature());
                    e.box(sourceProp.propertyAsmType());
                    e.push(targetProp.name());
                    e.visitLdcInsn(this.getBoxType(targetProp.propertyAsmType()));
                    e.push(this.isAssignable(sourceProp.type(), targetProp.type()));
                    e.invoke_interface(Constants.TYPE_CONVERTER, Constants.SIGNATURE_CONVERTER$convert);
                    e.unbox_or_zero(targetProp.propertyAsmType());
                } else {
                    if (!this.isAssignable(sourceProp.type(), targetProp.type())) {
                        continue;
                    }
                    e.load_local(target);
                    e.load_local(source);
                    e.invoke_virtual(sourceType, sourceProp.signature());
                }
            }

            e.invoke_virtual(targetType, targetProp.signature());
            if (targetProp.method().getReturnType() != void.class) {
                e.pop();
            }
        }

        e.return_value();
        e.end_method();
    }
}
