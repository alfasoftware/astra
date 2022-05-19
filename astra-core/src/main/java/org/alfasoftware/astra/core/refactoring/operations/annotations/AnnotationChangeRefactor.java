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
    private Map<String, String> membersAndValuesToAdd;


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

    public AnnotationChangeRefactor build() {
      return new AnnotationChangeRefactor(fromType, toType, membersAndValuesToAdd);
    }

    public Builder removeMemberNameValuePairs(Map<String, String> membersAndValuesToRemove) {
      // TODO Auto-generated method stub
      return null;
    }

    public Builder updateMemberName(Map<String, String> currentToNewName) {
      // TODO Auto-generated method stub
      return null;
    }

    public Builder removeMembersWithNames(Set<String> namesForMembersToRemove) {
      // TODO Auto-generated method stub
      return null;
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

        AstraUtils.updateImport(compilationUnit, before.getFullyQualifiedName(), after, rewriter);

        if (! AstraUtils.getSimpleName(after).equals(AstraUtils.getSimpleName(annotation.getTypeName().getFullyQualifiedName()))) {
          rewriteAnnotation(rewriter, annotation);
        }
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
    if (annotation instanceof NormalAnnotation) {
      rewriter.set(annotation, NormalAnnotation.TYPE_NAME_PROPERTY, name, null);
      final ListRewrite listRewrite = rewriter.getListRewrite(annotation, NormalAnnotation.VALUES_PROPERTY);
      @SuppressWarnings("unchecked")
      List<MemberValuePair> currentList = listRewrite.getRewrittenList();
      // clear down existing list
      currentList.forEach(i -> listRewrite.remove(i, null));

      newMembersAndValues.forEach((memberName, value) -> {
        MemberValuePair newMemberAndValue = annotation.getAST().newMemberValuePair();
        newMemberAndValue.setName(annotation.getAST().newSimpleName(memberName));
        final StringLiteral valueLiteral = annotation.getAST().newStringLiteral();
        valueLiteral.setLiteralValue(value);
        newMemberAndValue.setValue(valueLiteral);
        listRewrite.insertLast(newMemberAndValue, null);
      });


    } else if (annotation instanceof MarkerAnnotation) {
      rewriter.set(annotation, MarkerAnnotation.TYPE_NAME_PROPERTY, name, null);
    } else if (annotation instanceof SingleMemberAnnotation) {
      rewriter.set(annotation, SingleMemberAnnotation.TYPE_NAME_PROPERTY, name, null);

    }

  }
}
