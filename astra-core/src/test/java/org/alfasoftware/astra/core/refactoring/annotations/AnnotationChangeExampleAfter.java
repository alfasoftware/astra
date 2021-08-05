package org.alfasoftware.astra.core.refactoring.annotations;

import org.alfasoftware.astra.exampleTypes.AnnotationB;

@AnnotationB("")
public class AnnotationChangeExampleAfter {
    @AnnotationB(value="A")
    protected long someField;

    @AnnotationB("BAR")
    public char getBar(){
        return 'a';
    }
}
