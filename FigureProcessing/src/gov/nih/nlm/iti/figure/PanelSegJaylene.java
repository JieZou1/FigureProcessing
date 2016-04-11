package gov.nih.nlm.iti.figure;

import static org.bytedeco.javacpp.opencv_imgcodecs.CV_LOAD_IMAGE_COLOR;
import static org.bytedeco.javacpp.opencv_imgcodecs.imread;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
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
	//private String imageFilePath;
	private Mat matImage;
	private BufferedImage bufferedImage;
	
	private void segment()
	{
		figure = new Figure(matImage);	//Construct a figure object for saving processing results
		figure.segmentationResult = new ArrayList<PanelSegInfo>();

		PanelSplitter extractPanel = new PanelSplitter(bufferedImage);	//Construct Jaylene's panel object for calling her segmentation method
		extractPanel.removeLabel();
		ArrayList<Rectangle> rects = extractPanel.extract();

		for (Rectangle rect : rects)
		{
			PanelSegInfo panel = new PanelSegInfo();
			panel.panelRect = rect;
			figure.segmentationResult.add(panel);
		}
	}
	
	/**
	 * The main entrance function to perform segmentation.
	 * Call getResult* functions to retrieve result in different format.
	 */
	public void segment(String image_file_path) 
	{
		matImage = imread(image_file_path, CV_LOAD_IMAGE_COLOR);
		bufferedImage = AlgorithmEx.mat2BufferdImg(matImage);

		segment();
	}

	/**
	 * The main entrance function to perform segmentation.
	 * Call getResult* functions to retrieve result in different format.
	 *  
	 */
	public void segment(BufferedImage image)
	{
		matImage = AlgorithmEx.bufferdImg2Mat(image);
		bufferedImage = AlgorithmEx.mat2BufferdImg(matImage);
		segment();
	}
	
	/**
	 * The main entrance function to perform segmentation.
	 * Call getResult* functions to retrieve result in different format.
	 *  
	 */
	public void segment(Mat image)
	{
		matImage = image;
		bufferedImage = AlgorithmEx.mat2BufferdImg(image);
		segment();
	}
}
