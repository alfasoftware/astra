package org.alfasoftware.astra.core.refactoring.operations.annotations;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;

import org.alfasoftware.astra.core.utils.ASTOperation;
import org.alfasoftware.astra.core.utils.AstraUtils;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;

/**
 * Operation to add an annotation to an ASTNode.
 *
 * The annotation could be a marker annotation:
 *
 * @MarkerExample
 * class Foo {
 *
 * ...or a single member annotation:
 *
 * @SingleMemberExample("member")
 * class Foo {
 *
 * Additional imports (such as for the annotation or a member) can also be specified.
 */
public class AddAnnotationRefactor implements ASTOperation {

  private final Predicate<ASTNode> nodeToAnnotatePredicate;
  private Optional<String[]> additionalImports;
  private String annotationName;
  private Optional<String> member;

  // Use Builder instead
  private AddAnnotationRefactor(Builder builder) {
    super();
    this.nodeToAnnotatePredicate = builder.nodeToAnnotatePredicate;
    this.additionalImports = builder.additionalImports;
    this.annotationName = builder.annotationName;
    this.member = builder.member;
  }


  /**
   * @return a new instance to build an AddAnnotationRefactor.
   */
  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Predicate<ASTNode> nodeToAnnotatePredicate = t -> true;
    private Optional<String[]> additionalImports = Optional.empty();
    private String annotationName;
    private Optional<String> member = Optional.empty();

    /**
     * Don't construct this directly - use the static method.
     */
    private Builder() {
      super();
    }

    public AddAnnotationRefactor build() {
      return new AddAnnotationRefactor(this);
    }

    public Builder withNodeToAnnotate(Predicate<ASTNode> nodeToAnnotatePredicate) {
      this.nodeToAnnotatePredicate = nodeToAnnotatePredicate;
      return this;
    }

    public Builder withAnnotationName(String annotationName) {
      this.annotationName = annotationName;
      return this;
    }

    public Builder withOptionalAnnotationMember(String member) {
      this.member = Optional.of(member);
      return this;
    }

    public Builder withAdditionalImports(String... imports) {
      this.additionalImports = Optional.of(imports);
      return this;
    }
  }


  @Override
  public void run(CompilationUnit compilationUnit, ASTNode node, ASTRewrite rewriter)
      throws IOException, MalformedTreeException, BadLocationException {
      if (nodeToAnnotatePredicate.test(node)) {
        ChildListPropertyDescriptor modifiersProperty;
        if (node instanceof BodyDeclaration) {
          BodyDeclaration bodyDeclaration = (BodyDeclaration) node;
          modifiersProperty = bodyDeclaration.getModifiersProperty();
        } else {
          throw new UnsupportedOperationException("Node type not yet supported for adding annotations: " + node);
        }

        Annotation annotation = null;
        if (member.isPresent()) {
          annotation = AstraUtils.buildSingleMemberAnnotation(node, annotationName, member.get(), rewriter);
        } else {
          annotation = AstraUtils.buildMarkerAnnotation(node, annotationName);
        }
        AstraUtils.addAnnotationToNode(node, annotation, rewriter, modifiersProperty);

        additionalImports.ifPresent(imports -> Arrays.asList(imports).forEach(i ->
          AstraUtils.addImport(compilationUnit, i, rewriter)
        ));
      }
  }
}

