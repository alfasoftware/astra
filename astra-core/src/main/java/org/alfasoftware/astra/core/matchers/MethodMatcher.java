package org.alfasoftware.astra.core.matchers;

import static org.alfasoftware.astra.core.matchers.DescribedPredicate.describedPredicate;
import static org.alfasoftware.astra.core.utils.AstraUtils.CLASSPATHS_MISSING_WARNING;
import static org.alfasoftware.astra.core.utils.AstraUtils.getName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.alfasoftware.astra.core.utils.AstraUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;

/**
 * A way of matching a method by its properties.
 * Examples of method properties that can be used to match include the method name,
 * arguments, the fully qualified name of the declaring type, and the return type.
 */
public class MethodMatcher {

  private static final Logger log = LoggerFactory.getLogger(MethodMatcher.class);

  private Optional<DescribedPredicate<String>> fullyQualifiedDeclaringTypePredicate = Optional.empty();
  private Optional<String> fullyQualifiedDeclaringTypeExactName = Optional.empty();
  private Optional<DescribedPredicate<String>> methodNamePredicate = Optional.empty();
  private Optional<String> methodNameExactName = Optional.empty();
  private Optional<List<String>> fullyQualifiedParameterNames = Optional.empty();
  private Optional<Boolean> isVarargs = Optional.empty();
  private Optional<MethodMatcher> parentContextMatcher = Optional.empty();
  private Optional<DescribedPredicate<String>> returnTypePredicate = Optional.empty();
  private Optional<DescribedPredicate<? super ASTNode>> customPredicate = Optional.empty();
  private Optional<List<String>> requiredAnnotations = Optional.empty();


  private MethodMatcher(Builder builder) {
    this.fullyQualifiedDeclaringTypePredicate = builder.fullyQualifiedDeclaringTypePredicate;
    this.fullyQualifiedDeclaringTypeExactName = builder.fullyQualifiedDeclaringTypeExactMatch;
    this.methodNamePredicate = builder.methodNamePredicate;
    this.methodNameExactName = builder.methodNameExactMatch;
    this.fullyQualifiedParameterNames = builder.fullyQualifiedParameterNames;
    this.isVarargs = builder.isVarargs;
    this.parentContextMatcher = builder.parentContext;
    this.returnTypePredicate = builder.returnTypePredicate; // only implemented for MethodDeclarations so far
    this.customPredicate = builder.customPredicate;
    this.requiredAnnotations = builder.requiredAnnotations;
  }

  /**
   * @return a new instance to build a MethodMatcher.
   */
  public static Builder builder() {
    return new Builder();
  }


  /**
   * Builder for setting up {@link MethodMatcher}s.
   */
  public static class Builder {
    private Optional<DescribedPredicate<String>> methodNamePredicate = Optional.empty();
    private Optional<String> methodNameExactMatch = Optional.empty();
    private Optional<DescribedPredicate<String>> fullyQualifiedDeclaringTypePredicate = Optional.empty();
    private Optional<String> fullyQualifiedDeclaringTypeExactMatch = Optional.empty();
    private Optional<List<String>> fullyQualifiedParameterNames = Optional.empty();
    private Optional<Boolean> isVarargs = Optional.empty();
    private Optional<MethodMatcher> parentContext = Optional.empty();
    private Optional<DescribedPredicate<String>> returnTypePredicate = Optional.empty();
    private Optional<DescribedPredicate<? super ASTNode>> customPredicate = Optional.empty();
    private Optional<List<String>> requiredAnnotations = Optional.empty();


    /**
     * Don't construct this directly - use the static method.
     */
    private Builder() {
      super();
    }

