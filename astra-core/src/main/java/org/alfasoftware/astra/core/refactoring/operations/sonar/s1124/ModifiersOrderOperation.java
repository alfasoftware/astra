package org.alfasoftware.astra.core.refactoring.operations.sonar.s1124;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.alfasoftware.astra.core.utils.ASTOperation;
import org.alfasoftware.astra.core.utils.AstraUtils;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Refactoring operation implementing SonarQube rule java:S1124
 * "Modifiers should be declared in the correct order".
 *
 * <p>The Java Language Specification recommends the following canonical modifier order:
 * <ol>
 *   <li>Annotations</li>
 *   <li>{@code public}</li>
 *   <li>{@code protected}</li>
 *   <li>{@code private}</li>
 *   <li>{@code abstract}</li>
 *   <li>{@code default}</li>
 *   <li>{@code static}</li>
 *   <li>{@code final}</li>
 *   <li>{@code transient}</li>
 *   <li>{@code volatile}</li>
 *   <li>{@code synchronized}</li>
 *   <li>{@code native}</li>
 *   <li>{@code strictfp}</li>
 * </ol>
 *
 * <p>When keyword modifiers appear out of this order on any body declaration (type, method, field,
 * etc.) they are reordered in-place. Annotations are left at their original positions.
 *
 * <p>Examples:
 * <ul>
 *   <li>{@code static public void foo()}      &rarr; {@code public static void foo()}</li>
 *   <li>{@code final static int X = 1}        &rarr; {@code static final int X = 1}</li>
 *   <li>{@code final public static String S}  &rarr; {@code public static final String S}</li>
 *   <li>{@code synchronized static void bar()} &rarr; {@code static synchronized void bar()}</li>
 * </ul>
 *
 * <p>Mirrors the detection logic of SonarJava's {@code ModifiersOrderCheck}.
 */
public class ModifiersOrderOperation implements ASTOperation {

  private static final Logger log = LoggerFactory.getLogger(ModifiersOrderOperation.class);

  /**
   * Canonical modifier order per the Java Language Specification (JLS 17).
   * Modifiers not present in this list are placed at the end in their original relative order.
   */
  static final List<Modifier.ModifierKeyword> MODIFIER_ORDER = List.of(
      Modifier.ModifierKeyword.PUBLIC_KEYWORD,
      Modifier.ModifierKeyword.PROTECTED_KEYWORD,
      Modifier.ModifierKeyword.PRIVATE_KEYWORD,
      Modifier.ModifierKeyword.ABSTRACT_KEYWORD,
      Modifier.ModifierKeyword.DEFAULT_KEYWORD,
      Modifier.ModifierKeyword.STATIC_KEYWORD,
      Modifier.ModifierKeyword.FINAL_KEYWORD,
      Modifier.ModifierKeyword.TRANSIENT_KEYWORD,
      Modifier.ModifierKeyword.VOLATILE_KEYWORD,
      Modifier.ModifierKeyword.SYNCHRONIZED_KEYWORD,
      Modifier.ModifierKeyword.NATIVE_KEYWORD,
      Modifier.ModifierKeyword.STRICTFP_KEYWORD
  );

  @Override
  public void run(CompilationUnit compilationUnit, ASTNode node, ASTRewrite rewriter)
      throws IOException, MalformedTreeException, BadLocationException {

    if (!(node instanceof BodyDeclaration)) {
      return;
    }

    BodyDeclaration bodyDecl = (BodyDeclaration) node;

    @SuppressWarnings("unchecked")
    List<IExtendedModifier> allModifiers = bodyDecl.modifiers();

    List<Modifier> keywordModifiers = allModifiers.stream()
        .filter(m -> m instanceof Modifier)
        .map(m -> (Modifier) m)
        .collect(Collectors.toList());

    if (keywordModifiers.size() < 2) {
      return;
    }

    List<Modifier.ModifierKeyword> currentOrder = keywordModifiers.stream()
        .map(Modifier::getKeyword)
        .collect(Collectors.toList());

    List<Modifier.ModifierKeyword> sortedOrder = new ArrayList<>(currentOrder);
    sortedOrder.sort(Comparator.comparingInt(k -> {
      int idx = MODIFIER_ORDER.indexOf(k);
      return idx == -1 ? MODIFIER_ORDER.size() : idx;
    }));

    if (currentOrder.equals(sortedOrder)) {
      return;
    }

    log.info("Reordering modifiers in [{}]: {} -> {}",
        AstraUtils.getNameForCompilationUnit(compilationUnit),
        currentOrder.stream().map(k -> k.toString()).collect(Collectors.joining(" ")),
        sortedOrder.stream().map(k -> k.toString()).collect(Collectors.joining(" ")));

    ChildListPropertyDescriptor modifiersProperty = findModifiersProperty(bodyDecl);
    if (modifiersProperty == null) {
      return;
    }

    AST ast = compilationUnit.getAST();
    ListRewrite listRewrite = rewriter.getListRewrite(bodyDecl, modifiersProperty);

    for (int i = 0; i < keywordModifiers.size(); i++) {
      Modifier.ModifierKeyword target = sortedOrder.get(i);
      if (!currentOrder.get(i).equals(target)) {
        listRewrite.replace(keywordModifiers.get(i), ast.newModifier(target), null);
      }
    }
  }

  /**
   * Finds the "modifiers" ChildListPropertyDescriptor for the given node by inspecting
   * its structural properties. This avoids hard-coding per-subclass property constants
   * (MethodDeclaration.MODIFIERS2_PROPERTY, FieldDeclaration.MODIFIERS2_PROPERTY, etc.)
   * which are distinct objects even though they share the same semantics.
   */
  @SuppressWarnings("rawtypes")
  private static ChildListPropertyDescriptor findModifiersProperty(ASTNode node) {
    for (Object prop : node.structuralPropertiesForType()) {
      if (prop instanceof ChildListPropertyDescriptor) {
        ChildListPropertyDescriptor clpd = (ChildListPropertyDescriptor) prop;
        if ("modifiers".equals(clpd.getId())) {
          return clpd;
        }
      }
    }
    return null;
  }
}
