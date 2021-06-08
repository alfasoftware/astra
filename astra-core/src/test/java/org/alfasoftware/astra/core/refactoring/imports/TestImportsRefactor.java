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
  public void testDuplicateStaticImports() {
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
}
