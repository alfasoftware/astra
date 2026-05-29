package org.alfasoftware.astra.core.refactoring;

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

  /**
   * Returns a predicate applied to the raw file content (as a String) before
   * the file is parsed into an AST. Files for which this predicate returns
   * {@code false} are skipped entirely, avoiding the cost of AST construction.
   *
   * <p>The default implementation accepts every file. Override this to provide
   * a fast content-based check — for example, a simple
   * {@code content -> content.contains("OldTypeName")} guard reduces parse
   * overhead substantially on large codebases when only a minority of files
   * reference the types being refactored.
   *
   * <p>This predicate is applied <em>after</em> {@link #getPrefilteringPredicate()}
   * and only when that path-level predicate has already passed.
   *
   * <p>Examples:
   * <pre>
   * // pass files that mention any of several type names:
   * content -&gt; content.contains("OldFoo") || content.contains("OldBar");
   *
   * // pass files that mention all of several tokens:
   * content -&gt; content.contains("OldFoo") &amp;&amp; content.contains("@Deprecated");
   * </pre>
   */
  default Predicate<String> getContentPrefilteringPredicate() {
    return content -> true;
  }

  Set<? extends ASTOperation> getOperations();

  /**
   * Additional class files to use when building the AST.
   * This is intended to be overridden with additional classpaths, when required.
   */
  default Set<String> getAdditionalClassPathEntries() {
    return new HashSet<>();
  }

  /**
   * @return The absolute classpaths required to resolve types and bindings when running the ASTOperations specified in this use case.
   *         The running VM's boot classpath is included automatically by the parser.
   */
  default String[] getClassPath() {
    return getAdditionalClassPathEntries().toArray(new String[0]);
  }

  /**
   * @return The absolute paths to source code required to resolve types and bindings when running the ASTOperations specified in this use case.
   */
  default String[] getSources() {
    return new String[] { "" };
  }

  /**
   * @return The number of threads to use when processing files in parallel.
   *         Defaults to {@link Runtime#availableProcessors()}.
   *         Override and return {@code 1} to force sequential processing.
   */
  default int getParallelism() {
    return Runtime.getRuntime().availableProcessors();
  }
}
