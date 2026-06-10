package org.alfasoftware.astra.core.refactoring.operations.sonar.s2959;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfasoftware.astra.core.utils.ASTOperation;
import org.alfasoftware.astra.core.utils.CompilationUnitProperty;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.RecordDeclaration;
import org.eclipse.jdt.core.dom.SwitchExpression;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.TargetSourceRangeComputer;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;

/**
 * Removes unnecessary semicolons, satisfying SonarQube rule java:S2959
 * "Unnecessary semicolons should be omitted".
 *
 * <p>Five forms are handled:
 * <ol>
 *   <li><b>Try-with-resources trailing semicolons</b> — the core SonarJava S2959 detection:
 *       when the number of separators in the resource list equals the number of resources,
 *       the last separator is unnecessary. In JDT terms: any {@code ;} found between the
 *       end of the last resource expression and the closing {@code )} of the resource list.</li>
 *   <li><b>Block-level empty statements</b> — a lone {@code ;} inside a {@link Block}. The
 *       parent of such an {@link EmptyStatement} is a {@link Block}.</li>
 *   <li><b>Switch-case empty statements</b> — a lone {@code ;} inside a {@link SwitchStatement}
 *       or {@link SwitchExpression} case list.</li>
 *   <li><b>Class-level empty declarations</b> — a {@code ;} written directly in any type body
 *       (class, inner class, enum body section, interface, record, or anonymous class). These
 *       are invisible to the JDT AST and are found by scanning source-text gaps between
 *       consecutive {@code bodyDeclarations()} entries at every nesting level. For enum
 *       declarations the scan skips past the required separator between enum constants and
 *       body declarations.</li>
 *   <li><b>Post-type-brace semicolons</b> — a {@code ;} that appears immediately after the
 *       closing {@code \}} of a top-level type declaration (e.g. {@code class Foo \{\};}),
 *       found by scanning the source text just beyond the type node's end position.</li>
 * </ol>
 *
 * <p>Semicolons that fall inside any comment (line or block) are never removed.
 * {@link CompilationUnit#getCommentList()} is used to build the exclusion set.
 *
 * <p>Empty statements whose immediate parent is a control-flow construct (e.g.
 * {@code while (cond);}, {@code for (...);}, {@code if (cond);}) are intentional and are
 * left untouched, because removing them would alter the program's semantics.
 *
 * <p>Processing is rooted at top-level type declarations (those whose parent is the
 * {@link CompilationUnit}). Nested and anonymous types are handled automatically via
 * recursive source-text scanning within the outer type's replacement region.
 */
public class UnnecessarySemicolonOperation implements ASTOperation {

  private static final String EXTENDED_RANGES_KEY =
      UnnecessarySemicolonOperation.class.getName() + ".extendedRanges";

  @Override
  public void run(CompilationUnit compilationUnit, ASTNode node, ASTRewrite rewriter)
      throws IOException, MalformedTreeException, BadLocationException {

    if (!(node instanceof AbstractTypeDeclaration)) {
      return;
    }
    if (!(node.getParent() instanceof CompilationUnit)) {
      return;
    }
    AbstractTypeDeclaration typeDecl = (AbstractTypeDeclaration) node;

    String source = (String) compilationUnit.getProperty(CompilationUnitProperty.SOURCE);
    if (source == null) {
      return;
    }

    // Install the TargetSourceRangeComputer on the first visit to any top-level type in this
    // file. It is consulted at rewriteAST() time to obtain the replacement region for each
    // node; for types with a trailing ';' we extend the region to cover that character too.
    ensureRangeComputerInstalled(compilationUnit, rewriter);

    List<int[]> commentRanges = buildCommentRanges(compilationUnit);
    List<int[]> deletions = new ArrayList<>();
    collectBlockLevelDeletions(source, typeDecl, deletions, commentRanges);
    collectSwitchCaseDeletions(source, typeDecl, deletions, commentRanges);
    collectTryWithResourcesTrailingSemicolons(source, typeDecl, deletions, commentRanges);
    collectClassLevelSemicolons(source, typeDecl, deletions, commentRanges);

    int typeStart = typeDecl.getStartPosition();
    int typeLength = typeDecl.getLength();
    int postTypeSemicolon = findPostTypeSemicolon(source, typeStart + typeLength, commentRanges);

    int extEndMutable = typeStart + typeLength;
    if (postTypeSemicolon >= 0) {
      extEndMutable = postTypeSemicolon + 1;
      deletions.add(semiColonDeletionRange(source, typeStart, postTypeSemicolon));
      @SuppressWarnings("unchecked")
      Map<ASTNode, int[]> extendedRanges =
          (Map<ASTNode, int[]>) compilationUnit.getProperty(EXTENDED_RANGES_KEY);
      extendedRanges.put(typeDecl, new int[]{typeStart, extEndMutable - typeStart});
    }
    final int extEnd = extEndMutable;

    if (deletions.isEmpty()) {
      return;
    }

    // Sort from end to start so each deletion's offset stays valid after prior deletions.
    deletions.sort((a, b) -> Integer.compare(b[0], a[0]));

    StringBuilder sb = new StringBuilder(source.substring(typeStart, extEnd));

    for (int[] deletion : deletions) {
      int relStart = deletion[0] - typeStart;
      int len = deletion[1];
      if (relStart >= 0 && relStart + len <= sb.length()) {
        sb.delete(relStart, relStart + len);
      }
    }

    ASTNode placeholder = rewriter.createStringPlaceholder(sb.toString(), typeDecl.getNodeType());
    rewriter.replace(typeDecl, placeholder, null);
  }

