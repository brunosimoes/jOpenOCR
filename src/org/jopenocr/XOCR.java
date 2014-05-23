package org.jopenocr;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Vector;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jopenocr.filters.ImageFilter;
import org.jopenocr.filters.LazyAlgorithm;
import org.jopenocr.network.Entry;
import org.jopenocr.network.KohonenNetwork;
import org.jopenocr.network.NeuralReportable;
import org.jopenocr.network.Sample;
import org.jopenocr.network.SampleData;
import org.jopenocr.network.TrainingSet;

public class XOCR extends JFrame implements Runnable, NeuralReportable {

	private static final long serialVersionUID = 1922217L;

	/** The downsample width for the application. */
	private static final int DOWNSAMPLE_WIDTH = 20;

	/** The down sample height for the application. */
	private static final int DOWNSAMPLE_HEIGHT = 20;

	/** The letters that have been defined. */
	private DefaultListModel letterListModel = new DefaultListModel();

	/** The neural network. */
	private KohonenNetwork net;

	/** The background thread used for training. */
	private Thread trainThread = null;

	private Vector <ImageSample> imagesamples;

	private JLabel title;
	private JButton del, load, save, train, loadImage, recognizeAll;
	private JScrollPane lettersbox;
	private JList letters;
	private int imageletters = 0;
	private String filename = Configuration.NUMBERS_DATASET;
	private ImageFilter parser = null;
	
	public XOCR(){

		setTitle("jOpenOCR v1.0");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
		
		getContentPane().setLayout(null);
		int shift = 25;

		setSize(1110, 382);
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation((screenSize.width - getWidth()) / 2, (screenSize.height - getHeight()) / 2);
		setVisible(false);

		title = new JLabel();
		title.setText("Symbols");
		getContentPane().add(title);
		title.setBounds(60 + shift, 8, 84, 12);

		shift += 12;
		letters = new JList();
		lettersbox = new JScrollPane();
		lettersbox.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED );
		lettersbox.setOpaque(true);
		getContentPane().add(lettersbox);
		lettersbox.setBounds(shift, 24, 144, 132);
		lettersbox.getViewport().add(letters);
		letters.setBounds(0, 0, 126, 129);

		del = new JButton();
		del.setText("Delete");
		del.setActionCommand("Delete");
		getContentPane().add(del);
		del.setBounds(shift, 156, 144, 24);

		load = new JButton();
		load.setText("Load");
		load.setActionCommand("Load");
		getContentPane().add(load);
		load.setBounds(shift, 180, 72, 24);

		save = new JButton();
		save.setText("Save");
		save.setActionCommand("Save");
		getContentPane().add(save);
		save.setBounds(72 + shift, 180, 72, 24);

		train = new JButton();
		train.setText("Begin Training");
		train.setActionCommand("Begin Training");
		getContentPane().add(train);
		train.setBounds(shift, 204, 144, 24);

		JLabel elabel = new JLabel("    ");
		getContentPane().add(elabel);
		elabel.setBounds(shift, 228, 144, 24);
		
		loadImage = new JButton();
		loadImage.setText("Load Image");
		loadImage.setActionCommand("Load Image");
		getContentPane().add(loadImage);
		loadImage.setBounds(shift, 252, 144, 24);

		recognizeAll = new JButton();
		recognizeAll.setText("Recognize");
		recognizeAll.setActionCommand("Recognize");
		getContentPane().add(recognizeAll);
		recognizeAll.setBounds(shift, 274, 144, 24);

		SymAction lSymAction = new SymAction();
		recognizeAll.addActionListener(lSymAction);
		load.addActionListener(lSymAction);
		save.addActionListener(lSymAction);
		train.addActionListener(lSymAction);
		loadImage.addActionListener(lSymAction);

		SymListSelection lSymListSelection = new SymListSelection();
		letters.addListSelectionListener(lSymListSelection);
		letters.setModel(letterListModel);

		imagesamples = new Vector<ImageSample>();
		for(int i = 0; i < 4; i++){
			imagesamples.add(new ImageSample(i, shift, getContentPane(), lSymAction));
			shift += 220;
		}

