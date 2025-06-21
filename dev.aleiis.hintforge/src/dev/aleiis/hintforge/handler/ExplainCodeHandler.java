package dev.aleiis.hintforge.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.xtext.ui.editor.XtextEditor;

import dev.aleiis.hintforge.Activator;
import dev.aleiis.hintforge.assistant.CodeExplanationAssistant;
import dev.aleiis.hintforge.model.DslProfile;
import dev.aleiis.hintforge.preference.PreferenceManager;
import dev.aleiis.hintforge.view.CodeExplanationView;

public class ExplainCodeHandler extends AbstractHandler {

	private IPreferenceStore preferenceStore = null;

	private IWorkbenchPage workbenchPage = null;
	private Shell parentShell = null;
	private XtextEditor editor = null;
	private ISourceViewer sourceViewer = null;
	private IDocument document = null;
	
	private CodeExplanationView view;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException { 
		this.preferenceStore = Activator.getDefault().getPreferenceStore();
		if (isReady()) {
			openPreferencePage();
			return null;
		}
		
		this.parentShell = HandlerUtil.getActiveShell(event);;
		this.workbenchPage = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage();

		IEditorPart editorPart = HandlerUtil.getActiveEditor(event);
		if (!(editorPart instanceof XtextEditor)) {
			System.out.println("ERROR: IEditorPart is not instance of XtextEditor");
			return null;
		}

		this.editor = (XtextEditor) editorPart;
		this.sourceViewer = this.editor.getInternalSourceViewer();
		this.document = sourceViewer.getDocument();
		
		try {
			this.view = (CodeExplanationView) this.workbenchPage.showView("dev.aleiis.hintforge.view.codeexplanation");
		} catch (PartInitException e) {
			MessageDialog.openError(parentShell, "Explain Code Error", "The Explain Code view could not be initialized.");
		}
		
		this.view.setStatusMessage("Generating explanation...");

		ISelection selection = this.editor.getSelectionProvider().getSelection();
		String code = this.document.get();
		String selectedCode = null;

		if (selection instanceof ITextSelection textSel && !textSel.getText().isEmpty()) {
			selectedCode = textSel.getText();
		}

		String explanation = getAssistantExplanation(code, selectedCode);
		if (explanation == null) {
			this.view.setStatusMessage("Error while generating the explanation.");
			return null;
		}

		this.view.showExplanation(explanation);
		this.view.setStatusMessage("Explanation generated.");
		
		return null;
	}

	private String getAssistantExplanation(String code, String selectedCode) {		
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
		
		CodeExplanationAssistant assistant = new CodeExplanationAssistant(apiKey, dslProfile, PreferenceManager.getInstance().getModelName());
		assistant.setEmbeddingStore(PreferenceManager.getInstance().getEmbeddingStore(dslProfile));
		String explanation = assistant.explain(code, selectedCode);
		if (explanation == null) {
			Shell shell = editor.getSite().getShell();
			MessageDialog.openError(shell, "Error", "Failed reading the DSL files.");
			return null;
		}
		
		return explanation;
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
	
	private void openPreferencePageErrorDialog(String title, String message) {
		PreferenceDialog dialog = openPreferencePage();
		MessageDialog.openError(dialog.getShell(), title, message);
	}
 }
