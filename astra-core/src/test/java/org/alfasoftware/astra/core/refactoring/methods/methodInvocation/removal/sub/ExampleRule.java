package org.alfasoftware.astra.core.refactoring.methods.methodInvocation.removal.sub;

import java.util.Arrays;

import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

/**
 * Example class for testing purposes.
 *
 */
public class ExampleRule implements MethodRule {

  @SuppressWarnings("unused")
  private final Object[] modules;

  public ExampleRule(final Object... modules) {
    this.modules = Arrays.copyOf(modules, modules.length);
  }

  public static ExampleRule ruleFor(@SuppressWarnings("unused") String identifier, final Object... modules) {
    return new ExampleRule(modules);
  }


  @Override
  public Statement apply(Statement base, FrameworkMethod method, Object target) {
    return base;
  }

}

