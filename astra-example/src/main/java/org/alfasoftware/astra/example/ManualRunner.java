package org.alfasoftware.astra.example;

import java.nio.file.Paths;

import org.alfasoftware.astra.core.refactoring.UseCase;
import org.alfasoftware.astra.core.utils.AstraCore;

/**
 * Used to manually call the core Astra runner with a given directory and UseCase, from the IDE.
 */
public class ManualRunner {

  /*
   * This would normally be something like "C:/Code/LocalCheckout",
   * but this should resolve to the root of this folder (eg "C:\Code\workspace\astra\astra-example")
   */
  private static final String DIRECTORY_PATH = Paths.get(".").toAbsolutePath().normalize().toString();
  private static final UseCase USE_CASE = new ExampleUseCase();

  public static void main(String[] args) {
    AstraCore.run(DIRECTORY_PATH, USE_CASE);
  }
}
