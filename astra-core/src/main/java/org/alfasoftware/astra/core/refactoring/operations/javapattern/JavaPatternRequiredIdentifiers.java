package org.alfasoftware.astra.core.refactoring.operations.javapattern;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;

/**
 * Finds the identifiers which must appear in the source of any code that a {@link JavaPattern} matches.
 *
 * <p>{@link JavaPatternASTMatcher} compares the names of the methods invoked and of the types instantiated by a
 * pattern with those of the code it is matched against, except where the pattern captures them: the names of
 * {@link Substitute} methods (and the expressions they are invoked on), names that are the pattern's parameters,
 * type variables, and the arguments after a varargs parameter. Every other one of these names must appear in
 * source code that the pattern matches.
 */
final class JavaPatternRequiredIdentifiers extends ASTVisitor {

  private final Collection<SingleVariableDeclaration> patternParameters;
  private final Collection<MethodDeclaration> substituteMethods;
  private final Set<String> identifiers = new HashSet<>();
  private boolean unresolvedBinding;


  private JavaPatternRequiredIdentifiers(Collection<SingleVariableDeclaration> patternParameters,
      Collection<MethodDeclaration> substituteMethods) {
    this.patternParameters = patternParameters;
    this.substituteMethods = substituteMethods;
  }


  /**
   * @param pattern the pattern to match
   * @param patternParameters the parameters of the {@link JavaPattern} annotated method declaring the pattern
   * @param substituteMethods the {@link Substitute} annotated methods
   * @return identifiers which must all appear in source code matched by the pattern. This is empty if none are
   *         certain to be required, including when the pattern's bindings could not be resolved.
   */
  static Set<String> of(ASTNode pattern, Collection<SingleVariableDeclaration> patternParameters,
      Collection<MethodDeclaration> substituteMethods) {
    JavaPatternRequiredIdentifiers visitor = new JavaPatternRequiredIdentifiers(patternParameters, substituteMethods);
    pattern.accept(visitor);
    return visitor.unresolvedBinding ? Set.of() : Set.copyOf(visitor.identifiers);
  }


  @Override
  public boolean visit(MethodInvocation methodInvocation) {
    IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
    if (methodBinding == null) {
      unresolvedBinding = true;
      return false;
    }
    boolean isSubstitute = substituteMethods.stream()
        .anyMatch(substituteMethod -> methodBinding.getMethodDeclaration().isEqualTo(substituteMethod.resolveBinding()));
    if (! isSubstitute) {
      addUnlessParameterName(methodInvocation.getName());
      visitIfPresent(methodInvocation.getExpression());
    }
    visitArgumentsUpToVarargs(methodInvocation.arguments());
    return false;
  }


  @Override
  public boolean visit(ClassInstanceCreation classInstanceCreation) {
    Type type = classInstanceCreation.getType();
    if (type.isParameterizedType()) {
      type = ((ParameterizedType) type).getType();
    }
    if (type.isSimpleType() && ((SimpleType) type).getName().isSimpleName()) {
      ITypeBinding typeBinding = type.resolveBinding();
      if (typeBinding == null) {
        unresolvedBinding = true;
        return false;
      }
      if (! typeBinding.isTypeVariable()) {
        addUnlessParameterName((SimpleName) ((SimpleType) type).getName());
      }
    }
    visitIfPresent(classInstanceCreation.getExpression());
    visitArgumentsUpToVarargs(classInstanceCreation.arguments());
    visitIfPresent(classInstanceCreation.getAnonymousClassDeclaration());
    return false;
  }


  @Override
  public boolean visit(Javadoc javadoc) {
    // doc comments are not compared
    return false;
  }


  /**
   * An argument which is an array parameter of the pattern captures all of the remaining arguments,
   * so those are not compared.
   *
   * @see JavaPatternASTMatcher.JavaPatternMatcher#matchAndCaptureArgumentList(List, List)
   */
  private void visitArgumentsUpToVarargs(List<?> arguments) {
    for (Object argument : arguments) {
      if (argument instanceof SimpleName) {
        Optional<SingleVariableDeclaration> patternParameter = findPatternParameter((SimpleName) argument);
        if (patternParameter.isPresent()) {
          IVariableBinding parameterBinding = patternParameter.get().resolveBinding();
          if (parameterBinding == null) {
            unresolvedBinding = true;
            return;
          }
          if (parameterBinding.getType().isArray()) {
            return;
          }
        }
      }
      ((ASTNode) argument).accept(this);
    }
  }


  private void visitIfPresent(ASTNode node) {
    if (node != null) {
      node.accept(this);
    }
  }


  private void addUnlessParameterName(SimpleName name) {
    if (findPatternParameter(name).isEmpty()) {
      identifiers.add(name.getIdentifier());
    }
  }


  private Optional<SingleVariableDeclaration> findPatternParameter(SimpleName name) {
    return patternParameters.stream()
        .filter(parameter -> parameter.getName().toString().equals(name.toString()))
        .findAny();
  }
}
