package org.alfasoftware.astra.core.refactoring.types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.alfasoftware.astra.core.refactoring.AbstractRefactorTest;
import org.alfasoftware.astra.core.refactoring.UseCase;
import org.alfasoftware.astra.core.refactoring.operations.types.UpdateTypeRefactor;
import org.alfasoftware.astra.core.refactoring.types.newpackage.UpdatedTypeExampleAfter;
import org.alfasoftware.astra.core.utils.ASTOperation;
import org.alfasoftware.astra.core.utils.AstraCore;
import org.eclipse.jface.text.BadLocationException;
import org.junit.Test;

public class TestUpdateTypeRefactor extends AbstractRefactorTest {

  /**
   * Verify that the "from" type itself is updated correctly - this means:
   * <ul>
   *  <li>updating the name in the type declaration</li>
   *  <li>updating the package declaration</li>
   *  <li>update all references to the simple or qualified name of the "from" type, 
   *      just as we do in external references to the type</li>
   *  <li>adding imports for all referenced types, as if the type is moved to a new package, some types which were not imported
   *      due to being in the same package will need to be explicitly imported 
   * </ul>
   */
  @Test
  @SuppressWarnings("rawtypes")
  public void testUpdateTypeNameAndPackageInFromFile() {
    
    Class beforeClass = UpdateTypeToChangeExample.class;
    Class afterClass = UpdatedTypeExampleAfter.class;
    
    Set<ASTOperation> operations = new HashSet<>(Arrays.asList(
      UpdateTypeRefactor.builder()
        .fromType(beforeClass.getName())
        .toType(afterClass.getName())
        .build()
    ));
    
    
    File before = new File(TEST_EXAMPLES + "/" + beforeClass.getName().replaceAll("\\.", "/") + ".java");
    File after = new File(TEST_EXAMPLES + "/" + afterClass.getName().replaceAll("\\.", "/") + ".java");

    try {
      String fileContentBefore = new String(Files.readAllBytes(before.toPath()));
      String expectedAfter = new String(Files.readAllBytes(after.toPath()));
      String expectedBefore = new AstraCore().applyOperationsToFile(
        fileContentBefore, 
        operations, 
        new HashSet<>(Arrays.asList(TEST_SOURCE)).toArray(new String[0]),
        UseCase.defaultClasspathEntries.toArray(new String[0]));

      expectedBefore = changesToApplyToBefore.apply(expectedBefore);

      assertEquals(
        expectedAfter,
        expectedBefore);
    } catch (IOException | BadLocationException e) {
      e.printStackTrace();
      fail();
    }
  }
}

