package org.jopenocr.image;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

public class ImageBoundingBox {
	
	private int xi, xf, yi, yf;
	private BufferedImage image;

	public ImageBoundingBox(int xi, int yi, int xf, int yf, BufferedImage image) {
		this.xi = xi;
		this.xf = xf;
		this.yi = yi;
		this.yf = yf;
		this.image = image.getSubimage(xi, yi, xf, yf);
	}

	public BufferedImage getImage() {
		return image;
	}

	public void draw(Graphics g) {
		g.setColor(Color.red);
		g.drawRect(xi, yi, xf, yf);
	}
}