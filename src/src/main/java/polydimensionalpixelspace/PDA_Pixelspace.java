package src.main.java.polydimensionalpixelspace;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;
import ij.IJ;
import ij.ImagePlus;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.filter.GaussianBlur;


/**
 * <p>This package implements Image PCA functionality: the ability to run a principle components analysis on either the distribution of
 * thresholded pixel values, or the positions of exactly specified points. Outputs are a PCA Table, and a visualisation of the result mapped
 * back to the original image.</p>
 * 
 * <p>This class is the main interface for the package. It includes methods for running this analysis from other plugins, as a macro, or for 
 * launching the graphical user interface.</p>
 * 
 * @author John Lapage
 * @version 1.0
 * 
 */

public class PDA_Pixelspace implements PlugIn{
	
	public static final String[] POSITIONALFORMATS = new String[]{"mtj","mdf","txt","csv","xml"};
	public static final String[] IMAGEFORMATS = new String[]{"tiff","tif"};
	public static final int PCA_MODE = 1, K_MODE = 2;

	public static final DecimalFormat NF = new DecimalFormat("#.00");

	
	/**
	 * 
	 * The simple (and original) implementation of the plugin. Takes the current thresholded image, runs the PCA
	 * and displays the results immediately. This is the only instance where the implementation and the interface are linked
	 * as this is a user-interface driven process
	 * 
	 * @param useMask		True if some form of mask is used, false if not.
	 * @param splitMask		True if mask is used in segregating mode
	 * @param covariance 	True if PCA done on Correlation matrix, false if used on covariance matrix (ignored if KMeans Mode)
	 * @param sigma			Value of blurring sigma
	 * @param kmeansMode	If true, perform k-means analysis
	 * @param kGroups		If in k-means mode, number of K groups (ignored if PCA mode)
	 * @param kIter			If in k-means mode, number of permitted iterations for k-means analysis(ignored if PCA mode)
	 * 
	 */
	public static void processCurrentImage(boolean useMask, boolean splitMask, boolean covariance, double sigma, boolean kmeansMode, int kGroups, int kIter){
		IJ.log("Processing Current Image");

			ArrayList<PDA_PS_Image> pcaImages = new ArrayList<PDA_PS_Image>();
			pcaImages.add(new PDA_PS_Thresholded(IJ.getImage(),useMask,splitMask));
			PDA_Results result  = coreProcess(pcaImages,true,covariance,sigma, kmeansMode, kGroups,kIter).get(0);
			result.pdaImp.show();
			if(!kmeansMode) {
				result.eigenVectorTable.show("EigenVectors");
				result.weightingsTable.show("Loadings Matrix");
				result.results.show("PCA Results");
			}
		
	}
	
	/**
	 * 
	 * Method for initiating the analysis of multiple thresholded images. Takes a directory, creates the PCAImages and passes onto main multiple image mode.
	 * 
	 * @param dir			Directory of images to be analysed
	 * @param series		True if series, false if global
	 * @param useMask		True if some form of mask is used, false if not.
	 * @param splitMask		True if mask is used in segregating mode
	 * @param covariance 	True if PCA done on covariance matrix, false if used on correlation matrix
	 * @param sigma			Value of blurring sigma
	 * 
	 */
	public static ArrayList<PDA_Results> processThresholdedDirectory(String dir, boolean series, boolean useMask, boolean splitMask, boolean covariance, double sigma, boolean kmeansMode, int kGroups, int kIter){
		ArrayList<PDA_PS_Image> pcaImages = new ArrayList<PDA_PS_Image>();
		String[] fileNames = new File(dir).list(new QuickFilter(IMAGEFORMATS));
		for(int i = 0; i<fileNames.length; i++){
			ImagePlus imp = IJ.openVirtual(dir+fileNames[i]);
			pcaImages.add(new PDA_PS_Thresholded(imp,useMask,splitMask));
			imp.close();
		}
		return coreProcess(pcaImages,series,covariance,sigma, kmeansMode, kGroups,kIter);
		
	}
	
