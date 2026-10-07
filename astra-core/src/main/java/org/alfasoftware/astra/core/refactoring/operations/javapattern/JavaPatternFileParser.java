package org.alfasoftware.astra.core.refactoring.operations.javapattern;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.alfasoftware.astra.core.utils.AstraUtils;
import org.alfasoftware.astra.core.utils.MethodDeclarationVisitor;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
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

  public void buildMatchers(Path javaFile) throws IOException {
    buildMatchersWithSourcesAndClassPath(javaFile, new String[]{}, new String[]{});
  }

  public void buildMatchersWithSources(Path javaFile, String[] sources) throws IOException {
    buildMatchersWithSourcesAndClassPath(javaFile, sources, new String[]{});
  }

  public void buildMatchersWithSourcesAndClassPath(Path javaFile, String[] sources, String[] classpath) throws IOException {
    String matcherFile = new String(Files.readAllBytes(javaFile.toAbsolutePath()));
    CompilationUnit compilationUnit = AstraUtils.readAsCompilationUnit(javaFile, matcherFile, sources, classpath);

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

        nodesToMatch.add(new SingleASTNodePatternMatcher(expressionToMatch, singleVariableDeclarations, substituteMethods));
      });
    });
    return nodesToMatch;
  }

  private ASTNode parsePatternToRefactorTo(MethodDeclarationVisitor visitor) {
    final MethodDeclaration methodToRefactorTo = parseMethodAnnotatedWithJavaPatternReplacement(visitor);
    final List<Statement> statements = methodToRefactorTo.getBody().statements();
    if (statements.size() != 1) {
      throw new IllegalArgumentException("The method annotated with @JavaPatternReplacement must have exactly one statement in its body, describing the replacement expression");
    }
    final Statement statement = statements.get(0);
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
    return new JavaPatternASTMatcher(patternsToMatch);
  }

  /**
   * @param matchCandidate the ASTNode we are testing for a match
   * @return false if none of the patterns can match the candidate, as determined by cheap checks
   * @see SingleASTNodePatternMatcher#couldMatch(ASTNode)
   */
  public boolean couldAnyPatternMatch(ASTNode matchCandidate) {
    for (SingleASTNodePatternMatcher pattern : patternsToMatch) {
      if (pattern.couldMatch(matchCandidate)) {
        return true;
      }
    }
    return false;
  }

  /**
   * @return a predicate which rejects source code that none of the java patterns can match,
   *         because it does not contain all of the identifiers required by any one of the patterns.
   * @see JavaPatternRequiredIdentifiers
   */
  public Predicate<String> getContentPrefilteringPredicate() {
    final List<Set<String>> requiredIdentifiersOfEachPattern = patternsToMatch.stream()
        .map(pattern -> JavaPatternRequiredIdentifiers.of(pattern.getJavaPatternToMatch(), pattern.getSingleVariableDeclarations(), substituteMethods))
        .collect(Collectors.toList());
    return content ->
        // identifiers can be written using unicode escapes, which a plain text search would not find
        content.contains("\\u") ||
        requiredIdentifiersOfEachPattern.stream().anyMatch(requiredIdentifiers -> requiredIdentifiers.stream().allMatch(content::contains));
  }

  /**
   * Contains the information required to match against an ASTNode.
   *
   * <p>Facts about the pattern which the matcher needs for every candidate node are worked out once, here,
   * rather than for every candidate. This also avoids resolving the pattern's bindings repeatedly, which
   * synchronizes on the pattern's binding resolver, shared by all threads.
   */
  static class SingleASTNodePatternMatcher {
    ASTNode patternToMatch;
    Collection<SingleVariableDeclaration> singleVariableDeclarations;
    private final Map<String, SingleVariableDeclaration> singleVariableDeclarationsByName = new HashMap<>();
    private final Set<MethodInvocation> substituteMethodInvocations = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<MethodInvocation> methodInvocationsWithCapturedNames = Collections.newSetFromMap(new IdentityHashMap<>());

    public SingleASTNodePatternMatcher(ASTNode patternToMatch, List<SingleVariableDeclaration> singleVariableDeclarations,
        Collection<MethodDeclaration> substituteMethods) {
      this.patternToMatch = patternToMatch;
      this.singleVariableDeclarations = singleVariableDeclarations;
      singleVariableDeclarations.forEach(singleVariableDeclaration ->
          singleVariableDeclarationsByName.putIfAbsent(singleVariableDeclaration.getName().getIdentifier(), singleVariableDeclaration));
      patternToMatch.accept(new ASTVisitor() {
        @Override
        public boolean visit(MethodInvocation methodInvocation) {
          IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
          boolean isSubstitute = methodBinding != null && substituteMethods.stream().anyMatch(substituteMethod ->
              methodBinding.getMethodDeclaration().isEqualTo(substituteMethod.resolveBinding()));
          if (isSubstitute) {
            substituteMethodInvocations.add(methodInvocation);
          }
          if (methodBinding == null || isSubstitute || singleVariableDeclarationsByName.containsKey(methodInvocation.getName().getIdentifier())) {
            methodInvocationsWithCapturedNames.add(methodInvocation);
          }
          return true;
        }
      });
    }

    public Collection<SingleVariableDeclaration> getSingleVariableDeclarations() {
      return singleVariableDeclarations;
    }

    /**
     * @param name a name from the pattern
     * @return the parameter of the {@link JavaPattern} annotated method with that name, if there is one
     */
    Optional<SingleVariableDeclaration> findSingleVariableDeclaration(SimpleName name) {
      return Optional.ofNullable(singleVariableDeclarationsByName.get(name.getIdentifier()));
    }

    /**
     * @param methodInvocationFromJavaPattern a MethodInvocation from the pattern
     * @return true if it invokes a {@link Substitute} annotated method
     */
    boolean isSubstituteMethodInvocation(MethodInvocation methodInvocationFromJavaPattern) {
      return substituteMethodInvocations.contains(methodInvocationFromJavaPattern);
    }

    /**
     * @param methodInvocationFromJavaPattern a MethodInvocation from the pattern
     * @return false if a matching MethodInvocation must invoke a method with the same name: true if the method is a
     *         {@link Substitute} method, or the name is that of a pattern parameter, as these are captured rather than
     *         compared. Also true if the method's binding cannot be resolved, leaving that case to the full match.
     */
    boolean isMethodNameCaptured(MethodInvocation methodInvocationFromJavaPattern) {
      return methodInvocationsWithCapturedNames.contains(methodInvocationFromJavaPattern);
    }

    public ASTNode getJavaPatternToMatch() {
      return patternToMatch;
    }

    /**
     * Rejects candidates which the pattern cannot match, using only cheap checks, so that no matcher needs to be created
     * for them. Every node type is matched by the ASTMatcher method for that type, which only matches nodes of the same
     * type, except for names, which {@link JavaPatternASTMatcher} matches more leniently. A pattern which is a
     * MethodInvocation also needs the same method name, unless that is captured.
     *
     * @param matchCandidate the ASTNode we are testing for a match
     * @return false if the pattern cannot match the candidate
     */
    boolean couldMatch(ASTNode matchCandidate) {
      if (patternToMatch instanceof Name) {
        return true;
      }
      if (! patternToMatch.getClass().isInstance(matchCandidate)) {
        return false;
      }
      if (patternToMatch instanceof MethodInvocation && ! isMethodNameCaptured((MethodInvocation) patternToMatch)) {
        return ((MethodInvocation) patternToMatch).getName().getIdentifier().equals(((MethodInvocation) matchCandidate).getName().getIdentifier());
      }
      return true;
    }
  }
}
