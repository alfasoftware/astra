package org.alfasoftware.astra.core.utils;

import java.io.IOException;
import java.util.function.Predicate;

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


	/**
	 * Returns a predicate applied to the raw content of a source file before it is parsed.
	 * Returning {@code false} for some content is a promise that {@link #run} would do nothing for any node of that file.
	 *
	 * <p>A file is only parsed if at least one operation of the use case accepts its content,
	 * so this lets cheap text checks avoid the cost of parsing files that an operation cannot apply to.
	 * The default implementation accepts every file, which is always safe.
	 */
	default Predicate<String> getContentPrefilteringPredicate() {
		return content -> true;
	}
}
