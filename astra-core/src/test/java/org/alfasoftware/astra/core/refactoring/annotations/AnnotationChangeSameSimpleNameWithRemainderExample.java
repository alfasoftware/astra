package org.alfasoftware.astra.core.refactoring.annotations;

import org.alfasoftware.astra.exampleTypes.AnnotationA;

@AnnotationA("")
public class AnnotationChangeSameSimpleNameWithRemainderExample {

    /**
     * This annotation should not change
     */
    @AnnotationA(value="A")
    protected long someField;

    @AnnotationA("BAR")
    public char getBar(){
        return 'a';
    }
}