		load(true);
	}

	private void clear(){
		for(int i = 0; i < imagesamples.size(); i++){
			ImageSample s = imagesamples.get(i);
			s.clear();
			imageletters = 0;
		}
	}

	public String read(String url){
		parser = new LazyAlgorithm(url);
		imageletters = parser.count();
		for(int i = 0; i < imageletters && i < 4; i++)
			imagesamples.get(i).setBufferedImage(parser.getImage(i));

		String text = "";
		for(int i = 0; i < imageletters && i < 4; i++)
			text += imagesamples.get(i).recognize(false);

		return text;
	}

	public String read(BufferedImage image){
		parser = new LazyAlgorithm(image);
		imageletters = parser.count();
		for(int i = 0; i < imageletters && i < 4; i++)
			imagesamples.get(i).setBufferedImage(parser.getImage(i));

		String text = "";
		for(int i = 0; i < imageletters && i < 4; i++)
			text += imagesamples.get(i).recognize(false);

		return text;
	}

	private void loadimage(){
		if(parser != null)
			clear();
		JFileChooser chooser = new JFileChooser(Configuration.DEFAULT_IMG_DIR);
		int returnVal = chooser.showOpenDialog(this);
		if(returnVal == JFileChooser.APPROVE_OPTION) {
			parser = new LazyAlgorithm(chooser.getSelectedFile());
			imageletters = parser.count();
			for(int i = 0; i < imageletters && i < 4; i++)
				imagesamples.get(i).setBufferedImage(parser.getImage(i));
		}
	}

	private void load(boolean defaultOption){
		try {

			/** The actual file stream */
			FileReader f;

			/** Used to read the file line by line */
			BufferedReader r;
			if(!defaultOption){
				JFileChooser chooser = new JFileChooser("./");
				chooser.addChoosableFileFilter(new JavaFilter());
				int returnVal = chooser.showOpenDialog(this);
				if(returnVal == JFileChooser.APPROVE_OPTION) {
					f = new FileReader( chooser.getSelectedFile() );
					filename = chooser.getSelectedFile().getName();
				}
				else {
					f = new FileReader( new File(filename) );
				}
			}
			else {
				f = new FileReader( new File(filename) );
			}

			r = new BufferedReader(f);
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
			JOptionPane.showMessageDialog(this, "Error: " + e, "Training", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void save(){
		try {
			/** The actual file stream */
			OutputStream os;

			/** Used to write the file line by line */
			PrintStream ps;

			os = new FileOutputStream( filename, false );
			ps = new PrintStream(os);

			for ( int i = 0; i < letterListModel.size(); i++ ) {
				SampleData ds = (SampleData)letterListModel.elementAt(i);
				ps.print( ds.getLetter() + ":" );
				for ( int y = 0;y < ds.getHeight(); y++ ) {
					for ( int x = 0; x < ds.getWidth(); x++ ) {
						ps.print( ds.getData(x,y) ? "1" : "0" );
					}
				}
				ps.println("");
			}

			ps.close();
			os.close();
		}
		catch ( Exception e ) {
			JOptionPane.showMessageDialog(this,"Error: " + e, "Training", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void del(){
		int i = letters.getSelectedIndex();

		if ( i==-1 ) {
			JOptionPane.showMessageDialog(this, "Please select a letter to delete.", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		letterListModel.remove(i);
	}

	private void train(){
		if ( trainThread == null ) {
			train.setText("Stop Training");
			train.repaint();
			trainThread = new Thread(this);
			trainThread.start();
		}
		else {
			net.halt = true;
		}
	}


	private void recognizeAll(){
		String text = "";
		for(int i = 0; i < imageletters && i < 4; i++){
			text += imagesamples.get(i).recognize(false);
			System.out.println(text);
		}
		if(!text.trim().isEmpty())
			JOptionPane.showMessageDialog(this, "  " + text , "That Text is", JOptionPane.PLAIN_MESSAGE);
		else
			JOptionPane.showMessageDialog(this, "Unable to read the image using current symbols settings", "Warning", JOptionPane.PLAIN_MESSAGE);
	}


	private void lettersValueChanged(ListSelectionEvent event){
		if ( letters.getSelectedIndex() == -1 )
			return;
		SampleData selected = (SampleData)letterListModel.getElementAt(letters.getSelectedIndex());
		imagesamples.get(0).setData(selected);
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
		catch ( Exception e ) {
			JOptionPane.showMessageDialog(this,"Error: " + e, "Training", JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * Called to update the stats from the neural network.
	 *
	 * @param retry How many tries.
	 * @param totalError The current error.
	 * @param bestError The best error.
	 */

	public void update(int retry,double totalError,double bestError){
		if ( (((retry%100)!=0) || (retry==10)) && !net.halt )
			return;

		if ( net.halt ) {
			trainThread = null;
			train.setText("Begin Training");
		}

		UpdateStats stats = new UpdateStats();
		stats._tries = retry;
		stats._lastError=totalError;
		stats._bestError=bestError;

		try {
			SwingUtilities.invokeAndWait(stats);
		}
		catch ( Exception e ) {
			JOptionPane.showMessageDialog(this,"Error: " + e,"Training", JOptionPane.ERROR_MESSAGE);
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

	class JavaFilter extends javax.swing.filechooser.FileFilter {
		public boolean accept(File f) {
			if (f.getName( ).endsWith(".ocr")) return true;
			else if (f.isDirectory( )) return true;
			return false;
		}

		public String getDescription( ) {
			return "OCR definition file (*.ocr)";
		}
	}

	class UpdateStats implements Runnable {
		long _tries;
		double _lastError;
		double _bestError;

		public void run(){}
	}

	class SymAction implements ActionListener {
		public void actionPerformed(ActionEvent event){
			Object object = event.getSource();
			for(int i = 0; i < imagesamples.size(); i++){
				ImageSample s = imagesamples.get(i);
				if ( object == s.downSample )		s.downSample();
				else if ( object == s.clear )		s.clear();
				else if ( object == s.add )			s.add();
				else if ( object == s.recognize )	s.recognize(true);
			}

			if ( object == loadImage )				loadimage();
			else if ( object == load )				load(false);
			else if ( object == save )				save();
			else if ( object == del )				del();
			else if ( object == train )				train();
			else if ( object == recognizeAll )		recognizeAll();
		}
	}

	class SymListSelection implements ListSelectionListener {
		public void valueChanged(ListSelectionEvent event){
			Object object = event.getSource();
			if ( object == letters )
				lettersValueChanged(event);
		}
	}

	class ImageSample extends JFrame {

		private static final long serialVersionUID = 19981017L;

		JLabel title;
		public JButton add, clear, recognize, downSample;

		/** The entry component for the user to draw into. */
		Entry entry;

		/** The down sample component to display the drawing downsampled. */
		Sample sample;

		ImageSample(int n, int shift, Container c, SymAction action){

			title = new JLabel();
			add = new JButton();
			clear = new JButton();
			recognize = new JButton();
			downSample = new JButton();

			title.setText("Sample " + n);
			c.add(title);
			title.setBounds(246 + shift, 8, 144, 12);

			/** Draw Box */
			c.setLayout(null);
			entry = new Entry();
			entry.setBounds(168 + shift, 25, 200, 128);
			c.add(entry);

			sample = new Sample(DOWNSAMPLE_WIDTH, DOWNSAMPLE_HEIGHT);
			sample.setBounds(235 + shift, 252, 80, 80);
			entry.setSample(sample);
			c.add(sample);

			downSample.setText("Down Sample");
			downSample.setActionCommand("Down Sample");
			c.add(downSample);
			downSample.setBounds(252 + shift, 180, 117, 24);

			add.setText("Add");
			add.setActionCommand("Add" + n);
			c.add(add);
			add.setBounds(168 + shift, 156, 84, 24);

			clear.setText("Clear");
			clear.setActionCommand("Clear" + n);
			c.add(clear);
			clear.setBounds(168 + shift, 180, 84, 24);

			recognize.setText("Recognize");
			recognize.setActionCommand("Recognize" + n);
			c.add(recognize);
			recognize.setBounds(252 + shift, 156, 117, 24);

			downSample.addActionListener(action);
			clear.addActionListener(action);
			add.addActionListener(action);
			del.addActionListener(action);
			recognize.addActionListener(action);

		}

		public void downSample(){
			entry.downSample();
		}

		public void clear(){
			entry.clear();
			sample.getData().clear();
			sample.repaint();
		}

		public void add(){
			int i;

			String letter = JOptionPane.showInputDialog("Please enter a letter you would like to assign this sample to.");
			if ( letter==null )
				return;

			if ( letter.length()>1 ) {
				JOptionPane.showMessageDialog(this, "Please enter only a single letter.", "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}

			entry.downSample();
			SampleData sampleData = (SampleData)sample.getData().clone();
			sampleData.setLetter(letter.charAt(0));

			for ( i = 0;i < letterListModel.size();i++ ) {
				SampleData str = (SampleData)letterListModel.getElementAt(i);
				if ( str.equals(letter) ) {
					JOptionPane.showMessageDialog(this,
									"That letter is already defined, delete it first!", "Error",
										JOptionPane.ERROR_MESSAGE);
					return;
				}

				if ( str.compareTo(sampleData)>0 ) {
					letterListModel.add(i, sampleData);
					return;
				}
			}
			letterListModel.add(letterListModel.size(), sampleData);
			letters.setSelectedIndex(i);
			entry.clear();
			sample.repaint();
			train();
		}

		public void setBufferedImage(Image image){
			entry.setBufferedImage(image);
		}

		public char recognize(boolean message){
			if ( net == null ) {
				train();
			}

			entry.downSample();
			double input[] = new double[DOWNSAMPLE_HEIGHT * DOWNSAMPLE_WIDTH];
			int idx = 0;
			SampleData ds = sample.getData();
			for ( int y = 0; y < ds.getHeight(); y++ ) {
				for ( int x = 0; x < ds.getWidth(); x++ ) {
					input[idx++] = ds.getData(x,y)?.5:-.5;
				}
			}

			double normfac[] = new double[1];
			double synth[] = new double[1];

			int best = net.winner ( input , normfac , synth ) ;
			char map[] = mapNeurons();
			
			if(best != 0){
				if(message)
					JOptionPane.showMessageDialog(this, "  " + map[best] , "That Letter is", JOptionPane.PLAIN_MESSAGE);
				return map[best];
			}
			else if(best == 0)
				JOptionPane.showMessageDialog(this, "Unable to read the image using current symbols settings", "Warning", JOptionPane.PLAIN_MESSAGE);
			
			return ' ';
		}

		public void setData(SampleData selected){
			sample.setData((SampleData)selected.clone());
			sample.repaint();
			entry.clear();
		}
	}

	public static void main(String [] args){
		XOCR x = new XOCR();
		x.setVisible(true);
	}
}