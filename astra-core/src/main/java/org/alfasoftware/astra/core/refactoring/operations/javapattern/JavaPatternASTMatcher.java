package org.alfasoftware.astra.core.refactoring.operations.javapattern;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;


/**
 * Tries to match some number of JavaPatterns with a candidate ASTNode.
 *
 * Results are returned by calling getFoundMatches() which returns {@link ASTNodeMatchInformation} for
 * each JavaPattern that matched the candidate ASTNode.
 */
class JavaPatternASTMatcher {

  private final Collection<MethodDeclaration> substituteMethods;
  private final Collection<JavaPatternFileParser.SingleASTNodePatternMatcher> javaPatternsToMatch;
  private final Collection<ASTNodeMatchInformation> foundMatches = new ArrayList<>();

  public JavaPatternASTMatcher(Collection<JavaPatternFileParser.SingleASTNodePatternMatcher> javaPatternsToMatch, Collection<MethodDeclaration> substituteMethods) {
    this.javaPatternsToMatch = javaPatternsToMatch;
    this.substituteMethods = substituteMethods;
  }

  /**
   * Iterates over the {@link JavaPattern}s specified and captures the match information
   * @param matchCandidate the ASTNode we are testing for a match
   * @return true if any matches are found
   */
  boolean matchAndCapture(ASTNode matchCandidate){
    for (JavaPatternFileParser.SingleASTNodePatternMatcher javaPatternToMatch: javaPatternsToMatch) {
      final JavaPatternMatcher javaPatternMatcher = new JavaPatternMatcher(javaPatternToMatch);
      if (javaPatternMatcher.match(javaPatternToMatch.getJavaPatternToMatch(), matchCandidate)) {
        foundMatches.add(javaPatternMatcher.getNodeMatch());
      }
    }
    return ! foundMatches.isEmpty();
  }

  /**
   * Returns any found matches and the captured information
   * @return the matches found and their captured information
   */
  public Collection<ASTNodeMatchInformation> getFoundMatches() {
    return foundMatches;
  }


  /**
   * This ASTMatcher sub-type has support for matching a Pattern against a single ASTNode
   * For matches found it collects the match information expressed by the @JavaPattern's method parameters and the @Substitute methods.
   */
  class JavaPatternMatcher extends ASTMatcher {
    private final Map<String, ASTNode> substituteMethodToCapturedNode = new HashMap<>();
    private final Map<String, ASTNode> simpleNameToCapturedNode = new HashMap<>();
    private final JavaPatternFileParser.SingleASTNodePatternMatcher patternToMatch;
    private final Map<String, ITypeBinding> simpleTypeToCapturedType = new HashMap<>();
    private final Map<String,List<ASTNode>> varArgsToCapturedNodes = new HashMap<>();
    private ASTNode astNodeToMatchAgainst;

    public JavaPatternMatcher(JavaPatternFileParser.SingleASTNodePatternMatcher patternToMatch) {
      this.patternToMatch = patternToMatch;
    }


    /**
     * Overridden matcher for simpleName.
     *
     * If the simpleName is one of the parameters specified for the {@link JavaPattern} annotated method, this
     * - Checks that the type of the simpleName and the type resolved for the matchCandidate are compatible. They are considered a match if
     * -- they resolve to the same type
     * -- the match candidate resolves to a type which is a subtype of the simpleName
     * -- the simpleName we are matching is a TypeVariable
     *
     * If we have a match
     * - capture the matchCandidate ASTNode against the simpleName
     * - capture any TypeArguments from the matchCandidate
     * - return true to show that we have a match, and exit early from the matcher for this tree.
     *
     * If the simpleName is not one of the specified parameters we don't care about the name.
     *
     * @param simpleNameFromPatternMatcher the simpleName from the {@link JavaPattern} we are trying to match
     * @param matchCandidate the ASTNode we are testing for a match
     * @return true if the matchCandidate is considered a match for the simpleName that comes from the {@link JavaPattern}
     */
    @Override
    public boolean match(SimpleName simpleNameFromPatternMatcher, Object matchCandidate) {
      final Optional<SingleVariableDeclaration> patternParameter = findPatternParameterFromSimpleName(simpleNameFromPatternMatcher);

      if (patternParameter.isPresent() &&
          isTypeOfSimpleNameCompatibleWithMatchCandidate(simpleNameFromPatternMatcher, (Expression) matchCandidate)) {
        // we may need to resolve Type variables defined in the JavaPattern
        if (simpleNameFromPatternMatcher.resolveTypeBinding().isParameterizedType()) {
          if (!matchAndCaptureSimpleTypesForParameterizedType(simpleNameFromPatternMatcher, (Expression) matchCandidate)) {
            return false;
          }
        }
        return putSimpleNameAndCapturedNode(simpleNameFromPatternMatcher, (ASTNode) matchCandidate);
      } else if (patternParameter.isPresent()) {
        return false;
      } else if (simpleNameFromPatternMatcher.getParent() instanceof SingleVariableDeclaration
      || simpleNameFromPatternMatcher.getLocationInParent().getId().equals("expression")) {
        // don't care about it if it's just the name of a variable
        return true;
      } else {
        return super.match(simpleNameFromPatternMatcher, matchCandidate);
      }
    }


