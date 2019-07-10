package org.arend.core.expr;

import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.util.Decision;

import java.util.List;

public class TupleExpression extends Expression {
  private final List<Expression> myFields;
  private final SigmaExpression myType;

  public TupleExpression(List<Expression> fields, SigmaExpression type) {
    myFields = fields;
    myType = type;
  }

  public List<Expression> getFields() {
    return myFields;
  }

  public SigmaExpression getSigmaType() {
    return myType;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitTuple(this, params);
  }

  @Override
  public Decision isWHNF(boolean normalizing) {
    return Decision.YES;
  }

  @Override
  public Expression getStuckExpression(boolean normalizing) {
    return null;
  }
}
