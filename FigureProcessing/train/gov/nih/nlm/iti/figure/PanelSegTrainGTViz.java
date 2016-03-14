package gov.nih.nlm.iti.figure;

import static org.bytedeco.javacpp.opencv_imgcodecs.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;
import org.bytedeco.javacpp.opencv_core.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;


/**
 * For ground truth annotation visualization  
 */
public class PanelSegTrainGTViz extends PanelSegTrainMethod 
{

	@Override
	public void Train(Path imageFilePath, Path resultFolder) 
	{
		String image_file_path = imageFilePath.toString();
		
		//Load GT annotation
		String gt_xml_file = image_file_path.replaceAll(".jpg", "_data.xml");
		
		if (!Files.exists(Paths.get(gt_xml_file))) return;
		
		ArrayList<PanelSegResult> gt_segmentation = null;
		try {
			gt_segmentation = PanelSeg.LoadPanelSegGt(gt_xml_file);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//Load original Image
		Mat image = imread(image_file_path, CV_LOAD_IMAGE_COLOR);
		
		//Superimpose gt annotation to orginal image
		Scalar red = new Scalar(0, 0, 255, 0), blue = new Scalar(255, 0, 0, 0);
		for (int i = 0; i < gt_segmentation.size(); i++)
		{
			Scalar color = i%2 == 0? red:blue;
			PanelSegResult panel = gt_segmentation.get(i);
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
