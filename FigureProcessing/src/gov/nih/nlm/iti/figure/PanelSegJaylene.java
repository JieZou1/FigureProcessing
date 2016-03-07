package gov.nih.nlm.iti.figure;

import static org.bytedeco.javacpp.opencv_imgcodecs.CV_LOAD_IMAGE_COLOR;
import static org.bytedeco.javacpp.opencv_imgcodecs.imread;

import java.awt.Rectangle;
import java.util.ArrayList;

import org.bytedeco.javacpp.opencv_core.Mat;

import gov.nih.nlm.iti.panelSegmentation.regular.segmentation.PanelSplitter;

/**
 * A wrapper class on top of PanelSplitter class implemented by Jaylene in panelSegmentationModule.jar 
 * 
 * @author Jie Zou
 *
 */
public final class PanelSegJaylene extends PanelSeg
{
	/**
	 * The main entrance function to perform segmentation.
	 * Call getResult* functions to retrieve result in different format.
	 */
	public void segment(String image_file_path) 
	{
		Mat image = imread(image_file_path, CV_LOAD_IMAGE_COLOR);
		figure = new Figure(image);	//Construct a figure object for saving processing results
		figure.segmentationResult = new ArrayList<PanelSegResult>();
	
		PanelSplitter extractPanel = new PanelSplitter(image_file_path);	//Construct Jaylene's panel object for calling her segmentation method
		extractPanel.removeLabel();
		ArrayList<Rectangle> rects = extractPanel.extract();

		for (Rectangle rect : rects)
		{
			PanelSegResult panel = new PanelSegResult();
			panel.panelRect = rect;
			figure.segmentationResult.add(panel);
		}
	}

	/**
	 * The main entrance function to perform segmentation.
	 * Call getResult* functions to retrieve result in different format.
	 *  
	 * NOTICE: I have not found the best way to convert Mat to BufferedImage, such that we could construct a PanelSplitter from Mat.
	 * So, for now we have to rely on the PanelSplitter's ctor, which accept a file path.
	 */
	public void segment(Mat image) throws Exception
	{
		throw new Exception("Not implemented yet");
	}

}
