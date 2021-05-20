package org.alfasoftware.astra.core.refactoring.operations.methods;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.alfasoftware.astra.core.matchers.MethodMatcher;
import org.alfasoftware.astra.core.utils.ASTOperation;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;

/**
 * Similar to a method invocation refactor, this swaps two chained method invocations to a different method.
 *
 * For example,
 * <pre>getCurrentFoo().doFooThing()</pre>
 * can be swapped to
 * <pre>doBarThing()</pre>
 *
 * In that case, the chain to refactor is:
 * <pre>["org.example.ThingProvider getCurrentFoo", "org.example.Foo doFooThing"]</pre>
 * and the method name should be swapped to:
 * <pre>["doBarThing"]</pre>
 */
public class ChainedMethodInvocationRefactor implements ASTOperation {

  private List<MethodMatcher> before = new ArrayList<>();
  private List<String> after = new ArrayList<>();

  public ChainedMethodInvocationRefactor(List<MethodMatcher> before, List<String> after) {
    this.before = before;
    this.after = after;
  }

  @Override
  public void run(CompilationUnit compilationUnit, ASTNode node, ASTRewrite rewriter)
      throws IOException, MalformedTreeException, BadLocationException {
    if (node instanceof MethodInvocation) {
      handleMethodInvocation((MethodInvocation) node, rewriter);
    }
  }


  private void handleMethodInvocation(MethodInvocation node, ASTRewrite rewriter) {
    // second
    if (before.get(before.size() - 1).getMethodName().filter(name -> name.test(node.getName().toString())).isPresent()) {
      // wrappedA.get().first()
      if (node.getExpression() != null && node.getExpression() instanceof MethodInvocation) {
        // first
        MethodInvocation nextMethodInvocation = (MethodInvocation) node.getExpression();
        if (before.get(before.size() - 2).getMethodName().filter(name -> name.test(nextMethodInvocation.getName().toString())).isPresent()) {
          // TODO make this looped, so we can handle chains of arbitrary length

          // change the chain to the "after"
          // TODO handle arbitrary "after" lengths
          rewriter.set(node, MethodInvocation.EXPRESSION_PROPERTY, nextMethodInvocation.getExpression(), null);
          rewriter.set(node, MethodInvocation.NAME_PROPERTY, node.getAST().newSimpleName(after.get(0)), null);
        }
      }
    }
  }
}

