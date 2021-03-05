package org.alfasoftware.astra.core.refactoring.annotations;

import org.alfasoftware.astra.exampleTypes.AnnotationA;

@AnnotationA("")
public class AnnotationRemovalExampleAfter {

  @AnnotationA(value = "A") protected long someField;

  public String getFoo() {
    return "";
  }

  @AnnotationA("*BLAH2")
  @Deprecated
  public char getBar() {
    return 'a';
  }

  @AnnotationA("BAR")
  @Deprecated
  public char getFooBar() {
    return 'a';
  }

  @Deprecated
  public char getSpace() {
    return 'a';
  }
}

