package org.alfasoftware.astra.core.refactoring.annotations;

import java.util.Arrays;
import java.util.HashSet;

import org.alfasoftware.astra.core.matchers.AnnotationMatcher;
import org.alfasoftware.astra.core.refactoring.AbstractRefactorTest;
import org.alfasoftware.astra.core.refactoring.operations.annotations.AddAnnotationRefactor;
import org.alfasoftware.astra.core.refactoring.operations.annotations.RemoveAnnotationRefactor;
import org.alfasoftware.astra.core.utils.AstraUtils;
import org.alfasoftware.astra.exampleTypes.AnnotationB;
import org.alfasoftware.astra.exampleTypes.AnnotationC;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.junit.Test;

public class TestAnnotationsRefactor extends AbstractRefactorTest {

  @Test
  public void testAddAnnotation() {
    assertRefactor(
      AddAnnotationExample.class,
      new HashSet<>(
        Arrays.asList(
          AddAnnotationRefactor.builder()
          .withNodeToAnnotate(t -> 
            t instanceof TypeDeclaration && 
            AstraUtils.getFullyQualifiedName((TypeDeclaration) t).equals(AddAnnotationExample.class.getName()))
          .withAnnotationName("AnnotationC")
          .withOptionalAnnotationMember("A.class")
          .withAdditionalImports(
            AnnotationC.class.getName(), 
            "org.alfasoftware.astra.exampleTypes.A")
          .build()
        )
      )
    );
  }
  
  @Test
  public void testRemoveAnnotation() {
    assertRefactor(
      AnnotationRemovalExample.class,
      new HashSet<>(Arrays.asList(
        new RemoveAnnotationRefactor(
          AnnotationMatcher.builder()
          .withFullyQualifiedName(AnnotationB.class.getName())
          .build()))));
  }
}

