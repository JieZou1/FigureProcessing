package gov.nih.nlm.iti.figure;

import static org.bytedeco.javacpp.opencv_imgproc.resize;

import java.io.IOException;
import java.util.ArrayList;

import org.bytedeco.javacpp.opencv_core.*;
import libsvm.*;

class PanelSegLabelRegHoGSvm extends PanelSegLabelRegHoG
{
	protected static svm_model svmModel;
	
	public PanelSegLabelRegHoGSvm()
	{
		super(); //Call base constructor

		if (svmModel == null)
		{
			try {
				svmModel = svm.svm_load_model("svm_model");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * The main entrance function to perform segmentation.
	 * Call getResult* functions to retrieve result in different format.
	 */
	public void segment(Mat image)
	{
		preSegment(image);	//Common initializations for all segmentation method.

		HoGDetect();		//HoG Detection, detected patches are stored in figure.hogDetectionResult.
	
		mergeDetectedLabelsSimple();	//All detected patches are merged into figure.segmentationResult

		SvmClassification();			//SVM (RBF kernel) classification of each detected patch in figure.segmentationResult.

		MergeRecognitionLabelsSimple();	
	}

	protected void SvmClassification() 
	{
		if (figure.panelSegResult.size() == 0) return;
		
		for (int i = 0; i < figure.panelSegResult.size(); i++)
		{
			PanelSegInfo info = figure.panelSegResult.get(i);
			if (info == null) continue;
			
			Mat patch = info.labelInverted ? AlgorithmEx.cropImage(figure.imageGrayInverted, info.labelRect) : AlgorithmEx.cropImage(figure.imageGray, info.labelRect);
	        Mat patchNormalized = new Mat(); resize(patch, patchNormalized, winSize_64);
			
	        float[] feature = featureExtraction(patchNormalized);
	        svm_node[] svmNode = LibSvmEx.float2SvmNode(feature); double[] probs = new double[LibSvmEx.getNrClass(svmModel)];
	        /*double label = */svm.svm_predict_probability(svmModel, svmNode, probs);

	        info.labelProbs = probs;
	        //figure.segmentationResult.set(i, info);
		}
		
//		for (int i = 0; i < figure.segmentationResultIndividualLabel.size(); i++)
//		{
//			ArrayList<PanelSegInfo> infos = figure.segmentationResultIndividualLabel.get(i);
//			if (infos == null) continue;
//			for (int j = 0; j < infos.size(); j++)
//			{
//				PanelSegInfo info = infos.get(j);
//				if (info == null) continue;
//				
//				//int x = info.labelRect.x, y = info.labelRect.y, w = info.labelRect.width, h = info.labelRect.height;
//				Mat patch = info.labelInverted ? AlgorithmEx.cropImage(figure.imageGrayInverted, info.labelRect) : AlgorithmEx.cropImage(figure.imageGray, info.labelRect);
//		        Mat patchNormalized = new Mat(); resize(patch, patchNormalized, winSize_64);
//				
//		        float[] feature = featureExtraction(patchNormalized);
//		        svm_node[] svmNode = LibSvmEx.float2SvmNode(feature); double[] probs = new double[LibSvmEx.getNrClass(svmModel)];
//		        /*double label = */svm.svm_predict_probability(svmModel, svmNode, probs);
//
//		        info.labelProbs = probs;		        infos.set(j, info);
//			}
//			//figure.segmentationResultIndividualLabel.set(i, infos);
//		}
	}

	private void MergeRecognitionLabelsSimple()
	{
		if (figure.panelSegResult.size() == 0) return;
		
		//set label and score according to the max of labelProbs, computed by SVM
		ArrayList<PanelSegInfo> candidates = new ArrayList<PanelSegInfo>();
        for (int j = 0; j < figure.panelSegResult.size(); j++)
        {
        	PanelSegInfo obj = figure.panelSegResult.get(j);
        	int maxIndex = AlgorithmEx.findMaxIndex(obj.labelProbs);
        	if (maxIndex == labelToReg.length) continue; //Classified as a negative sample.
        		
	        obj.labelScore = obj.labelProbs[maxIndex];
	        obj.panelLabel = "" + labelToReg[maxIndex];
	        candidates.add(obj);
        }
		
        figure.panelSegResult = RemoveOverlappedCandidates(candidates);
	}
}

