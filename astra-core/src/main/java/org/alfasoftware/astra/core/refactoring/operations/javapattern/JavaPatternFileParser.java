package org.alfasoftware.astra.core.refactoring.operations.javapattern;

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

import org.alfasoftware.astra.core.utils.AstraUtils;
import org.alfasoftware.astra.core.utils.MethodDeclarationVisitor;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;

/**
 * Reads a Java Matcher file and extracts the
 * - @Substitution methods to capture from the java pattern for substitution in the replacement
 * - @JavaPattern a java pattern to match, expressed through a method, with parameters and @Substitute methods specifying elements to capture
 * - @JavaPatternReplacement the expression to replace matches with, specifying how the parameters and @Substitute methods should be used
 */
class JavaPatternFileParser {

  private final Collection<SingleASTNodePatternMatcher> patternsToMatch = new ArrayList<>();
  private final Collection<MethodDeclaration> substituteMethods = new ArrayList<>();
  private ASTNode patternToRefactorTo;

  public void buildMatchers(File javaFile) throws IOException {
    buildMatchersWithSourcesAndClassPath(javaFile, new String[]{}, new String[]{});
  }

  public void buildMatchersWithSources(File javaFile, String[] sources) throws IOException {
    buildMatchersWithSourcesAndClassPath(javaFile, sources, new String[]{});
  }

  public void buildMatchersWithSourcesAndClassPath(File javaFile, String[] sources, String[] classpath) throws IOException {
    String matcherFile = new String(Files.readAllBytes(Paths.get(javaFile.getAbsolutePath())));
    CompilationUnit compilationUnit = AstraUtils.readAsCompilationUnit(matcherFile, sources, classpath);

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
    if (methodsWithJavaPatternReplacementAnnotation.size() != 1) {
      throw new IllegalArgumentException("There should be exactly one method in the pattern matcher file with the @JavaPatternReplacement annotation");
    }
    return methodsWithJavaPatternReplacementAnnotation.get(0);
  }

  private Collection<MethodDeclaration> parseMethodsDefiningExpressionsToRefactorFrom(MethodDeclarationVisitor visitor) {
    final Set<MethodDeclaration> methodsWithJavaCodeToMatch = visitor.getMethodDeclarations().stream()
        .filter(methodDeclaration -> Arrays.stream(methodDeclaration.resolveBinding().getAnnotations()).anyMatch(iAnnotationBinding -> iAnnotationBinding.getName().equals(JavaPattern.class.getSimpleName())))
        .collect(Collectors.toSet());

    if (methodsWithJavaCodeToMatch.isEmpty()) {
      throw new IllegalStateException("There must be at least one @JavaPattern annotated method to match");
    }
    return methodsWithJavaCodeToMatch;
  }

  private Collection<? extends MethodDeclaration> parseSubstituteMethods(MethodDeclarationVisitor visitor) {
    return visitor.getMethodDeclarations().stream()
        .filter(methodDeclaration -> Arrays.stream(methodDeclaration.resolveBinding().getAnnotations()).anyMatch(iAnnotationBinding -> iAnnotationBinding.getName().equals(Substitute.class.getSimpleName())))
        .collect(Collectors.toSet());
  }

  @SuppressWarnings("unchecked")
  private List<SingleASTNodePatternMatcher> parseJavaPatternsToMatch(MethodDeclarationVisitor visitor) {
    final Collection<MethodDeclaration> methodsWithJavaCodeToMatch = parseMethodsDefiningExpressionsToRefactorFrom(visitor);
    List<SingleASTNodePatternMatcher> nodesToMatch = new ArrayList<>();
    methodsWithJavaCodeToMatch.forEach(methodDeclaration -> {
      List<Statement> statements = methodDeclaration.getBody().statements();
      statements.forEach(statement -> {
        ASTNode expressionToMatch;

        if (statement instanceof ReturnStatement) {
          expressionToMatch = ((ReturnStatement) statement).getExpression();
        } else if(statement instanceof ExpressionStatement){
          expressionToMatch = ((ExpressionStatement) statement).getExpression();
        } else {
          expressionToMatch = statement;
        }

        List<SingleVariableDeclaration> singleVariableDeclarations = methodDeclaration.parameters();

        nodesToMatch.add(new SingleASTNodePatternMatcher(expressionToMatch, singleVariableDeclarations));
      });
    });
    return nodesToMatch;
  }

  private ASTNode parsePatternToRefactorTo(MethodDeclarationVisitor visitor) {
    final MethodDeclaration methodToRefactorTo = parseMethodAnnotatedWithJavaPatternReplacement(visitor);
    final Statement statement = (Statement) methodToRefactorTo.getBody().statements().get(0);
    ASTNode parsedPatternToRefactorTo;
    if (statement instanceof ReturnStatement) {
      parsedPatternToRefactorTo = ((ReturnStatement) statement).getExpression();
    } else if (statement instanceof ExpressionStatement){
      parsedPatternToRefactorTo = ((ExpressionStatement) statement).getExpression();
    } else {
      parsedPatternToRefactorTo = statement;
    }
    return parsedPatternToRefactorTo;
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
