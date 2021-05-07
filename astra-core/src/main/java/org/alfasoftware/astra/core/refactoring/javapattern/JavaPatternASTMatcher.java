package org.alfasoftware.astra.core.refactoring.javapattern;

import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


/**
 * This ASTMatcher sub-type has support for matching a Pattern against a single ASTNode
 * For matches found it collects the match information expressed by the Pattern.
 *
 */
public class JavaPatternASTMatcher extends ASTMatcher {

  private final Collection<MethodDeclaration> placeHolderMethods;
  private final Collection<JavaPatternFileParser.SingleASTNodePatternMatcher> javaPatternsToMatch;
  private final Collection<ASTNodeMatchInformation> foundMatches = new ArrayList<>();

  public JavaPatternASTMatcher(Collection<JavaPatternFileParser.SingleASTNodePatternMatcher> javaPatternsToMatch, Collection<MethodDeclaration> placeHolderMethods) {
    this.javaPatternsToMatch = javaPatternsToMatch;
    this.placeHolderMethods = placeHolderMethods;
  }

  boolean matchAndCapture(Object other){
    for (JavaPatternFileParser.SingleASTNodePatternMatcher javaPatternToMatch: javaPatternsToMatch) {
      final JavaPatternMatcher javaPatternMatcher = new JavaPatternMatcher(javaPatternToMatch);
      if(javaPatternMatcher.match(javaPatternToMatch.getJavaPatternToMatch(), other)) {
        foundMatches.add(javaPatternMatcher.getNodeMatch());
      }
    }
    return !foundMatches.isEmpty();
  }

  public Collection<ASTNodeMatchInformation> getFoundMatches() {
    return foundMatches;
  }

  class JavaPatternMatcher extends ASTMatcher {
    private final Map<String, ASTNode> placeHolderMethodToCapturedNode = new HashMap<>();
    private final Map<String, ASTNode> simpleNameToCapturedNode = new HashMap<>();
    private final JavaPatternFileParser.SingleASTNodePatternMatcher patternToMatch;
    private final Map<String, String> simpleTypeToCapturedType = new HashMap<>();
    private ASTNode astNodeToMatchAgainst;

    public JavaPatternMatcher(JavaPatternFileParser.SingleASTNodePatternMatcher patternToMatch) {
      this.patternToMatch = patternToMatch;
    }

    @Override
    public boolean match(SimpleName simpleNameFromPatternMatcher, Object matchCandidate) {
      final Optional<SingleVariableDeclaration> patternParameter = patternToMatch.getSingleVariableDeclarations().stream()
          .filter(singleVariableDeclaration -> singleVariableDeclaration.getName().toString().equals(simpleNameFromPatternMatcher.toString()))
          .findAny();

      if(patternParameter.isPresent() &&
          (isSubTypeCompatible(simpleNameFromPatternMatcher, (Expression) matchCandidate) ||
              resolvedBindingIsEqual(simpleNameFromPatternMatcher, (Expression) matchCandidate) ||
              simpleNameFromPatternMatcher.resolveTypeBinding().isTypeVariable())) { // this should be more specific. Not just a type variable, but needs to match what the captured type is if there is one.
        if(simpleNameFromPatternMatcher.resolveTypeBinding().isParameterizedType()) {
          final ITypeBinding[] matchCandidateTypeParameters = ((Expression) matchCandidate).resolveTypeBinding().getTypeArguments();
          final ITypeBinding[] simpleTypesToMatch = simpleNameFromPatternMatcher.resolveTypeBinding().getTypeArguments();
          for (int i = 0; i < simpleTypesToMatch.length; i++) {
            simpleTypeToCapturedType.put(simpleTypesToMatch[i].getName(), matchCandidateTypeParameters[i].getName());
          }
          simpleNameToCapturedNode.put(simpleNameFromPatternMatcher.toString(), (ASTNode) matchCandidate);
          return true;
        } else {
          simpleNameToCapturedNode.put(simpleNameFromPatternMatcher.toString(), (ASTNode) matchCandidate);
          return true;
        }
      } else {
        return super.match(simpleNameFromPatternMatcher, matchCandidate);
      }
    }

    private boolean resolvedBindingIsEqual(SimpleName simpleNameFromPatternMatcher, Expression matchCandidate) {
      return simpleNameFromPatternMatcher.resolveTypeBinding().getTypeDeclaration()
          .isEqualTo(matchCandidate.resolveTypeBinding().getTypeDeclaration());
    }

    private boolean isSubTypeCompatible(SimpleName simpleNameFromPatternMatcher, Expression matchCandidate) {
      return matchCandidate.resolveTypeBinding().getTypeDeclaration().isSubTypeCompatible(simpleNameFromPatternMatcher.resolveTypeBinding().getTypeDeclaration());
    }

    @Override
    public boolean match(MethodInvocation node, Object other) {
      if(methodInvocationMatchesPlaceHolder(node)) { // TODO investigate whether this handling of methods is adequate, and whether we need similar matches for other methodinvocationlikes, such as InfixExpression
        if (other instanceof MethodInvocation && safeSubtreeListMatch(node.arguments(), ((MethodInvocation) other).arguments())) {
          placeHolderMethodToCapturedNode.put(node.getName().toString(), (ASTNode) other);
        }
        return true;
      } else {
        return super.match(node, other);
      }
    }

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

    private boolean methodInvocationMatchesPlaceHolder(MethodInvocation o) {
      for(MethodDeclaration methodDeclaration : placeHolderMethods){
        if(methodDeclaration.getReturnType2().resolveBinding().isEqualTo(o.resolveMethodBinding().getReturnType())
            && methodDeclaration.resolveBinding().getParameterTypes().length == o.resolveMethodBinding().getMethodDeclaration().getParameterTypes().length) {
          placeHolderMethodToCapturedNode.put(methodDeclaration.getName().toString(), o);
          return true;
        }
      }
      return false;
    }


    public boolean match(ASTNode astNode, Object matchCandidate) {
      astNodeToMatchAgainst = (ASTNode) matchCandidate;
      return astNode.subtreeMatch(this, matchCandidate);
    }

    ASTNodeMatchInformation getNodeMatch(){
      return new ASTNodeMatchInformation(astNodeToMatchAgainst, placeHolderMethodToCapturedNode, simpleNameToCapturedNode, simpleTypeToCapturedType);
    }
  }

}
