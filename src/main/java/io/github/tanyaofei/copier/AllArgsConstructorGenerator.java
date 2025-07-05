
package io.github.tanyaofei.copier;

import jakarta.annotation.Nonnull;
import net.sf.cglib.core.ClassEmitter;
import net.sf.cglib.core.Signature;
import org.objectweb.asm.Type;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author tanyaofei
 * @since 2025/6/19
 **/
class AllArgsConstructorGenerator extends Generator {

    public AllArgsConstructorGenerator(@Nonnull Class<?> source, @Nonnull Class<?> target, boolean useConverter, @Nonnull MethodHandles.Lookup lookup) {
        super(source, target, useConverter, lookup);
    }

    @Override
    @SuppressWarnings("DuplicatedCode")
    protected void generateCopyMethod(@Nonnull ClassEmitter ce) {
        var e = ce.begin_method(Constants.ACC_PUBLIC | Constants.ACC_FINAL, Constants.SIGNATURE_COPIER$copy, null);

        var sourceType = Type.getType(this.source);
        var targetType = Type.getType(this.target);

        var sourceProps = Arrays.stream(Property.forClassReaders(this.source)).collect(Collectors.toMap(Property::name, Function.identity()));
        var targetProps = Property.forClassConstructors(this.target);

        var source = e.make_local();
        e.load_arg(0);
        e.checkcast(sourceType);
        e.store_local(source);

        e.new_instance(targetType);
        e.dup();

        for (var targetProp : targetProps) {
            var sourceProp = sourceProps.get(targetProp.name());
            if (sourceProp == null) {
                if (this.useConverter) {
                    e.load_arg(1);
                    e.load_arg(0);
                    e.push(targetProp.name());
                    e.visitLdcInsn(this.getBoxType(targetProp.propertyAsmType()));
                    e.invoke_interface(Constants.TYPE_CONVERTER, Constants.SIGNATURE_CONVERTER$provide);
                    e.unbox_or_zero(targetProp.propertyAsmType());
                } else {
                    e.zero_or_null(targetProp.propertyAsmType());
                }
            } else {
                if (this.useConverter) {
                    e.load_arg(1);
                    e.load_local(source);
                    e.invoke_virtual(sourceType, sourceProp.signature());
                    e.box(sourceProp.propertyAsmType());
                    e.push(targetProp.name());
                    e.visitLdcInsn(this.getBoxType(targetProp.propertyAsmType()));
                    e.push(this.isAssignable(sourceProp.type(), targetProp.type()));
                    e.invoke_interface(Constants.TYPE_CONVERTER, Constants.SIGNATURE_CONVERTER$convert);
                    e.unbox_or_zero(targetProp.propertyAsmType());
                } else {
                    if (this.isAssignable(sourceProp.type(), targetProp.type())) {
                        e.load_local(source);
                        e.invoke_virtual(sourceType, sourceProp.signature());
                    } else {
                        e.zero_or_null(targetProp.propertyAsmType());
                    }
                }
            }
        }

        e.invoke_constructor(targetType, new Signature(
                Constants.CONSTRUCTOR_NAME,
                Type.VOID_TYPE,
                Arrays.stream(targetProps).map(Property::propertyAsmType).toArray(Type[]::new)
        ));
        e.return_value();

        e.end_method();
    }

    @Override
    protected void generateCopyIntoMethod(@Nonnull ClassEmitter ce) {


    }


}
