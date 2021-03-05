package org.alfasoftware.astra.core.analysis.assignment;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.alfasoftware.astra.core.analysis.AbstractAnalysisTest;
import org.alfasoftware.astra.core.analysis.operations.assignment.AssignmentAnalysisResult;
import org.alfasoftware.astra.core.analysis.operations.assignment.FindAssignmentAnalysis;
import org.junit.Test;


public class TestAssignmentAnalysis extends AbstractAnalysisTest {


  @Test
  public void testSingleAssignment() {
    FindAssignmentAnalysis analysis = new FindAssignmentAnalysis(Date.class.getName());

    final List<AssignmentAnalysisResult> expectedResults = Arrays.asList(
        new AssignmentAnalysisResult("fieldDateNotAssigned", "null"),
        new AssignmentAnalysisResult("fieldDateAssignedLater", "null"),
        new AssignmentAnalysisResult("fieldDateAssignedNull", "null"),
        new AssignmentAnalysisResult("fieldDateAssignedDate", "new Date()"),
        new AssignmentAnalysisResult("fieldDateAssignedIndirectDate", "GregorianCalendar.from(ZonedDateTime.now()).getGregorianChange()"),
        new AssignmentAnalysisResult("variableDateNull", "null"),
        new AssignmentAnalysisResult("variableDateAssignedLater", "null"),
        new AssignmentAnalysisResult("variableDateAssignedDate", "new Date()"),
        new AssignmentAnalysisResult("variableDateAssignedIndirectDate", "GregorianCalendar.from(ZonedDateTime.now()).getTime()"),
        new AssignmentAnalysisResult("fieldDateAssignedLater", "new Date()"),
        new AssignmentAnalysisResult("variableDateAssignedLater", "new Date(1234)"),
        new AssignmentAnalysisResult("multiAssignedOne", "new Date()"),
        new AssignmentAnalysisResult("multiAssignedTwo", "null"),
        new AssignmentAnalysisResult("multiAssignedThree", "null"),
        new AssignmentAnalysisResult("dateParameter", "new Date()")

    );

    assertAnalysis(AssignmentExample.class, analysis, expectedResults);
  }
}

