package gov.nih.nlm.iti.figure;

import static org.bytedeco.javacpp.opencv_core.subtract;
import static org.bytedeco.javacpp.opencv_imgcodecs.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;

import java.nio.file.Path;
import java.util.concurrent.ThreadLocalRandom;

import org.bytedeco.javacpp.opencv_core.*;

final class PanelSegTrainLabelPatchNeg extends PanelSegTrainMethod 
{

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
