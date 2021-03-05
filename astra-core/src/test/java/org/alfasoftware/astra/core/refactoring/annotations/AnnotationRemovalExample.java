package org.alfasoftware.astra.core.refactoring.annotations;

import org.alfasoftware.astra.exampleTypes.AnnotationA;
import org.alfasoftware.astra.exampleTypes.AnnotationB;

@AnnotationA("")
@AnnotationB("AAA")
public class AnnotationRemovalExample {

  @AnnotationA(value = "A") @AnnotationB("FOO")
  protected long someField;

  @AnnotationB("*BLAH1")
  public String getFoo() {
    return "";
  }

  @AnnotationA("*BLAH2")
  @Deprecated
  public char getBar() {
    return 'a';
  }

  @AnnotationA("BAR")
  @AnnotationB("*BLAH2")
  @Deprecated
  public char getFooBar() {
    return 'a';
  }

  @AnnotationB("*BLAH2")

  @Deprecated
  public char getSpace() {
    return 'a';
  }
}

