package org.alfasoftware.astra.core.refactoring.operations.javapattern;

import org.alfasoftware.astra.core.utils.ASTOperation;
import org.alfasoftware.astra.core.utils.ClassVisitor;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import java.io.File;
import java.io.IOException;


/**
 * This is a specific ASTOperation with support for parsing Matcher files using the
 * {@link JavaPattern} and {@link JavaPatternReplacement} annotations to describe an AST refactor.
 *
 */
public class JavaPatternASTOperation implements ASTOperation {

  private final JavaPatternFileParser javaPatternFileParser;

  /**
   * Takes a File reference to a Matcher file and parses it.
   *
   * @param refactorFile the reference to the Matcher file to use
   * @throws IOException
   */
  public JavaPatternASTOperation(File refactorFile) throws IOException {
    javaPatternFileParser = new JavaPatternFileParser();
    javaPatternFileParser.buildMatchers(refactorFile);
  }

  /**
   * Takes a File reference to a Matcher file and parses it, as well as an array of Sources to use for resolving bindings.
   *
   * @param refactorFile the reference to the Matcher file to use
   * @param sources the sources to use to resolve bindings
   * @throws IOException
   */
  public JavaPatternASTOperation(File refactorFile, String[] sources) throws IOException {
    javaPatternFileParser = new JavaPatternFileParser();
    javaPatternFileParser.buildMatchersWithClassPath(refactorFile, sources);
  }

  @Override
  public void run(CompilationUnit compilationUnit, ASTNode node, ASTRewrite rewriter) {
    final JavaPatternASTMatcher javaPatternASTMatcher = javaPatternFileParser.getParsedExpressionMatchers();
    if (javaPatternASTMatcher.matchAndCapture(node)) {
      rewriteTarget(rewriter, javaPatternASTMatcher, javaPatternFileParser.getPatternToRefactorTo(), compilationUnit);
    }
  }

  /**
   *
   * Takes match information for the Matcher file specified on instantiation and applies it to the target compilation unit.
   *
   * This works by copying the patternToRefactorTo ASTNode, and replacing all references to parameter and @Substitute methods with the ASTNodes captured during matching.
   *
   * The matched node in the compilation unit is then replaced with the new node.
   *
   * @param rewriter The ASTrewriter to use to rewrite the current compilation unit. Passed in from the framework
   * @param javaPatternASTMatcher The pattern matcher with a collection of NodeMatchInformation describing all matches found.
   * @param patternToRefactorTo The pattern to replace matched nodes with
   * @param targetCompilationUnit The compilation unit we are currently rewriting. Passed in from the framework
   */
  private static void rewriteTarget(ASTRewrite rewriter, JavaPatternASTMatcher javaPatternASTMatcher, ASTNode patternToRefactorTo, CompilationUnit targetCompilationUnit) {
    javaPatternASTMatcher.getFoundMatches().forEach(astNodeMatchInformation -> {

          ASTNode astNode = ASTNode.copySubtree(targetCompilationUnit.getAST(), patternToRefactorTo);

          final ClassVisitor visitor = new ClassVisitor();
          astNode.accept(visitor);

          replaceCapturedSimpleNames(rewriter, astNodeMatchInformation, visitor);
          replaceCapturedSubstituteMethods(rewriter, astNodeMatchInformation, visitor);
          replaceCapturedSimpleTypes(rewriter, astNodeMatchInformation, visitor);

          astNode = wrapASTNodeInStatementIfRequired(rewriter, astNodeMatchInformation, astNode);

          rewriter.replace(astNodeMatchInformation.getNodeThatWasMatched(), astNode, null);
        }
    );
  }

  /**
   * If the ASTNode matched is a Statement, and the pattern we are replacing it with is an Expression, wrap the Expression in an ExpressionStatement
   * so that the trailing ';' is kept.
   */
  private static ASTNode wrapASTNodeInStatementIfRequired(ASTRewrite rewriter, ASTNodeMatchInformation astNodeMatchInformation, ASTNode astNode) {
    if (astNodeMatchInformation.getNodeThatWasMatched() instanceof Statement && astNode instanceof Expression) {
      astNode = rewriter.getAST().newExpressionStatement((Expression) astNode);
    }
    return astNode;
  }

  /**
   * Replaces the references to parameters in the patternToRefactorTo with the ASTNodes captured during matching.
   */
  private static void replaceCapturedSimpleNames(ASTRewrite rewriter, ASTNodeMatchInformation astNodeMatchInformation, ClassVisitor visitor) {
    visitor.getSimpleNames().stream().
        filter(simpleName -> astNodeMatchInformation.getSimpleNameToCapturedNode().get(simpleName.toString()) != null)
        .forEach(
            simpleName -> {
              if(simpleName.getParent() == null) { // To cater for situations where the simple name we're replacing is the whole pattern.
                rewriter.set(simpleName, SimpleName.IDENTIFIER_PROPERTY, astNodeMatchInformation.getSimpleNameToCapturedNode().get(simpleName.toString()), null);
              } else {
                rewriter.replace(simpleName,
                    astNodeMatchInformation.getSimpleNameToCapturedNode().get(simpleName.toString()),
                    null);
              }
            }
        );
  }

  /**
   * Replaces the references to @Substitute methods in the patternToRefactorTo with the ASTNodes captured during matching.
   */
  private static void replaceCapturedSubstituteMethods(ASTRewrite rewriter, ASTNodeMatchInformation ASTNodeMatchInformation, ClassVisitor visitor) {
    visitor.getMethodInvocations().stream().
        filter(methodInvocation -> ASTNodeMatchInformation.getSubstituteMethodToCapturedNode().get(methodInvocation.getName().toString()) != null)
        .forEach(
            methodInvocation -> {
              final ClassVisitor methodVisitor = new ClassVisitor();
              methodInvocation.accept(methodVisitor);
              replaceCapturedSimpleNames(rewriter, ASTNodeMatchInformation, methodVisitor);
              rewriter.set(methodInvocation, MethodInvocation.NAME_PROPERTY,
                  ((MethodInvocation) ASTNodeMatchInformation.getSubstituteMethodToCapturedNode().get(methodInvocation.getName().toString())).getName(), null);
            }
        );
  }

  /**
   * Replaces references to TypeParameters with the Types captured during matching.
   *
   */
  private static void replaceCapturedSimpleTypes(ASTRewrite rewriter, ASTNodeMatchInformation ASTNodeMatchInformation, ClassVisitor visitor) {
    visitor.getSimpleTypes()
        .stream().filter(simpleType -> ASTNodeMatchInformation.getSimpleTypeToCapturedType().get(simpleType.getName().toString()) != null)
        .forEach(simpleType ->
            rewriter.replace(simpleType, rewriter.getAST().newSimpleType(
                rewriter.getAST().newSimpleName(ASTNodeMatchInformation.getSimpleTypeToCapturedType().get(simpleType.getName().toString()))
            ), null)
        );
  }

}
