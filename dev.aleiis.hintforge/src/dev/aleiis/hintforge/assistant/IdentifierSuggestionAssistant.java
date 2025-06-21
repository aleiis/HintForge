package dev.aleiis.hintforge.assistant;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import dev.aleiis.hintforge.model.DslProfile;
import dev.aleiis.hintforge.model.IdentifierSuggestionConfig;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

interface IdentifierSuggestionService {

	@SystemMessage("""
			### INSTRUCTIONS
			- You are an identifier suggestion assistant for Domain Specific Languages.
			- Locate the marker `[[CURSOR]]` in the user’s code and return a **ranked list of possible identifiers** that could replace it.
			- Suggest meaningful names that the user can assign to a new element (such as a reference, attribute, or class), or use to reference an existing one.
			- Suggestions must conform to the naming rules of the DSL.
			- Your output must be only a list of identifiers, one per line, without any extra formatting or explanation.
			- Each line can contain only one identifier.
			- You are only allowed to provide suggestions for the DSL grammar you are trained on.

			## DSL
			Name: {{name}}
			Description: {{description}}
			Grammar:
			{{grammar}}

			## EXAMPLE SCRIPTS
			{{examples}}
			""")

	String chat(@UserMessage String userMessage, @V("name") String name, @V("description") String description,
			@V("grammar") String grammar, @V("examples") String examples, @V("code") String code);
}

public class IdentifierSuggestionAssistant extends Assistant {

	public IdentifierSuggestionAssistant(Path homeFolder, String openAiApiKey, DslProfile dsl, String modelName) {
		super(homeFolder, openAiApiKey, dsl, modelName);
	}

	public IdentifierSuggestionAssistant(String openAiApiKey, DslProfile dsl, String modelName) {
		super(openAiApiKey, dsl, modelName);
	}

	public IdentifierSuggestionAssistant(String openAiApiKey, DslProfile dsl) {
		super(openAiApiKey, dsl);
	}

	/**
	 * Method used to generate identifier suggestions.
	 * 
	 * @param code   Context of the code.
	 * @param offset Offset in the code where the identifier must be inserted.
	 * 
	 * @return A list with the suggested identifiers.
	 */
	public List<String> suggest(String code, int offset) {
		DslProfile dsl = getDslProfile();
		IdentifierSuggestionConfig config = dsl.getIdentifierSuggestionConfig();

		String grammar;
		try {
			grammar = dsl.getXtextFile().readContent(getHomeFolder());
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		String markedCode = code.substring(0, offset) + "[[CURSOR]]" + code.substring(offset);

		int maxGenerationAttempts = config.getMaxGenerationAttempts();

		IdentifierSuggestionService assistant = buildAssistant(IdentifierSuggestionService.class, 0.5);
		List<String> identifiers = new ArrayList<>();

		for (int generationAttempts = 1; identifiers.isEmpty()
				&& generationAttempts <= maxGenerationAttempts; generationAttempts++) {
			
			String response = assistant.chat(config.getFewShotPrompt(), dsl.getName(), dsl.getDescription(), grammar,
					buildExamplesString(), markedCode);
			identifiers = List.of(response.split("\\R"));
			identifiers = identifiers.stream().map(id -> trimContextOverlap(code, offset, id)).toList();
			identifiers = removeDuplicates(identifiers);
			
			if (generationAttempts < maxGenerationAttempts) {
				identifiers = verifyIdentifiers(code, offset, identifiers);	
			}
		}

		return identifiers;
	}

	private List<String> removeDuplicates(List<String> identifiers) {
		Set<String> ids = new LinkedHashSet<>();
		ids.addAll(identifiers);
		return new ArrayList<>(ids);
	}

	private List<String> verifyIdentifiers(String code, int offset, List<String> identifiers) {
		SyntaxVerifier verifier = getVerifier();
		List<String> result = new ArrayList<>();
		for (String identifier : identifiers) {
			String completedCode = code.substring(0, offset) + identifier + code.substring(offset);
			if (verifier.validate(completedCode, true).isEmpty()) {
				result.add(identifier);
			}
		}
		return result;
	}
}
