package org.alfasoftware.astra.core.refactoring.javapattern.prefiltering;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.function.Predicate;

import org.alfasoftware.astra.core.refactoring.AbstractRefactorTest;
import org.alfasoftware.astra.core.refactoring.UseCase;
import org.alfasoftware.astra.core.refactoring.operations.javapattern.JavaPatternASTOperation;
import org.alfasoftware.astra.core.utils.ASTOperation;
import org.alfasoftware.astra.core.utils.AstraCore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests the content prefiltering predicate of {@link JavaPatternASTOperation}, which rejects source code that
 * cannot be matched because it does not contain the identifiers required by any of the patterns.
 */
public class TestJavaPatternContentPrefiltering extends AbstractRefactorTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();


  /**
   * See {@link MethodNamesPattern}.
   */
  @Test
  public void requiresTheNamesOfInvokedMethodsButNotOfParameters() throws IOException {
    Predicate<String> predicate = predicateFor(MethodNamesPattern.class);
    assertTrue(predicate.test("lookup.get(id).toString().equals(name)"));
    assertFalse(predicate.test("lookup.get(id).equals(name)"));
    assertFalse(predicate.test("lookup.toString().equals(name)"));
  }


  /**
   * See {@link SubstituteMethodPattern}.
   */
  @Test
  public void doesNotRequireTheNamesOfSubstituteMethods() throws IOException {
    Predicate<String> predicate = predicateFor(SubstituteMethodPattern.class);
    assertTrue(predicate.test("names.add(String.valueOf(value).trim());"));
    assertFalse(predicate.test("names.add(String.valueOf(value));"));
  }


  /**
   * See {@link SubstituteOnlyPattern}.
   */
  @Test
  public void acceptsEverythingWhenNoIdentifiersAreRequired() throws IOException {
    assertTrue(predicateFor(SubstituteOnlyPattern.class).test("class Empty {}"));
  }


  /**
   * See {@link ConstructorsPattern}.
   */
  @Test
  public void requiresTheIdentifiersOfAnyOnePattern() throws IOException {
    Predicate<String> predicate = predicateFor(ConstructorsPattern.class);
    assertTrue(predicate.test("new StringBuilder(text).reverse()"));
    assertTrue(predicate.test("new ArrayList<>(values)"));
    assertFalse(predicate.test("new StringBuilder(text)"));
    assertFalse(predicate.test("new LinkedList<>(values)"));
  }


  /**
   * See {@link VarargsPattern}.
   */
  @Test
  public void doesNotRequireArgumentsCapturedByAnArrayParameter() throws IOException {
    Predicate<String> predicate = predicateFor(VarargsPattern.class);
    assertTrue(predicate.test("System.arraycopy(source, 0, target, 0, length);"));
    assertFalse(predicate.test("Arrays.fill(values, null);"));
  }


  /**
   * Identifiers written with unicode escapes are not found by a text search, so such content is always accepted.
   */
  @Test
  public void acceptsContentWithUnicodeEscapes() throws IOException {
    assertTrue(predicateFor(MethodNamesPattern.class).test("lookup.\\u0067et(id).toString().equals(name)"));
  }


  /**
   * Files which can't be matched are skipped, and files which can are still refactored.
   */
  @Test
  public void refactorsFilesWhichCanBeMatched() throws IOException {
    Path directory = temporaryFolder.newFolder().toPath();
    String matching = "package example;\n"
        + "import java.util.Map;\n"
        + "class Matching {\n"
        + "  boolean check(Map<String, Integer> map) {\n"
        + "    return map.get(\"a\").toString().equals(\"b\");\n"
        + "  }\n"
        + "}\n";
    String notMatching = "package example;\n"
        + "import java.util.Map;\n"
        + "class NotMatching {\n"
        + "  boolean check(Map<String, Integer> map) {\n"
        + "    return map.get(\"a\").equals(\"b\");\n"
        + "  }\n"
        + "}\n";
    Files.writeString(directory.resolve("Matching.java"), matching);
    Files.writeString(directory.resolve("NotMatching.java"), notMatching);

    JavaPatternASTOperation operation = operationFor(MethodNamesPattern.class);
    AstraCore.run(directory.toString(), new UseCase() {
      @Override
      public Set<? extends ASTOperation> getOperations() {
        return Set.of(operation);
      }
    });

    assertTrue(Files.readString(directory.resolve("Matching.java")).contains("\"b\".equals(String.valueOf(map.get(\"a\")))"));
    assertEquals(notMatching, Files.readString(directory.resolve("NotMatching.java")));
  }


  private static Predicate<String> predicateFor(Class<?> pattern) throws IOException {
    return operationFor(pattern).getContentPrefilteringPredicate();
  }


  private static JavaPatternASTOperation operationFor(Class<?> pattern) throws IOException {
    return new JavaPatternASTOperation(Path.of(TEST_EXAMPLES + "/" + pattern.getName().replace('.', '/') + ".java"),
        new String[] {TEST_SOURCE});
  }
}
