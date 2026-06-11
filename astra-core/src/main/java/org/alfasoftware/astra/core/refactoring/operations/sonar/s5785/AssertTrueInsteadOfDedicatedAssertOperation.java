package org.alfasoftware.astra.core.refactoring.operations.sonar.s5785;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.alfasoftware.astra.core.utils.ASTOperation;
import org.alfasoftware.astra.core.utils.AstraUtils;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Refactoring operation implementing SonarQube rule java:S5785
 * "JUnit assertTrue/assertFalse should be simplified to its dedicated assertion".
 *
 * <p>Detects calls to {@code assertTrue} and {@code assertFalse} from JUnit 4 and JUnit 5
 * assertion classes where the boolean argument can be expressed more precisely with a dedicated
 * assertion method, and rewrites them accordingly.
 *
 * <p>Supported assertion classes:
 * <ul>
 *   <li>{@code org.junit.Assert} (JUnit 4)</li>
 *   <li>{@code junit.framework.Assert} (JUnit 4, deprecated)</li>
 *   <li>{@code junit.framework.TestCase} (JUnit 4)</li>
 *   <li>{@code org.junit.jupiter.api.Assertions} (JUnit 5)</li>
 * </ul>
 *
 * <p>Transformations (applies symmetrically for {@code assertTrue}/{@code assertFalse},
 * with {@code assertFalse} using the complement assertion):
 * <ul>
 *   <li>{@code assertTrue(a == null)}       &rarr; {@code assertNull(a)}</li>
 *   <li>{@code assertTrue(a != null)}       &rarr; {@code assertNotNull(a)}</li>
 *   <li>{@code assertTrue(a == b)} (primitives) &rarr; {@code assertEquals(a, b)}</li>
 *   <li>{@code assertTrue(a != b)} (primitives) &rarr; {@code assertNotEquals(a, b)}</li>
 *   <li>{@code assertTrue(a == b)} (objects) &rarr; {@code assertSame(a, b)}</li>
 *   <li>{@code assertTrue(a != b)} (objects) &rarr; {@code assertNotSame(a, b)}</li>
 *   <li>{@code assertTrue(a.equals(b))}    &rarr; {@code assertEquals(a, b)}</li>
 *   <li>{@code assertTrue(Objects.equals(a, b))} &rarr; {@code assertEquals(a, b)}</li>
 *   <li>{@code assertTrue(!expr)}          &rarr; complement of {@code assertTrue(expr)}</li>
 * </ul>
 *
 * <p>Message arguments are preserved in their original position (first for JUnit 4, last for JUnit 5).
 * Static imports are updated when the method is invoked without a qualifier.
 */
public class AssertTrueInsteadOfDedicatedAssertOperation implements ASTOperation {

  private static final Logger log = LoggerFactory.getLogger(AssertTrueInsteadOfDedicatedAssertOperation.class);

  static final Set<String> ASSERTION_CLASS_FQNS = Set.of(
      "org.junit.Assert",
      "junit.framework.Assert",
      "junit.framework.TestCase",
      "org.junit.jupiter.api.Assertions");

  private static final Set<String> JUNIT4_CLASS_FQNS = Set.of(
      "org.junit.Assert",
      "junit.framework.Assert",
      "junit.framework.TestCase");

  private static final String OBJECTS_FQN = "java.util.Objects";

  private enum Assertion {
    NULL("assertNull"),
    NOT_NULL("assertNotNull"),
    SAME("assertSame"),
    NOT_SAME("assertNotSame"),
    EQUALS("assertEquals"),
    NOT_EQUALS("assertNotEquals");

    final String methodName;

    Assertion(String methodName) {
      this.methodName = methodName;
    }

    Assertion complement() {
      switch (this) {
        case NULL:       return NOT_NULL;
        case NOT_NULL:   return NULL;
        case SAME:       return NOT_SAME;
        case NOT_SAME:   return SAME;
        case EQUALS:     return NOT_EQUALS;
        case NOT_EQUALS: return EQUALS;
        default: throw new IllegalStateException("Unknown assertion: " + this);
      }
    }
  }

  private static final class AnalysisResult {
    final Assertion assertion;
    final List<Expression> newArgs;

    AnalysisResult(Assertion assertion, List<Expression> newArgs) {
      this.assertion = assertion;
      this.newArgs = newArgs;
    }
  }

