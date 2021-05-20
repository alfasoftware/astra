package org.alfasoftware.astra.core.refactoring.operations.types;

import java.io.IOException;
import java.util.Collection;
import java.util.stream.Stream;

import org.alfasoftware.astra.core.utils.ASTOperation;
import org.alfasoftware.astra.core.utils.AstraUtils;
import org.alfasoftware.astra.core.utils.ClassVisitor;
import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;

/**
 * While the {@link TypeReferenceRefactor} will update references of one type and change it to another,
 * special handling is needed to update the type itself.
 * 
 * For example, the package declaration at the top of the file will need to be updated, 
 * and imports might need to be added for types that may not previously have been imported due to being in the same package.
 * 
 * Note that this doesn't currently move the file - the package declaration will be updated, 
 * but the file itself will still live in the folder matching the "fromType". 
 * This means that the version control move will need to be performed manually outside Astra.
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
    
    // If it is the type we're changing
    if (node instanceof TypeDeclaration) {
      TypeDeclaration typeDeclaration = (TypeDeclaration) node;
      if (AstraUtils.getFullyQualifiedName(typeDeclaration).equals(fromType) &&
          // and if we're updating the package name
          ! AstraUtils.getPackageName(fromType).equals(AstraUtils.getPackageName(toType))) {
        
        updatePackageDeclaration(compilationUnit, rewriter);
        addImportsFromOldPackage(compilationUnit, rewriter);
      }
      
    } else {
      typeReferenceRefactor.run(compilationUnit, node, rewriter);
    }
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

