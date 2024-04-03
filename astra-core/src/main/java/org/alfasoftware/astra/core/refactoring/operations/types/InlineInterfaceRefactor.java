package org.alfasoftware.astra.core.refactoring.operations.types;

import static org.alfasoftware.astra.core.utils.AstraUtils.addImport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.alfasoftware.astra.core.utils.ASTOperation;
import org.alfasoftware.astra.core.utils.AstraUtils;
import org.alfasoftware.astra.core.utils.ClassVisitor;
import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;

/**
 * Refactoring operation to inline methods from one interface onto another.
 *
 * This means that where interface A extends interface B,
 * the methods declared on interface B are now declared on A, and A no longer extends B.
 */
public class InlineInterfaceRefactor implements ASTOperation {

  private static final Logger log = Logger.getLogger(InlineInterfaceRefactor.class);

  private final String interfaceName;
  private ClassVisitor interfaceVisitor;

  public InlineInterfaceRefactor(String interfaceName, String interfacePath) {
    this.interfaceName = interfaceName;
    try {
      Path filePath = Paths.get(interfacePath);
      final CompilationUnit compilationUnit = AstraUtils.readAsCompilationUnit(
          filePath.toFile(),
          new String(Files.readAllBytes(filePath)),
          new String[] {""}, new String[] {System.getProperty("java.home") + "/lib/rt.jar"});
      interfaceVisitor = new ClassVisitor();
      compilationUnit.accept(interfaceVisitor);

    } catch (IOException e) {
      log.error(e);
    }
  }