    /**
     * For a parameterized type, verifies that the matchCandidate
     * has the same number of type arguments as the simpleName's type.
     *
     * Captures the SimpleTypes if they have not already been captured.
     * If an existing capture of a simpleType doesn't match the corresponding type argument in the matchCandidate
     * then return false, as we don't have a match between the simpleName and the matchCandidate.
     *
     * @param simpleNameFromPatternMatcher the simpleName we are matching
     * @param matchCandidate the candidate to match against
     * @return false if the simpleName doesn't match the matchCandidate
     */
    private boolean matchAndCaptureSimpleTypesForParameterizedType(SimpleName simpleNameFromPatternMatcher, Expression matchCandidate) {
      final ITypeBinding[] matchCandidateTypeParameters = matchCandidate.resolveTypeBinding().getTypeArguments();
      final ITypeBinding[] simpleTypesToMatch = simpleNameFromPatternMatcher.resolveTypeBinding().getTypeArguments();
      if (matchCandidateTypeParameters.length != simpleTypesToMatch.length) {
        return false;
      }
      for (int i = 0; i < simpleTypesToMatch.length; i++) {
        if (isSimpleTypeAlreadyCapturedWithTypeWhichIsDifferent(matchCandidateTypeParameters[i], simpleTypesToMatch[i])) {
          return false;
        }
        simpleTypeToCapturedType.put(simpleTypesToMatch[i].getName(), matchCandidateTypeParameters[i]);
      }
      return true;
    }

    private boolean isTypeOfSimpleNameCompatibleWithMatchCandidate(SimpleName simpleNameFromPatternMatcher, Expression matchCandidate) {
      return isAssignmentCompatible(simpleNameFromPatternMatcher, matchCandidate) ||
          isSubTypeCompatible(simpleNameFromPatternMatcher, matchCandidate) ||
          isTypeOfSimpleNameEqualToTypeOfMatchCandidate(simpleNameFromPatternMatcher, matchCandidate) ||
          simpleNameFromPatternMatcher.resolveTypeBinding().isTypeVariable();
    }


    /**
     * Checks whether a simpleName from the JavaPattern is one that is a Parameter and should therefore capture match information
     */
    private Optional<SingleVariableDeclaration> findPatternParameterFromSimpleName(SimpleName simpleNameFromPatternMatcher) {
      return patternToMatch.getSingleVariableDeclarations().stream()
          .filter(singleVariableDeclaration -> singleVariableDeclaration.getName().toString().equals(simpleNameFromPatternMatcher.toString()))
          .findAny();
    }


    /**
     * Checks whether we have found an existing value for a simpleName, which isn't the same as the ASTNode we are now matching against.
     * For example, if we have already resolved the simpleName "string" to "foo.toString()", then trying to set "string" to be "bar.stringValue()"
     * means we don't have a match.
     *
     * If we don't already have an ASTNode captured for the simpleName, store it.
     */
    private boolean putSimpleNameAndCapturedNode(SimpleName simpleNameFromPatternMatcher, ASTNode matchCandidate) {
      if (simpleNameToCapturedNode.get(simpleNameFromPatternMatcher.toString()) != null && 
          ! simpleNameToCapturedNode.get(simpleNameFromPatternMatcher.toString()).subtreeMatch(new ASTMatcher(), matchCandidate)) {
        return false;
      } else {
        simpleNameToCapturedNode.put(simpleNameFromPatternMatcher.toString(), matchCandidate);
        return true;
      }
    }


