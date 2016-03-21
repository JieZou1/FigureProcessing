package gov.nih.nlm.iti.figure;

import static org.bytedeco.javacpp.opencv_imgcodecs.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;
import org.bytedeco.javacpp.opencv_core.*;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * A PanelSegTrainMethod class for ground truth annotation visualization  
 * @author Jie Zou
 */
final class PanelSegTrainGTViz extends PanelSegTrainMethod 
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
				segTrain.methods.add(new PanelSegTrainGTViz());
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
		String image_file_path = imageFilePath.toString();
		
		//Load GT annotation
		String gt_xml_file = image_file_path.replaceAll(".jpg", "_data.xml");
		if (!Files.exists(Paths.get(gt_xml_file))) return;
		gtSegmentation = PanelSeg.LoadPanelSegGt(gt_xml_file);
		
		//Load original Image
		image = imread(image_file_path, CV_LOAD_IMAGE_COLOR);
		
		//Superimpose gt annotation to orginal image
		@SuppressWarnings("resource")
		Scalar red = new Scalar(0, 0, 255, 0), blue = new Scalar(255, 0, 0, 0);
		for (int i = 0; i < gtSegmentation.size(); i++)
		{
			Scalar color = (i % 2 == 0)? red : blue;
			PanelSegInfo panel = gtSegmentation.get(i);
			if (panel.panelRect != null)
			{
				Rect panel_rect = new Rect(panel.panelRect.x, panel.panelRect.y, panel.panelRect.width, panel.panelRect.height);	
				org.bytedeco.javacpp.opencv_imgproc.rectangle(image, panel_rect, color, 3, 8, 0);
			}
			if (panel.labelRect != null)
			{
				Rect label_rect = new Rect(panel.labelRect.x, panel.labelRect.y, panel.labelRect.width, panel.labelRect.height);	
				rectangle(image, label_rect, color,3, 8, 0);
			}
			if (panel.panelLabel != "")
			{
				putText(image, panel.panelLabel, new Point(panel.labelRect.x + panel.labelRect.width, panel.labelRect.y + panel.labelRect.height), CV_FONT_HERSHEY_PLAIN, 2.0, color, 3, 8, false);
			}			
		}
		
		String rst_image_path = resultFolder.resolve(imageFilePath.getFileName().toString()).toString();
		imwrite(rst_image_path, image);
	}

}
