package gov.nih.nlm.iti.figure;

import static org.bytedeco.javacpp.opencv_imgproc.resize;

import java.io.IOException;
import java.util.ArrayList;

import org.bytedeco.javacpp.opencv_core.*;
import libsvm.*;

public final class PanelSegLabelRegHoGSvm extends PanelSegLabelRegHoG
{
	private static svm_model svmModel;
	
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
		preSegment(image);

		HoGDetect();
	
		SvmClassification();

		MergeDetectedLabelsSimple();
	}

	private void SvmClassification() 
	{
		for (int i = 0; i < figure.segmentationResultIndividualLabel.size(); i++)
		{
			ArrayList<PanelSegInfo> infos = figure.segmentationResultIndividualLabel.get(i);
			if (infos == null) continue;
			for (int j = 0; j < infos.size(); j++)
			{
				PanelSegInfo info = infos.get(j);
				if (info == null) continue;
				
				int x = info.labelRect.x, y = info.labelRect.y, w = info.labelRect.width, h = info.labelRect.height;
				Mat patch = info.labelInverted ? figure.imageGrayInverted.apply(new Rect(x, y, w, h)) : figure.imageGray.apply(new Rect(x, y, w, h));
		        Mat patchNormalized = new Mat(); resize(patch, patchNormalized, winSize_64);
				
		        float[] feature = featureExtraction(patchNormalized);
		        svm_node[] svmNode = LibSvmEx.float2SvmNode(feature); double[] probs = new double[LibSvmEx.getNrClass(svmModel)];
		        /*double label = */svm.svm_predict_probability(svmModel, svmNode, probs);

		        info.labelProbs = probs;		        infos.set(j, info);
			}
			figure.segmentationResultIndividualLabel.set(i, infos);
		}
	}
}
