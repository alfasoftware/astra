package org.alfasoftware.astra.core.refactoring.operations.sonar.s4973;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.alfasoftware.astra.core.utils.ASTOperation;
import org.alfasoftware.astra.core.utils.AstraUtils;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fixes SonarQube rule java:S4973 - "Strings and Boxed types should be compared using equals()".
 */
public class StringAndBoxedTypeComparisonRefactor implements ASTOperation {

  private static final Logger log = LoggerFactory.getLogger(StringAndBoxedTypeComparisonRefactor.class);

  private static final Set<String> TARGET_TYPES = Set.of(
      "java.lang.String",
      "java.lang.Integer",
      "java.lang.Long",
      "java.lang.Short",
      "java.lang.Byte",
      "java.lang.Double",
      "java.lang.Float",
      "java.lang.Character",
      "java.lang.Boolean"
  );

  @Override
  public void run(CompilationUnit compilationUnit, ASTNode node, ASTRewrite rewriter)
      throws IOException, MalformedTreeException, BadLocationException {

    if (!(node instanceof InfixExpression)) {
      return;
    }

    InfixExpression infix = (InfixExpression) node;
    InfixExpression.Operator operator = infix.getOperator();

    if (operator != InfixExpression.Operator.EQUALS && operator != InfixExpression.Operator.NOT_EQUALS) {
      return;
    }

    Expression left = infix.getLeftOperand();
    Expression right = infix.getRightOperand();

    if (left instanceof NullLiteral || right instanceof NullLiteral) {
      return;
    }

    ITypeBinding leftType = left.resolveTypeBinding();
    ITypeBinding rightType = right.resolveTypeBinding();

    if (leftType == null || rightType == null || leftType.isRecovered() || rightType.isRecovered()) {
      return;
    }

    if (!TARGET_TYPES.contains(leftType.getQualifiedName()) || !TARGET_TYPES.contains(rightType.getQualifiedName())) {
      return;
    }

    log.info("Rewriting S4973 == / != comparison in [" + AstraUtils.getNameForCompilationUnit(compilationUnit) + "]: [" + infix + "]");

    // If right is a string literal and left is not, swap so literal is the receiver (avoids NPE).
    Expression caller;
    Expression argument;
    if (right instanceof StringLiteral && !(left instanceof StringLiteral)) {
      caller = right;
      argument = left;
    } else {
      caller = left;
      argument = right;
    }

    AST ast = infix.getAST();

    MethodInvocation equalsCall = ast.newMethodInvocation();
    equalsCall.setExpression((Expression) ASTNode.copySubtree(ast, caller));
    equalsCall.setName(ast.newSimpleName("equals"));
    @SuppressWarnings("unchecked")
    List<Expression> args = equalsCall.arguments();
    args.add((Expression) ASTNode.copySubtree(ast, argument));

    Expression replacement;
    if (operator == InfixExpression.Operator.NOT_EQUALS) {
      PrefixExpression not = ast.newPrefixExpression();
      not.setOperator(PrefixExpression.Operator.NOT);
      not.setOperand(equalsCall);
      replacement = not;
    } else {
      replacement = equalsCall;
    }

    rewriter.replace(infix, replacement, null);
  }
}
