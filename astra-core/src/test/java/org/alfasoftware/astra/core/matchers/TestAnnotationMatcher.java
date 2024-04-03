package org.alfasoftware.astra.core.matchers;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

import org.alfasoftware.astra.core.refactoring.UseCase;
import org.alfasoftware.astra.core.utils.AstraUtils;
import org.alfasoftware.astra.core.utils.ClassVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.junit.Assert;
import org.junit.Test;

public class TestAnnotationMatcher {

  protected static final String TEST_SOURCE = Paths.get(".").toAbsolutePath().normalize().toString().concat("/src/test/java");

  @Test
  public void testAnnotationSimpleName() {
    // Given
    String annotation = "import org.alfasoftware.astra.exampleTypes.AnnotationA;\r\b"
        + "@AnnotationA\r\b"
        + "class x{}";
    Matcher matcher = AnnotationMatcher.builder()
        .withFullyQualifiedName("org.alfasoftware.astra.exampleTypes.AnnotationA")
        .build();

    // When
    ClassVisitor visitor = parse(annotation);

    // Then
    List<MarkerAnnotation> annotations = visitor.getMarkerAnnotations();
    Assert.assertTrue(matcher.matches(annotations.get(0)));
  }

  @Test
  public void testAnnotationSimpleNameWithNoMatch() {
    // Given
    String annotation = "import org.alfasoftware.astra.exampleTypes.AnnotationA;\r\b"
        + "@AnnotationA\r\b"
        + "class x{}";
    Matcher matcher = AnnotationMatcher.builder()
        .withFullyQualifiedName("com.foo.AnnotationA")
        .build();

    // When
    ClassVisitor visitor = parse(annotation);

    // Then
    List<MarkerAnnotation> annotations = visitor.getMarkerAnnotations();
    Assert.assertFalse(matcher.matches(annotations.get(0)));
  }

  @Test
  public void testAnnotationWithSingleStringPropertyWithoutExplicitKey() {
    // Given
    String annotation = "import org.alfasoftware.astra.exampleTypes.AnnotationA;\r\b"
        + "@AnnotationA(\"test\")\r\n"
        + "class x{}";
    Matcher matcher = AnnotationMatcher.builder()
        .withFullyQualifiedName("org.alfasoftware.astra.exampleTypes.AnnotationA")
        .withValue("test")
        .build();

    // When
    ClassVisitor visitor = parse(annotation);

    // Then
    List<SingleMemberAnnotation> annotations = visitor.getSingleMemberAnnotations();
    Assert.assertTrue(matcher.matches(annotations.get(0)));
  }

  @Test
  public void testAnnotationWithSingleIntegerPropertyWithoutExplicitKey() {
    // Given
    String annotation = "import org.alfasoftware.astra.exampleTypes.AnnotationA;\r\b"
        + "@AnnotationA(2)\r\n"
        + "class x{}";
    Matcher matcher = AnnotationMatcher.builder()
        .withFullyQualifiedName("org.alfasoftware.astra.exampleTypes.AnnotationA")
        .withValue(2)
        .build();

    // When
    ClassVisitor visitor = parse(annotation);

    // Then
    List<SingleMemberAnnotation> annotations = visitor.getSingleMemberAnnotations();
    Assert.assertTrue(matcher.matches(annotations.get(0)));
  }

  @Test
  public void testAnnotationWithSingleIntegerPropertyWithoutExplicitKeyAndWrongName() {
    // Given
    String annotation = "import org.alfasoftware.astra.exampleTypes.AnnotationA;\r\b"
        + "@AnnotationA(2)\r\n"
        + "class x{}";
    Matcher matcher = AnnotationMatcher.builder()
        .withFullyQualifiedName("Annotation2")
        .withValue(2)
        .build();

    // When
    ClassVisitor visitor = parse(annotation);

    // Then
    List<SingleMemberAnnotation> annotations = visitor.getSingleMemberAnnotations();
    Assert.assertFalse(matcher.matches(annotations.get(0)));
  }

  @Test
  public void testAnnotationWithSingleStringPropertyWithExplicitKey() {
    // Given
    String annotation = "import org.alfasoftware.astra.exampleTypes.AnnotationA;\r\b"
        + "@AnnotationA(marker = \"test\")\r\n"
        + "class x{}";
    Matcher matcher = AnnotationMatcher.builder()
        .withFullyQualifiedName("org.alfasoftware.astra.exampleTypes.AnnotationA")
        .withWithMemberAndValue("marker", "test")
        .build();

    // When
    ClassVisitor visitor = parse(annotation);

    // Then
    List<NormalAnnotation> annotations = visitor.getNormalAnnotations();
    Assert.assertTrue(matcher.matches(annotations.get(0)));
  }

