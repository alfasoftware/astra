package org.alfasoftware.astra.core.refactoring.types;

import java.util.Arrays;
import java.util.HashSet;

import org.alfasoftware.astra.core.refactoring.AbstractRefactorTest;
import org.alfasoftware.astra.core.refactoring.operations.types.TypeReferenceRefactor;
import org.alfasoftware.astra.exampleTypes.A;
import org.alfasoftware.astra.exampleTypes.B;
import org.alfasoftware.astra.exampleTypes.EnumA;
import org.alfasoftware.astra.exampleTypes.EnumB;
import org.junit.Test;

public class TestTypeReferenceRefactor extends AbstractRefactorTest {

	
  /**
   * Changing from one type to another, anywhere it occurs.
   */
  @Test
  public void testChangeTypes() {
    assertRefactor(TypeReferenceExample.class,
        new HashSet<>(Arrays.asList(
          TypeReferenceRefactor.builder()
            .fromType(A.class.getName())
            .toType(B.class.getName())
            .build())));
  }


  /**
   * Changing from one type to another, when that type is extended.
   * e.g.
   * class SomeType extends org.foo.OldType {…}
   */
  @Test
  public void testChangeTypesWhenExtended() {
    assertRefactor(TypeReferenceExtendedTypeExample.class,
        new HashSet<>(Arrays.asList(
          TypeReferenceRefactor.builder()
            .fromType(A.class.getName())
            .toType(B.class.getName())
            .build())));
  }

  
  /**
   * Changing from one type to another, when that type is used in an enum reference.
   * e.g.
   * String type = org.foo.OldType.EMPTY;
   */
  @Test
  public void testChangeTypesWhenUsedInEnum() {
    assertRefactor(TypeReferenceEnumExample.class,
        new HashSet<>(Arrays.asList(
          TypeReferenceRefactor.builder()
            .fromType(EnumA.class.getName())
            .toType(EnumB.class.getName())
            .build())));
  }
  
  
  /**
   * This test checks that where the fully qualified names don't match, the types are not refactored.
   */
  @Test
  public void testChangeTypesNegativeExample() {
    assertRefactor(TypeReferenceNegativeExample.class,
        new HashSet<>(Arrays.asList(
          TypeReferenceRefactor.builder()
            .fromType(A.class.getName())
            .toType(B.class.getName())
            .build())));
  }


  /**
   * Changing from one type to another, where that type is parameterized.
   */
  @Test
  public void testChangeParameterizedType() {
    assertRefactor(TypeReferenceParameterizedExample.class,
        new HashSet<>(Arrays.asList(
          TypeReferenceRefactor.builder()
            .fromType(A.class.getName())
            .toType(B.class.getName())
            .build())));
  }
}
