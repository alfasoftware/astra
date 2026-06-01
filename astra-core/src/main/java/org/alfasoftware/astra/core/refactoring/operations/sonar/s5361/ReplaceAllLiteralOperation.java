package org.alfasoftware.astra.core.refactoring.operations.sonar.s5361;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.alfasoftware.astra.core.utils.ASTOperation;
import org.alfasoftware.astra.core.utils.AstraUtils;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Refactoring operation implementing SonarQube rule java:S5361
 * "String.replaceAll() should not be used if the first argument is not a regular expression".
 *
 * <p>When {@link String#replaceAll(String, String)} is called with a pattern argument that is a
 * plain string literal containing no unescaped regex metacharacters, it is rewritten to
 * {@link String#replace(CharSequence, CharSequence)}, which performs a literal search without
 * compiling a regex pattern.
 *
 * <p>Examples:
 * <ul>
 *   <li>{@code str.replaceAll("foo", "bar")}  &rarr; {@code str.replace("foo", "bar")}</li>
 *   <li>{@code str.replaceAll("\\.", "x")}    &rarr; {@code str.replace(".", "x")} (escaped dot &rarr; literal dot)</li>
 *   <li>{@code str.replaceAll("\\\\", "x")}   &rarr; {@code str.replace("\\", "x")} (escaped backslash)</li>
 *   <li>{@code str.replaceAll("foo.bar", "x")} &rarr; unchanged ({@code .} is a metacharacter)</li>
 *   <li>{@code str.replaceAll("\\d+", "x")}   &rarr; unchanged ({@code \d} is a regex shorthand)</li>
 * </ul>
 *
 * <p>Does NOT rewrite when:
 * <ul>
 *   <li>The first argument is not a string literal</li>
 *   <li>The first argument contains unescaped regex metacharacters</li>
 *   <li>The first argument contains a backslash followed by a non-metacharacter (e.g. {@code \d}, {@code \w})</li>
 *   <li>Binding resolution confirms the method is not {@code java.lang.String.replaceAll}</li>
 * </ul>
 */
public class ReplaceAllLiteralOperation implements ASTOperation {

  private static final Logger log = LoggerFactory.getLogger(ReplaceAllLiteralOperation.class);

  private static final String REPLACE_ALL = "replaceAll";
  private static final String REPLACE = "replace";
  private static final String STRING_FQN = "java.lang.String";

  /**
   * Characters that have special meaning in Java regex patterns.
   * A backslash before one of these makes it a literal character (safe to rewrite).
   * A backslash before any other character is a regex construct (unsafe to rewrite).
   */
  static final Set<Character> REGEX_METACHARACTERS =
      Set.of('.', '\\', '*', '+', '?', '^', '$', '{', '}', '[', ']', '|', '(', ')');

  @Override
  public void run(CompilationUnit compilationUnit, ASTNode node, ASTRewrite rewriter)
      throws IOException, MalformedTreeException, BadLocationException {

    if (!(node instanceof MethodInvocation)) {
      return;
    }

    MethodInvocation methodInvocation = (MethodInvocation) node;

    if (!REPLACE_ALL.equals(methodInvocation.getName().getIdentifier())) {
      return;
    }

    @SuppressWarnings("unchecked")
    List<Expression> arguments = methodInvocation.arguments();
    if (arguments.size() != 2) {
      return;
    }

    if (!(arguments.get(0) instanceof StringLiteral)) {
      return;
    }

    StringLiteral patternLiteral = (StringLiteral) arguments.get(0);
    String patternValue = patternLiteral.getLiteralValue();

    if (!isPlainLiteralPattern(patternValue)) {
      return;
    }

    // Use binding resolution to confirm this is java.lang.String.replaceAll
    IMethodBinding binding = methodInvocation.resolveMethodBinding();
    if (binding == null) {
      log.debug("Could not resolve binding for replaceAll in [{}] - skipping",
          AstraUtils.getNameForCompilationUnit(compilationUnit));
      return;
    }
    ITypeBinding declaringClass = binding.getDeclaringClass();
    if (declaringClass == null || !STRING_FQN.equals(declaringClass.getQualifiedName())) {
      return;
    }

    String newPatternValue = unescapeRegexLiteral(patternValue);

    log.info("Rewriting String.replaceAll to String.replace in [{}]: replaceAll(\"{}\", ...) -> replace(\"{}\", ...)",
        AstraUtils.getNameForCompilationUnit(compilationUnit), patternValue, newPatternValue);

    rewriter.set(methodInvocation.getName(), SimpleName.IDENTIFIER_PROPERTY, REPLACE, null);

    if (!newPatternValue.equals(patternValue)) {
      StringLiteral newPatternLiteral = compilationUnit.getAST().newStringLiteral();
      newPatternLiteral.setLiteralValue(newPatternValue);
      rewriter.replace(patternLiteral, newPatternLiteral, null);
    }
  }

  /**
   * Returns {@code true} if the given string value (as returned by
   * {@link StringLiteral#getLiteralValue()}) contains no unescaped regex metacharacters,
   * meaning the pattern matches only the literal text it contains.
   *
   * <p>A backslash followed by a metacharacter (e.g. {@code \.} or {@code \*}) is an escaped
   * literal — safe. A backslash followed by any other character (e.g. {@code \d}, {@code \w},
   * {@code \n}) is a regex construct — unsafe.
   */
  static boolean isPlainLiteralPattern(String value) {
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (c == '\\') {
        if (i + 1 >= value.length()) {
          return false; // trailing backslash — invalid
        }
        char next = value.charAt(i + 1);
        if (REGEX_METACHARACTERS.contains(next)) {
          i++; // escaped metacharacter (e.g. \. or \\) — safe, skip the pair
        } else {
          return false; // regex construct (e.g. \d, \w, \n) — not a plain literal
        }
      } else if (REGEX_METACHARACTERS.contains(c)) {
        return false; // unescaped metacharacter
      }
    }
    return true;
  }

  /**
   * Converts a regex literal pattern (confirmed plain by {@link #isPlainLiteralPattern}) to the
   * equivalent literal string by stripping backslash escapes before metacharacters.
   *
   * <p>For example, the string value {@code \.} (from source {@code "\\."}) becomes {@code .}.
   */
  static String unescapeRegexLiteral(String value) {
    if (!value.contains("\\")) {
      return value;
    }
    StringBuilder sb = new StringBuilder(value.length());
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (c == '\\' && i + 1 < value.length() && REGEX_METACHARACTERS.contains(value.charAt(i + 1))) {
        sb.append(value.charAt(i + 1));
        i++;
      } else {
        sb.append(c);
      }
    }
    return sb.toString();
  }
}
