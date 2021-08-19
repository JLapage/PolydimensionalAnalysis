package src.main.java.polydimensionalpixelspace;


import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import ij.IJ;
import ij.io.DirectoryChooser;
import ij.io.OpenDialog;

public class SPDA_Pixelspace_Dialog extends JFrame implements ActionListener, ItemListener{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JRadioButton 	thresholdButton, positionalButton, 
							manualButton, fromImageButton, fromTableButton, 
							singleButton, globalButton, seriesButton, 
							noMaskButton, annotativeButton, segregatingButton, pcaButton, kMeansButton;
	private JPanel dataInputPanel, dimensionsPanel, operationsPanel, maskPanel, kMeansPanel, sigmaPanel, analysisModePanel;
	private ButtonGroup inputGroup, dimensionsGroup, operationGroup, maskGroup, analysisModeGroup;
	private JButton runButton;
	private JTextField sigmaField, kField, iterField;
	private JLabel kLabel, iterLabel;
	
	public SPDA_Pixelspace_Dialog(){
		
		//Mode Panel: PCA or K-Means (Hey! room to expand!!)
		analysisModePanel = new JPanel();
		analysisModePanel.setLayout(new BoxLayout(analysisModePanel, BoxLayout.PAGE_AXIS));
		analysisModeGroup = new ButtonGroup();
		
		pcaButton = new JRadioButton("PCA",true);
		kMeansButton = new JRadioButton("K-Means");
		
		pcaButton.addItemListener(this);
		
		analysisModeGroup.add(pcaButton);
		analysisModeGroup.add(kMeansButton);
		
		analysisModePanel.add(pcaButton);
		analysisModePanel.add(kMeansButton);
		
		analysisModePanel.setBorder(BorderFactory.createTitledBorder("Analysis Mode"));
		
		//Data Panel: Data type. Images or text files.
		dataInputPanel = new JPanel();
		dataInputPanel.setLayout(new BoxLayout(dataInputPanel, BoxLayout.PAGE_AXIS));
		inputGroup = new ButtonGroup();
		
		thresholdButton = new JRadioButton("Thresholded Image",true);
		positionalButton = new JRadioButton("Positional File");
		
		thresholdButton.addItemListener(this);
		
		inputGroup.add(thresholdButton);
		inputGroup.add(positionalButton);
		
		
		dataInputPanel.add(thresholdButton);
		dataInputPanel.add(positionalButton);
		
		dataInputPanel.setBorder(BorderFactory.createTitledBorder("Data Input Type"));
		
		//Dimensions. If using positional files, where are the dimensions of the images? Disabled if thresholded images used.
		dimensionsPanel = new JPanel();
		dimensionsPanel.setLayout(new BoxLayout(dimensionsPanel, BoxLayout.PAGE_AXIS));
		dimensionsGroup = new ButtonGroup();
		
		manualButton = new JRadioButton("Manual Input",true);
		fromImageButton = new JRadioButton("From Image");
		fromTableButton = new JRadioButton("From Table");
		
		dimensionsGroup.add(manualButton);
		dimensionsGroup.add(fromImageButton);
		dimensionsGroup.add(fromTableButton);

		dimensionsPanel.add(manualButton);
		dimensionsPanel.add(fromImageButton);
		dimensionsPanel.add(fromTableButton);
		
		dimensionsPanel.setBorder(BorderFactory.createTitledBorder("Image Dimensions"));
		
		//Disabled at start
		dimensionsPanel.setEnabled(false);
		manualButton.setEnabled(false);
		fromImageButton.setEnabled(false);
		fromTableButton.setEnabled(false);
		
		//Operations Mode. Single or multiple analyses
		operationsPanel = new JPanel();
		operationsPanel.setLayout(new BoxLayout(operationsPanel, BoxLayout.PAGE_AXIS));
		operationGroup = new ButtonGroup();
		
		singleButton = new JRadioButton("Single",true);
		globalButton = new JRadioButton("Global Batch");
		seriesButton = new JRadioButton("Series Batch");
		
		operationGroup.add(singleButton);
		operationGroup.add(globalButton);
		operationGroup.add(seriesButton);

		operationsPanel.add(singleButton);
		operationsPanel.add(globalButton);
		operationsPanel.add(seriesButton);
		
		operationsPanel.setBorder(BorderFactory.createTitledBorder("Batch Mode"));
		
		// Mask Mode
		maskPanel = new JPanel();
		maskPanel.setLayout(new BoxLayout(maskPanel, BoxLayout.PAGE_AXIS));
		maskGroup = new ButtonGroup();

		annotativeButton = new JRadioButton("Annotative Mask",true);
		segregatingButton = new JRadioButton("Segregating Mask");
		noMaskButton = new JRadioButton("No Mask");

		maskGroup.add(noMaskButton);
		maskGroup.add(annotativeButton);
		maskGroup.add(segregatingButton);

		maskPanel.add(noMaskButton);
		maskPanel.add(annotativeButton);
		maskPanel.add(segregatingButton);

		maskPanel.setBorder(BorderFactory.createTitledBorder("Mask Mode"));
		
		//K-Means specific settings
		kMeansPanel = new JPanel();
		kMeansPanel.setLayout(new BoxLayout(kMeansPanel, BoxLayout.PAGE_AXIS));
		kMeansPanel.setBorder(BorderFactory.createTitledBorder("K-Means Settings"));
		kField = new JTextField("",5);
		iterField = new JTextField("100",5);
		
		

		kLabel = new JLabel("k:");
		iterLabel  = new JLabel("'Max. Iterations:");
		
		kMeansPanel.add(kLabel);
		kMeansPanel.add(kField);
		kMeansPanel.add(iterLabel);
		kMeansPanel.add(iterField);
		
		//Starts Disabled
		kMeansPanel.setEnabled(false);
		kLabel.setEnabled(false);
		iterLabel.setEnabled(false);
		kField.setEnabled(false);
		iterField.setEnabled(false);
		
		

		//Sixth Panel (No Border) - Buttons. Just a run button, but would be good to add a help button if there was a webpage of instructions.
		sigmaPanel = new JPanel();
		
		sigmaField = new JTextField("",5);
		sigmaPanel.add(new JLabel("Sampling Sigma (px):"));
		sigmaPanel.add(sigmaField);
		
		
		
		JPanel topPanel = (JPanel) this.getContentPane();
		topPanel.setLayout(new GridLayout(4,2));
		topPanel.add(analysisModePanel);
		topPanel.add(dataInputPanel);
		topPanel.add(dimensionsPanel);
		topPanel.add(operationsPanel);
		topPanel.add(maskPanel);
		topPanel.add(kMeansPanel);
		topPanel.add(sigmaPanel);
		
		runButton = new JButton("Run");
		runButton.addActionListener(this);
		
		topPanel.add(runButton);
		
		this.pack();
		this.setResizable(false);
		
	}
	
	
	/**
	 * Implements GUI functionality
	 */
	public void actionPerformed(ActionEvent e){
		try{
			ArrayList<SPDA_Results> results = new ArrayList<SPDA_Results>();
			String tablePath="", filePath="";
			int kGroups = 0;
			int kIter = 100;
			boolean kmeansMode = kMeansButton.isSelected();
			if(kmeansMode) {
				kGroups =Integer.parseInt(kField.getText());
				kIter =Integer.parseInt(iterField.getText());
			}
			double sigma = Double.parseDouble(sigmaField.getText());
			boolean useMask = false, splitMask = false;
			boolean covariance = false;
			
			boolean singleMode = singleButton.isSelected();
			boolean seriesMode = seriesButton.isSelected();
			
			boolean manualInput = manualButton.isSelected();
			boolean tableInput = fromTableButton.isSelected();
			boolean fromImage = fromImageButton.isSelected();
			
			boolean positional = positionalButton.isSelected();
			
			if(positional){
				if(useMask && !fromImage){
					IJ.showMessage("Overriding User Selection: if working with positional files and mask images, dimensions come from masks");
					manualInput = false;
					fromImage = true;
					tableInput = false;
				}
				if(tableInput){
					IJ.showMessage("Please Select tab separated table file with column headings of:\nFilename, XY Calibration, Z Calibration, Width, Height, Slices, Frames");
					OpenDialog od = new OpenDialog("Please Select Table File");
					if(od.getPath()!=null){
						tablePath = od.getPath();
					}
				}
			}

			
			if(segregatingButton.isSelected()){
				splitMask = true;
				useMask = true;
			} else if (annotativeButton.isSelected()){
				useMask = true;
			}
			

			
			if(singleMode){
				if(positional){
					IJ.showMessage("Select Positional File");
					OpenDialog od = new OpenDialog("Select Positional File");
					if(od.getPath()!=null){
						filePath = od.getDirectory();
						results = SPDA_Pixelspace.processPositional(filePath, new String[]{od.getFileName()}, tablePath, manualInput, tableInput, true, useMask,  splitMask,  covariance,  sigma, kmeansMode, kGroups, kIter);
					}
				} else {
					SPDA_Pixelspace.processCurrentImage(useMask,splitMask,covariance,sigma, kmeansMode, kGroups, kIter);
				}
			} else {
				if(positional){
					IJ.showMessage("Please select working directory, containing positional files and corresponding masks that have the same names (excluding suffixes)");
				} else {
					IJ.showMessage("Select Working Directory");
				}
				DirectoryChooser dc = new DirectoryChooser("Select Working Directory");
				if(dc.getDirectory()!=null){
					filePath = dc.getDirectory();
					if(positional){
						results = SPDA_Pixelspace.processPositionalDirectory(filePath,  tablePath,manualInput, tableInput, seriesMode,  useMask,  splitMask,  covariance,  sigma, kmeansMode, kGroups, kIter);
					} else {
						results = SPDA_Pixelspace.processThresholdedDirectory(filePath, seriesMode,  useMask,  splitMask,  covariance,  sigma, kmeansMode, kGroups, kIter);
					}
				}
				
			}
			
			if(results.size()>0){
				SPDA_Pixelspace.saveResults(results, filePath, seriesMode);
			}
		} catch (NumberFormatException a){
			IJ.showMessage("Invalid Input");
		}
	}
	
	
	/**
	 * 
	 * Implements the dimensions panel phasing in and out based on selection of thresholded mode.
	 * 
	 */
	public void itemStateChanged(ItemEvent e){
		if(thresholdButton.isSelected() == true){
			dimensionsPanel.setEnabled(false);
			manualButton.setEnabled(false);
			fromImageButton.setEnabled(false);
			fromTableButton.setEnabled(false);
		} else {
			dimensionsPanel.setEnabled(true);
			manualButton.setEnabled(true);
			fromImageButton.setEnabled(true);
			fromTableButton.setEnabled(true);
		}
		if(pcaButton.isSelected() == true){
			kMeansPanel.setEnabled(false);
			kLabel.setEnabled(false);
			iterLabel.setEnabled(false);
			kField.setEnabled(false);
			iterField.setEnabled(false);
		} else {
			kMeansPanel.setEnabled(true);
			kLabel.setEnabled(true);
			iterLabel.setEnabled(true);
			kField.setEnabled(true);
			iterField.setEnabled(true);
		}
	}
}
