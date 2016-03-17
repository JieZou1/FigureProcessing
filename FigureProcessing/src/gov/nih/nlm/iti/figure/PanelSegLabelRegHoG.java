package gov.nih.nlm.iti.figure;

import org.bytedeco.javacpp.opencv_core.*;
import org.bytedeco.javacpp.opencv_objdetect.HOGDescriptor;

public final class PanelSegLabelRegHoG extends PanelSeg 
{
	private HOGDescriptor hog;
	
    static private Size winSize_64 = new Size(64, 64);
    static private Size blockSize = new Size(16, 16);
    static private Size blockStride = new Size(8, 8);
    static private Size cellSize = new Size(8, 8);
    static private int nbins = 9;
    static private Size winStride = new Size(8, 8);
    static private Size trainPadding = new Size(0, 0);
//    static private int derivAperture = 1;
//    static private double winSigma = -1;
//    static private double L2HysThreshold = 0.2;
//    static private boolean gammaCorrection = true;
//    static private int nLevels = 64;
	
	public PanelSegLabelRegHoG() 
	{
		hog = new HOGDescriptor(winSize_64, blockSize, blockStride, cellSize, nbins);
		//hog = new HOGDescriptor(winSize_64, blockSize, blockStride, cellSize, nbins, derivAperture, winSigma, _histogramNormType, _L2HysThreshold, gammaCorrection, nlevels, _signedGradient)
	}
	
	/**
	 * The main entrance function to perform segmentation.
	 * Call getResult* functions to retrieve result in different format.
	 * @throws Exception 
	 */
	public void segment(Mat image)  
	{
		super.segment(image);
	}
	
	public float[] featureExtraction(Mat grayPatch) 
	{
		float[] features = null;
		hog.compute(grayPatch, features, winStride, trainPadding, null);
		return features;
	}
}
