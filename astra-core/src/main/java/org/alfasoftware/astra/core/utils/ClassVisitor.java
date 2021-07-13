package org.alfasoftware.astra.core.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IDocElement;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

/**
 * Tracks what is seen when visiting all nodes in a compilation unit.
 */
public class ClassVisitor extends ASTVisitor {

  private static final Logger log = Logger.getLogger(ClassVisitor.class);

  private final List<TypeDeclaration> typeDeclarations = new ArrayList<>();
  private final List<TypeParameter> typeParameters = new ArrayList<>();
  private final List<ParameterizedType> parameterizedTypes = new ArrayList<>();
  private final List<VariableDeclarationFragment> variableDeclarationFragments = new ArrayList<>();
  private final List<VariableDeclarationStatement> variableDeclarationStatements = new ArrayList<>();
  private final List<FieldDeclaration> fieldDeclarations = new ArrayList<>();
  private final List<ArrayType> arrayTypes = new ArrayList<>();
  private final List<MethodInvocation> methodInvocations = new ArrayList<>();
  private final List<MethodDeclaration> methodDeclarations = new ArrayList<>();
  private final List<SingleMemberAnnotation> singleMemberAnnotations = new ArrayList<>();
  private final List<NormalAnnotation> normalAnnotations = new ArrayList<>();
  private final List<MarkerAnnotation> markerAnnotations = new ArrayList<>();
  private final List<ClassInstanceCreation> classInstanceCreations = new ArrayList<>();
  private final List<TagElement> tagElements = new ArrayList<>();

  private final List<FieldAccess> fieldAccesses = new ArrayList<>();
  private final List<CastExpression> castExpressions = new ArrayList<>();

  private final List<ImportDeclaration> imports = new ArrayList<>();
  private final List<SimpleType> simpleTypes = new ArrayList<>();
  private final List<QualifiedType> qualifiedTypes = new ArrayList<>();
  private final List<Assignment> assignments = new ArrayList<>();
  private final List<SingleVariableDeclaration> singleVariableDeclarations = new ArrayList<>();

  private final List<SimpleName> simpleNames = new ArrayList<>();
  private final List<QualifiedName> qualifiedNames = new ArrayList<>();


  @Override
  public boolean visit(ImportDeclaration node) {
    log.debug("Import: " + node);
    imports.add(node);
    return super.visit(node);
  }

  @Override
  public boolean visit(SimpleType node) {
    log.debug("SimpleType: " + node);
    simpleTypes.add(node);
    return super.visit(node);
  }

  /**
   * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.TypeDeclaration)
   */
  @Override
  public boolean visit(TypeDeclaration node) {
    log.debug("Type declar: " + node);
    typeDeclarations.add(node);
    return super.visit(node);
  }

  /**
   * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.TypeParameter)
   */
  @Override
  public boolean visit(TypeParameter node) {
    log.debug("Type param: " + node);
    typeParameters.add(node);
    return super.visit(node);
  }

  @Override
  public boolean visit(ParameterizedType node) {
    log.debug("Parameterised type: " + node);
    parameterizedTypes.add(node);
    return super.visit(node);
  }

  @Override
  public boolean visit(VariableDeclarationStatement node) {
    log.debug("Variable declaration statement: " + node);
    variableDeclarationStatements.add(node);
    return super.visit(node);
  }

  @Override
  public boolean visit(FieldDeclaration node) {
    log.debug("Field declaration: " + node);
    fieldDeclarations.add(node);
    return super.visit(node);
  }

  @Override
  public boolean visit(MethodDeclaration node) {
    log.debug("Method declaration: " + node);
    methodDeclarations.add(node);
    return super.visit(node);
  }

  @Override
  public boolean visit(MethodInvocation node) {
    log.debug("Method invocation: " + node);
    methodInvocations.add(node);
    return super.visit(node);
  }

  @Override
  public boolean visit(Assignment node) {
    log.debug("Assignment: " + node);
    assignments.add(node);
    return super.visit(node);
  }

  /**
   * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.NormalAnnotation)
   */
  @Override
  public boolean visit(NormalAnnotation node) {
    log.debug("Normal annotation: " + node);
    normalAnnotations.add(node);
    return super.visit(node);
  }

  /**
   * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.SingleMemberAnnotation)
   */
  @Override
  public boolean visit(SingleMemberAnnotation node) {
    log.debug("Single member annotation: " + node);
    singleMemberAnnotations.add(node);
    return super.visit(node);
  }

  /**
   * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.MarkerAnnotation)
   */
  @Override
  public boolean visit(MarkerAnnotation node) {
    log.debug("Marker annotation: " + node);
    markerAnnotations.add(node);
    return super.visit(node);
  }

  @Override
  public boolean visit(VariableDeclarationFragment node) {
    log.debug("Variable declaration fragment: " + node);
    variableDeclarationFragments.add(node);
    return super.visit(node);
  }

  @Override
  public boolean visit(SingleVariableDeclaration node) {
    log.debug("Single variable declaration: " + node);
    this.singleVariableDeclarations.add(node);
    return super.visit(node);
  }


