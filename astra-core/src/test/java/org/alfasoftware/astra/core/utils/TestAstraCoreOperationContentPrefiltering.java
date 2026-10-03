package org.alfasoftware.astra.core.utils;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.alfasoftware.astra.core.refactoring.UseCase;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests that {@link AstraCore} only parses files accepted by the
 * {@link ASTOperation#getContentPrefilteringPredicate() content prefiltering predicate} of at least one operation.
 */
public class TestAstraCoreOperationContentPrefiltering {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private Path directory;


  @Before
  public void setUp() throws IOException {
    directory = temporaryFolder.newFolder().toPath();
    Files.writeString(directory.resolve("Included.java"), "public class Included {}");
    Files.writeString(directory.resolve("Excluded.java"), "public class Excluded {}");
  }


  @Test
  public void filesRejectedByEveryOperationAreNotParsed() {
    Set<String> visited = ConcurrentHashMap.newKeySet();

    run(content -> true, recordingOperation(visited, content -> content.contains("Included")));

    assertEquals(Set.of("Included.java"), visited);
  }


  @Test
  public void filesAcceptedByAnyOperationAreParsed() {
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
  public void operationsAcceptEveryFileByDefault() {
    Set<String> visited = ConcurrentHashMap.newKeySet();

    run(content -> true, (compilationUnit, node, rewriter) -> visited.add(fileName(compilationUnit)));

    assertEquals(Set.of("Included.java", "Excluded.java"), visited);
  }


  @Test
  public void theUseCasePredicateMustAlsoAcceptTheFile() {
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
