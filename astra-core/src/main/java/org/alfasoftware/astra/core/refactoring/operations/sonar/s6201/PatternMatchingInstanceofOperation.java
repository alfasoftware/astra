package org.alfasoftware.astra.core.refactoring.operations.sonar.s6201;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.alfasoftware.astra.core.utils.ASTOperation;
import org.alfasoftware.astra.core.utils.AstraUtils;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Refactoring operation implementing SonarQube rule java:S6201
 * "Pattern matching instanceof should be used".
 *
 * <p>Detects the old-style pattern:
 * <pre>
 * if (x instanceof Foo) {
 *   Foo f = (Foo) x;
 *   f.doSomething();
 * }
 * </pre>
 * and rewrites it to use Java 16+ pattern matching:
 * <pre>
 * if (x instanceof Foo f) {
 *   f.doSomething();
 * }
 * </pre>
 *
 * <p>Handles:
 * <ul>
 *   <li>if-statement then-block (condition may be parenthesised, e.g. {@code if ((obj instanceof Foo))})</li>
 *   <li>while-loop body</li>
 *   <li>ternary (conditional expression) then-expression</li>
 * </ul>
 *
 * <p>Does NOT rewrite when:
 * <ul>
 *   <li>The instanceof is negated (parent is {@code !(...)}); the cast in the else branch is a separate flow</li>
 *   <li>The instanceof is part of a compound {@code &&} / {@code ||} condition</li>
 *   <li>The declared cast variable is re-assigned anywhere after declaration</li>
 *   <li>The instanceof subject (left operand) is re-assigned between the condition and the cast declaration</li>
 *   <li>The instanceof subject is not a simple name (complex expressions may have side effects)</li>
 *   <li>The cast type does not exactly match the instanceof type</li>
 * </ul>
 */
public class PatternMatchingInstanceofOperation implements ASTOperation {

  private static final Logger log = LoggerFactory.getLogger(PatternMatchingInstanceofOperation.class);

  @Override
  public void run(CompilationUnit compilationUnit, ASTNode node, ASTRewrite rewriter)
      throws IOException, MalformedTreeException, BadLocationException {

    if (!(node instanceof InstanceofExpression)) {
      return;
    }

    InstanceofExpression instanceofExpr = (InstanceofExpression) node;

    // Only handle simple-name subjects to avoid side-effect concerns with method calls
    if (!(instanceofExpr.getLeftOperand() instanceof SimpleName)) {
      return;
    }

    // Walk up through any wrapping parentheses — e.g. "if ((obj instanceof Foo))" has a
    // ParenthesizedExpression between the InstanceofExpression and the IfStatement.
    Expression condExpr = instanceofExpr;
    while (condExpr.getParent() instanceof ParenthesizedExpression) {
      condExpr = (Expression) condExpr.getParent();
    }
    ASTNode parent = condExpr.getParent();

    if (parent instanceof IfStatement) {
      IfStatement ifStmt = (IfStatement) parent;
      // Direct condition — not negated, not part of &&/||
      if (ifStmt.getExpression() == condExpr && ifStmt.getThenStatement() instanceof Block) {
        tryRewriteBlock(compilationUnit, instanceofExpr, (Block) ifStmt.getThenStatement(), rewriter);
      }
      return;
    }

    if (parent instanceof WhileStatement) {
      WhileStatement whileStmt = (WhileStatement) parent;
      if (whileStmt.getExpression() == condExpr && whileStmt.getBody() instanceof Block) {
        tryRewriteBlock(compilationUnit, instanceofExpr, (Block) whileStmt.getBody(), rewriter);
      }
      return;
    }

    if (parent instanceof ConditionalExpression) {
      ConditionalExpression ternary = (ConditionalExpression) parent;
      if (ternary.getExpression() == condExpr) {
        tryRewriteTernary(compilationUnit, instanceofExpr, ternary, rewriter);
      }
    }
  }

  /**
   * Attempts to rewrite an instanceof + cast declaration pattern in a block.
   *
   * <p>Looks for a {@link VariableDeclarationStatement} anywhere in the block whose:
   * type matches the instanceof right operand, initializer is a cast of the instanceof
   * subject to the same type, and the declared variable is never re-assigned.
   */
  @SuppressWarnings("unchecked")
  private void tryRewriteBlock(
      CompilationUnit compilationUnit,
      InstanceofExpression instanceofExpr,
      Block block,
      ASTRewrite rewriter) {

    String subject = instanceofExpr.getLeftOperand().toString();
    String type = instanceofExpr.getRightOperand().toString();

    List<Statement> statements = block.statements();
    VariableDeclarationStatement castDecl = null;
    String varName = null;

    for (Statement stmt : statements) {
      if (!(stmt instanceof VariableDeclarationStatement)) {
        continue;
      }
      VariableDeclarationStatement vds = (VariableDeclarationStatement) stmt;
      if (!vds.getType().toString().equals(type)) {
        continue;
      }
      List<VariableDeclarationFragment> fragments = vds.fragments();
      if (fragments.size() != 1) {
        continue;
      }
      VariableDeclarationFragment fragment = fragments.get(0);
      Expression initializer = fragment.getInitializer();
      if (!(initializer instanceof CastExpression)) {
        continue;
      }
      CastExpression cast = (CastExpression) initializer;
      if (!cast.getType().toString().equals(type)) {
        continue;
      }
      if (!cast.getExpression().toString().equals(subject)) {
        continue;
      }
      castDecl = vds;
      varName = fragment.getName().getIdentifier();
      break;
    }

    if (castDecl == null) {
      return;
    }

    // Guard: subject must not be reassigned between instanceof and the cast declaration
    if (isNameAssignedBefore(subject, statements, castDecl)) {
      return;
    }

    // Guard: the declared variable must not be reassigned after its declaration
    if (isNameAssignedAfter(varName, statements, castDecl)) {
      return;
    }

    log.info("Rewriting instanceof pattern match in [{}]: {} instanceof {} {}",
        AstraUtils.getNameForCompilationUnit(compilationUnit), subject, type, varName);

    String patternText = subject + " instanceof " + type + " " + varName;
    ASTNode placeholder = rewriter.createStringPlaceholder(patternText, ASTNode.PATTERN_INSTANCEOF_EXPRESSION);
    rewriter.replace(instanceofExpr, placeholder, null);
    rewriter.remove(castDecl, null);
  }

