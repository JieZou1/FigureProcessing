package gov.nih.nlm.iti.figure;

import static org.bytedeco.javacpp.opencv_imgcodecs.CV_LOAD_IMAGE_COLOR;
import static org.bytedeco.javacpp.opencv_imgcodecs.imread;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.cvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.rectangle;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_core.Scalar;

enum Orientation { Horizontal, Vertical }

/**
 * The base class for all panel segmentation algorithms, which can apply to a Figure. <p>
 * 
 * This class is not intended to be instantiated, so we make it abstract. 
 * 
 * @author Jie Zou
 *
 */
public abstract class PanelSeg extends gov.nih.nlm.iti.figure.Algorithm 
{
	/**
	 * Some common initialization functions for all panel segmentation algorithms, including: <p>
	 * 1. Construct Figure object, figure <p>
	 * 2. Generate gray image, imageGray <p>
	 * 3. Construct segmentation result, segmentationResult.
	 * @param image
	 * @throws Exception
	 */
	public void segment(Mat image) 
	{
		figure = new Figure(image);	//Construct a figure object for saving processing results
		figure.imageGray = new Mat();		cvtColor(figure.image, figure.imageGray, CV_BGR2GRAY);
		figure.segmentationResult = new ArrayList<PanelSegResult>();		
	}
	/**
	 * The entrance function to perform panel segmentation. <p>
	 * It simply loads the image from the file, and then calls segment(Mat image) function.
	 * Call getSegmentationResult* functions to retrieve result in different format.
	 */
	public void segment(String image_file_path) 
	{
		Mat image = imread(image_file_path, CV_LOAD_IMAGE_COLOR);
		segment(image);		
	}
	
	/**
	 * The entrance function to perform segmentation.
	 * Call getSegmentationResult* functions to retrieve result in different format.
	 * It simply converts the buffered image to Mat, and then calls segment(Mat image) function.
	 *  
	 * NOTICE: I have not found the best way to convert BufferedImage to Mat (BGR) yet.
	 * So, this function is not ready yet.
	 */
	public void segment(BufferedImage buffered_image) throws Exception
	{
		throw new Exception("Not implemented yet");		
	}
	
	/**
	 * Get the panel segmentation result
	 * @return The detected panels
	 */
	public ArrayList<PanelSegResult> getSegmentationResult()	{	return figure.segmentationResult;	}

	/**
	 * Get the panel segmentation result by drawing the panel boundaries on the image
	 * @return the image with panel boundaries superimposed on it.
	 */
	public Mat getSegmentationResultInMat()
	{
		Mat img = figure.image.clone();
		for (PanelSegResult panel : figure.segmentationResult)
		{
			if (panel.panelRect != null )
			{
				Rectangle rect = panel.panelRect;
				rectangle(img, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), Scalar.RED, 3, 8, 0);
			}
			if (panel.labelRect != null)
			{
				Rectangle rect = panel.labelRect;
				rectangle(img, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), Scalar.BLUE, 1, 8, 0);
			}
		}

		return img;		
	}

}
