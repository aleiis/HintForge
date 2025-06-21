package dev.aleiis.hintforge.preference;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.IExpansionListener;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;

import dev.aleiis.hintforge.model.ContextAwareCompletionConfig;
import dev.aleiis.hintforge.model.DslProfile;
import dev.aleiis.hintforge.model.EmbeddableExternalFile;
import dev.aleiis.hintforge.model.ExternalFile;
import dev.aleiis.hintforge.model.IdentifierSuggestionConfig;

public class DslProfileDialog extends Dialog {

	private Text nameText;
	private Text descText;
	private Text xtextFileText;
	private Text extensionText;
	private Text standaloneClassText;

	private ListViewer examplesListViewer;
	private List<ExternalFile> examples = new ArrayList<>();

	private ListViewer documentationListViewer;
	private List<EmbeddableExternalFile> documentation = new ArrayList<>();

	private final DslProfile original;
	private DslProfile result;

	private List<DslProfile> existingProfiles;

	private Text codeCompletionFewShotText;
	private Spinner codeCompletionFixAttemptsSpinner;
	private Spinner codeCompletionGenerationAttemptsSpinner;

	private Text identifierSuggestionFewShotText;
	private Spinner identifierSuggestionGenerationAttemptsSpinner;

	public DslProfileDialog(Shell parentShell, DslProfile original, List<DslProfile> existingProfiles) {
		super(parentShell);
		this.original = original;
		this.existingProfiles = existingProfiles;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 10;
		layout.marginHeight = 10;
		container.setLayout(layout);
		
		addGeneralGroup(container);
		addExampleScriptsGroup(container);
		addDocumentationGroup(container);
		addAdvancedOptions(parent, container);

		loadContents();

		return container;
	}

	private void addGeneralGroup(Composite container) {
		Group generalGroup = new Group(container, SWT.SHADOW_ETCHED_OUT);
		generalGroup.setText("General");
		generalGroup.setLayout(new GridLayout(3, false));
		generalGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		new Label(generalGroup, SWT.NONE).setText("Name*:");
		nameText = new Text(generalGroup, SWT.BORDER);
		nameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		new Label(generalGroup, SWT.NONE).setText("Description:");
		descText = new Text(generalGroup, SWT.BORDER);
		descText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		new Label(generalGroup, SWT.NONE).setText("Xtext File*:");
		xtextFileText = new Text(generalGroup, SWT.BORDER | SWT.READ_ONLY);
		xtextFileText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		Button browseButton = new Button(generalGroup, SWT.PUSH);
		browseButton.setText("Browse...");
		browseButton.addListener(SWT.Selection, e -> openXtextFileDialog());

		Label extensionLabel = new Label(generalGroup, SWT.NONE);
		extensionLabel.setText("File Extension*:");
		extensionLabel
				.setToolTipText("Specifies the file extension for which this profile will be used  (e.g. '.mydsl').");
		extensionText = new Text(generalGroup, SWT.BORDER);
		extensionText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		
		new Label(generalGroup, SWT.NONE).setText("StandaloneSetup*:");
		standaloneClassText = new Text(generalGroup, SWT.BORDER);
		standaloneClassText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		Button selectClassButton = new Button(generalGroup, SWT.PUSH);
		selectClassButton.setText("Select...  ");
		selectClassButton.addListener(SWT.Selection, e -> {
			StandaloneSetupSelectorDialog dialog = new StandaloneSetupSelectorDialog(getShell());
			if (dialog.open() == Window.OK) {
				String selectedClass = dialog.getSelectedStandaloneSetupClass();
				if (selectedClass != null) {
					standaloneClassText.setText(selectedClass);
				}
			}
		});

	}

