package org.alfasoftware.astra.core.refactoring.operations.methods;

import java.io.IOException;
import java.util.stream.Collectors;

import org.alfasoftware.astra.core.matchers.MethodMatcher;
import org.alfasoftware.astra.core.utils.ASTOperation;
import org.alfasoftware.astra.core.utils.AstraUtils;
import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;

/**
 *  Refactoring operation to remove method declarations matching given criteria.
 *
 *  For example, we could remove the method "setFooBar" from this class:
 *
 *  <pre>
 *  public class Foo {
 *    private int foo;
 *
 *    void setFoo(int foo) {
 *      this.foo = foo;
 *    }
 *
 *    @Deprecated
 *    void setFooBar(int foo) {
 *      this.foo = foo;
 *    }
 *  }
 *  </pre>
 *
 *  So that it would look like this:
 *
 *  <pre>
 *  public class Foo {
 *    private int foo;
 *
 *    void setFoo(int foo) {
 *      this.foo = foo;
 *    }
 *  }
 *  </pre>
 *
 */
public class RemoveMethodDeclarationRefactor implements ASTOperation {
  
  private static final Logger log = Logger.getLogger(RemoveMethodDeclarationRefactor.class);

  private final MethodMatcher matcher;

  public RemoveMethodDeclarationRefactor(MethodMatcher matcher) {
    this.matcher = matcher;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void run(CompilationUnit compilationUnit, ASTNode node, ASTRewrite rewriter)
      throws IOException, MalformedTreeException, BadLocationException {
    if (node instanceof MethodDeclaration) {
      MethodDeclaration methodDeclaration = (MethodDeclaration) node;
      if (matcher.matches(methodDeclaration)) {
        
        log.info("Removing method declaration [" + AstraUtils.getNameForCompilationUnit(compilationUnit) + 
          "#" + methodDeclaration.getName() + 
          "(" + methodDeclaration.parameters().stream().map(p -> p.toString()).collect(Collectors.joining(", ")) + ")]");
        
        rewriter.remove(methodDeclaration, null);
      }
    }
  }

  public MethodMatcher getMatcher() {
    return matcher;
  }
}

