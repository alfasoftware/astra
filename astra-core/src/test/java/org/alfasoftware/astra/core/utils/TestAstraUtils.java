package org.alfasoftware.astra.core.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.alfasoftware.astra.core.refactoring.UseCase;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.junit.Test;

/**
 * Tests for the general purpose utilities provided in {@link AstraUtils}.
 */
public class TestAstraUtils {


  /**
   * Tests for a method which returns the Java simple name identifier from a qualified name.
   * <a href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-3.html#jls-3.8"> The JLS section 3.8</a> defines an identifier as
   * "an unlimited-length sequence of Java letters and Java digits, the first of which must be a Java letter."
   * "The 'Java letters' include uppercase and lowercase ASCII Latin letters A-Z (\u0041-\u005a), and a-z (\u0061-\u007a), and,
   * for historical reasons, the ASCII dollar sign ($, or \u0024) and underscore (_, or \u005f).
   * The dollar sign should be used only in mechanically generated source code or, rarely, to access pre-existing names on
   * legacy systems. The underscore may be used in identifiers formed of two or more characters, but it cannot be used as a
   * one-character identifier due to being a keyword.
   * The 'Java digits' include the ASCII digits 0-9 (\u0030-\u0039)."
   */
  @Test
  public void testGetSimpleName() {

    // Primitive
    assertEquals("boolean", AstraUtils.getSimpleName("boolean"));

    // Class name already simple name
    assertEquals("Foo", AstraUtils.getSimpleName("Foo"));

    // Fully qualified type names
    assertEquals("Foo", AstraUtils.getSimpleName("com.Foo"));
    assertEquals("Bar", AstraUtils.getSimpleName("com.foo.Bar"));

    // Method name qualified with class
    assertEquals("methodName", AstraUtils.getSimpleName("com.Foo.methodName"));

    // Inner class
    assertEquals("InnerFoo", AstraUtils.getSimpleName("com.Foo$InnerFoo"));

    // Static member with underscore
    assertEquals("100_000", AstraUtils.getSimpleName("com.Foo.100_000"));

    // Static method named with one non-alphanumeric character
    assertEquals("$", AstraUtils.getSimpleName("com.Foo.$"));
    assertEquals("_", AstraUtils.getSimpleName("com.Foo._"));

    // Invalid name
    assertEquals("", AstraUtils.getSimpleName("com.Foo.."));
  }


  @Test
  public void testGetNameForAnonymousClassDeclaration() throws IOException {
    // Given
    CompilationUnit compilationUnit = new TestingSourceParser(ExampleWithAnonymousClassDeclaration.class).buildCompilationUnit();

    // When
    List<AnonymousClassDeclaration> anonymousClassDeclarations = new ArrayList<>();
    ASTVisitor visitor = new ASTVisitor() {
      @Override
      public boolean visit(AnonymousClassDeclaration node) {
        anonymousClassDeclarations.add(node);
        return super.visit(node);
      }
    };
    compilationUnit.accept(visitor);

    // Then
    assertEquals(UseCase.class.getName(), AstraUtils.getName(anonymousClassDeclarations.get(0)));
    assertEquals(ASTOperation.class.getName(), AstraUtils.getName(anonymousClassDeclarations.get(1)));
  }


  @Test
  public void testGetPackageName() {
    // Given
    String source = "package org.alfasoftware.astra.core.utils; public class AstraUtils";
		List<TypeDeclaration> typeDeclarations = new TestingSourceParser(source)
      .getClassVisitor()
      .getTypeDeclarations();

    // When
    PackageDeclaration packageDeclaration = AstraUtils.getPackageName(typeDeclarations.get(0));

    // Then
    assertEquals("org.alfasoftware.astra.core.utils", packageDeclaration.getName().getFullyQualifiedName());
  }


  @Test
  public void testGetPackageNameWithNoPackageDeclaration() {
    assertNull(getPackageDeclarationFromSource("public class AstraUtils {}"));
    assertNull(getPackageDeclarationFromSource("public interface Test {}"));
  }


  @Test
  public void testGetFullyQualifiedNameOfEnumInEnum() {
    // Given
    String source = "package com.foo;\n"
        + "\n"
        + "public enum Foo {\n"
        + "  ONE,\n"
        + "  TWO;\n"
        + "\n"
        + "  static class Bar {\n"
        + "  }\n"
        + "}";
    List<AbstractTypeDeclaration> typeDeclarations = new TestingSourceParser(source)
      .getClassVisitor()
      .getAbstractTypeDeclarations();

    // When
    String fullyQualifiedName = AstraUtils.getFullyQualifiedName(typeDeclarations.get(1));

    // Then
    assertEquals("com.foo.Foo.Bar", fullyQualifiedName);
  }


  private PackageDeclaration getPackageDeclarationFromSource(String source) {
    List<TypeDeclaration> typeDeclarations = new TestingSourceParser(source)
      .getClassVisitor()
      .getTypeDeclarations();

    return AstraUtils.getPackageName(typeDeclarations.get(0));
  }


  /**
   * Used to parse test source files for use in unit tests.
   */
  private static class TestingSourceParser {
    private static final String SOURCE = Paths.get(".").toAbsolutePath().normalize().toString().concat("/src/main/java");
    private static final String TEST_SOURCE = Paths.get(".").toAbsolutePath().normalize().toString().concat("/src/test/java");
    private static final String TEST_EXAMPLES = "./src/test/java";

    private final String source;


    TestingSourceParser(String source) {
      this.source = source;
    }

    TestingSourceParser(Class<?> source) throws IOException {
      File before = new File(TEST_EXAMPLES + "/" + source.getName().replaceAll("\\.", "/") + ".java");
      this.source = new String(Files.readAllBytes(before.toPath()));
    }

    ClassVisitor getClassVisitor() {
      ClassVisitor visitor = new ClassVisitor();
      CompilationUnit compilationUnit = buildCompilationUnit();
			compilationUnit.accept(visitor);
      return visitor;
    }

    CompilationUnit buildCompilationUnit() {
      return AstraUtils.readAsCompilationUnit(source, new String[]{SOURCE, TEST_SOURCE}, UseCase.DEFAULT_CLASSPATH_ENTRIES.toArray(new String[0]));
    }
  }

}

