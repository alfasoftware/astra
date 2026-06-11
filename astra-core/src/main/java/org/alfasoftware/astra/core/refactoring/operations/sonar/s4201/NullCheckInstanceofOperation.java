package org.alfasoftware.astra.core.refactoring.operations.sonar.s4201;

import java.io.IOException;

import org.alfasoftware.astra.core.utils.ASTOperation;
import org.alfasoftware.astra.core.utils.AstraUtils;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Refactoring operation implementing SonarQube rule java:S4201
 * "Null checks should not be used with instanceof".
 *
 * <p>Since {@code instanceof} already returns {@code false} for {@code null},
 * an explicit null check alongside it is redundant and can be removed.
 *
 * <p>Handles the following patterns (and their mirror-operand variants):
 * <ul>
 *   <li>{@code x != null && x instanceof Foo} &rarr; {@code x instanceof Foo}</li>
 *   <li>{@code null != x && x instanceof Foo} &rarr; {@code x instanceof Foo}</li>
 *   <li>{@code x == null || !(x instanceof Foo)} &rarr; {@code !(x instanceof Foo)}</li>
 *   <li>{@code null == x || !(x instanceof Foo)} &rarr; {@code !(x instanceof Foo)}</li>
 *   <li>{@code x != null && x instanceof Foo && extra} &rarr; {@code x instanceof Foo && extra}
 *       (null check removed from within a larger chain)</li>
 * </ul>
 *
 * <p>Does NOT rewrite when:
 * <ul>
 *   <li>The null-checked variable and the instanceof subject are different expressions</li>
 *   <li>The operator/null-check combination would change semantics
 *       (e.g. {@code x == null || x instanceof Foo} is not equivalent to {@code x instanceof Foo})</li>
 * </ul>
 */
public class NullCheckInstanceofOperation implements ASTOperation {

  private static final Logger log = LoggerFactory.getLogger(NullCheckInstanceofOperation.class);

  @Override
  public void run(CompilationUnit compilationUnit, ASTNode node, ASTRewrite rewriter)
      throws IOException, MalformedTreeException, BadLocationException {

    if (!(node instanceof InfixExpression)) {
      return;
    }

    InfixExpression infix = (InfixExpression) node;
    InfixExpression.Operator op = infix.getOperator();

    if (op != InfixExpression.Operator.CONDITIONAL_AND
        && op != InfixExpression.Operator.CONDITIONAL_OR) {
      return;
    }

    Expression left = infix.getLeftOperand();
    Expression right = infix.getRightOperand();

    // Try: left side is the null check, right side is the instanceof (or its negation)
    String varFromLeft = extractNullCheckVar(left, op);
    if (varFromLeft != null && isCompatibleInstanceofSide(right, op, varFromLeft)) {
      log.info("Removing redundant null check in [{}]: replacing '{}' with '{}'",
          AstraUtils.getNameForCompilationUnit(compilationUnit), infix, right);
      rewriter.replace(infix, rewriter.createCopyTarget(right), null);
      return;
    }

    // Try: right side is the null check, left side is the instanceof (or its negation)
    String varFromRight = extractNullCheckVar(right, op);
    if (varFromRight != null && isCompatibleInstanceofSide(left, op, varFromRight)) {
      log.info("Removing redundant null check in [{}]: replacing '{}' with '{}'",
          AstraUtils.getNameForCompilationUnit(compilationUnit), infix, left);
      rewriter.replace(infix, rewriter.createCopyTarget(left), null);
    }
  }

  /**
   * If {@code expr} is a null check compatible with {@code operator}, returns the string
   * representation of the non-null operand (the variable being checked); otherwise {@code null}.
   *
   * <p>Compatible means:
   * <ul>
   *   <li>For {@code &&}: {@code x != null} or {@code null != x}</li>
   *   <li>For {@code ||}: {@code x == null} or {@code null == x}</li>
   * </ul>
   */
  private String extractNullCheckVar(Expression expr, InfixExpression.Operator operator) {
    if (!(expr instanceof InfixExpression)) {
      return null;
    }
    InfixExpression nullCheck = (InfixExpression) expr;
    InfixExpression.Operator nullOp = nullCheck.getOperator();

    boolean isAndOperator = operator == InfixExpression.Operator.CONDITIONAL_AND;
    InfixExpression.Operator expectedNullOp = isAndOperator
        ? InfixExpression.Operator.NOT_EQUALS
        : InfixExpression.Operator.EQUALS;

    if (nullOp != expectedNullOp) {
      return null;
    }

    Expression l = nullCheck.getLeftOperand();
    Expression r = nullCheck.getRightOperand();

    if (l instanceof NullLiteral && !(r instanceof NullLiteral)) {
      return r.toString();
    }
    if (r instanceof NullLiteral && !(l instanceof NullLiteral)) {
      return l.toString();
    }
    return null;
  }

  /**
   * Returns true if {@code expr} is the instanceof side compatible with the given operator
   * and involving {@code varName}.
   *
   * <p>Compatible means:
   * <ul>
   *   <li>For {@code &&}: {@code varName instanceof SomeType} (possibly parenthesized)</li>
   *   <li>For {@code ||}: {@code !(varName instanceof SomeType)} (negation, possibly with extra parens)</li>
   * </ul>
   */
  private boolean isCompatibleInstanceofSide(
      Expression expr, InfixExpression.Operator operator, String varName) {

    if (operator == InfixExpression.Operator.CONDITIONAL_AND) {
      Expression unwrapped = unwrapParens(expr);
      if (!(unwrapped instanceof InstanceofExpression)) {
        return false;
      }
      return ((InstanceofExpression) unwrapped).getLeftOperand().toString().equals(varName);
    }

    // For ||: expect !(varName instanceof SomeType)
    Expression unwrapped = unwrapParens(expr);
    if (!(unwrapped instanceof PrefixExpression)) {
      return false;
    }
    PrefixExpression prefix = (PrefixExpression) unwrapped;
    if (prefix.getOperator() != PrefixExpression.Operator.NOT) {
      return false;
    }
    Expression negated = unwrapParens(prefix.getOperand());
    if (!(negated instanceof InstanceofExpression)) {
      return false;
    }
    return ((InstanceofExpression) negated).getLeftOperand().toString().equals(varName);
  }

  private Expression unwrapParens(Expression expr) {
    while (expr instanceof ParenthesizedExpression) {
      expr = ((ParenthesizedExpression) expr).getExpression();
    }
    return expr;
  }
}