  /**
   * Installs a {@link TargetSourceRangeComputer} on {@code rewriter} on the first call for a
   * given file. The computer widens the replacement region for any type declaration that has a
   * registered post-brace extension; for all other nodes it falls back to the default behaviour.
   *
   * <p>The per-file extension map is stored as a property on the {@link CompilationUnit} so
   * that later {@link #run} calls (for additional top-level types in the same file) can add
   * entries without overwriting the computer that was already set.
   */
  @SuppressWarnings("unchecked")
  private void ensureRangeComputerInstalled(CompilationUnit compilationUnit, ASTRewrite rewriter) {
    if (compilationUnit.getProperty(EXTENDED_RANGES_KEY) != null) {
      return;
    }
    Map<ASTNode, int[]> extendedRanges = new HashMap<>();
    compilationUnit.setProperty(EXTENDED_RANGES_KEY, extendedRanges);
    rewriter.setTargetSourceRangeComputer(new TargetSourceRangeComputer() {
      @Override
      public SourceRange computeSourceRange(ASTNode n) {
        int[] range = extendedRanges.get(n);
        if (range != null) {
          return new SourceRange(range[0], range[1]);
        }
        return super.computeSourceRange(n);
      }
    });
  }

  // -------------------------------------------------------------------------
  // Block-level empty statements
  // -------------------------------------------------------------------------

  /**
   * Collects deletion ranges for {@link EmptyStatement} nodes directly inside a {@link Block}.
   * Statements whose parent is a control-flow construct (e.g. {@code if (x);}) are excluded
   * because removing them would change the programme's behaviour.
   */
  private void collectBlockLevelDeletions(String source, AbstractTypeDeclaration typeDecl,
      List<int[]> deletions, List<int[]> commentRanges) {
    int outerStart = typeDecl.getStartPosition();
    typeDecl.accept(new ASTVisitor() {
      @Override
      public boolean visit(EmptyStatement emptyStatement) {
        if (emptyStatement.getParent() instanceof Block) {
          int pos = emptyStatement.getStartPosition();
          if (!isInComment(pos, commentRanges)) {
            deletions.add(semiColonDeletionRange(source, outerStart, pos));
          }
        }
        return false;
      }
    });
  }

  // -------------------------------------------------------------------------
  // Switch-case empty statements
  // -------------------------------------------------------------------------

  /**
   * Collects deletion ranges for {@link EmptyStatement} nodes inside {@link SwitchStatement}
   * or {@link SwitchExpression} case lists.
   */
  private void collectSwitchCaseDeletions(String source, AbstractTypeDeclaration typeDecl,
      List<int[]> deletions, List<int[]> commentRanges) {
    int outerStart = typeDecl.getStartPosition();
    typeDecl.accept(new ASTVisitor() {
      @Override
      public boolean visit(EmptyStatement emptyStatement) {
        ASTNode parent = emptyStatement.getParent();
        if (parent instanceof SwitchStatement || parent instanceof SwitchExpression) {
          int pos = emptyStatement.getStartPosition();
          if (!isInComment(pos, commentRanges)) {
            deletions.add(semiColonDeletionRange(source, outerStart, pos));
          }
        }
        return false;
      }
    });
  }

  // -------------------------------------------------------------------------
  // Try-with-resources trailing semicolons (SonarJava S2959 core detection)
  // -------------------------------------------------------------------------