    /**
     * Checks whether we have found an existing value for a TypeArgument, which isn't the same as the Type we are now matching against.
     * For example, if we have already resolved the Type of K from the JavaPattern to be String, then trying to set K to be Integer means
     * we don't have a match.
     */
    private boolean isSimpleTypeAlreadyCapturedWithTypeWhichIsDifferent(ITypeBinding matchCandidateTypeParameter, ITypeBinding simpleTypesToMatch) {
      return simpleTypeToCapturedType.get(simpleTypesToMatch.getName()) != null && 
        ! simpleTypeToCapturedType.get(simpleTypesToMatch.getName()).isEqualTo(matchCandidateTypeParameter);
    }


    /**
     * Checks whether the type of the matchCandidate is the same as the type of the simpleName from the JavaPattern.
     */
    private boolean isTypeOfSimpleNameEqualToTypeOfMatchCandidate(SimpleName simpleNameFromPatternMatcher, Expression matchCandidate) {
      if (matchCandidate.resolveTypeBinding() == null) {
        return false;
      }
      return simpleNameFromPatternMatcher.resolveTypeBinding().getTypeDeclaration()
          .isEqualTo(matchCandidate.resolveTypeBinding().getTypeDeclaration());
    }


    /**
     * Checks whether the type of the matchCandidate can be assigned to the type of the simpleName from the JavaPattern.
     * For example, if the simpleName from the Pattern has type Foo, then any matchCandidate type which is a subtype of Foo should
     * be matched.
     */
    private boolean isAssignmentCompatible(SimpleName simpleNameFromPatternMatcher, Expression matchCandidate) {
      if (matchCandidate.resolveTypeBinding() == null) {
        return false;
      }
      return matchCandidate.resolveTypeBinding().isAssignmentCompatible(simpleNameFromPatternMatcher.resolveTypeBinding());
    }


    /**
     * Checks whether the TypeDeclaration for the resolved type binding for the simpleName and the matchCandidate are sub-type compatible.
     * The TypeDeclaration for the resolved type binding will be the generic version of the Type, if it is parameterised.
     * For example, Map<String, Integer> will return Map<K,V>, allowing us to match for example a matchCandidate which resovles to HashMap<String,Integer> to
     * a generic Map.
     */
    private boolean isSubTypeCompatible(SimpleName simpleNameFromPatternMatcher, Expression matchCandidate) {
      if (matchCandidate.resolveTypeBinding() == null) {
        return false;
      }
      return matchCandidate.resolveTypeBinding().getTypeDeclaration().isSubTypeCompatible(simpleNameFromPatternMatcher.resolveTypeBinding().getTypeDeclaration());
    }


    /**
     * Overridden matcher for MethodInvocation.
     * If the MethodInvocation from the {@link JavaPattern} is an invocation of a {@link Substitute} annotated method,
     * verify that the matchCandidate is appropriate for the substitute and capture it.
     *
     * If the MethodInvocation is not from a {@link Substitute} annotated method, use the full super types matching.
     *
     * Additionally has handling for varargs parameters in the JavaPattern for non {@link Substitute} annotated methods
     */
    @Override
    public boolean match(MethodInvocation methodInvocationFromJavaPattern, Object matchCandidate) {
      if (! (matchCandidate instanceof MethodInvocation)) {
        return false;
      }
      MethodInvocation o = (MethodInvocation) matchCandidate;

      if (! safeSubtreeListMatch(methodInvocationFromJavaPattern.typeArguments(), o.typeArguments())) {
        return false;
      }

      // check whether the number of parameters for the method matches the number of parameters in the method declaration
      // for the method we are comparing to, to be able to compare varargs parameters.
      if (o.resolveMethodBinding() == null || methodInvocationFromJavaPattern.resolveMethodBinding().getMethodDeclaration().getParameterTypes().length
          != o.resolveMethodBinding().getMethodDeclaration().getParameterTypes().length) {
        return false;
      }
      @SuppressWarnings("unchecked")
      List<Expression> arguments = methodInvocationFromJavaPattern.arguments();
      @SuppressWarnings("unchecked")
      List<Expression> otherArguments = o.arguments();
      if (! matchAndCaptureArgumentList(arguments, otherArguments)) {
        return false;
      }

      // If the method invocation is a substitute annotated method, we want the return type to match as well.
      if (methodInvocationMatchesSubstituteMethod(methodInvocationFromJavaPattern)) {
        if (isReturnTypeMatch(methodInvocationFromJavaPattern, ((MethodInvocation) matchCandidate).resolveTypeBinding())) {
          return putSubstituteNameAndCapturedNode(methodInvocationFromJavaPattern, (ASTNode) matchCandidate);
        } else {
          return false;
        }
      }

      return safeSubtreeMatch(methodInvocationFromJavaPattern.getExpression(), o.getExpression())
          && safeSubtreeMatch(methodInvocationFromJavaPattern.getName(), o.getName());
    }


