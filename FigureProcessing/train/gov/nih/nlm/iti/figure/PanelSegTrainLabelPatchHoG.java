package gov.nih.nlm.iti.figure;

import static org.bytedeco.javacpp.opencv_imgcodecs.*;
import static org.bytedeco.javacpp.opencv_imgproc.resize;

import java.nio.file.Path;

import org.bytedeco.javacpp.opencv_core.*;

final class PanelSegTrainLabelPatchHoG extends PanelSegTrainMethod
{
	float[] feature;

	@Override
	public void Train(Path imageFilePath, Path resultFolder) 
	{
		PanelSegLabelRegHoG hog = new PanelSegLabelRegHoG();
		
		Mat grayPatch = imread(imageFilePath.toString(), CV_LOAD_IMAGE_GRAYSCALE);
		if (grayPatch.cols() != 32 || grayPatch.rows() != 32)
			resize(grayPatch, grayPatch, new Size(32, 32));
		feature = hog.featureExtraction(grayPatch);
	}
	
}