  /**
   * Mirrors SonarJava's S2959 check: when the number of separators in a try-with-resources
   * resource list equals the number of resources, the last separator is unnecessary.
   *
   * <p>In SonarJava terms: {@code separators.size() == resources.size()} triggers a report.
   * In JDT terms: any {@code ;} found between the end of the last resource expression and
   * the closing {@code )} of the resource list is an unnecessary trailing separator.
   */
  @SuppressWarnings("unchecked")
  private void collectTryWithResourcesTrailingSemicolons(String source,
      AbstractTypeDeclaration typeDecl, List<int[]> deletions, List<int[]> commentRanges) {
    int outerStart = typeDecl.getStartPosition();
    typeDecl.accept(new ASTVisitor() {
      @Override
      public boolean visit(TryStatement tryStatement) {
        List<Expression> resources = tryStatement.resources();
        if (resources.isEmpty()) {
          return true;
        }
        Expression lastResource = resources.get(resources.size() - 1);
        int searchFrom = lastResource.getStartPosition() + lastResource.getLength();

        // Scan for the closing ')' of the resource list; any ';' found on the way is trailing.
        for (int k = searchFrom; k < source.length(); k++) {
          char c = source.charAt(k);
          if (c == ')') {
            break;
          }
          if (c == ';' && !isInComment(k, commentRanges)) {
            deletions.add(semiColonDeletionRange(source, outerStart, k));
          }
        }
        return true;
      }
    });
  }

  // -------------------------------------------------------------------------
  // Class-level semicolons (invisible to the AST, found via source scanning)
  // -------------------------------------------------------------------------

  /**
   * Collects all class-level {@code ;} positions by scanning source-text gaps between
   * consecutive {@code bodyDeclarations()} entries in the given top-level type and all
   * nested/anonymous types at any depth.
   *
   * <p>For {@link EnumDeclaration} nodes the scan of the opening gap (before the first body
   * declaration) starts <em>after</em> the required {@code ;} that separates enum constants
   * from body declarations, to avoid incorrectly flagging that separator as unnecessary.
   */
  private void collectClassLevelSemicolons(String source, AbstractTypeDeclaration typeDecl,
      List<int[]> deletions, List<int[]> commentRanges) {
    int outerStart = typeDecl.getStartPosition();

    // For a top-level enum the scan must start after the required ';' that separates the
    // enum constants from the body declarations, just as for nested enums (see visitor below).
    // Passing 0 tells scanTypeBodyGaps to start immediately after the opening '{', which
    // would incorrectly include the enum constants region and their required ';'.
    int topLevelScanStart = (typeDecl instanceof EnumDeclaration)
        ? enumBodyScanStart(source, (EnumDeclaration) typeDecl)
        : 0;
    scanTypeBodyGaps(source, outerStart,
        typeDecl.getStartPosition(),
        typeDecl.getStartPosition() + typeDecl.getLength() - 1,
        typeDecl.bodyDeclarations(),
        topLevelScanStart,
        deletions,
        commentRanges);

    // Recurse into every nested named type and every anonymous class at any depth.
    typeDecl.accept(new ASTVisitor() {

      @Override
      public boolean visit(TypeDeclaration nested) {
        if (nested != typeDecl) {
          scanTypeBodyGaps(source, outerStart,
              nested.getStartPosition(),
              nested.getStartPosition() + nested.getLength() - 1,
              nested.bodyDeclarations(),
              0,
              deletions,
              commentRanges);
        }
        return true;
      }

      @Override
      public boolean visit(EnumDeclaration nested) {
        if (nested == typeDecl) {
          return true;
        }
        int scanStart = enumBodyScanStart(source, nested);
        scanTypeBodyGaps(source, outerStart,
            nested.getStartPosition(),
            nested.getStartPosition() + nested.getLength() - 1,
            nested.bodyDeclarations(),
            scanStart,
            deletions,
            commentRanges);
        return true;
      }

      @Override
      public boolean visit(AnnotationTypeDeclaration nested) {
        scanTypeBodyGaps(source, outerStart,
            nested.getStartPosition(),
            nested.getStartPosition() + nested.getLength() - 1,
            nested.bodyDeclarations(),
            0,
            deletions,
            commentRanges);
        return true;
      }

      @Override
      public boolean visit(RecordDeclaration nested) {
        scanTypeBodyGaps(source, outerStart,
            nested.getStartPosition(),
            nested.getStartPosition() + nested.getLength() - 1,
            nested.bodyDeclarations(),
            0,
            deletions,
            commentRanges);
        return true;
      }

      /**
       * {@link AnonymousClassDeclaration#getStartPosition()} is the opening {@code \{},
       * so the pre-declarations scan starts at the very next character.
       */
      @Override
      public boolean visit(AnonymousClassDeclaration acd) {
        scanTypeBodyGaps(source, outerStart,
            acd.getStartPosition(),
            acd.getStartPosition() + acd.getLength() - 1,
            acd.bodyDeclarations(),
            acd.getStartPosition() + 1,
            deletions,
            commentRanges);
        return true;
      }
    });
  }

