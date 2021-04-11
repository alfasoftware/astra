package org.alfasoftware.astra.example;

import java.util.function.Predicate;

public class ExampleUseCaseWithPrefilter extends ExampleUseCase {

  @Override
  public Predicate<String> getPrefilteringPredicate() {
    return filePath -> filePath.contains("astra-example");
  }

}
