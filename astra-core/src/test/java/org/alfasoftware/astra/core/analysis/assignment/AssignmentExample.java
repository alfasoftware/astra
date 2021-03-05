package org.alfasoftware.astra.core.analysis.assignment;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.GregorianCalendar;

public class AssignmentExample {

  Date fieldDateNotAssigned;
  Date fieldDateAssignedLater;
  Date fieldDateAssignedNull = null;
  Date fieldDateAssignedDate = new Date();
  Date fieldDateAssignedIndirectDate = GregorianCalendar.from(ZonedDateTime.now()).getGregorianChange();
  Date multiAssignedOne = new Date(), multiAssignedTwo = null, multiAssignedThree;

  @SuppressWarnings("unused")
  public void doFoo(Date dateParameter) {
    dateParameter = new Date();
    fieldDateAssignedLater = new Date();
    Date variableDateNull = null;
    Date variableDateAssignedLater;
    Date variableDateAssignedDate = new Date();
    Date variableDateAssignedIndirectDate = GregorianCalendar.from(ZonedDateTime.now()).getTime();
    variableDateAssignedLater = new Date(1234);
  }
}
