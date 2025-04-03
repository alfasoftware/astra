package org.alfasoftware.astra.core.refactoring.annotations;

import org.alfasoftware.astra.exampleTypes.AnnotationA;

@org.alfasoftware.astra.moreexampletypes.AnnotationA("")
public class AnnotationChangeSameSimpleNameWithRemainderExampleAfter {

    /**
     * This annotation should not change
     */
    @AnnotationA(value="A")
    protected long someField;

    @org.alfasoftware.astra.moreexampletypes.AnnotationA("BAR")
    public char getBar(){
        return 'a';
    }
}
