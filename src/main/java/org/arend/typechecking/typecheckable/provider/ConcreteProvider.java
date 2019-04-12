package org.arend.typechecking.typecheckable.provider;

import org.arend.naming.reference.ClassReferable;
import org.arend.naming.reference.GlobalReferable;
import org.arend.term.concrete.Concrete;

import javax.annotation.Nullable;

public interface ConcreteProvider extends PartialConcreteProvider {
  @Nullable Concrete.ReferableDefinition getConcrete(GlobalReferable referable);
  @Nullable Concrete.FunctionDefinition getConcreteFunction(GlobalReferable referable);
  @Nullable Concrete.FunctionDefinition getConcreteInstance(GlobalReferable referable);
  @Nullable Concrete.ClassDefinition getConcreteClass(ClassReferable referable);
  @Nullable Concrete.DataDefinition getConcreteData(GlobalReferable referable);

  @Override
  @Nullable
  default Concrete.ReferenceExpression getInstanceTypeReference(GlobalReferable instance) {
    Concrete.FunctionDefinition concreteInstance = getConcreteInstance(instance);
    return concreteInstance == null ? null : concreteInstance.getReferenceExpressionInType();
  }

  @Override
  default boolean isInstance(GlobalReferable ref) {
    return getConcreteInstance(ref) != null;
  }

  @Override
  default boolean isUse(GlobalReferable ref) {
    Concrete.FunctionDefinition func = getConcreteFunction(ref);
    return func != null && func.getKind().isUse();
  }

  @Override
  default boolean isData(GlobalReferable ref) {
    return getConcreteData(ref) != null;
  }
}
