package org.arend.error.doc;

import org.arend.naming.reference.Referable;

public class ReferenceDoc extends LineDoc {
  private final Referable myReference;

  ReferenceDoc(Referable reference) {
    assert reference != null;
    myReference = reference;
  }

  public Referable getReference() {
    return myReference;
  }

  @Override
  public <P, R> R accept(DocVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitReference(this, params);
  }

  @Override
  public int getWidth() {
    return myReference.textRepresentation().length();
  }

  @Override
  public boolean isEmpty() {
    return myReference.textRepresentation().isEmpty();
  }
}
