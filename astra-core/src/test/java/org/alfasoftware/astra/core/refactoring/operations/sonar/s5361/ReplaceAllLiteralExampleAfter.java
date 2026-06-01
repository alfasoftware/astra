package org.alfasoftware.astra.core.refactoring.operations.sonar.s5361;

public class ReplaceAllLiteralExampleAfter {

  // plain string — no metacharacters
  void plainString(String s) {
    s.replace("foo", "bar");
  }

  // empty string
  void emptyString(String s) {
    s.replace("", "bar");
  }

  // string with spaces and alphanumerics only
  void spacesAndAlpha(String s) {
    s.replace("hello world", "hi");
  }

  // escaped dot (\. in regex = literal dot)
  void escapedDot(String s) {
    s.replace(".", "x");
  }

  // escaped asterisk (\* in regex = literal asterisk)
  void escapedAsterisk(String s) {
    s.replace("*", "x");
  }

  // escaped open parenthesis
  void escapedOpenParen(String s) {
    s.replace("(", "x");
  }

  // escaped backslash (\\\\ in source = \\ value = regex \\ matching one backslash)
  void escapedBackslash(String s) {
    s.replace("\\", "x");
  }

  // result of replaceAll used as expression
  void usedAsExpression(String s) {
    String result = s.replace("foo", "bar");
  }

  // chained on a string literal
  void calledOnStringLiteral() {
    "foo bar".replace("foo", "baz");
  }
}
