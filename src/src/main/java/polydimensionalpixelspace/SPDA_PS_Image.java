package src.main.java.polydimensionalpixelspace;


/**
 * <p>This package implements Polydimensional Analysis Pixelspace OCA functionality: the ability to run a principle components analysis on either the distribution of
 * thresholded pixel values, or the positions of exactly specified points. Outputs are a Pixel Table, and a visualisation of the result mapped
 * back to the original image.</p>
 * 
 * <p>This class is the abstract superclass for the  types of Polydimensional PS Image - as such, it implements the core shared functionality of 
 * 'blurring' the locations of voxels, sampling the dataset, and applying the PCA or K-Means.</p>
 * 
 * @author John MJ Lapage
 * @version 1.0
 * 
 */

import java.util.ArrayList;
import java.util.Random;

import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.ResultsTable;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.LUT;
import src.main.java.colordistance.Colour_Distance_Analysis;

public abstract class SPDA_PS_Image{
	protected int width,height,channels, frames, slices;
	protected double zDiff;
	protected String path, title;
	protected boolean useMask, splitMask;
	protected ArrayList<int[]> coordinates;
	protected double[][] proportions;
	protected boolean[][][][][] locationMap;
	protected int[][][][] mask;
	protected ImagePlus imp;
	protected int mode;
	


	/**
	 * 
	 * Blurs the entire image, or an individual region
	 * 
	 * @param present	Threshold map of pixels that are above user-defined levels
	 * @param mask		Region mask of the image
	 * @param kernel	The blurring kernel
	 * @param targetMask	The region that should be blurred, ignoring pixels in other masked regions. If -1 this is ignored and all regions are blurred as one
	 * @return		An array of images representing the blurred colour occupancy values
	 * 
	 */
	protected FloatProcessor[][][] blurWithMask(boolean[][][][][] present, float[] kernel, int targetMask){
		int kernelLength = kernel.length;
		int halfKernel = kernelLength/2;
		FloatProcessor[][][][] thisSlice = new FloatProcessor[3][slices][frames][channels+1];
		for(int i = 0; i<3; i++){
			for(int c= 0; c<=channels; c++){
				for(int z = 0; z<slices; z++){
					for(int t = 0; t<frames; t++){
						thisSlice[i][z][t][c]=new FloatProcessor(width,height);
					}
				}
			}

		}

		boolean noDelimit = targetMask == -1;
		
		String statusString = "Blurring Region "+targetMask+": ";
		if(noDelimit){
			statusString = "Blurring: ";
		}
		//blur Y direction
		for(int t = 0; t<frames; t++){
			for(int z = 0; z<slices; z++){
				for(int x = 0; x<width; x++){
					float percent = t/(float)frames+(1/(float)frames*z/(float)slices)+(1/(float)slices*x/(float)width);
					IJ.showStatus(statusString+SPDA_Pixelspace.NF.format(percent*100)+"%");
					for(int y = 0; y<height;y++){
						float [] pixel  = new float[channels];
						float sum = 0.0f;
						for(int c = 0; c<channels;c++){
							pixel[c] = 0.0f;
						}
						for(int k = 0; k<kernelLength;k++){
							int ky=y-halfKernel+k;
							if(ky<0){continue;}
							if(ky>=height){break;}
							int kernelMask = -1;
							if(useMask) kernelMask = mask[z][t][x][ky];
							if(kernelMask == targetMask || (noDelimit && useMask && kernelMask>0) || !useMask){
								for(int c =0; c<channels;c++){
									boolean counted = false;
									if(present[x][ky][z][c][t]){
										pixel[c] += kernel[k];
										if(!counted){
											sum += kernel[k];
											counted = true;
										}
									}
								}
							}
						}
						for(int c =0; c<channels;c++){
							thisSlice[0][z][t][c].setf(x,y,pixel[c]);
						}
						thisSlice[0][z][t][channels].setf(x,y,sum);
					}
				}



				//blur X direction
				for(int y = 0; y<height; y++){
					for(int x = 0; x<width; x++){
						int thisMask = -1;
						if(useMask) thisMask = mask[z][t][x][y];
						if(thisMask == targetMask || (thisMask>0 && noDelimit) || !useMask){
							float [] pixel = new float[channels];
							float sum = 0.0f;
							for(int c = 0; c<channels;c++){
								pixel[c] = 0.0f;
							}
							for(int k = 0; k<kernelLength;k++){
								int kx=x-halfKernel+k;
								if(kx<0){continue;}
								if(kx>=width){break;}
								for(int c =0; c<channels;c++){
									pixel[c] += (kernel[k]*thisSlice[0][z][t][c].getf(kx,y));
								}
								sum += (kernel[k]*thisSlice[0][z][t][channels].getf(kx,y));
							}
							for(int c =0; c<channels;c++){	
								thisSlice[1][z][t][c].setf(x,y,pixel[c]);
							}
							thisSlice[1][z][t][channels].setf(x,y,sum);
						}
					}
				}

			}
			//Blur Z
			for(int z=0;z<slices;z++){
				for(int kz=0; kz<slices;kz++){
					int kernelIndex = (int)(halfKernel+Math.round((z-kz)*zDiff));
					if(kernelIndex<0 || kernelIndex>kernel.length-1) continue;
					float kernelVal= kernel[kernelIndex];
					for(int x = 0; x<width; x++){
						for(int y = 0; y<height; y++){
							for(int c=0; c<channels; c++){
								thisSlice[2][z][t][c].setf(x, y,thisSlice[2][z][t][c].getf(x,y)+(kernelVal*thisSlice[1][kz][t][c].getf(x,y)));
								thisSlice[2][z][t][channels].setf(x, y,thisSlice[2][z][t][channels].getf(x,y)+(kernelVal*thisSlice[1][kz][t][channels].getf(x,y)));
							}
						}
					}

				}
				for(int x = 0; x<width; x++){
					for(int y = 0; y<height; y++){
						int thisMask = -1;
						if(useMask) thisMask = mask[z][t][x][y];
						boolean inMask = thisMask == targetMask || (thisMask>0 && noDelimit) || !useMask;
						float sum = thisSlice[2][z][t][channels].getf(x,y);
						boolean sumNonZero = sum > 0.0f;
						for(int c = 0; c<channels; c++){
							if(!inMask){
								thisSlice[2][z][t][c].setf(x, y,0.0f);
							} else if (sumNonZero){
								thisSlice[2][z][t][c].setf(x, y,thisSlice[2][z][t][c].getf(x,y)/sum);
							}
						}
					}
				}
			}
		}
		return thisSlice[2];
	}

