package org.alfasoftware.astra.core.matchers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.alfasoftware.astra.core.utils.AstraUtils;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;

/**
 * A way of matching a type by its properties.
 * <p>
 * Match options currently include
 * Is the type a class or interface
 * Does the name match exactly or a regular expression
 * Is it public, private or package visibility
 * Is it static, abstract or final
 * Does it extend a specific class
 * Does it implement one or more interfaces
 * Does it have any class annotations matching a list
 */
public class TypeMatcher implements Matcher {
  private final TypeBuilder typeBuilder;

  private enum Visibility {PUBLIC, PACKAGE, PRIVATE}

  /**
   * Private constructor. Creating a matcher should be through calling the build method on the enclosed builder
   *
   * @param typeBuilder the type builder to match against
   */
  private TypeMatcher(final TypeBuilder typeBuilder) {
    this.typeBuilder = typeBuilder;
  }

  /**
   * This is the query builder for a type, and once the criteria are complete it will create a matcher by calling build()
   */
  public static class TypeBuilder implements Builder {
    private Boolean isInterface;
    private Boolean isClass;
    private String typeName;
    private Set<String> interfaces;
    private String superClass;
    private Visibility visibility;
    private Boolean isStatic;
    private Boolean isAbstract;
    private Boolean isFinal;
    private String typeNameRegex;
    private Predicate<String> typeNamePredicate;

    private List<Builder> annotationBuilders;

    /**
     * Private constructor, use {@code TypeMatcher.builder} instead
     */
    private TypeBuilder() {
    }

    /**
     * Creates a matcher based on the current builder state
     *
     * @return a new matcher
     */
    @Override
    public Matcher build() {
      return new TypeMatcher(this);
    }

    /**
     * Specifies the type must be an interface. This automatically precludes it being a class or final.
     *
     * @return the builder
     */
    public TypeBuilder asInterface() {
      isInterface = true;
      return this;
    }

    /**
     * Specifies the type must be a class
     *
     * @return the builder
     */
    public TypeBuilder asClass() {
      isClass = true;
      return this;
    }

    /**
     * Specifies the type must have the exact name provided
     *
     * @param typeName the type name to match
     * @return the builder
     */
    public TypeBuilder withName(final String typeName) {
      this.typeName = typeName;
      return this;
    }

    /**
     * Specifies a regular expression to test against the type name
     *
     * @param typeNameRegex a regular expression to match the type name
     * @return the builder
     */
    public TypeBuilder withNameLike(final String typeNameRegex) {
      this.typeNameRegex = typeNameRegex;
      return this;
    }

    /**
     * Specifies the type must have the exact name provided
     *
     * @param typeNamePredicate a predicate for matching on the type name
     * @return the builder
     */
    public TypeBuilder withNamePredicate(final Predicate<String> typeNamePredicate) {
      this.typeNamePredicate = typeNamePredicate;
      return this;
    }
    

    /**
     * Defines a type this type must extend
     *
     * @param superClass the type name to match
     * @return the builder
     */
    public TypeBuilder extending(String superClass) {
      this.superClass = superClass;
      return this;
    }
    
    
    /**
     * Specifies an interface name the type must implement. This can be called multiple times for multiple interfaces
     * This will match against fully qualified names
     * i.e. Test matches against "implements org.junit.Test"
     *
     * @param interfaceNames fully qualified interface names the type must implement
     * @return the builder
     */
    public TypeBuilder implementingInterfaces(Set<String> interfaceNames) {
      interfaces = interfaceNames;
      return this;
    }

    /**
     * Specifies a type annotation to match. See {@link org.alfasoftware.astra.core.matchers.AnnotationMatcher.AnnotationBuilder}
     * Multiple annotations can be added using the pattern below repeatedly
     * .withAnnotation().[annotation criteria].endTypeAnnotation()
     *
     * @return an annotation builder allowing criteria for the annotation to be set
     */
    public AnnotationMatcher.AnnotationBuilder withAnnotation() {
      if (annotationBuilders == null) {
        annotationBuilders = new ArrayList<>();
      }
      AnnotationMatcher.AnnotationBuilder builder = AnnotationMatcher.builder(this);
      annotationBuilders.add(builder);
      return builder;
    }

