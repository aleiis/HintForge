package dev.aleiis.hintforge.assistant;

import java.io.IOException;
import java.nio.file.Path;

import dev.aleiis.hintforge.model.DslProfile;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

interface CodeExplanationService {

	@SystemMessage("""
			## INSTRUCTIONS
			- You are a code explanation assistant for Domain Specific Languages.
			- Your task is to provide a clear, concise and accurate explanation of a code snippet or a whole program.
			- If a specific fragment is marked between the [[START]] and [[END]] tags, you must explain only that fragment.
			- Never mention the tags [[START]] and [[END]].
			- If there are no markers, explain the entire code as a whole.
			- Your explanation should be in natural language and accessible to a developer who understands programming,
			but might not be familiar with this specific DSL or language.

			## DSL
			Name: {{name}}
			Description: {{description}}
			Grammar:
			{{grammar}}
			""")
	String explain(@UserMessage String userMessage, @V("name") String name, @V("description") String description,
			@V("grammar") String grammar);
}

public class CodeExplanationAssistant extends Assistant {

	

	public CodeExplanationAssistant(Path homeFolder, String openAiApiKey, DslProfile dsl, String modelName) {
		super(homeFolder, openAiApiKey, dsl, modelName);
	}

	public CodeExplanationAssistant(String openAiApiKey, DslProfile dsl, String modelName) {
		super(openAiApiKey, dsl, modelName);
	}

	public CodeExplanationAssistant(String openAiApiKey, DslProfile dsl) {
		super(openAiApiKey, dsl);
	}

	/**
	 * Explains the whole code.
	 *
	 * @param code The full code to be explained.
	 * 
	 * @return A explanation of the full code in NL
	 */
	public String explain(String code) {
		return this.explain(code, null);
	}

	/**
	 * Explains a selected code fragment in the context of the full code.
	 *
	 * @param fullCode  The full code.
	 * @param selection The code fragment selected to explain.
	 * 
	 * @return Natural language explanation.
	 */
	public String explain(String fullCode, String selection) {
		DslProfile dsl = getDslProfile();
		
		String grammar;
		try {
			grammar = dsl.getXtextFile().readContent(getHomeFolder());
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		String markedCode;
		if (selection != null && !selection.isBlank()) {
			markedCode = fullCode.replace(selection, " [[START]] " + selection + " [[END]] ");
		} else {
			markedCode = fullCode;
		}

		CodeExplanationService assistant = buildAssistant(CodeExplanationService.class, 0.6);
		return assistant.explain(markedCode, dsl.getName(), dsl.getDescription(), grammar);
	}
}
