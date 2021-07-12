package org.alfasoftware.astra.core.utils;

import java.io.IOException;
import java.util.Set;

import org.alfasoftware.astra.core.refactoring.UseCase;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;

class ExampleWithAnonymousClassDeclaration {

  void foo() {
    
    new UseCase() {
      @Override
      public Set<? extends ASTOperation> getOperations() {
        return null;
      }
    };
    
    new ASTOperation() {
      @Override
      public void run(CompilationUnit compilationUnit, ASTNode node, ASTRewrite rewriter)
          throws IOException, MalformedTreeException, BadLocationException {
        return;
      }
    };
  }
}

