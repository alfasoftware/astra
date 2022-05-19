package org.alfasoftware.astra.core.refactoring.annotations;

import org.alfasoftware.astra.exampleTypes.AnnotationA;

public class AddAndRemoveMemberFromAnnotationExample {

  @AnnotationA
  protected long someField;

  @AnnotationA(value="A string of no importance")
  protected String someStringField;

  /*
   *  TODO
   *
   *  Before:
   *    @AnnotationD(value="A string of no importance", othervalue = "Other string of no importance")
   *    protected String someOtherStringField;
   *
   *  After:
   *    @AnnotationD(value = "Other string of no importance")
   *    protected String someOtherStringField;
   */
}

