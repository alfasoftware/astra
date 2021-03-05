package org.alfasoftware.astra.core.refactoring.operations.methods;

import static org.alfasoftware.astra.core.utils.AstraUtils.removeImport;

import java.io.IOException;

import org.alfasoftware.astra.core.matchers.MethodMatcher;
import org.alfasoftware.astra.core.utils.ASTOperation;
import org.alfasoftware.astra.core.utils.AstraUtils;
import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;

/**
 * Refactoring operation to swap a method invocation with a single parameter, to a supplier.
 *
 * For example,
 * <pre>com.google.inject.util.Providers.of(foo)</pre>
 *
 * becomes
 * <pre>() -> foo</pre>
 */
public class MethodWithSingleArgumentToSupplier implements ASTOperation {

  private static final Logger log = Logger.getLogger(MethodInvocationRefactor.class);

  private final MethodMatcher before;

  public MethodWithSingleArgumentToSupplier(MethodMatcher before) {
    super();
    this.before = before;
  }

  @Override
  public void run(CompilationUnit compilationUnit, ASTNode node, ASTRewrite rewriter) throws IOException, MalformedTreeException, BadLocationException {

    if (node instanceof MethodInvocation) {
      MethodInvocation methodInvocation = (MethodInvocation) node;

      if (before.matches(methodInvocation, compilationUnit)) {
        log.info("Refactoring method to supplier: [" + methodInvocation.getName() + "] in [" +
            AstraUtils.getNameForCompilationUnit(compilationUnit) + "]");

        LambdaExpression lambda = methodInvocation.getAST().newLambdaExpression();
        rewriter.set(lambda, LambdaExpression.BODY_PROPERTY, methodInvocation.arguments().get(0), null);
        rewriter.replace(methodInvocation, lambda, null);
        removeImport(compilationUnit, AstraUtils.getFullyQualifiedName(methodInvocation, compilationUnit), rewriter);
      }
    }
  }
}
