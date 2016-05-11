package gov.nih.nlm.iti.figure;

import static org.bytedeco.javacpp.opencv_imgcodecs.*;
import static org.bytedeco.javacpp.opencv_imgproc.resize;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import org.bytedeco.javacpp.opencv_core.*;

/**
 * A PanelSegTrainMethod class for extracting HoG features from normalized patches and then create SVM training file
 * @author Jie Zou
 *
 */
final class PanelSegTrainLabelHoG extends PanelSegTrainMethod
{
	/**
	 * Prepare the Panel Segmentation training. 
	 * @param method	The PanelSeg method
	 * @param srcFolder	The source folder
	 * @param rstFolder	The result folder
	 *
	 * @return a PanelSegTrain instance with all the parameters are set 
	 */
	static PanelSegTrain createPanelSegTrain(String method, Path srcFolder, Path rstFolder)
	{
		PanelSegTrain segTrain = new PanelSegTrain(method, srcFolder, rstFolder);
		
		segTrain.flags = new ArrayList<Boolean>();
		
		//Positive samples
		Path posPath = srcFolder.resolve("pos");
		try (DirectoryStream<Path> dirStrm = Files.newDirectoryStream(posPath)) 
		{			
			for (Path path : dirStrm)
			{
				String filename = path.toString();
				if (!filename.endsWith(".bmp")) continue;
				segTrain.allPaths.add(path);
				segTrain.methods.add(new PanelSegTrainLabelHoG());
				segTrain.flags.add(true);
			}
		}
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//Positive samples collected from bootstrapping
		Path pos1Path = srcFolder.resolve("pos1");
		try (DirectoryStream<Path> dirStrm = Files.newDirectoryStream(pos1Path)) 
		{			
			for (Path path : dirStrm)
			{
				String filename = path.toString();
				if (!filename.endsWith(".bmp")) continue;
				segTrain.allPaths.add(path);
				segTrain.methods.add(new PanelSegTrainLabelHoG());
				segTrain.flags.add(true);
			}
		}
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//Negative samples
		Path negPath = srcFolder.resolve("neg");
		try (DirectoryStream<Path> dirStrm = Files.newDirectoryStream(negPath)) 
		{			
			for (Path path : dirStrm)
			{
				String filename = path.toString();
				if (!filename.endsWith(".bmp")) continue;
				segTrain.allPaths.add(path);
				segTrain.methods.add(new PanelSegTrainLabelHoG());
				segTrain.flags.add(false);
			}
		}
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return segTrain;
	}
	
	float[] feature;

	@Override
	public void Train(Path imageFilePath, Path resultFolder) 
	{
		PanelSegLabelRegHoG hog = new PanelSegLabelRegHoG();
		
		Mat grayPatch = imread(imageFilePath.toString(), CV_LOAD_IMAGE_GRAYSCALE);
		if (grayPatch.cols() != 64 || grayPatch.rows() != 64)
			resize(grayPatch, grayPatch, new Size(64, 64));
		feature = hog.featureExtraction(grayPatch);
	}
	
	static void generateLabelPatchHoGTrain(ArrayList<Boolean> flags, ArrayList<PanelSegTrainMethod> methods, String filename)
	{
		int n = flags.size();		double[] targets = new double[n];		float[][] features = new float[n][];
		for (int i = 0; i < n; i++)
		{
			targets[i] = flags.get(i) == true ? 1 : -1;
			PanelSegTrainLabelHoG method = (PanelSegTrainLabelHoG)methods.get(i);
			features[i] = method.feature;
		}
		LibSvmEx.SaveInLibSVMFormat(filename, targets, features);
	}
	
}
