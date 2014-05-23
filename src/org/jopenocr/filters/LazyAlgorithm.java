package org.jopenocr.filters;

import java.awt.Image;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Vector;
import javax.imageio.ImageIO;
import org.jopenocr.image.ImageToolkit;

public class LazyAlgorithm implements ImageFilter {

	private MaxIntensity rgb;
	private BufferedImage image;
	private Vector <Point> points;
	private Vector <Integer> colors;

	public LazyAlgorithm(File image){
		try {
			this.image = ImageIO.read(image);
			analyse();
		}
		catch( Exception e ) {
			System.out.println( "Exception: " + e );
			e.printStackTrace();
		}
	}

	public LazyAlgorithm(String image){
		try {
			this.image = ImageIO.read(new File(image));
			analyse();
		}
		catch( Exception e ) {
			System.out.println( "Exception: " + e );
			e.printStackTrace();
		}
	}

	public LazyAlgorithm( BufferedImage image ){
		this.image = image;
		analyse();
	}

	@Override
	public int count(){ return colors.size();}

	@Override
	public Image getImage(){
		return image.getScaledInstance(100, 100, Image.SCALE_SMOOTH);
	}

	@Override
	public Image getImage(int p){
		int r = colors.get(p);
		BufferedImage copy = new BufferedImage(image.getWidth( ), image.getHeight( ), BufferedImage.TYPE_INT_RGB);

		int x = -1, y = -1, xf = 0, yf = 0;
		for( int i = 0; i < image.getWidth(); i++ ){
			for( int j = 0; j < image.getHeight(); j++ ){
				if(r != ImageToolkit.red(image.getRGB( i, j ))) copy.setRGB(i, j, 0xffffff);
				else {
					if(i > xf) xf = i;
					if(x == -1) x = i;
					if(j > yf) yf = j;
					if(y == -1) y = j;
					if(i < x) x = i;
					if(j < y) y = j;
				}
			}
		}

		if(x < 0 || y < 0 || xf-x <= 0 || yf-y >= 0) return copy;
		BufferedImage b = copy.getSubimage(x, y, xf-x, yf-y);
		return ImageToolkit.resize(b, 80, 80);
	}
	
	/** Get vertical lines */
	private Vector <Integer> vlines(){
		Vector <Integer> vlines = new Vector<Integer>();
		int d = 0;
		for( int i = 2; i < image.getWidth() - 2; i++ ){
			d = 0;
			for( int j = 2; j < image.getHeight() - 2; j++ ) {
				if(gpixel(image.getRGB( i, j )) && gpixel(image.getRGB( i+1, j ))) d++;
				if(j == image.getHeight() - 3 && d < 15)
					vlines.add(i);
			}
		}
		return vlines;
	}

	/** Get horizontal lines */
	private Vector <Integer> hlines(){
		Vector <Integer> hlines = new Vector<Integer>();
		int d = 0;
		for( int i = 2; i < image.getHeight() - 2; i++ ){
			d = 0;
			for( int j = 2; j < image.getWidth() - 2; j++ ) {
				if(gpixel(image.getRGB( j, i )) && gpixel(image.getRGB( j, i+1 ))) d++;
				if(j == image.getWidth() - 3 && d < 5)
					hlines.add(i);
			}
		}
		return hlines;
	}

	private void clearLine(int xs, int ys, int xf, int yf){
		for(int i = xs; i<xf; i++)
			for(int j = ys; j<yf; j++)
				image.setRGB(i, j, 0xffffff);
	}

	private void clear(){
		for( int i = 0; i < image.getHeight(); i++ ){
			for( int j = 0; j < image.getWidth(); j++ ) {
				if(gpixel(image.getRGB( j, i )))
					image.setRGB(j, i, 0xffffff);
			}
		}
	}

