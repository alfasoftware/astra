package org.alfasoftware.astra.core.matchers;

import org.eclipse.jdt.core.dom.ASTNode;

public interface Matcher {
  boolean matches(ASTNode node);
}
