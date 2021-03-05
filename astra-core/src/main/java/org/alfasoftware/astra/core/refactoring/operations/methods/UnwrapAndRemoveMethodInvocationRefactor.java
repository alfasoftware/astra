package org.alfasoftware.astra.core.refactoring.operations.methods;

import static org.alfasoftware.astra.core.utils.AstraUtils.getSimpleName;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.alfasoftware.astra.core.matchers.MethodMatcher;
import org.alfasoftware.astra.core.utils.ASTOperation;
import org.alfasoftware.astra.core.utils.AstraUtils;
import org.alfasoftware.astra.core.utils.AstraUtils.MethodInvocationType;
import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;

/**
 * This is intended to find a method being used and then refactor the method and the parent call.
 *
 * e.g.
 * <pre>new MyClass(a, Foo.staticCall(x, y), b)</pre>
 * can become
 * <pre>new MyOtherClass(a, x, y, b)</pre>
 *
 * or
 * <pre>MyClass.frobnicate(a, b, Spline.splinesFor(x, y))</pre>
 * can become
 * <pre>MyClass.frobnicateSpline(a, b, x, y)</pre>
 *
 *
 * For either of the above, this *must* be used with a MethodMatcher that has a parent-context.
 *
 * For removing the method invocation regardless of context, no parent context is required.
 *
 * e.g.
 * <pre>Foo.staticCall(c)</pre>
 * can become
 * <pre>c</pre>
 */
public class UnwrapAndRemoveMethodInvocationRefactor implements ASTOperation {

  private static final Logger log = Logger.getLogger(UnwrapAndRemoveMethodInvocationRefactor.class);

  private final MethodMatcher beforeMatcher;
  private final Optional<String> afterType;
  private final Optional<String> afterMethodName;
  private final int parameterIndex;
  private final boolean justUnwrap;


  private UnwrapAndRemoveMethodInvocationRefactor(MethodMatcher beforeMatcher, Changes changes) {
    super();
    this.beforeMatcher = beforeMatcher;
    this.afterType = changes.afterType;
    this.afterMethodName = changes.afterMethodName;
    this.parameterIndex = changes.index;
    this.justUnwrap = changes.justUnwrap;
  }


  public static NeedsTo from(MethodMatcher fromMethodMatcher) {
    return new NeedsTo(fromMethodMatcher);
  }

  public static class NeedsTo {
    private final MethodMatcher matcher;
    public NeedsTo(MethodMatcher fromMethodMatcher) {
      this.matcher = fromMethodMatcher;
    }
    public UnwrapAndRemoveMethodInvocationRefactor to(Changes changes) {
      return new UnwrapAndRemoveMethodInvocationRefactor(matcher, changes);
    }
  }


  public static class Changes {
    private Optional<String> afterType = Optional.empty();
    private Optional<String> afterMethodName = Optional.empty();
    private int index = 0;
    private boolean justUnwrap = false;

    public static Changes build() {
      return new Changes();
    }

    public Changes toNewParentMethodName(String newMethodName) {
      this.afterMethodName = Optional.of(newMethodName);
      return this;
    }
    public Changes toNewParentType(String fullyQualifiedType) {
      this.afterType = Optional.of(fullyQualifiedType);
      return this;
    }
    public Changes atParameterPosition(int index) {
      this.index = index;
      return this;
    }
    public Changes justRemove() {
      this.justUnwrap = true;
      return this;
    }
  }


  @Override
  public void run(CompilationUnit compilationUnit, ASTNode node, ASTRewrite rewriter) throws IOException, MalformedTreeException, BadLocationException {
		if (node instanceof MethodInvocation) {
			MethodInvocation methodInvocation = (MethodInvocation) node;
			if (beforeMatcher.matches(methodInvocation, compilationUnit)) {
        log.info("Unwrapping method invocation [" + beforeMatcher.getFullyQualifiedDeclaringType() + " " + beforeMatcher.getMethodName() + "] "
            + "to [" + afterType.orElse(AstraUtils.getFullyQualifiedName(methodInvocation, compilationUnit))
            + " " + afterMethodName.orElse(methodInvocation.getName().toString()) + "] "
            + "in [" + AstraUtils.getNameForCompilationUnit(compilationUnit) + "]");

        switchToAfter(compilationUnit, rewriter, methodInvocation);
			}
		}
	}

  private void switchToAfter(CompilationUnit compilationUnit, ASTRewrite rewriter, MethodInvocation methodInvocation) {

    if (justUnwrap) {
      switchSimpleUnwrap(compilationUnit, rewriter, methodInvocation);
    } else if (methodInvocation.getParent() instanceof MethodInvocation) {
      switchToAfterInInvocation(compilationUnit, rewriter, (MethodInvocation)methodInvocation.getParent(), methodInvocation);
    } else if (methodInvocation.getParent() instanceof ClassInstanceCreation) {
      switchToAfterOnConstructor(compilationUnit, rewriter, (ClassInstanceCreation) methodInvocation.getParent());
    }
  }


