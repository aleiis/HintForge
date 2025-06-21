package dev.aleiis.hintforge.handler;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.dialogs.ListDialog;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.xtext.ui.editor.XtextEditor;

import dev.aleiis.hintforge.Activator;
import dev.aleiis.hintforge.assistant.IdentifierSuggestionAssistant;
import dev.aleiis.hintforge.model.DslProfile;
import dev.aleiis.hintforge.preference.PreferenceManager;

public class SuggestIdentifiersHandler extends AbstractHandler {

	private IPreferenceStore preferenceStore = null;

	private Shell parentShell = null;
	private XtextEditor editor = null;
	private ISourceViewer sourceViewer = null;
	private IDocument document = null;

	private int offset = -1;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		this.preferenceStore = Activator.getDefault().getPreferenceStore();
		if (isReady()) {
			openPreferencePage();
			return null;
		}

		this.parentShell = HandlerUtil.getActiveWorkbenchWindowChecked(event).getShell();

		IEditorPart editorPart = HandlerUtil.getActiveEditor(event);
		if (!(editorPart instanceof XtextEditor)) {
			System.out.println("ERROR: IEditorPart is not instance of XtextEditor");
			return null;
		}

		this.editor = (XtextEditor) editorPart;
		this.sourceViewer = this.editor.getInternalSourceViewer();
		this.document = sourceViewer.getDocument();
		this.offset = sourceViewer.getSelectedRange().x;

		List<String> suggestions = getIdentifierSuggestions();
		if (suggestions == null || suggestions.isEmpty()) {
			MessageDialog.openError(parentShell, "Identifier Suggestion Error", "Suggestions list is empty.");
			return null;
		}
		
		ListDialog dialog = new ListDialog(parentShell);
		dialog.setContentProvider(ArrayContentProvider.getInstance());
		dialog.setLabelProvider(new LabelProvider());
		dialog.setInput(suggestions);
		dialog.setTitle("Suggest Identifiers");
		dialog.setMessage("Select one identifier:");
		dialog.setHelpAvailable(false);
		dialog.setInitialSelections(new Object[] { suggestions.get(0) });

		if (dialog.open() == Window.OK) {
		    Object[] result = dialog.getResult();
		    if (result != null && result.length == 1) {
		        String identifier = (String) result[0];
		        insertIdentifier(identifier);
		    }
		}
		
		return null;
	}

	private void insertIdentifier(String identifier) {	
		try {
			document.replace(offset, 0, identifier);
			StyledText styledText = sourceViewer.getTextWidget();
			styledText.setCaretOffset(offset + identifier.length());
			styledText.showSelection();
		} catch (BadLocationException e) {
				MessageDialog.openError(
				    parentShell,
				    "Identifier Suggestion Error",
				    "Failed inserting the identifier suggestion in the document. "
				    + "Invalid offset in this document. Insertion offset may be out of range."
				);
		}

	}
	
	private String getFileExtension() {
		IEditorInput input = editor.getEditorInput();
        if (input instanceof FileEditorInput) {
        	IFile file = ((FileEditorInput) input).getFile();
            return file.getFileExtension();	
        }
        return null;
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
	
	private void openPreferencePageErrorDialog(String title, String message) {
		PreferenceDialog dialog = openPreferencePage();
		MessageDialog.openError(dialog.getShell(), title, message);
	}

	private List<String> getIdentifierSuggestions() {
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
		
		IdentifierSuggestionAssistant assistant = new IdentifierSuggestionAssistant(apiKey, dslProfile, PreferenceManager.getInstance().getModelName());
		assistant.setEmbeddingStore(PreferenceManager.getInstance().getEmbeddingStore(dslProfile));
		List<String> identifiers = assistant.suggest(code, offset);
		if (identifiers == null) {
			Shell shell = editor.getSite().getShell();
			MessageDialog.openError(shell, "Error", "Failed reading the DSL files.");
			return null;
		}
		
		return identifiers;
	}
	
	private boolean isReady() {
		return preferenceStore.getString("API_KEY").isBlank();
	}

	private PreferenceDialog openPreferencePage() {
		PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(parentShell, "dev.aleiis.hintforge.preferencepage",
				null, null);
		if (dialog != null) {
			dialog.setBlockOnOpen(false);
			dialog.open();
			return dialog;
		}
		return null;
	}
}
