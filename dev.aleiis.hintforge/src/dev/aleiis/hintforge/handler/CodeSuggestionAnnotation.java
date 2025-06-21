package dev.aleiis.hintforge.handler;

import org.eclipse.jface.text.source.Annotation;

public class CodeSuggestionAnnotation extends Annotation {
	
	public static final String TYPE = "dev.aleiis.hintforge.annotations.codesuggestion";
	
	public CodeSuggestionAnnotation(String text) {
		super(TYPE, false, text);
	}
}
