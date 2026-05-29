package org.alfasoftware.astra.core.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.alfasoftware.astra.core.refactoring.UseCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link UseCase#getContentPrefilteringPredicate()} and its integration with
 * {@link AstraCore}. Verifies that files whose raw content does not satisfy the predicate
 * are skipped before AST parsing, while files that do satisfy the predicate are processed
 * normally.
 */
public class TestContentPrefilteringPredicate {

  private Path tempDir;

  @Before
  public void setUp() throws IOException {
    tempDir = Files.createTempDirectory("astra-content-prefilter-test");
  }

  @After
  public void tearDown() throws IOException {
    Files.walk(tempDir)
        .sorted(Comparator.reverseOrder())
        .forEach(path -> path.toFile().delete());
  }


  /**
   * Verifies that a file whose content does not contain the required token is not
   * visited by any AST operation, while a file that does contain the token is visited.
   */
  @Test
  public void testContentPredicateSkipsNonMatchingFile() throws IOException {
    String token = "SomeSpecificToken";
    String matchingContent = "public class WithToken { void method() { /* " + token + " */ } }";
    String nonMatchingContent = "public class WithoutToken { void method() {} }";

    Path matchingFile = tempDir.resolve("WithToken.java");
    Path nonMatchingFile = tempDir.resolve("WithoutToken.java");
    Files.writeString(matchingFile, matchingContent);
    Files.writeString(nonMatchingFile, nonMatchingContent);

    Set<Path> visitedFiles = ConcurrentHashMap.newKeySet();

    UseCase useCase = new UseCase() {
      @Override
      public Predicate<String> getContentPrefilteringPredicate() {
        return content -> content.contains(token);
      }

      @Override
      public Set<? extends ASTOperation> getOperations() {
        return Set.of((compilationUnit, node, rewriter) -> {
          Path path = (Path) compilationUnit.getProperty(CompilationUnitProperty.ABSOLUTE_PATH);
          if (path != null) {
            visitedFiles.add(path.getFileName());
          }
        });
      }

      @Override
      public int getParallelism() {
        return 1;
      }
    };

    AstraCore.run(tempDir.toString(), useCase);

    // The file with the token should have been visited by an AST operation
    assertTrue("File containing the token should have been visited",
        visitedFiles.stream().anyMatch(p -> p.toString().equals("WithToken.java")));

    // The file without the token should NOT have been visited by any AST operation
    assertFalse("File not containing the token should have been skipped",
        visitedFiles.stream().anyMatch(p -> p.toString().equals("WithoutToken.java")));

    // The non-matching file's content must be unchanged
    assertEquals("Non-matching file content must be unchanged",
        nonMatchingContent, Files.readString(nonMatchingFile));
  }


  /**
   * Verifies that the default content predicate (which accepts all files) preserves
   * existing behaviour: all files are visited by the AST operations.
   */
  @Test
  public void testDefaultContentPredicateAcceptsAllFiles() throws IOException {
    int fileCount = 3;
    for (int i = 1; i <= fileCount; i++) {
      Files.writeString(tempDir.resolve("DefaultFile" + i + ".java"),
          "public class DefaultFile" + i + " {}");
    }

    Set<Path> visitedFiles = ConcurrentHashMap.newKeySet();

    // UseCase with no override of getContentPrefilteringPredicate — default accepts all
    UseCase useCase = new UseCase() {
      @Override
      public Set<? extends ASTOperation> getOperations() {
        return Set.of((compilationUnit, node, rewriter) -> {
          Path path = (Path) compilationUnit.getProperty(CompilationUnitProperty.ABSOLUTE_PATH);
          if (path != null) {
            visitedFiles.add(path);
          }
        });
      }

      @Override
      public int getParallelism() {
        return 1;
      }
    };

    AstraCore.run(tempDir.toString(), useCase);

    assertEquals("All files should be visited when default predicate is used",
        fileCount, visitedFiles.size());
  }


  /**
   * Verifies the {@link UseCase#containsAnyOf(String...)} static factory method.
   * The predicate should return {@code true} when the content contains any of the
   * supplied tokens, and {@code false} when none are present.
   */
  @Test
  public void testContainsAnyOfHelper() {
    Predicate<String> predicate = UseCase.containsAnyOf("FooBar", "BazQux");

    assertTrue("Should match when first token is present",
        predicate.test("import com.example.FooBar;"));
    assertTrue("Should match when second token is present",
        predicate.test("import com.example.BazQux;"));
    assertTrue("Should match when both tokens are present",
        predicate.test("FooBar and BazQux together"));
    assertFalse("Should not match when no token is present",
        predicate.test("import com.example.SomethingElse;"));
    assertFalse("Should not match empty content",
        predicate.test(""));
  }


  /**
   * Verifies that a content predicate that rejects all files causes no files to be
   * processed, and all file contents remain unchanged.
   */
  @Test
  public void testContentPredicateRejectingAllFilesSkipsEverything() throws IOException {
    String originalContent = "public class Untouched {}";
    Path file = tempDir.resolve("Untouched.java");
    Files.writeString(file, originalContent);

    Set<Path> visitedFiles = ConcurrentHashMap.newKeySet();

    UseCase useCase = new UseCase() {
      @Override
      public Predicate<String> getContentPrefilteringPredicate() {
        return content -> false; // reject everything
      }

      @Override
      public Set<? extends ASTOperation> getOperations() {
        return Set.of((compilationUnit, node, rewriter) -> {
          Path path = (Path) compilationUnit.getProperty(CompilationUnitProperty.ABSOLUTE_PATH);
          if (path != null) {
            visitedFiles.add(path);
          }
        });
      }

      @Override
      public int getParallelism() {
        return 1;
      }
    };

    AstraCore.run(tempDir.toString(), useCase);

    assertTrue("No files should be visited when content predicate rejects all",
        visitedFiles.isEmpty());
    assertEquals("File content should be unchanged when skipped by content predicate",
        originalContent, Files.readString(file));
  }
}