	/**
	 * 
	 * Allows for blurring that does not cross differently indexed regions of the sample, blurring each separately and then combining the results
	 * I.e. implements the 'segregating mask' functionality.
	 * 
	 * @param present	Threshold map
	 * @param mask		An image holding the regional indices of each pixel
	 * @param kernel	The blurring kernel
	 * @return		An array of images representing the blurred colour occupancy values
	 */

	protected FloatProcessor[][][] splitBlurWithMask(boolean[][][][][] present, float[] kernel){
		
		//Find maximum mask index
		int masks = 1;
		for(int t = 0; t<frames; t++){
			for(int z = 0; z<slices; z++){
				for(int x = 0; x<width; x++){
					for(int y = 0; y<height; y++){
						masks = Math.max(masks,mask[z][t][x][y]);
					}
				}
			}
		}

		//Get blurred compartments
		FloatProcessor[][][][] compartments = new FloatProcessor[masks][slices][frames][channels+1];
		for(int i = 0; i<masks;i++){
			compartments[i] = blurWithMask(present, kernel,i+1);
		}
		//merge blurred compartments
		FloatProcessor[][][] output = new FloatProcessor[slices][frames][channels];
		for(int z = 0; z<slices; z++){
			for(int t = 0; t<frames;t++){
				for(int c = 0; c<channels; c++){
					output[z][t][c] = new FloatProcessor(width,height);
				}
			}
		}
		for(int t = 0; t<frames; t++){
			for(int z = 0; z<slices; z++){
				for(int x=0; x<width;x++){
					IJ.showStatus("Reconstituting blurred image "+SPDA_Pixelspace.NF.format((double)x/(double)width)+"%");
					for(int y=0; y<height; y++){
						int thisMask = mask[z][t][x][y]-1;
						if(thisMask == -1){continue;}
						for(int c=0;c<channels;c++){
							output[z][t][c].setf(x,y,compartments[thisMask][z][t][c].getf(x,y));
						}
					}
				}
			}
		}
		return output;
	}

	/**
	 * 
	 * @param blurred	An array of floatprocessers holding the blurred occupancy values for each point
	 * @param blurred	If true, respect mask. If not, ignore.

	 * @return		An array holding pixel values 
	 */

