package org.alfasoftware.astra.core.refactoring.operations.types;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.stream.Stream;

import org.alfasoftware.astra.core.utils.ASTOperation;
import org.alfasoftware.astra.core.utils.AstraUtils;
import org.alfasoftware.astra.core.utils.ClassVisitor;
import org.alfasoftware.astra.core.utils.CompilationUnitProperty;
import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;

/**
 * Moves a file to the specified 'toType' location. Makes all the necessary updates on referencing classes.
 * <h3>Actions that will be performed on the 'fromType' class:</h3>
 * <ul>
 *   <li>Update package declaration</li>
 *   <li>Rename type (if necessary)</li>
 *   <li>Update internal references to itself (if necessary)</li>
 *   <li>Update all types referencing refactored type</li>
 * </ul>
 *
 * Limitation: this operation does <b>not</b> currently move the <b>nested types</b>. In scenarios where a nested class is specified
 * to be moved, this operation will still update types referencing the nested class, but the actual movement has
 * to be performed manually.
 */
public class UpdateTypeRefactor implements ASTOperation {

  private static final Logger log = Logger.getLogger(UpdateTypeRefactor.class);

  private final String fromType;
  private final String toType;
  private final TypeReferenceRefactor typeReferenceRefactor;


  private UpdateTypeRefactor(Builder builder) {
    this.fromType = builder.fromType;
    this.toType = builder.toType;
    typeReferenceRefactor = TypeReferenceRefactor.builder().fromType(fromType).toType(toType).build();
  }

  /**
   * @return a new builder for this refactor
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder class for the refactor.
   */
  public static class Builder {
    private String fromType;
    private String toType;
    private Builder() {
      super();
    }

    public Builder fromType(String fromType) {
      this.fromType = fromType;
      return this;
    }

    public Builder toType(String toType) {
      this.toType = toType;
      return this;
    }

    public UpdateTypeRefactor build() {
      return new UpdateTypeRefactor(this);
    }
  }

  /**
   * @return the fromType
   */
  public String getFromType() {
    return fromType;
  }
  
  

  @Override
  public void run(CompilationUnit compilationUnit, ASTNode node, ASTRewrite rewriter)
      throws IOException, MalformedTreeException, BadLocationException {

    if (fromType.equals(toType)) {
      return;
    }

    // If it is the fromType we're changing
    if (node instanceof TypeDeclaration && AstraUtils.getFullyQualifiedName((TypeDeclaration) node).equals(fromType)) {

      boolean isNewPackage = !AstraUtils.getPackageName(fromType).equals(AstraUtils.getPackageName(toType));
      if (isNewPackage) {
        updatePackageDeclaration(compilationUnit, rewriter);
        addImportsFromOldPackage(compilationUnit, rewriter);
      }

      moveType(compilationUnit, node, rewriter);

    } else {
      typeReferenceRefactor.run(compilationUnit, node, rewriter);
    }

  }

  private static boolean isInnerType(ASTNode node) {
    return node.getParent() instanceof TypeDeclaration;
  }

  /**
   * Applies any necessary rename operations on the file content and moves the underline Java source file to its new location.
   */
  private void moveType(CompilationUnit compilationUnit, ASTNode node, ASTRewrite rewriter) {
    if (isInnerType(node)) {
      log.warn(String.format("Moving inner types is currently not supported. Skipping moving [%s]", AstraUtils.getNameForCompilationUnit(compilationUnit)));
      return;
    }

    try {
      Path fileAbsolutePath = Path.of((String) compilationUnit.getProperty(CompilationUnitProperty.ABSOLUTE_PATH));

      // Compute new file location
      String fromTypePath = fromType.replace(".", File.separator);
      String toTypePath = toType.replace(".", File.separator);
      Path fileNewPath = Path.of(fileAbsolutePath.toString().replace(fromTypePath, toTypePath));
      fileNewPath.getParent().toFile().mkdirs();

      // Create the new Java file
      updateInternalTypeReferences(compilationUnit, rewriter);
      String content = AstraUtils.makeChangesFromAST(new String(Files.readAllBytes(fileAbsolutePath)), rewriter);
      Files.write(fileNewPath, content.getBytes(), StandardOpenOption.CREATE);

      // Delete old file
      Files.delete(fileAbsolutePath);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Updates references to the {@link UpdateTypeRefactor#fromType} type to itself.
   * For example, nested classes can reference the parent class being refactored.
   */
  private void updateInternalTypeReferences(CompilationUnit compilationUnit, ASTRewrite rewriter) {
    final ClassVisitor visitor = new ClassVisitor();
    compilationUnit.accept(visitor);
    visitor.getVisitedNodes().forEach(visitedNode -> {
      if (visitedNode instanceof SimpleName) {
        IBinding binding = ((SimpleName) visitedNode).resolveBinding();
        if (binding instanceof ITypeBinding && ((ITypeBinding) binding).getQualifiedName().equals(getFromType())) {
          log.info("Refactoring simple type [" + visitedNode + "] to [" + AstraUtils.getSimpleName(toType) + "] in [" +
              AstraUtils.getNameForCompilationUnit(compilationUnit) + "]");
          rewriter.set(visitedNode, SimpleName.IDENTIFIER_PROPERTY, AstraUtils.getSimpleName(toType), null);
        }
      }
    });
  }
  
  
  private void updatePackageDeclaration(CompilationUnit compilationUnit, ASTRewrite rewriter) {
    if (! AstraUtils.getPackageName(fromType).equals(AstraUtils.getPackageName(toType))) {
      
      log.info("Refactoring package declaration [" + AstraUtils.getPackageName(fromType) + "] "
          + "to [" + AstraUtils.getPackageName(toType) + "] "
          + "in [" + AstraUtils.getNameForCompilationUnit(compilationUnit) + "]");
      
      rewriter.replace(
        compilationUnit.getPackage().getName(), 
        rewriter.createStringPlaceholder(AstraUtils.getPackageName(toType), ASTNode.STRING_LITERAL), 
        null);
    }
  }
  
  
  /*
   * Imports need to be added for types not previously imported due to being in the same package.
   */
  private void addImportsFromOldPackage(CompilationUnit compilationUnit, ASTRewrite rewriter) {
    ClassVisitor visitor = new ClassVisitor();
    compilationUnit.accept(visitor);
    
    Stream.of(
      visitor.getSimpleTypes(),
      visitor.getQualifiedTypes()
    )
    .flatMap(Collection::stream)
    .map(AstraUtils::getFullyQualifiedName)
    .filter(n -> AstraUtils.getPackageName(n).equals(AstraUtils.getPackageName(fromType)))
    .forEach(n -> {
      log.info("Adding import [" + n + "] in [" + AstraUtils.getNameForCompilationUnit(compilationUnit) + "]");
      AstraUtils.addImport(compilationUnit, n, rewriter);
    });
  }
}

