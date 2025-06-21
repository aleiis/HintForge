package dev.aleiis.hintforge.assistant;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.IResourceValidator;
import org.eclipse.xtext.validation.Issue;

import com.google.inject.Injector;

public class SyntaxVerifier {

    private final Injector injector;
    private final XtextResourceSet resourceSet;
    private final IResourceValidator validator;
    private final String fileExtension;

    public SyntaxVerifier(String standaloneSetupClassName, String fileExtension) {
        this.fileExtension = fileExtension;

        try {
            Class<?> standaloneSetupClass = Class.forName(standaloneSetupClassName);
            Object standaloneSetupInstance = standaloneSetupClass.getDeclaredConstructor().newInstance();
            Method injectorMethod = standaloneSetupClass.getMethod("createInjectorAndDoEMFRegistration");
            this.injector = (Injector) injectorMethod.invoke(standaloneSetupInstance);
            this.resourceSet = injector.getInstance(XtextResourceSet.class);
            this.validator = injector.getInstance(IResourceValidator.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize SyntaxVerifier", e);
        }
    }

    public List<String> validate(String code, boolean excludeEtypeErrors) {
        if (injector == null || resourceSet == null || validator == null || fileExtension == null) {
            throw new IllegalStateException("SyntaxVerifier not configured properly.");
        }

        Resource resource = resourceSet.createResource(
            URI.createURI("dummy:/dummy_" + UUID.randomUUID().toString() + "." + fileExtension)
        );

        List<String> issues = new ArrayList<>();

        try {
            resource.load(new ByteArrayInputStream(code.getBytes()), null);
            List<Issue> allIssues = validator.validate(resource, CheckMode.ALL, null);
            if (!allIssues.isEmpty()) {
                Pattern etypeErrorPattern = Pattern.compile("^Couldn't resolve reference to");
                Pattern linkingErrorPattern = Pattern.compile("Linking$");
                for (Issue issue : allIssues) {
                    if (excludeEtypeErrors &&
                        issue.getCode() != null && linkingErrorPattern.matcher(issue.getCode()).find() &&
                        issue.getMessage() != null && etypeErrorPattern.matcher(issue.getMessage()).find()) {
                        continue;
                    }
                    issues.add(issueToString(issue));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return issues;
    }
    
    public String issueToString(Issue issue) {
    	StringBuilder result = new StringBuilder(issue.getSeverity().name());
		result.append(": ").append(issue.getMessage());
		result.append(" (");
		result.append(" line : ").append(issue.getLineNumber()).append("; column : ").append(issue.getColumn()).append(")");
		return result.toString();
    }
}