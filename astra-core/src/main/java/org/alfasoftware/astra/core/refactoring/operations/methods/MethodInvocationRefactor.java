package org.alfasoftware.astra.core.refactoring.operations.methods;

import static org.alfasoftware.astra.core.utils.AstraUtils.addImport;
import static org.alfasoftware.astra.core.utils.AstraUtils.updateImport;

import java.io.IOException;
import java.util.Optional;

import org.alfasoftware.astra.core.matchers.MethodMatcher;
import org.alfasoftware.astra.core.utils.ASTOperation;
import org.alfasoftware.astra.core.utils.AstraUtils;
import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;

/**
 * Finds a method invocation, and applies some kind of transform.
 * Transforms may include:
 * <ul>
 *  <li>changing the name of the invoked method<ul>a.<b>foo</b>() -&gt; a.<b>bar</b>()</ul></li>
 *  <li>changing the type on which the method is invoked<ul><b>a</b>.foo() -&gt; <b>b</b>.foo()</ul></li>
 *  <li>changing the parameters with which the method is invoked<ul>a.foo(<b>x</b>) -&gt; a.foo(<b>x, y</b>)</ul></li>
 * </ul>
 * Where a type is changed, imports will be handled. For static method invocations,
 * the original invocation type will be preserved.
 * <ul>
 *  <li><b>A.foo()</b> would become <b>B.bar()</b>, with the import updated</li>
 *  <li><b>com.x.y.A.foo()</b> would become <b>com.x.y.z.B.bar()</b></li>
 *  <li>Statically imported methods would be updated</li>
 * </ul>
 */
public class MethodInvocationRefactor implements ASTOperation {

  private static final Logger log = Logger.getLogger(MethodInvocationRefactor.class);

  private final MethodMatcher beforeMatcher;
  private final Optional<String> afterType;
  private final Optional<String> afterMethodName;
  private final Optional<Parameter> parameter;
  private final boolean doInlineStaticImport;
  private final Optional<InvocationTransform> transform;

  private MethodInvocationRefactor(MethodMatcher beforeMatcher, Changes changes) {
    super();
    this.beforeMatcher = beforeMatcher;
    this.afterType = changes.afterType;
    this.afterMethodName = changes.afterMethodName;
    this.parameter = changes.parameter;
    this.doInlineStaticImport = changes.doInlineStaticImport;
    this.transform = changes.transform;
  }

  public static NeedsTo from(MethodMatcher fromMethodMatcher) {
    return new NeedsTo(fromMethodMatcher);
  }

  public static class NeedsTo {
    private final MethodMatcher matcher;

    public NeedsTo(MethodMatcher fromMethodMatcher) {
      this.matcher = fromMethodMatcher;
    }

    public MethodInvocationRefactor to(Changes changes) {
      return new MethodInvocationRefactor(matcher, changes);
    }
  }

  public static class Changes {
    private Optional<String> afterType = Optional.empty();
    private Optional<String> afterMethodName = Optional.empty();
    private Optional<Parameter> parameter = Optional.empty();
    private boolean doInlineStaticImport = false;
    private Optional<InvocationTransform> transform = Optional.empty();

    public Changes toNewMethodName(String newMethodName) {
      this.afterMethodName = Optional.of(newMethodName);
      return this;
    }

    public Changes toNewType(String fullyQualifiedType) {
      this.afterType = Optional.of(fullyQualifiedType);
      return this;
    }

    public Changes withNewParameter(Object parameterLiteral, Position position) {
      Parameter value = new Parameter();
      value.parameterLiteral = parameterLiteral;
      value.position = position;
      this.parameter = Optional.of(value);
      return this;
    }

    public Changes withStaticImportInlined() {
      this.doInlineStaticImport = true;
      return this;
    }

    public Changes withInvocationTransform(InvocationTransform transform) {
      this.transform = Optional.of(transform);
      return this;
    }
  }

  @FunctionalInterface
  public interface InvocationTransform {
    public void apply(CompilationUnit compilationUnit, MethodInvocation methodInvocation, ASTRewrite rewriter);
  }

  static class Parameter {
    Object parameterLiteral;
    Position position;
    int supplied;
  }

  public enum Position {
    FIRST,
    LAST,
    SUPPLIED;
  }

  public MethodMatcher getBeforeMatcher() {
    return beforeMatcher;
  }


