package org.arend.typechecking.error.local;

import org.arend.error.doc.DocFactory;
import org.arend.error.doc.LineDoc;
import org.arend.naming.reference.ClassReferable;
import org.arend.naming.reference.FieldReferable;
import org.arend.term.concrete.Concrete;
import org.arend.term.prettyprint.PrettyPrinterConfig;

import java.util.Collection;
import java.util.stream.Collectors;

import static org.arend.error.doc.DocFactory.*;

public class FieldsImplementationError extends TypecheckingError {
  public boolean alreadyImplemented;
  public ClassReferable classReferable;
  public Collection<? extends FieldReferable> fields;

  public FieldsImplementationError(boolean alreadyImplemented, ClassReferable classReferable, Collection<? extends FieldReferable> fields, Concrete.SourceNode cause) {
    super("The following fields are " + (alreadyImplemented ? "already" : "not") + " implemented: ", cause);
    this.alreadyImplemented = alreadyImplemented;
    this.classReferable = classReferable;
    this.fields = fields;
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig src) {
    return hList(text(message), hSep(text(", "), fields.stream().map(DocFactory::refDoc).collect(Collectors.toList())));
  }
}