	/**
	 * 
	 * Never actually used, but provided for convenience for future programmers: method for taking an Array of ImagePlus objects
	 * 
	 * @param dir			Directory of images to be analysed
	 * @param closeImps		True if images should be discarded after loading (recommended, for memory purposes, but not required.
	 * @param series		True if series, false if global
	 * @param useMask		True if some form of mask is used, false if not.
	 * @param splitMask		True if mask is used in segregating mode
	 * @param covariance 	True if PCA done on covariance matrix, false if used on correlation matrix
	 * @param sigma			Value of blurring sigma
	 * 
	 */
	public static ArrayList<PDA_Results> processThresholdedImageSet(ImagePlus[] imps, boolean closeImps, boolean series, boolean useMask, boolean splitMask, boolean covariance, double sigma, boolean kmeansMode, int kGroups, int kIter){
		ArrayList<PDA_PS_Image> pcaImages = new ArrayList<PDA_PS_Image>();
		for(int i = 0; i<imps.length; i++){
			pcaImages.add(new PDA_PS_Thresholded(imps[i],useMask,splitMask));
			if(closeImps){
				imps[i].close();
			}
		}
		return coreProcess(pcaImages,series,covariance,sigma, kmeansMode, kGroups,kIter);
		
	}
	

	
	/**
	 * 
	 * Method for initiating the analysis of multiple positional images. Takes a directory, works out the file names and passes it onto the main positional handling method.
	 * 
	 * @param dir			Directory of images to be analysed
	 * @param dimensional	If Dimension table used, path to that file. If template images used, path to that directory. If manual dimension entry, ignored string.
	 * @param manual		True if manual entry of dimensions
	 * @param table			True if loaded table entry of dimensions
	 * @param series		True if series, false if global
	 * @param useMask		True if some form of mask is used, false if not.
	 * @param splitMask		True if mask is used in segregating mode
	 * @param correlation 	False if PCA done on Correlation matrix, True if used on covariance matrix
	 * @param sigma			Value of blurring sigma
	 * 
	 */
	public static ArrayList<PDA_Results> processPositionalDirectory(String dir, String tableFilePath, boolean manual, boolean table, boolean series, boolean useMask, boolean splitMask, boolean covariance, double sigma, boolean kmeansMode, int kGroups, int kIter){
		File directory = new File(dir);
		if(!directory.exists()){
			IJ.showMessage("Invalid Directory");
			return null;
		} else {
			String[] fileNames = directory.list(new QuickFilter(POSITIONALFORMATS));
			return processPositional(dir,fileNames,tableFilePath,manual,table,series,useMask,splitMask,covariance,sigma, kmeansMode, kGroups, kIter);
		}
	}
	