	private boolean isolation(int pixels){
		boolean detected = false;
		for( int i = 2; i < image.getWidth() - 2; i++ ){
			for( int j = 2; j < image.getHeight() - 2; j++ ) {
				int bad = 0;
				bad += (gpixel(image.getRGB( i + 1, j - 1 ))) ? 1 : 0;
				bad += (gpixel(image.getRGB( i + 2, j - 1 ))) ? 1 : 0;
				bad += (gpixel(image.getRGB( i + 1, j - 2 ))) ? 1 : 0;
				bad += (gpixel(image.getRGB( i + 2, j - 2 ))) ? 1 : 0;
				bad += (gpixel(image.getRGB( i + 1, j + 1 ))) ? 1 : 0;
				bad += (gpixel(image.getRGB( i + 2, j + 1 ))) ? 1 : 0;
				bad += (gpixel(image.getRGB( i + 1, j + 2 ))) ? 1 : 0;
				bad += (gpixel(image.getRGB( i + 2, j + 2 ))) ? 1 : 0;

				bad += (gpixel(image.getRGB( i - 1, j - 1 ))) ? 1 : 0;
				bad += (gpixel(image.getRGB( i - 2, j - 1 ))) ? 1 : 0;
				bad += (gpixel(image.getRGB( i - 1, j - 2 ))) ? 1 : 0;
				bad += (gpixel(image.getRGB( i - 2, j - 2 ))) ? 1 : 0;
				bad += (gpixel(image.getRGB( i - 1, j + 1 ))) ? 1 : 0;
				bad += (gpixel(image.getRGB( i - 2, j + 1 ))) ? 1 : 0;
				bad += (gpixel(image.getRGB( i - 1, j + 2 ))) ? 1 : 0;
				bad += (gpixel(image.getRGB( i - 2, j + 2 ))) ? 1 : 0;

				if(bad > pixels){
					image.setRGB(i, j, 0xffffff);
					detected = true;
				}
			}
		}
		return detected;
	}

	private void delimit(){
		int density [][] = new int[image.getWidth()-4][image.getHeight() - 4];
		for( int i = 2; i < image.getWidth() - 2; i++ ){
			for( int j = 2; j < image.getHeight() - 2; j++ ) {
				int left = image.getRGB( i - 1, j );
				int left2 = image.getRGB( i - 2, j );
				int right = image.getRGB( i + 1, j );
				int right2 = image.getRGB( i + 2, j );
				int up = image.getRGB( i, j - 1 );
				int down = image.getRGB( i, j + 1 );
				int pixels = (gpixel(left)) ? 0 : 1;
				pixels += (gpixel(left2)) ? 0 : 1;
				pixels += (gpixel(right)) ? 0 : 1;
				pixels += (gpixel(right2)) ? 0 : 1;
				pixels += (gpixel(up)) ? 0 : 1;
				pixels += (gpixel(down)) ? 0 : 1;
				density [i-2][j-2] = pixels - 1;
				if(density [i-2][j-2] > 0) image.setRGB(i-2, j-2, 0xff);
			}
		}

		for( int i = 1; i < image.getWidth(); i++ )
			for( int j = 1; j < image.getHeight(); j++ )
				image.setRGB(i, j, 0xffffff);

		for( int i = 2; i < image.getWidth() - 2; i++ ){
			for( int j = 2; j < image.getHeight() - 2; j++ ) {
				if(density [i-2][j-2] > 0) image.setRGB(i-2, j-2, 0);
				else image.setRGB(i-2, j-2, 0xffffff);
			}
		}
	}

	private void lines(){
		Vector <Integer> hlines = vlines();
		Vector <Integer> vlines = hlines();

		for( int i = 0; i < hlines.size(); i ++){
			int h = hlines.get(i);
			for( int j = 2; j < image.getHeight() - 2; j++ ){
				if(gpixel(image.getRGB(h+1, j )) && gpixel(image.getRGB(h-1, j)))
					image.setRGB(h, j, 0xffffff);
			}
		}

		for( int i = 0; i < vlines.size(); i ++){
			int v = vlines.get(i);
			for( int j = 2; j < image.getWidth() - 2; j++ )
				if(gpixel(image.getRGB(j, v+1)) && gpixel(image.getRGB(j, v-1)))
					image.setRGB(j, v, 0xffffff);
		}
	}

	/** Check if is a good pixel */
	private boolean gpixel(int pixel){
		if(ImageToolkit.red(pixel) > rgb.red() && 
				ImageToolkit.blue(pixel) > rgb.blue() && 
					ImageToolkit.green(pixel) > rgb.green()) return true;
		return false;
	}
	
