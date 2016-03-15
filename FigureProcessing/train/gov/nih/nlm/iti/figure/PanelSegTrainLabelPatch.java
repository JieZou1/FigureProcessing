package gov.nih.nlm.iti.figure;

import static org.bytedeco.javacpp.opencv_imgcodecs.*;

import org.bytedeco.javacpp.opencv_core.*;

import java.awt.Rectangle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class PanelSegTrainLabelPatch extends PanelSegTrainMethod 
{

	@Override
	public void Train(Path imageFilePath, Path resultFolder) throws Exception 
	{
		String image_file_path = imageFilePath.toString();
		
		//Load GT annotation
		String gt_xml_file = image_file_path.replaceAll(".jpg", "_data.xml");
		if (!Files.exists(Paths.get(gt_xml_file))) return;
		gt_segmentation = PanelSeg.LoadPanelSegGt(gt_xml_file);
		
		//Load original Image
		image = imread(image_file_path, CV_LOAD_IMAGE_COLOR);

		for (int i = 0; i < gt_segmentation.size(); i++)
		{
			PanelSegInfo panel = gt_segmentation.get(i);
			if (panel.panelLabel.length() > 1) continue; //We recognize single-char label only at this time.

			{	//Crop the original patch
				Mat patch = cropPatch(panel.labelRect);
			
				//Construct filename
				Path resultPatchFolder = resultFolder.resolve("patch");
				if (!Files.exists(resultPatchFolder))	Files.createDirectory(resultPatchFolder);
				resultPatchFolder = resultPatchFolder.resolve(panel.panelLabel);
				if (!Files.exists(resultPatchFolder))	Files.createDirectory(resultPatchFolder);
				String resultFilename = imageFilePath.getFileName().toString();
				int pos = resultFilename.lastIndexOf('.');
				resultFilename = resultFilename.substring(0, pos) + "." + i + ".bmp";
				Path resultPatchFile = resultPatchFolder.resolve(resultFilename);
			
				imwrite(resultPatchFile.toString(), patch);
			}
			
			{	//Normalize the patch to 32x32
				Mat patch = NormalizePatch(panel.labelRect);
				
			}
		}
	}
	
	private Mat cropPatch(Rectangle rect) 
	{
		Mat patch = image.apply(new Rect(rect.x, rect.y, rect.width, rect.height));
		return patch;
	}
	
	private Mat NormalizePatch(Rectangle rect)
	{
		
		return null;
		
	}

}
