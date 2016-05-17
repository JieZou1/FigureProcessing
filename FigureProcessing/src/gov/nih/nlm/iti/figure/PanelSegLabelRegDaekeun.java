package gov.nih.nlm.iti.figure;

import static org.bytedeco.javacpp.opencv_imgcodecs.CV_LOAD_IMAGE_COLOR;
import static org.bytedeco.javacpp.opencv_imgcodecs.imread;

import java.awt.Rectangle;
import java.util.ArrayList;

import org.bytedeco.javacpp.opencv_core.Mat;
import gov.nih.nlm.iti.figure.PanelSplitter.Final_Panel;
import weka.classifiers.Classifier;

public class PanelSegLabelRegDaekeun extends PanelSegLabelReg 
{
	static Classifier OCR_model = null;
	public PanelSegLabelRegDaekeun()
	{
		if (OCR_model == null)
		{
			try {
				OCR_model = (Classifier) weka.core.SerializationHelper.read("NN_OCR.model");
			} catch (Exception e) {
				System.out.println("Unable to read the OCR model file");
				e.printStackTrace();
			}
		}
	}

	@Override
	public void segment(Mat image) 
	{
	}

	/**
	 * The main entrance function to perform segmentation.
	 * Call getResult* functions to retrieve result in different format.
	 */
	public void segment(String image_file_path) 
	{
		Mat image = imread(image_file_path, CV_LOAD_IMAGE_COLOR);
		
		preSegment(image);
		
		PanelSplitter panelSplitter = new PanelSplitter();
		ArrayList<Final_Panel> panels = panelSplitter.panelSplitter(image_file_path, OCR_model);		
	
		for (Final_Panel p : panels)
		{
			PanelSegInfo panel = new PanelSegInfo();
			if (Character.isLetter(p.label))
			{
				panel.labelRect = new Rectangle(p.left, p.top, p.right-p.left, p.bottom-p.top);
				panel.panelLabel = "" + p.label;
			}
			figure.panelSegResult.add(panel);
		}
	}
	
}
