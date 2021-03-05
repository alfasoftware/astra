package org.alfasoftware.astra.core.utils;

import java.io.IOException;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;

/**
 *  A visitor for ASTNodes which can specify analysis or refactoring tasks.
 *  In the case of refactoring tasks, the ASTRewrite can be used to record changes to write back to the compilation unit source file.
 */
public interface ASTOperation {

	void run(CompilationUnit compilationUnit, ASTNode node, ASTRewrite rewriter) throws IOException, MalformedTreeException, BadLocationException;
}
