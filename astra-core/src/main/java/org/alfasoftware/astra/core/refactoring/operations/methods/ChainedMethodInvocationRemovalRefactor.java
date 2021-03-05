package org.alfasoftware.astra.core.refactoring.operations.methods;

import java.io.IOException;

import org.alfasoftware.astra.core.matchers.MethodMatcher;
import org.alfasoftware.astra.core.utils.ASTOperation;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;

/**
 * <p>
 * An operation which removes a specific method invocation within a chain of invocations.
 * For example if you are removing a deprecated method from a builder this could remove all usages of it.
 * </p>
 * <p>
 * Note that method calls that result in an assignment to a variable cannot be removed by this
 * for example: Thing thing = obj.withA().withB();  // here we can remove .withA() but not .withB()
 * </p>
 */
public class ChainedMethodInvocationRemovalRefactor implements ASTOperation {

  private final MethodMatcher methodToRemove;


  public ChainedMethodInvocationRemovalRefactor(MethodMatcher toRemove) {
    methodToRemove = toRemove;
  }


  /**
   * Remove a method call from a chain of method calls.
   *
   * Given:
   *      object.withA().withB().withC().withD());
   *
   * If:
   *    we are passed a MethodInvocationMatcher that will match "withC()"
   *
   * Then:
   *     We modify the code such that it becomes:
   *                                            object.withA().withB().withD());
   *
   * @see org.alfasoftware.astra.core.utils.ASTOperation#run(org.eclipse.jdt.core.dom.CompilationUnit, org.eclipse.jdt.core.dom.ASTNode, org.eclipse.jdt.core.dom.rewrite.ASTRewrite)
   */
  @Override
  public void run(CompilationUnit compilationUnit, ASTNode node, ASTRewrite rewriter)
      throws IOException, MalformedTreeException, BadLocationException {
    if (node instanceof MethodInvocation) {
      MethodInvocation methodInvocation = (MethodInvocation) node;

      if (methodToRemove.matches(methodInvocation, compilationUnit)) {

        /*
         * Given:
         *       object.withA().withB().withC().withD());
         *
         * When considering the node ".withC()"...
         *      getParent() refers to   ".withD()"
         *      getExpression() refers to "object.withA().withB()"
         *
         *  This is slightly counter-intuitive.
         *  The aim here is to join the parent and the expression, discarding the target node
         */
        ASTNode parent = methodInvocation.getParent();
        Expression optionalExpression = methodInvocation.getExpression();
        if (optionalExpression != null && optionalExpression instanceof MethodInvocation && parent instanceof MethodInvocation) {
          MethodInvocation previousMethodInvocation = (MethodInvocation) optionalExpression;
          MethodInvocation parentMethod = (MethodInvocation) parent;

          // change the chain to join the Parent to the Expression
          rewriter.set(parentMethod, MethodInvocation.EXPRESSION_PROPERTY, previousMethodInvocation, null);
        }
      }
    }
  }
}