    public Builder withFullyQualifiedDeclaringType(String fullyQualifiedDeclaringType) {
      // Removing $ from inner class names as this won't match with resolved type binding names
      String exactName = fullyQualifiedDeclaringType.replaceAll("\\$", ".");
      this.fullyQualifiedDeclaringTypeExactMatch = Optional.of(exactName);
      this.fullyQualifiedDeclaringTypePredicate = Optional.of(describedPredicate("FQ type is [" + fullyQualifiedDeclaringType + "]", Predicate.isEqual(exactName)));
      return this;
    }
    public Builder withFullyQualifiedDeclaringType(DescribedPredicate<String> fullyQualifiedDeclaringTypePredicate) {
      this.fullyQualifiedDeclaringTypePredicate = Optional.of(fullyQualifiedDeclaringTypePredicate);
      return this;
    }
    public Builder withMethodName(String methodName) {
      this.methodNameExactMatch = Optional.of(methodName);
      this.methodNamePredicate = Optional.of(describedPredicate("method name is [" + methodName + "]", Predicate.isEqual(methodName)));
      return this;
    }
    public Builder withMethodName(DescribedPredicate<String> methodNamePredicate) {
      this.methodNamePredicate = Optional.of(methodNamePredicate);
      return this;
    }
    public Builder withFullyQualifiedParameters(List<String> fullyQualifiedParameterNames) {
      this.fullyQualifiedParameterNames = Optional.of(fullyQualifiedParameterNames)
       // Removing $ from inner class names as this won't match with resolved type binding names
          .map(names -> names.stream().map(name -> name.replaceAll("\\$", ".")).collect(Collectors.toList()));
      return this;
    }
    public Builder withParentContext(MethodMatcher parentContextMatcher) {
      this.parentContext = Optional.of(parentContextMatcher);
      return this;
    }
    public Builder isVarargs(boolean isVarargs) {
      this.isVarargs = Optional.of(isVarargs);
      return this;
    }
    public Builder withReturnType(DescribedPredicate<String> returnTypePredicate) {
      this.returnTypePredicate = Optional.of(returnTypePredicate);
      return this;
    }
    public Builder withfullyQualifiedReturnType(String fullyQualifiedReturnType) {
      this.returnTypePredicate = Optional.of(describedPredicate("method return type is [" + fullyQualifiedReturnType + "]", Predicate.isEqual(fullyQualifiedReturnType)));
      return this;
    }
    public Builder withCustomPredicate(DescribedPredicate<? super ASTNode> customInvocationPredicate) {
      this.customPredicate = Optional.of(customInvocationPredicate);
      return this;
    }

    /**
     * Specifies a fully qualified annotation name that the method must be annotated with.
     * Can be called multiple times; all supplied annotations must be present (AND semantics).
     * Works for method declarations and also for invocation sites (by resolving the binding
     * to the declaring method's annotations), making it possible to find all calls to
     * methods carrying a given annotation — e.g. {@code withAnnotation("java.lang.Deprecated")}.
     *
     * @param fullyQualifiedAnnotationName the fully qualified name of the required annotation
     * @return the builder
     */
    public Builder withAnnotation(String fullyQualifiedAnnotationName) {
    	  if (this.requiredAnnotations.isEmpty()) {
    	    this.requiredAnnotations = Optional.of(new ArrayList<>());
    	  }
    	
      this.requiredAnnotations.get().add(fullyQualifiedAnnotationName.replaceAll("\\$", "."));
      return this;
    }