  @Override
  public void run(CompilationUnit compilationUnit, ASTNode node, ASTRewrite rewriter)
      throws IOException, MalformedTreeException, BadLocationException {

    if (!(node instanceof MethodInvocation)) {
      return;
    }
    MethodInvocation mi = (MethodInvocation) node;

    String methodName = mi.getName().getIdentifier();
    if (!"assertTrue".equals(methodName) && !"assertFalse".equals(methodName)) {
      return;
    }

    IMethodBinding binding = mi.resolveMethodBinding();
    if (binding == null) {
      log.debug("Could not resolve binding for {} in [{}] - skipping",
          methodName, AstraUtils.getNameForCompilationUnit(compilationUnit));
      return;
    }
    ITypeBinding declaringClass = binding.getDeclaringClass();
    if (declaringClass == null) {
      return;
    }
    String declaringFqn = declaringClass.getQualifiedName();
    if (!ASSERTION_CLASS_FQNS.contains(declaringFqn)) {
      return;
    }

    boolean isJunit4 = JUNIT4_CLASS_FQNS.contains(declaringFqn);
    boolean isAssertFalse = "assertFalse".equals(methodName);

    @SuppressWarnings("unchecked")
    List<Expression> args = mi.arguments();

    // Find the first boolean-typed argument (mirrors SonarJava's detection)
    Expression boolArg = null;
    for (Expression arg : args) {
      ITypeBinding tb = arg.resolveTypeBinding();
      if (tb != null && tb.isPrimitive() && "boolean".equals(tb.getName())) {
        boolArg = arg;
        break;
      }
    }
    if (boolArg == null) {
      return;
    }

    Optional<AnalysisResult> resultOpt = analyze(boolArg, isAssertFalse);
    if (!resultOpt.isPresent()) {
      return;
    }
    AnalysisResult result = resultOpt.get();

    log.info("Rewriting {}.{} to {} in [{}]",
        declaringClass.getName(), methodName, result.assertion.methodName,
        AstraUtils.getNameForCompilationUnit(compilationUnit));

    // Change the method name
    rewriter.set(mi.getName(), SimpleName.IDENTIFIER_PROPERTY, result.assertion.methodName, null);

    // If the method has no expression qualifier and is static, it was statically imported;
    // add a static import for the replacement method.
    if (mi.getExpression() == null && Modifier.isStatic(binding.getModifiers())) {
      addStaticImportIfNeeded(compilationUnit, rewriter, declaringFqn, result.assertion.methodName);
    }

    // Replace the boolean arg with the new arg(s) in-place, preserving any message arg.
    // Using replace + insertAfter relative to the original boolArg position means the
    // message arg (if present) stays wherever it already is in the list.
    ListRewrite lrw = rewriter.getListRewrite(mi, MethodInvocation.ARGUMENTS_PROPERTY);
    List<Expression> newArgs = result.newArgs;

    ASTNode firstCopy = rewriter.createCopyTarget(newArgs.get(0));
    lrw.replace(boolArg, firstCopy, null);
    for (int i = 1; i < newArgs.size(); i++) {
      ASTNode copy = rewriter.createCopyTarget(newArgs.get(i));
      lrw.insertAfter(copy, boolArg, null);
    }
  }

  private static void addStaticImportIfNeeded(
      CompilationUnit compilationUnit, ASTRewrite rewriter,
      String declaringFqn, String newMethodName) {

    String fullImport = declaringFqn + "." + newMethodName;
    for (ImportDeclaration imp : AstraUtils.getImportDeclarations(compilationUnit)) {
      if (!imp.isStatic()) {
        continue;
      }
      String name = imp.getName().getFullyQualifiedName();
      // On-demand (wildcard) static import already covers the new method
      if (imp.isOnDemand() && declaringFqn.equals(name)) {
        return;
      }
      // Exact import already present
      if (!imp.isOnDemand() && fullImport.equals(name)) {
        return;
      }
    }
    AstraUtils.addStaticImport(compilationUnit, fullImport, rewriter);
  }