	/**
	 * 
	 * Main positional processing method - used by single file as well as multiples.
	 * 
	 * @param dir			Path to directory of files
	 * @param fileNames		Names of files to be analysed
	 * @param dimensional	If Dimension table used, path to that file. If template images used, path to that directory. If manual dimension entry, ignored string.
	 * @param manual		True if manual entry of dimensions
	 * @param table			True if loaded table entry of dimensions
	 * @param series		True if series, false if global
	 * @param useMask		True if some form of mask is used, false if not.
	 * @param splitMask		True if mask is used in segregating mode
	 * @param covariance 	False if PCA done on Correlation matrix, True if used on covariance matrix
	 * @param sigma			Value of blurring sigma
	 * 
	 */
	public static ArrayList<PDA_Results> processPositional(String dir, String[] fileNames, String tableFilePath, boolean manual, boolean table, boolean series, boolean useMask, boolean splitMask, boolean covariance, double sigma, boolean kMeansMode, int kGroups, int kIter){
		double[][] dimensions;
		ArrayList<PDA_PS_Image> pcaImages = new ArrayList<PDA_PS_Image>();
		String[] maskFiles = new File(dir).list(new QuickFilter(IMAGEFORMATS));

		
		if(manual){
			Manual_Dimension_Table_Dialog dialog = new Manual_Dimension_Table_Dialog(fileNames);
			dimensions = dialog.getDimensions();
		} else if (table){
			dimensions = dimensionsFromTable(fileNames,tableFilePath);
		} else if (useMask){
			dimensions = new double[0][0];
		} else {
			dimensions = dimensionsFromImages(fileNames,dir);
		}
		
		boolean loaded = true;
		toploop:
		for(int i = 0; i<fileNames.length; i++){
			if(useMask){
				String shortName = fileNames[i].split("\\.")[0];
				for(int j = 0; j<maskFiles.length;j++){
					if(maskFiles[j].startsWith(shortName)){
						pcaImages.add(new PDA_PS_Positional(dir+fileNames[i],IJ.openVirtual(dir+maskFiles[j]),splitMask));
						continue toploop;
					}
					if(j==maskFiles.length-1){
						loaded=false;
						IJ.showMessage("No mask found.\nCould not find "+shortName+".tif or "+shortName+".tiff in "+dir);
						break toploop;
					}
				}
			} else {
				pcaImages.add(new PDA_PS_Positional(dir+fileNames[i],(int)Math.round(dimensions[i][1]),(int)Math.round(dimensions[i][2]),(int)Math.round(dimensions[i][3]),(int)Math.round(dimensions[i][4]),dimensions[i][0]));
			}
		}
		if(loaded){
			return coreProcess(pcaImages,series,covariance,sigma, kMeansMode, kGroups, kIter);
		} else {
			return null;
		}
	}
	
	
	public static float[] makeKernel(double sigma) {
		//Make the kernel
				GaussianBlur gb = new GaussianBlur();
				float[][] halfKernel = gb.makeGaussianKernel(sigma,0.01,500);
				float[] kernel = new float[halfKernel[0].length*2];
				for(int i = 0; i<kernel.length; i++){
					if(i<halfKernel[0].length){
						kernel[i] = halfKernel[0][halfKernel[0].length-i-1];
					} else {
						kernel[i] = halfKernel[0][i-halfKernel[0].length];
					}
				}
				return kernel;
	}
	
	
	/**
	 * 
	 * Core method for processing - takes the ArrayList of PDAImages, implements and returns the analysis
	 * 
	 */
	public static ArrayList<PDA_Results> coreProcess(ArrayList<PDA_PS_Image> pdaImages, boolean series,boolean covariance,double sigma, boolean kMeansMode, int kGroups, int kIter){
		ArrayList<double[]> growList =  new ArrayList<double[]>();
		ArrayList<PDA_Results> results = new ArrayList<PDA_Results>();
		
		
		//Make the kernel
		float[] kernel = makeKernel(sigma);

		if(kMeansMode) {
			for(int i = 0; i<pdaImages.size(); i++){
				PDA_PS_Image imp = pdaImages.get(i);
				double[][] sampletable = imp.analyse(kernel);
				int[] groups = imp.doKMeans(sampletable, kGroups, kIter);
				ImagePlus kImp = imp.drawKMeansImage(imp.mask,groups);
				results.add(new PDA_Results(kImp));
			}
		} else {
		
			for(int i = 0; i<pdaImages.size(); i++){
				if(series){
					double[][] sampleTable = pdaImages.get(i).analyse(kernel);
					PDA_Results template = doPCA(sampleTable,covariance);
					results.add(pdaImages.get(i).getPCAResults(template));
				} else {
					growList.addAll(Arrays.asList(pdaImages.get(i).analyse(kernel)));
				}
			}
			
			if(!series){
				double[][] sampleTable = toArray(growList);
				PDA_Results template = doPCA(sampleTable,covariance);
				for(int i = 0; i<pdaImages.size(); i++){
					results.add(pdaImages.get(i).getPCAResults(template));
				}
			}
		}
		
		
		
		return results;
	}
	
	/**
	 * Converts the double[] list into a double[][] - simply using toArray and casting does not work for this.
	 * 
	 * @param list		A list of double[]
	 * @return			The same data represented as a double[][]
	 */
	public static double[][] toArray(ArrayList<double[]> list){
		double[][] array = new double[list.size()][list.get(0).length];
		for(int i = 0; i<array.length; i++){
			array[i] = list.get(i);
		}
		return array;
	}
	
	/**
	 *
	 *	Get Dimensions from a directory of images. If using masks, user is forced to use this (because the mask must always
	 *	match the image dimensions, and this saves them the most time).
	 *
	 * @param fileNames
	 * @param dir
	 * @return
	 */
	
