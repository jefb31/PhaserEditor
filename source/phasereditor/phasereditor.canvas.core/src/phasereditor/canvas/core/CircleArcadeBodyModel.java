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
package phasereditor.canvas.core;

import org.json.JSONObject;

/**
 * @author arian
 *
 */
public class CircleArcadeBodyModel extends ArcadeBodyModel {
	private double _radius;

	public double getRadius() {
		return _radius;
	}

	public void setRadius(double radius) {
		_radius = radius;
	}

	@Override
	public BodyType getBodyType() {
		return BodyType.ARCADE_CIRCLE;
	}

	@Override
	protected void writeJSON(JSONObject data) {
		super.writeJSON(data);
		data.put("radius", _radius);
	}

	@Override
	public void readJSON(JSONObject data) {
		super.readJSON(data);
		_radius = data.optDouble("radius");
	}
}