    public MethodMatcher build() {
      return new MethodMatcher(this);
    }
  }
  
  
  /**
   * For a method signature, returns a method matcher.
   * Example valid inputs:
   *
   * <ul>
   *  <li>com.Foo.doFoo()<li>
   *  <li>com.Foo.doFoo(int,com.Bar)<li>
   *  <li>com.Foo.doFoo(int, com.Bar)<li>
   * </ul>
   */
  public static MethodMatcher buildMethodMatcherForFQSignature(String fqSignature) {
    final String trimmed = fqSignature.trim();
    final String[] split = trimmed.split("\\(");
    
    List<String> typeSplits = new ArrayList<>(Arrays.asList(split[0].split("\\.")));
    String methodName = typeSplits.get(typeSplits.size() - 1);
    typeSplits.remove(typeSplits.size() - 1);
    String declaringType = String.join(".", typeSplits);
    
    List<String> fullyQualifiedParametersList = new ArrayList<>();
    // If this is just ")", then no parameters
    if (split[1].length() != 1) {
      for (String parameterSplit : split[1].substring(0, split[1].indexOf(')')).split(",")) {
        fullyQualifiedParametersList.add(parameterSplit.trim());
      }
    }
    
    return MethodMatcher.builder()
            .withFullyQualifiedDeclaringType(declaringType)
            .withMethodName(methodName)
            .withFullyQualifiedParameters(fullyQualifiedParametersList)
            .build();
  }
  
  
  public static MethodMatcher buildMethodMatcherForMethodBinding(IMethodBinding methodBinding) {
    return MethodMatcher.builder()
      .withMethodName(methodBinding.getName())
      .withFullyQualifiedDeclaringType(AstraUtils.getFullyQualifiedName(methodBinding))
      .withFullyQualifiedParameters(
        Arrays.asList(methodBinding.getParameterTypes())
          .stream()
          .map(AstraUtils::getName)
        .collect(Collectors.toList()))
      .build();
  }
  

  /**
   * Checks that all required annotations are present among the provided binding annotations.
   * Returns {@code true} if no annotations were specified.
   */
  private boolean isAnnotationMatch(IAnnotationBinding[] annotations) {
    if (requiredAnnotations.isEmpty() || requiredAnnotations.get().isEmpty()) {
      return true;
    }
    Set<String> presentAnnotations = Arrays.stream(annotations)
        .map(IAnnotationBinding::getAnnotationType)
        .filter(Objects::nonNull)
        .map(ITypeBinding::getQualifiedName)
        .collect(Collectors.toSet());
    return presentAnnotations.containsAll(requiredAnnotations.get());
  }


  /**
   * AST-level annotation check for {@link MethodDeclaration} nodes, used as a fallback
   * when no binding is available. Matches against resolved annotation type names where
   * possible, or against the unresolved annotation name as a last resort.
   */
  private boolean isAnnotationMatchFromAST(MethodDeclaration methodDeclaration) {
    if (requiredAnnotations.isEmpty() || requiredAnnotations.get().isEmpty()) {
      return true;
    }
    @SuppressWarnings("unchecked")
    List<Object> modifiers = methodDeclaration.modifiers();
    Set<String> presentNames = new HashSet<>();
    for (Object modifier : modifiers) {
      if (modifier instanceof Annotation ann) {
        ITypeBinding typeBinding = ann.getTypeName().resolveTypeBinding();
        if (typeBinding != null) {
          presentNames.add(typeBinding.getQualifiedName());
        } else {
          presentNames.add(ann.getTypeName().getFullyQualifiedName());
        }
      }
    }
    return presentNames.containsAll(requiredAnnotations.get());
  }


  private boolean isMethodNameMatch(MethodInvocation mi) {
    return ! methodNamePredicate.isPresent() || methodNamePredicate.get().test(mi.getName().toString());
  }


