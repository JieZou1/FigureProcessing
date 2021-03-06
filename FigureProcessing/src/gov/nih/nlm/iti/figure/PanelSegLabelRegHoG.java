package gov.nih.nlm.iti.figure;

import static org.bytedeco.javacpp.opencv_imgproc.resize;

import java.awt.Rectangle;
import java.lang.reflect.Field;
import java.util.ArrayList;

import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.javacpp.opencv_core.*;
import org.bytedeco.javacpp.opencv_objdetect.HOGDescriptor;

public class PanelSegLabelRegHoG extends PanelSegLabelReg
{
	static String[] labelSetsHOG = {"A", "ad", "BDEFPpR", "bhKk", "CceGOoQ", "fIiJjLlrTt", "gqSs", "HMmNn" };
	
	//The HoG parameters used in both training and testing
    static protected Size winSize_64 = new Size(64, 64);
//    static private Size winSize_32 = new Size(32, 32); //The size of the training label patches
    static private Size blockSize = new Size(16, 16);
    static private Size blockStride = new Size(8, 8);
    static private Size cellSize = new Size(8, 8);
    static private int nbins = 9;
//  static private int derivAperture = 1;
//  static private double winSigma = -1;
//  static private double L2HysThreshold = 0.2;
//  static private boolean gammaCorrection = true;
//  static private int nLevels = 64;
	
    //The HoG parameters used in testing only.
    static private double hitThreshold = 0;			//Threshold for the distance between features and SVM classifying plane.
    static private Size winStride = new Size(8, 8); //Sliding window step, It must be a multiple of block stride
    static private Size padding = new Size(0, 0);	//Adds a certain amount of extra pixels on each side of the input image
    //static private Size padding = new Size(32, 32);	//Adds a certain amount of extra pixels on each side of the input image
    static private double scale0 = 1.05;			//Coefficient of the detection window increase
    static private int groupThreshold = 2;
    static private boolean useMeanShiftGrouping = false;
    
    static private double minimumLabelSize = 12.0;	//We assume the smallest label patch is 12x12.
    
    private HOGDescriptor hog;
    private float[][] svmModels;
	
	protected ArrayList<ArrayList<PanelSegInfo>> hogDetectionResult; //The HOG method detection result of all labelSetsToDetect
    
