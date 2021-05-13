package org.alfasoftware.astra.core.refactoring.operations.javadoc;

import java.io.IOException;
import java.util.Objects;

import org.alfasoftware.astra.core.utils.ASTOperation;
import org.alfasoftware.astra.core.utils.AstraUtils;
import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;

/**
 * Simple swap of a Javadoc tag, like <pre>@link</pre> to <pre>@linkplain</pre>.
 * Input should include the @ symbol.
 * Example inputs include TagElement.TAG_LINK and TagElement.TAG_LINKPLAIN.
 */
public class JavadocTagRefactor implements ASTOperation {

  private static final Logger log = Logger.getLogger(JavadocTagRefactor.class);

  private final String fromTag;
  private final String toTag;
  private JavadocTagRefactor(String fromTag, String toTag) {
    this.fromTag = fromTag;
    this.toTag = toTag;
  }

  public static NeedsTo fromTag(String fromTag) {
    return new NeedsTo(fromTag);
  }

  public static class NeedsTo {
    private final String fromTag;
    private NeedsTo(String fromTag) {
      this.fromTag = fromTag;
    }

    public JavadocTagRefactor toTag(String toTag) {
      return new JavadocTagRefactor(fromTag, toTag);
    }
  }


  @Override
  public void run(CompilationUnit compilationUnit, ASTNode node, ASTRewrite rewriter)
      throws IOException, MalformedTreeException, BadLocationException {
    if (node instanceof TagElement) {
      TagElement tagElement = (TagElement) node;
      if (Objects.equals(tagElement.getTagName(), fromTag)) {
        log.info("Refactoring javadoc tag [" + fromTag + "] to [" + toTag + "] in [" + AstraUtils.getNameForCompilationUnit(compilationUnit) + "]");

        if (tagElement.isNested()) {
          rewriter.set(tagElement, TagElement.TAG_NAME_PROPERTY, "{" + toTag, null);
        } else {
          rewriter.set(tagElement, TagElement.TAG_NAME_PROPERTY, toTag, null);
        }
      }
    }
  }
}
