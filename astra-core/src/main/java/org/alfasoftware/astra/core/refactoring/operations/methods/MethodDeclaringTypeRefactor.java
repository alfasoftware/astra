package org.alfasoftware.astra.core.refactoring.operations.methods;

import static org.alfasoftware.astra.core.utils.AstraUtils.getSimpleName;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.alfasoftware.astra.core.matchers.MethodMatcher;
import org.alfasoftware.astra.core.utils.ASTOperation;
import org.alfasoftware.astra.core.utils.AstraUtils;
import org.alfasoftware.astra.core.utils.ClassVisitor;
import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;

/**
 * This refactor identifies a method call for a given original-type
 * and changes all instances of the original-type to a new type.
 *
 * e.g.
 * <pre>
 * import oldPackage.OldFoo;
 *
 * public(OldFoo oldfoo) {
 *   oldfoo.targetMethod();
 * }</pre>
 * becomes
 * <pre>
 * import newPackage.NewFoo;
 *
 * public(NewFoo oldfoo) {
 *   oldfoo.targetMethod();
 * }</pre>
 */
public class MethodDeclaringTypeRefactor implements ASTOperation {

  private static final Logger log = Logger.getLogger(MethodDeclaringTypeRefactor.class);

  private final String toType;
  @SuppressWarnings("unused")
  private final Set<String> methods = new HashSet<>();

  private final MethodMatcher methodMatcher;

  private MethodDeclaringTypeRefactor(MethodMatcher methodMatcher, String toType) {
    this.methodMatcher = methodMatcher;
    this.toType = toType;
  }

  public static NeedsTo forMethod(MethodMatcher methodMatcher) {
    return new NeedsTo(methodMatcher);
  }

  public static class NeedsTo {
    private final MethodMatcher methodMatcher;

    private NeedsTo(MethodMatcher methodMatcher) {
      this.methodMatcher = methodMatcher;
    }

    public MethodDeclaringTypeRefactor toType(String toType) {
      return new MethodDeclaringTypeRefactor(methodMatcher, toType);
    }
  }


  @Override
  public void run(CompilationUnit compilationUnit, ASTNode node, ASTRewrite rewriter)
      throws IOException, MalformedTreeException, BadLocationException {
    if (node instanceof TypeDeclaration) {
      ClassVisitor visitor = new ClassVisitor();
      node.accept(visitor);
      for (MethodInvocation method : visitor.getMethodInvocations()) {
        if (methodMatcher.matches(method, compilationUnit)) {

          // Then we have a match
          // This is a pretty blunt tool. It's saying "look for all the times we used the old type name, and change them to the new"
          for (SimpleType simpleType : visitor.getSimpleTypes()) {
            if (Optional.of(simpleType)
              .map(SimpleType::resolveBinding)
              .map(ITypeBinding::getBinaryName)
              .filter(n -> ! methodMatcher.getFullyQualifiedDeclaringType().isPresent() || methodMatcher.getFullyQualifiedDeclaringType().get().test(n))
              .isPresent()) {

              log.info("Refactoring method declaration type of "
                  + "method [" + method.getName().toString() + "] "
                  + "from [" + getSimpleName(AstraUtils.getFullyQualifiedName(method, compilationUnit)) + "] "
                  + "to [" + getSimpleName(toType) + "] "
                  + "in [" + AstraUtils.getNameForCompilationUnit(compilationUnit) + "]");
              rewriter.set(simpleType, SimpleType.NAME_PROPERTY, node.getAST().newSimpleName(getSimpleName(toType)), null);

              AstraUtils.addImport(compilationUnit, toType, rewriter);
            }
          }
        }
      }
    }
  }
}
