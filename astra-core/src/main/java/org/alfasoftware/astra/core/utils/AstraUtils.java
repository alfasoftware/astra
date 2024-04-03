package org.alfasoftware.astra.core.utils;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.alfasoftware.astra.core.matchers.AnnotationMatcher;
import org.apache.log4j.Logger;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.ChildPropertyDescriptor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextElement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jface.text.BadLocationException;

/**
 * Utility functions for working with ASTs including creation of ASTs from source files, writing changes back to the source file,
 * reading information from existing ASTNodes, and generating new ones.
 */
public class AstraUtils {

  private static final Logger log = Logger.getLogger(AstraUtils.class);
  public static final String CLASSPATHS_MISSING_WARNING = "This may be a sign that classpaths for the operation need to be supplied. ";

  public static CompilationUnit readAsCompilationUnit(File file, String fileSource, String[] sources, String[] classPath) {
    ASTParser parser = createParser(fileSource, sources, classPath);

    CompilationUnit compilationUnit = (CompilationUnit) parser.createAST(null);
    compilationUnit.setProperty(CompilationUnitProperty.ABSOLUTE_PATH, file.getAbsolutePath());
    compilationUnit.recordModifications();
    return compilationUnit;
  }

  private static final String JAVA_VERSION = JavaCore.VERSION_1_8;


  public static ASTParser createParser(String fileSource, String[] sources, String[] classPath) {
    @SuppressWarnings("deprecation") // This is just saying "use a newer Java version"
    ASTParser parser = ASTParser.newParser(AST.JLS8);
    parser.setResolveBindings(true);
    parser.setBindingsRecovery(true);
    parser.setStatementsRecovery(true);
    parser.setKind(ASTParser.K_COMPILATION_UNIT);
    parser.setSource(fileSource.toCharArray());
    parser.setUnitName("");
    HashMap<String, String> javaCoreOptions = new HashMap<>(JavaCore.getOptions());
    JavaCore.setComplianceOptions(JAVA_VERSION, javaCoreOptions);

    parser.setCompilerOptions(javaCoreOptions);

    final String[] encodings = new String[sources.length];
    Arrays.fill(encodings, "UTF-8");

    parser.setEnvironment(classPath, sources, encodings, true);
    return parser;
  }


