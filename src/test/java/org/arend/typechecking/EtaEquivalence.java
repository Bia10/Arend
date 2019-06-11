package org.arend.typechecking;

import org.arend.core.definition.FunctionDefinition;
import org.arend.core.elimtree.LeafElimTree;
import org.arend.core.expr.ClassCallExpression;
import org.arend.core.expr.LamExpression;
import org.arend.core.expr.NewExpression;
import org.arend.core.expr.visitor.CompareVisitor;
import org.arend.typechecking.implicitargs.equations.DummyEquations;
import org.arend.typechecking.implicitargs.equations.Equations;
import org.junit.Test;

import static org.arend.ExpressionFactory.Ref;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EtaEquivalence extends TypeCheckingTestCase {
  @Test
  public void classesEq() {
    typeCheckModule(
        "\\record Foo { | foo : Nat | bar : Nat }\n" +
        "\\func f (l : Foo) => \\new Foo { | foo => l.foo | bar => l.bar }");
    assertTrue(getDefinition("f") instanceof FunctionDefinition);
    FunctionDefinition f = (FunctionDefinition) getDefinition("f");
    assertTrue(CompareVisitor.compare(DummyEquations.getInstance(), Equations.CMP.EQ, new NewExpression((ClassCallExpression) f.getResultType()), Ref(f.getParameters()), null));
    assertTrue(CompareVisitor.compare(DummyEquations.getInstance(), Equations.CMP.EQ, Ref(f.getParameters()), new NewExpression((ClassCallExpression) f.getResultType()), null));
  }

  @Test
  public void classesGe() {
    typeCheckModule(
        "\\record Foo { | foo : Nat | bar : Nat }\n" +
        "\\func f (l : Foo) => \\new Foo { | foo => l.foo | bar => l.bar }");
    assertTrue(getDefinition("f") instanceof FunctionDefinition);
    FunctionDefinition f = (FunctionDefinition) getDefinition("f");
    assertTrue(CompareVisitor.compare(DummyEquations.getInstance(), Equations.CMP.GE, new NewExpression((ClassCallExpression) f.getResultType()), Ref(f.getParameters()), null));
    assertTrue(CompareVisitor.compare(DummyEquations.getInstance(), Equations.CMP.GE, Ref(f.getParameters()), new NewExpression((ClassCallExpression) f.getResultType()), null));
  }

  @Test
  public void classesLe() {
    typeCheckModule(
        "\\record Foo { | foo : Nat | bar : Nat }\n" +
        "\\func f (l : Foo) => \\new Foo { | foo => l.foo | bar => l.bar }");
    assertTrue(getDefinition("f") instanceof FunctionDefinition);
    FunctionDefinition f = (FunctionDefinition) getDefinition("f");
    assertTrue(CompareVisitor.compare(DummyEquations.getInstance(), Equations.CMP.LE, new NewExpression((ClassCallExpression) f.getResultType()), Ref(f.getParameters()), null));
    assertTrue(CompareVisitor.compare(DummyEquations.getInstance(), Equations.CMP.LE, Ref(f.getParameters()), new NewExpression((ClassCallExpression) f.getResultType()), null));
  }

  @Test
  public void pathEtaLeftTest() {
    typeCheckDef("\\func test (p : 0 = 0) => (\\lam (x : path (\\lam i => p @ i) = p) => x) (path (\\lam _ => p))");
  }

  @Test
  public void pathEtaRightTest() {
    typeCheckDef("\\func test (p : 0 = 0) => (\\lam (x : p = p) => x) (path (\\lam _ => path (\\lam i => p @ i)))");
  }

  @Test
  public void pathEtaLeftTestLevel() {
    typeCheckDef("\\func test (p : Nat = Nat) => (\\lam (x : path (\\lam i => p @ i) = p) => x) (path (\\lam _ => p))");
  }

  @Test
  public void pathEtaRightTestLevel() {
    typeCheckDef("\\func test (p : Nat = Nat) => (\\lam (x : p = p) => x) (path (\\lam _ => path (\\lam i => p @ i)))");
  }

  @Test
  public void onlyDefCallsExpanded() {
    FunctionDefinition fun = (FunctionDefinition) typeCheckDef("\\func f (x : Nat -> Nat) => x");
    assertFalse(((LeafElimTree) fun.getBody()).getExpression().isInstance(LamExpression.class));
  }

  @Test
  public void emptyClass() {
    typeCheckModule(
      "\\record Unit\n" +
      "\\func f (x : Unit) : x = \\new Unit => path (\\lam _ => x)");
  }

  @Test
  public void emptyClass2() {
    typeCheckModule(
      "\\record Unit\n" +
      "\\func f (x y : Unit) : x = y => path (\\lam _ => x)");
  }

  @Test
  public void emptyClass3() {
    typeCheckModule(
      "\\record C { | n : Nat }\n" +
      "\\func f (x y : C { | n => 0 }) : x = y => path (\\lam _ => x)");
  }

  @Test
  public void emptyClass4a() {
    typeCheckModule(
      "\\record C { | n : Nat }\n" +
      "\\func f (x : C { | n => 0 }) (y : C) : x = y => path (\\lam _ => x)", 1);
  }

  @Test
  public void emptyClass4b() {
    typeCheckModule(
      "\\record C { | n : Nat }\n" +
      "\\func f (x : C) (y : C { | n => 0 }) : x = y => path (\\lam _ => x)", 1);
  }

  @Test
  public void unitClass() {
    typeCheckModule(
      "\\record C { | n : Nat }\n" +
      "\\func f (x : C) (y : C { | n => x.n }) : x = y => path (\\lam _ => x)");
  }

  @Test
  public void unitClass2() {
    typeCheckModule(
      "\\record C { | n : Nat | m : Nat }\n" +
      "\\func f (x : C { | n => 3 }) (y : C { | n => 3 | m => x.m }) : x = y => path (\\lam _ => y)");
  }

  @Test
  public void unitClass3() {
    typeCheckModule(
      "\\record C {| m : Nat | n : Nat  }\n" +
      "\\func f (x : C) (y : C { | m => x.m }) : x = y => path (\\lam _ => y)", 1);
  }

  @Test
  public void unitClass4() {
    typeCheckModule(
      "\\record C { | n : Nat | m : Nat }\n" +
      "\\func f (x : C { | n => 3 }) (y : C { | n => 3 | m => x.n }) : x = y => path (\\lam _ => y)", 1);
  }

  @Test
  public void sigmaTest() {
    typeCheckModule("\\func f (x : \\Sigma Nat Nat) : x = (x.1,x.2) => path (\\lam _ => x)");
  }

  @Test
  public void sigmaUnitTest() {
    typeCheckModule("\\func f (x y : \\Sigma) : x = y => path (\\lam _ => x)");
  }
}