	protected double[][] sample(FloatProcessor[][][] blurred){

		ArrayList<double[]> cellProps = new ArrayList<double[]>();
		coordinates = new ArrayList<int[]>();

		double[] maxColours = new double[channels];
		for(int z = 0; z<slices; z++){
			for(int t = 0; t<frames; t++){
				for(int c=0; c<channels; c++){
					maxColours[c] = Math.max(maxColours[c], blurred[z][t][c].getMax());
				}
			}
		}
		
		for(int z = 0; z<slices ; z++){
			for(int t = 0; t<frames; t++){
				for(int x = 0; x<width; x++){
					IJ.showStatus("Sampling "+SPDA_Pixelspace.NF.format((double)x/(double)width*100)+"%");
					for(int y=0; y<height;y++){
						int thisMask = mask[z][t][x][y];
						if(thisMask == 0){continue;}
						double[] proportions = new double[channels];
							coordinates.add(new int[]{x,y,thisMask,z,t});
							for(int c = 0; c<channels;c++){
								if(maxColours[c] == 0.0){continue;}
								proportions[c]=(double) blurred[z][t][c].getf(x,y);
								proportions[c] = proportions[c]/maxColours[c]; //Normalise within colours
							}
						
						cellProps.add(proportions);
						
					}
				}
			}
		}
		
		//Normalise across colours 
			for(int i = 0; i<cellProps.size(); i++){
				IJ.showStatus("Normalising across Colours "+SPDA_Pixelspace.NF.format((double)i/(double)cellProps.size()*100)+"%");
				double[] thisPixel = cellProps.get(i);
				double sum = 0.0;
				for(int c = 0; c<channels;c++){
					sum += thisPixel[c];
				}
				for(int c = 0; c<channels;c++){
					if(sum != 0.0){
						thisPixel[c]=thisPixel[c]/sum;
				}
			}
		}
		

		//Create double array of proportions for the PCA
		double [][] proportionsArray = new double[cellProps.size()][channels];
    	for(int i = 0; i<proportionsArray.length; i++){
    		proportionsArray[i] = cellProps.get(i);
    	}
		
		return proportionsArray;
	}
	

	/**
	 * Generates a blank (all-inclusive) mask
	 */
	
	protected int[][][][] blankMask() {
		int[][][][] newmask = new int[slices][frames][width][height];
		for(int z = 0; z<slices; z++) {
			for(int t = 0; t<frames; t++) {
				for(int x = 0; x<width; x++) {
					for(int y = 0; y<height; y++) {
						newmask[z][t][x][y] = 1;
					}
				}
			}
		}
		return newmask;
	}

	/**
	 * 
	 * Outputs both the results table and images following the PCA analysis
	 * 
	 * @param eigenvec	The PCA eigenvectors
	 */

	public SPDA_Results getPCAResults(SPDA_Results template){
		double[][] table = applyPCA(template.eigenVec);
		ResultsTable pcaResults = new ResultsTable();
	    	
	    	for(int i = 0; i<table.length;i++){
	    		pcaResults.incrementCounter();
	    		pcaResults.addValue("X",coordinates.get(i)[0]);
	    		pcaResults.addValue("Y",coordinates.get(i)[1]);
	    		if(useMask){
	    			pcaResults.addValue("Region",coordinates.get(i)[2]);
	    		}
	    		if(slices>1){
	    			pcaResults.addValue("Slice",coordinates.get(i)[3]+1);
	    		}
	    		if(frames>1){
	    			pcaResults.addValue("Frame",coordinates.get(i)[4]+1);
	    		}
	    		for(int j=0; j<table[0].length;j++){
	    			pcaResults.addValue("PC"+(j+1),table[i][j]);
	    		}
	    		
	    	}
	    	pcaResults.setPrecision(8);
		ImageStack stack = new ImageStack(width,height);
		int maxC = Math.min(3,channels);

		int totalSlices = maxC*slices*frames;
		for(int i = 0; i<totalSlices; i++){
			stack.addSlice(new FloatProcessor(width,height));
			
		}
		CompositeImage output = new CompositeImage(new ImagePlus("Image PCA of "+title,stack),CompositeImage.COMPOSITE);
		output.setDimensions(maxC, slices, frames);
		
		FloatProcessor canvas = (FloatProcessor) stack.getProcessor(1);
		for(int c = 0; c<maxC; c++){
			int lastZ = -1; 
			int lastT = -1;
			for(int i = 0; i<table.length; i++){
				int[] coord = coordinates.get(i);
				if(coord[3]!=lastZ || coord[4]!=lastT){
					lastZ=coord[3];
					lastT=coord[4];
					canvas= (FloatProcessor) stack.getProcessor(output.getStackIndex(c+1, coord[3]+1, coord[4]+1));
				}
				 canvas.setf(coord[0],coord[1],(float)table[i][c]);
			 }
		}
		
		
		
		SPDA_Results results = template.duplicate();
		results.pdaImp = output;
		results.results = pcaResults;
		return results;
		
	}
	
	 	


