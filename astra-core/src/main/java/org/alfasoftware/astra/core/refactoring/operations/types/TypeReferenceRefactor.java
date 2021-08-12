package org.alfasoftware.astra.core.refactoring.operations.types;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.alfasoftware.astra.core.utils.ASTOperation;
import org.alfasoftware.astra.core.utils.AstraUtils;
import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IDocElement;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodRef;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;

/**
 * This refactor changes all references to one type to another.
 * 
 * It can optionally give variables of this type a new variable name, 
 * though this has limitations and may not work where there
 * are multiple variables of the same type in the same scope.
 *
 * <pre>
 * import oldPackage.OldFoo;
 * public OldFoo doSomething(OtherFoo otherFoo) {
 *   OldFoo result = new OldFoo(otherFoo);
 *   result.setSomeField("abc");
 *   return result;
 * }
 * </pre>
 *
 * becomes
 *
 * <pre>
 * import newPackage.NewFoo;
 * public NewFoo doSomething(OtherFoo otherFoo) {
 *   NewFoo result = new NewFoo(otherFoo);
 *   result.setSomeField("abc");
 *   return result;
 * }
 * </pre>
 */
public class TypeReferenceRefactor implements ASTOperation {

  private static final Logger log = Logger.getLogger(TypeReferenceRefactor.class);

  private final String fromType;
  private final String toType;


  private TypeReferenceRefactor(Builder builder) {
    this.fromType = builder.fromType;
    this.toType = builder.toType;
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

    public TypeReferenceRefactor build() {
      return new TypeReferenceRefactor(this);
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

    if (node instanceof SimpleName) {
      updateSimpleName(compilationUnit, (SimpleName) node, rewriter);
    }

    if (node instanceof QualifiedName) {
      updateQualifiedName(compilationUnit, (QualifiedName) node, rewriter);
    }

    if (node instanceof TypeDeclaration) {
      updateJavadocTypes(compilationUnit, (TypeDeclaration) node, rewriter);
    }
  }
  

  private void updateSimpleName(CompilationUnit compilationUnit, SimpleName name, ASTRewrite rewriter) {
    IBinding binding = name.resolveBinding();
    if (binding != null && 
    		binding instanceof ITypeBinding && 
    		((ITypeBinding) binding).getQualifiedName().equals(getFromType())) {
      log.info("Refactoring simple type [" + name.toString() + "] to [" + AstraUtils.getSimpleName(toType) + "] in [" +
          AstraUtils.getNameForCompilationUnit(compilationUnit) + "]");
      rewriter.set(name, SimpleName.IDENTIFIER_PROPERTY, AstraUtils.getSimpleName(toType), null);
    }
  }
  

  private void updateQualifiedName(CompilationUnit compilationUnit, QualifiedName name, ASTRewrite rewriter) {
    if (name.getFullyQualifiedName().equals(getFromType())) {
      log.info("Refactoring qualified type [" + name.getFullyQualifiedName() + "] "
          + "to [" + toType + "] "
          + "in [" + AstraUtils.getNameForCompilationUnit(compilationUnit) + "]");
      rewriter.set(name, QualifiedName.QUALIFIER_PROPERTY, name.getAST().newName(AstraUtils.getPackageName(toType)), null);
      rewriter.set(name, QualifiedName.NAME_PROPERTY, name.getAST().newName(AstraUtils.getSimpleName(toType)), null);
    }
  }
  

  private void updateJavadocTypes(CompilationUnit compilationUnit, TypeDeclaration typeDeclaration, ASTRewrite rewriter) {
    if (typeDeclaration.resolveBinding() != null && ! typeDeclaration.resolveBinding().isNested()) {
      // Special handling for Javadoc references to types
      JavadocVisitor visitor = new JavadocVisitor();
      typeDeclaration.accept(visitor);
      for (Name name : visitor.types) {
        if (name instanceof SimpleName) {
          if (name.toString().equals(AstraUtils.getSimpleName(getFromType()))) {
            log.info("Refactoring simple type in Javadoc [" + name.toString() + "] "
                + "to [" + AstraUtils.getSimpleName(toType) + "] "
                + "in [" + AstraUtils.getNameForCompilationUnit(compilationUnit) + "]");
            rewriter.set(name, SimpleName.IDENTIFIER_PROPERTY, AstraUtils.getSimpleName(toType), null);
          }
        } else if (name instanceof QualifiedName && name.toString().equals(getFromType())) {
          log.info("Refactoring qualified type in Javadoc [" + name.toString() + "] "
              + "to [" + toType + "] "
              + "in [" + AstraUtils.getNameForCompilationUnit(compilationUnit) + "]");

          QualifiedName newQualifiedName = compilationUnit.getAST().newQualifiedName(
            compilationUnit.getAST().newName(toType.replace("." + AstraUtils.getSimpleName(toType), "")),
            compilationUnit.getAST().newSimpleName(AstraUtils.getSimpleName(toType)));
          rewriter.replace(name, newQualifiedName, null);
        }
      }
    }
  }

  private class JavadocVisitor extends ASTVisitor {
    private final Set<Name> types = new HashSet<>();
    @Override
    public boolean visit(Javadoc node) {
      for (TagElement te : getAllTagElementsFromJavadoc(node)) {
        for (Object f : te.fragments()) {
          if (f instanceof SimpleName) {
            Optional.of(f)
              .map(SimpleName.class::cast)
              .map(SimpleName::resolveTypeBinding)
              .map(ITypeBinding::getQualifiedName)
              .filter(n -> n.equals(getFromType()))
              .ifPresent(t -> types.add((SimpleName) f));
            
          } else if (f instanceof QualifiedName) {
            Optional.of(f)
              .map(QualifiedName.class::cast)
              .map(QualifiedName::getFullyQualifiedName)
              .filter(n -> n.equals(getFromType()))
              .ifPresent(t -> types.add((QualifiedName) f));
            
          } else if (f instanceof MethodRef) {
            Optional.of(f)
              .map(MethodRef.class::cast)
              .map(MethodRef::getQualifier)
              .ifPresent(types::add);
          }
        }
      }
      return super.visit(node);
    }
    
    @SuppressWarnings("unchecked")
    private Set<TagElement> getAllTagElementsFromJavadoc(Javadoc node) {
      Set<TagElement> allTags = new HashSet<>();
      List<TagElement> tags = node.tags();
      for (TagElement tag : tags) {
        getAllTagElementsFromTagElement(allTags, tag);
      }
      return allTags;
    }
    
    @SuppressWarnings("unchecked")
    private Set<TagElement> getAllTagElementsFromTagElement(Set<TagElement> tagElements, TagElement tagElement) {
      tagElements.add(tagElement);
      List<IDocElement> fragments = tagElement.fragments();
      for (IDocElement fragment : fragments) {
        if (fragment instanceof TagElement) {
          getAllTagElementsFromTagElement(tagElements, (TagElement) fragment);
        }
      }
      return tagElements;
    }
  }
}

