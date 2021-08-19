package src.main.java.polydimensionalpixelspace;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ImageProcessor;

/**
 * <p>This package implements Image PCA functionality: the ability to run a principle components analysis on either the distribution of
 * thresholded pixel values, or the positions of exactly specified points. Outputs are a PCA Table, and a visualisation of the result mapped
 * back to the original image.</p>
 * 
 * <p>This class represents an image that includes thresholded voxel information. Images provided must be thresholded via the Min/Max brightness
 * controls (the minimum is used as the threshold).</p>
 * 
 * @author John MJ Lapage
 * @version 1.0
 * 
 */

public class PDA_PS_Thresholded extends PDA_PS_Image {

	
	public PDA_PS_Thresholded(ImagePlus impIn, boolean masked, boolean splitMask){
		imp=impIn;
		useMask = masked;
		this.splitMask = splitMask;
		if(!useMask) mask = blankMask();
		populate();
	}

	
	/**
	 * 
	 * Gets the dimensions of the image
	 * 
	 */
	private void populate(){
		width = imp.getWidth();
		height = imp.getHeight();
		channels = imp.getNChannels();
		title = imp.getTitle();
		frames = imp.getNFrames();
		slices = imp.getNSlices();
		Calibration calib = imp.getCalibration();
		zDiff = calib.pixelHeight/calib.pixelWidth;
		locationMap = this.thresholdMap();
		if(useMask){
			mask = PDA_PS_Image.getMask(imp);
			channels--;
		} 
	}
	

	
	
	
	/**
	 *
	 * Reads the image for positive (255) pixels and remaps the image as a boolean array.
	 * 
	 * @param stack	
	 * @return map	A zero-indexed five dimensional boolean map arranged X/Y/Slice/Channel/Frame, showing which pixels are occupied by values above the threshold.
	 *   
	 */
	private boolean[][][][][] thresholdMap(){	
		ImageStack stack = imp.getStack();
		boolean[][][][][] map = new boolean[width][height][slices][channels][frames];
		
		for(int t = 0; t<frames; t++){
			for(int z=0; z<slices; z++){
				for(int c = 0; c<channels; c++){
					ImageProcessor ip = stack.getProcessor(imp.getStackIndex(c+1, z+1, t+1));
					for(int x = 0; x<width;x++){
						IJ.showStatus("Logging Pixel Locations "+PDA_Pixelspace.NF.format((double)x/(double)width*100)+"%");
						for(int y=0; y<height; y++){
							if(ip.getPixel(x,y)==255){
								map[x][y][z][c][t] = true;
							} else {
								map[x][y][z][c][t] = false;
							}
							
						}
					}
				}
			}
		}
		return map;
	}
}
