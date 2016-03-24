package gov.nih.nlm.iti.figure;

import java.util.ArrayList;
import org.bytedeco.javacpp.opencv_core.*;

/**
 * The core class for holding all information about this Figure. 
 * The design is: different algorithms must have a Figure field, which is constructed during the Algorithm instance construction; 
 * the algorithm takes some fields in Figure object as inputs and then save the results to some other fields of the Figure object.  
 * 
 * @author Jie Zou
 *
 */
class Figure 
{
	Mat image;		//The original figure image, has to be BGR image
	Mat imageGray;	//The gray image converted from original BGR image
	Mat imageGrayInverted;	//The inverted gray image
	int imageWidth, imageHeight;
	
	ArrayList<ArrayList<PanelSegInfo>> segmentationResultIndividualLabel; //The segmentation results for each individual labels
	
	ArrayList<PanelSegInfo> segmentationResult;	//The finally segmentation results
	
	//Below info is collected from LabelStatistics.txt
	static final int label_box_max_width = 70;
	//static int label_box_min_width = 5;
	static final int label_box_max_height = 70;
	//static int label_box_min_height = 5;
	
	/**
	 * ctor, from a BGR image
	 * @param img
	 */
	Figure(Mat img)
	{
		image = new Mat(img);
		imageWidth = image.cols(); imageHeight = image.rows();
	}
	
}
