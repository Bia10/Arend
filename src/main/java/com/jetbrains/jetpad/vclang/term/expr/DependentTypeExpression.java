package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.UntypedDependentLink;
import com.jetbrains.jetpad.vclang.term.definition.TypeUniverseNew;
import com.jetbrains.jetpad.vclang.term.definition.Universe;

public abstract class DependentTypeExpression extends Expression {
  private final DependentLink myLink;

  public DependentTypeExpression(DependentLink link) {
    myLink = link;
  }

  public DependentLink getParameters() {
    return myLink;
  }

  public TypeUniverseNew getUniverse() {
    DependentLink link = myLink;
    TypeUniverseNew universe = null;

    while (link.hasNext()) {
      if (!(link instanceof UntypedDependentLink)) {
        UniverseExpression type = link.getType().getType().toUniverse();
        if (type == null) return null;
        if (universe == null) {
          universe = type.getUniverse();
        } else {
          //Universe.CompareResult cmp = universe.compare(type.getUniverse());
          //if (cmp == null) return null;
          universe = universe.max(type.getUniverse());
        }
      }
      link = link.getNext();
    }

    return universe;
  }

  @Override
  public Expression getType() {
    TypeUniverseNew universe = getUniverse();
    return universe == null ? null : new UniverseExpression(universe);
  }

  @Override
  public DependentTypeExpression toDependentType() {
    return this;
  }
}
