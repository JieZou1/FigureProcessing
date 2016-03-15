package gov.nih.nlm.iti.figure;

import java.nio.file.Path;
import java.util.ArrayList;

import org.bytedeco.javacpp.opencv_core.Mat;

/**
 * The base class for all panel segmentation Training routines. <p>
 * 
 * This class is not intended to be instantiated, so we make it abstract. 
 * 
 * @author Jie Zou
 *
 */
public abstract class PanelSegTrainMethod 
{
	protected Mat image;
	protected ArrayList<PanelSegInfo> gt_segmentation;
	
	public abstract void Train(Path imageFilePath, Path resultFolder) throws Exception;
}
