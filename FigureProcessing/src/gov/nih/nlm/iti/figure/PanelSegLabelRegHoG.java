package gov.nih.nlm.iti.figure;

import static org.bytedeco.javacpp.opencv_core.subtract;
import static org.bytedeco.javacpp.opencv_imgproc.resize;

import java.awt.Rectangle;
import java.security.PrivilegedActionException;
import java.util.ArrayList;

import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.*;
import org.bytedeco.javacpp.opencv_objdetect.HOGDescriptor;

public final class PanelSegLabelRegHoG extends PanelSeg 
{
//    static private Size winSize_64 = new Size(64, 64);
//    static private Size winSize_32 = new Size(32, 32);
//    static private Size blockSize = new Size(16, 16);
//    static private Size blockStride = new Size(8, 8);
//    static private Size cellSize = new Size(8, 8);
//    static private int nbins = 9;
//    static private Size winStride = new Size(8, 8);
//    static private Size trainPadding = new Size(0, 0);
//    static private int derivAperture = 1;
//    static private double winSigma = -1;
//    static private double L2HysThreshold = 0.2;
//    static private boolean gammaCorrection = true;
//    static private int nLevels = 64;
	
	private HOGDescriptor hog;
	
	public PanelSegLabelRegHoG() 
	{
	    Size winSize_32 = new Size(32, 32);
	    Size blockSize = new Size(16, 16);
	    Size blockStride = new Size(8, 8);
	    Size cellSize = new Size(8, 8);
	    int nbins = 9;
	    
		hog = new HOGDescriptor(winSize_32, blockSize, blockStride, cellSize, nbins);
		//hog = new HOGDescriptor(winSize_32, blockSize, blockStride, cellSize, nbins, derivAperture, winSigma, _histogramNormType, _L2HysThreshold, gammaCorrection, nlevels, _signedGradient)
	}
	
	/**
	 * The main entrance function to perform segmentation.
	 * Call getResult* functions to retrieve result in different format.
	 * @throws Exception 
	 */
	public void segment(Mat image)  
	{
		super.segment(image);

		//Resize the image. We assume the smallest label patch is 10x10.
        double scale = 32.0 / 10.0; //check statistics.txt to decide this scaling factor.
        int _width = (int)(image.cols() * scale + 0.5);
        int _height = (int)(image.rows() * scale + 0.5);
        Size newSize = new Size(_width, _height);
        Mat imgScaled = new Mat(); resize(figure.imageGray, imgScaled, newSize);
		Mat imgeScaledInverted = subtract(Scalar.all(255), imgScaled).asMat();
		
		int n = PanelSeg.labelArray.length;
		ArrayList<PanelSegInfo> results = new ArrayList<PanelSegInfo>();
		for (int i = 0; i < n; i++)
		{
			FloatPointer fp = new FloatPointer(PanelSegLabelRegHoGModels.svmModels[i]);
			hog.setSVMDetector(new Mat(fp));
			
			{	//Search on original images (after scaling of course)
				RectVector rectVector = new RectVector();
				hog.detectMultiScale(imgScaled, rectVector);
				for (int k = 0; k < rectVector.size(); k++)
				{
					Rect labelRect = rectVector.get(k);
					
					PanelSegInfo segInfo = new PanelSegInfo();
					segInfo.labelRect = new Rectangle(labelRect.x(), labelRect.y(), labelRect.width(), labelRect.height());
					segInfo.panelLabel = "" + PanelSeg.labelArray[i];
					segInfo.labelInverted = false;
					results.add(segInfo);
				}
			}
			
			{	//Search on inverted images (after scaling of course)
				RectVector rectVector = new RectVector();
				hog.detectMultiScale(imgeScaledInverted, rectVector);
				for (int k = 0; k < rectVector.size(); k++)
				{
					Rect labelRect = rectVector.get(k);
					
					PanelSegInfo segInfo = new PanelSegInfo();
					segInfo.labelRect = new Rectangle(labelRect.x(), labelRect.y(), labelRect.width(), labelRect.height());
					segInfo.panelLabel = "" + PanelSeg.labelArray[i];
					segInfo.labelInverted = true;
					results.add(segInfo);
				}
			}
		}
		
		//Scale back to the original size
		figure.segmentationResult = new ArrayList<PanelSegInfo>();
		for (int i = 0; i < results.size(); i++)
		{
			PanelSegInfo segInfo = results.get(i);
			Rectangle rect = segInfo.labelRect;
            Rectangle orig_rect = new Rectangle((int)(rect.x / scale + .5), (int)(rect.y / scale + .5), (int)(rect.width / scale + .5), (int)(rect.height / scale + .5));
            segInfo.labelRect = orig_rect;
            figure.segmentationResult.add(segInfo);
		}
	}
	
	public float[] featureExtraction(Mat grayPatch) 
	{
        //Size winStride = new Size(8, 8);        Size trainPadding = new Size(0, 0);
		FloatPointer descriptors = new FloatPointer();		
		hog.compute(grayPatch, descriptors);
		//hog.compute(grayPatch, descriptors, winStride, trainPadding, null);
		
		int n = (int)hog.getDescriptorSize();
		float[] features = new float[n];		descriptors.get(features);
		return features;
	}
}