	private static double[][] dimensionsFromImages(String[] fileNames,String dir){
		String[] fileList = new File(dir).list();
		double[][] dimensions = new double[fileNames.length][5];
		toploop:
		for(int i = 0; i<fileNames.length; i++){
			String subName = fileNames[i].split("\\.")[0];
			for(int j = 0; j<fileList.length; j++){
				if(fileList[j].contains(subName) &&( fileList[j].endsWith(".tif") ||  fileList[j].endsWith(".tiff"))){
					ImagePlus template = IJ.openVirtual(dir+fileList[j]);
					Calibration calib = template.getLocalCalibration();
					dimensions[i][0] = calib.pixelDepth/calib.pixelWidth;
					dimensions[i][1] = template.getWidth();
					dimensions[i][2] = template.getHeight();
					dimensions[i][3] = template.getNSlices();
					dimensions[i][4] = template.getNFrames();
					template.close();
					continue toploop;
				}
				if(j==fileList.length-1){
					IJ.showMessage(fileNames[i]+" Template Image Not Found - Image should have the same title.");
					return null;
				}
			}
		}
		return dimensions;
	}
	


	/**
	 * 
	 * Imports image dimensions from a user-specified file. This must be provided in the order filename, Z calibration difference, x,y,z,t.
	 * Third column is assumed to be z if provided. Z and T are 1-indexed.
	 * 
	 * @param fileNames
	 * @param tablePath
	 * @return
	 */
	private static double[][] dimensionsFromTable(String[] fileNames,String tablePath){
		int numberFiles = fileNames.length;
		try{
			BufferedReader reader = new BufferedReader(new FileReader(new File(tablePath)));
			String line = reader.readLine();
			int length = line.split("\t").length-1;
			double[][] table = new double[numberFiles][length];
			int lineCount = 0;
			while(true){
				line = reader.readLine();
				if(line == null) break;
				String[] lineParts = line.split("\t");
				lineCount++;
				String thisFile = lineParts[0].trim();
				for(int i = 0; i<numberFiles; i++){
					if(fileNames[i].equals(thisFile) || fileNames[i].startsWith(thisFile) || thisFile.startsWith(fileNames[i])){
						double zDiff,x,y,z,t;
						zDiff=Double.parseDouble(lineParts[1]);
						x = Double.parseDouble(lineParts[2]);
						y = Double.parseDouble(lineParts[3]);
						if(length>4){
							z = Double.parseDouble(lineParts[4]);
							if(z == 0) z=1;
						} else {
							z = 1;
						}
						if(length>5){
							t = Double.parseDouble(lineParts[5]);
							if(t==0) t = 1;
						} else {
							t = 1;
						}
						table[i] = new double []{zDiff,x,y,z,t};
					}
				}
			}
			reader.close();
			if(lineCount < numberFiles){
				IJ.showMessage("Insufficient Rows");
				return null;
			} else {
				return table;
			}
		} catch (Exception e){
			IJ.showMessage("Error Reading Dimensions Table");
			return null;
		}
	}
	
	
	
	/**
	 * Saves an arraylist of results to the desired directory
	 * 
	 * @param results	ArrayList of PcaResults
	 * @param dir		Target Directory Path
	 * @param series	If true, each analysis will have its own eigenvector and weightings table saved. If false, only the first will be saved (as it will be the same for all analyses).
	 */
	
	public static void saveResults(ArrayList<PDA_Results> results, String dir, boolean series){
		for(int i = 0; i<results.size(); i++){
			PDA_Results result = results.get(i);
			String title = result.pdaImp.getTitle();
			if(series){
				result.eigenVectorTable.save(dir+title+" EigenVector Table");
				result.weightingsTable.save(dir+title+" Matrix");
			} else if (i==0){
				result.eigenVectorTable.save(dir+"EigenVector Table");
				result.weightingsTable.save(dir+"Matrix");
			}
			IJ.saveAsTiff(result.pdaImp,dir+title+"-pcaresult");
			result.results.save(dir+title+" PCA Results");
		}
	}
	
