package org.alfasoftware.astra.core.refactoring;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import org.alfasoftware.astra.core.utils.ASTOperation;

/**
 * A use case is a collection of operations to be applied at the same time.
 * It also includes any additional sources or classpaths required for those operations to use when resolving bindings.
 *
 * For example, upgrading a library from one version to another is a use case which might consist of multiple refactoring operations,
 * and the classpath for the old library, currently used in the code to refactor.
 */
public interface UseCase {

  /**
   * This can be used to prefilter the files in the directory that the UseCase runs over,
   * based on the absolute path.
   *
   * For example, if we might have a list of files we know we want to refactor, we might have this
   * predicate only return true where the end of the absolute path (ie, the package and file name)
   * matches an entry in the list.
   */
  default Predicate<String> getPrefilteringPredicate() {
    return s -> true; // i.e. no filter by default
  }

  Set<? extends ASTOperation> getOperations();

  /**
   * Additional class files to use when building the AST.
   * This is intended to be overridden with additional classpaths, when required.
   */
  default Set<String> getAdditionalClassPathEntries() {
    return new HashSet<>();
  }

  static final String JAVA_PATH = System.getProperty("java.home"); // The JRE
  HashSet<String> defaultClasspathEntries = new HashSet<>(Arrays.asList(JAVA_PATH));

  /**
   * @return The absolute classpaths required to resolve types and bindings when running the ASTOperations specified in this use case.
   */
  default String[] getClassPath() {
    Set<String> classPath = new HashSet<>();
    classPath.addAll(defaultClasspathEntries);
    classPath.addAll(getAdditionalClassPathEntries());
    return classPath.toArray(new String[0]);
  }

  /**
   * @return The absolute paths to source code required to resolve types and bindings when running the ASTOperations specified in this use case.
   */
  default String[] getSources() {
    return new String[] { "" };
  }
}