  /**
   * Scans source-text gaps of a single type body for stray {@code ;} characters.
   *
   * <p>Three regions are covered:
   * <ol>
   *   <li>The gap between the opening {@code \{} and the first body declaration.</li>
   *   <li>Gaps between adjacent body declarations.</li>
   *   <li>The gap between the last body declaration and the closing {@code \}}.</li>
   * </ol>
   *
   * <p>The opening {@code \{} is located by scanning from {@code nodeStart} up to
   * {@code firstDeclOrClose}, taking the last occurrence found. This search boundary
   * deliberately excludes any {@code \{} that belongs to a method or initialiser body,
   * preventing those from being misidentified as the type body's opening brace.
   *
   * @param nodeStart               start position of the type node (or the {@code \{} for
   *                                anonymous class declarations)
   * @param lastCharPos             position of the closing {@code \}}
   * @param rawBodyDecls            the type's {@code bodyDeclarations()} list
   * @param preDeclarationsScanStart if positive and greater than {@code openBrace + 1}, the
   *                                pre-declarations gap scan begins here instead of immediately
   *                                after the opening brace; used for enums to skip past the
   *                                required separator between constants and body declarations
   */
  @SuppressWarnings("unchecked")
  private void scanTypeBodyGaps(String source, int outerStart, int nodeStart, int lastCharPos,
      List<?> rawBodyDecls, int preDeclarationsScanStart, List<int[]> deletions,
      List<int[]> commentRanges) {

    List<ASTNode> bodyDecls = (List<ASTNode>) rawBodyDecls;

    int firstDeclOrClose = bodyDecls.isEmpty()
        ? lastCharPos
        : bodyDecls.get(0).getStartPosition();

    // Locate the type body's opening '{' by searching only up to firstDeclOrClose.
    // This prevents '{' characters inside method bodies from being picked up.
    int openBrace = -1;
    for (int k = nodeStart; k < firstDeclOrClose && k < source.length(); k++) {
      if (source.charAt(k) == '{') {
        openBrace = k;  // take last '{' found (handles '{' in annotation arguments)
      }
    }
    if (openBrace < 0) {
      return;
    }

    // Gap before the first body declaration (or the entire body if empty).
    int gapStart = (preDeclarationsScanStart > openBrace + 1)
        ? preDeclarationsScanStart
        : openBrace + 1;
    scanGap(source, outerStart, gapStart, firstDeclOrClose, deletions, commentRanges);

    // Gaps between adjacent body declarations and after the last one.
    for (int i = 0; i < bodyDecls.size(); i++) {
      ASTNode decl = bodyDecls.get(i);
      int gapFrom = decl.getStartPosition() + decl.getLength();
      int gapEnd = (i + 1 < bodyDecls.size())
          ? bodyDecls.get(i + 1).getStartPosition()
          : lastCharPos;
      scanGap(source, outerStart, gapFrom, gapEnd, deletions, commentRanges);
    }
  }

  /** Scans a source range for {@code ;} characters outside comments and records deletion ranges. */
  private void scanGap(String source, int outerStart, int gapStart, int gapEnd,
      List<int[]> deletions, List<int[]> commentRanges) {
    for (int j = gapStart; j < gapEnd && j < source.length(); j++) {
      if (source.charAt(j) == ';' && !isInComment(j, commentRanges)) {
        deletions.add(semiColonDeletionRange(source, outerStart, j));
      }
    }
  }

  // -------------------------------------------------------------------------
  // Post-type-brace semicolons
  // -------------------------------------------------------------------------

  /**
   * Scans the source text starting at {@code searchFrom} for a {@code ;} that appears
   * immediately after the closing {@code \}} of a top-level type declaration (with only
   * whitespace in between).  Returns the absolute position of the {@code ;}, or {@code -1}
   * if the first non-whitespace character encountered is not a {@code ;} or if the
   * semicolon falls inside a comment.
   */
  private int findPostTypeSemicolon(String source, int searchFrom, List<int[]> commentRanges) {
    for (int k = searchFrom; k < source.length(); k++) {
      char c = source.charAt(k);
      if (c == ';') {
        return isInComment(k, commentRanges) ? -1 : k;
      }
      if (c != ' ' && c != '\t' && c != '\r' && c != '\n') {
        return -1;
      }
    }
    return -1;
  }

