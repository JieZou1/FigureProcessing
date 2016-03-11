package gov.nih.nlm.iti.figure;

import static org.bytedeco.javacpp.opencv_imgcodecs.CV_LOAD_IMAGE_COLOR;
import static org.bytedeco.javacpp.opencv_imgcodecs.imread;
import static org.bytedeco.javacpp.opencv_imgproc.*;

import java.awt.Rectangle;
import java.util.ArrayList;

import org.apache.commons.math3.stat.StatUtils;
import org.bytedeco.javacpp.indexer.UByteBufferIndexer;
import org.bytedeco.javacpp.opencv_core.*;

import weka.classifiers.Classifier;
import weka.core.SerializationHelper;

/**
 * A segmetation method try combining Daekeun, Jaylene and Santosh methods 
 * 
 * @author Jie Zou
 *
 */

public class PanelSeg1 extends PanelSeg
{
	private static Classifier ocrNN = null;
	
	ArrayList<UniformBand> uniformBands;
	
	public static void Initialize() 
	{
		if (ocrNN == null)
		{
			try 
			{
				ocrNN = (Classifier)SerializationHelper.read("\\Users\\jie\\Openi\\Panel\\programs\\PanelSegmentation\\NN_OCR.model");
			} 
			catch (Exception e) 
			{
				System.out.println("Unable to read the OCR model file");
				e.printStackTrace();
			}
			System.out.println("OCR model Loaded");
		}
	}
	
	public void segment(String image_file_path) 
	{
		Mat image = imread(image_file_path, CV_LOAD_IMAGE_COLOR);
		segment(image);
	}
	
	public void segment(Mat image)
	{
		figure = new Figure(image);	//Construct a figure object for saving processing results
		figure.segmentationResult = new ArrayList<PanelSegResult>();

		
		//panel = new Figure(image, new Rect(0, 0, image.cols(), image.rows()));
		//panel.DetectUniformBand(5, 15.0, 15.0, 200, 200, true);
		DetectLabelsDaekeun();
		//panel.DetectLabelCandidates();
	}

	void DetectLabelsDaekeun()
	{
		PanelLabelDetection panelLabelDetectionRef = new PanelLabelDetection();
		ArrayList<PanelLabelDetection.PanelLabelInfo_Final> candLabelSet = new ArrayList<PanelLabelDetection.PanelLabelInfo_Final>();
		
		// panel label detection. 
		// candLabelSet has all candidate label sets and the setNo denotes the total cand numbers.		
		if (figure.imageWidth < 1000 && figure.imageHeight < 1000) 
		{
			Mat resized_img = new Mat();
			resize(figure.imageGray, resized_img, new Size(figure.imageWidth* 2, figure.imageHeight * 2));
			IplImage ipl_img = new IplImage(resized_img);
			panelLabelDetectionRef.panelLabelDetection(ipl_img, true, candLabelSet, ocrNN);			
		} 
		else
		{
			IplImage ipl_img = new IplImage(figure.imageGray);
			panelLabelDetectionRef.panelLabelDetection(ipl_img, false, candLabelSet, ocrNN);
		}
		
		//int setNo = panelLabelDetectionRef.setNo;	
		
		// remove duplicate rectangles across the candidate label sets
		figure.segmentationResult = new ArrayList<PanelSegResult>();
		for (int i = 0; i < candLabelSet.size(); i++)
		{
			System.out.println("candidate label set: " + i);
			PanelLabelDetection.PanelLabelInfo_Final panelLabelInfo_Final = candLabelSet.get(i);
			ArrayList<PanelLabelDetection.PanelLabelInfo> labels = panelLabelInfo_Final.labels;
			
			for (int j = 0; j < labels.size(); j++ )
			{
				PanelLabelDetection.PanelLabelInfo label = labels.get(j);
				if (!Character.isAlphabetic(label.label) && !Character.isDigit(label.label)) continue;
				
				System.out.println("label: " + label.label + " left: "+ Integer.toString(label.left) + " right: "+ Integer.toString(label.right)
						+ " top: "+ Integer.toString(label.top) + " bottom: "+ Integer.toString(label.bottom) + " score: " + Double.toString(label.score));
				boolean isDuplicate = false;
				for (int k = 0; k < figure.segmentationResult.size(); k++)
				{
					Rectangle rect = figure.segmentationResult.get(k).labelRect;
					
					if (rect.x == label.left && rect.y == label.top && rect.width == label.right - label.left && rect.height == label.bottom - label.top)
					{
						isDuplicate = true; 
						break;
					}
				}
				if (!isDuplicate)
				{
					PanelSegResult panel = new PanelSegResult();
					panel.labelRect = new Rectangle(label.left, label.top, label.right-label.left, label.bottom-label.top);
					panel.panelLabel = Character.toString(label.label);
					panel.labelScore = label.score;
					figure.segmentationResult.add(panel);
				}				
			}
		}
	}
	