  @Test
  public void testAnnotationWithSingleIntegerPropertyWithExplicitKey() {
    // Given
    String annotation = "import org.alfasoftware.astra.exampleTypes.AnnotationA;\r\b"
        + "@AnnotationA(marker = 2)\r\n"
        + "class x{}";
    Matcher matcher = AnnotationMatcher.builder()
        .withFullyQualifiedName("org.alfasoftware.astra.exampleTypes.AnnotationA")
        .withWithMemberAndValue("marker", 2)
        .build();

    // When
    ClassVisitor visitor = parse(annotation);

    // Then
    List<NormalAnnotation> annotations = visitor.getNormalAnnotations();
    Assert.assertTrue(matcher.matches(annotations.get(0)));
  }

  @Test
  public void testAnnotationWithSingleStringPropertyWithExplicitKeyAndWrongName() {
    // Given
    String annotation = "import org.alfasoftware.astra.exampleTypes.AnnotationA;\r\b"
        + "@AnnotationA(marker = \"test\")\r\n"
        + "class x{}";
    Matcher matcher = AnnotationMatcher.builder()
        .withFullyQualifiedName("Annotation2")
        .withWithMemberAndValue("marker", "test")
        .build();

    // When
    ClassVisitor visitor = parse(annotation);

    // Then
    List<NormalAnnotation> annotations = visitor.getNormalAnnotations();
    Assert.assertFalse(matcher.matches(annotations.get(0)));
  }

  @Test
  public void testAnnotationWithMultipleAnnotationsOfDifferentTypesWithFullMatch() {
    // Given
    String annotation = "import org.alfasoftware.astra.exampleTypes.AnnotationA;\r\b" +
        "@Annotation\r\n" +
        "@Annotation2(\"test\")\r\n" +
        "@Annotation3(marker = \"test\")\r\n" +
        "class x{}";
    Matcher matcher1 = AnnotationMatcher.builder()
        .withFullyQualifiedName("Annotation")
        .build();
    Matcher matcher2 = AnnotationMatcher.builder()
        .withFullyQualifiedName("Annotation2")
        .withValue("test")
        .build();
    Matcher matcher3 = AnnotationMatcher.builder()
        .withFullyQualifiedName("Annotation3")
        .withWithMemberAndValue("marker", "test")
        .build();

    // When
    ClassVisitor visitor = parse(annotation);

    // Then
    final List<MarkerAnnotation> markerAnnotations = visitor.getMarkerAnnotations();
    Assert.assertTrue(matcher1.matches(markerAnnotations.get(0)));
    final List<SingleMemberAnnotation> singleMemberAnnotations = visitor.getSingleMemberAnnotations();
    Assert.assertTrue(matcher2.matches(singleMemberAnnotations.get(0)));
    final List<NormalAnnotation> normalAnnotations = visitor.getNormalAnnotations();
    Assert.assertTrue(matcher3.matches(normalAnnotations.get(0)));
  }

  @Test
  public void testAnnotationWithMultiplePropertiesWithExplicitKeys() {
    // Given
    String annotation = "import org.alfasoftware.astra.exampleTypes.AnnotationA;\r\b"
        + "@AnnotationA(marker = 2, test=\"thing\", otherProperty=2.3)\r\n"
        + "class x{}";
    Matcher matcher = AnnotationMatcher.builder()
        .withFullyQualifiedName("org.alfasoftware.astra.exampleTypes.AnnotationA")
        .withWithMemberAndValue("marker", 2)
        .withWithMemberAndValue("test", "thing")
        .withWithMemberAndValue("otherProperty", 2.3)
        .build();

    // When
    ClassVisitor visitor = parse(annotation);

    // Then
    List<NormalAnnotation> annotations = visitor.getNormalAnnotations();
    Assert.assertTrue(matcher.matches(annotations.get(0)));
  }

  private ClassVisitor parse(String source) {
    CompilationUnit compilationUnit = AstraUtils.readAsCompilationUnit(new File(""), source, new String[]{TEST_SOURCE}, UseCase.DEFAULT_CLASSPATH_ENTRIES.toArray(new String[0]));
    ClassVisitor visitor = new ClassVisitor();
    compilationUnit.accept(visitor);
    return visitor;
  }
}