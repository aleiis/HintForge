package dev.aleiis.hintforge.view;

import java.io.FileWriter;
import java.io.IOException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;

public class CodeExplanationView extends ViewPart {

	private Text explanationText;
	private Label statusLabel;

	@Override
	public void createPartControl(Composite parent) {
		GridLayout parentLayout = new GridLayout(1, false);
		parentLayout.marginWidth = 10;
		parentLayout.marginHeight = 10;
		parent.setLayout(parentLayout);

		explanationText = new Text(parent, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.MULTI);
		explanationText.setEditable(false);
		GridData areaData = new GridData(SWT.FILL, SWT.FILL, true, true);
		areaData.heightHint = 300;
		explanationText.setLayoutData(areaData);
		
		Composite bottom = new Composite(parent, SWT.NONE);
		bottom.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false));
		bottom.setLayout(new GridLayout(2, false));

		statusLabel = new Label(bottom, SWT.NONE);
		statusLabel.setText("Press Ctrl+H+3 to generate an explanation.");
		GridData statusData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		statusLabel.setLayoutData(statusData);

		Composite buttonBar = new Composite(bottom, SWT.NONE);
		buttonBar.setLayout(new RowLayout(SWT.HORIZONTAL));
		GridData buttonBarData = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
		buttonBar.setLayoutData(buttonBarData);

		Button copyButton = new Button(buttonBar, SWT.PUSH);
		copyButton.setText(" Copy ");
		copyButton.addListener(SWT.Selection, e -> copyToClipboard());

		Button saveButton = new Button(buttonBar, SWT.PUSH);
		saveButton.setText(" Save as... ");
		saveButton.addListener(SWT.Selection, e -> saveToFile());
	}

	public void showExplanation(String explanation) {
		explanationText.setText(explanation);
		statusLabel.setText("Explicación generada.");
	}

	private void copyToClipboard() {
		String content = explanationText.getText();
		if (content != null && !content.isEmpty()) {
			Clipboard clipboard = new Clipboard(Display.getDefault());
			clipboard.setContents(new Object[] { content }, new Transfer[] { TextTransfer.getInstance() });
			clipboard.dispose();
			statusLabel.setText("Copied to the clipboard.");
		} else {
			statusLabel.setText("Nothing to copy.");
		}
	}

	private void saveToFile() {
		FileDialog dialog = new FileDialog(getSite().getShell(), SWT.SAVE);
		dialog.setText("Save explanation as...");
		dialog.setFilterExtensions(new String[] { "*.txt" });
		dialog.setFileName("explanation.txt");
		String filePath = dialog.open();
		if (filePath != null) {
			try (FileWriter writer = new FileWriter(filePath, false)) {
				writer.write(explanationText.getText());
				statusLabel.setText("Saved in " + filePath);
			} catch (IOException ex) {
				statusLabel.setText("Save failed: " + ex.getMessage());
			}
		} else {
			statusLabel.setText("Save failed: empty file path");
		}
	}
	
	public void setStatusMessage(String message) {
		statusLabel.setText(message);
	}

	@Override
	public void setFocus() {
		explanationText.setFocus();
	}

}
