package org.alfasoftware.astra.core.refactoring.operations.annotations;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfasoftware.astra.core.matchers.AnnotationMatcher;
import org.alfasoftware.astra.core.utils.ASTOperation;
import org.alfasoftware.astra.core.utils.AstraUtils;
import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;

/**
 * Simple swap of an annotation, eg,
 * <pre>
 * com.google.inject.Inject -&gt; javax.inject.Inject.
 * <pre>
 */
public class AnnotationChangeRefactor implements ASTOperation {

  private static final Logger log = Logger.getLogger(AnnotationChangeRefactor.class);

  private final AnnotationMatcher before;
  private final String after;
  private Map<String, String> newMembersAndValues;

  private AnnotationChangeRefactor(AnnotationMatcher before, String after, Map<String, String> newMembersAndValues) {
    this.before = before;
    this.after = after;
    this.newMembersAndValues = newMembersAndValues;
  }

  @Override
  public String toString() {
    return String.format("AnnotationChangeRefactor from [%s] to [%s]", before, after);
  }

  /**
   * @return a new builder for this refactor
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder class for the refactor.
   */
  public static class Builder {
    private AnnotationMatcher fromType;
    private String toType;
    private Map<String, String> membersAndValuesToAdd = Map.of();
    private Set<String> namesForMembersToRemove = Set.of();
    private Map<String, String> memberNameUpdates = Map.of();

    private Builder() {
      super();
    }

    public Builder from(AnnotationMatcher fromType) {
      this.fromType = fromType;
      return this;
    }

    public Builder to(String toType) {
      // Removing $ from inner class names as this won't match with resolved type binding names
      this.toType = toType.replaceAll("\\$", ".");
      return this;
    }

    public Builder addMemberNameValuePairs(Map<String, String> membersAndValuesToAdd) {
      this.membersAndValuesToAdd = membersAndValuesToAdd;
      return this;
    }

    public Builder updateMemberName(Map<String, String> currentToNewName) {
      this.memberNameUpdates = currentToNewName;
      return this;
    }

    public Builder removeMembersWithNames(Set<String> namesForMembersToRemove) {
      this.namesForMembersToRemove = namesForMembersToRemove;
      return this;
    }

    public Builder updateMembersWithNameToValue(Map<String, String> updateNamesToNewValues) {
      // TODO Auto-generated method stub
      return null;
    }

    public AnnotationChangeRefactor build() {
      return new AnnotationChangeRefactor(fromType, toType, membersAndValuesToAdd);
    }
  }

  @Override
  public void run(CompilationUnit compilationUnit, ASTNode node, ASTRewrite rewriter)
      throws IOException, MalformedTreeException, BadLocationException {
    if (node instanceof Annotation) {
      Annotation annotation = (Annotation) node;

      if (shouldRefactor(annotation)) {
        log.info("Refactoring annotation [" + before + "] "
            + "to [" + after + "] "
            + "in [" + AstraUtils.getNameForCompilationUnit(compilationUnit) + "]");

        if (! AstraUtils.getSimpleName(after).equals(AstraUtils.getSimpleName(annotation.getTypeName().getFullyQualifiedName()))) {
          AstraUtils.updateImport(compilationUnit, before.getFullyQualifiedName(), after, rewriter);
        }

        rewriteAnnotation(rewriter, annotation);
      }
    }
  }

  private boolean shouldRefactor(Annotation annotation) {
    return before.matches(annotation);
  }

  private void rewriteAnnotation(ASTRewrite rewriter, Annotation annotation) {
    Name name = null;
    if (annotation.getTypeName().isQualifiedName()) {
      name = annotation.getAST().newName(after);
    } else {
      name = annotation.getAST().newSimpleName(AstraUtils.getSimpleName(after));
    }

    // add new members
//    addNewMembersToAnnotation(rewriter, annotation);

    if (annotation instanceof NormalAnnotation) {
      rewriteNormalAnnotation(rewriter, name, (NormalAnnotation) annotation);

    } else if (annotation instanceof MarkerAnnotation) {
      if(!newMembersAndValues.isEmpty()){
        // Need to change MarkerAnnotation to a NormalAnnotation if we are adding new members.
        final NormalAnnotation normalAnnotation = rewriter.getAST().newNormalAnnotation();
        rewriteNormalAnnotation(rewriter, name, normalAnnotation);
        rewriter.replace(annotation, normalAnnotation, null);
      } else {
        rewriter.set(annotation, MarkerAnnotation.TYPE_NAME_PROPERTY, name, null);
      }

    } else if (annotation instanceof SingleMemberAnnotation) {
      if(!newMembersAndValues.isEmpty()){
        // Need to change MarkerAnnotation to a NormalAnnotation if we are adding new members.
        final NormalAnnotation normalAnnotation = rewriter.getAST().newNormalAnnotation();

        final MemberValuePair memberValuePair = annotation.getAST().newMemberValuePair();

        memberValuePair.setName(annotation.getAST().newSimpleName("value"));
        rewriter.set(memberValuePair, MemberValuePair.VALUE_PROPERTY, ((SingleMemberAnnotation) annotation).getValue(), null);

        final ListRewrite listRewrite = rewriter.getListRewrite(normalAnnotation, NormalAnnotation.VALUES_PROPERTY);
        listRewrite.insertLast(memberValuePair, null);

        rewriteNormalAnnotation(rewriter, name, normalAnnotation);
        rewriter.replace(annotation, normalAnnotation, null);
      } else {
        rewriter.set(annotation, SingleMemberAnnotation.TYPE_NAME_PROPERTY, name, null);
      }
    }

  }

  private void rewriteNormalAnnotation(ASTRewrite rewriter, Name name, NormalAnnotation normalAnnotation) {
    rewriter.set(normalAnnotation, NormalAnnotation.TYPE_NAME_PROPERTY, name, null);
    final ListRewrite listRewrite = rewriter.getListRewrite(normalAnnotation, NormalAnnotation.VALUES_PROPERTY);
    newMembersAndValues.forEach((memberName, value) -> {
      MemberValuePair newMemberAndValue = rewriter.getAST().newMemberValuePair();
      newMemberAndValue.setName(rewriter.getAST().newSimpleName(memberName));
      final StringLiteral valueLiteral = rewriter.getAST().newStringLiteral();
      valueLiteral.setLiteralValue(value);
      newMemberAndValue.setValue(valueLiteral);
      listRewrite.insertLast(newMemberAndValue, null);
    });
  }
}
