package org.alfasoftware.astra.core.refactoring.operations.interfaces;

import java.io.IOException;
import java.util.List;

import org.alfasoftware.astra.core.matchers.TypeMatcher;
import org.alfasoftware.astra.core.utils.ASTOperation;
import org.alfasoftware.astra.core.utils.ClassVisitor;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;

/**
 * Refactoring operation to remove the unnecessary "public" modifier from interface method declarations.
 */
public class RemovePublicModifierFromInterfaces implements ASTOperation {

  @Override
  public void run(final CompilationUnit compilationUnit, final ASTNode node, final ASTRewrite rewriter) throws IOException, MalformedTreeException, BadLocationException {
    // If the node is an interface
    if (! TypeMatcher.builder().asInterface().build().matches(node)) {
      return;
    }

    // get the method declarations
    getMethodDeclarations(node)
      .stream()
      // find any public modifiers
      .map(n -> n.modifiers())
      .flatMap(List<Modifier>::stream)
      .filter(m -> m.isPublic())
      // remove them
      .forEach(m -> rewriter.remove(m, null));
  }

  private List<MethodDeclaration> getMethodDeclarations(ASTNode node) {
    ClassVisitor visitor = new ClassVisitor();
    node.accept(visitor);
    return visitor.getMethodDeclarations();
  }
}
