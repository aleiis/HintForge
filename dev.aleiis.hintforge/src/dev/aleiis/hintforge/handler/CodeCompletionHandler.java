package dev.aleiis.hintforge.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.xtext.ui.editor.XtextEditor;

import dev.aleiis.hintforge.Activator;
import dev.aleiis.hintforge.assistant.ContextAwareCompletionAssistant;
import dev.aleiis.hintforge.model.DslProfile;
import dev.aleiis.hintforge.preference.PreferenceManager;

public class CodeCompletionHandler extends AbstractHandler {

	private ExecutionEvent event;
	private IPreferenceStore preferenceStore = null;

	private XtextEditor editor = null;
	private ISourceViewer sourceViewer = null;
	private IDocument document = null;
	private IAnnotationModel annotationModel = null;

	private CodeSuggestionAnnotation annotation = null;
	private String suggestionText = null;
	private int offset = -1;

	private Shell popupShell = null;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		this.event = event;
		this.preferenceStore = Activator.getDefault().getPreferenceStore();
		if (isReady()) {
			openPreferencePage();
			return null;
		}

		IEditorPart editorPart = HandlerUtil.getActiveEditor(event);
		if (!(editorPart instanceof XtextEditor)) {
			System.out.println("ERROR: IEditorPart is not instance of XtextEditor");
			return null;
		}

		this.editor = (XtextEditor) editorPart;
		this.sourceViewer = this.editor.getInternalSourceViewer();
		this.document = sourceViewer.getDocument();
		this.offset = sourceViewer.getSelectedRange().x;

