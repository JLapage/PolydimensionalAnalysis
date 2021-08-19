package src.main.java.polydimensionalpixelspace;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;

/**
 * This class of Pixelspace image is for data where the blurring operation has already been performed - 
 * this is used for integration with other plugins and is not natively accessible from the GUI.
 * 
 * @author John MJ Lapage
 * @version 1.0
 *
 */
public class SPDA_PS_Mapped extends SPDA_PS_Image {
	public SPDA_PS_Mapped(ImagePlus imp, boolean masked) {
		this.imp = imp;
		width = imp.getWidth();
		height = imp.getHeight();
		channels = imp.getNChannels();
		slices = 1;
		frames = 1;
		if(masked) {
			mask = getMask(imp);
			channels--;
		} else {
			mask = blankMask();
		}
	}
	
	/**
	 * 
	 * @param kernel
	 * @return
	 */
	@Override
	public double[][] analyse(float[] kernel){ 
		FloatProcessor[][][] floats = new FloatProcessor[1][1][channels];
		ImageStack stack = imp.getStack();
		for(int i = 0; i<channels; i++) {
			floats[0][0][i] = (FloatProcessor) stack.getProcessor(imp.getStackIndex(i+1, 1, 1));
		}
		proportions = sample(floats);
		return proportions;
	}
	
}
