package org.alfasoftware.astra.core.refactoring.annotations;

import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import org.alfasoftware.astra.core.matchers.AnnotationMatcher;
import org.alfasoftware.astra.core.refactoring.AbstractRefactorTest;
import org.alfasoftware.astra.core.refactoring.operations.annotations.AddAnnotationRefactor;
import org.alfasoftware.astra.core.refactoring.operations.annotations.AnnotationChangeRefactor;
import org.alfasoftware.astra.core.refactoring.operations.annotations.RemoveAnnotationRefactor;
import org.alfasoftware.astra.core.utils.AstraUtils;
import org.alfasoftware.astra.exampleTypes.A.InnerAnnotationA;
import org.alfasoftware.astra.exampleTypes.AnnotationA;
import org.alfasoftware.astra.exampleTypes.AnnotationB;
import org.alfasoftware.astra.exampleTypes.AnnotationC;
import org.alfasoftware.astra.exampleTypes.AnnotationD;
import org.alfasoftware.astra.exampleTypes.B.InnerAnnotationB;
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
  public void testAddMarkerAnnotation() {
    assertRefactor(
      AddMarkerAnnotationExample.class,
      new HashSet<>(Arrays.asList(
        AddAnnotationRefactor.builder()
          .withNodeToAnnotate(t ->
            t instanceof TypeDeclaration && AstraUtils.getFullyQualifiedName((TypeDeclaration) t)
              .equals(AddMarkerAnnotationExample.class.getName()))
          .withAnnotationName("AnnotationA")
          .withAdditionalImports(
            AnnotationA.class.getName(),
            "org.alfasoftware.astra.exampleTypes.A").build()
        )
      )
    );
  }

  @Test
  public void testAnnotationChange() {
    assertRefactor(
      AnnotationChangeExample.class,
      new HashSet<>(Arrays.asList(
        AnnotationChangeRefactor.builder()
          .from(AnnotationMatcher.builder()
            .withFullyQualifiedName(AnnotationA.class.getName())
            .withValue("")
            .build())
          .to(AnnotationB.class.getTypeName()).build(),
        AnnotationChangeRefactor.builder()
          .from(AnnotationMatcher.builder()
            .withFullyQualifiedName(AnnotationA.class.getName())
            .withValue("A")
            .build())
          .to(AnnotationB.class.getTypeName()).build(),
        AnnotationChangeRefactor.builder()
          .from(AnnotationMatcher.builder()
            .withFullyQualifiedName(AnnotationA.class.getName())
            .withValue("BAR")
            .build())
          .to(AnnotationB.class.getTypeName()).build()
      )));
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


  @Test
  public void testAnnotationInnerTypeChange() {
    assertRefactor(
      AnnotationChangeInnerTypeExample.class,
      new HashSet<>(Arrays.asList(
        AnnotationChangeRefactor.builder()
          .from(AnnotationMatcher.builder()
            .withFullyQualifiedName(InnerAnnotationA.class.getName())
            .build())
          .to(InnerAnnotationB.class.getName()).build()
      )));
  }

  @Test
  public void testAnnotationMemberNameChange() {
    assertRefactor(
      AnnotationMemberChangeExample.class,
        new HashSet<>(Arrays.asList(
          AnnotationChangeRefactor.builder()
            .from(AnnotationMatcher.builder()
              .withFullyQualifiedName(AnnotationA.class.getName())
              .withValue("FOO")
              .build())
            .to(AnnotationD.class.getName())
              .withMemberValuePairs(Map.of("description", "BAR"))
              .build()
    )));
  }


  /**
   * Example covers:
   * - annotation without members
   * - annotation with existing member
   */
  @Test
  public void testAddMemberToAnnotation() {
    fail("TODO");
  }


  /**
   * Example covers:
   * - removing only member from an annotation
   * - removing one member from an annotation with more than one member
   */
  @Test
  public void testRemoveMemberFromAnnotation() {
    fail("TODO");
  }


  /**
   * Example covers:
   * - adding and removing one member from an annotation an existing member
   * - adding and removing one member from an annotation more than one member
   */
  @Test
  public void testAddAndRemoveMemberFromAnnotation() {
    fail("TODO");
  }


  /**
   * Example covers:
   * - updating the name of an annotation with the name specified
   * - that no changes are made to an annotation with just the value shown
   */
  @Test
  public void testUpdateMemberName() {
    fail("TODO");
  }


  /**
   * Example covers:
   * - updating the value of an annotation where just the value is shown
   * - updating the value of an annotation where a name-value pair is shown
   */
  @Test
  public void testUpdateMemberValue() {
    fail("TODO");
  }
}