  @Override
  public void run(CompilationUnit compilationUnit, ASTNode node, ASTRewrite rewriter)
      throws IOException, MalformedTreeException, BadLocationException {

    if (!(node instanceof TypeDeclaration)) {
      return;
    }

    TypeDeclaration typeDeclaration = (TypeDeclaration) node;
    Type interfaceType = findMatch(typeDeclaration);
    if (interfaceType == null) {
      return;
    }

    SimpleType parameterType = extractParameterizedType(interfaceType);

    TypeParameter interfaceParamType = interfaceVisitor.getTypeParameters().isEmpty()
      ? null
      : interfaceVisitor.getTypeParameters().get(0);

    // filter methods to add
    @SuppressWarnings("unused")
    List<MethodDeclaration> methodsToAdd = interfaceVisitor.getMethodDeclarations();
    MethodDeclaration[] existingMethods = typeDeclaration.getMethods();
    Map<AbstractMap.SimpleEntry<String, List<String>>, MethodDeclaration> existingMethodNamesToArgumentTypes = new HashMap<>();
    for (MethodDeclaration existingMethod : existingMethods) {
      List<?> parameters = existingMethod.parameters();
      List<String> existingMethodParameters = Stream.ofNullable(parameters)
        .flatMap(Collection::stream)
        .filter(SingleVariableDeclaration.class::isInstance)
        .map(SingleVariableDeclaration.class::cast)
        .map(SingleVariableDeclaration::getType)
        .map(Type::toString)
        .collect(Collectors.toList());
      existingMethodNamesToArgumentTypes.put(
          new AbstractMap.SimpleEntry<>(existingMethod.getName().toString(), existingMethodParameters),
          existingMethod);
    }


    final ListRewrite bodyRewriter = rewriter.getListRewrite(typeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
    for (MethodDeclaration method : interfaceVisitor.getMethodDeclarations()) {
      List<?> superTypeParameters = method.parameters();
      List<String>  superTypeParamTypes = Stream.ofNullable(superTypeParameters)
        .flatMap(Collection::stream)
        .filter(SingleVariableDeclaration.class::isInstance)
        .map(SingleVariableDeclaration.class::cast)
        .map(SingleVariableDeclaration::getType)
        .filter(type -> interfaceParamType != null && type.toString().equals(interfaceParamType.toString()))
        .map(type -> type = parameterType)
        .filter(Objects::nonNull)
        .map(Objects::toString)
        .collect(Collectors.toList());
      @SuppressWarnings("rawtypes")
      AbstractMap.SimpleEntry<String, List> superTypePair = new AbstractMap.SimpleEntry<>(method.getName().toString(), superTypeParamTypes);
      MethodDeclaration matchingExistingMethod = existingMethodNamesToArgumentTypes.get(superTypePair);
      if (matchingExistingMethod != null) {
        List<?> modifiers = matchingExistingMethod.modifiers();
        Stream.ofNullable(modifiers)
          .flatMap(Collection::stream)
          .filter(MarkerAnnotation.class::isInstance)
          .map(MarkerAnnotation.class::cast)
          .filter(markerAnnotation -> Override.class.getSimpleName().equals(markerAnnotation.getTypeName().toString()))
          .forEach(markerAnnotation -> {
              final ListRewrite modifiersList = rewriter.getListRewrite(matchingExistingMethod, MethodDeclaration.MODIFIERS2_PROPERTY);
              modifiersList.remove(markerAnnotation, null);
          });
        continue;
      }


      MethodDeclaration newMethodDeclaration = (MethodDeclaration) MethodDeclaration.copySubtree(typeDeclaration.getAST(), method);
      bodyRewriter.insertLast(newMethodDeclaration, null);
      rewriteGenericType(newMethodDeclaration, interfaceParamType, parameterType, rewriter);
    }

    // Remove the interface extension
    final ListRewrite interfaceList = rewriter.getListRewrite(typeDeclaration, TypeDeclaration.SUPER_INTERFACE_TYPES_PROPERTY);
    interfaceList.remove(interfaceType, null);

    removeImportForInterface(compilationUnit, rewriter);

    // Add the other imports
    Stream.ofNullable(interfaceVisitor.getImports())
      .flatMap(Collection::stream)
      .forEach(imp -> addImport(compilationUnit, imp.getName().getFullyQualifiedName(), rewriter));


    log.info("Inlining interface [" + interfaceName + "] onto [" + typeDeclaration.getName().toString() + "]");
  }


  private SimpleType extractParameterizedType(Type interfaceType) {
    if (interfaceType instanceof ParameterizedType) {
      ParameterizedType parameterizedInterfaceType = (ParameterizedType) interfaceType;
      @SuppressWarnings("rawtypes")
      List typeArguments = parameterizedInterfaceType.typeArguments();
      if (! typeArguments.isEmpty()) {
        Object object = typeArguments.get(0);
        if (object instanceof SimpleType) {
          return (SimpleType) object;
        }
      }
    }
    return null;
  }


  private Type findMatch(TypeDeclaration typeDeclaration) {
    if (typeDeclaration.isInterface() &&
        ! typeDeclaration.resolveBinding().isNested() &&
        ! typeDeclaration.superInterfaceTypes().isEmpty()) {
      @SuppressWarnings("rawtypes")
      List superInterfaceTypes = typeDeclaration.superInterfaceTypes();
      for (Object superInterfaceType : superInterfaceTypes) {
        Type interfaceType = (Type) superInterfaceType;
        ITypeBinding interfaceBinding = interfaceType.resolveBinding();
        if (interfaceBinding != null) {
          String binaryName = interfaceBinding.getBinaryName();
          if (binaryName != null && binaryName.equals(interfaceName)) {
            return interfaceType;
          }
        }
      }
    }
    return null;
  }

  private void rewriteGenericType(MethodDeclaration newMethodDeclaration,
      TypeParameter interfaceParamType,
      SimpleType parameterType,
      ASTRewrite rewriter) {
      if (parameterType != null && interfaceParamType != null) {
        ClassVisitor methodVisitor = new ClassVisitor();
        newMethodDeclaration.accept(methodVisitor);
        Stream.ofNullable(methodVisitor.getSimpleTypes())
          .flatMap(Collection::stream)
          .filter(type -> type.getName().toString().equals(interfaceParamType.getName().toString()))
          .forEach(type -> rewriter.set(type.getName(), SimpleName.IDENTIFIER_PROPERTY, parameterType.getName(), null));
      }
  }

  private void removeImportForInterface(CompilationUnit compilationUnit, ASTRewrite rewriter) {
    final ListRewrite importList = rewriter.getListRewrite(compilationUnit, CompilationUnit.IMPORTS_PROPERTY);
    @SuppressWarnings("unchecked")
    List<ImportDeclaration> currentList = importList.getRewrittenList();
    Stream.ofNullable(currentList)
      .flatMap(Collection::stream)
      .filter(existingImport -> existingImport.getName().getFullyQualifiedName().equals(interfaceName))
      .forEach(existingImport -> importList.remove(existingImport, null));
  }
}
