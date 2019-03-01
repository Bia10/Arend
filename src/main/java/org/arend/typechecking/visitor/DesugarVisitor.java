package org.arend.typechecking.visitor;

import org.arend.naming.error.WrongReferable;
import org.arend.naming.reference.*;
import org.arend.naming.scope.ClassFieldImplScope;
import org.arend.prelude.Prelude;
import org.arend.term.concrete.BaseConcreteExpressionVisitor;
import org.arend.term.concrete.Concrete;
import org.arend.term.concrete.ConcreteDefinitionVisitor;
import org.arend.typechecking.error.LocalErrorReporter;
import org.arend.typechecking.error.local.ArgInferenceError;
import org.arend.typechecking.error.local.LocalError;
import org.arend.typechecking.error.local.TypecheckingError;
import org.arend.typechecking.typecheckable.provider.ConcreteProvider;

import java.util.*;

public class DesugarVisitor extends BaseConcreteExpressionVisitor<Void> implements ConcreteDefinitionVisitor<Void, Void> {
  private final ConcreteProvider myConcreteProvider;
  private final LocalErrorReporter myErrorReporter;

  private DesugarVisitor(ConcreteProvider concreteProvider, LocalErrorReporter errorReporter) {
    myConcreteProvider = concreteProvider;
    myErrorReporter = errorReporter;
  }

  public static void desugar(Concrete.Definition definition, ConcreteProvider concreteProvider, LocalErrorReporter errorReporter) {
    definition.accept(new DesugarVisitor(concreteProvider, errorReporter), null);
  }

  private Set<LocatedReferable> getClassFields(ClassReferable classRef) {
    Set<LocatedReferable> fields = new HashSet<>();
    new ClassFieldImplScope(classRef, false).find(ref -> {
      if (ref instanceof LocatedReferable) {
        fields.add((LocatedReferable) ref);
      }
      return false;
    });
    return fields;
  }

  private Referable checkDefinition(Concrete.Definition def) {
    if (def.enclosingClass != null) {
      Referable thisParameter = new HiddenLocalReferable("this");
      def.accept(new ClassFieldChecker(thisParameter, def.enclosingClass, myConcreteProvider, getClassFields(def.enclosingClass), null, myErrorReporter), null);
      return thisParameter;
    } else {
      return null;
    }
  }

  @Override
  public Void visitFunction(Concrete.FunctionDefinition def, Void params) {
    if (def.enclosingClass != null && def.getKind().isUse()) {
      myErrorReporter.report(new TypecheckingError("\\use is not allowed inside a class definition", def));
      def.enclosingClass = null;
    }

    // Add this parameter
    Referable thisParameter = checkDefinition(def);
    if (thisParameter != null) {
      def.getParameters().add(0, new Concrete.TelescopeParameter(def.getData(), false, Collections.singletonList(thisParameter), new Concrete.ReferenceExpression(def.getData(), def.enclosingClass)));
      if (def.getBody().getEliminatedReferences().isEmpty()) {
        for (Concrete.FunctionClause clause : def.getBody().getClauses()) {
          clause.getPatterns().add(0, new Concrete.NamePattern(clause.getData(), false, thisParameter, null));
        }
      }
    }

    // Process expressions
    super.visitFunction(def, null);
    return null;
  }

  @Override
  public Void visitData(Concrete.DataDefinition def, Void params) {
    // Add this parameter
    Referable thisParameter = checkDefinition(def);
    if (thisParameter != null) {
      def.getParameters().add(0, new Concrete.TelescopeParameter(def.getData(), false, Collections.singletonList(thisParameter), new Concrete.ReferenceExpression(def.getData(), def.enclosingClass)));
      if (def.getEliminatedReferences() != null && def.getEliminatedReferences().isEmpty()) {
        for (Concrete.ConstructorClause clause : def.getConstructorClauses()) {
          clause.getPatterns().add(0, new Concrete.NamePattern(clause.getData(), false, thisParameter, null));
        }
      }
    }

    // Process expressions
    super.visitData(def, null);
    return null;
  }

