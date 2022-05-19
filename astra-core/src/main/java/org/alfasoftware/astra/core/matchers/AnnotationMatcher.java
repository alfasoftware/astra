package org.alfasoftware.astra.core.matchers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;

/**
 * A way of matching an annotation by its properties.
 * Examples of annotation properties that can be used to match include the fully qualified annotation type name,
 * properties, and the type in which it is used.
 */
public class AnnotationMatcher implements Matcher {
  private final AnnotationBuilder annotationBuilder;

  private AnnotationMatcher(final AnnotationBuilder annotationBuilder) {
    this.annotationBuilder = annotationBuilder;
  }

  public static AnnotationBuilder builder() {
    return new AnnotationBuilder();
  }

  public static AnnotationBuilder builder(TypeMatcher.TypeBuilder parent) {
    return new AnnotationBuilder(parent);
  }

  /**
   * Builder for setting up {@link AnnotationMatcher}s.
   */
  public static class AnnotationBuilder implements Builder {

    private TypeMatcher.TypeBuilder parent;
    private String annotationName;
    private Map<String, Object> properties;
    private Optional<Predicate<Annotation>> annotationPredicate = Optional.empty();

    /**
     * Don't construct this directly - use the static method.
     */
    private AnnotationBuilder() {
      super();
    }

    private AnnotationBuilder(TypeMatcher.TypeBuilder parent) {
      this.parent = parent;
    }

    public TypeMatcher.TypeBuilder endTypeAnnotation() {
      return parent;
    }

    @Override
    public AnnotationMatcher build() {
      return new AnnotationMatcher(this);
    }

    public AnnotationBuilder withFullyQualifiedName(String annotationName) {
      // Removing $ from inner class names as this won't match with resolved type binding names
      this.annotationName = annotationName.replaceAll("\\$", ".");
      return this;
    }

    /**
     * Used to match on the provided value for a single member annotation.
     *
     * e.g. to match "test" in @Annotation("test")
     */
    public AnnotationBuilder withValue(Object value) {
      //if the key is not specified, it is "value"
      return withWithMemberAndValue("value", value);
    }


    /**
     * Used to match on an annotation's property, and the provided value.
     *
     * e.g. to match 'marker = "test"' in @Annotation(marker = "test")
     */
    public AnnotationBuilder withWithMemberAndValue(String key, Object value) {
      if (properties == null) {
        properties = new HashMap<>();
      }
      properties.put(key, value);
      return this;
    }


    /**
     * Used to match directly on the annotation as it appears in the AST generated from the source files where it is used.
     */
    public AnnotationBuilder withAnnotationPredicate(Predicate<Annotation> annotationPredicate) {
      this.annotationPredicate = Optional.of(annotationPredicate);
      return this;
    }
  }

  public String getFullyQualifiedName() {
    return this.annotationBuilder.annotationName;
  }

  @Override
  public boolean matches(ASTNode node) {
    Annotation annotation = (Annotation) node;

    // Check annotation name
    ITypeBinding annotationTypeBinding = annotation.getTypeName().resolveTypeBinding();
    boolean typeNameMatches = annotationTypeBinding != null && annotationTypeBinding.getQualifiedName().equals(annotationBuilder.annotationName);
    if (! typeNameMatches) {
      return false;
    }

    // Check annotation properties
    if (annotationBuilder.properties != null) {
      if (node instanceof SingleMemberAnnotation) {
        SingleMemberAnnotation singleMemberAnnotation = (SingleMemberAnnotation) node;
        if (! checkProperties(singleMemberAnnotation)) {
          return false;
        }
      } else if (node instanceof NormalAnnotation) {
        NormalAnnotation normalAnnotation = (NormalAnnotation) node;
        if (! checkProperties(normalAnnotation)) {
          return false;
        }
      }
    }

    // Check annotation predicate
    return ! annotationBuilder.annotationPredicate.isPresent() || annotationBuilder.annotationPredicate.get().test((Annotation) node);
  }


  private boolean checkProperties(SingleMemberAnnotation annotation) {
    final String expected = annotationBuilder.properties.get("value").toString();
    final String actual = annotation.getValue().toString();
    // Bit hacky because string literals contain the quotes they were delivered with
    return compareExpression(expected, actual);
  }


  private boolean checkProperties(NormalAnnotation annotation) {
    @SuppressWarnings("unchecked")
    final List<MemberValuePair> values = annotation.values();
    boolean matches = true;
    for (MemberValuePair pair : values) {
      final String key = pair.getName().getIdentifier();
      final Object expectedValue = annotationBuilder.properties.get(key);
      if (expectedValue != null) {
        matches = matches && compareExpression(expectedValue.toString(), pair.getValue().toString());
      }
    }
    return matches;
  }


  private boolean compareExpression(final String expected, final String actual) {
    return actual.equals(expected) || actual.equals("\"" + expected + "\"");
  }
}