  /**
   * Apply the recorded changes from the ASTRewrite to the source file, and return the result.
   *
   * @param source Java source document.
   * @param rewriter AST re-writer which contains the modifications to apply to the document.
   */
  public static String makeChangesFromAST(String source, ASTRewrite rewriter) throws BadLocationException {
    Map<String, String> formattingOptions = new HashMap<>();
    formattingOptions.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_FIELD, JavaCore.INSERT);
    formattingOptions.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_LOCAL_VARIABLE, JavaCore.INSERT);
    formattingOptions.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_METHOD, JavaCore.INSERT);
    formattingOptions.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_PARAMETER, JavaCore.INSERT);
    formattingOptions.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_TYPE, JavaCore.INSERT);
    formattingOptions.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
    formattingOptions.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "2");
    formattingOptions.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_NOT_OPERATOR, JavaCore.INSERT);
    JavaCore.setComplianceOptions(JAVA_VERSION, formattingOptions);
    org.eclipse.jface.text.Document document = new org.eclipse.jface.text.Document(source);
    rewriter.rewriteAST(document, formattingOptions).apply(document);
    return document.get();
  }


  /**
   * Build the fully qualified name for a type.
   *
   * @param type The type for which to build a class name.
   * @return fully qualified class name.
   */
  public static String getFullyQualifiedName(final AbstractTypeDeclaration type) {
    StringBuilder fullName = new StringBuilder(type.getName().toString());
    ASTNode parent = type.getParent();
    while (true) {
      if (parent instanceof CompilationUnit) {
        fullName.insert(0, ((CompilationUnit) parent).getPackage().getName().toString() + ".");
        break;
      } else if (parent instanceof AbstractTypeDeclaration) {
        fullName.insert(0, ((AbstractTypeDeclaration) parent).getName().toString() + ".");
        parent = parent.getParent();
      }
    }
    return fullName.toString();
  }


  public static String getFullyQualifiedName(MethodInvocation mi, CompilationUnit compilationUnit) {
    Optional<ITypeBinding> typeBinding = Optional.of(mi)
        .map(MethodInvocation::getExpression)
        .map(Expression::resolveTypeBinding);

    if (typeBinding
        .filter(ITypeBinding::isRecovered)
        .isPresent()) {
      log.error("Binding not found for type of method invocation. "
          + CLASSPATHS_MISSING_WARNING
          + "Method invocation: [" + mi + "]");
      return "";
    }

    Optional<String> qualifiedNameNonStatic = typeBinding.map(ITypeBinding::getQualifiedName);
    if (qualifiedNameNonStatic.isPresent()) {
      return qualifiedNameNonStatic.get();
    }

    if (isMethodInvocationStatic(mi)) {
      @SuppressWarnings("unchecked")
      List<ImportDeclaration> imports = compilationUnit.imports();

      Set<String> matches = imports.stream()
        .filter(ImportDeclaration::isStatic)
        .map(ImportDeclaration::getName)
        .map(Object::toString)
        .filter(importName -> importName.substring(importName.lastIndexOf('.') + 1).equals(mi.getName().toString()))
        .map(importName -> importName.substring(0, importName.lastIndexOf('.')))
        .collect(Collectors.toSet());
      if (matches.size() == 1) {
        return matches.iterator().next();
      }

      Set<String> onDemandMatches = getStaticAndOnDemandImportMatchesForMethodInvocation(imports, mi);
      if (onDemandMatches.size() == 1) {
        return onDemandMatches.iterator().next();
      }
    }

    Optional<String> resolvedMethodBindingName = Optional.of(mi)
      .map(MethodInvocation::resolveMethodBinding)
      .map(IMethodBinding::getDeclaringClass)
      .map(ITypeBinding::getQualifiedName);
    if (resolvedMethodBindingName.isPresent()) {
      return resolvedMethodBindingName.get();
    }

    return "";
  }


  private static Set<String> getStaticAndOnDemandImportMatchesForMethodInvocation(List<ImportDeclaration> imports, MethodInvocation mi) {
    Set<String> onDemandMatches = new HashSet<>();
    imports.stream()
      .filter(importCandidate -> importCandidate.isStatic() && importCandidate.isOnDemand())
      .forEach(importCandidate -> {
        IBinding binding = importCandidate.resolveBinding();
        if (binding instanceof ITypeBinding) {
          ITypeBinding iTypeBinding = (ITypeBinding) binding;
          Arrays.stream(iTypeBinding.getDeclaredMethods())
            .filter(methodBinding -> methodBinding.getName().equals(mi.getName().toString()))
            .map(methodBinding -> importCandidate.getName().toString())
            .forEach(onDemandMatches::add);
          if (iTypeBinding.getSuperclass() != null) {
            Arrays.stream(iTypeBinding.getSuperclass().getDeclaredMethods())
              .filter(methodBinding -> methodBinding.getName().equals(mi.getName().toString()))
              .map(methodBinding -> importCandidate.getName().toString())
              .forEach(onDemandMatches::add);
          }
        }
      });
    return onDemandMatches;
  }


  public static String getFullyQualifiedName(IMethodBinding methodBinding) {
    return Optional.of(methodBinding)
        .map(IMethodBinding::getDeclaringClass)
        .map(ITypeBinding::getQualifiedName)
        .orElse("");
  }


  public static String getFullyQualifiedName(Type type) {
    Optional<ITypeBinding> typeBinding = Optional.ofNullable(type)
        .map(Type::resolveBinding);

    if (typeBinding
        .filter(ITypeBinding::isRecovered)
        .isPresent()) {
      log.error("Binding not found for type. "
          + CLASSPATHS_MISSING_WARNING
          + "Type: [" + type + "]");
      return "";
    } else {
      return typeBinding.map(ITypeBinding::getQualifiedName)
          .orElse("");
    }
  }


  public static String getName(ITypeBinding typeBinding) {
    if (typeBinding.isArray() && (typeBinding.getElementType().isTypeVariable() || typeBinding.getElementType().isParameterizedType())) {
      return typeBinding.getErasure().getQualifiedName();
    } else if (typeBinding.isTypeVariable()) {
      return typeBinding.getErasure().getBinaryName();
    } else if (typeBinding.isPrimitive() || typeBinding.isArray()) {
      return typeBinding.getQualifiedName();
    } else {
      return typeBinding.getBinaryName();
    }
  }


  public static String getName(AnonymousClassDeclaration anonymousClassDeclaration) {
    ITypeBinding resolveTypeBinding = anonymousClassDeclaration.resolveBinding();
    if (resolveTypeBinding != null &&
        resolveTypeBinding.isLocal()) {

      // Superclass
      String superclassName = AstraUtils.getName(resolveTypeBinding.getSuperclass());
      if (! Object.class.getName().equals(superclassName)) {
        return superclassName;
      }

      // Interface
      if (anonymousClassDeclaration.getParent() instanceof ClassInstanceCreation &&
          ((Expression) anonymousClassDeclaration.getParent()).resolveTypeBinding() != null) {
        ClassInstanceCreation parent = (ClassInstanceCreation) anonymousClassDeclaration.getParent();
        return Arrays.stream(parent.resolveTypeBinding().getInterfaces())
                .filter(i -> parent.toString().contains("new " + AstraUtils.getSimpleName(AstraUtils.getName(i))))
                .map(AstraUtils::getName)
                .findFirst()
                .orElse("");
      }
    }
    return "";
  }


  public static String getFullyQualifiedName(ClassInstanceCreation cic) {
    return Optional.of(cic)
        .map(ClassInstanceCreation::resolveConstructorBinding)
        .map(IMethodBinding::getDeclaringClass)
        .map(ITypeBinding::getQualifiedName)
        .orElse("");
  }

  private static final Set<Character> finalTypeChars = new HashSet<>(Arrays.asList('.', '$'));


  /**
   * Returns the Java identifier representing the simple name from a String representation of a qualified name.
   * The JLS section 3.8 ({@link https://docs.oracle.com/javase/specs/jls/se11/html/jls-3.html#jls-3.8}) defines an identifier as
   * "an unlimited-length sequence of Java letters and Java digits, the first of which must be a Java letter."
   * "The 'Java letters' include uppercase and lowercase ASCII Latin letters A-Z (\u0041-\u005a), and a-z (\u0061-\u007a), and,
   *    for historical reasons, the ASCII dollar sign ($, or \u0024) and underscore (_, or \u005f).
   *    The dollar sign should be used only in mechanically generated source code or, rarely, to access pre-existing names on
   *    legacy systems. The underscore may be used in identifiers formed of two or more characters, but it cannot be used as a
   *     one-character identifier due to being a keyword.
   *  The 'Java digits' include the ASCII digits 0-9 (\u0030-\u0039)."
   */
  public static String getSimpleName(String fullName) {
    // Strip package and enclosing class name, if present
    for (int i = fullName.length() - 1; i >= 0; i--) {
      if (finalTypeChars.contains(fullName.charAt(i))) {
        String name = fullName.substring(i + 1);
        if (name.isBlank() && '$' == fullName.charAt(i)) {
          return "$";
        }
        return name;
      }
    }
    return fullName;
  }


  /**
   * Find the package name of a type.
   *
   * @param type The type for which to find the package name.
   * @return The given type package name.
   */
  public static PackageDeclaration getPackageName(final TypeDeclaration type) {
    ASTNode parent = type.getParent();
    while (true) {
      if (parent instanceof CompilationUnit) {
        return ((CompilationUnit) parent).getPackage();
      }
      parent = parent.getParent();
    }
  }


  /**
   * Consider using {@link Signature#getSignatureQualifier(String)}
   */
  public static String getPackageName(String fullyQualifiedName) {
    return fullyQualifiedName.split("\\.[A-Z]")[0];
  }


  /**
   * Returns the qualifier for a fully qualified name.
   * This is not necessarily just a package name - for inner types,
   * this will be the fully qualified name of the outer type.
   *
   * <pre>
   * For input: com.package.Foo
   * Returns output: com.package
   *
   * For input: com.package.Foo.InnerFoo
   * Returns output: com.package.Foo
   * </pre>
   */
  public static String getQualifier(String fullyQualifiedName) {
    return fullyQualifiedName.substring(0, fullyQualifiedName.lastIndexOf("."));
  }


  /**
   * @return true if the import is for an inner type e.g. import com.Foo.Bar
   */
  public static boolean isImportOfInnerType(ImportDeclaration importDeclaration) {
    return !importDeclaration.isOnDemand() && importDeclaration.getName().toString().split("\\.[A-Z]").length > 2;
  }


  /**
   * Returns the fully qualified name of the first type declared in the
   * compilation unit.
   *
   * @param compilationUnit
   * @return
   */
  public static String getNameForCompilationUnit(CompilationUnit compilationUnit) {
    if (! compilationUnit.types().isEmpty()) {
      AbstractTypeDeclaration type = (AbstractTypeDeclaration) compilationUnit.types().get(0);
      if (compilationUnit.getPackage() != null) {
        return compilationUnit.getPackage().getName().toString() + "." + type.getName().toString();
      }
    }

    return "";
  }


  public static MarkerAnnotation buildMarkerAnnotation(ASTNode nodeToAnnotate, String annotationClassName) {
    // Create the annotation
    MarkerAnnotation markerAnnotation = nodeToAnnotate.getAST().newMarkerAnnotation();
    markerAnnotation.setTypeName(nodeToAnnotate.getAST().newName(annotationClassName));
    return markerAnnotation;
  }

  /**
   * Adds an annotation to a node.
   *
   * @param nodeToAnnotate Node to which the annotation should be applied.
   * @param annotation Annotation to be applied.
   * @param rewriter AST re-writer to apply changes.
   * @param modifiersProperty Access to the modifiers list property of the
   *          entity to annotate.
   */
  public static void addAnnotationToNode(ASTNode nodeToAnnotate, Annotation annotation, ASTRewrite rewriter, ChildListPropertyDescriptor modifiersProperty) {
    // Add the annotation to the parent element
    final ListRewrite modifiersList = rewriter.getListRewrite(nodeToAnnotate, modifiersProperty);
    modifiersList.insertFirst(annotation, null);
  }


  /**
   * Builds an annotation with a single 'value' content.
   *
   * @param nodeToAnnotate Node to which the annotation should be applied.
   * @param annotationClassName Simple name of the annotation to apply (must be
   *          separately imported)
   * @param annotationContent Content of the annotation value.
   * @param rewriter AST re-writer to apply changes.
   */
  public static SingleMemberAnnotation buildSingleMemberAnnotation(ASTNode nodeToAnnotate, String annotationClassName, String annotationContent, ASTRewrite rewriter) {
    // Create the annotation
    SingleMemberAnnotation singleMemberAnnotation = nodeToAnnotate.getAST().newSingleMemberAnnotation();
    singleMemberAnnotation.setTypeName(nodeToAnnotate.getAST().newName(annotationClassName));

    // Define the single value contents
    StringLiteral documentationContents = nodeToAnnotate.getAST().newStringLiteral();
    rewriter.set(documentationContents, StringLiteral.ESCAPED_VALUE_PROPERTY, annotationContent, null);
    singleMemberAnnotation.setValue(documentationContents);
    return singleMemberAnnotation;
  }


  /**
   * @return true if the body declaration has an annotation with a fully qualified name matching the argument.
   */
  public static boolean isAnnotatedWith(BodyDeclaration bodyDeclaration, String fqAnnotationToMatch) {
    AnnotationMatcher annotationMatcher = AnnotationMatcher.builder()
        .withFullyQualifiedName(fqAnnotationToMatch)
        .build();

    for (Object modifier : bodyDeclaration.modifiers()) {
      if (modifier instanceof Annotation) {
        Annotation annotation = (Annotation) modifier;
        if (annotationMatcher.matches(annotation)) {
          return true;
        }
      }
    }
    return false;
  }


  /**
   * Adds a new import to the compilation unit.
   *
   * @param compilationUnit Compilation unit to add the import to.
   * @param importPath Fully qualified path of the import to add.
   * @param rewriter AST re-writer to apply changes.
   */
  public static void addImport(CompilationUnit compilationUnit, String importPath, ASTRewrite rewriter) {

    // If the type to import is a parameterized type, only import the base type
    if (importPath.contains("<")) {
      importPath = importPath.substring(0, importPath.indexOf('<'));
    }

    addImport(compilationUnit, importPath, rewriter, false);
  }


  /**
   * Adds a new static import to the compilation unit.
   *
   * @param compilationUnit Compilation unit to add the import to.
   * @param importPath Fully qualified path of the import to add.
   * @param rewriter AST re-writer to apply changes.
   */
  public static void addStaticImport(CompilationUnit compilationUnit, String importPath, ASTRewrite rewriter) {
    addImport(compilationUnit, importPath, rewriter, true);
  }


  private static void addImport(CompilationUnit compilationUnit, String importPath, ASTRewrite rewriter, boolean isStatic) {

    // Don't add import if the supplied import path is blank
    if (importPath.trim().isEmpty()) {
      // TODO Should this throw an IllegalArgumentException?
      // We'd likely get here due to missing classpaths
      return;
    }

    final ListRewrite importList = rewriter.getListRewrite(compilationUnit, CompilationUnit.IMPORTS_PROPERTY);
    @SuppressWarnings("unchecked")
    List<ImportDeclaration> currentList = importList.getRewrittenList();

    // Don't add import if that exact import already exists
    if (currentList.stream().anyMatch(existingImport -> importPath.equals(existingImport.getName().getFullyQualifiedName()))) {
      return;
    }

    // Don't add an import if it would clash with an existing type -
    // instead, update that import
    Optional<ImportDeclaration> existingImportWithSameName = currentList.stream()
        .filter(i -> getSimpleName(importPath).equals(getSimpleName(i.getName().getFullyQualifiedName())))
        .findFirst();
    if (existingImportWithSameName.isPresent() && ! existingImportWithSameName.get().isStatic()) {
      updateImport(compilationUnit, existingImportWithSameName.get().getName().getFullyQualifiedName(), importPath, rewriter);
      return;
    }

    // Otherwise add the import
    final ImportDeclaration importDeclaration = compilationUnit.getAST().newImportDeclaration();
    importDeclaration.setName(compilationUnit.getAST().newName(importPath));
    importDeclaration.setStatic(isStatic);
    if (isStatic) {
      importList.insertFirst(importDeclaration, null);
    } else {
      int index = 0;
      for (ImportDeclaration existingImport : currentList) {
        // Add imports alphabetically
        if (existingImport.isStatic() ||
            AstraUtils.getPackageName(importPath)
              .compareTo(AstraUtils.getPackageName(existingImport.getName().toString())) < 0 ||
            importPath.compareTo(existingImport.getName().toString()) > 0) {
          index++;
        }
      }
      importList.insertAt(importDeclaration, index, null);
    }
  }


  public static void updateImport(CompilationUnit compilationUnit, String importBefore, String importAfter, ASTRewrite rewriter) {
    PackageDeclaration packageDeclaration = compilationUnit.getPackage();
    if (packageDeclaration.getName().toString().equals(AstraUtils.getPackageName(importAfter))) {
      return;
    }

    if (importAfter.startsWith("java.lang")) {
      return;
    }

    final ListRewrite importList = rewriter.getListRewrite(compilationUnit, CompilationUnit.IMPORTS_PROPERTY);
    @SuppressWarnings("unchecked")
    List<ImportDeclaration> currentList = importList.getRewrittenList();

    // Don't add import if that exact import already exists
    if (currentList.stream().anyMatch(existingImport -> importAfter.equals(existingImport.getName().getFullyQualifiedName()))) {
      return;
    }

    // find the existing entry, and update it
    for (ImportDeclaration importDeclaration : currentList) {
      if (importDeclaration.getName().toString().equals(importBefore)) {
        Name newName = compilationUnit.getAST().newName(importAfter);
        rewriter.set(importDeclaration, ImportDeclaration.NAME_PROPERTY, newName, null);
      }
    }
  }


  public static void removeImport(CompilationUnit compilationUnit, String importBefore, ASTRewrite rewriter) {
    // does import already
    for (Object item : compilationUnit.imports()) {
      if (item instanceof ImportDeclaration) {
        ImportDeclaration importDeclaration = (ImportDeclaration) item;
        if (importDeclaration.getName().toString().equals(importBefore)) {
          rewriter.remove(importDeclaration, null);
        }
      }
    }
  }


  public static void removeImport(ImportDeclaration importDeclaration,
      ASTRewrite rewriter) {
    // does import already
    rewriter.remove(importDeclaration, null);
  }


  public static boolean isMethodInvocationStatic(MethodInvocation methodInvocation) {
    return Optional.ofNullable(methodInvocation.resolveMethodBinding())
            .filter(mb -> Modifier.isStatic(mb.getModifiers()))
            .isPresent();
  }


  public enum MethodInvocationType {
    /*
     * The method is statically imported, so the name alone is used e.g.
     *
     *   import static com.package.DeclaringType.methodName;     //NOSONAR
     *   methodName(); <<<<
     */
    STATIC_METHOD_METHOD_NAME_ONLY,

    /*
     * The simple class is used inline e.g.
     *
     *   import com.package.DeclaringType;                       //NOSONAR
     *   DeclaringType.methodName(); <<<<
     */
    STATIC_METHOD_SIMPLE_NAME,

    /*
     * The fully qualified name is used inline e.g.
     *
     *   com.package.DeclaringType.methodName(); <<<<            //NOSONAR
     */
    STATIC_METHOD_FULLY_QUALIFIED_NAME,

    /*
     * The method is invoked on a variable of the given type e.g.
     *
     *   import com.package.DeclaringType;                       //NOSONAR
     *   DeclaringType a = new DeclaringType();                  //NOSONAR
     *   a.methodName(); <<<<
     */
    ON_CLASS_INSTANCE,
  }


  public static MethodInvocationType getMethodInvocationType(MethodInvocation methodInvocation, CompilationUnit compilationUnit,
      String fullyQualifiedDeclaringType, String methodName) {
    if (methodInvocation.getExpression() != null) {
      String methodInvocationExpressionString = methodInvocation.getExpression().toString();
      if (methodInvocationExpressionString.equals(AstraUtils.getSimpleName(fullyQualifiedDeclaringType))) {
        return MethodInvocationType.STATIC_METHOD_SIMPLE_NAME;
      } else if (methodInvocationExpressionString.equals(fullyQualifiedDeclaringType)) {
        return MethodInvocationType.STATIC_METHOD_FULLY_QUALIFIED_NAME;
      } else if (methodInvocation.getExpression().resolveTypeBinding().getQualifiedName().equals(fullyQualifiedDeclaringType)) {
        return MethodInvocationType.ON_CLASS_INSTANCE;
      }
    }
    if (isStaticallyImportedMethod(methodInvocation, compilationUnit, fullyQualifiedDeclaringType, methodName)) {
      return MethodInvocationType.STATIC_METHOD_METHOD_NAME_ONLY;
    }
    throw new IllegalStateException("Unknown scenario for method invocation [" + methodInvocation.toString() +
      "] in [" + AstraUtils.getNameForCompilationUnit(compilationUnit) + "]");
  }


  public static boolean isStaticallyImportedMethod(MethodInvocation methodInvocation, CompilationUnit compilationUnit,
                                                   String fullyQualifiedDeclaringType, String methodName) {

    if (!methodInvocation.getName().toString().equals(methodName)) {
      return false;
    }

    Expression expression = methodInvocation.getExpression();
    if (isExpressionNameEmpty(expression)) {
      String nameForImport = String.join(".", fullyQualifiedDeclaringType, methodName);
      for (ImportDeclaration importDeclaration : getImportDeclarations(compilationUnit)) {
        String name = importDeclaration.getName().toString();
        if ((name.equals(fullyQualifiedDeclaringType) || name.equals(nameForImport.substring(0, nameForImport.lastIndexOf(".")))) && importDeclaration.isOnDemand()) {
          return true;
        }
        if (name.equals(nameForImport)) {
          return true;
        }
      }
    }
    return false;
  }


  private static boolean isExpressionNameEmpty(Expression expression) {
    String expressionBindingName = "";
    String typeBindingName = "";
    String typeQualifiedName = "";

    if (expression instanceof Name) {
      IBinding binding = ((Name) expression).resolveBinding();
      if (binding != null) {
        expressionBindingName = binding.getName();
      }

      ITypeBinding typeBinding = expression.resolveTypeBinding();
      if (typeBinding != null) {
        typeBindingName = typeBinding.getName();
        typeQualifiedName = typeBinding.getQualifiedName();
      }
    }

    return expressionBindingName.isEmpty() && typeBindingName.isEmpty() && typeQualifiedName.isEmpty();
  }


  @SuppressWarnings("unchecked")
  public static List<ImportDeclaration> getImportDeclarations(CompilationUnit compilationUnit) {
    return compilationUnit.imports();
  }


  public static Set<AbstractTypeDeclaration> getTypesDeclaredInCompilationUnit(CompilationUnit compilationUnit) {
    ClassVisitor visitor = new ClassVisitor();
    compilationUnit.accept(visitor);
    return new HashSet<>(visitor.getAbstractTypeDeclarations());
  }


  /**
   * Replaces the Javadoc defined for the node with a new and empty Javadoc
   * block.
   *
   * @param parentNode Parent entity to replace the Javadoc for.
   * @param javadocProperty Path to the Javadoc property for this entity.
   * @param rewriter AST re-writer to apply changes.
   * @return New Javadoc definition.
   */
  public static Javadoc replaceJavadoc(ASTNode parentNode, ChildPropertyDescriptor javadocProperty, ASTRewrite rewriter) {
    final Javadoc newJavadoc = parentNode.getAST().newJavadoc();
    rewriter.set(parentNode, javadocProperty, newJavadoc, null);
    return newJavadoc;
  }


  /**
   * Adds a new tag to the end of a Javadoc block.
   *
   * @param javadocNode Javadoc definition the tag should be added to.
   * @param tagName Name of the tag to add, can be null if this is an initial
   *          paragraph without a tag.
   * @param tagContent Content of the tag to display.
   * @param rewriter AST re-writer to apply changes.
   */
  public static void addJavadocTag(Javadoc javadocNode, String tagName, String tagContent, ASTRewrite rewriter) {
    final ListRewrite tagList = rewriter.getListRewrite(javadocNode, Javadoc.TAGS_PROPERTY);
    final TagElement tagElement = javadocNode.getAST().newTagElement();
    tagElement.setTagName(tagName);
    tagList.insertLast(tagElement, null);
    final ListRewrite tagFragementList = rewriter.getListRewrite(tagElement, TagElement.FRAGMENTS_PROPERTY);
    TextElement tagTextElement = tagElement.getAST().newTextElement();
    tagTextElement.setText(tagContent);
    tagFragementList.insertFirst(tagTextElement, null);
  }


  /**
   * Adds a new method to an existing type definition.
   *
   * @param typeDeclaration Type declaration to implement the new method on.
   * @param methodName Name of the new method to implement.
   * @param returnType Name of the type to return or null if nothing should be
   *          returned.
   * @param rewriter AST re-writer to apply changes.
   * @param modifiers One or more modifiers to apply to the method definition.
   * @return Newly created method declaration.
   */
  public static MethodDeclaration defineNewMethod(TypeDeclaration typeDeclaration, String methodName, ASTNode returnType,
      ASTRewrite rewriter, ModifierKeyword... modifiers) {
    final ListRewrite bodyRewriter = rewriter.getListRewrite(typeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
    MethodDeclaration newMethod = typeDeclaration.getAST().newMethodDeclaration();
    newMethod.setName(typeDeclaration.getAST().newSimpleName(methodName));
    if (returnType != null) {
      rewriter.set(newMethod, MethodDeclaration.RETURN_TYPE2_PROPERTY, returnType, null);
    }
    ListRewrite setMethodModifiers = rewriter.getListRewrite(newMethod, MethodDeclaration.MODIFIERS2_PROPERTY);
    for (ModifierKeyword modifier : modifiers) {
      setMethodModifiers.insertLast(newMethod.getAST().newModifier(modifier), null);
    }

    bodyRewriter.insertLast(newMethod, null);
    return newMethod;
  }
}