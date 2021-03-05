package org.alfasoftware.astra.core.refactoring.operations.types;

import static org.alfasoftware.astra.core.utils.AstraUtils.addImport;
import static org.alfasoftware.astra.core.utils.AstraUtils.getSimpleName;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfasoftware.astra.core.utils.ASTOperation;
import org.alfasoftware.astra.core.utils.AstraUtils;
import org.alfasoftware.astra.core.utils.ClassVisitor;
import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.PrimitiveType.Code;
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
      final CompilationUnit compilationUnit = AstraUtils.readAsCompilationUnit(
          new String(Files.readAllBytes(Paths.get(interfacePath))),
          new String[] {""}, new String[] {System.getProperty("java.home") + "/lib/rt.jar"});
      interfaceVisitor = new ClassVisitor();
      compilationUnit.accept(interfaceVisitor);

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void run(CompilationUnit compilationUnit, ASTNode node, ASTRewrite rewriter)
      throws IOException, MalformedTreeException, BadLocationException {

    if (node instanceof TypeDeclaration) {
      TypeDeclaration typeDeclaration = (TypeDeclaration) node;
      Type interfaceType = findMatch(typeDeclaration);
      if (interfaceType != null) {

        SimpleType parameterType = null;
        if (interfaceType instanceof ParameterizedType) {
          ParameterizedType parameterizedInterfaceType = (ParameterizedType) interfaceType;
          @SuppressWarnings("rawtypes")
          List typeArguments = parameterizedInterfaceType.typeArguments();
          if (! typeArguments.isEmpty()) {
            Object object = typeArguments.get(0);
            if (object instanceof SimpleType) {
              parameterType = (SimpleType) object;
            }
          }
        }

        TypeParameter interfaceParamType = null;
        if (! interfaceVisitor.getTypeParameters().isEmpty()) {
          interfaceParamType = interfaceVisitor.getTypeParameters().get(0);
        }

        // filter methods to add
        @SuppressWarnings("unused")
        List<MethodDeclaration> methodsToAdd = interfaceVisitor.getMethodDeclarations();
        MethodDeclaration[] existingMethods = typeDeclaration.getMethods();
        Map<AbstractMap.SimpleEntry<String, List<String>>, MethodDeclaration> existingMethodNamesToArgumentTypes = new HashMap<>();
        for (MethodDeclaration existingMethod : existingMethods) {
          @SuppressWarnings("rawtypes")
          List parameters = existingMethod.parameters();
          List<String> existingMethodParameters = new ArrayList<>();
          for (Object obj : parameters) {
            if (obj instanceof SingleVariableDeclaration) {
              SingleVariableDeclaration param = (SingleVariableDeclaration) obj;
              Type type = param.getType();
              existingMethodParameters.add(type.toString());
            }
          }
          existingMethodNamesToArgumentTypes.put(
            new AbstractMap.SimpleEntry<>(existingMethod.getName().toString(), existingMethodParameters),
            existingMethod);
        }


        final ListRewrite bodyRewriter = rewriter.getListRewrite(typeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
        for (MethodDeclaration method : interfaceVisitor.getMethodDeclarations()) {


          @SuppressWarnings("rawtypes")
          List superTypeParameters = method.parameters();
          List<String> superTypeParamTypes = new ArrayList<>();
          for (Object obj : superTypeParameters) {
            if (obj instanceof SingleVariableDeclaration) {
              SingleVariableDeclaration param = (SingleVariableDeclaration) obj;
              Type type = param.getType();
              if (interfaceParamType != null && type.toString().equals(interfaceParamType.toString())) {
                type = parameterType;
              }
              superTypeParamTypes.add(type.toString());
            }
          }
          @SuppressWarnings("rawtypes")
          AbstractMap.SimpleEntry<String, List> superTypePair = new AbstractMap.SimpleEntry<>(method.getName().toString(), superTypeParamTypes);
          MethodDeclaration matchingExistingMethod = existingMethodNamesToArgumentTypes.get(superTypePair);
          if (matchingExistingMethod != null) {
            @SuppressWarnings("rawtypes")
            List modifiers = matchingExistingMethod.modifiers();
            for (Object obj : modifiers) {
              if (obj instanceof MarkerAnnotation) {
                MarkerAnnotation annotation = (MarkerAnnotation) obj;
                if (annotation.getTypeName().toString().equals(Override.class.getSimpleName())) {
                  final ListRewrite modifiersList = rewriter.getListRewrite(matchingExistingMethod, MethodDeclaration.MODIFIERS2_PROPERTY);
                  modifiersList.remove(annotation, null);
                }
              }
            }
            continue;
          }


          MethodDeclaration newMethodDeclaration =(MethodDeclaration) MethodDeclaration.copySubtree(typeDeclaration.getAST(), method);

          bodyRewriter.insertLast(newMethodDeclaration, null);

          // Rewrite generic types
          if (parameterType != null && interfaceParamType != null) {
            ClassVisitor methodVisitor = new ClassVisitor();
            newMethodDeclaration.accept(methodVisitor);
            for (SimpleType type : methodVisitor.getSimpleTypes()) {
              if (type.getName().toString().equals(interfaceParamType.getName().toString())) {
                rewriter.set(type.getName(), SimpleName.IDENTIFIER_PROPERTY, parameterType.getName(), null);
              }
            }

          }
        }

        // Remove the interface extension
        final ListRewrite interfaceList = rewriter.getListRewrite(typeDeclaration, TypeDeclaration.SUPER_INTERFACE_TYPES_PROPERTY);
        interfaceList.remove(interfaceType, null);

        // Remove the import for the interface
        final ListRewrite importList = rewriter.getListRewrite(compilationUnit, CompilationUnit.IMPORTS_PROPERTY);
        @SuppressWarnings("unchecked")
        List<ImportDeclaration> currentList = importList.getRewrittenList();
        for (ImportDeclaration existingImport : currentList) {
          if (existingImport.getName().getFullyQualifiedName().equals(interfaceName)) {
            importList.remove(existingImport, null);
          }
        }

        // Add the other imports
        for (ImportDeclaration interfaceImport : interfaceVisitor.getImports()) {
          addImport(compilationUnit, interfaceImport.getName().getFullyQualifiedName(), rewriter);
        }

        log.info("Inlining interface [" + interfaceName + "] onto [" + typeDeclaration.getName().toString() + "]");
      }
    }

    //refactorUsingTypeBinding(compilationUnit, node, rewriter);
  }

  @SuppressWarnings("unused")
  private void refactorUsingTypeBinding(CompilationUnit compilationUnit, ASTNode node, ASTRewrite rewriter) {
    if (node instanceof TypeDeclaration) {
      TypeDeclaration typeDeclaration = (TypeDeclaration) node;
      Type interfaceType = findMatch(typeDeclaration);
      if (interfaceType != null) {
        ITypeBinding interfaceTypeBinding = interfaceType.resolveBinding();
        IMethodBinding[] declaredMethods = interfaceTypeBinding.getDeclaredMethods();

        final ListRewrite bodyRewriter = rewriter.getListRewrite(typeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
        for (IMethodBinding methodBinding : declaredMethods) {

          // skip if name and parameters match existing method

          /*
           * Attempt to get parameter names.
           * Actual type is org.eclipse.jdt.core.dom.MethodBinding
           * MethodBinding.binding.parameterNames exists. Possible?
           */

          // Name
          MethodDeclaration newMethod = typeDeclaration.getAST().newMethodDeclaration();
          newMethod.setName(typeDeclaration.getAST().newSimpleName(methodBinding.getName()));

          // Parameters
          List<ITypeBinding> parameterTypeBindings = new ArrayList<>(
              Arrays.asList(methodBinding.getParameterTypes()));
          if (parameterTypeBindings != null) {
            final ListRewrite paramList = rewriter.getListRewrite(newMethod, MethodDeclaration.PARAMETERS_PROPERTY);
            int paramIndex = 0;
            for (ITypeBinding parameterTypeBinding : parameterTypeBindings) {
              String parameterTypeName = parameterTypeBinding.getName();
              ASTNode parameter;
              Type type;
              String identifier;

              parameterTypeBinding.getTypeParameters();

              if (Character.isLowerCase(parameterTypeName.charAt(0))) {
                parameter = node.getAST().newSingleVariableDeclaration();
                type = node.getAST().newPrimitiveType(PrimitiveType.toCode(parameterTypeName));
                identifier = parameterTypeName;
              } else {
                parameter = node.getAST().newSingleVariableDeclaration();
                String parameterQualifiedName = parameterTypeBinding.getQualifiedName().toString();
                if (parameterTypeName.contains("[")) {

                  parameterTypeName = parameterTypeName.replace("[]", "");
                  parameterQualifiedName = parameterQualifiedName.replace("[]", "");

                  // Identify whether actually varargs
                  boolean isLast = parameterTypeBindings.indexOf(parameterTypeBinding) == parameterTypeBindings.size() - 1;
                  if (methodBinding.isVarargs() && isLast) {
                    rewriter.set(parameter, SingleVariableDeclaration.VARARGS_PROPERTY, true, null);
                    type = node.getAST().newSimpleType(node.getAST().newSimpleName(parameterTypeName));
                  } else {
                    type = node.getAST().newArrayType(node.getAST().newSimpleType(node.getAST().newSimpleName(parameterTypeName)));
                  }
                } else {
                  type = node.getAST().newSimpleType(node.getAST().newSimpleName(parameterTypeName));
                }
                identifier = Character.toLowerCase(parameterTypeName.charAt(0)) + parameterTypeName.substring(1);
                addImport(compilationUnit, parameterQualifiedName, rewriter);
              }
              rewriter.set(parameter, SingleVariableDeclaration.TYPE_PROPERTY, type, null);
              rewriter.set(parameter, SingleVariableDeclaration.NAME_PROPERTY, node.getAST().newName(identifier + paramIndex), null);
              paramList.insertLast(parameter, null);
              paramIndex++;
            }
          }

          // Return type
          ITypeBinding returnTypeBinding = methodBinding.getReturnType();
          if (returnTypeBinding != null) {
            String returnTypeName = returnTypeBinding.getName();
            ASTNode returnType;
            if (Character.isLowerCase(returnTypeName.charAt(0))) {
              Code code = PrimitiveType.toCode(returnTypeName);
              returnType = node.getAST().newPrimitiveType(code);
            } else {
              if (returnTypeName.contains("$")) {
                String justReturnTypeName = getSimpleName(returnTypeBinding.getBinaryName());
                returnType = node.getAST().newName(justReturnTypeName);
                String[] enclosingSplit = returnTypeName.split("\\$");
                String enclosingType = enclosingSplit[0];
                Name enclosingName = node.getAST().newName(enclosingType);
                IPackageBinding packageOfEnclosingType = returnTypeBinding.getPackage();
                String fullImport = packageOfEnclosingType.getName() + "." + enclosingName.toString() + "." + justReturnTypeName;
                addImport(compilationUnit, fullImport, rewriter);
              } else {
                if (returnTypeBinding.isParameterizedType()) {
                  String baseTypeName = getSimpleName(returnTypeBinding.getBinaryName());
                  returnType = node.getAST().newParameterizedType(node.getAST().newSimpleType(node.getAST().newName(baseTypeName)));

                  String[] enclosingSplit = returnTypeName.replaceAll(">", "").split("\\<");

                  final ListRewrite paramRewriter = rewriter.getListRewrite(returnType, ParameterizedType.TYPE_ARGUMENTS_PROPERTY);
                  Type param;
                  if (enclosingSplit[1].contains("[")) {
                    param = node.getAST().newArrayType(node.getAST().newSimpleType(
                        node.getAST().newName(enclosingSplit[1].replace("[]", ""))));
                  } else {
                    param = node.getAST().newSimpleType(node.getAST().newName(enclosingSplit[1]));
                  }
                  paramRewriter.insertLast(
                      param, null);


                  IPackageBinding packageOfEnclosingType = returnTypeBinding.getPackage();
                  String fullImport = packageOfEnclosingType.getName() + "." + baseTypeName;
                  addImport(compilationUnit, fullImport, rewriter);
                } else {
                  returnType = node.getAST().newName(returnTypeName);
                  addImport(compilationUnit, returnTypeBinding.getQualifiedName().toString(), rewriter);
                }
              }
            }
            rewriter.set(newMethod, MethodDeclaration.RETURN_TYPE2_PROPERTY, returnType, null);
          }

          // Modifiers
          // Skipped, because it's an interface

          // This approach doesn't get variable names or javadoc

          bodyRewriter.insertLast(newMethod, null);
        }

        // Remove the interface extension
        final ListRewrite interfaceList = rewriter.getListRewrite(typeDeclaration, TypeDeclaration.SUPER_INTERFACE_TYPES_PROPERTY);
        interfaceList.remove(interfaceType, null);

        // Remove the import for the interface
        final ListRewrite importList = rewriter.getListRewrite(compilationUnit, CompilationUnit.IMPORTS_PROPERTY);
        @SuppressWarnings("unchecked")
        List<ImportDeclaration> currentList = importList.getRewrittenList();
        for (ImportDeclaration existingImport : currentList) {
          if (existingImport.getName().getFullyQualifiedName().equals(interfaceName)) {
            importList.remove(existingImport, null);
          }
        }
      }
    }
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
}