	public Mat getUniformBandInMat() 
	{
		Mat img = figure.image.clone();
		for (UniformBand band : uniformBands)
		{
			Rectangle rect = band.rectangle;
			rectangle(img, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), Scalar.RED, CV_FILLED, 8, 0);
		}
		return img;
	}
	
	public Mat getLabelCandidatesInMat() 
	{
		Mat img = figure.image.clone();
		
		if (figure.segmentationResult == null || figure.segmentationResult.size() == 0) return img;
		
		for (PanelSegResult panel : figure.segmentationResult)
		{
			Rect rect = new Rect(panel.labelRect.x, panel.labelRect.y, panel.labelRect.width, panel.labelRect.height);
			rectangle(img, rect, Scalar.RED, 1, 8, 0);
		}
		return img;
	}

	/**
	 * Find Uniform Crossing Band candidates. 
	 * They have to be larger than certain width; lower than certain intensity variance
	 * @param pad	The width of border pixels that we don't consider when calculating intensity variances. 
	 * @param thres_ver
	 * @param thres_hor
	 * @param thres_mean_hor
	 * @param thres_mean_ver
	 * @param flag_mean
	 */
	void DetectUniformBand(int pad, double thres_ver, double thres_hor, double thres_mean_hor, double thres_mean_ver, boolean flag_mean) 
	{
		uniformBands = new ArrayList<UniformBand>();
		
		//We work with grayscale image only for detecting uniform crossing bands 
		UByteBufferIndexer grayIndexer = figure.imageGray.createIndexer();
		
        // calculate the variance of each horizontal line
        // and find the low variance horizontal lines
        ArrayList<Integer> ind_hor = new ArrayList<Integer>();
		for (int y = 0; y < figure.imageHeight; y++)
		{
	        double[] line = new double[figure.imageWidth];
			for (int x = 0; x < figure.imageWidth; x++)	line[x] = grayIndexer.getDouble(y, x);
	        double mean_hor = StatUtils.mean(line, pad, figure.imageWidth - 2 * pad);
	        double var_hor = StatUtils.variance(line, mean_hor, pad, figure.imageWidth - 2 * pad);
		
	        if (flag_mean)
	        {
	        	if (var_hor <= thres_hor && mean_hor >= thres_mean_hor)	ind_hor.add(y);
	        	//System.out.println("Horizontal low variance lines:" +  Integer.toString(i));
	        }
	        else
	        {
	        	if (var_hor <= thres_hor) ind_hor.add(y);
	        }
		}		
        //--------------------------------------------------------------
        // extract the low variance horizontal band
        ArrayList<Rectangle> rect_hor = new ArrayList<Rectangle>();
        // the first low variance horizontal line
        if (ind_hor.size() > 0) 
        {
            // left upper point
            int xmin = 0, ymin = ind_hor.get(0);
            // extend to the border if it is close to the border
            if (ymin < 0.05 * (double) figure.imageHeight)  ymin = 0;
            // right lower point
            int xmax = xmin, ymax = ymin; // temp value, will change in the following loop
            Rectangle rt = new Rectangle(xmin,ymin, xmax-xmin+1, ymax-ymin+1);
            rect_hor.add(rt);
        }
        for (int i = 0; i < ind_hor.size() - 1; i++) 
        {
            int diff = ind_hor.get(i + 1) - ind_hor.get(i);
            //System.out.println("difference : " + Integer.toString(diff));

            if ((double) diff >= 0.05 * (double) figure.imageWidth) 
            {
                //Set the right lower point of the previous band
                Rectangle rt = rect_hor.get(rect_hor.size() - 1);
                int xmin = rt.x, ymin = rt.y, xmax = figure.imageWidth - 1, ymax = ind_hor.get(i);
                Rectangle rt1 = new Rectangle(xmin,ymin, xmax-xmin+1, ymax-ymin+1);
                rect_hor.set(rect_hor.size() - 1, rt1);

                // set the location of the current band
                xmin = 0; ymin = ind_hor.get(i + 1);// left upper point
                xmax = xmin; ymax = ymin; // right lower point; temp value, will change in the next loop
                Rectangle rt2 = new Rectangle(xmin,ymin, xmax-xmin+1, ymax-ymin+1);
                rect_hor.add(rt2);
            }
        }
        // the last low variance horizontal line
        if (ind_hor.size() > 0) 
        {
            // set the right lower point of the previous band
            Rectangle rt = rect_hor.get(rect_hor.size() - 1);
            int xmin = rt.x, ymin = rt.y, xmax = figure.imageWidth - 1, ymax = ind_hor.get(ind_hor.size() - 1);
            // extend to the border if it is close to the border
            if (ymax > 0.95 * (double) figure.imageHeight)	ymax = figure.imageHeight - 1;
            Rectangle rt1 = new Rectangle(xmin,ymin,xmax-xmin+1, ymax-ymin+1);
            rect_hor.set(rect_hor.size() - 1, rt1);
        }
        for (int i = 0; i < rect_hor.size(); i++)
        {
        	Rectangle rect = rect_hor.get(i);
        	if (rect.y <= 0 || rect.y + rect.height >= figure.imageHeight ) continue; //We ignore the board bands
            UniformBand band = new UniformBand(Orientation.Horizontal, rect);
            uniformBands.add(band);
        }
        
        // calculate the variance of each vertical line
        // and find the low variance vertical lines
        ArrayList<Integer> ind_ver = new ArrayList<Integer>();
		for (int x = 0; x < figure.imageWidth; x++)
		{
	        double[] line = new double[figure.imageHeight];
			for (int y = 0; y < figure.imageHeight; y++)	line[y] = grayIndexer.getDouble(y, x);
	        double mean_ver = StatUtils.mean(line, pad, figure.imageHeight - 2 * pad);
	        double var_ver = StatUtils.variance(line, mean_ver, pad, figure.imageHeight - 2 * pad);
		
	        if (flag_mean)
	        {
	        	if (var_ver <= thres_ver && mean_ver >= thres_mean_ver)	ind_ver.add(x);
	        	//System.out.println("Vertical low variance lines:" +  Integer.toString(x));
	        }
	        else
	        {
	        	if (var_ver <= thres_ver) ind_ver.add(x);
	        }
		}		
        //--------------------------------------------------------------
        // extract the low variance vertical band
        ArrayList<Rectangle> rect_ver = new ArrayList<Rectangle>();
        // the first low variance horizontal line
        if (ind_ver.size() > 0) 
        {
            // left upper point
            int ymin = 0, xmin = ind_ver.get(0);
            // extend to the border if it is close to the border
            if (xmin < 0.05 * (double) figure.imageWidth)  xmin = 0;
            // right lower point
            int xmax = xmin, ymax = ymin; // temp value, will change in the following loop
            Rectangle rt = new Rectangle(xmin,ymin, xmax-xmin+1, ymax-ymin+1);
            rect_ver.add(rt);
        }
        for (int i = 0; i < ind_ver.size() - 1; i++) 
        {
            int diff = ind_ver.get(i + 1) - ind_ver.get(i);
            //System.out.println("difference : " + Integer.toString(diff));

            if ((double) diff >= 0.05 * (double) figure.imageHeight) 
            {
                //Set the right lower point of the previous band
                Rectangle rt = rect_ver.get(rect_ver.size() - 1);
                int xmin = rt.x, ymin = rt.y, ymax = figure.imageHeight - 1, xmax = ind_ver.get(i);
                Rectangle rt1 = new Rectangle(xmin,ymin, xmax-xmin+1, ymax-ymin+1);
                rect_ver.set(rect_ver.size() - 1, rt1);

                // set the location of the current band
                ymin = 0; xmin = ind_ver.get(i + 1);// left upper point
                xmax = xmin; ymax = ymin; // right lower point; temp value, will change in the next loop
                Rectangle rt2 = new Rectangle(xmin,ymin, xmax-xmin+1, ymax-ymin+1);
                rect_ver.add(rt2);
            }
        }
        // the last low variance vertical line
        if (ind_ver.size() > 0) 
        {
            // set the right lower point of the previous band
            Rectangle rt = rect_ver.get(rect_ver.size() - 1);
            int xmin = rt.x, ymin = rt.y, ymax = figure.imageHeight - 1, xmax = ind_ver.get(ind_ver.size() - 1);
            // extend to the border if it is close to the border
            if (xmax > 0.95 * (double) figure.imageWidth)	xmax = figure.imageWidth - 1;
            Rectangle rt1 = new Rectangle(xmin,ymin,xmax-xmin+1, ymax-ymin+1);
            rect_ver.set(rect_ver.size() - 1, rt1);

        }
        for (int i = 0; i < rect_ver.size(); i++)
        {
        	Rectangle rect = rect_ver.get(i);
        	if (rect.x <= 0 || rect.x + rect.width >= figure.imageWidth ) continue; //We ignore the board bands
            UniformBand band = new UniformBand(Orientation.Vertical, rect);
            uniformBands.add(band);
        }
	} 
	
}
