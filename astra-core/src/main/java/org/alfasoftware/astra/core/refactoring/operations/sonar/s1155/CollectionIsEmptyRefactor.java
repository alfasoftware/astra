package org.alfasoftware.astra.core.refactoring.operations.sonar.s1155;

import java.io.IOException;

import org.alfasoftware.astra.core.utils.ASTOperation;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;

/**
 * Implements SonarQube rule java:S1155 — {@code Collection.isEmpty()} should be used to test for emptiness.
 *
 * <p>Replaces {@code size()} comparisons against 0 or 1 with the semantically equivalent
 * {@code isEmpty()} call. Both operand orderings are supported.
 *
 * <pre>
 *   col.size() == 0   →  col.isEmpty()
 *   col.size() != 0   →  !col.isEmpty()
 *   col.size() &gt; 0    →  !col.isEmpty()
 *   col.size() &gt;= 1   →  !col.isEmpty()
 *   col.size() &lt; 1    →  col.isEmpty()
 *   col.size() &lt;= 0   →  col.isEmpty()
 *   0 == col.size()   →  col.isEmpty()
 *   0 != col.size()   →  !col.isEmpty()
 * </pre>
 *
 * <p>Binding resolution is used to confirm the receiver is a {@code java.util.Collection} subtype
 * before rewriting. Expressions where the type cannot be resolved are left unchanged.
 */
public class CollectionIsEmptyRefactor implements ASTOperation {

  private static final String COLLECTION_FQN = "java.util.Collection";

  @Override
  public void run(CompilationUnit compilationUnit, ASTNode node, ASTRewrite rewriter)
      throws IOException, MalformedTreeException, BadLocationException {
    if (!(node instanceof InfixExpression)) {
      return;
    }
    InfixExpression infix = (InfixExpression) node;

    // Extended operands occur on chains like a + b + c; comparisons never have them.
    // Guard anyway to keep the logic below simple.
    if (!infix.extendedOperands().isEmpty()) {
      return;
    }

    Expression left = infix.getLeftOperand();
    Expression right = infix.getRightOperand();
    InfixExpression.Operator op = infix.getOperator();

    // size() on left, literal on right
    if (isSizeInvocation(left) && isZeroOrOne(right)) {
      Boolean negate = shouldNegate(op, literalValue(right), false);
      if (negate != null && isCollectionReceiver((MethodInvocation) left)) {
        rewrite(rewriter, infix, (MethodInvocation) left, negate);
      }
      return;
    }

    // literal on left, size() on right (reversed operand ordering)
    if (isZeroOrOne(left) && isSizeInvocation(right)) {
      Boolean negate = shouldNegate(op, literalValue(left), true);
      if (negate != null && isCollectionReceiver((MethodInvocation) right)) {
        rewrite(rewriter, infix, (MethodInvocation) right, negate);
      }
    }
  }


  private boolean isSizeInvocation(Expression expr) {
    if (!(expr instanceof MethodInvocation)) return false;
    MethodInvocation mi = (MethodInvocation) expr;
    return "size".equals(mi.getName().getIdentifier())
        && mi.arguments().isEmpty()
        && mi.getExpression() != null;
  }


  private boolean isZeroOrOne(Expression expr) {
    if (!(expr instanceof NumberLiteral)) return false;
    String token = ((NumberLiteral) expr).getToken();
    return "0".equals(token) || "1".equals(token);
  }


  private int literalValue(Expression expr) {
    return Integer.parseInt(((NumberLiteral) expr).getToken());
  }


  /**
   * Returns {@code true} when the replacement should be {@code !isEmpty()}, {@code false} for
   * {@code isEmpty()}, or {@code null} when the operator/literal combination is not a recognised
   * S1155 pattern.
   *
   * @param op            the infix operator
   * @param value         the numeric literal value (0 or 1)
   * @param sizeOnRight   whether {@code size()} is the right operand (reversed ordering)
   */
  private Boolean shouldNegate(InfixExpression.Operator op, int value, boolean sizeOnRight) {
    InfixExpression.Operator normalized = sizeOnRight ? flip(op) : op;

    if (normalized == InfixExpression.Operator.EQUALS && value == 0) return false;
    if (normalized == InfixExpression.Operator.NOT_EQUALS && value == 0) return true;
    if (normalized == InfixExpression.Operator.GREATER && value == 0) return true;
    if (normalized == InfixExpression.Operator.GREATER_EQUALS && value == 1) return true;
    if (normalized == InfixExpression.Operator.LESS && value == 1) return false;
    if (normalized == InfixExpression.Operator.LESS_EQUALS && value == 0) return false;
    return null;
  }


  private InfixExpression.Operator flip(InfixExpression.Operator op) {
    if (op == InfixExpression.Operator.GREATER) return InfixExpression.Operator.LESS;
    if (op == InfixExpression.Operator.LESS) return InfixExpression.Operator.GREATER;
    if (op == InfixExpression.Operator.GREATER_EQUALS) return InfixExpression.Operator.LESS_EQUALS;
    if (op == InfixExpression.Operator.LESS_EQUALS) return InfixExpression.Operator.GREATER_EQUALS;
    return op; // == and != are symmetric
  }


  private boolean isCollectionReceiver(MethodInvocation sizeCall) {
    Expression receiver = sizeCall.getExpression();
    ITypeBinding binding = receiver.resolveTypeBinding();
    if (binding == null || binding.isRecovered()) {
      return false;
    }
    return implementsCollection(binding);
  }


  private boolean implementsCollection(ITypeBinding binding) {
    if (binding == null) return false;
    if (COLLECTION_FQN.equals(binding.getErasure().getQualifiedName())) return true;
    for (ITypeBinding iface : binding.getInterfaces()) {
      if (implementsCollection(iface)) return true;
    }
    return implementsCollection(binding.getSuperclass());
  }


  private void rewrite(ASTRewrite rewriter, InfixExpression infix, MethodInvocation sizeCall, boolean negate) {
    AST ast = infix.getAST();

    MethodInvocation isEmptyCall = ast.newMethodInvocation();
    isEmptyCall.setName(ast.newSimpleName("isEmpty"));
    isEmptyCall.setExpression((Expression) ASTNode.copySubtree(ast, sizeCall.getExpression()));

    if (negate) {
      PrefixExpression prefix = ast.newPrefixExpression();
      prefix.setOperator(PrefixExpression.Operator.NOT);
      prefix.setOperand(isEmptyCall);
      rewriter.replace(infix, prefix, null);
    } else {
      rewriter.replace(infix, isEmptyCall, null);
    }
  }
}
