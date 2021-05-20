package org.alfasoftware.astra.core.refactoring.methods.constructortobuilder;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.alfasoftware.astra.core.matchers.MethodMatcher;
import org.alfasoftware.astra.core.refactoring.AbstractRefactorTest;
import org.alfasoftware.astra.core.refactoring.methods.constructortobuilder.builder.BuiltType;
import org.alfasoftware.astra.core.refactoring.methods.constructortobuilder.constructor.ConstructorType;
import org.alfasoftware.astra.core.refactoring.operations.javapattern.JavaPatternASTOperation;
import org.alfasoftware.astra.core.refactoring.operations.methods.ConstructorToBuilderRefactor;
import org.alfasoftware.astra.core.refactoring.operations.methods.ConstructorToBuilderRefactor.BuilderSection;
import org.junit.Test;

public class TestConstructorToBuilderRefactor extends AbstractRefactorTest {

  @Test
  public void testConstructorToInnerClassBuilder() {
    assertRefactor(
      ConstructorToBuilderInnerClassBuilderExample.class,
      new HashSet<>(Arrays.asList(
          new ConstructorToBuilderRefactor(
              MethodMatcher.builder()
                .withMethodName(BuiltType.class.getSimpleName())
                .withFullyQualifiedDeclaringType(BuiltType.class.getName())
                .withFullyQualifiedParameters(
                    Arrays.asList(
                        "java.lang.Object",
                        "long",
                        "java.lang.String"))
                .build(),
                Arrays.asList(
                  new BuilderSection().withLiteral("BuiltType.builderFor("),
                  new BuilderSection().withParameterFromIndex(0),
                  new BuilderSection().withLiteral(").withTwoAndThree("),
                  new BuilderSection().withParameterFromIndex(1),
                  new BuilderSection().withLiteral(", "),
                  new BuilderSection().withParameterFromIndex(2),
                  new BuilderSection().withLiteral(")"))),
          new ConstructorToBuilderRefactor(
              MethodMatcher.builder()
                .withMethodName(BuiltType.class.getSimpleName())
                .withFullyQualifiedDeclaringType(BuiltType.class.getName())
                .isVarargs(true)
                .withFullyQualifiedParameters(
                    Arrays.asList(
                        "java.lang.String",
                        "java.lang.Object[]"))
                .build(),
                Arrays.asList(
                  new BuilderSection().withLiteral("BuiltType.builderForKey("),
                  new BuilderSection().withParameterFromIndex(0),
                  new BuilderSection().withLiteral(").withValues("),
                  new BuilderSection().withParameterFromIndex(1).isFirstVararg(),
                  new BuilderSection().withLiteral(").build()"))),
          new ConstructorToBuilderRefactor(
              MethodMatcher.builder()
                .withMethodName(BuiltType.class.getSimpleName())
                .withFullyQualifiedDeclaringType(BuiltType.class.getName())
                .isVarargs(true)
                .withFullyQualifiedParameters(
                    Arrays.asList(
                        "java.lang.String",
                        "java.lang.String",
                        "java.lang.Object[]"))
                .build(),
                Arrays.asList(
                  new BuilderSection().withLiteral("BuiltType.builderForKey("),
                  new BuilderSection().withParameterFromIndex(0),
                  new BuilderSection().withLiteral(").withValues("),
                  new BuilderSection().withParameterFromIndex(1).isFirstVararg(),
                  new BuilderSection().withLiteral(").build()")
                )
              )
          )));
  }

  /**
   * This variant ensures that where the builder used is not an inner class of the existing type,
   * an import can be added for the new one.
   */
  @Test
  public void testConstructorToInnerClassBuilderMatcher() throws IOException {
    assertRefactor(
        ConstructorToBuilderInnerClassBuilderExample.class,
        Collections.singleton(
            new JavaPatternASTOperation(
                new File(TEST_EXAMPLES + "/" + ConstructorToBuilderInnerClassBuilderExampleMatcher2.class.getName().replaceAll("\\.", "/") + ".java")
            )
        )
    );
  }


  /**
   * This variant ensures that where the builder used is not an inner class of the existing type,
   * an import can be added for the new one.
   */
  @Test
  public void testConstructorToBuilderWithExternalBuilder() {
    assertRefactor(
      ConstructorToBuilderExternalBuilderExample.class,
      new HashSet<>(Arrays.asList(
          new ConstructorToBuilderRefactor(
              MethodMatcher.builder()
                .withMethodName(ConstructorType.class.getSimpleName())
                .withFullyQualifiedDeclaringType(ConstructorType.class.getName())
                .withFullyQualifiedParameters(
                    Arrays.asList(
                        "java.lang.Object",
                        "long",
                        "java.lang.String"))
                .build(),
                Arrays.asList(
                  new BuilderSection().withLiteral("BuiltType.builderFor("),
                  new BuilderSection().withParameterFromIndex(0),
                  new BuilderSection().withLiteral(").withTwoAndThree("),
                  new BuilderSection().withParameterFromIndex(1),
                  new BuilderSection().withLiteral(", "),
                  new BuilderSection().withParameterFromIndex(2),
                  new BuilderSection().withLiteral(")")))
          .withNewImport(BuiltType.class.getName()),
          new ConstructorToBuilderRefactor(
              MethodMatcher.builder()
                .withMethodName(ConstructorType.class.getSimpleName())
                .withFullyQualifiedDeclaringType(ConstructorType.class.getName())
                .isVarargs(true)
                .withFullyQualifiedParameters(
                    Arrays.asList(
                        "java.lang.String",
                        "java.lang.Object[]"))
                .build(),
                Arrays.asList(
                  new BuilderSection().withLiteral("BuiltType.builderForKey("),
                  new BuilderSection().withParameterFromIndex(0),
                  new BuilderSection().withLiteral(").withValues("),
                  new BuilderSection().withParameterFromIndex(1).isFirstVararg(),
                  new BuilderSection().withLiteral(").build()")))
          .withNewImport(BuiltType.class.getName()),
          new ConstructorToBuilderRefactor(
              MethodMatcher.builder()
                .withMethodName(ConstructorType.class.getSimpleName())
                .withFullyQualifiedDeclaringType(ConstructorType.class.getName())
                .isVarargs(true)
                .withFullyQualifiedParameters(
                    Arrays.asList(
                        "java.lang.String",
                        "java.lang.String",
                        "java.lang.Object[]"))
                .build(),
                Arrays.asList(
                  new BuilderSection().withLiteral("BuiltType.builderForKey("),
                  new BuilderSection().withParameterFromIndex(0),
                  new BuilderSection().withLiteral(").withValues("),
                  new BuilderSection().withParameterFromIndex(1).isFirstVararg(),
                  new BuilderSection().withLiteral(").build()")
                )
              )
          .withNewImport(BuiltType.class.getName())
          )));
  }
}
