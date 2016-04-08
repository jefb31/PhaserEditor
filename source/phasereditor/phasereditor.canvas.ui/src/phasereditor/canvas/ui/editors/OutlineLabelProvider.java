// The MIT License (MIT)
//
// Copyright (c) 2015, 2016 Arian Fornaris
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
package phasereditor.canvas.ui.editors;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.ui.shapes.IObjectNode;
import phasereditor.canvas.ui.shapes.GroupNode;
import phasereditor.canvas.ui.shapes.SpriteNode;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.IEditorSharedImages;

/**
 * @author arian
 *
 */
public class OutlineLabelProvider extends LabelProvider implements IEditorSharedImages {
	@Override
	public Image getImage(Object element) {
		if (element instanceof GroupNode) {
			return EditorSharedImages.getImage(IMG_SHAPE_GROUP_NODE);
		}
		if (element instanceof SpriteNode) {
			return EditorSharedImages.getImage(IMG_SHAPE);
		}
		return super.getImage(element);
	}

	@Override
	public String getText(Object element) {
		if (element instanceof IObjectNode) {
			BaseObjectModel model = ((IObjectNode) element).getControl().getModel();
			return model.getEditorName();
		}
		return super.getText(element);
	}
}