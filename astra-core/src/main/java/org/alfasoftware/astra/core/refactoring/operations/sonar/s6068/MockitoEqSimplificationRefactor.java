package org.alfasoftware.astra.core.refactoring.operations.sonar.s6068;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;

/**
 * Removes unnecessary {@code eq(...)} argument-matcher wrappers from Mockito
 * {@code verify}, {@code when}, and {@code given} calls (java:S6068).
 *
 * <p>When every argument to the stubbed or verified method is wrapped in
 * {@code org.mockito.ArgumentMatchers.eq(...)}, those wrappers are redundant:
 * Mockito's default argument matching is already equality-based.  This operation
 * replaces each {@code eq(x)} call with its inner value {@code x}, producing
 * simpler, more readable test code.  Unused static imports of {@code eq} are
 * cleaned up automatically by {@link org.alfasoftware.astra.core.refactoring.operations.imports.UnusedImportRefactor},
 * which runs after every file modification.
 *
 * <p>The all-eq gate — mirroring SonarJava's {@code MockitoEqSimplificationCheck}
 * exactly — means the operation is a no-op when:
 * <ul>
 *   <li>any argument uses a different matcher (e.g. {@code anyString()});</li>
 *   <li>the method is called with zero arguments;</li>
 *   <li>method bindings cannot be resolved (Mockito not on the JDT classpath).</li>
 * </ul>
 *
 * <p>Parenthesised {@code eq()} calls such as {@code (eq(x))} are handled:
 * the parentheses are stripped before the check and the whole wrapped expression
 * is replaced by the inner value.
 *
 * <p><b>Classpath requirement:</b> Mockito must be included in the
 * {@code UseCase}'s additional classpath entries so that JDT can resolve
 * {@code org.mockito.ArgumentMatchers}, {@code org.mockito.Mockito}, etc.
 *
 * <p>Mirrors the detection logic of SonarJava's {@code MockitoEqSimplificationCheck}
 * and {@code AbstractMockitoArgumentChecker}.
 */
public class MockitoEqSimplificationRefactor extends AbstractMockitoArgumentCheckOperation {

  private static final String ARGUMENT_MATCHERS = "org.mockito.ArgumentMatchers";
  /** Deprecated in Mockito 2, removed in Mockito 4 — kept for backwards compatibility. */
  private static final String MATCHERS = "org.mockito.Matchers";
  /** Mockito extends ArgumentMatchers; eq() may be accessed through Mockito directly. */
  private static final String MOCKITO = "org.mockito.Mockito";


  @Override
  protected void visitArguments(
      List<Expression> arguments, CompilationUnit compilationUnit, ASTRewrite rewriter)
      throws IOException, MalformedTreeException, BadLocationException {

    // Collect the list element (arg) alongside the resolved eq() call (unwrapped).
    // We need both: arg is the node to replace; unwrapped is where the inner value lives.
    List<Expression> argNodes = new ArrayList<>();
    List<MethodInvocation> eqCalls = new ArrayList<>();

    for (Expression arg : arguments) {
      Expression unwrapped = skipParentheses(arg);
      if (unwrapped instanceof MethodInvocation mi && isMockitoEq(mi)) {
        argNodes.add(arg);
        eqCalls.add(mi);
      } else {
        // At least one argument is not eq() — mixing matchers with plain values would
        // break Mockito at runtime, so we must leave the whole call unchanged.
        return;
      }
    }

    if (eqCalls.isEmpty()) {
      return; // zero-argument method — nothing to simplify
    }

    for (int i = 0; i < eqCalls.size(); i++) {
      MethodInvocation eq = eqCalls.get(i);
      Expression argNode = argNodes.get(i);
      @SuppressWarnings("unchecked")
      List<Expression> innerArgs = eq.arguments();
      // Replace eq(x) — or (eq(x)) if parenthesised — with a copy of x.
      rewriter.replace(argNode, rewriter.createCopyTarget(innerArgs.get(0)), null);
    }
  }


  private boolean isMockitoEq(MethodInvocation invocation) {
    if (!"eq".equals(invocation.getName().getIdentifier())) {
      return false;
    }
    IMethodBinding binding = invocation.resolveMethodBinding();
    if (binding == null) {
      return false;
    }
    String declaringType = binding.getDeclaringClass().getQualifiedName();
    return ARGUMENT_MATCHERS.equals(declaringType)
        || MATCHERS.equals(declaringType)
        || MOCKITO.equals(declaringType);
  }
}
