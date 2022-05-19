package org.alfasoftware.astra.core.refactoring.operations.annotations;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

  private final AnnotationMatcher fromType;
  private final String toType;
  private final Map<String, String> membersAndValuesToAdd;
  private final Set<String> namesForMembersToRemove;
  private final Map<String, String> memberNameUpdates;
  private final Map<String, String> memberNamesToUpdateWithNewValues;
  private final Optional<Transform> transform;

  public AnnotationChangeRefactor(AnnotationMatcher fromType, String toType, Map<String, String> membersAndValuesToAdd, Set<String> namesForMembersToRemove, Map<String, String> memberNameUpdates, Map<String, String> memberNamesToUpdateWithNewValues, Optional<Transform> transform) {
    this.fromType = fromType;
    this.toType = toType;
    this.membersAndValuesToAdd = membersAndValuesToAdd;
    this.namesForMembersToRemove = namesForMembersToRemove;
    this.memberNameUpdates = memberNameUpdates;
    this.memberNamesToUpdateWithNewValues = memberNamesToUpdateWithNewValues;
    this.transform = transform;
  }


  @Override
  public String toString() {
    return String.format("AnnotationChangeRefactor from [%s] to [%s]", fromType, toType);
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
    private Optional<Transform> transform = Optional.empty();
    private Map<String, String> memberNamesToUpdateWithNewValues = Map.of();

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
      this.memberNamesToUpdateWithNewValues = updateNamesToNewValues;
      return this;
    }

    public Builder withTransform(Transform transform) {
      this.transform = Optional.of(transform);
      return this;
    }

    public AnnotationChangeRefactor build() {
      return new AnnotationChangeRefactor(fromType,
              toType,
              membersAndValuesToAdd,
              namesForMembersToRemove,
              memberNameUpdates,
              memberNamesToUpdateWithNewValues,
              transform);
    }
  }


  @FunctionalInterface
  public interface Transform {
    public void apply(CompilationUnit compilationUnit, Annotation annotation, ASTRewrite rewriter);
  }


  @Override
  public void run(CompilationUnit compilationUnit, ASTNode node, ASTRewrite rewriter)
      throws IOException, MalformedTreeException, BadLocationException {
    if (node instanceof Annotation) {
      Annotation annotation = (Annotation) node;

      if (shouldRefactor(annotation)) {
        log.info("Refactoring annotation [" + fromType + "] "
            + "to [" + toType + "] "
            + "in [" + AstraUtils.getNameForCompilationUnit(compilationUnit) + "]");

        if (! AstraUtils.getSimpleName(toType).equals(AstraUtils.getSimpleName(annotation.getTypeName().getFullyQualifiedName()))) {
          AstraUtils.updateImport(compilationUnit, fromType.getFullyQualifiedName(), toType, rewriter);
        }

        rewriteAnnotation(compilationUnit, annotation, rewriter);
      }
    }
  }

  private boolean shouldRefactor(Annotation annotation) {
    return fromType.matches(annotation);
  }

  /**
   * We have to re-assign the annotation after each transformation, rather than reusing the initial one,
   * as we might replace the ASTNode representing the annotation as part of a transformation
   */
  private void rewriteAnnotation(CompilationUnit compilationUnit, Annotation annotation, ASTRewrite rewriter) {
    // change name of annotation
    changeAnnotationName(rewriter, annotation);

    // add new members
    annotation = addNewMembersToAnnotation(rewriter, annotation);

    annotation = removeMembersFromAnnotation(rewriter, annotation);

    annotation = updateMemberNames(rewriter, annotation);

    annotation = applyTransformation(compilationUnit, rewriter, annotation);
  }

  private void changeAnnotationName(ASTRewrite rewriter, Annotation annotation) {
    Name name;
    if (annotation.getTypeName().isQualifiedName()) {
      name = annotation.getAST().newName(toType);
    } else {
      name = annotation.getAST().newSimpleName(AstraUtils.getSimpleName(toType));
    }
    if(annotation.isSingleMemberAnnotation()) {
      rewriter.set(annotation, SingleMemberAnnotation.TYPE_NAME_PROPERTY, name, null);
    } else if(annotation.isMarkerAnnotation()){
      rewriter.set(annotation, MarkerAnnotation.TYPE_NAME_PROPERTY, name, null);
    } else if (annotation.isNormalAnnotation()){
      rewriter.set(annotation, NormalAnnotation.TYPE_NAME_PROPERTY, name, null);
    }
  }

  private Annotation addNewMembersToAnnotation(ASTRewrite rewriter, Annotation annotation) {
    if(!membersAndValuesToAdd.isEmpty()) {
      final NormalAnnotation normalAnnotation;
      if (annotation.isMarkerAnnotation() || annotation.isSingleMemberAnnotation()) {
        normalAnnotation = convertAnnotationToNormalAnnotation(rewriter, annotation);
      } else {
        normalAnnotation = (NormalAnnotation) annotation;
      }
      addMembersToNormalAnnotation(rewriter, normalAnnotation, membersAndValuesToAdd);
      return normalAnnotation;
    }
    return annotation;
  }

  private Annotation removeMembersFromAnnotation(ASTRewrite rewriter, Annotation annotation) {
    if(!namesForMembersToRemove.isEmpty()){
      if(annotation.isSingleMemberAnnotation() && namesForMembersToRemove.contains("value")){
        return convertAnnotationToMarkerAnnotation(rewriter, annotation);
      } else if(annotation.isNormalAnnotation()) {
        NormalAnnotation normalAnnotation = (NormalAnnotation) annotation;
        final ListRewrite listRewrite = rewriter.getListRewrite(normalAnnotation, NormalAnnotation.VALUES_PROPERTY);
        final List<MemberValuePair> rewrittenList = listRewrite.getRewrittenList();
        for (MemberValuePair memberValuePair : rewrittenList) {
          if(namesForMembersToRemove.contains(memberValuePair.getName().getIdentifier())){
            listRewrite.remove(memberValuePair, null);
          }
        }
        if(listRewrite.getRewrittenList().size() == 0) {
          return convertAnnotationToMarkerAnnotation(rewriter, annotation);
        } else {
          return normalAnnotation;
        }
      }
      if(annotation.isMarkerAnnotation()){
        log.warn("TODO");
      }
    }
    return annotation;
  }

  private Annotation updateMemberNames(ASTRewrite rewriter, Annotation annotation) {
    if(!memberNameUpdates.isEmpty() ) {
      if(annotation.isSingleMemberAnnotation()) {
        final String newMemberName = memberNameUpdates.get("value");
        if(newMemberName != null) {
          NormalAnnotation normalAnnotation = rewriter.getAST().newNormalAnnotation();
          rewriter.set(normalAnnotation, NormalAnnotation.TYPE_NAME_PROPERTY, annotation.getTypeName(), null);
          final MemberValuePair memberValuePair = rewriter.getAST().newMemberValuePair();
          rewriter.set(memberValuePair, MemberValuePair.VALUE_PROPERTY, ((SingleMemberAnnotation) annotation).getValue(), null);
          memberValuePair.setName(rewriter.getAST().newSimpleName(newMemberName));

          rewriter.getListRewrite(normalAnnotation, NormalAnnotation.VALUES_PROPERTY).insertLast(memberValuePair,  null);
          rewriter.replace(annotation, normalAnnotation, null);
        }
      } else if (annotation.isNormalAnnotation()) {
        NormalAnnotation normalAnnotation = (NormalAnnotation) annotation;
        final ListRewrite listRewrite = rewriter.getListRewrite(normalAnnotation, NormalAnnotation.VALUES_PROPERTY);
        final List<MemberValuePair> rewrittenList = listRewrite.getRewrittenList();
        for (MemberValuePair memberValuePair : rewrittenList) {
          final String newName = memberNameUpdates.get(memberValuePair.getName().getIdentifier());
          if(newName != null) {
            rewriter.set(memberValuePair, MemberValuePair.NAME_PROPERTY, rewriter.getAST().newSimpleName(newName), null);
          }
        }
      }
    }
    return annotation;
  }

  private Annotation applyTransformation(CompilationUnit compilationUnit, ASTRewrite rewriter, Annotation annotation) {
    transform.ifPresent(t -> t.apply(compilationUnit, annotation, rewriter));
    return annotation;
  }

  private NormalAnnotation convertAnnotationToNormalAnnotation(ASTRewrite rewriter, Annotation annotation) {
    final NormalAnnotation normalAnnotation;
    normalAnnotation = rewriter.getAST().newNormalAnnotation();
    rewriter.set(normalAnnotation, NormalAnnotation.TYPE_NAME_PROPERTY, annotation.getTypeName(), null);
    rewriter.replace(annotation, normalAnnotation, null);
    if(annotation.isSingleMemberAnnotation()){
      final MemberValuePair existingMemberValuePair = rewriter.getAST().newMemberValuePair();
      rewriter.set(existingMemberValuePair, MemberValuePair.VALUE_PROPERTY, ((SingleMemberAnnotation) annotation).getValue(), null);
      existingMemberValuePair.setName(rewriter.getAST().newSimpleName("value"));
      rewriter.getListRewrite(normalAnnotation, NormalAnnotation.VALUES_PROPERTY).insertLast(existingMemberValuePair, null);
    }
    return normalAnnotation;
  }


  private MarkerAnnotation convertAnnotationToMarkerAnnotation(ASTRewrite rewriter, Annotation annotation) {
    final MarkerAnnotation markerAnnotation = rewriter.getAST().newMarkerAnnotation();
    rewriter.set(markerAnnotation, MarkerAnnotation.TYPE_NAME_PROPERTY, annotation.getTypeName(), null);
    rewriter.replace(annotation, markerAnnotation, null);
    return markerAnnotation;
  }


  private void addMembersToNormalAnnotation(ASTRewrite rewriter, NormalAnnotation normalAnnotation, Map<String, String> membersAndValuesToAdd) {
    final ListRewrite listRewrite = rewriter.getListRewrite(normalAnnotation, NormalAnnotation.VALUES_PROPERTY);
    membersAndValuesToAdd.forEach((memberName, value) -> {
      MemberValuePair newMemberAndValue = rewriter.getAST().newMemberValuePair();
      newMemberAndValue.setName(rewriter.getAST().newSimpleName(memberName));
      final StringLiteral valueLiteral = rewriter.getAST().newStringLiteral();
      valueLiteral.setLiteralValue(value);
      newMemberAndValue.setValue(valueLiteral);
      listRewrite.insertLast(newMemberAndValue, null);
    });
  }


}