    /**
     * Specifies the type must have public visibility
     *
     * @return the builder
     */
    public TypeBuilder withPublicVisibility() {
      visibility = Visibility.PUBLIC;
      return this;
    }

    /**
     * Specifies the type must have package visibility
     *
     * @return the builder
     */
    public TypeBuilder withPackageVisibility() {
      visibility = Visibility.PACKAGE;
      return this;
    }

    /**
     * Specifies the type must have private visibility
     *
     * @return the builder
     */
    public TypeBuilder withPrivateVisibility() {
      visibility = Visibility.PRIVATE;
      return this;
    }

    /**
     * Specifies the type must be static
     *
     * @return the builder
     */
    public TypeBuilder isStatic() {
      isStatic = true;
      return this;
    }

    /**
     * Specifies the type must be abstract
     *
     * @return the builder
     */
    public TypeBuilder isAbstract() {
      isAbstract = true;
      return this;
    }

    /**
     * Specifies the type must be final
     *
     * @return the builder
     */
    public TypeBuilder isFinal() {
      isFinal = true;
      return this;
    }
  }

  /**
   * Creates a new type builder
   *
   * @return as above
   */
  public static TypeBuilder builder() {
    return new TypeBuilder();
  }

  /**
   * Checks the node against the builder to see if it matches
   *
   * @param node the AST node to check
   * @return true if it is a match, false otherwise
   */
  @Override
  public boolean matches(ASTNode node) {
    TypeDeclaration typeDeclaration;
    if (node instanceof TypeDeclaration) {
      typeDeclaration = (TypeDeclaration) node;
    } else {
      return false;
    }
    
    boolean matches = true;
    
    if (matches && typeBuilder.isInterface != null) {
      matches = typeBuilder.isInterface == typeDeclaration.isInterface();
    }
    if (matches && typeBuilder.isClass != null) {
      matches = typeBuilder.isClass == ! typeDeclaration.isInterface();
    }
    if (matches && typeBuilder.typeName != null) {
      matches = checkTypeName(typeDeclaration);
    }
    if (matches && typeBuilder.typeNameRegex != null) {
      matches = checkClassNameRegex(typeDeclaration);
    }
    if (matches && typeBuilder.typeNamePredicate != null) {
      matches = checkTypeNamePredicate(typeDeclaration);
    }
    if (matches && typeBuilder.interfaces != null) {
      matches = checkInterfaces(typeDeclaration);
    }
    if (matches && typeBuilder.annotationBuilders != null) {
      matches = checkAnnotations(typeDeclaration);
    }
    if (matches && typeBuilder.visibility != null) {
      matches = checkVisibility(typeDeclaration);
    }
    if (matches && typeBuilder.isStatic != null) {
      matches = checkIsStatic(typeDeclaration);
    }
    if (matches && typeBuilder.isAbstract != null) {
      matches = checkIsAbstract(typeDeclaration);
    }
    if (matches && typeBuilder.isFinal != null) {
      matches = checkIsFinal(typeDeclaration);
    }
    if (matches && typeBuilder.superClass != null) {
      Type superclassType = typeDeclaration.getSuperclassType();
      if (superclassType == null || superclassType.resolveBinding() == null) {
        matches = false;
      // If we have specified a parameterized supertype, match on the qualified name
      } else if (typeBuilder.superClass.contains("<")) {
        if (! typeBuilder.superClass.equals(superclassType.resolveBinding().getQualifiedName())) {
          matches = false;
        }
      // If we have not specified a paramaterized supertype, then only match on binary type name
      } else if (! typeBuilder.superClass.equals(superclassType.resolveBinding().getBinaryName())) {
        matches = false;
      }
    }

    return matches;
  }

  /**
   * Checks the name of the type against the expected name
   *
   * @param typeDeclaration the type being checked
   * @return true if it matches, false otherwise
   */
  private boolean checkTypeName(final TypeDeclaration typeDeclaration) {
    return typeBuilder.typeName.equals(AstraUtils.getFullyQualifiedName(typeDeclaration));
  }

