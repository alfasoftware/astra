package org.alfasoftware.astra.core.refactoring.operations.types;

import java.io.IOException;
import java.util.List;

import org.alfasoftware.astra.core.utils.ASTOperation;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
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

    ListRewrite modifiersRewrite = rewriter.getListRewrite(typeDeclaration, TypeDeclaration.MODIFIERS2_PROPERTY);
    modifiersRewrite.insertLast(typeDeclaration.getAST().newModifier(ModifierKeyword.FINAL_KEYWORD), null);
  }
}
