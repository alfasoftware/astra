package org.alfasoftware.astra.core.refactoring.operations.sonar.s5361;

public class ReplaceAllLiteralExample {

  // plain string — no metacharacters
  void plainString(String s) {
    s.replaceAll("foo", "bar");
  }

  // empty string
  void emptyString(String s) {
    s.replaceAll("", "bar");
  }

  // string with spaces and alphanumerics only
  void spacesAndAlpha(String s) {
    s.replaceAll("hello world", "hi");
  }

  // escaped dot (\. in regex = literal dot)
  void escapedDot(String s) {
    s.replaceAll("\\.", "x");
  }

  // escaped asterisk (\* in regex = literal asterisk)
  void escapedAsterisk(String s) {
    s.replaceAll("\\*", "x");
  }

  // escaped open parenthesis
  void escapedOpenParen(String s) {
    s.replaceAll("\\(", "x");
  }

  // escaped backslash (\\\\ in source = \\ value = regex \\ matching one backslash)
  void escapedBackslash(String s) {
    s.replaceAll("\\\\", "x");
  }

  // result of replaceAll used as expression
  void usedAsExpression(String s) {
    String result = s.replaceAll("foo", "bar");
  }

  // chained on a string literal
  void calledOnStringLiteral() {
    "foo bar".replaceAll("foo", "baz");
  }
}
