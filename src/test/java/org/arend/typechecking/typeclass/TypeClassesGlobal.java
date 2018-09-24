package org.arend.typechecking.typeclass;

import org.arend.typechecking.TypeCheckingTestCase;
import org.arend.typechecking.error.CycleError;
import org.junit.Test;

import static org.arend.typechecking.Matchers.instanceInference;
import static org.arend.typechecking.Matchers.typeMismatchError;
import static org.hamcrest.core.IsInstanceOf.instanceOf;

public class TypeClassesGlobal extends TypeCheckingTestCase {
  @Test
  public void inferInstance() {
    typeCheckModule(
      "\\class X (A : \\Type0) {\n" +
      "  | B : A -> \\Type0\n" +
      "}\n" +
      "\\instance Nat-X : X | A => Nat | B => \\lam n => Nat\n" +
      "\\func f => B 0");
  }

  @Test
  public void instanceClassWithArg() {
    typeCheckModule(
      "\\class X (A : \\Type0) {\n" +
      "  | B : A -> \\Type0\n" +
      "}\n" +
      "\\instance Nat-X : X Nat | B => \\lam n => Nat\n" +
      "\\func f => B 0");
  }

  @Test
  public void incorrectInstance() {
    typeCheckModule(
      "\\class X (A : \\Type0) {\n" +
      "  | B : A -> \\Type0\n" +
      "}\n" +
      "\\func f : \\Type1 => Nat\n" +
      "\\instance Nat-X : X | A => f | B => \\lam n => Nat", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void differentInstances() {
    typeCheckModule(
      "\\class X (A : \\Type0) {\n" +
      "  | B : A -> \\Type0\n" +
      "}\n" +
      "\\instance Nat-X : X | A => Nat | B => \\lam n => Nat\n" +
      "\\instance I-X : X | A => I | B => \\lam n => Nat -> Nat\n" +
      "\\func f => B 0\n" +
      "\\func g => B left");
  }

  @Test
  public void localInstance() {
    typeCheckModule(
      "\\class X (A : \\Type0) {\n" +
      "  | B : A -> \\Set0\n" +
      "}\n" +
      "\\instance Nat-X : X | A => Nat | B => \\lam n => Nat -> Nat\n" +
      "\\func f (y : X { | A => Nat }) => B 0\n" +
      "\\func test : Nat = Nat => path (\\lam _ => f (\\new X { | A => Nat | B => \\lam _ => Nat }))");
  }

  @Test
  public void transitiveInferInstance() {
    typeCheckModule(
      "\\class X (A : \\Type0) {\n" +
      "  | B : A -> \\Type0\n" +
      "}\n" +
      "\\instance Nat-X : X | A => Nat | B => \\lam n => Nat -> Nat\n" +
      "\\func f {A : \\Type0} {y : X { | A => A } } (a : A) => B a\n" +
      "\\func g => f 0");
  }

  @Test
  public void transitiveInferInstance2() {
    typeCheckModule(
      "\\class X (A : \\Type0) {\n" +
      "  | B : A -> \\Type0\n" +
      "}\n" +
      "\\instance Nat-X : X | A => Nat | B => \\lam n => Nat -> Nat\n" +
      "\\func f {x : X} (a : x.A) => B a\n" +
      "\\func g => f 0");
  }

  @Test
  public void mutuallyRecursiveInstance() {
    typeCheckModule(
      "\\instance Nat-X : X | A => Nat | B => \\lam _ => Nat\n" +
      "\\data D | c\n" +
      "\\func g {x : X { | A => Nat }} => \\Prop\n" +
      "\\func f : \\Set0 => g\n" +
      "\\instance D-X : X | A => D | B => \\lam _ => f\n" +
      "\\class X (A : \\Type0) {\n" +
      "  | B : A -> \\Type0\n" +
      "}");
  }

  @Test
  public void mutuallyRecursiveInstanceError() {
    typeCheckModule(
      "\\instance Nat-X : X | A => Nat | B => \\lam _ => Nat\n" +
      "\\data D | c\n" +
      "\\func g {x : X { | A => Nat }} => \\Prop\n" +
      "\\instance D-X : X | A => D | B => \\lam _ => f\n" +
      "\\func f : \\Set0 => g\n" +
      "\\class X (A : \\Type0) {\n" +
      "  | B : A -> \\Type0\n" +
      "}", 1);
    assertThatErrorsAre(instanceOf(CycleError.class));
  }

  // We do not check for duplicate global instances currently
  @Test
  public void duplicateInstance() {
    typeCheckModule(
      "\\class X (A : \\Type0) {\n" +
      "  | B : A -> \\Type0\n" +
      "}\n" +
      "\\data D\n" +
      "\\instance D-X : X | A => D | B => \\lam n => D\n" +
      "\\instance D-Y : X | A => D | B => \\lam n => D -> D");
    // assertThatErrorsAre(duplicateInstanceError());
  }

  @Test
  public void withoutClassifyingFieldError() {
    typeCheckModule(
      "\\class A { | n : Nat }\n" +
      "\\func f => n", 1);
    assertThatErrorsAre(instanceInference(getDefinition("A")));
  }

  @Test
  public void withoutClassifyingField() {
    typeCheckModule(
      "\\class A { | n : Nat }\n" +
      "\\instance a0 : A | n => 0\n" +
      "\\instance a1 : A | n => 1\n" +
      "\\func f : n = n => path (\\lam _ => 1)");
  }

  @Test
  public void checkClassifyingExpressionArguments() {
    typeCheckModule(
      "\\data Data (A : \\Set)\n" +
      "\\data D\n" +
      "\\data D'\n" +
      "\\class B (X : \\Set) { | foo : X -> X }\n" +
      "\\instance B-inst : B (Data D) | foo => \\lam x => x\n" +
      "\\func f (x : Data D') => foo x", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void classifyingFieldIsNotADefCall() {
    typeCheckModule(
      "\\class B (n : Nat)\n" +
      "\\instance B-inst {x : Nat} : B x", 1);
  }

  @Test
  public void superClassInstance() {
    typeCheckModule(
      "\\class A { | x : Nat }\n" +
      "\\class B \\extends A\n" +
      "\\instance B-inst : B | x => 0\n" +
      "\\func f => x");
  }

  @Test
  public void superClassWithClassifyingFieldInstance() {
    typeCheckModule(
      "\\class A (C : \\Set) { | c : C }\n" +
      "\\class B \\extends A\n" +
      "\\instance B-inst : B Nat | c => 0\n" +
      "\\func f : Nat => c");
  }

  @Test
  public void superClassWithClassifyingFieldNoInstance() {
    typeCheckModule(
      "\\class A (C : \\Set) { | c : C }\n" +
      "\\class B \\extends A\n" +
      "\\data Nat'\n" +
      "\\instance B-inst : B Nat | c => 0\n" +
      "\\func f : Nat' => c", 1);
    assertThatErrorsAre(instanceInference(getDefinition("A")));
  }

  @Test
  public void instanceProp() {
    typeCheckModule(
      "\\class A (C : \\Type) { | c : C }\n" +
      "\\data D : \\Prop\n" +
      "\\instance aaa : A \\Prop | c => D\n" +
      "\\func f1 : \\Prop => c\n" +
      "\\func f2 : \\Set => c\n" +
      "\\func f3 : \\Type => c");
  }

  @Test
  public void instanceSet() {
    typeCheckModule(
      "\\class A (C : \\Type) { | c : C }\n" +
      "\\instance a : A \\Set | c => Nat\n" +
      "\\func f1 : \\Set => c\n" +
      "\\func f2 : \\Type => c");
  }

  @Test
  public void instanceType() {
    typeCheckModule(
      "\\class A (C : \\Type) { | c : C }\n" +
      "\\data D | con \\Set0\n" +
      "\\instance a : A \\Type1 | c => D\n" +
      "\\func f : \\1-Type => c");
  }

  @Test
  public void instanceTypeError() {
    typeCheckModule(
      "\\class A (C : \\Type) { | c : C }\n" +
      "\\instance a : A \\Set | c => Nat\n" +
      "\\func f : \\Prop => c", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void instanceTypeError2() {
    typeCheckModule(
      "\\class A (C : \\Type) { | c : C }\n" +
      "\\data D | con \\Set0\n" +
      "\\instance a : A \\0-Type1 | c => D", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void instanceTypeError3() {
    typeCheckModule(
      "\\class A (C : \\Type) { | c : C }\n" +
      "\\data D | con \\Set0\n" +
      "\\instance a : A \\1-Type1 | c => D\n" +
      "\\func f : \\1-Type0 => c", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void instanceTypeCheckTest() {
    typeCheckModule(
      "\\class A (C : \\Type) { | c : C | n : Nat }\n" +
      "\\instance a : A \\Set | c => Nat | n => 0\n" +
      "\\func f {c : A { | C => \\Set | n => 1 }} => 2\n" +
      "\\func g => f", 1);
    assertThatErrorsAre(instanceInference(getDefinition("A")));
  }
}