  @Override
  public void run(CompilationUnit compilationUnit, ASTNode node, ASTRewrite rewriter) throws IOException, MalformedTreeException, BadLocationException {

    if (node instanceof MethodInvocation) {
      MethodInvocation methodInvocation = (MethodInvocation) node;

      if (beforeMatcher.matches(methodInvocation, compilationUnit)) {
        log.info("Refactoring method invocation [" +
            AstraUtils.getFullyQualifiedName(methodInvocation, compilationUnit) + " " + methodInvocation.getName().toString() + "] "
            + "to [" + afterType.orElse(AstraUtils.getFullyQualifiedName(methodInvocation, compilationUnit)) + " " +
            afterMethodName.orElse(methodInvocation.getName().toString()) + "] "
            + "in [" + AstraUtils.getNameForCompilationUnit(compilationUnit) + "]");

        switchToAfter(compilationUnit, rewriter, methodInvocation);
      }
    }
  }


  private void switchToAfter(CompilationUnit compilationUnit, ASTRewrite rewriter, MethodInvocation methodInvocation) {

    String afterTypeFQ = afterType.orElse(AstraUtils.getFullyQualifiedName(methodInvocation, compilationUnit));
    String afterTypeSimple = AstraUtils.getSimpleName(afterTypeFQ);
    String methodName = afterMethodName.orElse(methodInvocation.getName().toString());

    switch (AstraUtils.getMethodInvocationType(
      methodInvocation, compilationUnit, AstraUtils.getFullyQualifiedName(methodInvocation, compilationUnit), methodInvocation.getName().toString())) {
        case STATIC_METHOD_METHOD_NAME_ONLY:
          if (doInlineStaticImport) {
            addImport(compilationUnit, afterTypeFQ, rewriter);
            Name newName = compilationUnit.getAST().newName(afterTypeSimple);
            rewriter.set(methodInvocation, MethodInvocation.EXPRESSION_PROPERTY, newName, null);
            rewriter.set(methodInvocation.getName(), SimpleName.IDENTIFIER_PROPERTY, methodName, null);
          } else {
            updateImport(compilationUnit,
              AstraUtils.getFullyQualifiedName(methodInvocation, compilationUnit) + "." + methodInvocation.getName().toString(),
              afterTypeFQ + "." + methodName, rewriter);
            rewriter.set(methodInvocation.getName(), SimpleName.IDENTIFIER_PROPERTY, methodName, null);
          }
          break;

        case STATIC_METHOD_FULLY_QUALIFIED_NAME:
          QualifiedName newQualifiedName = compilationUnit.getAST().newQualifiedName(
            compilationUnit.getAST().newName(afterTypeFQ.replace("." + afterTypeSimple, "")),
            compilationUnit.getAST().newSimpleName(afterTypeSimple));
          rewriter.set(methodInvocation, MethodInvocation.EXPRESSION_PROPERTY, newQualifiedName, null);
          rewriter.set(methodInvocation.getName(), SimpleName.IDENTIFIER_PROPERTY, methodName, null);
          break;

        case STATIC_METHOD_SIMPLE_NAME:
          addImport(compilationUnit, afterTypeFQ, rewriter);
          Name newName = compilationUnit.getAST().newName(afterTypeSimple);
          rewriter.set(methodInvocation, MethodInvocation.EXPRESSION_PROPERTY, newName, null);
          rewriter.set(methodInvocation.getName(), SimpleName.IDENTIFIER_PROPERTY, methodName, null);
          break;

        case ON_CLASS_INSTANCE:
          addImport(compilationUnit, afterTypeFQ, rewriter);
          rewriter.set(methodInvocation.getName(), SimpleName.IDENTIFIER_PROPERTY, methodName, null);
          break;

        default:
          throw new UnsupportedOperationException(
              "Unknown method invocation type - can't refactor. Why did we say this was a match?");
    }

    // If a parameter change is required
    if (parameter.isPresent()) {
      ListRewrite methodArguments = rewriter.getListRewrite(methodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);

      ASTNode newArgument = null;
      if (parameter.get().parameterLiteral instanceof String) {
        newArgument = methodInvocation.getAST().newStringLiteral();
        rewriter.set(newArgument, StringLiteral.ESCAPED_VALUE_PROPERTY, parameter.get().parameterLiteral, null);
      } else {
        // Unfortunately have to handle each argument type individually.
        // Hopefully once primitives and general object types are covered, this will be less of a pain.
        throw new IllegalArgumentException("Unhandled argument type: " + parameter.get().parameterLiteral.getClass());
      }

      switch (parameter.get().position) {
        case FIRST:
          methodArguments.insertFirst(newArgument, null);
          break;
        case LAST:
          methodArguments.insertLast(newArgument, null);
          break;
        case SUPPLIED:
          methodArguments.insertAt(newArgument, parameter.get().supplied, null);
          break;
        default:
          break;
      }
    }

    if (transform.isPresent()) {
      transform.get().apply(compilationUnit, methodInvocation, rewriter);
    }
  }
}
