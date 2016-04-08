// The MIT License (MIT)
//
// Copyright (c) 2015 Arian Fornaris
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to permit
// persons to whom the Software is furnished to do so, subject to the
// following conditions: The above copyright notice and this permission
// notice shall be included in all copies or substantial portions of the
// Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
// NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
// OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
// USE OR OTHER DEALINGS IN THE SOFTWARE.
package phasereditor.assetpack.core;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.ui.IMemento;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import phasereditor.assetpack.core.AssetPackCore.PackDelta;

public final class AssetPackModel {
	static final String MEMENTO_KEY = "com.boniatillo.assetpack.ept.AssetPackModel";
	public static final String PROP_DIRTY = "dirty";
	public static final String PROP_FILE = "file";
	public static final String PROP_ASSET_KEY = "assetKey";
	protected List<AssetSectionModel> _sections;
	private IFile _file;
	private boolean _dirty;
	private List<AssetModel> _lastBuiltAssets;
	private String _backup;

	public AssetPackModel(IFile file) throws Exception {
		this(readJSON(file));
		_file = file;
	}

	public AssetPackModel(JSONObject jsonDoc) throws Exception {
		build(jsonDoc);
		_lastBuiltAssets = new ArrayList<>();
	}

	public void makeBackup() {
		_backup = toJSON();
	}

