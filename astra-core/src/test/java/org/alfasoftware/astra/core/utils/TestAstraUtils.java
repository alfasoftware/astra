package org.alfasoftware.astra.core.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.alfasoftware.astra.core.refactoring.UseCase;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.junit.Test;

/**
 * Tests for the general purpose utilities provided in {@link AstraUtils}.
 */
public class TestAstraUtils {

  protected static final String SOURCE = Paths.get(".").toAbsolutePath().normalize().toString().concat("/src/main/java");
  protected static final String TEST_SOURCE = Paths.get(".").toAbsolutePath().normalize().toString().concat("/src/test/java");
  protected static final String TEST_EXAMPLES = "./src/test/java";
  

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
  public void testGetNameForAnonymousClassDeclaration() {
    // Given
    CompilationUnit compilationUnit = parse(ExampleWithAnonymousClassDeclaration.class);
    
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
  
  
  private CompilationUnit parse(Class<?> source) {
    File before = new File(TEST_EXAMPLES + "/" + source.getName().replaceAll("\\.", "/") + ".java");
    
    try {
      String fileContentBefore = new String(Files.readAllBytes(before.toPath()));
      
      return AstraUtils.readAsCompilationUnit(fileContentBefore, new String[] {SOURCE, TEST_SOURCE}, UseCase.DEFAULT_CLASSPATH_ENTRIES.toArray(new String[0]));
    } catch (IOException e) {
      e.printStackTrace();
      fail();
      throw new IllegalArgumentException();
    }
  }
}