  private boolean isFQInvocationTypeNameMatch(MethodInvocation mi, CompilationUnit cu) {
    return ! fullyQualifiedDeclaringTypePredicate.isPresent() ||
            fullyQualifiedDeclaringTypePredicate.get().test(AstraUtils.getFullyQualifiedName(mi, cu)) ||
            (mi.getExpression() != null &&  mi.getExpression().resolveTypeBinding() != null &&
              isSuperTypeOrInterfaceMatch(mi.getExpression().resolveTypeBinding(), fullyQualifiedDeclaringTypePredicate.get()));
  }

  
  private boolean isSuperTypeOrInterfaceMatch(ITypeBinding typeBinding, Predicate<String> test) {
    return isSuperTypeMatch(typeBinding, test) ||
        isInterfaceMatch(typeBinding, test);
  }
  
  
  private boolean isSuperTypeMatch(ITypeBinding resolveTypeBinding, Predicate<String> test) {
    
    // If testing for Object, then any class will match 
    if (test.test(Object.class.getName())) {
      return true;
    }
    
    final ITypeBinding superclass = resolveTypeBinding.getSuperclass();
    if (superclass != null) {
       if (test.test(getName(superclass))) {
         return true;
       } else if (superclass.getSuperclass() != null) {
         return isSuperTypeMatch(superclass, test);
       }
    }
    return false;
  }
  
  
  private boolean isInterfaceMatch(ITypeBinding typeBinding, Predicate<String> test) {
    if (test.test(getName(typeBinding))) {
      return true;
    }
    return Arrays.stream(typeBinding.getInterfaces()).anyMatch(i -> isInterfaceMatch(i, test));
  }


  private boolean isFQDeclaringTypeNameMatch(IMethodBinding mb) {
    return Optional.of(mb)
      .map(IMethodBinding::getDeclaringClass)
      .map(iTypeBinding -> iTypeBinding.isAnonymous() ? iTypeBinding.getSuperclass() : iTypeBinding)
      .map(ITypeBinding::getQualifiedName)
      .filter(n -> ! fullyQualifiedDeclaringTypePredicate.isPresent() || fullyQualifiedDeclaringTypePredicate.get().test(n))
      .isPresent();
  }


  /**
   * @return true if we aren't trying to match parameters or if the list is the same length and an exact match.
   */
  private boolean isMethodParameterListMatch(IMethodBinding mb) {
    if (!fullyQualifiedParameterNames.isPresent()) {
      return true;
    }
    if (fullyQualifiedParameterNames.get().size() != mb.getParameterTypes().length) {
      return false;
    }

    // Check the parameters are as expected, and in the correct order
    for (int i = 0; i < mb.getParameterTypes().length; i++) {
      
      if (! isTypeBindingMatch(mb.getParameterTypes()[i], fullyQualifiedParameterNames.get().get(i))) {
        return false;
      }
    }
    // if we get here, all parameter types must match our expectations
    return true;
  }


  private boolean isTypeBindingMatch(ITypeBinding resolveTypeBinding, String test) {
    if (getName(resolveTypeBinding) != null &&
        (getName(resolveTypeBinding).equals(test) ||
         getName(resolveTypeBinding).replaceAll("\\$", ".").equals(test))) {
      return true; 
    } else if (isSuperTypeOrInterfaceMatch(resolveTypeBinding, test::equals)) {
      return true;
    } else if (resolveTypeBinding.isArray() && test.endsWith("[]")) {
      return isSuperTypeOrInterfaceMatch(resolveTypeBinding.getComponentType(), s -> test.substring(0, test.indexOf("[")).equals(s));
    }
    return false;
  }
  

  private boolean isMethodVarargs(IMethodBinding mb) {
    return Optional.of(mb)
      .filter(IMethodBinding::isVarargs)
      .isPresent();
  }

  
  private boolean isSimpleNameMatch(ClassInstanceCreation cic) {
    return ! methodNamePredicate.isPresent() || methodNamePredicate.get().test(AstraUtils.getSimpleName(cic.getType().toString()));
  }

  
  private boolean isSimpleNameMatch(IMethodBinding iMethodBinding) {
    return ! methodNamePredicate.isPresent() || methodNamePredicate.get().test(AstraUtils.getSimpleName(iMethodBinding.getName()));
  }

  
  private boolean isCICFQTypeNameMatch(IMethodBinding iMethodBinding) {
    final Optional<IMethodBinding> binding = Optional.of(iMethodBinding);

    return binding
      .filter(this::isFQDeclaringTypeNameMatch)
      .isPresent();
  }


