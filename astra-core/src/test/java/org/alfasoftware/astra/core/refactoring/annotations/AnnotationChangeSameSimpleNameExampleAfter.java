package org.alfasoftware.astra.core.refactoring.annotations;

import org.alfasoftware.astra.moreexampletypes.AnnotationA;

@AnnotationA("")
public class AnnotationChangeSameSimpleNameExampleAfter {

    @AnnotationA(value="A")
    protected long someField;

    @AnnotationA("BAR")
    public char getBar(){
        return 'a';
    }
}