	private void addExampleScriptsGroup(Composite container) {
		Group examplesGroup = new Group(container, SWT.SHADOW_ETCHED_OUT);
		examplesGroup.setText("Example Scripts");
		examplesGroup.setLayout(new GridLayout(2, false));
		examplesGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		examplesListViewer = new ListViewer(examplesGroup, SWT.BORDER | SWT.V_SCROLL);
		examplesListViewer.setContentProvider(ArrayContentProvider.getInstance());
		examplesListViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof ExternalFile) {
					ExternalFile example = (ExternalFile) element;
					return example.getOriginalFileName() + " ("
							+ ((example.getRelativePath() != null) ? example.getRelativePath()
									: example.getSourcePath())
							+ ")";
				}
				return super.getText(element);
			}
		});
		GridData listData = new GridData(SWT.FILL, SWT.FILL, true, true);
		examplesListViewer.getList().setLayoutData(listData);

		Composite buttons = new Composite(examplesGroup, SWT.NONE);
		buttons.setLayout(new GridLayout());
		buttons.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));

		Button addButton = new Button(buttons, SWT.PUSH);
		addButton.setText("Add");
		addButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		addButton.addListener(SWT.Selection, e -> addExample());

		Button removeButton = new Button(buttons, SWT.PUSH);
		removeButton.setText("Remove");
		removeButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		removeButton.addListener(SWT.Selection, e -> removeSelectedExample());
	}

	private void addExample() {
		FileDialog dialog = new FileDialog(getShell(), SWT.OPEN | SWT.MULTI);
		dialog.setText("Add Example Script");
		String file = dialog.open();
		if (file != null) {
			String[] selectedFiles = dialog.getFileNames();
			String directory = dialog.getFilterPath();
			for (String fileName : selectedFiles) {
				String fullPath = Path.of(directory, fileName).toString();
				ExternalFile example = new ExternalFile(fullPath, fileName);
				examples.add(example);
			}
			refreshExamplesList();
		}
	}

	private void removeSelectedExample() {
		IStructuredSelection sel = (IStructuredSelection) examplesListViewer.getSelection();
		if (!sel.isEmpty()) {
			Object elem = sel.getFirstElement();
			if (elem instanceof ExternalFile example) {
				examples.remove(example);
				refreshExamplesList();
			}
		}
	}

	private void refreshExamplesList() {
		examplesListViewer.setInput(examples.toArray());
	}

	private void addDocumentationGroup(Composite container) {
		Group documentationGroup = new Group(container, SWT.SHADOW_ETCHED_OUT);
		documentationGroup.setText("Documentation");
		documentationGroup.setLayout(new GridLayout(2, false));
		documentationGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		documentationListViewer = new ListViewer(documentationGroup, SWT.BORDER | SWT.V_SCROLL);
		documentationListViewer.setContentProvider(ArrayContentProvider.getInstance());
		documentationListViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof EmbeddableExternalFile doc) {
					return doc.getOriginalFileName() + " ("
							+ ((doc.getRelativePath() != null) ? doc.getRelativePath() : doc.getSourcePath()) + ")";
				}
				return super.getText(element);
			}
		});
		GridData listData = new GridData(SWT.FILL, SWT.FILL, true, true);
		documentationListViewer.getList().setLayoutData(listData);

		Composite buttons = new Composite(documentationGroup, SWT.NONE);
		buttons.setLayout(new GridLayout());
		buttons.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));

		Button addButton = new Button(buttons, SWT.PUSH);
		addButton.setText("Add");
		addButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		addButton.addListener(SWT.Selection, e -> addDocumentation());

		Button removeButton = new Button(buttons, SWT.PUSH);
		removeButton.setText("Remove");
		removeButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		removeButton.addListener(SWT.Selection, e -> removeSelectedDoc());
	}

	private void addDocumentation() {
		FileDialog dialog = new FileDialog(getShell(), SWT.OPEN | SWT.MULTI);
		dialog.setText("Add Documentation File(s)");
		dialog.setFilterNames(new String[] { "Text Files" });
		dialog.setFilterExtensions(new String[] { "*.txt;*.md;*.mdx;*.json;*.xml" });
		String file = dialog.open();
		if (file != null) {
			String[] selectedFiles = dialog.getFileNames();
			String directory = dialog.getFilterPath();
			for (String fileName : selectedFiles) {
				String fullPath = Path.of(directory, fileName).toString();
				EmbeddableExternalFile doc = new EmbeddableExternalFile(fullPath, fileName);
				documentation.add(doc);
			}
			refreshDocumentationList();
		}
	}

	private void removeSelectedDoc() {
		IStructuredSelection sel = (IStructuredSelection) documentationListViewer.getSelection();
		if (!sel.isEmpty()) {
			Object elem = sel.getFirstElement();
			if (elem instanceof EmbeddableExternalFile doc) {
				documentation.remove(doc);
				refreshDocumentationList();
			}
		}
	}

	private void refreshDocumentationList() {
		documentationListViewer.setInput(documentation);
	}

	private void addAdvancedOptions(Composite parent, Composite container) {
		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		toolkit.setBackground(parent.getBackground());

		ExpandableComposite expandable = toolkit.createExpandableComposite(container,
				ExpandableComposite.TWISTIE | ExpandableComposite.CLIENT_INDENT);
		expandable.setText("Advanced Options");
		expandable.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		Composite advancedOptions = toolkit.createComposite(expandable);
		advancedOptions.setLayout(new GridLayout(2, false));

		expandable.setClient(advancedOptions);
		expandable.addExpansionListener(new IExpansionListener() {
			@Override
			public void expansionStateChanging(ExpansionEvent e) {
			}

			@Override
			public void expansionStateChanged(ExpansionEvent e) {
				container.layout(true, true);
			}
		});

		// <----- "Code Completion" group
		Group codeCompletionGroup = new Group(advancedOptions, SWT.NONE);
		codeCompletionGroup.setText("Code Completion");
		codeCompletionGroup.setLayout(new GridLayout(2, false));
		codeCompletionGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

		Label codeCompletionFewShotLabel = new Label(codeCompletionGroup, SWT.NONE);
		codeCompletionFewShotLabel.setText("Few-shot Prompt:");

		codeCompletionFewShotText = new Text(codeCompletionGroup, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = 80;
		codeCompletionFewShotText.setLayoutData(gd);

		Label fixAttemptsLabel = new Label(codeCompletionGroup, SWT.NONE);
		fixAttemptsLabel.setText("Fix Attempts:");

		codeCompletionFixAttemptsSpinner = new Spinner(codeCompletionGroup, SWT.BORDER);
		codeCompletionFixAttemptsSpinner.setMinimum(0);
		codeCompletionFixAttemptsSpinner.setMaximum(100);

		Label generationAttemptsLabel = new Label(codeCompletionGroup, SWT.NONE);
		generationAttemptsLabel.setText("Generation Attempts:");

		codeCompletionGenerationAttemptsSpinner = new Spinner(codeCompletionGroup, SWT.BORDER);
		codeCompletionGenerationAttemptsSpinner.setMinimum(1);
		codeCompletionGenerationAttemptsSpinner.setMaximum(100);

		// <----- "Identifier Suggestion" group
		Group identifierSuggestionGroup = new Group(advancedOptions, SWT.NONE);
		identifierSuggestionGroup.setText("Identifier Suggestion");
		identifierSuggestionGroup.setLayout(new GridLayout(2, false));
		identifierSuggestionGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

		Label identifierSuggestionFewShotLabel = new Label(identifierSuggestionGroup, SWT.NONE);
		identifierSuggestionFewShotLabel.setText("Few-shot Prompt:");

		identifierSuggestionFewShotText = new Text(identifierSuggestionGroup,
				SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
		identifierSuggestionFewShotText.setLayoutData(gd);

		new Label(identifierSuggestionGroup, SWT.NONE).setText("Generation Attempts:");

		identifierSuggestionGenerationAttemptsSpinner = new Spinner(identifierSuggestionGroup, SWT.BORDER);
		identifierSuggestionGenerationAttemptsSpinner.setMinimum(1);
		identifierSuggestionGenerationAttemptsSpinner.setMaximum(100);
	}

	private void loadContents() {
		if (original != null) {

			String name = original.getName();
			nameText.setText(name != null ? name : "");

			String description = original.getDescription();
			descText.setText(description != null ? description : "");

			ExternalFile xtextFile = original.getXtextFile();
			if (xtextFile != null) {
				String relativePath = xtextFile.getRelativePath();
				String sourcePath = xtextFile.getSourcePath();
				xtextFileText.setText(relativePath != null ? relativePath : sourcePath);
			} else {
				xtextFileText.setText("");
			}

			String extension = original.getFileExtension();
			extensionText.setText(extension != null ? extension : "");
			
			String standaloneSetup = original.getStandaloneSetupClass();
			standaloneClassText.setText(standaloneSetup != null ? standaloneSetup : "");

			List<ExternalFile> scriptExamples = original.getScriptExamples();
			if (scriptExamples != null) {
				examples = new ArrayList<>(scriptExamples);
			}
			refreshExamplesList();

			List<EmbeddableExternalFile> documentation = original.getEmbeddingManager().getFiles();
			if (documentation != null) {
				this.documentation = new ArrayList<>(documentation);
			}
			refreshDocumentationList();

			ContextAwareCompletionConfig codeCompletionConfig = original.getCodeCompletionConfig();
			codeCompletionFewShotText.setText(codeCompletionConfig.getFewShotPrompt());
			codeCompletionFixAttemptsSpinner.setSelection(codeCompletionConfig.getMaxFixAttempts());
			codeCompletionGenerationAttemptsSpinner.setSelection(codeCompletionConfig.getMaxGenerationAttempts());

			IdentifierSuggestionConfig identifierSuggestionConfig = original.getIdentifierSuggestionConfig();
			identifierSuggestionFewShotText.setText(identifierSuggestionConfig.getFewShotPrompt());
			identifierSuggestionGenerationAttemptsSpinner
					.setSelection(identifierSuggestionConfig.getMaxGenerationAttempts());
		} else {
			codeCompletionFewShotText.setText(ContextAwareCompletionConfig.DEFAULT_FEW_SHOT_PROMPT);
			codeCompletionFixAttemptsSpinner.setSelection(2);
			codeCompletionGenerationAttemptsSpinner.setSelection(1);

			identifierSuggestionFewShotText.setText(IdentifierSuggestionConfig.DEFAULT_FEW_SHOT_PROMPT);
			identifierSuggestionGenerationAttemptsSpinner.setSelection(1);
		}
	}

	private void openXtextFileDialog() {
		FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
		dialog.setText("Select Xtext File");
		dialog.setFilterExtensions(new String[] { "*.xtext" });
		dialog.setFilterNames(new String[] { "Xtext Files (*.xtext)" });
		String selected = dialog.open();
		if (selected != null) {
			xtextFileText.setText(selected);
		}
	}

	@Override
	protected void okPressed() {
		String name = nameText.getText().trim();
		String description = descText.getText();
		String xtextFilePath = xtextFileText.getText();
		String fileExtension = extensionText.getText();
		String standaloneSetup = standaloneClassText.getText();
		String codeCompletionFewShotPrompt = codeCompletionFewShotText.getText();
		String identifierSuggestionFewShotPrompt = identifierSuggestionFewShotText.getText();

		// Validate values
		if (name.isBlank()) {
			showError("Name cannot be empty.");
			return;
		}
		if (existingProfiles.stream().anyMatch(profile -> profile.getName().equals(name))) {
			showError("A profile with this name already exists.");
			return;
		}

		if (xtextFilePath.isEmpty()) {
			showError("Xtext File path cannot be empty.");
			return;
		}

		if (fileExtension.isBlank()) {
			showError("File Extension cannot be empty.");
			return;
		}
		if (fileExtension.contains(".")) {
			showError("The file extension must be specified without the leading dot.");
			return;
		}
		if (existingProfiles.stream().anyMatch(profile -> profile.getFileExtension().equals(fileExtension))) {
			showError("A profile with this file extension already exists.");
			return;
		}
		
		if (standaloneSetup.isBlank()) {
			showError("The StandaloneSetup class name cannot be empty.");
			return;
		}

		if (!codeCompletionFewShotPrompt.contains("{{code}}")
				|| !codeCompletionFewShotPrompt.contains("{{instruction}}")) {
			showError(
					"The few-shot prompt for the Code Completion task does not contain the {{code}} or {{instruction}} tag.");
			return;
		}

		if (!identifierSuggestionFewShotPrompt.contains("{{code}}")) {
			showError("The few-shot prompt for the Identifier Suggestion task does not contain the {{code}} tag.");
			return;
		}

		// Save values
		ExternalFile xtextFile;
		if (original != null && !original.getXtextFile().getSourcePath().equals(xtextFilePath)) {
			xtextFile = original.getXtextFile();
		} else {
			xtextFile = new ExternalFile(xtextFilePath, Path.of(xtextFilePath).getFileName().toString());
		}

		result = new DslProfile(name, fileExtension, xtextFile);
		result.setDescription(description);
		result.setStandaloneSetupClass(standaloneSetup);
		result.setScriptExamples(examples);
		if (original != null)
			result.setEmbeddingManager(original.getEmbeddingManager());
		result.getEmbeddingManager().setFiles(documentation);
		ContextAwareCompletionConfig codeCompletionConfig = result.getCodeCompletionConfig();
		codeCompletionConfig.setFewShotPrompt(codeCompletionFewShotPrompt);
		codeCompletionConfig.setMaxFixAttempts(codeCompletionFixAttemptsSpinner.getSelection());
		codeCompletionConfig.setMaxGenerationAttempts(codeCompletionGenerationAttemptsSpinner.getSelection());
		IdentifierSuggestionConfig identifierSuggestionConfig = result.getIdentifierSuggestionConfig();
		identifierSuggestionConfig.setFewShotPrompt(identifierSuggestionFewShotPrompt);
		identifierSuggestionConfig.setMaxGenerationAttempts(identifierSuggestionGenerationAttemptsSpinner.getSelection());

		super.okPressed();
	}

	@Override
	protected Point getInitialSize() {
		return new Point(800, 730);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		if (original == null) {
			newShell.setText("New DSL Profile");
		} else {
			newShell.setText("Edit DSL Profile");
		}
	}

	public DslProfile getResult() {
		return result;
	}

	private void showError(String message) {
		MessageDialog.openError(getShell(), "Invalid Profile", message);
	}
}
