package org.alfasoftware.astra.core.refactoring.operations.sonar.s5361;

public class ReplaceAllLiteralNoopExample {

  // dot is an unescaped metacharacter
  void unescapedDot(String s) {
    s.replaceAll("foo.bar", "x");
  }

  // character class bracket
  void characterClass(String s) {
    s.replaceAll("[abc]", "x");
  }

  // asterisk quantifier
  void asteriskQuantifier(String s) {
    s.replaceAll("a*b", "x");
  }

  // plus quantifier
  void plusQuantifier(String s) {
    s.replaceAll("a+b", "x");
  }

  // question mark quantifier
  void questionQuantifier(String s) {
    s.replaceAll("a?b", "x");
  }

  // caret anchor
  void caretAnchor(String s) {
    s.replaceAll("^foo", "x");
  }

  // dollar anchor
  void dollarAnchor(String s) {
    s.replaceAll("foo$", "x");
  }

  // alternation pipe
  void alternation(String s) {
    s.replaceAll("a|b", "x");
  }

  // regex digit shorthand
  void regexDigit(String s) {
    s.replaceAll("\\d+", "x");
  }

  // regex word-character shorthand
  void regexWord(String s) {
    s.replaceAll("\\w", "x");
  }

  // regex whitespace shorthand
  void regexWhitespace(String s) {
    s.replaceAll("\\s", "x");
  }

  // first argument is not a literal — cannot determine safety
  void nonLiteralPattern(String s, String pattern) {
    s.replaceAll(pattern, "x");
  }

  // non-String replaceAll — declaring type is not java.lang.String
  void nonStringReplaceAll(ReplaceAllProvider provider) {
    provider.replaceAll("foo", "bar");
  }
}
