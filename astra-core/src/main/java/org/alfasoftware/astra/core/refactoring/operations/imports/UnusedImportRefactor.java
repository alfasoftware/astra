package org.alfasoftware.astra.core.refactoring.operations.imports;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.alfasoftware.astra.core.matchers.MethodMatcher;
import org.alfasoftware.astra.core.utils.ASTOperation;
import org.alfasoftware.astra.core.utils.AstraUtils;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.MethodRef;
import org.eclipse.jdt.core.dom.MethodRefParameter;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;

/**
 * Removes unused imports.
 *
 * This refactor can be run standalone, but will also be run as part of cleanup after any operation has altered a file.
 *
 * This refactor currently loses comments that are on the import lines e.g. // NOPMD.
 */
public class UnusedImportRefactor implements ASTOperation {

  private static final String JAVA = "java.";
  private static final String JAVAX = "javax.";
  private static final String ORG = "org.";


  @Override
  public void run(CompilationUnit compilationUnit, ASTNode node, ASTRewrite rewriter)
      throws IOException, MalformedTreeException, BadLocationException {

    final ListRewrite importListRewrite = rewriter.getListRewrite(compilationUnit, CompilationUnit.IMPORTS_PROPERTY);
    
    // remove unnecessary imports
    removeUnnecessaryImports(compilationUnit, node, rewriter);

    @SuppressWarnings("unchecked")
    List<ImportDeclaration> currentList = importListRewrite.getRewrittenList();

    // clear down existing list
    currentList.forEach(i -> importListRewrite.remove(i, null));

    // Sort the imports
    List<ImportDeclaration> sortedImports = sortImports(currentList);

    // Add in blank line separators, if needed
    List<ImportDeclaration> sortedImportsWithSeparators = addSeparatorsToImportList(rewriter, sortedImports);

    // Write in the (now sorted) imports with blank line separators
    for (int i = 0; i < sortedImportsWithSeparators.size(); i++) {
      importListRewrite.insertAt(sortedImportsWithSeparators.get(i), i, null);
    }
  }


  private List<ImportDeclaration> addSeparatorsToImportList(ASTRewrite rewriter, List<ImportDeclaration> sortedImports) {
    List<ImportDeclaration> newList = new ArrayList<>();
    for (int i = 0; i < sortedImports.size(); i++) {
      newList.add(sortedImports.get(i));
      if (sortedImports.size() > i + 1) {
        // Don't put separators between static methods
        if (sortedImports.get(i).isStatic() && sortedImports.get(i + 1).isStatic()) {
          continue;
        }

        // Add blank line separators between:
        // - the static and non-static imports
        // - imports starting with java. and others
        // - imports starting with a different first letter
        if (sortedImports.get(i).isStatic() != sortedImports.get(i + 1).isStatic() ||
            sortedImports.get(i).getName().toString().startsWith(JAVA) != sortedImports.get(i + 1).getName().toString().startsWith(JAVA) ||
            sortedImports.get(i).getName().toString().charAt(0) != sortedImports.get(i + 1).getName().toString().charAt(0)) {
          ASTNode placeholder = rewriter.createStringPlaceholder("", ASTNode.IMPORT_DECLARATION);
          newList.add((ImportDeclaration) placeholder);
        }
      }
    }
    return newList;
  }


  private List<ImportDeclaration> sortImports(List<ImportDeclaration> currentList) {
    return Stream.concat(
        // Static imports are sorted by name alone
        currentList.stream()
        .filter(ImportDeclaration::isStatic)
        .sorted(Comparator.comparing(i -> i.getName().toString())),
        // Non-static imports are sorted with java, javax and org packages first, then package name, then simple name
        currentList.stream()
        .filter(i -> ! i.isStatic())
        .sorted(Comparator
          .comparing((ImportDeclaration i) -> ! AstraUtils.getPackageName(i.getName().toString()).startsWith(JAVA))
          .thenComparing(i -> ! AstraUtils.getPackageName(i.getName().toString()).startsWith(JAVAX))
          .thenComparing(i -> ! AstraUtils.getPackageName(i.getName().toString()).startsWith(ORG))
          .thenComparing(i -> AstraUtils.getPackageName(i.getName().toString()))
          .thenComparing(i -> i.getName().toString()))
          )
      // filter out blank line separators
      .filter(i -> !i.getName().toString().equals("MISSING.MISSING"))
      .collect(Collectors.toList());
  }


  private void removeUnnecessaryImports(CompilationUnit compilationUnit, ASTNode node, ASTRewrite rewriter) {
    // Only remove imports for top-level types
    if (node instanceof TypeDeclaration &&
      ((TypeDeclaration) node).isPackageMemberTypeDeclaration()) {
        
      ReferenceTrackingVisitor visitor = new ReferenceTrackingVisitor();
      compilationUnit.accept(visitor);
      @SuppressWarnings("unchecked")
      List<ImportDeclaration> imports = compilationUnit.imports();
      
      Set<String> remainingImports = new HashSet<>();

      for (ImportDeclaration importDeclaration : imports) {
        
        // Remove unnecessary imports
        // Can't easily tell if on-demand imports are actually needed so best to leave them in place.
        if (! isImportOnDemand(importDeclaration) && 
            (isImportDuplicate(remainingImports, importDeclaration) ||
             ! isImportUsed(visitor, importDeclaration, compilationUnit) ||
             isImportFromSamePackageAndNotStatic(compilationUnit, importDeclaration) ||
             isImportJavaLangAndNotStatic(importDeclaration))) {
          AstraUtils.removeImport(importDeclaration, rewriter);
        }
        
        remainingImports.add(importDeclaration.getName().toString());
      }
    }
  }


