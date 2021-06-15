package org.alfasoftware.astra.core.refactoring.imports;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.alfasoftware.astra.core.matchers.MethodMatcher;
import org.alfasoftware.astra.core.refactoring.AbstractRefactorTest;
import org.alfasoftware.astra.core.refactoring.operations.imports.UnusedImportRefactor;
import org.alfasoftware.astra.core.refactoring.operations.methods.MethodInvocationRefactor;
import org.alfasoftware.astra.core.utils.ASTOperation;
import org.alfasoftware.astra.core.utils.AstraUtils;
import org.alfasoftware.astra.exampleTypes.A;
import org.alfasoftware.astra.exampleTypes.B;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;
import org.junit.Test;

public class TestImportsRefactor extends AbstractRefactorTest {

  @Test
  public void testUnusedImportRefactor() {
    assertRefactor(UnusedImportExample.class,
      new HashSet<>(Arrays.asList(new UnusedImportRefactor())));
  }

  @Test
  public void testImportReordering() {
    assertRefactor(ImportsReorderingExample.class,
      new HashSet<>(Arrays.asList(new UnusedImportRefactor())));
  }
  
  @Test
  public void testDuplicateStaticImportsWithMethodInvocationRefactor() {
    assertRefactor(DuplicateStaticImportExample.class,
      new HashSet<>(Arrays.asList(new ASTOperation() {
        
        @Override
        public void run(CompilationUnit compilationUnit, ASTNode node, ASTRewrite rewriter)
            throws IOException, MalformedTreeException, BadLocationException {
          
          MethodInvocationRefactor.from(
            MethodMatcher.builder()
              .withFullyQualifiedDeclaringType(A.class.getName())
              .withMethodName("staticFour")
              .withFullyQualifiedParameters(Collections.singletonList(String.class.getName()))
              .build())
            .to(new MethodInvocationRefactor.Changes()
              .toNewType(B.class.getName())
              .toNewMethodName("staticThree"))
          .run(compilationUnit, node, rewriter);
        }
      })));
  }
  
  
  @Test
  public void testDuplicateStaticImportsWithManualTransform() {
    assertRefactor(DuplicateStaticImportExample.class,
      new HashSet<>(Arrays.asList(new ASTOperation() {
        
        @Override
        public void run(CompilationUnit compilationUnit, ASTNode node, ASTRewrite rewriter)
            throws IOException, MalformedTreeException, BadLocationException {
          
          MethodInvocationRefactor.from(
            MethodMatcher.builder()
              .withFullyQualifiedDeclaringType(A.class.getName())
              .withMethodName("staticFour")
              .withFullyQualifiedParameters(Collections.singletonList(String.class.getName()))
              .build())
            .to(new MethodInvocationRefactor.Changes()
              .withInvocationTransform((cu, mi, rw) -> {
                ASTNode newArgument = mi.getAST().newStringLiteral();
                rw.set(newArgument, StringLiteral.ESCAPED_VALUE_PROPERTY, "staticThree(" + mi.arguments().get(0) + ");", null);
                rw.replace(mi.getParent(), newArgument, null);
                AstraUtils.addStaticImport(cu, "org.alfasoftware.astra.exampleTypes.B.staticThree", rw);
              }))
          .run(compilationUnit, node, rewriter);
          
          AstraUtils.addStaticImport(compilationUnit, "org.alfasoftware.astra.exampleTypes.B.staticThree", rewriter);
        }
      })));
  }
  

  @Test
  public void testDuplicateStaticWithMethodInvocationRefactor() {
    assertRefactor(StaticToDuplicateExample.class,
      new HashSet<>(Arrays.asList(new ASTOperation() {
        
        @Override
        public void run(CompilationUnit compilationUnit, ASTNode node, ASTRewrite rewriter)
            throws IOException, MalformedTreeException, BadLocationException {
          
          MethodInvocationRefactor.from(
            MethodMatcher.builder()
              .withFullyQualifiedDeclaringType(B.class.getName())
              .withMethodName("staticThree")
              .withFullyQualifiedParameters(Collections.singletonList(String.class.getName()))
              .build())
            .to(new MethodInvocationRefactor.Changes()
              .toNewType(A.class.getName())
              .toNewMethodName("staticFour"))
          .run(compilationUnit, node, rewriter);
        }
      })));
  }
  
  
  @Test
  public void testDuplicateStaticWithManualTransform() {
    assertRefactor(StaticToDuplicateExample.class,
      new HashSet<>(Arrays.asList(new ASTOperation() {
        
        @Override
        public void run(CompilationUnit compilationUnit, ASTNode node, ASTRewrite rewriter)
            throws IOException, MalformedTreeException, BadLocationException {
          
          MethodInvocationRefactor.from(
            MethodMatcher.builder()
              .withFullyQualifiedDeclaringType(B.class.getName())
              .withMethodName("staticThree")
              .withFullyQualifiedParameters(Collections.singletonList(String.class.getName()))
              .build())
            .to(new MethodInvocationRefactor.Changes()
              .withInvocationTransform((cu, mi, rw) -> {
                ASTNode newArgument = mi.getAST().newStringLiteral();
                rw.set(newArgument, StringLiteral.ESCAPED_VALUE_PROPERTY, "staticFour(" + mi.arguments().get(0) + ");", null);
                rw.replace(mi.getParent(), newArgument, null);
                AstraUtils.addStaticImport(cu, "org.alfasoftware.astra.exampleTypes.A.staticFour", rw);
              }))
          .run(compilationUnit, node, rewriter);
        }
      })));
  }
  
  
  /**
   * Tests that the following static imports are not removed:
   * <ul>
   *   <li>A static method import from another type in the same package</li>
   *   <li>A static method import from an inner class of that type</li>
   * </ul>
   */
  @Test
  public void testStaticImportsFromSamePackageAreNotRemoved() {
    assertRefactor(StaticImportSamePackageExample.class,
        new HashSet<>(Arrays.asList(new UnusedImportRefactor())));
  }

  /**
   * Tests that static imports from an inner class of a top level type are not incorrectly removed
   */
  @Test
  public void testStaticImportsFromInnerClassOfRefactoredClassAreNotRemoved() {
    assertRefactor(StaticImportSameTypeInnerClassExample.class,
        new HashSet<>(Arrays.asList(new UnusedImportRefactor())));
  }
}
