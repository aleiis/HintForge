package dev.aleiis.hintforge.preference;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import dev.aleiis.hintforge.Activator;
import dev.aleiis.hintforge.model.DslProfile;
import dev.langchain4j.model.openai.OpenAiChatModelName;

public class HintForgePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private Text homeFolderText;

	private Text apiKeyText;
	private Combo modelCombo;
	private String[] modelComboValues;

	private ListViewer configListViewer;
	private List<DslProfile> dslProfiles = new ArrayList<>();

	private PreferenceManager preferenceManager = PreferenceManager.getInstance();
	private IPreferenceStore store;

	public HintForgePreferencePage() {
		store = Activator.getDefault().getPreferenceStore();
		setPreferenceStore(store);
		setDescription(
				"General settings for Xtext DSLs software development with HintForge. Required files are denoted by '*'.");
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, true));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		addGeneralSettingsGroup(main);
		addLlmApiGroup(main);
		addDslProfileGroup(main);

		return main;
	}

	private void addGeneralSettingsGroup(Composite parent) {
		Group group = new Group(parent, SWT.SHADOW_ETCHED_OUT);
		group.setText("General Settings");
		group.setLayout(new GridLayout(4, false));
		group.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		// Home Folder
		new Label(group, SWT.NONE).setText("Home Folder*:");

		homeFolderText = new Text(group, SWT.BORDER | SWT.READ_ONLY);
		homeFolderText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Button openButton = new Button(group, SWT.PUSH);
		openButton.setText("Open in Explorer");
		openButton.addListener(SWT.Selection, event -> {
			try {
				Desktop.getDesktop().open(new File(homeFolderText.getText()));
			} catch (Exception exception) {
				MessageDialog.openError(getShell(), getTitle(),
						"Error while opening the HintForge Home folder in the System Explorer. "
								+ exception.toString());
			}
		});

		Button changeLocationButton = new Button(group, SWT.PUSH);
		changeLocationButton.setText("Change Location");
		changeLocationButton.addListener(SWT.Selection, event -> {
			DirectoryDialog dialog = new DirectoryDialog(getShell());
			dialog.setText("Select new location");
			String newFolderPath = dialog.open();
			if (newFolderPath != null) {
				homeFolderText.setText(newFolderPath);
			}
		});

		Label note = new Label(group, SWT.NONE);
		note.setText("Note: Restore Defaults does not affect the Home Folder setting.");
		note.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));

		homeFolderText.setText(preferenceManager.getHomeFolder().toString());
	}

	private void addLlmApiGroup(Composite parent) {
		Group group = new Group(parent, SWT.SHADOW_ETCHED_OUT);
		group.setText("LLM API configuration");
		group.setLayout(new GridLayout(2, false));
		group.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));

		new Label(group, SWT.NONE).setText("OpenAI API Key*:");
		apiKeyText = new Text(group, SWT.SINGLE | SWT.BORDER | SWT.H_SCROLL);
		GridData apiKeyData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		apiKeyData.widthHint = 250;
		apiKeyText.setLayoutData(apiKeyData);
		apiKeyText.setText(preferenceManager.getApiKey());
		
		new Label(group, SWT.NONE).setText("OpenAI Model:");
		modelComboValues = Stream.of(OpenAiChatModelName.values()).map(String::valueOf).toArray(String[]::new);
		modelCombo = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
		modelCombo.setItems(modelComboValues);
		selectModelComboValue(preferenceManager.getModelName());
	}

	private void addDslProfileGroup(Composite parent) {
		Group group = new Group(parent, SWT.SHADOW_ETCHED_OUT);
		group.setText("DSL profiles");
		group.setLayout(new GridLayout(2, false));
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		configListViewer = new ListViewer(group, SWT.BORDER | SWT.V_SCROLL);
		configListViewer.setContentProvider(ArrayContentProvider.getInstance());
		configListViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof DslProfile) {
					DslProfile profile = (DslProfile) element;
					return profile.getName();
				}
				return super.getText(element);
			}
		});
		configListViewer.setInput(dslProfiles);
		GridData listData = new GridData(SWT.FILL, SWT.FILL, true, true);
		listData.heightHint = 120;
		configListViewer.getList().setLayoutData(listData);

		Composite buttons = new Composite(group, SWT.NONE);
		buttons.setLayout(new GridLayout());
		buttons.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));

		createButton(buttons, "New", new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				openProfileDialog(null);
			}
		});
		createButton(buttons, "Edit", new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection sel = (IStructuredSelection) configListViewer.getSelection();
				if (!sel.isEmpty())
					openProfileDialog((DslProfile) sel.getFirstElement());
			}
		});
		createButton(buttons, "Remove", new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection sel = (IStructuredSelection) configListViewer.getSelection();
				if (!sel.isEmpty()) {
					dslProfiles.remove(sel.getFirstElement());
					refreshDslProfileList();
				}
			}
		});

		loadDslProfiles();
	}

	private void createButton(Composite parent, String text, SelectionAdapter listener) {
		Button button = new Button(parent, SWT.PUSH);
		button.setText(text);
		button.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		button.addSelectionListener(listener);
	}

	private void openProfileDialog(DslProfile toEdit) {
		DslProfileDialog dialog = new DslProfileDialog(getShell(), toEdit, getDslProfiles(toEdit));
		if (dialog.open() == Window.OK) {
			DslProfile result = dialog.getResult();
			if (toEdit != null) {
				dslProfiles.remove(toEdit);	
			}
			dslProfiles.add(result);
			refreshDslProfileList();
		}
	}

	private void refreshDslProfileList() {
		configListViewer.setInput(dslProfiles.toArray());
	}

	private void loadDslProfiles() {
		DslProfile[] profiles = preferenceManager.getDslProfiles();
		if (profiles != null) {
			dslProfiles = new ArrayList<>(Arrays.asList(profiles));
		}
		refreshDslProfileList();
	}

	private List<DslProfile> getDslProfiles(DslProfile toExclude) {
		List<DslProfile> copy = new ArrayList<>(dslProfiles);
		if (toExclude != null) {
			copy.remove(toExclude);
		}
		return List.copyOf(copy); // immutable
	}
	
	private void selectModelComboValue(String modelName) {
		int selectedIndex = Arrays.asList(modelComboValues).indexOf(modelName);
		if (selectedIndex >= 0) {
			modelCombo.select(selectedIndex);
		} else {
			modelCombo.select(0);
		}
	}

	@Override
	public boolean performOk() {
		String homeFolder = homeFolderText.getText();
		if (!homeFolder.equals(preferenceManager.getHomeFolder().toString())) {
			boolean isSuccessfulMove = preferenceManager.moveHomeFolder(Path.of(homeFolder));
			if (!isSuccessfulMove) {
				MessageDialog.openError(getShell(), getTitle(), "The selected folder is already in use by other application. You must select an available one.");
				return false;
			}
		}
		
		if (!preferenceManager.isHomeFolderAvailable()) {
			MessageDialog.openError(getShell(), getTitle(), "The actual home folder is in use by other application. You must change it and select an available one.");
			return false;
		}
				
		preferenceManager.setApiKey(apiKeyText.getText());
		
		int selectedIndex = modelCombo.getSelectionIndex();
		if (selectedIndex >= 0) {
			preferenceManager.setModelName(modelComboValues[selectedIndex]);
		}
		
		try {
			preferenceManager.saveDslProfiles(dslProfiles.toArray(new DslProfile[0]));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return true;
	}

	@Override
	protected void performDefaults() {
		apiKeyText.setText(store.getDefaultString("API_KEY"));
		selectModelComboValue(store.getDefaultString("MODEL_NAME"));

		String dslProfilesJson = store.getDefaultString("DSL_PROFILES");
		if (dslProfilesJson != null && !dslProfilesJson.isEmpty()) {
			dslProfiles = new ArrayList<>(Arrays.asList(DslProfile.fromJson(dslProfilesJson)));
		}
		refreshDslProfileList();

		super.performDefaults();
	}
}
