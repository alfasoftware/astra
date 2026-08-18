package org.alfasoftware.astra.core.refactoring.operations.sonar.s1125;

import java.io.IOException;

import org.alfasoftware.astra.core.utils.ASTOperation;
import org.alfasoftware.astra.core.utils.AstraUtils;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Refactoring operation implementing SonarQube rule java:S1125
 * "Boolean literals should not be redundant".
 *
 * <p>Removes redundant comparisons with boolean literals:
 * <ul>
 *   <li>{@code x == true} &rarr; {@code x}</li>
 *   <li>{@code x == false} &rarr; {@code !x}</li>
 *   <li>{@code x != true} &rarr; {@code !x}</li>
 *   <li>{@code x != false} &rarr; {@code x}</li>
 *   <li>{@code true == x} &rarr; {@code x}</li>
 *   <li>{@code false == x} &rarr; {@code !x}</li>
 *   <li>{@code true != x} &rarr; {@code !x}</li>
 *   <li>{@code false != x} &rarr; {@code x}</li>
 * </ul>
 *
 * <p>Uses type binding resolution to ensure the non-literal operand is a {@code boolean}
 * or {@code Boolean} before performing the rewrite.
 */
public class RedundantBooleanLiteralRefactor implements ASTOperation {

  private static final Logger log = LoggerFactory.getLogger(RedundantBooleanLiteralRefactor.class);

  @Override
  public void run(CompilationUnit compilationUnit, ASTNode node, ASTRewrite rewriter)
      throws IOException, MalformedTreeException, BadLocationException {

    if (!(node instanceof InfixExpression)) {
      return;
    }

    InfixExpression infix = (InfixExpression) node;
    InfixExpression.Operator op = infix.getOperator();

    if (op != InfixExpression.Operator.EQUALS && op != InfixExpression.Operator.NOT_EQUALS) {
      return;
    }

    if (!infix.extendedOperands().isEmpty()) {
      return;
    }

    Expression left = infix.getLeftOperand();
    Expression right = infix.getRightOperand();

    // booleanVal == true / false
    if (isBooleanLiteral(right) && isBooleanExpression(left)) {
      boolean literalVal = ((BooleanLiteral) right).booleanValue();
      log.info("Removing redundant boolean literal in [{}]: replacing '{}'",
          AstraUtils.getNameForCompilationUnit(compilationUnit), infix);
      rewrite(rewriter, infix, left, op, literalVal);
      return;
    }

    // true / false == booleanVal
    if (isBooleanLiteral(left) && isBooleanExpression(right)) {
      boolean literalVal = ((BooleanLiteral) left).booleanValue();
      log.info("Removing redundant boolean literal in [{}]: replacing '{}'",
          AstraUtils.getNameForCompilationUnit(compilationUnit), infix);
      rewrite(rewriter, infix, right, op, literalVal);
    }
  }

  private boolean isBooleanLiteral(Expression expr) {
    return expr instanceof BooleanLiteral;
  }

  private boolean isBooleanExpression(Expression expr) {
    ITypeBinding binding = expr.resolveTypeBinding();
    if (binding == null) {
      return false;
    }
    String fqn = binding.getErasure().getQualifiedName();
    return "boolean".equals(fqn) || "java.lang.Boolean".equals(fqn);
  }

  private void rewrite(ASTRewrite rewriter, InfixExpression infix, Expression expr, InfixExpression.Operator op, boolean literalVal) {
    AST ast = infix.getAST();
    boolean equals = (op == InfixExpression.Operator.EQUALS);
    boolean negate = equals ? !literalVal : literalVal;

    Expression replacement = (Expression) ASTNode.copySubtree(ast, expr);

    if (negate) {
      PrefixExpression prefix = ast.newPrefixExpression();
      prefix.setOperator(PrefixExpression.Operator.NOT);
      prefix.setOperand(replacement);
      rewriter.replace(infix, prefix, null);
    } else {
      rewriter.replace(infix, replacement, null);
    }
  }
}