	public static PDA_Results doPCA(double[][] table, boolean covar){
		PDA_Results results = new PDA_Results();
		int rows = table.length;
		int columns = table[0].length;
		
		//Calculate means for each channel
		double[] means = new double[columns];
		for(int i = 0; i<rows;i++){
			for(int c = 0; c<columns;c++){
				means[c]+= table[i][c];
			}
		}
		for(int c = 0; c<columns;c++){
			means[c]= means[c]/rows;
		}

		//Calculate  matrix
		Matrix matrix = new Matrix(columns,columns);

		ResultsTable matrixLog = new ResultsTable();

		//Calculate covariance for each pair
		if(covar){
			
			for(int y = 0; y<columns;y++){
				IJ.showStatus("Calculating Covariance "+PDA_Pixelspace.NF.format((double)y/columns*100)+"%");
				matrixLog.incrementCounter();
				for(int x=0; x<columns;x++){
					double sum = 0.0;
					for(int i = 0; i<rows;i++){
						sum += ((table[i][x]-means[x])*(table[i][y]-means[y]));
					}
					double covariance = sum/(rows-1);
					matrix.set(x,y,covariance);
					matrixLog.addValue("C"+x,covariance);
					if(Double.isNaN(covariance)){
						IJ.showMessage("Failure to calculate Covariance Matrix");
						matrixLog.show("Correlation Matrix");
						return null;
					}
				}
			}
			matrixLog.show("Covariance Matrix");
		} else { // calculate correlation
			for(int y = 0; y<columns;y++){
				IJ.showStatus("Calculating Correlation "+PDA_Pixelspace.NF.format((double)y/columns*100)+"%");
				matrixLog.incrementCounter();
				for(int x=0; x<columns;x++){
					double sumAA = 0.0;
					double sumBB = 0.0;
					double sumAB = 0.0;
					for(int i = 0; i<rows;i++){
						sumAB += ((table[i][x]-means[x])*(table[i][y]-means[y]));
						sumAA += ((table[i][x]-means[x])*(table[i][x]-means[x]));
						sumBB += ((table[i][y]-means[y])*(table[i][y]-means[y]));
					}
					double correlation = sumAB/Math.sqrt(sumAA*sumBB);
					
					matrix.set(x,y,correlation);
					
					matrixLog.addValue("C"+x,correlation);
					if(Double.isNaN(correlation)){
						IJ.showMessage("Failure to calculate Correlation Matrix");
						matrixLog.show("Correlation Matrix");
						return null;
					}
				}
			}
			matrixLog.show("Correlation Matrix");
		}
		IJ.showStatus("Computing Eigenvectors");
		//Compute eigenvectors
		EigenvalueDecomposition eigen = matrix.eig();
		
		double[] eigenVal = eigen.getRealEigenvalues();
		double[][] eigenVec = eigen.getV().getArray();

		//Sort by eigenvalue
		//Get order of eigenvalues
		IJ.showStatus("Sorting");
		int[] pos = new int[eigenVal.length];
		for(int i = 0; i<eigenVal.length; i++){
			pos[i] = 0;
			for(int j = 0; j<eigenVal.length; j++){
				if(eigenVal[i]<eigenVal[j]){
					pos[i]++;
				}
			}
		}
		//sort eigenvalue array
		double[] tempEig = new double[eigenVal.length];
		for(int i = 0; i<tempEig.length;i++){
			tempEig[i]=eigenVal[pos[i]];
		}
		eigenVal = tempEig;

		//sort eigenVector array
		double[][] tempEigVec = new double[eigenVec.length][eigenVec[0].length];
		for(int i = 0; i<tempEigVec.length;i++){
			for(int j=0;j<tempEigVec[0].length;j++){
				tempEigVec[i][j]=eigenVec[i][pos[j]];
			}
		}
		eigenVec = tempEigVec;
		
		ResultsTable eigenTable = new ResultsTable();
		eigenTable.incrementCounter();
		double total = 0.0;
		for(int i = 0; i<eigenVal.length;i++){
			total += eigenVal[i];
			eigenTable.addValue("EigenVec"+(i+1),eigenVal[i]);
		}
		eigenTable.addLabel("Eigenvalue");
		for(int i = 0; i<eigenVec.length;i++){
			eigenTable.incrementCounter();
			for(int j = 0;j<eigenVec[0].length;j++){
				eigenTable.addValue("EigenVec"+(j+1),eigenVec[i][j]);
			}
		}
		//Calculate and add the cumulative variation accounted for by each
		eigenTable.incrementCounter();
		double cumulative = 0.0;
		for(int i = 0; i<eigenVal.length;i++){
			cumulative += eigenVal[i];
			eigenTable.addValue("EigenVec"+(i+1),(cumulative/total*100)+"%");
		}
		eigenTable.addLabel("Cum. Var. %");
		
		results.eigenVec = eigenVec;
		results.eigenVectorTable = eigenTable;
		results.weightingsTable = matrixLog;

		return results;
	}
	
