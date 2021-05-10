package org.alfasoftware.astra.core.refactoring.operations.javapattern;

import org.alfasoftware.astra.core.utils.AstraUtils;
import org.alfasoftware.astra.core.utils.MethodDeclarationVisitor;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reads a Java Matcher file and extracts the
 * - @Substitution methods to capture from the java pattern for substitution in the replacement
 * - @JavaPattern a java pattern to match, expressed through a method, with parameters and @Substitute methods specifying elements to capture
 * - @JavaPatternReplacement the expression to replace matches with, specifying how the parameters and @Substitute methods should be used
 *
 */
public class JavaPatternFileParser {

  private final Collection<SingleASTNodePatternMatcher> patternsToMatch = new ArrayList<>();
  private final Collection<MethodDeclaration> substituteMethods = new ArrayList<>();
  private ASTNode patternToRefactorTo;

  public void buildMatchers(File javaFile) throws IOException {
    buildMatchersWithClassPath(javaFile, new String[]{});
  }

  public void buildMatchersWithClassPath(File javaFile, String[] sources) throws IOException {
    String matcherFile = new String(Files.readAllBytes(Paths.get(javaFile.getAbsolutePath())));
    CompilationUnit compilationUnit = AstraUtils.readAsCompilationUnit(matcherFile, sources, new String[]{});

    MethodDeclarationVisitor visitor = new MethodDeclarationVisitor();
    compilationUnit.accept(visitor);

    substituteMethods.addAll(parseSubstituteMethods(visitor));

    patternsToMatch.addAll(parseJavaPatternsToMatch(visitor));

    patternToRefactorTo = parsePatternToRefactorTo(visitor);
  }

  private MethodDeclaration parseMethodAnnotatedWithJavaPatternReplacement(MethodDeclarationVisitor visitor) {
    final List<MethodDeclaration> methodsWithJavaPatternReplacementAnnotation = visitor.getMethodDeclarations().stream()
        .filter(methodDeclaration -> Arrays.stream(methodDeclaration.resolveBinding().getAnnotations()).anyMatch(iAnnotationBinding -> iAnnotationBinding.getName().equals(JavaPatternReplacement.class.getSimpleName())))
        .collect(Collectors.toList());
    if(methodsWithJavaPatternReplacementAnnotation.size() != 1) {
      throw new IllegalArgumentException("There should be exactly one method in the pattern matcher file with the @JavaPatternReplacement annotation");
    }
    return methodsWithJavaPatternReplacementAnnotation.get(0);
  }

  private Collection<MethodDeclaration> parseMethodsDefiningExpressionsToRefactorFrom(MethodDeclarationVisitor visitor) {
    final Set<MethodDeclaration> methodsWithJavaCodeToMatch = visitor.getMethodDeclarations().stream()
        .filter(methodDeclaration -> Arrays.stream(methodDeclaration.resolveBinding().getAnnotations()).anyMatch(iAnnotationBinding -> iAnnotationBinding.getName().equals(JavaPattern.class.getSimpleName())))
        .collect(Collectors.toSet());

    if(methodsWithJavaCodeToMatch.isEmpty()){
      throw new IllegalStateException("There must be at least one @JavaPattern annotated method to match");
    }
    return methodsWithJavaCodeToMatch;
  }

  private Collection<? extends MethodDeclaration> parseSubstituteMethods(MethodDeclarationVisitor visitor) {
    return visitor.getMethodDeclarations().stream()
        .filter(methodDeclaration -> Arrays.stream(methodDeclaration.resolveBinding().getAnnotations()).anyMatch(iAnnotationBinding -> iAnnotationBinding.getName().equals(Substitute.class.getSimpleName())))
        .collect(Collectors.toSet());
  }

  private List<SingleASTNodePatternMatcher> parseJavaPatternsToMatch(MethodDeclarationVisitor visitor) {
    final Collection<MethodDeclaration> methodsWithJavaCodeToMatch = parseMethodsDefiningExpressionsToRefactorFrom(visitor);
    List<SingleASTNodePatternMatcher> nodesToMatch = new ArrayList<>();
    methodsWithJavaCodeToMatch.forEach(methodDeclaration ->
    {
      methodDeclaration.getBody()
          .statements().forEach(statement -> {
            ASTNode expressionToMatch;

            if (statement instanceof ReturnStatement) {
              expressionToMatch = ((ReturnStatement) statement).getExpression();
            } else if(statement instanceof ExpressionStatement){
              expressionToMatch = ((ExpressionStatement) statement).getExpression();
            } else {
              expressionToMatch = (ASTNode) statement;
            }

            List<SingleVariableDeclaration> singleVariableDeclarations = methodDeclaration.parameters();

            nodesToMatch.add(new SingleASTNodePatternMatcher(expressionToMatch, singleVariableDeclarations));
          }
      );
    });
    return nodesToMatch;
  }

  private ASTNode parsePatternToRefactorTo(MethodDeclarationVisitor visitor) {
    final MethodDeclaration methodToRefactorTo = parseMethodAnnotatedWithJavaPatternReplacement(visitor);
    final Statement statement = (Statement) methodToRefactorTo.getBody().statements().get(0);
    ASTNode patternToRefactorTo;
    if (statement instanceof ReturnStatement) {
      patternToRefactorTo = ((ReturnStatement) statement).getExpression();
    } else if (statement instanceof ExpressionStatement){
      patternToRefactorTo = ((ExpressionStatement) statement).getExpression();
    } else {
      patternToRefactorTo = statement;
    }
    return patternToRefactorTo;
  }


  public ASTNode getPatternToRefactorTo() {
    return patternToRefactorTo;
  }

  public JavaPatternASTMatcher getParsedExpressionMatchers() {
    return new JavaPatternASTMatcher(patternsToMatch, substituteMethods);
  }

  /**
   * Contains the information required to match against an ASTNode
   */
  static class SingleASTNodePatternMatcher {
    ASTNode patternToMatch;
    Collection<SingleVariableDeclaration> singleVariableDeclarations;

    public SingleASTNodePatternMatcher(ASTNode patternToMatch, List<SingleVariableDeclaration> singleVariableDeclarations) {
      this.patternToMatch = patternToMatch;
      this.singleVariableDeclarations = singleVariableDeclarations;
    }

    public Collection<SingleVariableDeclaration> getSingleVariableDeclarations() {
      return singleVariableDeclarations;
    }

    public ASTNode getJavaPatternToMatch() {
      return patternToMatch;
    }
  }
}
