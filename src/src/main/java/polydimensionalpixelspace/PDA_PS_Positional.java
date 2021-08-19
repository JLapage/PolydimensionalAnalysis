package src.main.java.polydimensionalpixelspace;

import ij.IJ;
import ij.ImagePlus;
import ij.process.FloatProcessor;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * <p>This package implements Image PCA functionality: the ability to run a principle components analysis on either the distribution of
 * thresholded pixel values, or the positions of exactly specified points. Outputs are a PCA Table, and a visualisation of the result mapped
 * back to the original image.</p>
 * 
 * <p>This class represents an dataset of cell locations. This can be provided either from the xml format of Kurt De Vos' Cell Counter plugin,
 * the mtj format of Erik Meijering's MTrackJ plugin, or as a simple tab separated value file (explained more fully in 'importCategoriser' method).</p>
 * 
 * @author John Lapage
 * @version 1.0
 * 
 */


public class PDA_PS_Positional extends PDA_PS_Image {
	String posPath;
	
	/**
	 * Main constructor
	 * 
	 * @param posPath	Path to positional file (Cell Counter xml, MTrackJ mtj or simple Categorised text file
	 * @param step		Sampling step for table
	 * @param width
	 * @param height
	 * @param slices
	 * @param frames
	 * @param zDiff		Multiple of z calibration compared to xy calibration (how much bigger is a slice than a pixel)
	 */
	
	public PDA_PS_Positional(String posPath,int width, int height, int slices, int frames,double zDiff){
		this.width=width;
		this.height = height;
		this.slices = slices;
		this.frames = frames;
		this.zDiff = zDiff;
		useMask = false;
		mask = blankMask();
		splitMask = false;
		this.posPath = posPath;
		if(posPath.endsWith(".xml")){
			importCellCounter(posPath);
		} else if (posPath.endsWith(".mdf") || posPath.endsWith(".mtj")){
			importMTrackJ(posPath);
		} else {
			importCategoriser(posPath);
		}
	}
	
	/**
	 * Constructor for either using a mask image
	 * 
	 * @param posPath	Path to positional file (Cell Counter xml, MTrackJ mtj or simple Categorised text file
	 * @param step		Sampling step for table
	 * @param maskImp	Image used for template and/or mask
	 * @param useMask	True if mask is to be used
	 */
	
	public PDA_PS_Positional(String posPath, ImagePlus maskImp, boolean splitMask){
		this(posPath,maskImp.getWidth(), maskImp.getHeight(),maskImp.getNSlices(),maskImp.getNFrames(), maskImp.getCalibration().pixelDepth/ maskImp.getCalibration().pixelWidth);
		useMask = true;
		this.splitMask = splitMask;
		mask = PDA_PS_Image.getMask(maskImp);
		maskImp.close();
		
	}

	

	
	/**
	 * Constructor used for the Confetti_Assessment_Simulator, i.e. injection of externally defined markers.
	 * 
	 * @param markers Arraylist of boolean maps of width/height/slice/frames - one for each channel (i.e. cell type)
	 */
	public PDA_PS_Positional(ArrayList<boolean[][][][]> markers){
		channels = markers.size();
		boolean[][][][] chan1 = markers.get(0);
		width = chan1.length;
		height = chan1[0].length;
		slices = chan1[0][0].length;
		frames = chan1[0][0][0].length;
		useMask = true;
		mask = blankMask();
		splitMask = false;
		populateLocationMap(markers);
		
	}


	
	/**
	 * 
	 * Runs the first step of the analysis: blurring and gathering data from each image.
	 * 
	 * @param blurWithMask		If true, regions are blurred discretely rather than as a whole
	 * @param kernel		Blurring kernel
	 * @param propOfColour		If true, colour values are reported as a proportion of the total coloured pixels within the kernel
	 * @param propOfTotal		If true, colour values are reported as a proportion of the total volume within the kernel
	 * @param normColour		If true, normalise the coloured values such that the brightest value of the image is 1
	 * @param normAll		If true, normalise each pixel so that the sum of colours is equal to 1, correcting for regional density/brightness variations
	 * @param addDensity		If true, take the density of colour as an additional variable
	 * @param normDensity		If true, normalise the density values so that the most dense region is 1.
	 */