	public PanelSegLabelRegHoG() 
	{
		int n = labelSetsHOG.length;		svmModels = new float[n][];
		for (int i = 0; i < n; i++)
		{
			String classString = "gov.nih.nlm.iti.figure.PanelSegLabelRegHoGModel_" + labelSetsHOG[i];
			try {
				Class<?> cls = Class.forName(classString);
				Field field = cls.getField("svmModel");
				svmModels[i] = (float[])field.get(null);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		hog = new HOGDescriptor(winSize_64, blockSize, blockStride, cellSize, nbins);
		//hog = new HOGDescriptor(winSize_32, blockSize, blockStride, cellSize, nbins, derivAperture, winSigma, _histogramNormType, _L2HysThreshold, gammaCorrection, nlevels, _signedGradient)
	}
	
	/**
	 * The main entrance function to perform segmentation.
	 * Call getResult* functions to retrieve result in different format.
	 */
	public void segment(Mat image)  
	{
		preSegment(image);	//Common initializations for all segmentation method.

		HoGDetect();		//HoG Detection, detected patches are stored in figure.hogDetectionResult.
		
		//Merge all hogDetectionResult to segmentationResult
		mergeDetectedLabelsSimple();
	}
	
	protected void HoGDetect() 
	{
		int n = labelSetsHOG.length;
		hogDetectionResult = new ArrayList<ArrayList<PanelSegInfo>>();
		for (int i = 0; i < n; i++) hogDetectionResult.add(null);
		
		//Resize the image. 
        //double scale = 64.0 / minimumLabelSize; //check statistics.txt to decide this scaling factor.
        double scale = 64.0 / minimumLabelSize; //check statistics.txt to decide this scaling factor.
        int _width = (int)(figure.imageWidth * scale + 0.5), _height = (int)(figure.imageHeight * scale + 0.5);
        Size newSize = new Size(_width, _height);
        Mat imgScaled = new Mat(); resize(figure.imageGray, imgScaled, newSize);
        //Mat imgeScaledInverted = new Mat(); resize(figure.imageGrayInverted, imgeScaledInverted, newSize);
		//Mat imgeScaledInverted = subtract(Scalar.all(255), imgScaled).asMat();
		
		for (int i = 0; i < n; i++)
		{
			String panelLabelSet = labelSetsHOG[i];
			float[] svmModel = svmModels[i];
			double minSize = labelMinSize * scale;
			double maxSize = labelMaxSize * scale;
			
			FloatPointer fp = new FloatPointer(svmModel);			hog.setSVMDetector(new Mat(fp));
			
			//Search on original and inverted images (after scaling of course)
			ArrayList<PanelSegInfo> candidates1 = DetectMultiScale(imgScaled, maxSize, minSize, panelLabelSet, false);
			//ArrayList<PanelSegInfo> candidates2 = DetectMultiScale(imgeScaledInverted, maxSize, minSize, panelLabelSet, true);
			
			ArrayList<PanelSegInfo> candidates = new ArrayList<PanelSegInfo>();
			if (candidates1 != null) candidates.addAll(candidates1);
			//if (candidates2 != null) candidates.addAll(candidates2);
			
			if (candidates.size() > 0)
			{
				candidates = RemoveOverlappedCandidates(candidates);

				//Scale back to the original size, and save the result to figure.segmentationResultIndividualLabel
				ArrayList<PanelSegInfo> segmentationResult = new ArrayList<PanelSegInfo>();
				for (int j = 0; j < candidates.size(); j++)
				{
					PanelSegInfo segInfo = candidates.get(j);
					Rectangle rect = segInfo.labelRect;
		            Rectangle orig_rect = new Rectangle((int)(rect.x / scale + .5), (int)(rect.y / scale + .5), (int)(rect.width / scale + .5), (int)(rect.height / scale + .5));
		            segInfo.labelRect = orig_rect;
		            segmentationResult.add(segInfo);
				}
				hogDetectionResult.set(i, segmentationResult);
			}
		}
	}
	
	private ArrayList<PanelSegInfo> DetectMultiScale(Mat img, double maxSize, double minSize, String panelLabelSet, Boolean inverted)
	{
		ArrayList<PanelSegInfo> candidates = new ArrayList<PanelSegInfo>();
		
		RectVector rectVector = new RectVector();			DoublePointer dp = new DoublePointer();	
		//hog.detectMultiScale(img, rectVector, dp);
		hog.detectMultiScale(img, rectVector, dp, hitThreshold, winStride, padding, scale0, groupThreshold, useMeanShiftGrouping);
		
		if (rectVector == null || rectVector.size() == 0) return null;
		
		double[] scores = new double[(int)rectVector.size()]; dp.get(scores);		
		for (int k = 0; k < rectVector.size(); k++)
		{
			Rect labelRect = rectVector.get(k);
			if (labelRect.width() > maxSize || labelRect.height() > maxSize) continue;
			if (labelRect.width() < minSize || labelRect.height() < minSize) continue;
			
			int centerX = labelRect.x() + labelRect.width() / 2;
			int centerY = labelRect.y() + labelRect.height() / 2;
			if (centerX <= 0 || centerX >= img.cols()) continue;
			if (centerY <= 0 || centerY >= img.rows()) continue; //We ignore cases, where the detected patch is half outside the image.
			
			PanelSegInfo segInfo = new PanelSegInfo();
			segInfo.labelRect = new Rectangle(labelRect.x(), labelRect.y(), labelRect.width(), labelRect.height());
			segInfo.panelLabel = panelLabelSet;
			segInfo.labelInverted = inverted;
			segInfo.labelScore = scores[k];
			
			candidates.add(segInfo);
		}
		return candidates;
	}
	
	/**
	 * The simplest method to merge label detection results saved in segmentationResultIndividualLabel to segmentationResult <p>
	 * This method simply combine all detected results
	 */
	protected void mergeDetectedLabelsSimple() 
	{
		figure.panelSegResult = new ArrayList<PanelSegInfo>(); //Reset
		if (hogDetectionResult == null) return;
		
		for (int i = 0; i < hogDetectionResult.size(); i++)
		{
			ArrayList<PanelSegInfo> result = hogDetectionResult.get(i);
			if (result == null) continue;
			for (int j = 0; j < result.size(); j++)
				figure.panelSegResult.add(result.get(j));
		}
	}
	
	/**
	 * Extract HoG descriptors from gray patch
	 * @param grayPatch
	 * @return
	 */
	public float[] featureExtraction(Mat grayPatch) 
	{
		FloatPointer descriptors = new FloatPointer();		
		hog.compute(grayPatch, descriptors);
		//hog.compute(grayPatch, descriptors, winStride, padding, null);
		
		int n = (int)hog.getDescriptorSize();
		float[] features = new float[n];		descriptors.get(features);
		return features;
	}
}
