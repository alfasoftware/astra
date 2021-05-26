package org.alfasoftware.astra.core.refactoring.operations.javapattern;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;



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
      if(javaPatternMatcher.match(javaPatternToMatch.getJavaPatternToMatch(), matchCandidate)) {
        foundMatches.add(javaPatternMatcher.getNodeMatch());
      }
    }
    return !foundMatches.isEmpty();
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
    private final Map<String, String> simpleTypeToCapturedType = new HashMap<>();
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

      if(patternParameter.isPresent() &&
          (isAssignmentCompatible(simpleNameFromPatternMatcher, (Expression) matchCandidate) ||
              isSubTypeCompatible(simpleNameFromPatternMatcher, (Expression) matchCandidate) ||
              typeOfSimpleNameIsEqual(simpleNameFromPatternMatcher, (Expression) matchCandidate) ||
              simpleNameFromPatternMatcher.resolveTypeBinding().isTypeVariable())) {
        // we may need to resolve Type variables defined in the JavaPattern
        if(simpleNameFromPatternMatcher.resolveTypeBinding().isParameterizedType()) {
          final ITypeBinding[] matchCandidateTypeParameters = ((Expression) matchCandidate).resolveTypeBinding().getTypeArguments();
          final ITypeBinding[] simpleTypesToMatch = simpleNameFromPatternMatcher.resolveTypeBinding().getTypeArguments();
          if(matchCandidateTypeParameters.length != simpleTypesToMatch.length){
            return false;
          }
          for (int i = 0; i < simpleTypesToMatch.length; i++) {
            if(weAlreadyHaveACapturedTypeForThisSimpleTypeWhichIsDifferent(matchCandidateTypeParameters[i], simpleTypesToMatch[i])){
              return false;
            }
            simpleTypeToCapturedType.put(simpleTypesToMatch[i].getName(), matchCandidateTypeParameters[i].getName());
          }
        }
        return putSimpleNameAndCapturedNode(simpleNameFromPatternMatcher, (ASTNode) matchCandidate);
      } else if (patternParameter.isPresent()) {
        return false;
      } else if (simpleNameFromPatternMatcher.getParent() instanceof SingleVariableDeclaration
      || simpleNameFromPatternMatcher.getLocationInParent().getId().equals("expression")) {
        // don't care about it if it's the name of a variable only
        return true;
      } else {
        return super.match(simpleNameFromPatternMatcher, matchCandidate); // the names given to variables in the pattern don't matter.
      }
    }


    private Optional<SingleVariableDeclaration> findPatternParameterFromSimpleName(SimpleName simpleNameFromPatternMatcher) {
      return patternToMatch.getSingleVariableDeclarations().stream()
          .filter(singleVariableDeclaration -> singleVariableDeclaration.getName().toString().equals(simpleNameFromPatternMatcher.toString()))
          .findAny();
    }

    private boolean putSimpleNameAndCapturedNode(SimpleName simpleNameFromPatternMatcher, ASTNode matchCandidate) {
      if(simpleNameToCapturedNode.get(simpleNameFromPatternMatcher.toString()) != null &&
      !simpleNameToCapturedNode.get(simpleNameFromPatternMatcher.toString()).subtreeMatch(new ASTMatcher(), matchCandidate)){
        return false;
      } else {
        simpleNameToCapturedNode.put(simpleNameFromPatternMatcher.toString(), matchCandidate);
        return true;
      }
    }

    private boolean weAlreadyHaveACapturedTypeForThisSimpleTypeWhichIsDifferent(ITypeBinding matchCandidateTypeParameter, ITypeBinding simpleTypesToMatch) {
      return simpleTypeToCapturedType.get(simpleTypesToMatch.getName()) != null
          && !simpleTypeToCapturedType.get(simpleTypesToMatch.getName()).equals(matchCandidateTypeParameter.getName());
    }

    private boolean typeOfSimpleNameIsEqual(SimpleName simpleNameFromPatternMatcher, Expression matchCandidate) {
      if(matchCandidate.resolveTypeBinding() == null) {
        return false;
      }
      return simpleNameFromPatternMatcher.resolveTypeBinding().getTypeDeclaration()
          .isEqualTo(matchCandidate.resolveTypeBinding().getTypeDeclaration());
    }

    private boolean isAssignmentCompatible(SimpleName simpleNameFromPatternMatcher, Expression matchCandidate) {
      if(matchCandidate.resolveTypeBinding() == null) {
        // log warning that we were unable to resolve a type binding here.
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
      if(matchCandidate.resolveTypeBinding() == null) {
        return false;
      }
      return matchCandidate.resolveTypeBinding().getTypeDeclaration().isSubTypeCompatible(simpleNameFromPatternMatcher.resolveTypeBinding().getTypeDeclaration());
    }

    /**
     * Overridden matcher for MethodInvocation.
     * Tests whether a MethodInvocation in the {@link JavaPattern} matches a given ASTNode.
     * If the MethodInvocation from the {@link JavaPattern} is an invocation of a {@link Substitute} annotated method,
     * verify that the matchCandidate is appropriate for the substitute and capture it.
     * If the MethodInvocation is not from a {@link Substitute} annotated method, delegate to the default matching.
     *
     * Additionally has handling for varargs parameters in the JavaPattern
     */
    @Override
    public boolean match(MethodInvocation methodInvocationFromJavaPattern, Object matchCandidate) {
      if(methodInvocationMatchesSubstituteMethod(methodInvocationFromJavaPattern)) { // TODO investigate whether this handling of methods is adequate, and whether we need similar matches for other methodinvocationlikes, such as InfixExpression
        if (matchCandidate instanceof MethodInvocation &&
            returnTypeMatches(methodInvocationFromJavaPattern, (MethodInvocation) matchCandidate) &&
            safeSubtreeListMatch(methodInvocationFromJavaPattern.arguments(), ((MethodInvocation) matchCandidate).arguments())) {
          return putSubstituteNameAndCapturedNode(methodInvocationFromJavaPattern,  (ASTNode) matchCandidate);
        }
        return true; // this is probably not quite right. should still check the type of the matchcandidate
      } else {
        if (!(matchCandidate instanceof MethodInvocation)) {
          return false;
        }
        MethodInvocation o = (MethodInvocation) matchCandidate;

        if (!safeSubtreeListMatch(methodInvocationFromJavaPattern.typeArguments(), o.typeArguments())) {
          return false;
        }

        if(!(
            safeSubtreeMatch(methodInvocationFromJavaPattern.getExpression(), o.getExpression())
                && safeSubtreeMatch(methodInvocationFromJavaPattern.getName(), o.getName()))){
          return false;
        }

        // check whether the number of parameters for the method matches the number of parameters in the method declaration
        // for the method we are comparing to, to be able to compare varargs parameters.
        if(methodInvocationFromJavaPattern.resolveMethodBinding().getMethodDeclaration().getParameterTypes().length
            != o.resolveMethodBinding().getMethodDeclaration().getParameterTypes().length) {
          return false;
        }
        if(!matchAndCaptureArgumentList(methodInvocationFromJavaPattern.arguments(), o.arguments())){
          return false;
        }

        return true;
      }
    }

    private boolean returnTypeMatches(MethodInvocation methodInvocationFromJavaPattern, MethodInvocation matchCandidate) {
      final ITypeBinding returnType = methodInvocationFromJavaPattern.resolveMethodBinding().getReturnType();
      if(returnType.isTypeVariable() && simpleTypeToCapturedType.get(returnType.getName()) != null) {
        return simpleTypeToCapturedType.get(returnType.getName()).equals(matchCandidate.resolveMethodBinding().getReturnType().getName());
      } else if (returnType.isTypeVariable() && simpleTypeToCapturedType.get(returnType.getName()) == null){
        simpleTypeToCapturedType.put(returnType.getName(), matchCandidate.resolveMethodBinding().getReturnType().getName());
        return true;
      } else {
        return returnType.isEqualTo(matchCandidate.resolveMethodBinding().getReturnType());
      }
    }

    private boolean putSubstituteNameAndCapturedNode(MethodInvocation methodInvocationFromJavaPattern, ASTNode matchCandidate) {
      if(substituteMethodToCapturedNode.get(methodInvocationFromJavaPattern.toString()) != null &&
          !substituteMethodToCapturedNode.get(methodInvocationFromJavaPattern.toString()).subtreeMatch(new ASTMatcher(), matchCandidate)){
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
      if(other instanceof SimpleName) {
        final IBinding iBinding = ((SimpleName) other).resolveBinding();
        if(iBinding.isEqualTo(node.resolveBinding())){
          return true;
        }
      }
      if (!(other instanceof QualifiedName)) {
        return false;
      }
      QualifiedName o = (QualifiedName) other;
      return safeSubtreeMatch(node.getQualifier(), o.getQualifier())
          && safeSubtreeMatch(node.getName(), o.getName());
    }

    @Override
    public boolean match(SimpleType node, Object other) {
      if (!(other instanceof SimpleType)) {
        return false;
      }
      SimpleType o = (SimpleType) other;

      if(node.resolveBinding().isTypeVariable() &&
          weAlreadyHaveACapturedTypeForThisSimpleTypeWhichIsDifferent(o.resolveBinding(), node.resolveBinding())) {
        return false;
      } else if (node.resolveBinding().isTypeVariable()) {
        simpleTypeToCapturedType.put(node.resolveBinding().getName(), o.resolveBinding().getName());
        return true;
      } else {
        return super.match(node, other);
      }
    }




    /**
     * Overridden matcher for ClassInstanceCreation to handle varargs specified in the JavaPattern.
     *
     */
    public boolean match(ClassInstanceCreation node, Object other) {
      if (!(other instanceof ClassInstanceCreation)) {
        return false;
      }
      ClassInstanceCreation o = (ClassInstanceCreation) other;
      if (!safeSubtreeListMatch(node.typeArguments(), o.typeArguments())) {
        return false;
      }
      if (!safeSubtreeMatch(node.getType(), o.getType())) {
        return false;
      }

      if(node.arguments().size()
          != o.resolveConstructorBinding().getParameterTypes().length) {
        return false;
      }

      if(!matchAndCaptureArgumentList(node.arguments(), o.arguments())){
        return false;
      };

      return
          safeSubtreeMatch(node.getExpression(), o.getExpression())
              && safeSubtreeMatch(
              node.getAnonymousClassDeclaration(),
              o.getAnonymousClassDeclaration());
    }

    boolean matchAndCaptureArgumentList(List argumentsFromPattern, List candidateArguments){
      int size1 = argumentsFromPattern.size();
      int size2 = candidateArguments.size();

      if (size1 != size2
          && !lastPatternArgumentIsVarargs(argumentsFromPattern)) {
        return false;
      }
      for (Iterator it1 = argumentsFromPattern.iterator(), it2 = candidateArguments.iterator(); it1.hasNext();) {
        ASTNode n1 = (ASTNode) it1.next();

        if(n1 instanceof SimpleName) {
          final Optional<SingleVariableDeclaration> patternParameterFromSimpleName = findPatternParameterFromSimpleName((SimpleName) n1);
          if(patternParameterFromSimpleName.isPresent() && patternParameterFromSimpleName.get().resolveBinding().getType().isArray()){
            captureVarargs(it2, n1);
            return true;
          } else {
            ASTNode n2 = (ASTNode) it2.next();
            if (!n1.subtreeMatch(this, n2)) {
              return false;
            }
          }
        } else {
          // default behaviour
          ASTNode n2 = (ASTNode) it2.next();
          if (!n1.subtreeMatch(this, n2)) {
            return false;
          }
        }
      }
      return true;
    }

    private void captureVarargs(Iterator it2, ASTNode n1) {
      List<ASTNode> capturedArguments = new ArrayList<>();
      while(it2.hasNext()) {
        capturedArguments.add((ASTNode) it2.next());
      }
      varArgsToCapturedNodes.put(n1.toString(), capturedArguments);
    }

    private boolean lastPatternArgumentIsVarargs(List argumentsFromPattern) {
      if (argumentsFromPattern.size() ==0) {
        return false;
      }
      Object lastPatternArgument = argumentsFromPattern.get(argumentsFromPattern.size()-1);
      return lastPatternArgument instanceof SimpleName &&
          findPatternParameterFromSimpleName((SimpleName) lastPatternArgument).isPresent()
          && findPatternParameterFromSimpleName((SimpleName) lastPatternArgument).get().resolveBinding().getType().isArray();
    }

    /**
     *
     * @param o the methodinvocation to test
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

    ASTNodeMatchInformation getNodeMatch(){
      return new ASTNodeMatchInformation(astNodeToMatchAgainst, substituteMethodToCapturedNode, simpleNameToCapturedNode, simpleTypeToCapturedType, varArgsToCapturedNodes);
    }
  }

}