	/**
	 * Creates a byte version of a mask channel
	 */
	
	public static int[][][][] getMask(ImagePlus imp){
		ImageStack stack = imp.getStack();
		int slices = imp.getNSlices();
		int frames = imp.getNFrames();
		int maskChannel = imp.getNChannels();
		int width = imp.getWidth();
		int height = imp.getHeight();
		int[][][][] mask = new int[slices][frames][width][height];
		for(int z = 1; z<=slices; z++){
			for(int t=1; t<=frames; t++){
				ImageProcessor ip = stack.getProcessor(imp.getStackIndex(maskChannel,z,t));
				int nt = t-1;
				int nz = z-1;
				for(int x = 0; x<width; x++){
					for(int y=0; y<height; y++){
						mask[nz][nt][x][y]=(int) ip.getPixel(x, y);
					}
				}
			}
		}
		return mask;
	}
	
	/**
	 * Returns the mask of a particular slice and timepoint
	 * 
	 * @param z		Slice coordinate (0 indexed)
	 * @param t		Time coordinate (0 indexed)
	 * @return		The mask at the slice and time coordinates, as a byteprocessor
	 */
	public ByteProcessor returnMask(int z, int t) {
		ByteProcessor maskbp = new ByteProcessor(width,height);
		for(int x = 0; x<width; x++) {
			for(int y = 0; y<height; y++) {
				maskbp.set(x, y,mask[z][t][x][y]);
			}
		}
		
		return maskbp;
	}
	
	/**
	 * Returns the entire mask
	 * 
	 * @return	The mask of this PCAImage as an ImagePlus
	 */
	public ImagePlus returnMask() {
		ImageStack stack = new ImageStack(width,height);
		for(int t=0; t<frames; t++) {
			for(int z = 0; z<slices; z++) {
					stack.addSlice(returnMask(z,t));
			}
		}
		ImagePlus returnImp = new ImagePlus("Mask of "+title,stack);
		returnImp.setDimensions(1, slices, frames);
		return returnImp;
	}


	/**
	 * 
	 * Applies PCA to array of pixel values, converting the table's colour values to their positions within PCA Space
	 * 
	 * @param eigenVec	The output of the PCA Analysis
	 * @param table		The table of colour values to be converted
	 * @return		The original table with values plotted in pca space
	 * 
	 */
	 private double[][] applyPCA(double[][] eigenVec){
	 	int rows = proportions.length;
	 	int pcs = eigenVec[0].length;
	 	double[][] pCsArray = new double[rows][pcs];
	 	for(int pc = 0; pc<pcs; pc++){
	 		for(int i = 0; i<rows; i++){
	 			double sum = 0.0;
	 			for(int j = 0; j<proportions[0].length; j++){
	 				sum-= proportions[i][j]*eigenVec[j][pc];
	 			}
	 			pCsArray[i][pc]=sum;
	 		}
	 	}
	 	return pCsArray;
	 }
	 
		/**
		 * 
		 * Runs the first step of the analysis: blurring and gathering data from each image.
		 * 
		 * @param blurWithMask		If true, regions are blurred discretely rather than as a whole
		 * @param kernel		Blurring kernel

		 */

		public double[][] analyse(float[] kernel){
			
			FloatProcessor[][][] blurred = new FloatProcessor[0][][];
			if(splitMask){
				blurred = splitBlurWithMask(locationMap, kernel);
			} else {
				blurred =blurWithMask(locationMap, kernel,-1);
			}
			proportions =  sample(blurred);
			
			return proportions;
		}
		

