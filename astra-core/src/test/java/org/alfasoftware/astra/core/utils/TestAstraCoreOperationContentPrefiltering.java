package org.alfasoftware.astra.core.utils;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.alfasoftware.astra.core.refactoring.UseCase;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests that {@link AstraCore} only parses files accepted by the
 * {@link ASTOperation#getContentPrefilteringPredicate() content prefiltering predicate} of at least one operation.
 */
public class TestAstraCoreOperationContentPrefiltering {

  private Path directory;


  @Before
  public void setUp() throws IOException {
    directory = Files.createTempDirectory("astra-operation-prefiltering-test");
    Files.writeString(directory.resolve("Included.java"), "public class Included {}");
    Files.writeString(directory.resolve("Excluded.java"), "public class Excluded {}");
  }


  @After
  public void tearDown() throws IOException {
    Files.walk(directory)
        .sorted(Comparator.reverseOrder())
        .forEach(path -> path.toFile().delete());
  }


  @Test
  public void testFilesRejectedByEveryOperationAreNotParsed() {
    Set<String> visited = ConcurrentHashMap.newKeySet();

    run(content -> true, recordingOperation(visited, content -> content.contains("Included")));

    assertEquals(Set.of("Included.java"), visited);
  }


  @Test
  public void testFilesAcceptedByAnyOperationAreParsed() {
    Set<String> visitedByFilteringOperation = ConcurrentHashMap.newKeySet();
    Set<String> visitedByOtherOperation = ConcurrentHashMap.newKeySet();

    run(content -> true,
        recordingOperation(visitedByFilteringOperation, content -> content.contains("Included")),
        recordingOperation(visitedByOtherOperation, content -> content.contains("Excluded")));

    // a file which is parsed is passed to every operation
    assertEquals(Set.of("Included.java", "Excluded.java"), visitedByFilteringOperation);
    assertEquals(Set.of("Included.java", "Excluded.java"), visitedByOtherOperation);
  }


  @Test
  public void testOperationsAcceptEveryFileByDefault() {
    Set<String> visited = ConcurrentHashMap.newKeySet();

    run(content -> true, (compilationUnit, node, rewriter) -> visited.add(fileName(compilationUnit)));

    assertEquals(Set.of("Included.java", "Excluded.java"), visited);
  }


  @Test
  public void testTheUseCasePredicateMustAlsoAcceptTheFile() {
    Set<String> visited = ConcurrentHashMap.newKeySet();

    run(content -> ! content.contains("Excluded"), recordingOperation(visited, content -> true));

    assertEquals(Set.of("Included.java"), visited);
  }


  private void run(Predicate<String> useCasePredicate, ASTOperation... operations) {
    AstraCore.run(directory.toString(), new UseCase() {
      @Override
      public Set<? extends ASTOperation> getOperations() {
        return new LinkedHashSet<>(Arrays.asList(operations));
      }

      @Override
      public Predicate<String> getContentPrefilteringPredicate() {
        return useCasePredicate;
      }
    });
  }


  private static ASTOperation recordingOperation(Set<String> visited, Predicate<String> contentPrefilteringPredicate) {
    return new ASTOperation() {
      @Override
      public void run(CompilationUnit compilationUnit, ASTNode node, ASTRewrite rewriter) {
        visited.add(fileName(compilationUnit));
      }

      @Override
      public Predicate<String> getContentPrefilteringPredicate() {
        return contentPrefilteringPredicate;
      }
    };
  }


  private static String fileName(CompilationUnit compilationUnit) {
    return ((Path) compilationUnit.getProperty(CompilationUnitProperty.ABSOLUTE_PATH)).getFileName().toString();
  }
}