    /**
     * Checks whether the return type of an {@link Substitute} annotated method can be assigned from the matchCandidate.
     * If the return type is a TypeVariable V, then if we have already resolved a type for V, check that the return type
     * of the matchCandidate is assignable to the type of V.
     */
    private boolean isReturnTypeMatch(MethodInvocation methodInvocationFromJavaPattern, ITypeBinding matchCandidate) {
      final ITypeBinding returnType = methodInvocationFromJavaPattern.resolveMethodBinding().getReturnType();
      if (returnType.isTypeVariable() && simpleTypeToCapturedType.get(returnType.getName()) != null) {
        return matchCandidate.isAssignmentCompatible(simpleTypeToCapturedType.get(returnType.getName()));
      } else if (returnType.isTypeVariable() && simpleTypeToCapturedType.get(returnType.getName()) == null) {
        simpleTypeToCapturedType.put(returnType.getName(), matchCandidate);
        return true;
      } else {
        return returnType.isEqualTo(matchCandidate);
      }
    }


    /**
     * Checks whether we have found an existing value for a {@link Substitute} method, which isn't the same as the ASTNode we are now matching against.
     * For example, if we have already resolved the substitute method "aMethod" to "foo.toString()", then trying to set "aMethod" to be "bar.stringValue()"
     * means we don't have a match.
     *
     * If we don't already have an ASTNode captured for the substitute method, store the matchCandidate.
     */
    private boolean putSubstituteNameAndCapturedNode(MethodInvocation methodInvocationFromJavaPattern, ASTNode matchCandidate) {
      if (substituteMethodToCapturedNode.get(methodInvocationFromJavaPattern.toString()) != null && 
          ! substituteMethodToCapturedNode.get(methodInvocationFromJavaPattern.toString()).subtreeMatch(new ASTMatcher(), matchCandidate)) {
        return false;
      } else {
        substituteMethodToCapturedNode.put(methodInvocationFromJavaPattern.getName().toString(),  matchCandidate);
        return true;
      }
    }


    /**
     * Overridden match to be more relaxed about static arguments.
     */
    @Override
    public boolean match(QualifiedName node, Object other) {
      if (other instanceof SimpleName) {
        final IBinding iBinding = ((SimpleName) other).resolveBinding();
        if (iBinding.isEqualTo(node.resolveBinding())) {
          return true;
        }
      }
      if (! (other instanceof QualifiedName)) {
        return false;
      }
      QualifiedName o = (QualifiedName) other;
      return safeSubtreeMatch(node.getQualifier(), o.getQualifier())
          && safeSubtreeMatch(node.getName(), o.getName());
    }


    /**
     * If the simpleType from the pattern is a TypeVariable
     * then make sure that either we haven't already resolved a different type for it.
     * If we haven't resolved a type yet, then capture the type found and consider it a match.
     *
     */
    @Override
    public boolean match(SimpleType node, Object other) {
      if (! (other instanceof SimpleType)) {
        return false;
      }
      SimpleType o = (SimpleType) other;

      if (node.resolveBinding().isTypeVariable() &&
          isSimpleTypeAlreadyCapturedWithTypeWhichIsDifferent(o.resolveBinding(), node.resolveBinding())) {
        return false;
      } else if (node.resolveBinding().isTypeVariable()) {
        simpleTypeToCapturedType.put(node.resolveBinding().getName(), o.resolveBinding());
        return true;
      } else {
        return super.match(node, other);
      }
    }