  @Override
  public boolean visit(ClassInstanceCreation node) {
    log.debug("Class instance creation: " + node);
    this.classInstanceCreations.add(node);
    return super.visit(node);
  }


  @Override
  public boolean visit(Javadoc node) {
    @SuppressWarnings("unchecked")
    List<TagElement> tags = node.tags();
    for (TagElement tag : tags) {
      this.tagElements.add(tag);

      @SuppressWarnings("unchecked")
      List<IDocElement> fragments = tag.fragments();
      for (IDocElement fragment : fragments) {
        if (fragment instanceof TagElement) {
          this.tagElements.add((TagElement) fragment);
        }
      }
    }
    return super.visit(node);
  }

  @Override
  public boolean visit(SimpleName node) {
    log.debug("Simple name: " + node);
    this.simpleNames.add(node);
    return super.visit(node);
  }

  @Override
  public boolean visit(QualifiedName node) {
    log.debug("Qualified name: " + node);
    this.qualifiedNames.add(node);
    return super.visit(node);
  }

  @Override
  public boolean visit(FieldAccess node) {
    log.debug("Field access: " + node);
    fieldAccesses.add(node);
    return super.visit(node);
  }

  @Override
  public boolean visit(ArrayType node) {
    log.debug("Array type: " + node);
    arrayTypes.add(node);
    return super.visit(node);
  }

  @Override
  public boolean visit(QualifiedType node) {
    log.debug("Qual type: " + node);
    qualifiedTypes.add(node);
    return super.visit(node);
  }

  @Override
  public boolean visit(CastExpression node) {
    log.debug("Cast exp: " + node);
    castExpressions.add(node);
    return super.visit(node);
  }


  /**
   * @return Types which are defined.
   */
  public List<TypeDeclaration> getTypeDeclarations() {
    return typeDeclarations;
  }

  public List<ParameterizedType> getParameterizedTypes() {
    return parameterizedTypes;
  }

  public List<FieldDeclaration> getFieldDeclarations() {
    return fieldDeclarations;
  }

  public List<FieldAccess> getFieldAccesses() {
    return fieldAccesses;
  }


  public List<CastExpression> getCastExpressions() {
    return castExpressions;
  }

  public List<VariableDeclarationStatement> getVariableDeclarationStatements() {
    return variableDeclarationStatements;
  }

  public List<VariableDeclarationFragment> getVariableDeclarationFragments() {
    return variableDeclarationFragments;
  }

  public List<MethodDeclaration> getMethodDeclarations() {
    return methodDeclarations;
  }

  public List<ImportDeclaration> getImports() {
    return imports;
  }

  /**
   * @return Types which are defined.
   */
  public List<TypeParameter> getTypeParameters() {
    return typeParameters;
  }

  public List<ArrayType> getArrayTypes() {
    return arrayTypes;
  }

  public List<SimpleType> getSimpleTypes() {
    return simpleTypes;
  }

  public List<QualifiedType> getQualifiedTypes() {
    return qualifiedTypes;
  }

  public List<Assignment> getAssignments() {
    return assignments;
  }

  public List<TagElement> getTagElements() {
    return tagElements;
  }

  public List<SingleVariableDeclaration> getSingleVariableDeclarations() {
    return singleVariableDeclarations;
  }

  public List<ClassInstanceCreation> getClassInstanceCreations() {
    return classInstanceCreations;
  }

  /**
   * @return Methods which are called.
   */
  public List<MethodInvocation> getMethodInvocations() {
    return methodInvocations;
  }


  /**
   * @return Normal (multi-argument) annotations which are defined.
   */
  public List<NormalAnnotation> getNormalAnnotations() {
    return normalAnnotations;
  }


  /**
   * @return Single argument annotations which are defined.
   */
  public List<SingleMemberAnnotation> getSingleMemberAnnotations() {
    return singleMemberAnnotations;
  }


  /**
   * @return Marker annotations which are defined.
   */
  public List<MarkerAnnotation> getMarkerAnnotations() {
    return markerAnnotations;
  }

  public List<SimpleName> getSimpleNames() {
    return simpleNames;
  }

  public List<QualifiedName> getQualifiedNames() {
    return qualifiedNames;
  }
  
  /**
   * @return A set of all the nodes visited and collected by this visitor.
   * 
   * Note that if the node type is not explicitly handled by this visitor, they won't be returned.
   */
  public Set<ASTNode> getVisitedNodes() {
    return Stream.of(
      getTypeDeclarations(),
      getMethodInvocations(),
      getMethodDeclarations(),
      getTypeParameters(),
      getParameterizedTypes(),
      getFieldDeclarations(),
      getVariableDeclarationStatements(),
      getVariableDeclarationFragments(),
      getArrayTypes(),
      getSimpleTypes(),
      getSimpleNames(),
      getQualifiedNames(),
      getQualifiedTypes(),
      getAssignments(),
      getSingleVariableDeclarations(),
      getMarkerAnnotations(),
      getNormalAnnotations(),
      getSingleMemberAnnotations(),
      getClassInstanceCreations(),
      getTagElements(),
      getImports(),
      getFieldAccesses(),
      getCastExpressions())
    .flatMap(Collection::stream)
    .collect(Collectors.toSet());
  }
}