	public double[][] analyse(boolean blurWithMask, float[] kernel, boolean propOfColour, boolean propOfTotal, boolean normColour, boolean normAll, boolean addDensity, boolean normDensity, double zDiff, boolean useMask){
		
		FloatProcessor[][][] blurred = new FloatProcessor[0][][];
		if(blurWithMask){
			blurred = splitBlurWithMask(locationMap, kernel);
		} else {
			blurred =blurWithMask(locationMap, kernel,-1);
		}
		proportions =  sample(blurred);
		return proportions;
	}
	
	
	

	/**
	 * 
	 * Populates location table from an XML file from Kurt De Vos' Cell Counter plugin. Modified from that plugin.
	 * 
	 * @param path
	 */
	
	private void importCellCounter(String path){
		try{
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(new File(path));
	        
	        NodeList markerTypeNodeList = doc.getElementsByTagName("Marker_Type");
	        channels = markerTypeNodeList.getLength();
	        locationMap = new boolean[width][height][slices][channels][frames];
	        for (int i=0; i<channels; i++){
	            Element markerTypeElement = (Element)markerTypeNodeList.item(i);
	            
	            NodeList markerNodeList = markerTypeElement.getElementsByTagName("Marker");
	            for(int j=0; j<markerNodeList.getLength(); j++){
	                Element markerElement = (Element)markerNodeList.item(j);
	                NodeList markerXNodeList = markerElement.getElementsByTagName("MarkerX");
	                NodeList markerYNodeList = markerElement.getElementsByTagName("MarkerY");
	                NodeList markerZNodeList = markerElement.getElementsByTagName("MarkerZ");
	                int x = Integer.parseInt(readValue(markerXNodeList,0));
	                int y = (Integer.parseInt(readValue(markerYNodeList,0)));
	                int z = (Integer.parseInt(readValue(markerZNodeList,0)));
	                locationMap[x][y][z-1][i][0]=true;
	            }
	        }
		} catch (Exception e){
			IJ.log("Error Reading XML File");
		}
		
	}
	
	/**
	 * 
	 * Necessary Utility method from Cell Counter by Kurt De Vos
	 * 
	 * @param nodeList
	 * @param index
	 * @return
	 * @throws NullPointerException
	 */
	
	 private String readValue(NodeList nodeList, int index) throws NullPointerException{
	        Element element = (Element)nodeList.item(index);
	        NodeList elementNodeList = element.getChildNodes();
	        String str = elementNodeList.item(0).getNodeValue().trim();
	        return str;
	  }
	
	/**
	 * 
	 * Populates locationMap from MtrackJ file. Heavily adapted from run method of MTJ_Reader by Erik Meijering.
	 * 
	 * @param path	Path to MtrackJ file
	 */
	
	private void importMTrackJ(String path){
		ArrayList<boolean[][][][]> channelArrays = new ArrayList<boolean[][][][]>();
		locationMap = new boolean[width][height][slices][channels][frames];
		BufferedReader br = null;
		

		
		try {
			
			br = new BufferedReader(new FileReader(path));
			String line = br.readLine();			
			line = br.readLine(); 
			if (line.startsWith("Displaying")) {
				line = br.readLine(); 
			}
			if (line.startsWith("Offset")) {
				line = br.readLine(); 
			}
			if (line.startsWith("Origin") || line.startsWith("Reference")) {
				line = br.readLine();
			}
			while (line.startsWith("Assembly")) {
				line = br.readLine();
				while (line.startsWith("Cluster")) {
					line = br.readLine();
					while (line.startsWith("Track")) {
						line = br.readLine();
						while (line.startsWith("Point")) {
							final StringTokenizer pitems = new StringTokenizer(line);
							pitems.nextToken(); // Skip "Point"
							pitems.nextToken(); // Skip Point ID
							int x = (int) Math.round(Double.parseDouble(pitems.nextToken()));
							int y = (int) Math.round(Double.parseDouble(pitems.nextToken()));
							int z = (int) Math.round(Double.parseDouble(pitems.nextToken()));
							int t = (int) Math.round(Double.parseDouble(pitems.nextToken()));
							int c = (int) Math.round(Double.parseDouble(pitems.nextToken()));
							
							if(x<0) x = 0;
							if(y<0) y = 0;
							if(z<1) z = 1;
							if(t<1) t = 1;
							if(c<1) c = 1;
							
							if(x>= width) x = width-1;
							if(y>=height) y = height-1;
							if(z>slices) z = slices;
							if(t>frames) t = frames;

							
							//IJ.log(x+"/"+width+" "+y+"/"+height+" "+z+"/"+slices+" "+t+"/"+frames+" "+c+"/"+channels);
							
							while(channelArrays.size()<c) {
								channelArrays.add(new boolean[width][height][slices][frames]);
							}
							channelArrays.get(c-1)[x][y][z-1][t-1] = true;
							
							line = br.readLine();
							
						}
					}
				}
			}
			populateLocationMap(channelArrays);
			br.close(); 
			
		} catch(OutOfMemoryError e) {
			IJ.log("Out of memory while reading from \""+path+"\"");
			try { br.close(); } catch (Throwable x) { }
			
		} catch (Throwable e) {
			IJ.log("Could not read or interpret file");
			e.printStackTrace();
			IJ.error("An error occurred while reading from \""+path+"\"");
			try { br.close(); } catch (Throwable x) { }
		}

	}
	
