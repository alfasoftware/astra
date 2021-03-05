package org.alfasoftware.astra.core.refactoring.methods.methodInvocation.removal.sub;

import java.util.Arrays;

import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

/**
 * Example class for testing purposes.
 *
 */
@SuppressWarnings("unused")
public class ReplacementRule implements MethodRule {

  private final Object[] modules;

  public ReplacementRule(Object test, final Object... modules) {
    this.modules = Arrays.copyOf(modules, modules.length);
  }

  public static ReplacementRule replacementRuleFor(String identifier, Object test, final Object... modules) {
    return new ReplacementRule(test, modules);
  }


  @Override
  public Statement apply(Statement base, FrameworkMethod method, Object target) {
    return base;
  }

}

