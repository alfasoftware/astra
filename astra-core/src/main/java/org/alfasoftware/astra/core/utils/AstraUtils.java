package org.alfasoftware.astra.core.utils;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.ChildPropertyDescriptor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextElement;
import org.eclipse.jdt.core.dom.ThisExpression;
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

  public static CompilationUnit readAsCompilationUnit(String fileSource, String[] sources, String[] classPath) {
    ASTParser parser = createParser(fileSource, sources, classPath);

    CompilationUnit compilationUnit = (CompilationUnit) parser.createAST(null);
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
  public static String getFullyQualifiedName(final TypeDeclaration type) {
    StringBuilder fullName = new StringBuilder(type.getName().toString());
    ASTNode parent = type.getParent();
    while (true) {
      if (parent instanceof CompilationUnit) {
        fullName.insert(0, ((CompilationUnit) parent).getPackage().getName().toString() + ".");
        break;
      } else if (parent instanceof TypeDeclaration) {
        fullName.insert(0, ((TypeDeclaration) parent).getName().toString() + ".");
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


  public static String getFullyQualifiedName(ClassInstanceCreation cic) {
    return Optional.of(cic)
        .map(ClassInstanceCreation::resolveConstructorBinding)
        .map(IMethodBinding::getDeclaringClass)
        .map(ITypeBinding::getQualifiedName)
        .orElse("");
  }

  private static final Set<Character> finalTypeChars = new HashSet<>(Arrays.asList('.', '$'));


  /**
   * Consider using {@link Signature#getSimpleName(String)}
   */
  public static String getSimpleName(String fullName) {
    // Strip package and enclosing class name, if present
    for (int i = fullName.length() - 1; i >= 0; i--) {
      if (finalTypeChars.contains(fullName.charAt(i))) {
        return fullName.substring(i + 1);
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
   * @return true if the import is for an inner type e.g. import com.Foo.Bar
   */
  public static boolean isImportOfInnerType(ImportDeclaration importDeclaration) {
    return !importDeclaration.isOnDemand() && importDeclaration.getName().toString().split("\\.[A-Z]").length > 2;
  }


  public static ITypeBinding resolveGenericTypeArgumentsForSimpleName(SimpleName variableName) {
    IBinding resolveBinding = variableName.resolveBinding();
    if (resolveBinding instanceof IVariableBinding) {
      IVariableBinding variableBinding = (IVariableBinding) resolveBinding;
      ITypeBinding type = variableBinding.getType();
      if (type != null) {
        ITypeBinding[] typeArguments = type.getTypeArguments();
        // If it's got type arguments
        if (typeArguments != null && typeArguments.length == 1) {
          return typeArguments[0];
        }
      }
    }
    return null;
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


  public static boolean isTypeBindingQualifiedNameEqual(ITypeBinding type, String typeToMatch) {
    return Optional.ofNullable(type)
        .map(ITypeBinding::getQualifiedName)
        .filter(n -> n.equals(typeToMatch))
        .isPresent();
  }


  public static boolean isMethodInvocationStatic(MethodInvocation methodInvocation) {
    return Optional.ofNullable(methodInvocation.resolveMethodBinding())
            .filter(mb -> Modifier.isStatic(mb.getModifiers()))
            .isPresent();
  }
  

  public enum MethodInvocationType {
    /*
     * The method is statically imported, so the name alone is used e.g.
     * import static com.package.DeclaringType.methodName;
     * methodName(); <<<<
     */
    STATIC_METHOD_METHOD_NAME_ONLY,

    /*
     * The simple class is used inline e.g.
     * import com.package.DeclaringType;
     * DeclaringType.methodName(); <<<<
     */
    STATIC_METHOD_SIMPLE_NAME,

    /*
     * The fully qualified name is used inline e.g.
     * com.package.DeclaringType.methodName(); <<<<
     */
    STATIC_METHOD_FULLY_QUALIFIED_NAME,

    /*
     * The method is invoked on a variable of the given type e.g.
     * import com.package.DeclaringType;
     * DeclaringType a = new DeclaringType();
     * a.methodName(); <<<<
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
    throw new IllegalStateException("Unknown scenario for method invocation: " + methodInvocation.toString());
  }


  public static boolean isStaticallyImportedMethod(MethodInvocation methodInvocation, CompilationUnit compilationUnit,
      String fullyQualifiedDeclaringType, String methodName) {

    if (! methodInvocation.getName().toString().equals(methodName)) {
      return false;
    }

    String expressionBindingName = "";
    String typeBindingName = "";
    String typeQualifiedName = "";
    Expression expression = methodInvocation.getExpression();
    if (expression != null && expression instanceof Name) {
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
    if (expressionBindingName.isEmpty() &&
        typeBindingName.isEmpty() &&
        typeQualifiedName.isEmpty()) {
      String nameForImport = String.join(".", fullyQualifiedDeclaringType, methodName);
      for (Object item : compilationUnit.imports()) {
        if (item instanceof ImportDeclaration) {
          ImportDeclaration importDeclaration = (ImportDeclaration) item;
          if (importDeclaration.isOnDemand() &&
              importDeclaration.getName().toString().equals(fullyQualifiedDeclaringType)) {
            return true;
          }
          if (importDeclaration.getName().toString().equals(nameForImport)) {
            return true;
          }
        }
      }
    }
    return false;
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
   * Adds {@code return this;} to the end of the method body declaration to turn
   * it into a builder.
   *
   * @param method Method declaration to modify the body for.
   * @param rewriter AST re-writer to apply changes.
   */
  public static void addReturnThisStatement(MethodDeclaration method, ASTRewrite rewriter) {
    ReturnStatement returnStatement = method.getBody().getAST().newReturnStatement();
    ThisExpression thisExpression = returnStatement.getAST().newThisExpression();
    returnStatement.setExpression(thisExpression);
    ListRewrite statementsList = rewriter.getListRewrite(method.getBody(), Block.STATEMENTS_PROPERTY);
    statementsList.insertLast(returnStatement, null);
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