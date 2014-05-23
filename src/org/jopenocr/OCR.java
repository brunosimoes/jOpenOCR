package org.jopenocr;

import java.awt.Image;
import java.awt.image.PixelGrabber;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Vector;

import javax.swing.DefaultListModel;

import org.jopenocr.filters.ImageFilter;
import org.jopenocr.filters.LazyAlgorithm;
import org.jopenocr.network.KohonenNetwork;
import org.jopenocr.network.NeuralReportable;
import org.jopenocr.network.Sample;
import org.jopenocr.network.SampleData;
import org.jopenocr.network.TrainingSet;

public class OCR implements NeuralReportable {

	public static void main(String [] args){
		OCR ocr = new OCR(Configuration.NUMBERS_DATASET);
		String r = ocr.read(Configuration.DEFAULT_IMG_DIR + File.separator + 
								"PTR" + File.separator + "affablesite_tn1.jpg");
		System.out.println(r);
	}

	/** The downsample width for the application. */
	private static final int DOWNSAMPLE_WIDTH = 20;

	/** The down sample height for the application. */
	private static final int DOWNSAMPLE_HEIGHT = 20;

	/** The neural network. */
	private KohonenNetwork net;

	/** The background thread used for training. */
	private Thread trainThread = null;

	private Vector <ImageSample> imagesamples;

	private int imageletters = 0;
	private ImageFilter parser = null;
	private DefaultListModel letterListModel = new DefaultListModel();
	private String dataset;

	public OCR(String dataset){
		imagesamples = new Vector<ImageSample>();
		for(int i = 0; i < 4; i++)
			imagesamples.add(new ImageSample());
		
		this.dataset = dataset;
		load(true);
	}

	public String read(String url){
		parser = new LazyAlgorithm(url);
		imageletters = parser.count();
		for(int i = 0; i < imageletters && i < 4; i++)
			imagesamples.get(i).setBufferedImage(parser.getImage(i));

		String text = "";
		for(int i = 0; i < imageletters && i < 4; i++)
			text += imagesamples.get(i).recognize();
		return text;
	}

	private void load(boolean defaultOption){
		try {
			/** The actual file stream */
			FileReader f = new FileReader( new File(dataset) );

			/** Used to read the file line by line */
			BufferedReader r = new BufferedReader(f);
			String line;
			int i = 0;

			letterListModel.clear();
			while ( (line=r.readLine()) !=null ) {
				SampleData ds = new SampleData(line.charAt(0), DOWNSAMPLE_WIDTH, DOWNSAMPLE_HEIGHT);
				letterListModel.add(i++, ds);
				int idx = 2;
				for ( int y=0;y<ds.getHeight();y++ ) {
					for ( int x=0;x<ds.getWidth();x++ ) {
						ds.setData(x,y,line.charAt(idx++)=='1');
					}
				}
			}

			r.close();
			f.close();

			train();
		}
		catch ( Exception e ) {
		}
	}

	private void train(){
		if ( trainThread == null ) {
			run();
		}
		else {
			net.halt = true;
		}
	}

	/** Run method for the background training thread. */
	public void run(){
		try {
			int inputNeuron = DOWNSAMPLE_HEIGHT * DOWNSAMPLE_WIDTH;
			int outputNeuron = letterListModel.size();

			TrainingSet set = new TrainingSet(inputNeuron,outputNeuron);
			set.setTrainingSetCount(letterListModel.size());

			for ( int t=0;t<letterListModel.size();t++ ) {
				int idx=0;
				SampleData ds = (SampleData)letterListModel.getElementAt(t);
				for ( int y=0;y<ds.getHeight();y++ ) {
					for ( int x=0;x<ds.getWidth();x++ ) {
						set.setInput(t,idx++,ds.getData(x,y)?.5:-.5);
					}
				}
			}

			net = new KohonenNetwork(inputNeuron,outputNeuron,this);
			net.setTrainingSet(set);
			net.learn();
		}
		catch ( Exception e ) {}
	}

	/**
	 * Called to update the stats, from the neural network.
	 *
	 * @param retry How many tries.
	 * @param totalError The current error.
	 * @param bestError The best error.
	 */

	public void update(int retry, double totalError, double bestError){
		if ( (((retry%100)!=0) || (retry==10)) && !net.halt )
			return;

		if ( net.halt ) {
			trainThread = null;
		}
	}

	/**
	 * Used to map neurons to actual letters.
	 *
	 * @return The current mapping between neurons and letters as an array.
	 */

	private char [] mapNeurons(){
		char map[] = new char[letterListModel.size()];
		double normfac[] = new double[1];
		double synth[] = new double[1];

		for ( int i=0; i < map.length; i++ )
			map[i]='?';

		for ( int i=0; i<letterListModel.size(); i++ ) {
			double input[] = new double[DOWNSAMPLE_HEIGHT * DOWNSAMPLE_WIDTH];
			int idx = 0;
			SampleData ds = (SampleData)letterListModel.getElementAt(i);
			for ( int y=0;y<ds.getHeight();y++ ) {
				for ( int x=0;x<ds.getWidth();x++ ) {
					input[idx++] = ds.getData(x,y)?.5:-.5;
				}
			}

			int best = net.winner ( input , normfac , synth ) ;
			map[best] = ds.getLetter();
		}
		return map;
	}

	class ImageSample {

		/**
		 * The last x that the user was drawing at.
		 */
		protected int lastX = -1;

		/**
		 * The last y that the user was drawing at.
		 */
		protected int lastY = -1;

		/**
		 * The down sample component used with this
		 * component.
		 */
		protected Sample sample;

