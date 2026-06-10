package org.alfasoftware.astra.core.refactoring.operations.sonar.s6068;

import java.io.IOException;
import java.util.List;

import org.alfasoftware.astra.core.utils.ASTOperation;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;

/**
 * Abstract base for {@link ASTOperation}s that target the arguments of Mockito
 * methods {@code given}, {@code verify}, and {@code when}.
 *
 * <p>Mirrors the structure of SonarJava's {@code AbstractMockitoArgumentChecker}.
 * Handles all five Mockito entry points and extracts the relevant argument list
 * before delegating to {@link #visitArguments}.
 *
 * <p>The five entry points and how their argument lists are reached:
 * <ul>
 *   <li>{@code Mockito.when(foo.method(args))} — the single argument is itself
 *       the stubbed method call; its argument list is inspected.</li>
 *   <li>{@code BDDMockito.given(foo.method(args))} — same pattern.</li>
 *   <li>{@code Mockito.verify(mock).method(args)} — the chained (outer) call
 *       holds the argument list.</li>
 *   <li>{@code InOrder.verify(mock).method(args)} — same pattern.</li>
 *   <li>{@code Stubber.when(mock).method(args)} — same pattern.</li>
 * </ul>
 *
 * <p>Requires Mockito to be present on the JDT classpath so that method
 * bindings resolve to their declaring types.
 */
public abstract class AbstractMockitoArgumentCheckOperation implements ASTOperation {

  private static final String MOCKITO = "org.mockito.Mockito";
  private static final String BDD_MOCKITO = "org.mockito.BDDMockito";
  private static final String INORDER = "org.mockito.InOrder";
  private static final String STUBBER = "org.mockito.stubbing.Stubber";

  @Override
  public void run(CompilationUnit compilationUnit, ASTNode node, ASTRewrite rewriter)
      throws IOException, MalformedTreeException, BadLocationException {

    if (!(node instanceof MethodInvocation)) {
      return;
    }
    MethodInvocation invocation = (MethodInvocation) node;
    IMethodBinding binding = invocation.resolveMethodBinding();
    if (binding == null) {
      return;
    }

    String declaringType = binding.getDeclaringClass().getQualifiedName();
    String methodName = binding.getName();

    if (isWhenOrGiven(declaringType, methodName)) {
      handleWhenOrGiven(invocation, compilationUnit, rewriter);
    } else if (isVerifyOrStubberWhen(declaringType, methodName)) {
      handleConsecutiveCall(invocation, compilationUnit, rewriter);
    }
  }


  /**
   * For {@code Mockito.when(foo.method(args))} and {@code BDDMockito.given(foo.method(args))},
   * the single argument is itself a method call; its arguments are inspected.
   */
  @SuppressWarnings("unchecked")
  private void handleWhenOrGiven(MethodInvocation invocation, CompilationUnit compilationUnit, ASTRewrite rewriter)
      throws IOException, MalformedTreeException, BadLocationException {

    List<Expression> whenArgs = invocation.arguments();
    if (whenArgs.size() != 1) {
      return;
    }
    Expression arg = skipParentheses(whenArgs.get(0));
    if (arg instanceof MethodInvocation) {
      visitArguments(((MethodInvocation) arg).arguments(), compilationUnit, rewriter);
    }
  }


  /**
   * For {@code verify(mock).method(args)}, {@code InOrder.verify(mock).method(args)}, and
   * {@code Stubber.when(mock).method(args)}, the chained outer call holds the argument list.
   * Mirrors {@code MethodTreeUtils.consecutiveMethodInvocation} in SonarJava.
   */
  @SuppressWarnings("unchecked")
  private void handleConsecutiveCall(MethodInvocation invocation, CompilationUnit compilationUnit, ASTRewrite rewriter)
      throws IOException, MalformedTreeException, BadLocationException {

    ASTNode parent = invocation.getParent();
    if (parent instanceof MethodInvocation) {
      MethodInvocation chainedCall = (MethodInvocation) parent;
      // Confirm this invocation is the receiver of the chained call, not an argument to it.
      if (chainedCall.getExpression() == invocation) {
        visitArguments(chainedCall.arguments(), compilationUnit, rewriter);
      }
    }
  }


  private boolean isWhenOrGiven(String declaringType, String methodName) {
    return (MOCKITO.equals(declaringType) && "when".equals(methodName))
        || (BDD_MOCKITO.equals(declaringType) && "given".equals(methodName));
  }


  private boolean isVerifyOrStubberWhen(String declaringType, String methodName) {
    return (MOCKITO.equals(declaringType) && "verify".equals(methodName))
        || (INORDER.equals(declaringType) && "verify".equals(methodName))
        || (STUBBER.equals(declaringType) && "when".equals(methodName));
  }


  /**
   * Strips any number of nested {@link ParenthesizedExpression} wrappers.
   * Mirrors {@code ExpressionUtils.skipParentheses} in SonarJava.
   */
  protected static Expression skipParentheses(Expression expression) {
    Expression expr = expression;
    while (expr instanceof ParenthesizedExpression) {
      expr = ((ParenthesizedExpression) expr).getExpression();
    }
    return expr;
  }


  /**
   * Inspect the argument list of the stubbed or verified method call.
   * Implementors should examine the arguments and record any fixes in the
   * {@link ASTRewrite} if the rule is violated.
   *
   * @param arguments the arguments of the method being stubbed or verified
   * @param compilationUnit the containing compilation unit
   * @param rewriter to record AST rewrites
   */
  protected abstract void visitArguments(
      List<Expression> arguments, CompilationUnit compilationUnit, ASTRewrite rewriter)
      throws IOException, MalformedTreeException, BadLocationException;
}
