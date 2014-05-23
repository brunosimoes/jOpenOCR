package org.jopenocr.filters;

import java.awt.Image;

public interface ImageFilter {

	public int count();
	public Image getImage();
	public Image getImage(int i);

}