  /**
   * Checks the name of the type against the provided regular expression
   *
   * @param typeDeclaration the type being checked
   * @return true if it matches, false otherwise
   */
  private boolean checkClassNameRegex(final TypeDeclaration typeDeclaration) {
    return AstraUtils.getFullyQualifiedName(typeDeclaration).matches(typeBuilder.typeNameRegex);
  }
  
  /**
   * Checks the name of the type against the predicate
   *
   * @param typeDeclaration the type being checked
   * @return true if it matches, false otherwise
   */
  private boolean checkTypeNamePredicate(final TypeDeclaration typeDeclaration) {
    return typeBuilder.typeNamePredicate.test(AstraUtils.getFullyQualifiedName(typeDeclaration));
  }

  /**
   * Checks the each supplied fully qualified interface name against interfaces implemented by the type
   *
   * @param typeDeclaration the type being checked
   * @return true if all interfaces expected are present, false otherwise
   */
  private boolean checkInterfaces(final TypeDeclaration typeDeclaration) {
    @SuppressWarnings("unchecked")
    List<Type> interfaces = typeDeclaration.superInterfaceTypes();
    Set<String> actualInterfaceNames = new HashSet<>();
    for (Type interfaceName : interfaces) {
      actualInterfaceNames.add(AstraUtils.getFullyQualifiedName(interfaceName));
    }
    return actualInterfaceNames.containsAll(typeBuilder.interfaces);
  }


  /**
   * Checks the each supplied annotation matcher against the annotations on the type.
   *
   * @param typeDeclaration the type being checked
   * @return true if all annotations expected are present, false otherwise
   */
  private boolean checkAnnotations(final TypeDeclaration typeDeclaration) {
    for (Builder annotationBuilder : typeBuilder.annotationBuilders) {
      boolean found = false;
      for (Object modifier : typeDeclaration.modifiers()) {
        if (modifier instanceof Annotation) {
          Annotation marker = (Annotation) modifier;
          if (annotationBuilder.build().matches(marker)) {
            found = true;
          }
        }
      }
      if (! found) {
        return false;
      }
    }
    return true;
  }
    

  /**
   * Checks the type visibility against the expected one
   *
   * @param typeDeclaration type to check
   * @return true if the visibilty is correct, false otherwise
   */
  private boolean checkVisibility(final TypeDeclaration typeDeclaration) {
    boolean isPublic = checkForModifier(typeDeclaration, Modifier::isPublic);
    boolean isPrivate = checkForModifier(typeDeclaration, Modifier::isPrivate);
    return isPublic && typeBuilder.visibility == Visibility.PUBLIC ||
            isPrivate && typeBuilder.visibility == Visibility.PRIVATE ||
            !isPublic && !isPrivate && typeBuilder.visibility == Visibility.PACKAGE;
  }

  /**
   * Checks to see if the type is static
   *
   * @param typeDeclaration the type to check
   * @return true if it is static, false otherwise
   */
  private boolean checkIsStatic(final TypeDeclaration typeDeclaration) {
    return checkForModifier(typeDeclaration, Modifier::isStatic);
  }

  /**
   * Checks to see if the type is abstract
   *
   * @param typeDeclaration the type to check
   * @return true if it is abstract, false otherwise
   */
  private boolean checkIsAbstract(final TypeDeclaration typeDeclaration) {
    return checkForModifier(typeDeclaration, Modifier::isAbstract);
  }

  /**
   * Checks to see if the type is final
   *
   * @param typeDeclaration the type to check
   * @return true if it is final, false otherwise
   */
  private boolean checkIsFinal(final TypeDeclaration typeDeclaration) {
    return checkForModifier(typeDeclaration, Modifier::isFinal);
  }

  /**
   * helper method to check for specific type modifiers
   *
   * @param typeDeclaration the type to check
   * @param check           a predicate to check on the type
   * @return the result of the predicate as applied to the type definition
   */
  private boolean checkForModifier(final TypeDeclaration typeDeclaration, Predicate<Modifier> check) {
    for (Object modifier : typeDeclaration.modifiers()) {
      if (modifier instanceof Modifier) {
        Modifier theModifier = (Modifier) modifier;
        if (check.test(theModifier)) {
          return true;
        }
      }
    }
    return false;
  }
}
