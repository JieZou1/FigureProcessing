package gov.nih.nlm.iti.figure;

import static org.bytedeco.javacpp.opencv_imgcodecs.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;
import static org.bytedeco.javacpp.opencv_core.*;

import java.awt.Rectangle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

final class PanelSegTrainLabelPatch extends PanelSegTrainMethod 
{
	@Override
	public void Train(Path imageFilePath, Path resultFolder) throws Exception 
	{
		String image_file_path = imageFilePath.toString();
		
		//Load GT annotation
		String gt_xml_file = image_file_path.replaceAll(".jpg", "_data.xml");
		if (!Files.exists(Paths.get(gt_xml_file))) return;
		gtSegmentation = PanelSeg.LoadPanelSegGt(gt_xml_file);
		
		//Load original Image and generate gray, bina and inverted images.
		image = imread(image_file_path, CV_LOAD_IMAGE_COLOR);
		imageGray = new Mat();		cvtColor(image, imageGray, CV_BGR2GRAY);
		imageGrayInverted = subtract(Scalar.all(255), imageGray).asMat();
//		imwrite("image.jpg", image);
//		imwrite("imageGray.jpg", imageGray);
//		imwrite("imageGrayInverted.jpg", imageGrayInverted);
//		imwrite("imageBina.jpg", imageBina);
//		imwrite("imageBinaInverted.jpg", imageBinaInverted);

		for (int i = 0; i < gtSegmentation.size(); i++)
		{
			PanelSegInfo panel = gtSegmentation.get(i);
			if (panel.panelLabel.length() > 1) continue; //We recognize single-char label only at this time.

			{	//Crop the label patch, find the tightest boundingbox and then extend in each direction 10%.
				Mat patch 			= cropPatch(imageGray, panel.labelRect);
				Mat patchInverted 	= cropPatch(imageGrayInverted, panel.labelRect);
				
				{	//Construct filename to save the extened patch
					Path resultPatchFolder = resultFolder.resolve("ExtendPatch");	if (!Files.exists(resultPatchFolder))	Files.createDirectory(resultPatchFolder);
					resultPatchFolder = resultPatchFolder.resolve(panel.panelLabel); if (!Files.exists(resultPatchFolder))	Files.createDirectory(resultPatchFolder);
					String resultFilename = imageFilePath.getFileName().toString();
					int pos = resultFilename.lastIndexOf('.');
					
					String resultFilenameP = resultFilename.substring(0, pos) + "." + i + "p.bmp";
					Path resultPatchFileP = resultPatchFolder.resolve(resultFilenameP);
					imwrite(resultPatchFileP.toString(), patch);
					
					String resultFilenameN = resultFilename.substring(0, pos) + "." + i + "n.bmp";
					Path resultPatchFileN = resultPatchFolder.resolve(resultFilenameN);
					imwrite(resultPatchFileN.toString(), patchInverted);
					
				}
				
				//Normalize the patch to 32x32
		        Mat patchNormalized = new Mat();	resize(patch, patchNormalized, new Size(32, 32));
		        Mat patchInvertedNormalized = new Mat();	resize(patchInverted, patchInvertedNormalized, new Size(32, 32));

		        {	//Construct filename to save 32x32 patches
					Path resultPatchFolder = resultFolder.resolve("32x32");	if (!Files.exists(resultPatchFolder))	Files.createDirectory(resultPatchFolder);
					resultPatchFolder = resultPatchFolder.resolve(panel.panelLabel); if (!Files.exists(resultPatchFolder))	Files.createDirectory(resultPatchFolder);
					String resultFilename = imageFilePath.getFileName().toString();
					int pos = resultFilename.lastIndexOf('.');
					
					String resultFilenameP = resultFilename.substring(0, pos) + "." + i + "p.bmp";
					Path resultPatchFileP = resultPatchFolder.resolve(resultFilenameP);
					imwrite(resultPatchFileP.toString(), patchNormalized);
					
					String resultFilenameN = resultFilename.substring(0, pos) + "." + i + "n.bmp";
					Path resultPatchFileN = resultPatchFolder.resolve(resultFilenameN);
					imwrite(resultPatchFileN.toString(), patchInvertedNormalized);
		        }
		    }
			
			{	//Crop the original patch
				Rectangle rectangle = panel.labelRect;
				Rect rect = new Rect(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
				Mat patch = image.apply(rect);
			
				//Construct filename
				Path resultPatchFolder = resultFolder.resolve("Patch");	if (!Files.exists(resultPatchFolder))	Files.createDirectory(resultPatchFolder);
				resultPatchFolder = resultPatchFolder.resolve(panel.panelLabel); if (!Files.exists(resultPatchFolder))	Files.createDirectory(resultPatchFolder);
				String resultFilename = imageFilePath.getFileName().toString();
				int pos = resultFilename.lastIndexOf('.');
				
				resultFilename = resultFilename.substring(0, pos) + "." + i + ".bmp";
				Path resultPatchFile = resultPatchFolder.resolve(resultFilename);
				imwrite(resultPatchFile.toString(), patch);
			}
			
		}
	}
	
	private Mat cropPatch(Mat gray, Rectangle rectangle)
	{
		Mat patchGray = gray.apply(new Rect(rectangle.x, rectangle.y, rectangle.width, rectangle.height));
		Mat patchBina = new Mat();		threshold(patchGray, patchBina, 0, 255, CV_THRESH_BINARY | THRESH_OTSU);
		
		Rect boundingRect = Algorithm.findBoundingbox(patchBina);
		//Mat patchBinaTightest = patchBina.apply(boundingRect);
		
		//Make it square and then expand 10% in each direction
		int x = rectangle.x + boundingRect.x(), y = rectangle.y + boundingRect.y(), width = boundingRect.width(), height = boundingRect.height();
		if (width > height)
		{
            int height_new = width;
            int c = y + height / 2; y = c - height_new / 2; height = height_new;			
		}
		else
		{
            int width_new = height;
            int c = x + width / 2; x = c - width_new / 2; width = width_new;			
		}
		
        //Expand 10% in each direction
        x -= (int)((double)width / 10 + 0.5); width = (int)(width * 1.2 + 0.5);
        y -= (int)((double)height / 10 + 0.5); height = (int)(height * 1.2 + 0.5);
        if (x < 0) { width += x; x = 0; }
        if (y < 0) { height += y; y = 0;}
        if (x + width > image.cols()) { width = image.cols() - x; }
        if (y + height > image.rows()) { height = image.rows() - y; }
        //if (y < 0 || x < 0 || y + height > image.rows() || x + width > image.cols()) return null;

        Mat patch = gray.apply(new Rect(x, y, width, height));
        
//		imwrite("imageGray.jpg", gray);
//		imwrite("patchBina.jpg", patchBina);
//		imwrite("patchBinaTightest.jpg", patchBinaTightest);
//		imwrite("patch.jpg", patch);
//		imwrite("patchNormalized.jpg", patchNormalized);
		
		return patch;
		
	}
}