  private boolean isCICFQTypeNameMatch(ClassInstanceCreation cic) {
    final Optional<IMethodBinding> binding = Optional.of(cic)
      .map(ClassInstanceCreation::resolveConstructorBinding);

    if (! binding.isPresent()) {
      log.debug("Binding not found for constructor of class instance creation. "
          + CLASSPATHS_MISSING_WARNING
          + "Class instance creation: [" + cic + "]");
    }

    return binding
      .filter(this::isFQDeclaringTypeNameMatch)
      .isPresent();
  }


  /**
   * @return true if there is a required parent invocation pattern that this matches
   */
  private boolean parentInvocationMatches(MethodInvocation methodInvocation, CompilationUnit compilationUnit) {
    if (!parentContextMatcher.isPresent()) {
      return true;
    }
    if (methodInvocation.getParent() instanceof MethodInvocation) {
      return parentContextMatcher.get().matches((MethodInvocation)methodInvocation.getParent(), compilationUnit);
    }
    if (methodInvocation.getParent() instanceof ClassInstanceCreation) {
      return parentContextMatcher.get().matches((ClassInstanceCreation)methodInvocation.getParent());
    }
    return false;
  }


  public boolean matches(MethodInvocation methodInvocation, CompilationUnit compilationUnit) {

    if (! isMethodNameMatch(methodInvocation)) {
      return false;
    }

    if (! isFQInvocationTypeNameMatch(methodInvocation, compilationUnit)) {
      return false;
    }

    if (! parentInvocationMatches(methodInvocation, compilationUnit)) {
      return false;
    }

    if (fullyQualifiedParameterNames.isPresent() || isVarargs.isPresent() || ! requiredAnnotations.isEmpty()) {
      final Optional<IMethodBinding> binding = Optional.of(methodInvocation)
          .map(MethodInvocation::resolveMethodBinding);

      if (! binding.isPresent()) {
        log.debug("Binding not found for method invocation. "
            + CLASSPATHS_MISSING_WARNING
            + "Method invocation: [" + methodInvocation + "]");
      }
      if (! binding
          .filter(mb -> ! isVarargs.isPresent() || isMethodVarargs(mb))
          .filter(this::isMethodParameterListMatch)
          .filter(mb -> isAnnotationMatch(mb.getAnnotations()))
          .isPresent()) {
        return false;
      }
    }

    return ! customPredicate.isPresent() || customPredicate.get().test(methodInvocation);
  }


  public boolean matches(ClassInstanceCreation classInstanceCreation) {

    return Optional.of(classInstanceCreation)
      // does the constructor name match?
        .filter(this::isSimpleNameMatch)
      // is that constructor declared on the type we're looking for?
        .filter(this::isCICFQTypeNameMatch)
      // do the parameters match?
        .filter(cic -> isMethodParameterListMatch(cic.resolveConstructorBinding()))
      // if we're checking whether it's varargs, does it match our expectation?
        .filter(cic ->
          ! isVarargs.isPresent() || isMethodVarargs(cic.resolveConstructorBinding())
        )
      // do the required annotations match?
        .filter(cic -> cic.resolveConstructorBinding() != null &&
            isAnnotationMatch(cic.resolveConstructorBinding().getAnnotations()))
      // does the classInstanceCreation match the custom predicate
        .filter(cic -> !customPredicate.isPresent() || customPredicate.get().test(cic))
        .isPresent();
  }

  public boolean matches(IMethodBinding iMethodBinding) {

    return Optional.of(iMethodBinding)
        // does the constructor name match?
        .filter(this::isSimpleNameMatch)
        // is that constructor declared on the type we're looking for?
        .filter(this::isCICFQTypeNameMatch)
        // do the parameters match?
        .filter(this::isMethodParameterListMatch)
        // if we're checking whether it's varargs, does it match our expectation?
        .filter(imb ->
          ! isVarargs.isPresent() || isMethodVarargs(imb)
        )
        // do the required annotations match?
        .filter(imb -> isAnnotationMatch(imb.getAnnotations()))
        .isPresent();
  }


