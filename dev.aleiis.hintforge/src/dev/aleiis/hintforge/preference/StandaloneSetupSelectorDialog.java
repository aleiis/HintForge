package dev.aleiis.hintforge.preference;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.osgi.framework.Bundle;

public class StandaloneSetupSelectorDialog extends Dialog {

	private static List<String> EXCLUDED_BUNDLES = new ArrayList<>();
	
	private ComboViewer bundleViewer;
	private ComboViewer classViewer;

	private final Map<Bundle, List<String>> bundleClasses = new LinkedHashMap<>();
	private String selectedClassName;

	public StandaloneSetupSelectorDialog(Shell parentShell) {
		super(parentShell);
	}

	public String getSelectedStandaloneSetupClass() {
		return selectedClassName;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		getShell().setText("Select StandaloneSetup class");

		Composite container = (Composite) super.createDialogArea(parent);
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = 20;
		layout.marginHeight = 20;
		container.setLayout(layout);

		Label bundleLabel = new Label(container, SWT.NONE);
		bundleLabel.setText("Bundle:");
		bundleLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		bundleViewer = new ComboViewer(container, SWT.READ_ONLY);
		bundleViewer.getCombo().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		bundleViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Bundle bundle) {
					return bundle.getSymbolicName();
				}
				return super.getText(element);
			}
		});
		bundleViewer.setContentProvider(ArrayContentProvider.getInstance());
		bundleViewer.addSelectionChangedListener(event -> updateClassViewer());

		Label classLabel = new Label(container, SWT.NONE);
		classLabel.setText("StandaloneSetup class:");
		classLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		classViewer = new ComboViewer(container, SWT.READ_ONLY);
		classViewer.getCombo().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		classViewer.setLabelProvider(new LabelProvider());
		classViewer.setContentProvider(ArrayContentProvider.getInstance());

		loadBundlesAndClasses();
		bundleViewer.setInput(bundleClasses.keySet());

		return container;
	}

	@Override
	protected void okPressed() {
		IStructuredSelection selection = classViewer.getStructuredSelection();
		if (!selection.isEmpty()) {
			selectedClassName = selection.getFirstElement().toString();
		}
		super.okPressed();
	}

	private void updateClassViewer() {
		IStructuredSelection selection = bundleViewer.getStructuredSelection();
		if (!selection.isEmpty() && selection.getFirstElement() instanceof Bundle selectedBundle) {
			List<String> classes = bundleClasses.getOrDefault(selectedBundle, List.of());
			classViewer.setInput(classes);
			classViewer.getCombo().select(0);
			return;
		}
		classViewer.setInput(Collections.emptyList());
	}

	private void loadBundlesAndClasses() {
		Bundle[] bundles = Platform.getBundle("org.eclipse.core.runtime").getBundleContext().getBundles();

		for (Bundle bundle : bundles) {
			String symbolicName = bundle.getSymbolicName();
			String requireBundle = bundle.getHeaders().get("Require-Bundle");
			if (requireBundle == null || !requireBundle.contains("org.eclipse.xtext")) {
				continue;
			}
			
			if (EXCLUDED_BUNDLES.contains(symbolicName)) {
				continue;
			}

			List<String> classNames = new ArrayList<>();
			try {
				Enumeration<URL> entries = bundle.findEntries("/", "*.class", true);
				if (entries == null)
					continue;

				while (entries.hasMoreElements()) {
					URL classUrl = entries.nextElement();
					String path = classUrl.getPath();
					if (path.endsWith("StandaloneSetup.class")) {
						String symbolicPath = symbolicName.replace('.', '/');
						String className = path.replaceFirst(".*?" + symbolicPath, symbolicPath)
	                            .replace("/", ".")
	                            .replace(".class", "");
						try {
							bundle.loadClass(className);
							classNames.add(className);
						} catch (Throwable ignored) {
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			if (!classNames.isEmpty()) {
				Collections.sort(classNames);
				bundleClasses.put(bundle, classNames);
			}
		}
	}
}
