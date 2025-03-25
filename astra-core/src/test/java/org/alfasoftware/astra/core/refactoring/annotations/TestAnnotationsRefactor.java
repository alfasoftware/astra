package org.alfasoftware.astra.core.refactoring.annotations;

import static org.alfasoftware.astra.core.utils.AstraUtils.addImport;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
import org.alfasoftware.astra.exampleTypes.AnnotationE;
import org.alfasoftware.astra.exampleTypes.B.InnerAnnotationB;
import org.eclipse.jdt.core.dom.Annotation;
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
  public void testSameSimpleNameAnnotationChange() {
    assertRefactor(
      AnnotationChangeSameSimpleNameExample.class,
      new HashSet<>(Arrays.asList(
        AnnotationChangeRefactor.builder()
          .from(AnnotationMatcher.builder()
            .withFullyQualifiedName(AnnotationA.class.getName())
            .withValue("")
            .build())
          .to(org.alfasoftware.astra.moreexampletypes.AnnotationA.class.getTypeName()).build(),
        AnnotationChangeRefactor.builder()
          .from(AnnotationMatcher.builder()
            .withFullyQualifiedName(AnnotationA.class.getName())
            .withValue("A")
            .build())
          .to(org.alfasoftware.astra.moreexampletypes.AnnotationA.class.getTypeName()).build(),
        AnnotationChangeRefactor.builder()
          .from(AnnotationMatcher.builder()
            .withFullyQualifiedName(AnnotationA.class.getName())
            .withValue("BAR")
            .build())
          .to(org.alfasoftware.astra.moreexampletypes.AnnotationA.class.getTypeName()).build()
      )));
  }

  /**
   * Example where we are not swapping over all instances of an annotation with the same simple name
   * therefore we force the new annotation to be fully qualified.
   */
  @Test
  public void testSameSimpleNameAnnotationChangeWithRemainder() {
    assertRefactor(
      AnnotationChangeSameSimpleNameWithRemainderExample.class,
      new HashSet<>(Arrays.asList(
        AnnotationChangeRefactor.builder()
          .from(AnnotationMatcher.builder()
            .withFullyQualifiedName(AnnotationA.class.getName())
            .withValue("")
            .build())
          .to(org.alfasoftware.astra.moreexampletypes.AnnotationA.class.getTypeName())
          .forceQualifiedName()
          .build(),
        AnnotationChangeRefactor.builder()
          .from(AnnotationMatcher.builder()
            .withFullyQualifiedName(AnnotationA.class.getName())
            .withValue("BAR")
            .build())
          .to(org.alfasoftware.astra.moreexampletypes.AnnotationA.class.getTypeName())
          .forceQualifiedName()
          .build()
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


  /**
   * Example covers:
   * - annotation without members
   * - annotation with existing member
   */
  @Test
  public void testAddMemberToAnnotation() {
    assertRefactor(
        AddMemberToAnnotationExample.class,
          new HashSet<>(Arrays.asList(
            AnnotationChangeRefactor.builder()
              .from(AnnotationMatcher.builder()
                .withFullyQualifiedName(AnnotationA.class.getName())
                .build())
              .to(AnnotationD.class.getName())
              .addMemberNameValuePairs(Map.of("othervalue", "BAR"))
              .build()
      )));
  }


  /**
   * Example covers:
   * - removing only member from an annotation
   * - removing one member from an annotation with more than one member
   */
  @Test
  public void testRemoveMemberFromAnnotation() {
    assertRefactor(
        RemoveMemberFromAnnotationExample.class,
          new HashSet<>(Arrays.asList(
            AnnotationChangeRefactor.builder()
              .from(AnnotationMatcher.builder()
                .withFullyQualifiedName(AnnotationA.class.getName())
                .withWithMemberAndValue("value", "BAR")
                .build())
              .to(AnnotationD.class.getName())
              .removeMembersWithNames(Set.of("value"))
              .build()
      )));
  }


  /**
   * Example covers:
   * - adding and removing one member from an annotation with an existing member
   * - adding and removing one member from an annotation more than one member
   */
  @Test
  public void testAddAndRemoveMemberFromAnnotation() {
    assertRefactor(
        AddAndRemoveMemberFromAnnotationExample.class,
          new HashSet<>(Arrays.asList(
            AnnotationChangeRefactor.builder()
              .from(AnnotationMatcher.builder()
                .withFullyQualifiedName(AnnotationA.class.getName())
                .build())
              .to(AnnotationD.class.getName())
              .addMemberNameValuePairs(Map.of("othervalue", "BAR"))
              .removeMembersWithNames(Set.of("value"))
              .build()
      )));
  }


  /**
   * Example covers:
   * - updating the name of an annotation with the name specified
   * - that no changes are made to an annotation with just the value shown
   */
  @Test
  public void testUpdateMemberName() {
    assertRefactor(
        UpdateMemberNameInAnnotationExample.class,
          new HashSet<>(Arrays.asList(
            AnnotationChangeRefactor.builder()
              .from(AnnotationMatcher.builder()
                .withFullyQualifiedName(AnnotationA.class.getName())
                .build())
              .to(AnnotationD.class.getName())
              .updateMemberName(Map.of("value", "othervalue"))
              .build()
      )));
  }


  /**
   * Example covers:
   * - updating the value of an annotation where just the value is shown
   * - updating the value of an annotation where a name-value pair is shown
   */
  @Test
  public void testUpdateMemberValue() {
    assertRefactor(
        UpdateMemberValueInAnnotationExample.class,
          new HashSet<>(Arrays.asList(
            AnnotationChangeRefactor.builder()
              .from(AnnotationMatcher.builder()
                .withFullyQualifiedName(AnnotationA.class.getName())
                .build())
              .to(AnnotationD.class.getName())
              .updateMembersWithNameToValue(Map.of("value", "\"BAR\""))
              .build()
      )));
  }


  /**
   * Example covers:
   * - updating the member of an annotation with a simple type literal
   * - updating the member of an annotation with a qualified type literal
   */
  @Test
  public void testUpdateMemberType() {
    assertRefactor(
            UpdateMemberTypeInAnnotationExample.class,
            new HashSet<>(Arrays.asList(
                    AnnotationChangeRefactor.builder()
                            .from(AnnotationMatcher.builder()
                                    .withFullyQualifiedName(AnnotationE.class.getName())
                                    .build())
                            .to(AnnotationE.class.getName())
                            .addMemberNameTypePairs(Map.of("type", "Integer", "anotherType", "WithNestedClass.NestedClass"))
                            .withTransform((cu, a, rw) -> addImport(cu, "org.alfasoftware.astra.exampleTypes.WithNestedClass", rw))
                            .build()
            )));
  }


  /**
   * That a custom predicate can be used to identify an annotation to refactor,
   * and that a custom transformation can be specified - in this case, that the whole annotation should be removed.
   */
  @Test
  public void testAnnotationChangeWithPredicateAndTransformation() {
    assertRefactor(
        AnnotationChangeWithPredicateAndTransformExample.class,
        new HashSet<>(Arrays.asList(
            AnnotationChangeRefactor.builder()
            .from(AnnotationMatcher.builder()
                .withFullyQualifiedName(AnnotationA.class.getName())
                .withAnnotationPredicate(Annotation::isMarkerAnnotation)
                .build())
            .to(AnnotationA.class.getName())
            .withTransform((ci, a, rw) -> {
              rw.remove(a, null);
            })
            .build()
    )));
  }
}

