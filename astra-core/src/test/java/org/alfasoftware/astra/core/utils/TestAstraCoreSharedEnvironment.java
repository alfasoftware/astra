package org.alfasoftware.astra.core.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.alfasoftware.astra.core.refactoring.UseCase;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the shared JDT compilation environment introduced in {@link AstraCore}.
 *
 * <p>The batch parse path (invoked via {@link AstraCore#run}) amortises JDT classpath
 * initialisation across all files in a run by using
 * {@code ASTParser.createASTs()} with a single shared environment rather than calling
 * {@code ASTParser.createAST()} — and therefore {@code setEnvironment()} — once per file.
 *
 * <p>These tests verify:
 * <ul>
 *   <li>All files are processed through the batch path.</li>
 *   <li>Binding resolution works correctly for batch-parsed compilation units.</li>
 *   <li>Content prefiltering interacts correctly with batch parsing (skipped files are not
 *       parsed or visited).</li>
 *   <li>Batch parsing composes correctly with parallel operation application.</li>
 * </ul>
 */
public class TestAstraCoreSharedEnvironment {

  private Path tempDir;

  @Before
  public void setUp() throws IOException {
    tempDir = Files.createTempDirectory("astra-shared-env-test");
  }

  @After
  public void tearDown() throws IOException {
    Files.walk(tempDir)
        .sorted(Comparator.reverseOrder())
        .forEach(path -> path.toFile().delete());
  }


  /**
   * Verifies that the batch parse path processes every Java file in the target directory.
   * This is the primary smoke test for the shared-environment path.
   */
  @Test
  public void testBatchParsingVisitsAllFiles() throws IOException {
    int fileCount = 6;
    for (int i = 1; i <= fileCount; i++) {
      Files.writeString(tempDir.resolve("Shared" + i + ".java"),
          "public class Shared" + i + " {}");
    }

    Set<Path> visited = ConcurrentHashMap.newKeySet();

    AstraCore.run(tempDir.toString(), new UseCase() {
      @Override
      public Set<? extends ASTOperation> getOperations() {
        return Set.of((cu, node, rewriter) -> {
          Path p = (Path) cu.getProperty(CompilationUnitProperty.ABSOLUTE_PATH);
          if (p != null) {
            visited.add(p);
          }
        });
      }
    });

    assertEquals("Batch parse should visit every file", fileCount, visited.size());
  }


  /**
   * Verifies that binding resolution works correctly for compilation units produced by the
   * batch parse path.  Files that declare a field of type {@code java.util.List} should have
   * that field's type binding resolve to {@code java.util.List} (not be recovered/unknown).
   *
   * <p>The test runs with {@code parallelism = 2} to confirm that concurrent reads of
   * already-resolved bindings are safe after the sequential batch parse completes.
   */
  @Test
  public void testBatchParsingResolvesBindingsCorrectly() throws IOException {
    int fileCount = 4;
    for (int i = 1; i <= fileCount; i++) {
      Files.writeString(tempDir.resolve("BindingFile" + i + ".java"),
          "import java.util.List;\n"
          + "public class BindingFile" + i + " {\n"
          + "  List<String> items;\n"
          + "}");
    }

    Set<String> resolvedQualifiedNames = ConcurrentHashMap.newKeySet();
    Set<String> recoveredTypeNames    = ConcurrentHashMap.newKeySet();

    AstraCore.run(tempDir.toString(), new UseCase() {
      @Override
      public Set<? extends ASTOperation> getOperations() {
        return Set.of((CompilationUnit cu, ASTNode node, ASTRewrite rewriter) -> {
          if (!(node instanceof FieldDeclaration)) {
            return;
          }
          FieldDeclaration fd = (FieldDeclaration) node;
          ITypeBinding binding = fd.getType().resolveBinding();
          if (binding == null) {
            return;
          }
          if (binding.isRecovered()) {
            recoveredTypeNames.add(fd.getType().toString());
          } else {
            resolvedQualifiedNames.add(binding.getErasure().getQualifiedName());
          }
        });
      }

      @Override
      public int getParallelism() {
        return 2;
      }
    });

    assertTrue("java.util.List should resolve correctly in batch-parsed CUs",
        resolvedQualifiedNames.contains("java.util.List"));
    assertTrue("No bindings should be recovered (unresolvable) for standard library types",
        recoveredTypeNames.isEmpty());
  }


  /**
   * Verifies that content prefiltering interacts correctly with batch parsing.
   * Files whose content does not pass the predicate must not be visited by any AST operation,
   * and their content must remain unchanged.
   */
  @Test
  public void testContentPrefilteringSkipsFilesBeforeBatchParse() throws IOException {
    String token = "BATCH_TOKEN";
    String matchingContent   = "public class WithToken   { /* " + token + " */ }";
    String nonMatchingContent = "public class WithoutToken { /* no token here */ }";

    Path matchingFile    = tempDir.resolve("WithToken.java");
    Path nonMatchingFile = tempDir.resolve("WithoutToken.java");
    Files.writeString(matchingFile, matchingContent);
    Files.writeString(nonMatchingFile, nonMatchingContent);

    Set<Path> visitedPaths = ConcurrentHashMap.newKeySet();

    AstraCore.run(tempDir.toString(), new UseCase() {
      @Override
      public Predicate<String> getContentPrefilteringPredicate() {
        return content -> content.contains(token);
      }

      @Override
      public Set<? extends ASTOperation> getOperations() {
        return Set.of((cu, node, rewriter) -> {
          Path p = (Path) cu.getProperty(CompilationUnitProperty.ABSOLUTE_PATH);
          if (p != null) {
            visitedPaths.add(p);
          }
        });
      }

      @Override
      public int getParallelism() {
        return 1;
      }
    });

    assertTrue("File containing the token should be visited",
        visitedPaths.stream().anyMatch(p -> p.getFileName().toString().equals("WithToken.java")));
    assertFalse("File without the token should not be visited",
        visitedPaths.stream().anyMatch(p -> p.getFileName().toString().equals("WithoutToken.java")));
    assertEquals("Non-matching file content must be unchanged",
        nonMatchingContent, Files.readString(nonMatchingFile));
  }


  /**
   * Verifies that batch parsing composes correctly with parallel operation application:
   * all files should be visited even when multiple worker threads apply operations
   * concurrently on the batch-parsed compilation units.
   */
  @Test
  public void testBatchParsingWithParallelOpsVisitsAllFiles() throws IOException {
    int fileCount = 8;
    for (int i = 1; i <= fileCount; i++) {
      Files.writeString(tempDir.resolve("Par" + i + ".java"),
          "public class Par" + i + " {}");
    }

    Set<Path> visited = ConcurrentHashMap.newKeySet();

    AstraCore.run(tempDir.toString(), new UseCase() {
      @Override
      public Set<? extends ASTOperation> getOperations() {
        return Set.of((cu, node, rewriter) -> {
          Path p = (Path) cu.getProperty(CompilationUnitProperty.ABSOLUTE_PATH);
          if (p != null) {
            visited.add(p);
          }
        });
      }

      @Override
      public int getParallelism() {
        return 4;
      }
    });

    assertEquals("All files should be visited with batch parse + parallelism=4",
        fileCount, visited.size());
  }


  /**
   * Verifies that a per-file operation error in the batch path surfaces correctly and does
   * not prevent other files from being processed.
   */
  @Test
  public void testBatchParsingContinuesAfterPerFileError() throws IOException {
    int totalFiles = 4;
    for (int i = 1; i <= totalFiles; i++) {
      Files.writeString(tempDir.resolve("ErrFile" + i + ".java"),
          "public class ErrFile" + i + " {}");
    }

    Set<Path> successfulFiles = ConcurrentHashMap.newKeySet();

    UseCase useCase = new UseCase() {
      @Override
      public Set<? extends ASTOperation> getOperations() {
        return Set.of((cu, node, rewriter) -> {
          Path p = (Path) cu.getProperty(CompilationUnitProperty.ABSOLUTE_PATH);
          if (p != null && p.getFileName().toString().equals("ErrFile2.java")) {
            throw new RuntimeException("Intentional batch-path failure for ErrFile2");
          }
          if (p != null) {
            successfulFiles.add(p);
          }
        });
      }

      @Override
      public int getParallelism() {
        return 2;
      }
    };

    try {
      AstraCore.run(tempDir.toString(), useCase);
      fail("Expected a RuntimeException due to ErrFile2 failing");
    } catch (RuntimeException e) {
      assertNotNull(e.getMessage());
    }

    assertEquals("Non-failing files should still be processed", totalFiles - 1, successfulFiles.size());
  }


  /**
   * Verifies that a run with zero files after content prefiltering (all files excluded) completes
   * cleanly without errors, and the batch parse is not called with an empty file list.
   */
  @Test
  public void testBatchParsingWithAllFilesContentFiltered() throws IOException {
    Files.writeString(tempDir.resolve("FilteredOut.java"), "public class FilteredOut {}");

    Set<Path> visited = ConcurrentHashMap.newKeySet();

    AstraCore.run(tempDir.toString(), new UseCase() {
      @Override
      public Predicate<String> getContentPrefilteringPredicate() {
        return content -> false; // reject everything
      }

      @Override
      public Set<? extends ASTOperation> getOperations() {
        return Set.of((cu, node, rewriter) -> {
          Path p = (Path) cu.getProperty(CompilationUnitProperty.ABSOLUTE_PATH);
          if (p != null) {
            visited.add(p);
          }
        });
      }
    });

    assertTrue("No files should be visited when content predicate rejects all", visited.isEmpty());
  }
}