		ImagePlus drawKMeansImage(int[][][][] mask, int[] groups) {
			String title = "Test K-means Image";
			if(imp !=null) title = "K Means in "+imp.getTitle();
			ImagePlus kImp = IJ.createHyperStack(title, width, height, 1, 1, 1, 8);
			ByteProcessor kbp = (ByteProcessor) kImp.getChannelProcessor();
			int i = 0;
			for(int x = 0; x<width; x++) {
				for(int y=0; y<height; y++) {
					if(mask[0][0][x][y]>0) {
						kbp.set(x,y,groups[i]+1);
						i++;
					}
				}
			}
			kImp.setLut(kMeansLUT());
			return kImp;
		}
		
		/**
		 * Perform a K-Means analysis of the sample table
		 */
		
		int[] doKMeans(double[][] sampletable, int kmeans, int maxIterations){
			int rows = sampletable.length;
			int colours = sampletable[0].length;
			double[][] centroids = new double[kmeans][colours];
			Random rand = new Random();
			
			//Pick random rows from the sampletable as starting centroids
			for(int i = 0; i<kmeans; i++) {
				centroids[i] = sampletable[rand.nextInt(rows)];
			}
			
			int[] groupAssignments = new int[rows];
			boolean updated = true;
			
			
			//Repeat until the points are no longer reassigned
			for(int iter = 0; iter<maxIterations; iter++) {
				IJ.log("ITERATION "+iter);
				int[] newGroupAssignments = new int[rows];
				float[][] colourSums = new float[kmeans][colours];
				int[] groupCounts = new int[kmeans];
				updated = false;
				
				//For each row, find the nearest centroid in colour space and assign
				for(int i = 0; i<rows; i++) {
					float bestDistance = Float.MAX_VALUE;
					newGroupAssignments[i]= -1;
					for(int j = 0; j<kmeans; j++) {
						float distance = Colour_Distance_Analysis.getColourDistance(sampletable[i],centroids[j],true,false,false);
						distance = distance*distance;
						if(distance<bestDistance) {
							bestDistance = distance;
							newGroupAssignments[i]=j;
						}
					}
					
					groupCounts[newGroupAssignments[i]]++;
					
					for(int j = 0; j<colours;j++) {
						colourSums[newGroupAssignments[i]][j] += sampletable[i][j];
					}
					//If it has been reassigned since the last iteration we will need to repeat
					if(newGroupAssignments[i] != groupAssignments[i]) updated = true;
				}
				groupAssignments = newGroupAssignments;
				
				//recalculate centroids
				for(int i = 0; i<kmeans;i++) {
					IJ.log("Group "+i+": "+groupCounts[i]);
					if(groupCounts[i]>0) {

						for(int j = 0; j<colours; j++) {
							centroids[i][j] = colourSums[i][j]/groupCounts[i];
							IJ.log("C"+(j+1)+": "+centroids[i][j]);
		
						}
					}
					
				}
				if(!updated) break;
			}
			
			return groupAssignments;
		}
		
		
		/**
		 * Provides a LUT for Kmeans results with primary and secondary colours first and then random colours afterwards
		 * @return
		 */
		private LUT kMeansLUT() {
			Random rand = new Random();
			byte[] r = new byte[256];
			byte[] g = new byte[256];
			byte[] b = new byte[256];
			
			//Fill the arrays with random colours
			rand.nextBytes(r);
			rand.nextBytes(g);
			rand.nextBytes(b);
			
			//Impose sensible values on the first 8 clusters, and black for 0
			for(int i = 0; i<8; i++) {
				r[i] = (byte)0;
				g[i] = (byte)0;
				b[i] = (byte)0;
			}
			
			r[1] = (byte)255;
			g[2] = (byte)255;
			b[3] = (byte)255;
			
			r[4] = (byte)255;
			g[4] = (byte)255;
			
			r[5] = (byte)255;
			b[5] = (byte)255;
			
			g[6] = (byte)255;
			b[6] = (byte)255;
			
			r[7] = (byte)255;
			g[7] = (byte)255;
			b[7] = (byte)255;
			
			
			//Check that all of the rest are sufficiently bright, if not, reassign.
			for(int i = 7; i<256; i++) {
				while((int)r[i]+(int)g[i]+(int)b[i]<100) {
					r[i] = (byte) rand.nextInt(256);
					g[i] = (byte) rand.nextInt(256);
					b[i] = (byte) rand.nextInt(256);
				}
			}
			
			return new LUT(r,g,b);
		}
}