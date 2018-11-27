package org.arend.source;

import org.arend.error.ErrorReporter;
import org.arend.library.LibraryManager;
import org.arend.library.SourceLibrary;
import org.arend.module.ModulePath;
import org.arend.module.error.ModuleNotFoundError;
import org.arend.module.scopeprovider.ModuleScopeProvider;
import org.arend.naming.reference.converter.ReferableConverter;
import org.arend.typechecking.instance.provider.InstanceProviderSet;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Contains all necessary information for source loading.
 */
public final class SourceLoader {
  private final SourceLibrary myLibrary;
  private final ReferableConverter myReferableConverter;
  private final LibraryManager myLibraryManager;
  private final Map<ModulePath, SourceType> myLoadedModules = new HashMap<>();
  private final Map<ModulePath, BinarySource> myLoadingBinaryModules = new HashMap<>();
  private final Map<ModulePath, Source> myLoadingRawModules = new HashMap<>();

  private enum SourceType { RAW, BINARY, BINARY_FAIL }

  public SourceLoader(SourceLibrary library, LibraryManager libraryManager) {
    myLibrary = library;
    myLibraryManager = libraryManager;
    myReferableConverter = myLibrary.getReferableConverter();
  }

  public SourceLibrary getLibrary() {
    return myLibrary;
  }

  public ReferableConverter getReferableConverter() {
    return myReferableConverter;
  }

  public ModuleScopeProvider getModuleScopeProvider() {
    return myLibraryManager.getModuleScopeProvider();
  }

  public InstanceProviderSet getInstanceProviderSet() {
    return myLibraryManager.getInstanceProviderSet();
  }

  public ErrorReporter getTypecheckingErrorReporter() {
    return myLibraryManager.getTypecheckingErrorReporter();
  }

  public ErrorReporter getLibraryErrorReporter() {
    return myLibraryManager.getLibraryErrorReporter();
  }

  /**
   * Loads the structure of the source and its dependencies.
   *
   * @param modulePath  a module to load.
   * @return true if a binary source is available or if the raw source was successfully loaded, false otherwise.
   */
  public boolean preloadRaw(ModulePath modulePath) {
    if (myLoadedModules.containsKey(modulePath)) {
      return true;
    }
    if (myLoadingRawModules.containsKey(modulePath)) {
      return true;
    }

    Source rawSource = myLibrary.getRawSource(modulePath);
    boolean rawSourceIsAvailable = rawSource != null && rawSource.isAvailable();

    if (!rawSourceIsAvailable) {
      getLibraryErrorReporter().report(new ModuleNotFoundError(modulePath));
      return false;
    }

    myLoadedModules.put(modulePath, SourceType.RAW);
    myLoadingRawModules.put(modulePath, rawSource);
    if (!rawSource.preload(this)) {
      myLoadingRawModules.remove(modulePath);
      return false;
    }

    return true;
  }

  /**
   * Loads raw sources that were preloaded.
   */
  public void loadRawSources() {
    while (!myLoadingRawModules.isEmpty()) {
      for (Iterator<Source> it = myLoadingRawModules.values().iterator(); it.hasNext(); ) {
        Source source = it.next();
        Source.LoadResult loadResult = source.load(this);
        if (loadResult != Source.LoadResult.CONTINUE) {
          it.remove();
        }
      }
    }
  }

  /**
   * Loads a binary source.
   *
   * @param modulePath  a module to load.
   * @return true if the source was successfully loaded, false otherwise.
   */
  public boolean loadBinary(ModulePath modulePath) {
    return preloadBinary(modulePath) && fillInBinary(modulePath);
  }

  boolean fillInBinary(ModulePath modulePath) {
    BinarySource binarySource = myLoadingBinaryModules.remove(modulePath);
    if (binarySource != null) {
      Source.LoadResult result;
      do {
        result = binarySource.load(this);
      } while (result == Source.LoadResult.CONTINUE);

      if (result != Source.LoadResult.SUCCESS) {
        myLoadedModules.put(modulePath, SourceType.BINARY_FAIL);
        return false;
      }
    }

    return true;
  }

  /**
   * Loads the structure of the source and its dependencies without filling in actual data.
   *
   * @param modulePath  a module to load.
   * @return true if the source was successfully loaded, false otherwise.
   */
  boolean preloadBinary(ModulePath modulePath) {
    SourceType sourceType = myLoadedModules.get(modulePath);
    if (sourceType == SourceType.BINARY || sourceType == SourceType.BINARY_FAIL) {
      return sourceType == SourceType.BINARY;
    }
    if (myLibrary.hasRawSources() && sourceType != SourceType.RAW) {
      return false;
    }
    if (myLoadingBinaryModules.containsKey(modulePath)) {
      return true;
    }

    BinarySource binarySource = myLibrary.getBinarySource(modulePath);
    if (binarySource == null || !binarySource.isAvailable()) {
      return false;
    }

    if (myLibrary.hasRawSources()) {
      Source rawSource = myLibrary.getRawSource(modulePath);
      if (rawSource != null && rawSource.isAvailable() && binarySource.getTimeStamp() < rawSource.getTimeStamp()) {
        return false;
      }
    }

    myLoadedModules.put(modulePath, SourceType.BINARY);
    myLoadingBinaryModules.put(modulePath, binarySource);
    if (!binarySource.preload(this)) {
      myLoadedModules.put(modulePath, SourceType.BINARY_FAIL);
      myLoadingBinaryModules.remove(modulePath);
      return false;
    }

    return true;
  }
}
