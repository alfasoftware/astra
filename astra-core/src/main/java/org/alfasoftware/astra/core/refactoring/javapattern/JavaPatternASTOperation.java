package org.alfasoftware.astra.core.refactoring.javapattern;

import org.alfasoftware.astra.core.utils.ASTOperation;
import org.alfasoftware.astra.core.utils.ClassVisitor;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import java.io.File;
import java.io.IOException;


/**
 * This is a generic ASTOperation with support for parsing Java files using the
 * {@link JavaPattern} and {@link JavaPatternReplacement} annotations to describe an AST refactor
 */
public class JavaPatternASTOperation implements ASTOperation {

  private final JavaPatternFileParser javaPatternFileParser;

  public JavaPatternASTOperation(File refactorFile) throws IOException {
    javaPatternFileParser = new JavaPatternFileParser();
    javaPatternFileParser.buildMatchers(refactorFile);
  }

  @Override
  public void run(CompilationUnit compilationUnit, ASTNode node, ASTRewrite rewriter) {
    final JavaPatternASTMatcher javaPatternASTMatcher = javaPatternFileParser.getParsedExpressionMatchers();
    if (javaPatternASTMatcher.matchAndCapture(node)) {
      rewriteTarget(rewriter, javaPatternASTMatcher, javaPatternFileParser.getNodeToRefactorTo(), compilationUnit);
    }
  }

  private static void rewriteTarget(ASTRewrite rewriter, JavaPatternASTMatcher javaPatternASTMatcher, ASTNode nodeToRefactorTo, CompilationUnit targetCompilationUnit) {
    javaPatternASTMatcher.getFoundMatches().forEach(astNodeMatchInformation -> {

          ASTNode astNode = ASTNode.copySubtree(targetCompilationUnit.getAST(), nodeToRefactorTo);

          final ClassVisitor visitor = new ClassVisitor();
          astNode.accept(visitor);

          replaceCapturedSimpleNames(rewriter, astNodeMatchInformation, visitor);
          replaceCapturedMethodPlaceholders(rewriter, astNodeMatchInformation, visitor);
          replaceCapturedSimpleTypes(rewriter, astNodeMatchInformation, visitor);

          astNode = wrapASTNodeInStatementIfRequired(rewriter, astNodeMatchInformation, astNode);

          rewriter.replace(astNodeMatchInformation.getNodeThatWasMatched(), astNode, null);
        }
    );
  }

  private static ASTNode wrapASTNodeInStatementIfRequired(ASTRewrite rewriter, ASTNodeMatchInformation astNodeMatchInformation, ASTNode astNode) {
    if (astNodeMatchInformation.getNodeThatWasMatched() instanceof Statement && astNode instanceof Expression) {
      astNode = rewriter.getAST().newExpressionStatement((Expression) astNode);
    }
    return astNode;
  }

  private static void replaceCapturedSimpleNames(ASTRewrite rewriter, ASTNodeMatchInformation ASTNodeMatchInformation, ClassVisitor visitor) {
    visitor.getSimpleNames().stream().
        filter(simpleName -> ASTNodeMatchInformation.getSimpleNameToCapturedNode().get(simpleName.toString()) != null)
        .forEach(
            simpleName -> rewriter.replace(simpleName, ASTNodeMatchInformation.getSimpleNameToCapturedNode().get(simpleName.toString()), null)
        );
  }

  private static void replaceCapturedMethodPlaceholders(ASTRewrite rewriter, ASTNodeMatchInformation ASTNodeMatchInformation, ClassVisitor visitor) {
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