  private boolean isImportJavaLangAndNotStatic(ImportDeclaration importDeclaration) {
    return ! importDeclaration.isStatic() && importDeclaration.getName().toString().split("java\\.lang\\.[A-Z]").length > 1
        && ! AstraUtils.isImportOfInnerType(importDeclaration);
  }


  private boolean isImportFromSamePackageAndNotStatic(CompilationUnit compilationUnit, ImportDeclaration importDeclaration) {

    // Imports from the same package will still be valid if they are imports of static methods
    boolean isImportStaticMethod = Optional.ofNullable(importDeclaration)
        .filter(ImportDeclaration::isStatic)
        .map(ImportDeclaration::resolveBinding)
        .map(IMethodBinding.class::isInstance)
        .isPresent();

    return compilationUnit.getPackage().getName().toString().equals(
        AstraUtils.getPackageName(importDeclaration.getName().toString()))
        && ! AstraUtils.isImportOfInnerType(importDeclaration)
        && ! isImportStaticMethod;
  }


  private boolean isImportUsed(ReferenceTrackingVisitor visitor, ImportDeclaration importDeclaration, CompilationUnit compilationUnit) {
    return visitor.types.contains(AstraUtils.getSimpleName(importDeclaration.getName().toString())) ||
        visitor.variables.contains(AstraUtils.getSimpleName(importDeclaration.getName().toString())) ||
        visitor.unresolvableReferences.contains(AstraUtils.getSimpleName(importDeclaration.getName().toString())) ||
        isImportForStaticMethodAndUsed(visitor, importDeclaration, compilationUnit);
  }


  private boolean isImportForStaticMethodAndUsed(ReferenceTrackingVisitor visitor, 
      ImportDeclaration importDeclaration, CompilationUnit compilationUnit) {
    Optional<ITypeBinding> type = Optional.ofNullable(importDeclaration)
          .filter(ImportDeclaration::isStatic)
          .map(ImportDeclaration::resolveBinding)
          .filter(IMethodBinding.class::isInstance)
          .map(IMethodBinding.class::cast)
          .map(IMethodBinding::getDeclaringClass);
    
    if (! type.isPresent()) {
      return false;
    }
    
    return Arrays.stream(type.get().getDeclaredMethods())
      .filter(mb -> mb.getName().equals(AstraUtils.getSimpleName(importDeclaration.getName().toString())))
      .map(MethodMatcher::buildMethodMatcherForMethodBinding)
      .anyMatch(mm -> visitor.methods.stream().anyMatch(mi -> mm.matches(mi, compilationUnit)));
  }


  private boolean isImportDuplicate(Set<String> existingImports, ImportDeclaration importDeclaration) {
    return existingImports.contains(importDeclaration.getName().toString());
  }


  private boolean isImportOnDemand(ImportDeclaration importDeclaration) {
    return importDeclaration.isOnDemand();
  }


  private class ReferenceTrackingVisitor extends ASTVisitor {
    private final Set<String> types = new HashSet<>();
    private final Set<String> variables = new HashSet<>();
    private final Set<MethodInvocation> methods = new HashSet<>();
    
    // If we lack the classpath to resolve a type, we should be conservative about removing imports that match them.
    private final Set<String> unresolvableReferences = new HashSet<>();

    @Override
    public boolean visit(SimpleName node) {
      // If it's a type name
      if (Optional.of(node)
          .filter(n -> ! isInImport(n))
          .map(SimpleName::resolveBinding)
          .filter(ITypeBinding.class::isInstance)
          .isPresent()) {
        types.add(AstraUtils.getSimpleName(node.toString()));
      }
      
      // If it's a variable name
      if (Optional.of(node)
          .filter(n -> ! isInImport(n))
          .map(SimpleName::resolveBinding)
          .filter(IVariableBinding.class::isInstance)
          .isPresent()) {
        variables.add(AstraUtils.getSimpleName(node.toString()));
      }
      if (node.resolveBinding() == null) {
        unresolvableReferences.add(AstraUtils.getSimpleName(node.toString()));
      }
      
      return super.visit(node);
    }
    
    @Override
    public boolean visit(MethodInvocation node) {
      methods.add(node);
      return super.visit(node);
    }

    private boolean isInImport(SimpleName name) {
      ASTNode currentNode = name;
      while (currentNode.getParent() != null) {
        currentNode = currentNode.getParent();
        if (currentNode instanceof ImportDeclaration) {
          return true;
        }
      }
      return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean visit(Javadoc node) {
      for(TagElement element : (List<TagElement>)node.tags()) {
        visitTagElement(element);
      }
      return super.visit(node);
    }

    private void visitTagElement(TagElement element) {
      for (Object fragment : element.fragments()) {
        if (fragment instanceof TagElement) {
          visitTagElement((TagElement) fragment);
        } else if (fragment instanceof SimpleName) {
          types.add(AstraUtils.getSimpleName(((SimpleName) fragment).toString()));
        } else if (fragment instanceof MethodRef) {
          visitJavadocMethodRef((MethodRef) fragment);
        }
      }
    }

    @SuppressWarnings("unchecked")
    private void visitJavadocMethodRef(MethodRef methodRef) {
      if (methodRef.getQualifier() != null) {
        types.add(AstraUtils.getSimpleName(methodRef.getQualifier().toString()));
      }
      for (MethodRefParameter param : (List<MethodRefParameter>)methodRef.parameters()) {
        types.add(param.getType().toString());
      }
    }
  }
}
