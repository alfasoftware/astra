package org.alfasoftware.astra.core.refactoring.operations.javapattern;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.alfasoftware.astra.core.utils.ASTOperation;
import org.alfasoftware.astra.core.utils.AstraUtils;
import org.alfasoftware.astra.core.utils.ClassVisitor;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;


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
  public JavaPatternASTOperation(Path refactorFile) throws IOException {
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
  public JavaPatternASTOperation(Path refactorFile, String[] sources) throws IOException {
    javaPatternFileParser = new JavaPatternFileParser();
    javaPatternFileParser.buildMatchersWithSources(refactorFile, sources);
  }

  /**
   * Takes a File reference to a Matcher file and parses it,
   * as well as an array of Sources and an array of classpaths to use for resolving bindings.
   *
   * @param refactorFile the reference to the Matcher file to use
   * @param sources the sources to use to resolve bindings
   * @param classpath the classpath to use to resolve bindings
   * @throws IOException
   */
  public JavaPatternASTOperation(Path refactorFile, String[] sources, String[] classpath) throws IOException {
    javaPatternFileParser = new JavaPatternFileParser();
    javaPatternFileParser.buildMatchersWithSourcesAndClassPath(refactorFile, sources, classpath);
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
          replaceCapturedVarArgs(rewriter, astNodeMatchInformation, visitor);
          replaceCapturedSubstituteMethods(rewriter, astNodeMatchInformation, visitor);
          replaceCapturedSimpleTypes(rewriter, astNodeMatchInformation, visitor);

          astNode = wrapASTNodeInStatementIfRequired(rewriter, astNodeMatchInformation, astNode);

          CompilationUnit compilationUnit = getCompilationUnit(patternToRefactorTo);
          final ListRewrite listRewrite = rewriter.getListRewrite(targetCompilationUnit, CompilationUnit.IMPORTS_PROPERTY);
          @SuppressWarnings("unchecked")
          List<ImportDeclaration> imports = compilationUnit.imports();
          imports.stream().forEach(
              importFromPattern -> listRewrite.insertLast(importFromPattern, null)
          );

          rewriter.replace(astNodeMatchInformation.getNodeThatWasMatched(), astNode, null);

        }
    );
  }

  private static CompilationUnit getCompilationUnit(ASTNode patternToRefactorTo) {
    if(patternToRefactorTo instanceof CompilationUnit){
      return (CompilationUnit) patternToRefactorTo;
    }
    return getCompilationUnit(patternToRefactorTo.getParent());
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

  private static void replaceCapturedVarArgs(ASTRewrite rewriter, ASTNodeMatchInformation astNodeMatchInformation, ClassVisitor visitor) {
    visitor.getSimpleNames().stream().
        filter(simpleName -> astNodeMatchInformation.getVarArgsToCapturedNodes().get(simpleName.toString()) != null)
        .forEach(
            simpleName -> {
              ListRewrite argumentsList = rewriter.getListRewrite(simpleName.getParent(), MethodInvocation.ARGUMENTS_PROPERTY);
              argumentsList.remove(simpleName, null);
              for (Object object : astNodeMatchInformation.getVarArgsToCapturedNodes().get(simpleName.toString())) {
                argumentsList.insertLast((Expression) object, null);
              }
            }
        );
  }


  /**
   * Replaces the references to @Substitute methods in the patternToRefactorTo with the ASTNodes captured during matching.
   */
  private static void replaceCapturedSubstituteMethods(ASTRewrite rewriter, ASTNodeMatchInformation astNodeMatchInformation, ClassVisitor visitor) {
    visitor.getMethodInvocations().stream().
        filter(methodInvocation -> astNodeMatchInformation.getSubstituteMethodToCapturedNode().get(methodInvocation.getName().toString()) != null)
        .forEach(
            methodInvocation -> {
              final ClassVisitor methodVisitor = new ClassVisitor();
              methodInvocation.accept(methodVisitor);
              replaceCapturedSimpleNames(rewriter, astNodeMatchInformation, methodVisitor);
                final MethodInvocation capturedMethodInvocation = (MethodInvocation) astNodeMatchInformation.getSubstituteMethodToCapturedNode()
                    .get(methodInvocation.getName().toString());
                if(AstraUtils.isMethodInvocationStatic(capturedMethodInvocation)) {
                  rewriter.set(methodInvocation, MethodInvocation.EXPRESSION_PROPERTY, capturedMethodInvocation.getExpression(), null);
                }
                rewriter.set(methodInvocation, MethodInvocation.NAME_PROPERTY,
                    capturedMethodInvocation.getName(), null);

            }
        );
  }

  /**
   * Replaces references to TypeParameters with the Types captured during matching.
   *
   */
  private static void replaceCapturedSimpleTypes(ASTRewrite rewriter, ASTNodeMatchInformation astNodeMatchInformation, ClassVisitor visitor) {
    visitor.getSimpleTypes()
        .stream().filter(simpleType -> astNodeMatchInformation.getSimpleTypeToCapturedType().get(simpleType.getName().toString()) != null)
        .forEach(simpleType ->
            rewriter.replace(simpleType, rewriter.getAST().newSimpleType(
                rewriter.getAST().newSimpleName(astNodeMatchInformation.getSimpleTypeToCapturedType().get(simpleType.getName().toString()).getName())
            ), null)
        );
  }

}