		/**
		 * Specifies the left boundary of the cropping
		 * rectangle.
		 */
		protected int downSampleLeft;

		/**
		 * Specifies the right boundary of the cropping
		 * rectangle.
		 */
		protected int downSampleRight;

		/**
		 * Specifies the top boundary of the cropping
		 * rectangle.
		 */
		protected int downSampleTop;

		/**
		 * Specifies the bottom boundary of the cropping
		 * rectangle.
		 */
		protected int downSampleBottom;

		/**
		 * The downsample ratio for x.
		 */
		protected double ratioX;

		/**
		 * The downsample ratio for y
		 */
		protected double ratioY;

		/**
		 * The pixel map of what the user has drawn.
		 * Used to downsample it.
		 */
		protected int pixelMap[];

		/** The down sample component to display the drawing downsampled. */
		SampleData data;

		/**
		 * The image that the user is drawing into.
		 */
		protected Image entryImage;

		ImageSample(){
			data = new SampleData(' ', DOWNSAMPLE_WIDTH, DOWNSAMPLE_HEIGHT);
		}

		public void clear(){
			this.downSampleBottom = 0;
			this.downSampleTop = 0;
			this.downSampleLeft = 0;
			this.downSampleRight = 0;
			data.clear();
		}

		public void downSample(){
			int w = entryImage.getWidth(null);
			int h = entryImage.getHeight(null);
			PixelGrabber grabber = new PixelGrabber(entryImage, 0, 0, w, h, true);
			try {
				grabber.grabPixels();
				pixelMap = (int[])grabber.getPixels();
				findBounds(w,h);
				ratioX = (double)(downSampleRight - downSampleLeft)/(double)data.getWidth();
				ratioY = (double)(downSampleBottom - downSampleTop)/(double)data.getHeight();

				for ( int y = 0; y < data.getHeight(); y++ )
					for ( int x = 0; x < data.getWidth(); x++ )
							data.setData(x, y, downSampleQuadrant(x,y));

			}
			catch ( InterruptedException e ) {}
		}


		public void setBufferedImage(Image image){
			entryImage = image;
		}

		public char recognize(){
			if ( net == null ) {
				train();
			}

			downSample();
			double input[] = new double[DOWNSAMPLE_HEIGHT * DOWNSAMPLE_WIDTH];
			int idx = 0;
			for ( int y = 0; y < data.getHeight(); y++ ) {
				for ( int x = 0; x < data.getWidth(); x++ ) {
					input[idx++] = data.getData(x,y)?.5:-.5;
				}
			}

			double normfac[] = new double[1];
			double synth[] = new double[1];

			int best = net.winner ( input , normfac , synth ) ;
			char map[] = mapNeurons();
			return map[best];
		}

		public void setData(SampleData selected){
			data = ((SampleData)selected.clone());
			clear();
		}

		/**
		 * This method is called internally to
		 * see if there are any pixels in the given
		 * scan line. This method is used to perform
		 * autocropping.
		 *
		 * @param y The horizontal line to scan.
		 * @return True if there were any pixels in this
		 * horizontal line.
		 */

		protected boolean hLineClear(int y){
			int w = entryImage.getWidth(null);
			for ( int i = 0 ; i < w; i++ ) {
				if ( pixelMap[(y*w)+i] !=-1 )
					return false;
			}
			return true;
		}


		/**
		 * This method is called to determine ....
		 *
		 * @param x The vertical line to scan.
		 * @return True if there are any pixels in the
		 * specified vertical line.
		 */

		protected boolean vLineClear(int x){
			int w = entryImage.getWidth(null);
			int h = entryImage.getHeight(null);
			for ( int i = 0; i < h; i++ ) {
			if ( pixelMap[(i*w)+x] !=-1 )
				return false;
			}
			return true;
		}


		/**
		 * This method is called to automatically
		 * crop the image so that whitespace is
		 * removed.
		 *
		 * @param w The width of the image.
		 * @param h The height of the image
		 */

		protected void findBounds(int w,int h){
			/** Top line */
			for ( int y = 0; y < h; y++ ) {
				if ( !hLineClear(y) ) {
					downSampleTop=y;
					break;
				}
			}

			/** Bottom line */
			for ( int y = h-1; y >= 0; y-- ) {
				if ( !hLineClear(y) ) {
					downSampleBottom = y;
					break;
				}
			}

			/** Left line */
			for ( int x = 0; x < w; x++ ) {
				if ( !vLineClear(x) ) {
					downSampleLeft = x;
					break;
				}
			}

			/** Right line */
			for ( int x = w-1; x >= 0; x-- ) {
				if ( !vLineClear(x) ) {
					downSampleRight = x;
					break;
				}
			}
		}

		/**
		 * Called to downsample a quadrant of the image.
		 *
		 * @param x The x coordinate of the resulting downsample.
		 * @param y The y coordinate of the resulting downsample.
		 * @return Returns true if there were ANY pixels in the specified quadrant.
		 */
		protected boolean downSampleQuadrant(int x,int y){
			int w = entryImage.getWidth(null);
			int startX = (int)(downSampleLeft + (x * ratioX));
			int startY = (int)(downSampleTop + (y * ratioY));
			int endX = (int)(startX + ratioX);
			int endY = (int)(startY + ratioY);
			for ( int yy = startY; yy <= endY; yy++ ) {
				for ( int xx = startX; xx <= endX; xx++ ) {
					int loc = xx + (yy * w);
					if ( pixelMap[loc] != -1 )
						return true;
				}
			}
			return false;
		}
	}
}