	public void recoverBackup() {
		if (_backup != null) {
			JSONObject obj = new JSONObject(_backup);
			try {
				build(obj);
				setDirty(false);
				PackDelta delta = new PackDelta();
				delta.add(this);
				AssetPackCore.firePacksChanged(delta);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	private void build(JSONObject jsonRoot) throws Exception {
		_sections = new ArrayList<>();

		@SuppressWarnings("rawtypes")
		Iterator keysIter = jsonRoot.keys();
		while (keysIter.hasNext()) {
			String key = (String) keysIter.next();
			if (key.equals("meta")) {
				// keep meta-data here
			} else {
				JSONArray array = jsonRoot.getJSONArray(key);
				AssetSectionModel section = new AssetSectionModel(key, array, this);
				addSection(section, false);
			}
		}
	}

	public PackDelta computeDelta(IPath filePath) {
		PackDelta delta = new PackDelta();

		if (filePath == null) {
			return delta;
		}

		Set<AssetModel> deletedAssets = new HashSet<>(_lastBuiltAssets);
		Set<AssetModel> currentAssets = new HashSet<>(getAssets());
		deletedAssets.removeIf(a -> currentAssets.contains(a));
		delta.getAssets().addAll(deletedAssets);

		IPath packPath = getFile().getFullPath();

		if (packPath.equals(filePath)) {
			delta.add(this);
			return delta;
		}

		for (AssetModel asset : currentAssets) {
			IFile[] lastUsedFiles = asset.getLastUsedFiles();
			IFile[] usedFiles = asset.getUsedFiles();
			IFile[][] allfiles = { lastUsedFiles, usedFiles };

			for (IFile[] files : allfiles) {
				for (IFile used : files) {
					if (used != null) {
						IPath usedPath = used.getFullPath();
						if (usedPath.equals(filePath)) {
							delta.add(asset);
						}
					}
				}
			}
		}

		return delta;
	}

	public List<IStatus> build() {
		List<IStatus> problems = new ArrayList<>();
		for (AssetSectionModel section : _sections) {
			for (AssetModel model : section.getAssets()) {
				model.build(problems);
			}
		}

		_lastBuiltAssets = getAssets();

		return problems;
	}

	private static JSONObject readJSON(IFile file) throws Exception {
		try (InputStream contents = file.getContents()) {
			JSONObject obj = new JSONObject(new JSONTokener(contents));
			return obj;
		}
	}

	public String toJSON() {
		JSONObject pack = new JSONObject();
		for (AssetSectionModel section : _sections) {
			section.writeSection(pack);
		}
		writeMeta(pack);
		return pack.toString(4);
	}

	private static void writeMeta(JSONObject pack) {
		JSONObject meta = new JSONObject();
		pack.put("meta", meta);
		meta.put("generated", Long.toString(System.currentTimeMillis()));
		meta.put("app", "Phaser Editor");
		meta.put("url", "http://phasereditor.boniatillo.com");
		meta.put("version", "1.0");
		meta.put("copyright", "Arian Fornaris (c) 2015,2016");
	}

	public void save(IProgressMonitor monitor) {
		String json = toJSON();
		try (ByteArrayInputStream source = new ByteArrayInputStream(json.getBytes())) {
			_file.setContents(source, false, false, monitor);
		} catch (IOException | CoreException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		makeBackup();
		setDirty(false);
	}

	public boolean isDirty() {
		return _dirty;
	}

	public void setDirty(boolean dirty) {
		if (_dirty != dirty) {
			_dirty = dirty;
			firePropertyChange(PROP_DIRTY);
		}
	}

	public boolean isOnWorkspace() {
		return AssetPackCore.getAssetPackModels().contains(this);
	}

	/**
	 * Get the file associated with this model.
	 * 
	 * @return The file or <code>null</code> if the file was deleted.
	 */
	public IFile getFile() {
		return _file;
	}

	public void setFile(IFile file) {
		_file = file;
		firePropertyChange(PROP_FILE);
	}

	public String getName() {
		if (_file == null) {
			return "<unkown>";
		}
		return _file.getName();
	}

	public IContainer getAssetsFolder() {
		if (_file == null) {
			return null;
		}
		return _file.getParent();
	}

	public List<IFile> discoverImageFiles() throws CoreException {
		return AssetPackCore.discoverImageFiles(getAssetsFolder());
	}

	public List<IFile> discoverTilemapFiles() throws CoreException {
		return AssetPackCore.discoverTilemapFiles(getAssetsFolder());
	}

	public List<IFile> discoverAudioFiles() throws CoreException {
		return AssetPackCore.discoverAudioFiles(getAssetsFolder());
	}

	public List<IFile> discoverAudioSpriteFiles() throws CoreException {
		return AssetPackCore.discoverAudioSpriteFiles(getAssetsFolder());
	}

	public List<IFile> discoverAtlasFiles() throws CoreException {
		return AssetPackCore.discoverAtlasFiles(getAssetsFolder());
	}

	public void visitAssets(Consumer<AssetModel> visitor) {
		for (AssetSectionModel section : _sections) {
			for (AssetModel asset : section.getAssets()) {
				visitor.accept(asset);
			}
		}
	}

	public static void sortFilesByNotUsed(List<IFile> files, Set<IFile> usedFiles) {
		files.sort(new Comparator<IFile>() {

			@Override
			public int compare(IFile o1, IFile o2) {
				int a = usedFiles.contains(o1) ? 1 : 0;
				int b = usedFiles.contains(o2) ? 1 : 0;
				if (a == b) {
					return o1.getFullPath().toPortableString().compareTo(o2.getFullPath().toPortableString());
				}
				return Integer.compare(a, b);
			}
		});
	}

	public Set<IFile> sortFilesByNotUsed(List<IFile> files) {
		Set<IFile> usedFiles = findUsedFiles();
		sortFilesByNotUsed(files, usedFiles);
		return usedFiles;
	}

	public String getAssetUrl(IFile file) {
		IContainer assetsFolder = getAssetsFolder();
		IContainer parent = assetsFolder instanceof IProject ? assetsFolder : assetsFolder.getParent();
		String relPath = file.getFullPath().makeRelativeTo(parent.getFullPath()).toPortableString();
		return relPath;
	}

	public Set<IFile> findUsedFiles() {
		Set<IFile> usedFiles = new HashSet<>();
		visitAssets(new Consumer<AssetModel>() {

			@Override
			public void accept(AssetModel t) {
				IFile[] list = t.getUsedFiles();
				for (IFile f : list) {
					if (f != null) {
						usedFiles.add(f);
					}
				}
			}
		});
		return usedFiles;
	}

	public IFile pickFile(List<IFile> files) {
		if (files != null && !files.isEmpty()) {
			Set<IFile> used = findUsedFiles();
			for (IFile file : files) {
				if (!used.contains(file)) {
					return file;
				}
			}
		}
		return null;
	}

	public IFile pickFile(Function<IFile, Boolean> accept) throws CoreException {
		List<IFile> files = AssetPackCore.discoverFiles(getAssetsFolder(), accept);
		return pickFile(files);
	}

	/**
	 * Pick one of the not used image files.
	 * 
	 * @return The unused image resource, or null if there is not any image
	 *         available.
	 * @throws CoreException
	 */
	public IFile pickImageFile() throws CoreException {
		return pickFile(discoverImageFiles());
	}

	/**
	 * Pick one of the not used audiosprite files.
	 * 
	 * @return The unused audiosprite resource, or null if there is not any
	 *         audiosprite available.
	 * @throws CoreException
	 */
	public IFile pickAudioSpriteFile() throws CoreException {
		return pickFile(discoverAudioSpriteFiles());
	}

	/**
	 * Pick a list of not used audio files.
	 * 
	 * @return The list or not used audio files, or null if there is not anyone
	 *         available.
	 * @throws CoreException
	 */
	public List<IFile> pickAudioFiles() throws CoreException {
		Set<IFile> used = findUsedFiles();
		List<IFile> audios = discoverAudioFiles();
		Set<IFile> result = new HashSet<>();
		if (audios.size() > 0) {
			for (IFile audio : audios) {
				if (!used.contains(audio)) {
					List<IFile> closure = AssetPackCore.getSameNameFiles(audio, audios);
					result.addAll(closure);
					break;
				}
			}
		}
		return new ArrayList<>(result);
	}

	/**
	 * Pick a tilemap file that is not used.
	 * 
	 * @return The non used tilemap file or null if there is not anyone
	 *         available.
	 * @throws CoreException
	 */
	public IFile pickTilemapFile() throws CoreException {
		return pickFile(discoverTilemapFiles());
	}

	public void addSection(AssetSectionModel section, boolean notify) {
		section.setPack(this);
		_sections.add(section);
		if (notify) {
			setDirty(true);
		}
	}

	public void removeSection(AssetSectionModel section) {
		_sections.remove(section);
		setDirty(true);
	}

	public List<AssetSectionModel> getSections() {
		return Collections.unmodifiableList(_sections);
	}

	public List<AssetModel> getAssets() {
		List<AssetModel> list = new ArrayList<>();
		for (AssetSectionModel section : _sections) {
			for (AssetModel asset : section.getAssets()) {
				list.add(asset);
			}
		}
		return list;
	}

	public AssetSectionModel findSection(String key) {
		if (key == null) {
			return null;
		}
		for (AssetSectionModel section : _sections) {
			String key2 = section.getKey();
			if (key2 != null && key2.equals(key)) {
				return section;
			}
		}
		return null;
	}

	public String createKey(String prefix) {
		for (int i = 0; i < 200; i++) {
			String key = prefix + (i == 0 ? "" : new Integer(i));
			if (!hasKey(key)) {
				return key;
			}
		}
		// in the practice we always find a key, but at the end we have to
		// return something
		return prefix;
	}

	public String createKey(IFile file) {
		String name = file.getName();
		String ext = file.getFileExtension();
		if (ext.length() > 0) {
			name = name.substring(0, name.length() - ext.length() - 1);
		}
		return createKey(name);
	}

	public boolean hasKey(String key) {
		for (AssetSectionModel section : _sections) {
			if (section.getKey().equals(key)) {
				return true;
			}
			for (AssetModel asset : section.getAssets()) {
				if (asset.getKey().equals(key)) {
					return true;
				}
			}
		}
		return false;
	}

	public void saveState(IMemento memento, Object element) {
		memento.putString(MEMENTO_KEY, getStringReference(element));
	}

	public String getStringReference(Object element) {
		JSONObject obj = getAssetJSONRefrence(element);
		return obj.toString();
	}

	public JSONObject getAssetJSONRefrence(Object element) {
		JSONObject obj = new JSONObject();
		obj.put("file", _file.getFullPath().toString());
		AssetSectionModel section = null;
		AssetGroupModel group = null;
		AssetModel asset = null;
		if (element instanceof AssetSectionModel) {
			section = (AssetSectionModel) element;
		} else if (element instanceof AssetGroupModel) {
			group = (AssetGroupModel) element;
			section = group.getSection();
		} else if (element instanceof AssetModel) {
			asset = (AssetModel) element;
			section = asset.getSection();
		}
		if (section != null) {
			obj.put("section", section.getKey());
		}
		if (group != null) {
			obj.put("group", group.getType().name());
		}
		if (asset != null) {
			obj.put("asset", asset.getKey());
		}
		return obj;
	}

	public Object getElementFromStringReference(String ref) {
		try {
			JSONObject obj = new JSONObject(ref);
			return getElementFromJSONReference(obj);
		} catch (JSONException e) {
			return null;
		}
	}

	public Object getElementFromJSONReference(JSONObject obj) {
		AssetSectionModel section = null;
		AssetGroupModel group = null;
		AssetModel asset = null;
		if (obj.has("section")) {
			section = findSection(obj.getString("section"));
		}
		if (obj.has("group")) {
			if (section == null) {
				return null;
			}
			group = section.getGroup(AssetType.valueOf(obj.getString("group")));
			return group;
		}
		if (obj.has("asset")) {
			if (section == null) {
				return null;
			}
			asset = section.findAsset(obj.getString("asset"));
			return asset;
		}
		return section;
	}

	private transient final PropertyChangeSupport support = new PropertyChangeSupport(this);

	public void addPropertyChangeListener(PropertyChangeListener l) {
		support.addPropertyChangeListener(l);
	}

	public void removePropertyChangeListener(PropertyChangeListener l) {
		support.removePropertyChangeListener(l);
	}

	public void addPropertyChangeListener(String property, PropertyChangeListener l) {
		support.addPropertyChangeListener(property, l);
	}

	public void removePropertyChangeListener(String property, PropertyChangeListener l) {
		support.removePropertyChangeListener(property, l);
	}

	public void firePropertyChange(String property) {
		support.firePropertyChange(property, true, false);
	}
}