  @Override
  public Void visitClass(Concrete.ClassDefinition def, Void params) {
    Set<LocatedReferable> fields = getClassFields(def.getData());

    Set<TCReferable> futureFields = new HashSet<>();
    for (Concrete.ClassField field : def.getFields()) {
      futureFields.add(field.getData());
    }

    // Check fields
    ClassFieldChecker classFieldChecker = new ClassFieldChecker(null, def.getData(), myConcreteProvider, fields, futureFields, myErrorReporter);
    Concrete.Expression previousType = null;
    for (int i = 0; i < def.getFields().size(); i++) {
      Concrete.ClassField classField = def.getFields().get(i);
      Concrete.Expression fieldType = classField.getResultType();
      Referable thisParameter = new HiddenLocalReferable("this");
      classFieldChecker.setThisParameter(thisParameter);
      if (fieldType == previousType && classField.getParameters().isEmpty()) {
        classField.getParameters().addAll(def.getFields().get(i - 1).getParameters());
        classField.setResultType(def.getFields().get(i - 1).getResultType());
        classField.setResultTypeLevel(def.getFields().get(i - 1).getResultTypeLevel());
      } else {
        previousType = classField.getParameters().isEmpty() ? fieldType : null;
        classFieldChecker.visitParameters(classField.getParameters(), null);
        classField.getParameters().add(0, new Concrete.TelescopeParameter(fieldType.getData(), false, Collections.singletonList(thisParameter), new Concrete.ReferenceExpression(fieldType.getData(), def.getData())));
        classField.setResultType(fieldType.accept(classFieldChecker, null));
        if (classField.getResultTypeLevel() != null) {
          classField.setResultTypeLevel(classField.getResultTypeLevel().accept(classFieldChecker, null));
        }
      }
      futureFields.remove(classField.getData());
    }

    // Process expressions
    super.visitClass(def, null);

    // Check implementations
    for (Concrete.ClassFieldImpl classFieldImpl : def.getImplementations()) {
      Concrete.Expression impl = classFieldImpl.implementation;
      Referable thisParameter = new HiddenLocalReferable("this");
      classFieldChecker.setThisParameter(thisParameter);
      classFieldImpl.implementation = new Concrete.LamExpression(impl.getData(), Collections.singletonList(new Concrete.TelescopeParameter(impl.getData(), false, Collections.singletonList(thisParameter), new Concrete.ReferenceExpression(impl.getData(), def.getData()))), impl.accept(classFieldChecker, null));
    }

    return null;
  }

  @Override
  public Concrete.Expression visitApp(Concrete.AppExpression expr, Void params) {
    // Convert class call with arguments to class extension.
    expr = (Concrete.AppExpression) super.visitApp(expr, null);
    Concrete.Expression fun = expr.getFunction();
    if (fun instanceof Concrete.ReferenceExpression) {
      Referable ref = ((Concrete.ReferenceExpression) fun).getReferent();
      if (ref instanceof ClassReferable && ((ClassReferable) ref).getUnderlyingTypecheckable() != null) {
        List<Concrete.ClassFieldImpl> classFieldImpls = new ArrayList<>();
        Set<FieldReferable> notImplementedFields = ClassReferable.Helper.getNotImplementedFields((ClassReferable) ref);
        Iterator<FieldReferable> it = notImplementedFields.iterator();
        for (int i = 0; i < expr.getArguments().size(); i++) {
          if (!it.hasNext()) {
            myErrorReporter.report(new TypecheckingError("Too many arguments. Class '" + ref.textRepresentation() + "' " + (notImplementedFields.isEmpty() ? "does not have fields" : "has only " + ArgInferenceError.number(notImplementedFields.size(), "field")), expr.getArguments().get(i).expression));
            break;
          }

          FieldReferable fieldRef = it.next();
          boolean fieldExplicit = fieldRef.isExplicitField();
          if (fieldExplicit && !expr.getArguments().get(i).isExplicit()) {
            myErrorReporter.report(new TypecheckingError("Expected an explicit argument", expr.getArguments().get(i).expression));
            while (i < expr.getArguments().size() && !expr.getArguments().get(i).isExplicit()) {
              i++;
            }
            if (i == expr.getArguments().size()) {
              break;
            }
          }

          Concrete.Expression argument = expr.getArguments().get(i).expression;
          if (fieldExplicit == expr.getArguments().get(i).isExplicit()) {
            classFieldImpls.add(new Concrete.ClassFieldImpl(argument.getData(), fieldRef, argument, Collections.emptyList()));
          } else {
            classFieldImpls.add(new Concrete.ClassFieldImpl(argument.getData(), fieldRef, new Concrete.HoleExpression(argument.getData()), Collections.emptyList()));
            i--;
          }
        }
        return Concrete.ClassExtExpression.make(expr.getData(), fun, classFieldImpls);
      }
    }
    return expr;
  }

