package org.arend.typechecking.implicitargs;

import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import static org.arend.typechecking.Matchers.goal;

public class InferenceTest extends TypeCheckingTestCase {
  @Test
  public void doubleGoalTest() {
    String text =
        "\\data B | t | f\n" +
        "\\data D Nat | con Nat\n" +
        "\\func h : D 0 => con (\\case t \\with {\n" +
        "  | t => {?}\n" +
        "  | f => {?}\n" +
        "})";
    typeCheckModule(text, 2);
    assertThatErrorsAre(goal(0), goal(0));
  }

  @Test
  public void inferTailConstructor1() {
    typeCheckModule(
        "\\data D (n : Nat) {k : Nat} (m : Nat) | con\n" +
        "\\func f : D 0 {1} 2 => con");
  }

  @Test
  public void inferTailConstructor2() {
    typeCheckModule(
        "\\data D (n : Nat) {k : Nat} (m : Nat) | con\n" +
        "\\func f : \\Pi {m : Nat} -> D 0 {1} m => con");
  }

  @Test
  public void inferTailConstructor3() {
    typeCheckModule(
        "\\data D (n : Nat) {k : Nat} (m : Nat) | con\n" +
        "\\func f : \\Pi {k m : Nat} -> D 0 {k} m => con");
  }

  @Test
  public void inferTailConstructor4() {
    typeCheckModule(
        "\\data D (n : Nat) {k : Nat} (m : Nat) | con\n" +
        "\\func f : \\Pi {n k m : Nat} -> D n {k} m => con");
  }

  @Test
  public void inferConstructor1a() {
    typeCheckModule(
        "\\data D (n : Nat) {k : Nat} (m : Nat) | con (k = m)\n" +
        "\\func f => con {0} (path (\\lam _ => 1))");
  }

  @Test
  public void inferConstructor1b() {
    typeCheckModule(
        "\\data D (n : Nat) {k : Nat} (m : Nat) | con (n = k) (k = m)\n" +
        "\\func idp {A : \\Type} {a : A} => path (\\lam _ => a)\n" +
        "\\func f => con {0} idp idp");
  }

  @Test
  public void inferConstructor2a() {
    typeCheckModule(
        "\\data D (n : Nat) {k : Nat} (m : Nat) | con (k = m)\n" +
        "\\func f => con {0} (path (\\lam _ => 1))");
  }

  @Test
  public void inferConstructor2b() {
    typeCheckModule(
        "\\data D (n : Nat) {k : Nat} (m : Nat) | con (n = k) (k = m)\n" +
        "\\func idp {A : \\Type} {a : A} => path (\\lam _ => a)\n" +
        "\\func f => con {0} idp idp");
  }

  @Test
  public void inferConstructor3() {
    typeCheckModule(
        "\\data D (n : Nat) {k : Nat} (m : Nat) | con (n = k) (k = n)\n" +
        "\\func idp {A : \\Type0} {a : A} => path (\\lam _ => a)\n" +
        "\\func f => con {0} idp idp", 1);
  }

  @Test
  public void equations() {
    typeCheckModule(
        "\\data E (A B : \\Type) | inl A | inr B\n" +
        "\\data Empty : \\Prop\n" +
        "\\func neg (A : \\Type) => A -> Empty\n" +
        "\\func test (A : \\Type) => E (neg A) A"
    );
  }

  @Test
  public void classIntersection() {
    typeCheckModule(
      "\\class C (x y : Nat)\n" +
      "\\func f (c1 : C 1) (c2 : C 2) (p : c1 = c2) => p");
  }

  @Test
  public void tailInField() {
    typeCheckModule(
      "\\class C (E : \\Type)\n" +
      "  | \\infix 4 ~ : E -> E -> \\Type\n" +
      "  | refl {e : E} : e ~ e\n" +
      "  | \\infixr 8 * {x y z : E} : x ~ y -> y ~ z -> x ~ z\n" +
      "\\func f {X : C} {x y : X} (p : x ~ y) => p * refl");
  }
}