  /**
   * switches over the type constructed and removes the child invocation moving the arguments used onto the parent call
   */
  private void switchToAfterInInvocation(CompilationUnit compilationUnit, ASTRewrite rewriter, MethodInvocation parent, MethodInvocation child) {

    String beforeTypeName = AstraUtils.getFullyQualifiedName(parent, compilationUnit);
    String beforeMethodName = parent.getName().toString();
    switch (AstraUtils.getMethodInvocationType(parent, compilationUnit, beforeTypeName, beforeMethodName)) {

      case STATIC_METHOD_METHOD_NAME_ONLY :
        AstraUtils.addImport(compilationUnit, afterType.orElse(beforeTypeName), rewriter);
        Name newName = compilationUnit.getAST().newName(AstraUtils.getSimpleName(afterType.orElse(beforeTypeName)));
        rewriter.set(parent, MethodInvocation.EXPRESSION_PROPERTY, newName, null);
        rewriter.set(parent.getName(), SimpleName.IDENTIFIER_PROPERTY, afterMethodName.orElse(beforeMethodName), null);
        break;
      case STATIC_METHOD_FULLY_QUALIFIED_NAME :
        QualifiedName newQualifiedName = compilationUnit.getAST().newQualifiedName(
        compilationUnit.getAST().newName(afterType.orElse(beforeTypeName).replace("."+ AstraUtils.getSimpleName(afterType.orElse(beforeTypeName)), "")),
        compilationUnit.getAST().newSimpleName(AstraUtils.getSimpleName(afterType.orElse(beforeTypeName))));
        rewriter.set(parent, MethodInvocation.EXPRESSION_PROPERTY, newQualifiedName, null);
        rewriter.set(parent.getName(), SimpleName.IDENTIFIER_PROPERTY, afterMethodName.orElse(beforeMethodName), null);
        break;

      case STATIC_METHOD_SIMPLE_NAME :
        AstraUtils.addImport(compilationUnit, afterType.orElse(beforeTypeName), rewriter);
        Name newName2 = compilationUnit.getAST().newName(AstraUtils.getSimpleName(afterType.orElse(beforeTypeName)));
        rewriter.set(parent, MethodInvocation.EXPRESSION_PROPERTY, newName2, null);
        rewriter.set(parent.getName(), SimpleName.IDENTIFIER_PROPERTY, afterMethodName.orElse(beforeMethodName), null);
        break;

      case ON_CLASS_INSTANCE :
        AstraUtils.updateImport(compilationUnit, AstraUtils.getFullyQualifiedName(child, compilationUnit), afterType.orElse(beforeTypeName), rewriter);
        rewriter.set(parent.getName(), SimpleName.IDENTIFIER_PROPERTY, afterMethodName.orElse(beforeMethodName), null);
        break;

      default:
        throw new UnsupportedOperationException("Unknown method invocation type - can't refactor. Why did we say this was a match?");
    }

    ListRewrite methodArguments = rewriter.getListRewrite(parent, MethodInvocation.ARGUMENTS_PROPERTY);
    unWrapChildArgumentsIntoParentUsingIndexPosition(rewriter, methodArguments);
  }


  /**
   * switches over the type constructed and removes the child invocation moving the arguments used onto the parent call
   */
  private void switchToAfterOnConstructor(CompilationUnit compilationUnit, ASTRewrite rewriter, ClassInstanceCreation parent) {

    rewriter.set(parent, ClassInstanceCreation.TYPE_PROPERTY, parent.getAST().newSimpleName(getSimpleName(afterType.orElse(AstraUtils.getFullyQualifiedName(parent)))), null);
    AstraUtils.addImport(compilationUnit, afterType.orElse(AstraUtils.getFullyQualifiedName(parent)), rewriter);

    ListRewrite methodArguments = rewriter.getListRewrite(parent, ClassInstanceCreation.ARGUMENTS_PROPERTY);
    unWrapChildArgumentsIntoParentUsingIndexPosition(rewriter, methodArguments);
  }

  /**
   * For a set of arguments this removes the child invocation at the given _parameterindex_ moving the arguments used into the list,
   * starting at the same position
   */
  private void unWrapChildArgumentsIntoParentUsingIndexPosition(ASTRewrite rewriter, ListRewrite methodArguments) {
    Object originalArgument = methodArguments.getOriginalList().get(parameterIndex);

    if (originalArgument instanceof MethodInvocation) {
      MethodInvocation methodInvocation = (MethodInvocation)originalArgument;
      ListRewrite methodArgumentsToUnwrap = rewriter.getListRewrite(methodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);

      int counter = 0;
      for (Object innerMethodArgument : methodArgumentsToUnwrap.getOriginalList()) {
        ASTNode movedArgument = (ASTNode)innerMethodArgument;
        methodArguments.insertAt(movedArgument, counter+parameterIndex, null);
        counter++;
      }

      methodArguments.remove(methodInvocation, null);
    } else {
      throw new IllegalArgumentException("The parameter found at index [" + parameterIndex + "] is not a MethodInvocation but was :" + originalArgument.getClass());
    }
  }

  private void switchSimpleUnwrap(CompilationUnit compilationUnit, ASTRewrite rewriter, MethodInvocation methodInvocation) {
    MethodInvocationType methodInvocationType = AstraUtils.getMethodInvocationType(
      methodInvocation, compilationUnit, AstraUtils.getFullyQualifiedName(methodInvocation, compilationUnit), methodInvocation.getName().toString());

    switch (methodInvocationType) {

      case ON_CLASS_INSTANCE:
      case STATIC_METHOD_SIMPLE_NAME:
      case STATIC_METHOD_METHOD_NAME_ONLY:
        @SuppressWarnings("unchecked") List<Expression> arguments = methodInvocation.arguments();
        if (arguments.isEmpty()) {
          rewriter.replace(methodInvocation, methodInvocation.getExpression(), null);
          System.out.println("Replaced methodinvocation with expression: " + methodInvocation + ", Expression: " + methodInvocation.getExpression());
        } else {
          rewriter.replace(methodInvocation, arguments.get(0), null);
          System.out.println("Replaced methodinvocation with argument: " + methodInvocation + ", Argument: " + arguments.get(0));
        }
        break;

      default:
        break;
    }
  }
}
