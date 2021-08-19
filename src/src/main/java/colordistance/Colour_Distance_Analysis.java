package src.main.java.colordistance;

import java.awt.Rectangle;

import javax.swing.JTabbedPane;

import ij.ImageStack;
import ij.WindowManager;
import ij.gui.Roi;

public class Colour_Distance_Analysis extends JTabbedPane {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 * Constructs a window containing tabbed panes of the rest of the utilities
	 * 
	 */
	public Colour_Distance_Analysis(){
		/*LogStream.redirectSystem();
		JFrame frame = new JFrame("Delaminator Analysis");
		Colour_Blur blur = new Colour_Blur();
		this.addTab("Blurring",blur);

		Colour_Distance_Analysis vert = new Colour_Distance_Analysis();
		JPanel vertWrapper = new JPanel();
		vertWrapper.add(vert);
		this.addTab("Vertical Comparisons",vertWrapper);

		/*LateralSpreadAnalysis lateral = new LateralSpreadAnalysis();
		JPanel latWrapper = new JPanel();
		latWrapper.add(lateral);
		this.addTab("Lateral Spread",latWrapper);

		LinkColourBasins link = new LinkColourBasins();
		this.addTab("Link Basins",link);

		CharacterisePatches charPatch = new CharacterisePatches();
		JPanel charWrapper = new JPanel();
		charWrapper.add(charPatch);
		this.addTab("Characterise Patches",charWrapper);

		ExploreSinglePatch explorePatch = new ExploreSinglePatch();
		JPanel explWrapper = new JPanel();
		explWrapper.add(explorePatch);
		this.addTab("Explore Single Patch",explorePatch);

		AnalyseBasins countCells = new AnalyseBasins();
		this.addTab("Quantify Confetti Cells",countCells);
		
		frame.add(this);
		frame.pack();
		frame.setResizable(false);
		frame.setVisible(true);*/

	}

	/**
	 * 
	 * Returns the euclidian distance between two points through Pythagoras' theorem
	 * 
	 * @param x1,y1		xy coordinates of Point 1
	 * @param x2,y2		xy coordinates of Point 2
	 * @return		distance between points 1 and 2
	 * 
	 */
	public static float getDistance(float x1, float y1, float x2, float y2){
		return (float) Math.sqrt(Math.pow(Math.abs(x1-x2),2) + Math.pow(Math.abs(y1-y2),2));
	}
	
	
	/**
	 * 
	 * Convenience method masking for double entry. Only uses float precision.
	 * @param voxel1
	 * @param voxel2
	 * @param ratioMode
	 * @param inverted
	 * @param density
	 * @return
	 */
	public static float getColourDistance(double[] voxel1, double[] voxel2, boolean ratioMode, boolean inverted, boolean density){
		int voxLength = voxel1.length;
		float[] vox1fl = new float[voxLength];
		float[] vox2fl = new float[voxLength];
		for(int i = 0; i<voxLength; i++) {
			vox1fl[i]=(float)voxel1[i];
			vox2fl[i]=(float)voxel2[i];

		}
		return getColourDistance(vox1fl,vox2fl,ratioMode,inverted,density);
	}


