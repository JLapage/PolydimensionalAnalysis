package src.main.java.polydimensionalpixelspace;

import javax.swing.JTable;

import ij.IJ;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;


/**
 * Implements a simple table for the user to input image dimensions manually, when using positional files.
 * 
 * @author John MJ Lapage
 * @version 1.0
 *
 */
class Manual_Dimension_Table_Dialog extends JDialog implements ActionListener{

	private static final long serialVersionUID = 1L;
	private JTable table;
	private double[][] dimensions;
	private Object[][] data;
	private int rows;
	private String[] names;
	
	/**
	 * 
	 * Creates a small dialog that allows for manual image dimension data
	 * 
	 * @param fileNames
	 */
	public Manual_Dimension_Table_Dialog(String[] fileNames){
		this.setModal(true);
		names = fileNames;
		rows = fileNames.length;
		String[] columnNames = new  String[]{"File Name","XY microns per pixel","Z microns per pixel","Width (pixels)","Height (pixels)","Depth (Slices)","Duration (Frames)"};
		data = new Object[rows][7];
		for(int i = 0; i<rows; i++){
			data[i]= new Object[]{fileNames[i],"1","1","1","1","1","1"};
		}
		table = new JTable(data,columnNames);
		table.setShowGrid(true);
		
		JButton runButton = new JButton("Complete");
		runButton.addActionListener(this);
		
		Container container = this.getContentPane();
		container.setLayout(new BorderLayout());
		container.add(table.getTableHeader(), BorderLayout.PAGE_START);
		container.add(table, BorderLayout.CENTER);
		container.add(runButton,BorderLayout.PAGE_END);
		

		this.setTitle("Image Dimensions");
		this.pack();
		this.setVisible(true);
	}
	
	/**
	 * 
	 * Populates the arrays and closes the window.
	 * 
	 */
	public void actionPerformed(ActionEvent e){
		dimensions = new double[rows][5];
		for(int i = 0; i<rows; i++){
			String name = (String)data[i][0];
			if(!name.equals(names[i])){
				IJ.showMessage("File Name on row "+(i+1)+" changed. If this row no longer corresponds to the original file, this will crash.");
			}
			double xyCalib = Double.parseDouble((String)data[i][1]);
			double zCalib = Double.parseDouble((String)data[i][2]);
			int width = Integer.parseInt((String)data[i][3]);
			int height = Integer.parseInt((String)data[i][4]);
			int depth = Integer.parseInt((String)data[i][5]);
			int duration = Integer.parseInt((String)data[i][6]);
			dimensions[i] = new double[]{zCalib/xyCalib,width,height,depth,duration};
			if(dimensions[i][3] <= 0){
				dimensions[i][3] = 1;
			}
			if(dimensions[i][4] <= 0){
				dimensions[i][4] = 1;
			}
		}
		this.setVisible(false);
	}
	

	/**
	 * 
	 * Return the Dimensions array
	 * 
	 */
	public double[][] getDimensions(){
		return dimensions;
	}
	
}