  /**
   * Attempts to rewrite an instanceof used as a ternary condition where the then-expression
   * contains a cast of the same subject to the same type.
   *
   * <p>Example: {@code x instanceof Foo ? ((Foo) x).method() : other}
   * becomes {@code x instanceof Foo foo ? foo.method() : other}.
   */
  private void tryRewriteTernary(
      CompilationUnit compilationUnit,
      InstanceofExpression instanceofExpr,
      ConditionalExpression ternary,
      ASTRewrite rewriter) {

    String subject = instanceofExpr.getLeftOperand().toString();
    String type = instanceofExpr.getRightOperand().toString();

    Expression thenExpr = ternary.getThenExpression();
    List<ASTNode> matchingCasts = findMatchingCasts(thenExpr, subject, type);

    if (matchingCasts.isEmpty()) {
      return;
    }

    String varName = generateVarName(type);

    log.info("Rewriting instanceof ternary pattern match in [{}]: {} instanceof {} {}",
        AstraUtils.getNameForCompilationUnit(compilationUnit), subject, type, varName);

    AST ast = instanceofExpr.getAST();
    for (ASTNode castNode : matchingCasts) {
      rewriter.replace(castNode, ast.newSimpleName(varName), null);
    }

    String patternText = subject + " instanceof " + type + " " + varName;
    ASTNode placeholder = rewriter.createStringPlaceholder(patternText, ASTNode.PATTERN_INSTANCEOF_EXPRESSION);
    rewriter.replace(instanceofExpr, placeholder, null);
  }

  /**
   * Finds CastExpression nodes (and their enclosing ParenthesizedExpression when present)
   * within {@code expr} that cast {@code subject} to {@code type}.
   */
  private List<ASTNode> findMatchingCasts(Expression expr, String subject, String type) {
    List<ASTNode> result = new ArrayList<>();
    expr.accept(new ASTVisitor() {
      @Override
      public boolean visit(CastExpression node) {
        if (node.getType().toString().equals(type)
            && node.getExpression().toString().equals(subject)) {
          // If wrapped in an extra parenthesization, replace the outer paren to avoid "(varName)"
          ASTNode nodeToReplace = (node.getParent() instanceof ParenthesizedExpression)
              ? node.getParent()
              : node;
          result.add(nodeToReplace);
        }
        return true;
      }
    });
    return result;
  }

  /**
   * Returns true if {@code name} appears as the left-hand side of an assignment
   * in any statement appearing BEFORE {@code stopBefore} in {@code statements}.
   */
  private boolean isNameAssignedBefore(
      String name, List<Statement> statements, Statement stopBefore) {
    for (Statement stmt : statements) {
      if (stmt == stopBefore) {
        break;
      }
      if (containsAssignmentTo(name, stmt)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns true if {@code name} appears as the left-hand side of an assignment
   * in any statement appearing AFTER {@code startAfter} in {@code statements}.
   */
  private boolean isNameAssignedAfter(
      String name, List<Statement> statements, Statement startAfter) {
    boolean seen = false;
    for (Statement stmt : statements) {
      if (stmt == startAfter) {
        seen = true;
        continue;
      }
      if (seen && containsAssignmentTo(name, stmt)) {
        return true;
      }
    }
    return false;
  }

  private boolean containsAssignmentTo(String name, ASTNode scope) {
    boolean[] found = {false};
    scope.accept(new ASTVisitor() {
      @Override
      public boolean visit(Assignment node) {
        Expression lhs = node.getLeftHandSide();
        if (lhs instanceof SimpleName && ((SimpleName) lhs).getIdentifier().equals(name)) {
          found[0] = true;
        }
        return !found[0];
      }
    });
    return found[0];
  }

  /**
   * Generates a pattern variable name from a type name by lower-casing the first character
   * of the simple (unqualified, non-generic) name.
   */
  private String generateVarName(String typeName) {
    String simple = typeName;
    int dotIdx = typeName.lastIndexOf('.');
    if (dotIdx >= 0) {
      simple = typeName.substring(dotIdx + 1);
    }
    int ltIdx = simple.indexOf('<');
    if (ltIdx >= 0) {
      simple = simple.substring(0, ltIdx);
    }
    if (simple.isEmpty()) {
      return "it";
    }
    return Character.toLowerCase(simple.charAt(0)) + simple.substring(1);
  }
}