	/**
	 * 
	 * Calculates the euclidian distance between two points in n-dimensional space. This is used to explore 'colour distance', i.e. the difference
	 * between two 4-channel colours.
	 * 
	 * @param voxel1	The float colour values of point 1
	 * @param voxel2	The float colour values of point 2
	 * @param ratioMode	If true, the relative values of the colour intensities will be used. If not, the absolute values will be used.
	 * @param inverted	If true, the return value will represent similarity. If false, the return value will represent difference.
	 * @return		The euclidian distance between voxel1 and voxel2 in n-dimensional space where n is the length of the two arrays. 
	 */
	//Colour distance between two points
	public static float getColourDistance(float[] voxel1, float[] voxel2, boolean ratioMode, boolean inverted, boolean density){
		float sumSquares = 0.0f;
		float sum = 0.0f;
		int channels = voxel1.length;
		float[] val1, val2;
		for(int i =0; i<channels; i++){
			sum+=voxel1[i]+voxel2[i];
		}
		if(ratioMode){
			val1 = new float[channels];
			val2 = new float[channels];
			float max1 = 0.0f;
			float max2 = 0.0f;
			
			for(int i = 0; i<channels; i++){
				if(voxel1[i]>=	max1){
					max1 = voxel1[i];
					
				}
				if(voxel2[i]>=	max2){
					max2 = voxel2[i];
					
				}
			}
			for(int i = 0; i<channels; i++){
				val1[i] = voxel1[i]/max1;
				val2[i] = voxel2[i]/max2;
			}
		} else {
			val1 = voxel1;
			val2 = voxel2;
			
		}
		for(int i =0; i<channels; i++){
			sumSquares += Math.pow(val1[i]-val2[i],2);
		}
		
		float result = (float)Math.sqrt(sumSquares);

		
		if(Float.isNaN(result)){result=0.0f;}
		else if(inverted){
			result = (float) Math.sqrt(channels) - result;
		}
		//Encode density: divide the total cell density (maximum: number of channels) by the number of channels
		if(density){
			result *= sum/(channels*Math.sqrt(channels)*2);
		}
		return result;
	}

	/**
	 * 
	 * Gathers the colour information for a single voxel.
	 * 
	 * @param stack	The image from which the pixel is to be drawn
	 * @param x,y	The pixel to be measured
	 * @return 	array of ints representing the colour at this pixel
	 * 
	 */
	//turn voxel into int[] of colours ignoring mask
	public static int[] voxToIntArray(ImageStack stack, int x, int y){
		int[] voxel = new int[stack.getSize()-1];
		for(int i = 0; i<voxel.length;i++){
			voxel[i] = (int)stack.getVoxel(x,y,i);
		}
		return voxel;
	}

	/**
	 * 
	 * Gathers the colour information for a single voxel.
	 * 
	 * @param stack	The image from which the pixel is to be drawn
	 * @param x,y	The pixel to be measured
	 * 
	 */
	//turn voxel into int[] of colours ignoring mask
	public static float[] voxToFloatArray(ImageStack stack, int x, int y){
		float[] voxel = new float[stack.getSize()-1];
		for(int i = 0; i<voxel.length;i++){
			voxel[i] = (float)stack.getVoxel(x,y,i);
		}
		return voxel;
	}

	/**
	 * 
	 * Static method to get String array of all open windows. Used to populate comboboxes throughout program
	 * 
	 * @return 	Array of the titles of all open image windows.
	 * 
	 */
	public static String[] getWindowList(){
		int[] ids = WindowManager.getIDList();
		if( ids == null ){
			return new String[]{"No Images Open"};
		} else {
			String[] names = new String[ids.length];
			for(int i = 0; i<ids.length; i++){
				names[i] = WindowManager.getImage(ids[i]).getTitle();
			}
			return names;
		}
	}

	/**
	 * 
	 * Tests for any overlap of two confetti patches
	 * 
	 * @param polyA		ConfettiPatch to be evaluated
	 * @param polyB		Other ConfettiPatch to be evaluated
	 * 
	 * @return		true if overlap found, false if none found
	 * 
	 */
	public static boolean someOverlap(Roi polyA, Roi polyB){
		Rectangle boundsA = polyA.getBounds();
		Rectangle boundsB = polyB.getBounds();
		int startX = Math.max(boundsA.x,boundsB.x);
		int endX = Math.min(boundsA.x+boundsA.width,boundsB.x+boundsB.width);
		int startY = Math.max(boundsA.y,boundsB.y);
		int endY = Math.min(boundsA.y+boundsA.height,boundsB.y+boundsB.height);
		for(int x = startX; x<endX; x++){
			for(int y = startY; y<endY; y++){
				if(polyA.contains(x,y) && polyB.contains(x,y)){
					return true;
				}
			}
		}
		return false;
	}

}