	/**
	 * 
	 * Populates the location map from the ArrayList of multidimensional boolean arrays used by importCategoriser and importMTrackJ.
	 * 
	 * @param channelArrays
	 */
	
	private void populateLocationMap(ArrayList<boolean[][][][]> channelArrays){
		// Convert and store boolean array
		channels = channelArrays.size();
		int count = 0;
		
		//Remove any blank channels (which may occur if positional file channels are not sequential)
		chanloop:
		for(int c = 0; c<channels; c++) {
			boolean[][][][] thisChannel = channelArrays.get(c);
			for(int t = 0; t<frames; t++){
				for(int z = 0; z<slices; z++){
					for(int x = 0; x<width; x++){
						for(int y= 0; y<height; y++){
							if(thisChannel[x][y][z][t]) continue chanloop;
						}
					}
				}
			}
			channelArrays.remove(c);
			c--;
			channels--;
		}
		
		locationMap = new boolean[width][height][slices][channels][frames];
		for(int c = 0; c<channels; c++){
			boolean[][][][] thisChannel = channelArrays.get(c);
			for(int t = 0; t<frames; t++){
				for(int z = 0; z<slices; z++){
					for(int x = 0; x<width; x++){
						for(int y= 0; y<height; y++){
							if(thisChannel[x][y][z][t]) count++;
							locationMap[x][y][z][c][t] = thisChannel[x][y][z][t];
						}
					}
				}
			}
		}
		IJ.log(count+" points logged");
	}
	
	
	/**
	 * 
	 * Populates locationMap from a tab separated values file.
	 * 
	 * Different cell categories are split by a line which starts with the word 'CATEGORY'. This is equivalent to the 'channels'.
	 * The cell parameters must be provided in the format X Y Z T, separated by tabs. Z and T are optional columns. If three columns
	 * are provided the third column is assumed to be Z, not T. Therefore for a flat video, a Z value of 1 must be provided for each
	 * data point.
	 * 
	 * @param path		Path to file.
	 * 
	 */
	private void importCategoriser(String path){
		try{
			ArrayList<boolean[][][][]> channelArrays = new ArrayList<boolean[][][][]>();
			BufferedReader reader = new BufferedReader(new FileReader(new File(path)));
			int currentChannel = 0;
			while(true){
				String line = reader.readLine();
				if(line == null){
					break;
				} else if (line.startsWith("CATEGORY")){
					channelArrays.add(new boolean[width][height][slices][frames]);
					currentChannel++;
				} else {
					String[] lineParts = line.split("\t");
					int t=0,z=0;
					int x = Integer.parseInt(lineParts[0]);
					int y = Integer.parseInt(lineParts[1]);
					if(lineParts.length>2){
						z = Integer.parseInt(lineParts[2]);
					}
					if(lineParts.length>3){
						t = Integer.parseInt(lineParts[3]);
					}
					channelArrays.get(currentChannel)[x][y][z-1][t-1] = true;
				}
			}
			reader.close();
			populateLocationMap(channelArrays);
		} catch (Exception e){
			IJ.showMessage("Error Reading File");
			e.printStackTrace();
		}
		
		
	}
}
