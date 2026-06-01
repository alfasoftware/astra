package org.alfasoftware.astra.core.refactoring.operations.sonar.s6201;

import org.alfasoftware.astra.exampleTypes.A;
import org.alfasoftware.astra.exampleTypes.B;

public class PatternMatchingInstanceofNoopExampleAfter {

  private Object member;

  // Already using pattern matching - should not be touched
  public void alreadyPatternMatch(Object obj) {
    if (obj instanceof A a) {
      a.one();
    }
  }

  // Negated instanceof with cast in else branch - should not rewrite
  public void negatedWithCastInElse(Object obj) {
    if (!(obj instanceof A)) {
      System.out.println("not A");
    } else {
      A a = (A) obj;
      a.one();
    }
  }

  // Cast variable is reassigned - should not rewrite
  public void castVarReassigned(Object obj, A other) {
    if (obj instanceof A) {
      A a = (A) obj;
      a = other;
      a.one();
    }
  }

  // Subject reassigned before cast - should not rewrite
  public void subjectReassignedBeforeCast(Object obj, Object other) {
    if (obj instanceof A) {
      obj = other;
      A a = (A) obj;
      a.one();
    }
  }

  // No cast in block - nothing to rewrite
  public void noCastInBlock(Object obj) {
    if (obj instanceof A) {
      System.out.println("it is an A");
    }
  }

  // Cast to a different type than the instanceof type - should not rewrite
  public void castToDifferentType(Object obj) {
    if (obj instanceof A) {
      B b = (B) obj;
      b.one();
    }
  }

  // Cast of a different subject than the instanceof subject - should not rewrite
  public void castOfDifferentSubject(Object obj, Object other) {
    if (obj instanceof A) {
      A a = (A) other;
      a.one();
    }
  }

  // instanceof on a negated compound condition - should not rewrite
  public void negatedInIf(Object obj) {
    if (!(obj instanceof A)) {
      return;
    }
    A a = (A) obj;
    a.one();
  }

  // Subject is accessed via explicit 'this' qualifier (FieldAccess, not SimpleName) - should not rewrite
  public void qualifiedFieldSubject() {
    if (this.member instanceof A) {
      A a = (A) this.member;
      a.one();
    }
  }
}