  // -------------------------------------------------------------------------
  // Comment-range helpers
  // -------------------------------------------------------------------------

  /**
   * Builds a list of {@code [start, end)} ranges from every comment in the compilation unit.
   */
  @SuppressWarnings("unchecked")
  private List<int[]> buildCommentRanges(CompilationUnit compilationUnit) {
    List<int[]> ranges = new ArrayList<>();
    for (Comment comment : (List<Comment>) compilationUnit.getCommentList()) {
      int start = comment.getStartPosition();
      ranges.add(new int[]{start, start + comment.getLength()});
    }
    return ranges;
  }

  /** Returns {@code true} if {@code pos} falls within any comment range. */
  private boolean isInComment(int pos, List<int[]> commentRanges) {
    for (int[] range : commentRanges) {
      if (pos >= range[0] && pos < range[1]) {
        return true;
      }
    }
    return false;
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  /**
   * Returns the position from which to begin scanning the pre-body-declarations gap of an
   * {@link EnumDeclaration}. This is the position immediately after the {@code ;} that
   * separates the enum constants from the body declarations (if any constants exist), or
   * the position immediately after the opening {@code \{} (if the constants list is empty).
   *
   * <p>Skipping over the required separator prevents the scanner from incorrectly flagging
   * it as an unnecessary semicolon.
   */
  @SuppressWarnings("unchecked")
  private int enumBodyScanStart(String source, EnumDeclaration enumDecl) {
    List<ASTNode> constants = (List<ASTNode>) enumDecl.enumConstants();
    int declEnd = enumDecl.getStartPosition() + enumDecl.getLength() - 1;

    int searchFrom;
    if (!constants.isEmpty()) {
      ASTNode last = constants.get(constants.size() - 1);
      searchFrom = last.getStartPosition() + last.getLength();
    } else {
      // No constants — find the opening '{' and start from the character after it.
      searchFrom = -1;
      for (int k = enumDecl.getStartPosition(); k < declEnd && k < source.length(); k++) {
        if (source.charAt(k) == '{') {
          searchFrom = k + 1;
        }
      }
      if (searchFrom < 0) {
        return enumDecl.getStartPosition();
      }
    }

    // Advance past the required ';' (if present) — that semicolon is not stray.
    for (int k = searchFrom; k < declEnd && k < source.length(); k++) {
      char c = source.charAt(k);
      if (c == ';') {
        return k + 1;
      }
      if (c == '}') {
        return k;
      }
    }
    return searchFrom;
  }

  /**
   * Computes the source range to delete for a single {@code ;} at position {@code semiPos}.
   *
   * <ul>
   *   <li>If the {@code ;} is the only non-whitespace character on its source line, the
   *       entire line including the preceding newline is deleted so no blank line is left
   *       behind. Both LF ({@code \n}) and CRLF ({@code \r\n}) endings are handled.</li>
   *   <li>If the {@code ;} shares a line with other non-whitespace content (e.g. {@code \};}),
   *       only the single {@code ;} character is deleted.</li>
   * </ul>
   *
   * @param outerStart lower bound for the backward line-start scan (the outer type's start)
   * @param semiPos    absolute position of the {@code ;} in {@code source}
   * @return {@code int[2]} where {@code [0]} is the start of the deletion and {@code [1]}
   *         is the length to delete
   */
  private int[] semiColonDeletionRange(String source, int outerStart, int semiPos) {
    boolean soloOnLine = true;
    for (int k = semiPos - 1; k > outerStart; k--) {
      char c = source.charAt(k);
      if (c == '\n') {
        break;
      }
      if (c != ' ' && c != '\t' && c != '\r') {
        soloOnLine = false;
        break;
      }
    }

    if (!soloOnLine) {
      return new int[]{semiPos, 1};
    }

    // Solo on line: delete from the preceding newline (inclusive) through the ';'.
    int delStart = semiPos;
    for (int k = semiPos - 1; k >= outerStart; k--) {
      if (source.charAt(k) == '\n') {
        delStart = k;
        if (k > outerStart && source.charAt(k - 1) == '\r') {
          delStart = k - 1;
        }
        break;
      }
    }
    return new int[]{delStart, semiPos - delStart + 1};
  }
}
