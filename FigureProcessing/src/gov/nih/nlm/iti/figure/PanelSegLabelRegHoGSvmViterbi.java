package gov.nih.nlm.iti.figure;

import org.bytedeco.javacpp.opencv_core.Mat;

public class PanelSegLabelRegHoGSvmViterbi extends PanelSegLabelRegHoGSvm 
{
	public PanelSegLabelRegHoGSvmViterbi()
	{
		super(); //Call base constructor
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

		MergeRecognitionLabelsViterbi();
	}

	private void MergeRecognitionLabelsViterbi() 
	{
		MergeDetectedLabelsSimple();
	}
}
