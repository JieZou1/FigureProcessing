package gov.nih.nlm.iti.figure;

import static org.bytedeco.javacpp.opencv_imgcodecs.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;

import java.awt.Rectangle;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import org.bytedeco.javacpp.opencv_core.*;

/**
 * A PanelSegTrainMethod class for BootStrap Negative Patches for HoG-based Panel Label recognition  
 * @author Jie Zou
 *
 */
final class PanelSegTrainLabelBootStrap extends PanelSegTrainMethod 
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

		try (DirectoryStream<Path> dirStrm = Files.newDirectoryStream(srcFolder)) 
		{			
			for (Path path : dirStrm)
			{
				String filename = path.toString();
				if (!filename.endsWith(".jpg")) continue;
				
				segTrain.allPaths.add(path);
				segTrain.methods.add(new PanelSegTrainLabelBootStrap());
			}
		}
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return segTrain;
	}

	@Override
	public void Train(Path imageFilePath, Path resultFolder) throws Exception 
	{
		//Do segmentation, ie, label detection through HoG
		String image_file_path = imageFilePath.toString();
		PanelSegLabelRegHoG hog = new PanelSegLabelRegHoG();
		hog.segment(image_file_path);
		
		//Save detected patches
		for (int i = 0; i < hog.figure.segmentationResultIndividualLabel.size(); i++)
		{
			ArrayList<PanelSegInfo> segmentationResult = hog.figure.segmentationResultIndividualLabel.get(i);
			if (segmentationResult == null) continue;
			
			for (int j = 0; j < segmentationResult.size(); j++)
			{
				if (j == 3) break; //We just save the top 3 patches for training, in order to avoiding collecting a very large training set at the beginning.
				
				PanelSegInfo segInfo = segmentationResult.get(j);
				Rectangle rectangle = segInfo.labelRect;
				
				Mat patch = segInfo.labelInverted ? AlgorithmEx.cropImage(hog.figure.imageGrayInverted, rectangle) : AlgorithmEx.cropImage(hog.figure.imageGray, rectangle);
				resize(patch, patch, new Size(64, 64)); //Resize to 64x64 for easy browsing the results
				
				//Construct filename
				Path resultPatchFolder = Character.isUpperCase(PanelSeg.labelToDetect[i])? resultFolder.resolve(segInfo.panelLabel + "_") : resultFolder.resolve(segInfo.panelLabel);	
				if (!Files.exists(resultPatchFolder))	Files.createDirectory(resultPatchFolder);
				String resultFilename = imageFilePath.getFileName().toString();
				int pos = resultFilename.lastIndexOf('.');
				
				resultFilename = resultFilename.substring(0, pos) + "." + rectangle.toString() + "." + segInfo.labelInverted + ".bmp";
				Path resultPatchFile = resultPatchFolder.resolve(resultFilename);
				imwrite(resultPatchFile.toString(), patch);
			}
		}
	}

}
