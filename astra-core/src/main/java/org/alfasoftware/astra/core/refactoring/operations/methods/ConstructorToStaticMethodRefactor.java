package org.alfasoftware.astra.core.refactoring.operations.methods;

import java.io.IOException;
import java.util.List;

import org.alfasoftware.astra.core.matchers.MethodMatcher;
import org.alfasoftware.astra.core.utils.ASTOperation;
import org.alfasoftware.astra.core.utils.AstraUtils;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;

/**
 *  Refactoring operation to swap an object instantiation from a constructor call to a static method.
 *
 *  For example:
 *  <pre>
 *    import java.util.Date;
 *
 *    Object date = new Date();
 *  </pre>
 *
 *  Could become:
 *  <pre>
 *    import java.time.LocalDate;
 *
 *    Object date = LocalDate.now();
 *  </pre>
 */
public class ConstructorToStaticMethodRefactor implements ASTOperation {

  private final MethodMatcher fromConstructor;
  private final String toClass;
  private final String toFunction;

  public ConstructorToStaticMethodRefactor(MethodMatcher fromConstructor, String toStaticFunctionFQName, String toStaticFunctionName) {
    this.fromConstructor = fromConstructor;
    this.toClass = toStaticFunctionFQName;
    this.toFunction = toStaticFunctionName;
  }

  @Override
  public void run(CompilationUnit compilationUnit, ASTNode node, ASTRewrite rewriter)
      throws IOException, MalformedTreeException, BadLocationException {
    if (node instanceof ClassInstanceCreation) {
      ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation) node;
      if (fromConstructor.matches(classInstanceCreation)) {
        MethodInvocation methodInvocation = node.getAST().newMethodInvocation();
        rewriter.set(methodInvocation, MethodInvocation.EXPRESSION_PROPERTY, compilationUnit.getAST().newSimpleName(AstraUtils.getSimpleName(toClass)), null);
        rewriter.set(methodInvocation, MethodInvocation.NAME_PROPERTY, compilationUnit.getAST().newSimpleName(toFunction), null);

        // Simple copying of arguments from the constructor to the static method
        if (! fromConstructor.getFullyQualifiedParameterNames().filter(List::isEmpty).isPresent()) {
          ListRewrite argumentsList = rewriter.getListRewrite(methodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
          for (Object object : classInstanceCreation.arguments()) {
            argumentsList.insertLast((Expression) object, null);
          }
        }

        rewriter.replace(node, methodInvocation, null);
        AstraUtils.addImport(compilationUnit, toClass, rewriter);
      }
    }
  }
}