  /**
   * Determines the replacement assertion and the new arguments for the given boolean expression.
   * The {@code inverted} flag starts as {@code false} for {@code assertTrue} and {@code true}
   * for {@code assertFalse}, and is flipped each time a logical-complement ({@code !}) is unwrapped.
   */
  private static Optional<AnalysisResult> analyze(Expression expr, boolean inverted) {

    // Logical complement: !(inner) → recurse with flipped polarity
    if (expr instanceof PrefixExpression) {
      PrefixExpression prefix = (PrefixExpression) expr;
      if (PrefixExpression.Operator.NOT.equals(prefix.getOperator())) {
        return analyze(prefix.getOperand(), !inverted);
      }
    }

    // == and != operators
    if (expr instanceof InfixExpression) {
      InfixExpression infix = (InfixExpression) expr;
      if (InfixExpression.Operator.EQUALS.equals(infix.getOperator())) {
        return analyzeEquality(infix, false, inverted);
      }
      if (InfixExpression.Operator.NOT_EQUALS.equals(infix.getOperator())) {
        return analyzeEquality(infix, true, inverted);
      }
    }

    // a.equals(b) or Objects.equals(a, b)
    if (expr instanceof MethodInvocation) {
      MethodInvocation innerMi = (MethodInvocation) expr;
      if (isEqualsMethod(innerMi)) {
        Assertion base = inverted ? Assertion.NOT_EQUALS : Assertion.EQUALS;
        List<Expression> newArgs;
        @SuppressWarnings("unchecked")
        List<Expression> innerArgs = innerMi.arguments();
        IMethodBinding innerBinding = innerMi.resolveMethodBinding();
        ITypeBinding innerDeclaring = innerBinding != null ? innerBinding.getDeclaringClass() : null;
        if (innerDeclaring != null && OBJECTS_FQN.equals(innerDeclaring.getQualifiedName())) {
          // Objects.equals(a, b): carry both arguments directly (whether qualified or statically imported)
          newArgs = List.copyOf(innerArgs);
        } else if (innerMi.getExpression() != null) {
          // a.equals(b): receiver is expected, first argument is actual
          newArgs = List.of(innerMi.getExpression(), innerArgs.get(0));
        } else {
          return Optional.empty();
        }
        return Optional.of(new AnalysisResult(base, newArgs));
      }
    }

    return Optional.empty();
  }

  private static Optional<AnalysisResult> analyzeEquality(
      InfixExpression infix, boolean isNotEquals, boolean inverted) {

    Expression left = infix.getLeftOperand();
    Expression right = infix.getRightOperand();

    // Null check: a == null or null == a
    if (left instanceof NullLiteral) {
      Assertion base = isNotEquals ? Assertion.NOT_NULL : Assertion.NULL;
      Assertion chosen = inverted ? base.complement() : base;
      return Optional.of(new AnalysisResult(chosen, List.of(right)));
    }
    if (right instanceof NullLiteral) {
      Assertion base = isNotEquals ? Assertion.NOT_NULL : Assertion.NULL;
      Assertion chosen = inverted ? base.complement() : base;
      return Optional.of(new AnalysisResult(chosen, List.of(left)));
    }

    // Primitive or reference comparison — requires type resolution
    ITypeBinding leftType = left.resolveTypeBinding();
    ITypeBinding rightType = right.resolveTypeBinding();
    if (leftType == null || rightType == null) {
      return Optional.empty();
    }

    Assertion base;
    if (leftType.isPrimitive() || rightType.isPrimitive()) {
      base = isNotEquals ? Assertion.NOT_EQUALS : Assertion.EQUALS;
    } else {
      base = isNotEquals ? Assertion.NOT_SAME : Assertion.SAME;
    }
    Assertion chosen = inverted ? base.complement() : base;
    return Optional.of(new AnalysisResult(chosen, List.of(left, right)));
  }

  private static boolean isEqualsMethod(MethodInvocation mi) {
    if (!"equals".equals(mi.getName().getIdentifier())) {
      return false;
    }
    IMethodBinding binding = mi.resolveMethodBinding();
    if (binding == null) {
      return false;
    }
    ITypeBinding declaring = binding.getDeclaringClass();
    if (declaring == null) {
      return false;
    }
    @SuppressWarnings("unchecked")
    List<Expression> innerArgs = mi.arguments();

    // Objects.equals(a, b) — static, 2-argument form
    if (OBJECTS_FQN.equals(declaring.getQualifiedName()) && innerArgs.size() == 2) {
      return true;
    }

    // a.equals(b) — instance method with a single Object parameter
    if (innerArgs.size() == 1 && mi.getExpression() != null) {
      ITypeBinding[] params = binding.getParameterTypes();
      return params.length == 1 && "java.lang.Object".equals(params[0].getQualifiedName());
    }

    return false;
  }
}
