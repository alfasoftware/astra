package org.alfasoftware.astra.core.refactoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;
import java.util.function.Function;

import org.alfasoftware.astra.core.utils.ASTOperation;
import org.alfasoftware.astra.core.utils.AstraCore;
import org.eclipse.jface.text.BadLocationException;

public abstract class AbstractRefactorTest {

  protected static final String TEST_SOURCE = Paths.get(".").toAbsolutePath().normalize().toString().concat("/src/test/java");
  protected static final String TEST_EXAMPLES = "./src/test/java";


  /**
   * Reads the before and after examples. Calls the refactor on the before example,
   * and checks the output matches the after example.
   *
   * @param packageName package to find our before and after examples
   * @param beforeName name of the before example. The after example will need to be called this + "After"
   * @param refactors Set of refactors to apply
   */
  protected void assertRefactor(Class<?> beforeClass, Set<? extends ASTOperation> refactors) {
    // This just gets the java path.
    assertRefactorWithClassPath(beforeClass, refactors, UseCase.DEFAULT_CLASSPATH_ENTRIES.toArray(new String[0]));
  }

  protected void assertRefactorWithClassPath(Class<?> beforeClass, Set<? extends ASTOperation> refactors, String[] classPath) {
    assertRefactorWithSourcesAndClassPath(beforeClass, refactors, new String[]{TEST_SOURCE}, classPath);
  }

  protected Function<String, String> changesToApplyToBefore = a -> a;

  protected void assertRefactorWithSourcesAndClassPath(Class<?> beforeClass, Set<? extends ASTOperation> refactors, String[] sources, String[] classPath) {

    File before = new File(TEST_EXAMPLES + "/" + beforeClass.getName().replaceAll("\\.", "/") + ".java");
    File after = new File(TEST_EXAMPLES + "/" + beforeClass.getName().replaceAll("\\.", "/") + "After.java");
    if (! after.exists()) {
      after = new File(TEST_EXAMPLES + "/" + beforeClass.getName().replaceAll("\\.", "/") + "After.txt");
    }

    try {
      String fileContentBefore = new String(Files.readAllBytes(before.toPath()));
      String expectedAfter = new String(Files.readAllBytes(after.toPath()));
      String expectedBefore = new AstraCore().applyOperationsToFile(before, fileContentBefore, refactors, sources, classPath)
        .replaceAll(beforeClass.getSimpleName(), beforeClass.getSimpleName() + "After");

      expectedBefore = changesToApplyToBefore.apply(expectedBefore);

      assertEquals(expectedAfter, expectedBefore);
    } catch (IOException | BadLocationException e) {
      e.printStackTrace();
      fail();
    }
  }


  protected void assertRefactorWithSourcesAndClassPathAndTextFileExamples(String beforeClassName, String beforeClassSimpleName, Set<? extends ASTOperation> refactors, String[] sources, String[] classPath) {
    String baseClassName = "./src/test/java/" + beforeClassName.replaceAll("\\.", "/");
    File before = new File(baseClassName + ".java");
    if (! before.exists()) {
      before = new File(baseClassName + ".txt");
    }
    File after = new File(baseClassName + "After.java");
    if (! after.exists()) {
      after = new File(baseClassName + "After.txt");
    }

    try {
      String fileContentBefore = new String(Files.readAllBytes(before.toPath()));
      String expectedAfter = new String(Files.readAllBytes(after.toPath()));
      String expectedBefore = new AstraCore().applyOperationsToFile(before, fileContentBefore, refactors, sources, classPath).replaceAll(beforeClassSimpleName, beforeClassSimpleName + "After");
      expectedBefore = this.changesToApplyToBefore.apply(expectedBefore);
      assertEquals(expectedAfter, expectedBefore);
    } catch (BadLocationException | IOException exception) {
      exception.printStackTrace();
      fail();
    }
  }
}