	/**
	 * 
	 * 
	 * 
	 * Generic macro commands are split up by commas and are in the following format. 
	 * 
	 * 
	 * mode=K-Means / PCA
	 * seriesmode=Single / Series
	 * input=Positional / Thresholded
	 * pcamode=Correlation / Covariance
	 * mask=No Mask / Segregating / Annotative
	 * sigma=[sigma]
	 * filepath=(if not single positional) [path to directory or single file]
	 * dimensions=(if not positional) Manual / Table / From Images
	 * tablepath=(if using table) [table path]
	 * kgroups=[k groups]
	 * kiterations=[k iterations]
	 * 
	 */
	
	public void run(String arg){
		if(arg.equals("")){
			PDA_Pixelspace_Dialog dialog = new PDA_Pixelspace_Dialog();
			dialog.setVisible(true);
		} else {
			boolean kMeansMode=false;
			boolean singleMode = true;
			boolean seriesMode=false;
			String tablePath="", filePath="";	
			double sigma = 30;
			boolean splitMask = false;
			boolean useMask = true;
			boolean covariance = false;
			boolean positional = false;
			boolean manualInput = false;
			boolean tableInput = false;
			int kGroups = 1;
			int kIter = 100;
			
			
			//Split, sanitise  and interpret tokens
			String[] tokens = arg.split(",");
			for(int i = 0; i<tokens.length; i++){
				tokens[i] = tokens[i].trim();
				String key = tokens[i].split("=")[0].trim().toLowerCase();
				String var = tokens[i].split("=")[1].trim().toLowerCase();
				if(key.equals("mode")) {
					if(var.equals("k-means")) {
						kMeansMode= true;
					}
				} else if(key.equals("seriesmode")) {
					if(var.equals("series")) {
						seriesMode= false;
						singleMode = true;
					}
				} else if(key.equals("input")) {
					if(var.equals("positional")) {
						positional= true;
					}
				} else if(key.equals("pcamode")) {
					if(var.equals("covariance")) {
						covariance= true;
					}
				} else if(key.equals("mask")) {
					if(var.equals("no mask")) {
						useMask= false;
					} else if(var.equals("nomask")) {
						useMask= false;
					} else if (var.equals("segregating")) {
						splitMask=true;
					}
				} else if (key.equals("sigma")) {
					sigma = Double.parseDouble(var);
				} else if(key.equals("tablepath")) {
					tablePath = tokens[i].split("=")[1].trim();
				} else if(key.equals("filepath")) {
					filePath = tokens[i].split("=")[1].trim();
				} else if (key.equals("dimensions")) {
					if(var.equals("table")) {
						tableInput= false;
					} else if(var.equals("manual")) {
						manualInput= false;
					} 
				}else if (key.equals("kgroups")) {
					kGroups = Integer.parseInt(var);
				}else if (key.equals("kiterations")) {
					kIter = Integer.parseInt(var);
				}
			}
			ArrayList<PDA_Results> results = new ArrayList<PDA_Results>();
			
			
			

			
			if(singleMode){
				if(positional){
					String dir = new File(filePath).getParentFile().getName();
					results = PDA_Pixelspace.processPositional(dir, new String[]{filePath}, tablePath, manualInput, tableInput, true, useMask,  splitMask,  covariance,  sigma, kMeansMode, kGroups,kIter);
					
				} else {
					PDA_Pixelspace.processCurrentImage(useMask,splitMask,covariance,sigma, kMeansMode, kGroups,kIter);
				}
			} else {
				
					if(positional){
						results = PDA_Pixelspace.processPositionalDirectory(filePath,  tablePath, manualInput, tableInput, seriesMode,  useMask,  splitMask,  covariance,  sigma, kMeansMode, kGroups,kIter);
					} else {
						results = PDA_Pixelspace.processThresholdedDirectory( filePath, seriesMode,  useMask,  splitMask,  covariance,  sigma, kMeansMode, kGroups,kIter);
					}
			}
			
			if(results.size()>0){
				PDA_Pixelspace.saveResults(results, filePath, seriesMode);
			}
		}
		
	}

	
}
