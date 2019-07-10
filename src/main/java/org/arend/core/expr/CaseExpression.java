package org.arend.core.expr;

import org.arend.core.context.param.DependentLink;
import org.arend.core.elimtree.ElimTree;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.util.Decision;

import java.util.List;

public class CaseExpression extends Expression {
  private final DependentLink myParameters;
  private final Expression myResultType;
  private final Expression myResultTypeLevel;
  private final ElimTree myElimTree;
  private final List<Expression> myArguments;

  public CaseExpression(DependentLink parameters, Expression resultType, Expression resultTypeLevel, ElimTree elimTree, List<Expression> arguments) {
    myParameters = parameters;
    myElimTree = elimTree;
    myResultType = resultType;
    myResultTypeLevel = resultTypeLevel;
    myArguments = arguments;
  }

  public DependentLink getParameters() {
    return myParameters;
  }

  public Expression getResultType() {
    return myResultType;
  }

  public Expression getResultTypeLevel() {
    return myResultTypeLevel;
  }

  public ElimTree getElimTree() {
    return myElimTree;
  }

  public List<Expression> getArguments() {
    return myArguments;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitCase(this, params);
  }

  @Override
  public Decision isWHNF(boolean normalizing) {
    return myElimTree.isWHNF(myArguments, normalizing);
  }

  @Override
  public Expression getStuckExpression(boolean normalizing) {
    return myElimTree.getStuckExpression(myArguments, this, normalizing);
  }
}
