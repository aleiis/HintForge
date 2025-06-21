package dev.aleiis.hintforge.preference;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import dev.aleiis.hintforge.Activator;

public class HintForgePreferenceInitializer extends AbstractPreferenceInitializer {

    @Override
    public void initializeDefaultPreferences() {
        IPreferenceStore store = Activator.getDefault().getPreferenceStore();
        
//        store.setDefault("HOME_FOLDER", new File(System.getProperty("user.home"), ".hintforge").toString());
        
        IPath workspacePath = ResourcesPlugin.getWorkspace().getRoot().getLocation();
        store.setDefault("HOME_FOLDER", workspacePath.append(".metadata/.plugins/dev.aleiis.hintforge").toOSString());
        
        store.setDefault("MODEL_NAME", "gpt-4o-mini");
        store.setDefault("API_KEY", "");
        
        store.setDefault("DSL_PROFILES", "[]");
    }
}