		showInstructionPopup();
		return null;
	}

	private void showInstructionPopup() {
		StyledText styledText = sourceViewer.getTextWidget();
		int caretOffset = styledText.getCaretOffset();
		Point location = styledText.getLocationAtOffset(caretOffset);
		Point displayLocation = styledText.toDisplay(location);

		Shell parentShell = editor.getSite().getShell();
		Shell instructionShell = new Shell(parentShell, SWT.ON_TOP | SWT.TOOL);
		instructionShell.setLayout(new GridLayout(1, false));

		Text instructionText = new Text(instructionShell, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		instructionText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		instructionText.setSize(300, 100);

		// Pressing 'Esc' cancels the operation
		instructionText.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.ESC) {
					if (!instructionShell.isDisposed()) {
						instructionShell.dispose();
					}
				}
			}
		});

		Button generateButton = new Button(instructionShell, SWT.PUSH);
		generateButton.setText("Generate suggestion");
		generateButton.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));

		instructionShell.setDefaultButton(generateButton);

		generateButton.addListener(SWT.Selection, e -> {
			String userInstruction = instructionText.getText();
			instructionShell.dispose();

			suggestionText = getLLMSuggestion(userInstruction);
			if (suggestionText != null && !suggestionText.isEmpty()) {
				showSuggestionText();
				showSuggestionPopup();
			}
		});

		instructionShell.addShellListener(new ShellAdapter() {
			@Override
			public void shellDeactivated(ShellEvent e) {
				if (!instructionShell.isDisposed()) {
					instructionShell.dispose();
				}
			}
		});

		instructionShell.setSize(320, 160);
		instructionShell.setLocation(displayLocation.x, displayLocation.y - 45);
		instructionShell.open();
		instructionText.setFocus();
	}

	private void showSuggestionText() {
		this.annotationModel = sourceViewer.getAnnotationModel();

		// Place text
		try {
			document.replace(offset, 0, suggestionText);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}

		// Place annotation
		this.annotation = new CodeSuggestionAnnotation(suggestionText);
		Position position = new Position(offset, suggestionText.length());
		annotationModel.addAnnotation(this.annotation, position);
	}

	private void showSuggestionPopup() {
		if (popupShell != null && !popupShell.isDisposed()) {
			rejectSuggestion();
			return;
		}

		StyledText styledText = sourceViewer.getTextWidget();
		Point location = styledText.getLocationAtOffset(offset);
		Point displayLocation = styledText.toDisplay(location);

		Shell parentShell = editor.getSite().getShell();
		popupShell = new Shell(parentShell, SWT.ON_TOP | SWT.TOOL | SWT.NO_TRIM);
		popupShell.setLayout(new GridLayout(2, false));

		// Create buttons: Accept or Reject
		Button acceptButton = new Button(popupShell, SWT.PUSH);
		acceptButton.setText("✅ Accept");

		Button cancelButton = new Button(popupShell, SWT.PUSH);
		cancelButton.setText("❌ Reject");

		popupShell.addShellListener(new ShellAdapter() {
			@Override
			public void shellDeactivated(ShellEvent e) {
				rejectSuggestion();
				popupShell.dispose();
			}
		});

		acceptButton.addListener(SWT.Selection, e -> {
			acceptSuggestion();
			popupShell.dispose();
		});

		cancelButton.addListener(SWT.Selection, e -> {
			rejectSuggestion();
			popupShell.dispose();
		});

		popupShell.pack();
		popupShell.setLocation(displayLocation.x, displayLocation.y - 45);
		popupShell.open();

		acceptButton.setFocus();
		popupShell.setDefaultButton(acceptButton);
	}

	private void acceptSuggestion() {
		this.annotationModel.removeAnnotation(this.annotation);
		StyledText styledText = sourceViewer.getTextWidget();
		styledText.setCaretOffset(offset + suggestionText.length());
		styledText.showSelection();
	}

	private void rejectSuggestion() {
		try {
			this.document.replace(offset, suggestionText.length(), "");
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		this.annotationModel.removeAnnotation(this.annotation);
	}

	private boolean isReady() {
		return preferenceStore.getString("API_KEY").isBlank();
	}

	private PreferenceDialog openPreferencePage() {
		Shell shell = HandlerUtil.getActiveShell(event);
		PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(shell, "dev.aleiis.hintforge.preferencepage",
				null, null);
		if (dialog != null) {
			dialog.setBlockOnOpen(false);
			dialog.open();
			return dialog;
		}
		return null;
	}

	private void openPreferencePageErrorDialog(String title, String message) {
		PreferenceDialog dialog = openPreferencePage();
		MessageDialog.openError(dialog.getShell(), title, message);
	}

	private String getLLMSuggestion(String instruction) {
		String fileExtension = getFileExtension();
		if (fileExtension == null) {
			MessageDialog.openError(editor.getSite().getShell(), "Invalid Extension", "Could not get the file extension because the editor is not associated with any file.");
		}
		
		DslProfile dslProfile = getActiveDslProfile(fileExtension);
		if (dslProfile == null) {
			openPreferencePageErrorDialog("Invalid Profile", "No DSL profile found associated to that file extension!");
			return null;
		}

		String apiKey = PreferenceManager.getInstance().getApiKey();
		if (apiKey == null || apiKey.isEmpty()) {
			openPreferencePageErrorDialog("Invalid LLM API Key", "LLM API Key cannot be empty!");
			return null;
		}

		String code = document.get();

		ContextAwareCompletionAssistant assistant = new ContextAwareCompletionAssistant(apiKey, dslProfile, PreferenceManager.getInstance().getModelName());
		assistant.setEmbeddingStore(PreferenceManager.getInstance().getEmbeddingStore(dslProfile));

		String suggestion = assistant.suggest(instruction, code, offset);
		if (suggestion == null) {
			Shell shell = editor.getSite().getShell();
			MessageDialog.openError(shell, "Error", "Failed reading the DSL files.");
			return null;
		}

		return suggestion;
	}

	private DslProfile getActiveDslProfile(String fileExtension) {
		DslProfile[] profiles = PreferenceManager.getInstance().getDslProfiles();
		for (DslProfile profile : profiles) {
			if (fileExtension.equals(profile.getFileExtension())) {
				return profile;
			}
		}
		return null;
	}
	
	private String getFileExtension() {
		IEditorInput input = editor.getEditorInput();
        if (input instanceof FileEditorInput) {
        	IFile file = ((FileEditorInput) input).getFile();
            return file.getFileExtension();	
        }
        return null;
	}
}