  public boolean matches(MethodDeclaration methodDeclaration) {
    final Optional<MethodDeclaration> method = Optional.of(methodDeclaration)
      // does the method name match?
      .filter(m -> !methodNamePredicate.isPresent() || methodNamePredicate.get().test(m.getName().toString()))
      // does the return type match?
      .filter(m -> !returnTypePredicate.isPresent() || returnTypePredicate.get().test(AstraUtils.getFullyQualifiedName(m.getReturnType2())));

    final Optional<IMethodBinding> binding = method
      // Now on to the things that require a resolved binding
      .map(MethodDeclaration::resolveBinding);

    if (method.isPresent() && ! binding.isPresent()) {
      log.debug("Binding not found for method declaration. "
          + CLASSPATHS_MISSING_WARNING
          + "Method declaration: [" + methodDeclaration + "]");
    }
    
    if (customPredicate.isPresent() && ! customPredicate.get().test(methodDeclaration)) {
      return false;
    }

    // Annotation check: prefer binding-based resolution; fall back to AST modifiers
    if (binding.isPresent()) {
      if (! isAnnotationMatch(binding.get().getAnnotations())) {
        return false;
      }
    } else if (! isAnnotationMatchFromAST(methodDeclaration)) {
      return false;
    }

    return binding
        // is that method declared on the type we're looking for?
        .filter(this::isFQDeclaringTypeNameMatch)
        // do the parameters match?
        .filter(mb -> ! isVarargs.isPresent() || isMethodVarargs(mb))
        .filter(this::isMethodParameterListMatch)
        .isPresent();
  }

  
  public Optional<DescribedPredicate<String>> getFullyQualifiedDeclaringType() {
    return fullyQualifiedDeclaringTypePredicate;
  }

  public Optional<String> getFullyQualifiedDeclaringTypeExactName() {
    return fullyQualifiedDeclaringTypeExactName;
  }

  public Optional<DescribedPredicate<String>> getMethodName() {
    return methodNamePredicate;
  }

  public Optional<String> getMethodNameExactName() {
    return methodNameExactName;
  }

  public boolean isVarargs() {
    return isVarargs.orElse(false);
  }

  public Optional<List<String>> getFullyQualifiedParameterNames() {
    return fullyQualifiedParameterNames;
  }

  public Optional<MethodMatcher> getParentContextMatcher() {
    return parentContextMatcher;
  }


  @Override
  public String toString() {
    return "MethodMatcher [methodName=" + methodNamePredicate +
        ", fullyQualifiedDeclaringType=" + fullyQualifiedDeclaringTypePredicate +
        ", fullyQualifiedParameterNames=" + fullyQualifiedParameterNames +
        ", varArgs=" + isVarargs +
        ", parentContext=" + parentContextMatcher +
        ", requiredAnnotations=" + requiredAnnotations +
        "]";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + fullyQualifiedDeclaringTypePredicate.hashCode();
    result = prime * result + fullyQualifiedParameterNames.hashCode();
    result = prime * result + isVarargs.hashCode();
    result = prime * result + methodNamePredicate.hashCode();
    result = prime * result + parentContextMatcher.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    MethodMatcher other = (MethodMatcher) obj;
    if (!fullyQualifiedDeclaringTypePredicate.get().equals(other.fullyQualifiedDeclaringTypePredicate.get())) {
      return false;
    }
    if (!fullyQualifiedParameterNames.equals(other.fullyQualifiedParameterNames)) {
      return false;
    }
    if (!isVarargs.equals(other.isVarargs)) {
      return false;
    }
    if (!methodNamePredicate.equals(other.methodNamePredicate)) {
      return false;
    }
    return parentContextMatcher.equals(other.parentContextMatcher);
  }
}
