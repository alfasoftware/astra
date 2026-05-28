package org.alfasoftware.astra.core.matchers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.alfasoftware.astra.core.utils.AstraUtils;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.RecordDeclaration;
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
    private Boolean isRecord;
    private String typeName;
    private Set<String> interfaces;
    private String superClass;
    private Set<String> permittedTypes;
    private Visibility visibility;
    private Boolean isStatic;
    private Boolean isAbstract;
    private Boolean isFinal;
    private Boolean isSealed;
    private Boolean isNonSealed;
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
     * Specifies the type must be a record.
     *
     * @return the builder
     */
    public TypeBuilder asRecord() {
      isRecord = true;
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
     * Specifies fully qualified names that the type must declare in its {@code permits} clause.
     * This can be used to match a sealed type by the subtypes it permits.
     * All supplied names must be present in the permits clause for the type to match.
     *
     * @param permittedTypeNames fully qualified names the type must permit
     * @return the builder
     */
    public TypeBuilder permitting(Set<String> permittedTypeNames) {
      permittedTypes = permittedTypeNames;
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

    /**
     * Specifies the type must be sealed.
     *
     * @return the builder
     */
    public TypeBuilder isSealed() {
      isSealed = true;
      return this;
    }

    /**
     * Specifies the type must be non-sealed.
     *
     * @return the builder
     */
    public TypeBuilder isNonSealed() {
      isNonSealed = true;
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

    if (!(node instanceof AbstractTypeDeclaration)) {
      return false;
    }
    AbstractTypeDeclaration typeDeclaration = (AbstractTypeDeclaration) node;

    // Class/interface/record gates. A record only matches asRecord(), and asClass()/asInterface()
    // only match TypeDeclarations - mirroring how enums are excluded.
    if (! checkIsRecord(node)) {
      return false;
    }
    if (node instanceof TypeDeclaration) {
      if (! checkIsInterface((TypeDeclaration) node)) {
        return false;
      }
      if (! checkIsClass((TypeDeclaration) node)) {
        return false;
      }
    } else if (Boolean.TRUE.equals(typeBuilder.isClass) || Boolean.TRUE.equals(typeBuilder.isInterface)) {
      return false;
    }

    if (! checkTypeName(typeDeclaration)) {
      return false;
    }
    if (! checkClassNameRegex(typeDeclaration)) {
      return false;
    }
    if (! checkTypeNamePredicate(typeDeclaration)) {
      return false;
    }
    if (! checkInterfaces(typeDeclaration)) {
      return false;
    }
    if (! checkAnnotations(typeDeclaration)) {
      return false;
    }
    if (! checkVisibility(typeDeclaration)) {
      return false;
    }
    if (! checkIsStatic(typeDeclaration)) {
      return false;
    }
    if (! checkIsAbstract(typeDeclaration)) {
      return false;
    }
    if (! checkIsFinal(typeDeclaration)) {
      return false;
    }
    if (! checkIsSealed(typeDeclaration)) {
      return false;
    }
    if (! checkIsNonSealed(typeDeclaration)) {
      return false;
    }
    if (! checkPermittedTypes(node)) {
      return false;
    }

    // Superclasses only apply to a TypeDeclaration. If a superclass was required, anything else fails.
    if (node instanceof TypeDeclaration) {
      return checkSuperclass((TypeDeclaration) node);
    }
    return typeBuilder.superClass == null;
  }

  private boolean checkIsRecord(ASTNode node) {
    return typeBuilder.isRecord == null || (typeBuilder.isRecord == (node instanceof RecordDeclaration));
  }

  /**
   * Checks the fully qualified names declared in the type's {@code permits} clause against those expected.
   *
   * @param node the node being checked
   * @return true if all expected permitted types are present, false otherwise
   */
  private boolean checkPermittedTypes(ASTNode node) {
    if (typeBuilder.permittedTypes == null) {
      return true;
    }
    if (! (node instanceof TypeDeclaration)) {
      return false;
    }

    @SuppressWarnings("unchecked")
    List<Type> declaredPermittedTypes = ((TypeDeclaration) node).permittedTypes();
    Set<String> permittedNames = new HashSet<>();
    for (Type permittedType : declaredPermittedTypes) {
      ITypeBinding binding = permittedType.resolveBinding();
      if (binding != null) {
        permittedNames.add(binding.getQualifiedName());
        permittedNames.add(binding.getBinaryName());
      }
    }
    return permittedNames.containsAll(typeBuilder.permittedTypes);
  }

  private boolean checkIsSealed(AbstractTypeDeclaration typeDeclaration) {
    return typeBuilder.isSealed == null || checkForModifier(typeDeclaration, Modifier::isSealed);
  }

  private boolean checkIsNonSealed(AbstractTypeDeclaration typeDeclaration) {
    return typeBuilder.isNonSealed == null || checkForModifier(typeDeclaration, Modifier::isNonSealed);
  }

  private boolean checkSuperclass(TypeDeclaration typeDeclaration) {
    if (typeBuilder.superClass != null) {
      Type superclassType = typeDeclaration.getSuperclassType();
      if (superclassType == null) {
        return false;
      }
      ITypeBinding superclassBinding = superclassType.resolveBinding();
      if (superclassBinding == null) {
        return false;
      }
      Set<ITypeBinding> superClasses = getAllSuperClasses(superclassBinding, new HashSet<>());

      // If we have specified a parameterized supertype, match on the qualified name
      if (typeBuilder.superClass.contains("<")) {
        if (superClasses.stream().noneMatch(c -> typeBuilder.superClass.equals(c.getQualifiedName()))) {
          return false;
        }
      // If we have not specified a parameterized supertype, then only match on binary type name
      } else if (superClasses.stream().noneMatch(c -> typeBuilder.superClass.equals(c.getBinaryName()))) {
        return false;
      }
    }
    return true;
  }


  private Set<ITypeBinding> getAllSuperClasses(ITypeBinding typeBinding, Set<ITypeBinding> superClasses) {
    if (typeBinding != null) {
      superClasses.add(typeBinding);
      getAllSuperClasses(typeBinding.getSuperclass(), superClasses);
    }
    return superClasses;
  }


  private boolean checkIsClass(TypeDeclaration typeDeclaration) {
    return typeBuilder.isClass == null || typeBuilder.isClass != typeDeclaration.isInterface();
  }

  private boolean checkIsInterface(TypeDeclaration typeDeclaration) {
    return typeBuilder.isInterface == null || typeBuilder.isInterface == typeDeclaration.isInterface();
  }

  /**
   * Checks the name of the type against the expected name
   *
   * @param typeDeclaration the type being checked
   * @return true if it matches, false otherwise
   */
  private boolean checkTypeName(final AbstractTypeDeclaration typeDeclaration) {
    return typeBuilder.typeName == null ||
        typeBuilder.typeName.equals(AstraUtils.getFullyQualifiedName(typeDeclaration));
  }

  /**
   * Checks the name of the type against the provided regular expression
   *
   * @param typeDeclaration the type being checked
   * @return true if it matches, false otherwise
   */
  private boolean checkClassNameRegex(final AbstractTypeDeclaration typeDeclaration) {
    return typeBuilder.typeNameRegex == null ||
        AstraUtils.getFullyQualifiedName(typeDeclaration).matches(typeBuilder.typeNameRegex);
  }

  /**
   * Checks the name of the type against the predicate
   *
   * @param typeDeclaration the type being checked
   * @return true if it matches, false otherwise
   */
  private boolean checkTypeNamePredicate(final AbstractTypeDeclaration typeDeclaration) {
    return typeBuilder.typeNamePredicate == null ||
        typeBuilder.typeNamePredicate.test(AstraUtils.getFullyQualifiedName(typeDeclaration));
  }

  /**
   * Checks the each supplied fully qualified interface name against interfaces implemented by the type
   *
   * @param typeDeclaration the type being checked
   * @return true if all interfaces expected are present, false otherwise
   */
  private boolean checkInterfaces(final AbstractTypeDeclaration typeDeclaration) {
    if (typeBuilder.interfaces == null) {
      return true;
    }

    final Set<ITypeBinding> allInterfaces = getAllInterfaces(typeDeclaration);

    for (String interfaceToFind : typeBuilder.interfaces) {
      // If we have specified a parameterized supertype, match on the qualified name
      if (interfaceToFind.contains("<")) {
        if (allInterfaces.stream().noneMatch(c -> interfaceToFind.equals(c.getQualifiedName()))) {
          return false;
        }
        // If we have not specified a parameterized supertype, then only match on binary type name
      } else if (allInterfaces.stream().noneMatch(c -> interfaceToFind.equals(c.getBinaryName()))) {
        return false;
      }
    }

    return true;
  }


  private Set<ITypeBinding> getAllInterfaces(AbstractTypeDeclaration typeDeclaration) {
    List<Type> superInterfaceTypes = getSuperInterfaceTypes(typeDeclaration);
    Set<ITypeBinding> interfaces = new HashSet<>();
    for (Type interfaceName : superInterfaceTypes) {
      interfaces.add(interfaceName.resolveBinding());
      getSuperInterfaces(interfaceName.resolveBinding(), interfaces);
    }
    getSuperClassInterfaces(typeDeclaration.resolveBinding(), interfaces);
    return interfaces;
  }


  /**
   * Returns the declared super-interface types for a type. Records, enums and ordinary types each
   * expose these separately, so they are unified here.
   */
  @SuppressWarnings("unchecked")
  private List<Type> getSuperInterfaceTypes(AbstractTypeDeclaration typeDeclaration) {
    if (typeDeclaration instanceof TypeDeclaration) {
      return ((TypeDeclaration) typeDeclaration).superInterfaceTypes();
    }
    if (typeDeclaration instanceof RecordDeclaration) {
      return ((RecordDeclaration) typeDeclaration).superInterfaceTypes();
    }
    if (typeDeclaration instanceof EnumDeclaration) {
      return ((EnumDeclaration) typeDeclaration).superInterfaceTypes();
    }
    return new ArrayList<>();
  }


  private void getSuperClassInterfaces(ITypeBinding child, Set<ITypeBinding> interfaces) {
    if (child != null && child.getSuperclass() != null) {
      for (ITypeBinding superClassInterface : child.getSuperclass().getInterfaces()) {
        interfaces.add(superClassInterface);
        getSuperInterfaces(superClassInterface, interfaces);
      }
      getSuperClassInterfaces(child.getSuperclass(), interfaces);
    }
  }


  private void getSuperInterfaces(ITypeBinding child, Set<ITypeBinding> interfaces) {
    if (child != null && child.getInterfaces() != null) {
      for (ITypeBinding superClassInterface : child.getInterfaces()) {
        interfaces.add(superClassInterface);
        getSuperInterfaces(superClassInterface, interfaces);
      }
    }
  }


  /**
   * Checks the each supplied annotation matcher against the annotations on the type.
   *
   * @param typeDeclaration the type being checked
   * @return true if all annotations expected are present, false otherwise
   */
  private boolean checkAnnotations(final AbstractTypeDeclaration typeDeclaration) {
    if (typeBuilder.annotationBuilders == null) {
      return true;
    }

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
  private boolean checkVisibility(final AbstractTypeDeclaration typeDeclaration) {
    if (typeBuilder.visibility == null) {
      return true;
    }

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
  private boolean checkIsStatic(final AbstractTypeDeclaration typeDeclaration) {
    return typeBuilder.isStatic == null || checkForModifier(typeDeclaration, Modifier::isStatic);
  }

  /**
   * Checks to see if the type is abstract
   *
   * @param typeDeclaration the type to check
   * @return true if it is abstract, false otherwise
   */
  private boolean checkIsAbstract(final AbstractTypeDeclaration typeDeclaration) {
    return typeBuilder.isAbstract == null || checkForModifier(typeDeclaration, Modifier::isAbstract);
  }

  /**
   * Checks to see if the type is final
   *
   * @param typeDeclaration the type to check
   * @return true if it is final, false otherwise
   */
  private boolean checkIsFinal(final AbstractTypeDeclaration typeDeclaration) {
    return typeBuilder.isFinal == null || checkForModifier(typeDeclaration, Modifier::isFinal);
  }

  /**
   * helper method to check for specific type modifiers
   *
   * @param typeDeclaration the type to check
   * @param check           a predicate to check on the type
   * @return the result of the predicate as applied to the type definition
   */
  private boolean checkForModifier(final AbstractTypeDeclaration typeDeclaration, Predicate<Modifier> check) {
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
