package org.alfasoftware.astra.core.refactoring.operations.types;

import java.io.IOException;
import java.util.List;

import org.alfasoftware.astra.core.utils.ASTOperation;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.dom.rewrite.TargetSourceRangeComputer;
import org.eclipse.jdt.core.dom.rewrite.TargetSourceRangeComputer.SourceRange;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;

/**
 * Adds the {@code final} modifier to classes that have only private constructors,
 * satisfying the Checkstyle FinalClass rule.
 *
 * <p>A class with only private constructors cannot be subclassed and should be declared
 * {@code final}. Classes with no explicit constructors are skipped because they expose an
 * implicit public default constructor. Abstract and already-final classes are also skipped.
 * Enums, interfaces, annotations, and records are skipped because they are not represented
 * by {@link TypeDeclaration} nodes in the AST.
 */
public class MakeFinalClassOperation implements ASTOperation {

  @Override
  public void run(final CompilationUnit compilationUnit, final ASTNode node, final ASTRewrite rewriter)
      throws IOException, MalformedTreeException, BadLocationException {

    if (!(node instanceof TypeDeclaration)) {
      return;
    }
    TypeDeclaration typeDeclaration = (TypeDeclaration) node;

    if (typeDeclaration.isInterface()) {
      return;
    }

    @SuppressWarnings("unchecked")
    List<Object> modifiers = typeDeclaration.modifiers();
    for (Object mod : modifiers) {
      if (mod instanceof Modifier) {
        Modifier modifier = (Modifier) mod;
        if (modifier.isFinal() || modifier.isAbstract()) {
          return;
        }
      }
    }

    // Only classes with at least one explicit constructor are candidates — a class with no
    // explicit constructors has an implicit public default constructor and cannot be final-ised.
    MethodDeclaration[] methods = typeDeclaration.getMethods();
    boolean hasConstructor = false;
    for (MethodDeclaration method : methods) {
      if (method.isConstructor()) {
        hasConstructor = true;
        break;
      }
    }
    if (!hasConstructor) {
      return;
    }

    // All constructors must be private.
    for (MethodDeclaration method : methods) {
      if (!method.isConstructor()) {
        continue;
      }
      boolean isPrivate = false;
      @SuppressWarnings("unchecked")
      List<Object> ctorModifiers = method.modifiers();
      for (Object mod : ctorModifiers) {
        if (mod instanceof Modifier && ((Modifier) mod).isPrivate()) {
          isPrivate = true;
          break;
        }
      }
      if (!isPrivate) {
        return;
      }
    }

    // Determine whether the last element of the modifier list is an annotation rather than a
    // keyword modifier (e.g. @SuppressWarnings immediately before "class Foo").
    boolean lastModifierIsAnnotation = !modifiers.isEmpty()
        && !(modifiers.get(modifiers.size() - 1) instanceof Modifier);

    if (lastModifierIsAnnotation) {
      // ListRewrite.insertLast infers the separator between the new node and the 'class'
      // keyword from the original source text.  Because AstraUtils.makeChangesFromAST enables
      // FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_TYPE, JDT uses '\n' as that separator,
      // producing:
      //
      //   @MyAnnotation
      //   final          ← wrong: 'final' ends up on its own line
      //   class MyClass
      //
      // The correct output is:
      //
      //   @MyAnnotation
      //   final class MyClass
      //
      // Workaround: extend the name node's replacement range backward to cover the preceding
      // "class " text (keyword + one space = 6 chars), then replace that combined span with
      // "final class <Name>".  The newline that separates the annotation from "class" sits
      // just before the extended range and is preserved untouched, so the annotation keeps
      // its own line while 'final class' appears together on the next line.
      //
      // The 6-char assumption ("class ") is valid for standard Java formatting where the
      // 'class' keyword is followed by exactly one space before the type name.
      addFinalViaNameReplacement(typeDeclaration, rewriter);
    } else {
      // No annotation at the tail of the modifier list: insertLast uses a space separator
      // after the previous keyword modifier (or, for an empty list, produces the sole
      // modifier), which correctly places 'final' on the same line as 'class'.
      ListRewrite modifiersRewrite = rewriter.getListRewrite(typeDeclaration, TypeDeclaration.MODIFIERS2_PROPERTY);
      modifiersRewrite.insertLast(typeDeclaration.getAST().newModifier(ModifierKeyword.FINAL_KEYWORD), null);
    }
  }

  /**
   * Inserts {@code final} before the {@code class} keyword by replacing the name node's
   * source range, extended backward to cover {@code "class <Name>"} (6 extra chars).
   * This bypasses the newline-separator issue that arises with {@link ListRewrite#insertLast}
   * when the trailing modifier is an annotation.
   *
   * <p>Each call chains onto whatever {@link TargetSourceRangeComputer} is already installed
   * on the rewriter, so multiple annotation-headed type declarations within the same
   * compilation unit are all handled correctly in a single pass.
   */
  private static void addFinalViaNameReplacement(TypeDeclaration typeDeclaration, ASTRewrite rewriter) {
    SimpleName name = typeDeclaration.getName();
    int nameStart = name.getStartPosition();
    int nameLength = name.getLength();

    // Chain onto any computer already installed (e.g. from a previous TypeDeclaration in
    // the same pass) so earlier extensions are not lost.
    TargetSourceRangeComputer prior = rewriter.getExtendedSourceRangeComputer();
    rewriter.setTargetSourceRangeComputer(new TargetSourceRangeComputer() {
      @Override
      public SourceRange computeSourceRange(ASTNode node) {
        if (node == name) {
          // Extend backward by 6: covers "class " (5-char keyword + 1 space) that
          // immediately precedes the type name in standard-formatted Java source.
          return new SourceRange(nameStart - 6, nameLength + 6);
        }
        return prior.computeSourceRange(node);
      }
    });

    rewriter.replace(
        name,
        rewriter.createStringPlaceholder("final class " + name.getIdentifier(), ASTNode.SIMPLE_NAME),
        null);
  }
}
