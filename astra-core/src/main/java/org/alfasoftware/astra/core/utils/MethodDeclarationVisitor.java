package org.alfasoftware.astra.core.utils;

import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks what is seen when visiting MethodDeclaration nodes in a compilation unit.
 */
public class MethodDeclarationVisitor extends ASTVisitor {

  private static final Logger log = Logger.getLogger(MethodDeclarationVisitor.class);

  private final List<MethodDeclaration> methodDeclarations = new ArrayList<>();


  @Override
  public boolean visit(MethodDeclaration node) {
    log.debug("Method declaration: " + node);
    methodDeclarations.add(node);
    return super.visit(node);
  }

  public List<MethodDeclaration> getMethodDeclarations() {
    return methodDeclarations;
  }
}