  private void visitPatterns(List<Concrete.Pattern> patterns) {
    for (int i = 0; i < patterns.size(); i++) {
      Concrete.Pattern pattern = patterns.get(i);
      if (pattern instanceof Concrete.TuplePattern) {
        visitPatterns(((Concrete.TuplePattern) pattern).getPatterns());
      } else if (pattern instanceof Concrete.ConstructorPattern) {
        visitPatterns(((Concrete.ConstructorPattern) pattern).getPatterns());
      } else if (pattern instanceof Concrete.NumberPattern) {
        int n = ((Concrete.NumberPattern) pattern).getNumber();
        Concrete.Pattern newPattern = new Concrete.ConstructorPattern(pattern.getData(), true, Prelude.ZERO.getReferable(), Collections.emptyList(), n == 0 ? pattern.getAsReferables() : Collections.emptyList());
        boolean isNegative = n < 0;
        if (isNegative) {
          n = -n;
        }
        if (n > Concrete.NumberPattern.MAX_VALUE) {
          n = Concrete.NumberPattern.MAX_VALUE;
        }
        if (n == Concrete.NumberPattern.MAX_VALUE) {
          myErrorReporter.report(new TypecheckingError("Value too big", pattern));
        }
        for (int j = 0; j < n; j++) {
          newPattern = new Concrete.ConstructorPattern(pattern.getData(), true, Prelude.SUC.getReferable(), Collections.singletonList(newPattern), !isNegative && j == n - 1 ? pattern.getAsReferables() : Collections.emptyList());
        }
        if (isNegative) {
          newPattern = new Concrete.ConstructorPattern(pattern.getData(), true, Prelude.NEG.getReferable(), Collections.singletonList(newPattern), pattern.getAsReferables());
        }
        if (!pattern.isExplicit()) {
          newPattern.setExplicit(false);
        }
        patterns.set(i, newPattern);
      }
    }
  }

  @Override
  protected void visitClause(Concrete.Clause clause, Void params) {
    if (clause.getPatterns() != null) {
      visitPatterns(clause.getPatterns());
    }
    super.visitClause(clause, null);
  }

  private void visitClassFieldImpl(Concrete.ClassFieldImpl classFieldImpl, List<Concrete.ClassFieldImpl> result) {
    if (classFieldImpl.implementation != null) {
      classFieldImpl.implementation = classFieldImpl.implementation.accept(this, null);
      result.add(classFieldImpl);
    } else {
      boolean ok = true;
      if (classFieldImpl.getImplementedField() instanceof ClassReferable) {
        for (Concrete.ClassFieldImpl subClassFieldImpl : classFieldImpl.subClassFieldImpls) {
          visitClassFieldImpl(subClassFieldImpl, result);
        }
      } else if (classFieldImpl.getImplementedField() instanceof TypedReferable) {
        ClassReferable classRef = ((TypedReferable) classFieldImpl.getImplementedField()).getTypeClassReference();
        if (classRef != null) {
          visitClassFieldImpls(classFieldImpl.subClassFieldImpls, null);
          Object data = classFieldImpl.getData();
          classFieldImpl.implementation = new Concrete.NewExpression(data, Concrete.ClassExtExpression.make(data, new Concrete.ReferenceExpression(data, classRef), classFieldImpl.subClassFieldImpls));
          result.add(classFieldImpl);
        } else {
          ok = false;
        }
      } else {
        ok = false;
      }

      if (!ok) {
        LocalError error = new WrongReferable("Expected either a class or a field which has a class as its type", classFieldImpl.getImplementedField(), classFieldImpl);
        myErrorReporter.report(error);
        classFieldImpl.implementation = new Concrete.ErrorHoleExpression(classFieldImpl.getData(), error);
        result.add(classFieldImpl);
      }
    }
  }

  @Override
  protected void visitClassFieldImpls(List<Concrete.ClassFieldImpl> classFieldImpls, Void params) {
    if (classFieldImpls.isEmpty()) {
      return;
    }
    List<Concrete.ClassFieldImpl> originalClassFieldImpls = new ArrayList<>(classFieldImpls);
    classFieldImpls.clear();
    for (Concrete.ClassFieldImpl classFieldImpl : originalClassFieldImpls) {
      visitClassFieldImpl(classFieldImpl, classFieldImpls);
    }
  }
}
