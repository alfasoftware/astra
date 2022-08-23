package org.alfasoftware.astra.core.analysis.methods;

import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.alfasoftware.astra.core.analysis.AbstractAnalysisTest;
import org.alfasoftware.astra.core.analysis.operations.methods.FindMethodDeclarationsAnalysis;
import org.alfasoftware.astra.core.analysis.operations.methods.MatchedMethodResult;
import org.alfasoftware.astra.core.analysis.operations.methods.MethodAnalysisResult;
import org.alfasoftware.astra.core.matchers.MethodMatcher;
import org.junit.Test;

public class TestFindMethodDeclarationsAnalysis extends AbstractAnalysisTest {

    private MethodMatcher getMethodMatcher(){
        return MethodMatcher.builder()
        .withFullyQualifiedDeclaringType(ExampleMethodAnalysis.class.getName())
        .withfullyQualifiedReturnType("void")
        .withMethodName("baseMethod")
        .build();
    }

    private Set<MethodMatcher> getMethodMatcherSet(){
        Set<MethodMatcher> methodMatcherSet = new HashSet<>();
        methodMatcherSet.add(getMethodMatcher());
        return methodMatcherSet;
    }

    @Test
    public void testMethodDeclarationsAnalysis() {
        FindMethodDeclarationsAnalysis analysis = new FindMethodDeclarationsAnalysis(getMethodMatcherSet());
        List<MatchedMethodResult> matches = Arrays.asList(
            new MatchedMethodResult("public void baseMethod(){\n}\n", "org.alfasoftware.astra.core.analysis.methods.ExampleMethodAnalysis", 4)
        );

        List<MethodAnalysisResult> expectedResult = Arrays.asList(           
            new MethodAnalysisResult(getMethodMatcher(), matches)  
        );

        assertAnalysis(ExampleMethodAnalysis.class, analysis, expectedResult);
        assertNotNull(analysis.getPrintableResults());
    }
}