	private void analyse(){ 
		rgb = new MaxIntensity();

		int [] settings = {70, 62, 61, 60, 59, 58};

		rgb.detectMaxIntensityLevel(image, settings[0]);
		lines();

		rgb.detectMaxIntensityLevel(image, settings[1]);
		clear();

		rgb.detectMaxIntensityLevel(image, settings[2]);
		while(!isolation(12)){}

		rgb.detectMaxIntensityLevel(image, settings[3]);
		while(!isolation(13)){}

		rgb.detectMaxIntensityLevel(image, settings[4]);
		while(!isolation(14)){}

		rgb.detectMaxIntensityLevel(image, settings[5]);
		clear();

		delimit();

		rgb.setLevel(0, 0);
		clearLine(0, 0, image.getWidth(), 6);
		clearLine(0, image.getHeight()-6, image.getWidth(), image.getHeight());
		clearLine(0, 0, 6, image.getHeight());
		clearLine(image.getWidth()-6, 0, image.getWidth(), image.getHeight());

		detect();
	}

	private void mark(int x, int y, int xf, int yf, int r){
		try {
			if(x < xf && y < yf && x >= 0 && y >= 0){
				int rgb = image.getRGB( x, y );
				if(ImageToolkit.green(rgb) == 0 && ImageToolkit.red(rgb) == 0 && ImageToolkit.blue(rgb) == 0){
					points.remove(new Point(x, y));
					image.setRGB(x, y, ImageToolkit.rgb(r, 0, 0));
					if(!colors.contains(new Integer(r))) colors.add(new Integer(r));
					mark(x+1, y, xf, yf, r);
					mark(x+1, y+1, xf, yf, r);
					mark(x+1, y-1, xf, yf, r);
					mark(x, y+1, xf, yf, r);
					mark(x, y-1, xf, yf, r);
					mark(x-1, y, xf, yf, r);
					mark(x-1, y+1, xf, yf, r);
					mark(x-1, y-1, xf, yf, r);
				}
				else if(ImageToolkit.red(rgb) > 40) return;
			}
			else return;
		}
		catch(Exception e){
			return;
		}
	}

	private void detect(){
		try {
		points = new Vector<Point>();
		colors = new Vector<Integer>();
		for( int i = 0; i < image.getWidth(); i++ ){
			for( int j = 0; j < image.getHeight(); j++ ) {
				int rgb = image.getRGB( i, j );
				if(0 == ImageToolkit.green(rgb) && 0 == ImageToolkit.red(rgb) && 0 == ImageToolkit.blue(rgb))
					points.add(new Point(i, j));
			}
		}

		int r = 0;
		while( points.size() > 0){
			Point p = points.get(0);
			r += 10;
			mark(p.x, p.y, image.getWidth()-1, image.getHeight()-1, r);
		}
		}
		catch(Exception e){ System.out.println(e.getMessage());}
	}

	class MaxIntensity {
		public int red, green, blue;
		private int sz = 0;

		MaxIntensity(){
			this(0,0,0);
		}

		MaxIntensity(int red, int green, int blue){
			this.red = red;
			this.green = green;
			this.blue = blue;
		}

		public void detectMaxIntensityLevel(BufferedImage image, int sz){
			this.sz = sz;
			for( int i = 0; i < image.getWidth(); i++ ){
				for( int j = 0; j < image.getHeight(); j++ ){
					int pixel = image.getRGB( i, j );
					if(ImageToolkit.red(pixel) > 10)
						red = (red + ImageToolkit.red(pixel))/2;

					if(ImageToolkit.green(pixel) > 10)
						green = (green + ImageToolkit.green(pixel))/2;

					if(ImageToolkit.blue(pixel) > 10)
						blue = (blue + ImageToolkit.blue(pixel))/2;
				}
			}
		}

		public void setLevel(int level, int sz){
			this.sz = sz;
			red=green=blue=level;
		}

		public int red(){ return red * sz / 100; }
		public int green(){ return green * sz / 100; }
		public int blue(){ return blue * sz / 100; }

		public String toString(){
			return "rgb("+red+","+green+","+blue+")";
		}

	}
}