    /**
     * Overridden matcher for ClassInstanceCreation to handle varargs specified in the JavaPattern.
     *
     */
    @Override
    public boolean match(ClassInstanceCreation node, Object other) {
      if (! (other instanceof ClassInstanceCreation)) {
        return false;
      }
      ClassInstanceCreation o = (ClassInstanceCreation) other;
      if (! safeSubtreeListMatch(node.typeArguments(), o.typeArguments())) {
        return false;
      }
      if (! safeSubtreeMatch(node.getType(), o.getType())) {
        return false;
      }

      @SuppressWarnings("unchecked")
      List<Expression> arguments = node.arguments();
      @SuppressWarnings("unchecked")
      List<Expression> otherArguments = o.arguments();
      if (arguments.size() != o.resolveConstructorBinding().getParameterTypes().length) {
        return false;
      }

      if (! matchAndCaptureArgumentList(arguments, otherArguments)) {
        return false;
      }

      return
          safeSubtreeMatch(node.getExpression(), o.getExpression())
              && safeSubtreeMatch(
              node.getAnonymousClassDeclaration(),
              o.getAnonymousClassDeclaration());
    }


    /**
     * Similar to safeSubTreeListMatch, but has handling for when the left hand side is an array/varargs.
     * In that case, it will capture the remaining arguments from the list on the right hand side.
     *
     * The method assumes that checks on the number of arguments on the left hand side has already been compared to the
     * method declaration of the method being invoked on the right hand side.
     *
     * @param argumentsFromPattern the list of arguments from the JavaPattern invocation
     * @param candidateArguments the list of arguments in the ASTNode we are testing for a match
     *
     * @return true if the arguments are a match
     */
    boolean matchAndCaptureArgumentList(List<Expression> argumentsFromPattern, List<Expression> candidateArguments) {
      for (Iterator<Expression> it1 = argumentsFromPattern.iterator(), it2 = candidateArguments.iterator(); it1.hasNext();) {
        ASTNode n1 = (ASTNode) it1.next();

        if (n1 instanceof SimpleName) {
          final Optional<SingleVariableDeclaration> patternParameterFromSimpleName = findPatternParameterFromSimpleName((SimpleName) n1);
          if (patternParameterFromSimpleName.isPresent() && patternParameterFromSimpleName.get().resolveBinding().getType().isArray()) {
            captureVarargs(n1, it2);
            return true;
          }
        }

        // default behaviour
        ASTNode n2 = (ASTNode) it2.next();
        if (! n1.subtreeMatch(this, n2)) {
          return false;
        }

      }
      return true;
    }


    /**
     * Captures the remaining items from the passed in iterator against the node passed in from the JavaPattern.
     *
     * @param n1 the node in the pattern
     * @param it2 the iterator previously iterated to be at the same point in the argument list as n1.
     */
    private void captureVarargs(ASTNode n1, Iterator<Expression> it2) {
      List<ASTNode> capturedArguments = new ArrayList<>();
      while (it2.hasNext()) {
        capturedArguments.add((ASTNode) it2.next());
      }
      varArgsToCapturedNodes.put(n1.toString(), capturedArguments);
    }


    /**
     *
     * @param o the MethodInvocation to test
     * @return true, if the method invocation matches the declaration of an @Substitute annotated method
     */
    private boolean methodInvocationMatchesSubstituteMethod(MethodInvocation o) {
      return substituteMethods.stream().anyMatch(methodDeclaration ->
              o.resolveMethodBinding().getMethodDeclaration().isEqualTo(methodDeclaration.resolveBinding()));
    }


    /**
     * Entry point for testing whether a JavaPattern matches a matchCandidate ASTNode
     */
    public boolean match(ASTNode javaPattern, Object matchCandidate) {
      astNodeToMatchAgainst = (ASTNode) matchCandidate;
      return javaPattern.subtreeMatch(this, matchCandidate);
    }


    /**
     *
     * @return an ASTNodeMatchInformation containing the node that was matched and all of the captured information.
     */
    ASTNodeMatchInformation getNodeMatch(){
      return new ASTNodeMatchInformation(astNodeToMatchAgainst, substituteMethodToCapturedNode, simpleNameToCapturedNode, simpleTypeToCapturedType, varArgsToCapturedNodes);
    }
  }

}
