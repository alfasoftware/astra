package org.alfasoftware.astra.core.refactoring.operations.methods;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.alfasoftware.astra.core.matchers.MethodMatcher;
import org.alfasoftware.astra.core.utils.ASTOperation;
import org.alfasoftware.astra.core.utils.AstraUtils;
import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;

/**
 * Refactoring operation to replace constructor invocations with a builder.
 *
 * The builder to switch to is specified in a series of BuilderSections.
 *
 * For example -
 * <pre>new Foo("First", 2, "Third")</pre>
 * can be switched to
 * <pre>Foo.fooBuilderFor("First").withSecondAndThirdParam(2, "Third").build()</pre>
 *
 * This operation would be specified as follows:
 * <pre>
 * new ConstructorToBuilderRefactor(
 *   MethodMatcher.builder()
 *     .withMethodName("Foo")
 *     .withFullyQualifiedDeclaringType("com.example.Foo")
 *     .withFullyQualifiedParameters(
 *       Lists.newArrayList(
 *         "java.lang.String",
 *         "int",
 *         "java.lang.String"))
 *    .build(),
 *    Lists.newArrayList(
 *      new BuilderSection().withLiteral("Foo.fooBuilderFor("),
 *      new BuilderSection().withParameterFromIndex(0),
 *      new BuilderSection().withLiteral(").withSecondParam("),
 *      new BuilderSection().withParameterFromIndex(1),
 *      new BuilderSection().withLiteral(", "),
 *      new BuilderSection().withParameterFromIndex(2),
 *      new BuilderSection().withLiteral(").build()")
 *    )
 *  )
 * </pre>
 */
@SuppressWarnings("unchecked")
public class ConstructorToBuilderRefactor implements ASTOperation {

  private static final Logger log = Logger.getLogger(ConstructorToBuilderRefactor.class);

  private final MethodMatcher methodMatcher;
  private final List<BuilderSection> builderParts;
  private Optional<String> newImport = Optional.empty();

  /**
   * @param methodMatcher Constructor to match
   * @param builderParts List of parts to use when constructing the builder call. Can be set with:
   *  a string literal describing the method calls
   *  or,
   *  an index of the existing parameter list to insert
   *  isFirstVararg - the index of the first vararg parameter. If included, all parameters from this index onward
   *  will be inserted in a comma-separated string
   *
   * Example builderParts:
   * "BuiltType.builderFor(", 0, ").withTwoAndThree(", 1, ", ", 2, ")";
   *
   * with varargs:
   * "BuiltType.builderFor(", 0, ").withValues(", new BuilderSection(1), ")";
   */
  public ConstructorToBuilderRefactor(MethodMatcher methodMatcher, List<BuilderSection> builderParts) {
    this.methodMatcher = methodMatcher;
    this.builderParts = builderParts;
  }


  /**
   * Used to add a new import, if one is needed.
   * This might be used if the builder is not an inner class of the existing type.
   */
  public ConstructorToBuilderRefactor withNewImport(String newImport) {
    this.newImport = Optional.of(newImport);
    return this;
  }


  @Override
  public void run(CompilationUnit compilationUnit, ASTNode node, ASTRewrite rewriter)
      throws IOException, MalformedTreeException, BadLocationException {
    if (node instanceof ClassInstanceCreation) {
      ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation) node;
      if (methodMatcher.matches(classInstanceCreation)) {

        log.info("Refactoring constructor [" + methodMatcher.getFullyQualifiedDeclaringType() + "] "
            + "to builder in [" + AstraUtils.getNameForCompilationUnit(compilationUnit) + "]");

        StringLiteral literal = node.getAST().newStringLiteral();
        String builder = "";
        for (BuilderSection part : builderParts) {
          builder += part.getStringValue(classInstanceCreation);
        }

        rewriter.set(literal, StringLiteral.ESCAPED_VALUE_PROPERTY, builder, null);
        rewriter.replace(node, literal, null);

        newImport.ifPresent(newImport -> {
          AstraUtils.addImport(compilationUnit, newImport, rewriter);
        });
      }
    }
  }

  public static class BuilderSection {
    private String literal;
    private int index;
    private boolean isFirstVararg;
    private Function<Object, Object> transform;
    public BuilderSection withLiteral(String literal) {
      this.literal = literal;
      return this;
    }
    public BuilderSection withParameterFromIndex(int index) {
      this.index = index;
      return this;
    }
    public BuilderSection withTransform(Function<Object, Object> transform) {
      this.transform = transform;
      return this;
    }
    public BuilderSection isFirstVararg() {
      this.isFirstVararg = true;
      return this;
    }
    public String getStringValue(ClassInstanceCreation classInstanceCreation) {
      if (literal != null) {
        return literal;
      } else if (! isFirstVararg) {
        return getParamValue(classInstanceCreation.arguments().get(index)).toString();
      } else {
        List<String> args = (List<String>) classInstanceCreation.arguments()
          .subList(index, classInstanceCreation.arguments().size())
          .stream()
          .map(a -> a.toString())
          .collect(Collectors.toList());
        return String.join(", ", args);
      }
    }
    public Object getParamValue(Object param) {
      if (transform == null) {
        return param;
      } else {
        return transform.apply(param);
      }
    }
  }
}
