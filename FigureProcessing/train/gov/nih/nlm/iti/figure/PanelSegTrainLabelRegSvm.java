package gov.nih.nlm.iti.figure;

import static org.bytedeco.javacpp.opencv_imgcodecs.CV_LOAD_IMAGE_GRAYSCALE;
import static org.bytedeco.javacpp.opencv_imgcodecs.imread;
import static org.bytedeco.javacpp.opencv_imgproc.resize;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Size;

final class PanelSegTrainLabelRegSvm extends PanelSegTrainMethod
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

		segTrain.labels = new ArrayList<Integer>();
		
		//Add positive samples
		for (int i = 0 ; i < PanelSeg.labelToReg.length; i++)
		{
			char ch = PanelSeg.labelToReg[i];
			Path path = Character.isLowerCase(ch) ? srcFolder.resolve("" + PanelSeg.labelToReg[i]) : srcFolder.resolve("" + PanelSeg.labelToReg[i] +"_");
			
			try (DirectoryStream<Path> dirStrm = Files.newDirectoryStream(path)) 
			{			
				for (Path p : dirStrm)
				{
					String filename = p.toString();
					if (!filename.endsWith(".bmp")) continue;
					segTrain.allPaths.add(p);
					segTrain.methods.add(new PanelSegTrainLabelRegSvm());
					segTrain.labels.add(i);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		//Add negative samples (TO DO Labels)
		int negtiveLabel = PanelSeg.labelToReg.length;
		for (int i = 0 ; i < PanelSeg.labelToRegTodo.length; i++)
		{
			char ch = PanelSeg.labelToRegTodo[i];
			Path path = Character.isLowerCase(ch) ? srcFolder.resolve("" + PanelSeg.labelToRegTodo[i]) : srcFolder.resolve("" + PanelSeg.labelToRegTodo[i] +"_");
			
			try (DirectoryStream<Path> dirStrm = Files.newDirectoryStream(path)) 
			{			
				for (Path p : dirStrm)
				{
					String filename = p.toString();
					if (!filename.endsWith(".bmp")) continue;
					segTrain.allPaths.add(p);
					segTrain.methods.add(new PanelSegTrainLabelRegSvm());
					segTrain.labels.add(negtiveLabel);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		//Add negative samples
		{
			Path path = srcFolder.resolve("Neg");
			
			try (DirectoryStream<Path> dirStrm = Files.newDirectoryStream(path)) 
			{			
				for (Path p : dirStrm)
				{
					String filename = p.toString();
					if (!filename.endsWith(".bmp")) continue;
					segTrain.allPaths.add(p);
					segTrain.methods.add(new PanelSegTrainLabelRegSvm());
					segTrain.labels.add(negtiveLabel);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return segTrain;
	}

	float[] feature;
	
	@Override
	public void Train(Path imageFilePath, Path resultFolder) throws Exception 
	{
		PanelSegLabelRegHoG hog = new PanelSegLabelRegHoG();
		
		Mat grayPatch = imread(imageFilePath.toString(), CV_LOAD_IMAGE_GRAYSCALE);
		if (grayPatch.cols() != 64 || grayPatch.rows() != 64)
			resize(grayPatch, grayPatch, new Size(64, 64));
		feature = hog.featureExtraction(grayPatch);
	}

	static void generateLabelRegSvmTrain(ArrayList<Integer> labels, ArrayList<PanelSegTrainMethod> methods, String filename)
	{
		int n = labels.size();		double[] targets = new double[n];		float[][] features = new float[n][];
		for (int i = 0; i < n; i++)
		{
			targets[i] = labels.get(i);
			PanelSegTrainLabelRegSvm method = (PanelSegTrainLabelRegSvm)methods.get(i);
			features[i] = method.feature;
		}
		LibSvmEx.SaveInLibSVMFormat(filename, targets, features);
	}
	
}
