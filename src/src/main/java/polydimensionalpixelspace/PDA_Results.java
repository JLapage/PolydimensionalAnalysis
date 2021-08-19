package src.main.java.polydimensionalpixelspace;

import ij.CompositeImage;
import ij.ImagePlus;
import ij.measure.ResultsTable;

/**
 * 
 * PDA_Results is the generic data container for Polydimensional Results - it contains the reusult image, and for PCA, the various tables.
 *
 * @author John MJ Lapage
 * @version 1.0
 */
public class PDA_Results {
	public CompositeImage pdaImp;
	public double[][] eigenVec;
	public ResultsTable results,eigenVectorTable, weightingsTable;
	
	public PDA_Results(){
		
	}
	
	public PDA_Results(ImagePlus imp) {
		pdaImp = new CompositeImage(imp);
	}
	
	/**
	 * Creates a deep copy of these results
	 * 
	 * @return a duplicate PCA_Results
	 */
	public PDA_Results duplicate(){
		PDA_Results dup = new PDA_Results();
		if(this.pdaImp!=null){
			dup.pdaImp = this.pdaImp;
		}
	
		if(this.results!=null){
			dup.results = this.results;
		}
		if(this.eigenVectorTable!=null){
			dup.eigenVectorTable = this.eigenVectorTable;
		}
		if(this.weightingsTable!=null){
			dup.weightingsTable = this.weightingsTable;
		}
		if(this.eigenVec!=null){
			dup.eigenVec = this.eigenVec;
		}

		return dup;
	}
}
