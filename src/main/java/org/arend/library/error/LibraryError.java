package org.arend.library.error;

import org.arend.error.Error;
import org.arend.error.GeneralError;
import org.arend.error.doc.DocFactory;
import org.arend.error.doc.LineDoc;
import org.arend.module.ModulePath;
import org.arend.term.prettyprint.PrettyPrinterConfig;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.arend.error.doc.DocFactory.*;

public class LibraryError extends GeneralError {
  public final Stream<String> libraryNames;

  private LibraryError(String message, Stream<String> libraryNames) {
    super(Level.ERROR, message);
    this.libraryNames = libraryNames;
  }

  private LibraryError(Error.Level level, String message, Stream<String> libraryNames) {
    super(level, message);
    this.libraryNames = libraryNames;
  }

  public static LibraryError cyclic(Stream<String> libraryNames) {
    return new LibraryError("Cyclic dependencies in libraries", libraryNames);
  }

  public static LibraryError notFound(String libraryName) {
    return new LibraryError("Library not found", Stream.of(libraryName));
  }

  public static LibraryError unloadDuringLoading(Stream<String> libraryNames) {
    return new LibraryError("Cannot unload a library while loading other libraries", libraryNames);
  }

  public static LibraryError illegalName(String libraryName) {
    return new LibraryError("Illegal library name or path", Stream.of(libraryName));
  }

  public static LibraryError moduleNotFound(ModulePath modulePath, String libraryName) {
    return new LibraryError("Module '" + modulePath + "' is not found in library", Stream.of(libraryName));
  }

  public static LibraryError incorrectLibrary(String libraryName) {
    return new LibraryError(Level.INFO, "Library cannot be typechecked", Stream.of(libraryName));
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig src) {
    List<LineDoc> libraryDocs = libraryNames.map(DocFactory::text).collect(Collectors.toList());
    return libraryDocs.isEmpty() ? text(message) : hList(text(message), text(": "), hSep(text(", "), libraryDocs));
  }
}
