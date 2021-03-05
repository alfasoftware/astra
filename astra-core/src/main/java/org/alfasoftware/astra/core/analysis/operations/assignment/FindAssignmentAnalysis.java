package org.alfasoftware.astra.core.analysis.operations.assignment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.alfasoftware.astra.core.analysis.operations.AnalysisOperation;
import org.alfasoftware.astra.core.utils.AstraUtils;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.text.edits.MalformedTreeException;

/**
 * Finds assignments to variables of a specified fully qualified type.
 */
public class FindAssignmentAnalysis implements AnalysisOperation<AssignmentAnalysisResult> {

  private final String fullyQualifiedTypeName;
  private final List<AssignmentAnalysisResult> foundAssignments = new ArrayList<>();

  public FindAssignmentAnalysis(String fullyQualifiedTypeName) {
    this.fullyQualifiedTypeName = fullyQualifiedTypeName;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void run(CompilationUnit compilationUnit, ASTNode node, ASTRewrite rewriter)
      throws MalformedTreeException{
    if (node instanceof VariableDeclarationStatement) {
      VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement) node;
      addToResultIfAssignmentToType(variableDeclarationStatement.getType(), variableDeclarationStatement.fragments());
    }
    if (node instanceof FieldDeclaration) {
      FieldDeclaration fieldDeclaration = (FieldDeclaration) node;
      addToResultIfAssignmentToType(fieldDeclaration.getType(), fieldDeclaration.fragments());
    }
    if (node instanceof Assignment) {
      Assignment assignment = (Assignment) node;
      final ITypeBinding iTypeBinding = assignment.resolveTypeBinding();
      if (iTypeBinding != null && iTypeBinding.getQualifiedName().equals(fullyQualifiedTypeName)) {
          final AssignmentAnalysisResult assignmentAnalysisResult = new AssignmentAnalysisResult();
          assignmentAnalysisResult.setVariableName(assignment.getLeftHandSide().toString());
          assignmentAnalysisResult.setAssignedValue(assignment.getRightHandSide().toString());
          foundAssignments.add(assignmentAnalysisResult);
        }
    }
  }

  private void addToResultIfAssignmentToType(final Type type, final List<VariableDeclarationFragment> fragments) {
    if (AstraUtils.getFullyQualifiedName(type).equals(fullyQualifiedTypeName)) {
      for (VariableDeclarationFragment fragment : fragments) {
        final AssignmentAnalysisResult assignmentAnalysisResult = new AssignmentAnalysisResult();
        assignmentAnalysisResult.setVariableName(fragment.getName().getIdentifier());
        assignmentAnalysisResult.setAssignedValue(fragment.getInitializer() == null ? "null" : fragment.getInitializer() .toString());
        foundAssignments.add(assignmentAnalysisResult);
      }
    }
  }

  @Override
  public Collection<AssignmentAnalysisResult> getResults() {
    return foundAssignments;
  }
}

