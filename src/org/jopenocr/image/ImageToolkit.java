package org.jopenocr.image;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public final class ImageToolkit {

	public static final int red( int rgb ) { return (rgb & 0xff0000) >> 16; }
	public static final int green( int rgb ) { return (rgb & 0x00ff00) >> 8; }
	public static final int blue( int rgb ) { return rgb & 0xff; }
	public static final int rgb( int red, int green, int blue ) { return blue + (green << 8) + (red << 16); }

	public static final BufferedImage resize(BufferedImage img, int newW, int newH) {  
		int w = img.getWidth();
		int h = img.getHeight();
		BufferedImage dimg = new BufferedImage(newW, newH, img.getType());
		Graphics2D g = dimg.createGraphics();  
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);  
		g.drawImage(img, 0, 0, newW, newH, 0, 0, w, h, null);  
		g.dispose();
		return dimg;
	} 

}
