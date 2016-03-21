package gov.nih.nlm.iti.figure;

import static org.bytedeco.javacpp.opencv_core.subtract;
import static org.bytedeco.javacpp.opencv_imgcodecs.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ThreadLocalRandom;

import org.bytedeco.javacpp.opencv_core.*;

/**
 * A PanelSegTrainMethod class for generating some initial negative patched for label detection/recognition.
 * @author Jie Zou
 *
 */
final class PanelSegTrainLabelPatchNeg extends PanelSegTrainMethod 
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
				segTrain.methods.add(new PanelSegTrainLabelPatchNeg());
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
		//Load original Image and generate gray and inverted images.		
		String image_file_path = imageFilePath.toString();
		image = imread(image_file_path, CV_LOAD_IMAGE_COLOR);
		imageGray = new Mat();		cvtColor(image, imageGray, CV_BGR2GRAY);
		imageGrayInverted = subtract(Scalar.all(255), imageGray).asMat();
		
		{	//Randomly select one patch from gray images.
			int x, y, w, h;
			x = ThreadLocalRandom.current().nextInt(0, image.cols());
			y = ThreadLocalRandom.current().nextInt(0, image.rows());
			w = h = ThreadLocalRandom.current().nextInt(10, 80);
			
			if (x + w <= image.cols() && y + h <= image.rows())
			{
				Mat patch = imageGray.apply(new Rect(x, y, w, h));

				//Construct filename
				String resultFilename = imageFilePath.getFileName().toString();
				int pos = resultFilename.lastIndexOf('.');
				resultFilename = resultFilename.substring(0, pos) + ".p.bmp";
				Path resultPatchFile = resultFolder.resolve(resultFilename);
				imwrite(resultPatchFile.toString(), patch);
			}
		}
		
		{	//Randomly select one patch from grayInverted images.
			int x, y, w, h;
			x = ThreadLocalRandom.current().nextInt(0, image.cols());
			y = ThreadLocalRandom.current().nextInt(0, image.rows());
			w = h = ThreadLocalRandom.current().nextInt(10, 80);

			if (x + w <= image.cols() && y + h <= image.rows())
			{
				Mat patch = imageGrayInverted.apply(new Rect(x, y, w, h));

				//Construct filename
				String resultFilename = imageFilePath.getFileName().toString();
				int pos = resultFilename.lastIndexOf('.');
				resultFilename = resultFilename.substring(0, pos) + ".n.bmp";
				Path resultPatchFile = resultFolder.resolve(resultFilename);
				imwrite(resultPatchFile.toString(), patch);
			}
			
		}
		
	}

}
