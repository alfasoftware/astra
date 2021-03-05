package org.alfasoftware.astra.core.refactoring.operations.annotations;

import java.io.IOException;

import org.alfasoftware.astra.core.matchers.AnnotationMatcher;
import org.alfasoftware.astra.core.utils.ASTOperation;
import org.alfasoftware.astra.core.utils.AstraUtils;
import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;

/**
 * Simple removal of an annotation.
 */
public class RemoveAnnotationRefactor implements ASTOperation {

  private static final Logger log = Logger.getLogger(AnnotationChangeRefactor.class);

  private final AnnotationMatcher annotationToRemove;

  public RemoveAnnotationRefactor(AnnotationMatcher annotationToRemove) {
    this.annotationToRemove = annotationToRemove;
  }

  @Override
  public void run(CompilationUnit compilationUnit, ASTNode node, ASTRewrite rewriter)
      throws IOException, MalformedTreeException, BadLocationException {
    if (node instanceof Annotation) {
      Annotation annotation = (Annotation) node;

      if (annotationToRemove.matches(annotation)) {
        log.info("Removing annotation [" + annotationToRemove + "] "
            + "in [" + AstraUtils.getNameForCompilationUnit(compilationUnit) + "]");

        AstraUtils.removeImport(compilationUnit, annotationToRemove.getFullyQualifiedName(), rewriter);
        rewriter.remove(annotation, null);
      }
    }
  }
}

