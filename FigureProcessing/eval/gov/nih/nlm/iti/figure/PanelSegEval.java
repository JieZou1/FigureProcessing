package gov.nih.nlm.iti.figure;

import static org.bytedeco.javacpp.opencv_imgcodecs.imwrite;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.bytedeco.javacpp.opencv_core.Mat;

import com.thoughtworks.xstream.XStream;

public class PanelSegEval 
{
	public void Test(Path srcFolder, Path rstFolder, PanelSeg segMethod) throws Exception 
	{
		//Setup XStream
		XStream xStream = new XStream();
//		xStream.alias("PanelSegmentationResult", SegmentationResult.class);
//		xStream.alias("Rectangle", Rectangle.class);
		
		//String folder = "\\Users\\jie\\Openi\\Panel\\data\\dataset_sample";
		try (DirectoryStream<Path> dirStrm = Files.newDirectoryStream(srcFolder)) 
		{			
			for (Path path : dirStrm)
			{
				String filename = path.toString();
				if (!filename.endsWith(".jpg")) continue;

				System.out.println("Processing "+ filename);
				segMethod.segment(filename);
				
				//Save result in images
				Mat img_result = segMethod.getSegmentationResultInMat();
				String img_file = rstFolder.resolve(path.getFileName()).toString();
				imwrite(img_file, img_result);
				
				//Save result in xml files
				ArrayList<PanelSegResult> xml_result = segMethod.getSegmentationResult();
				String xml = xStream.toXML(xml_result);
				String xml_file = rstFolder.resolve(path.getFileName()).toString().replace(".jpg", ".xml");
				try (FileWriter fw = new FileWriter(xml_file))
				{
					fw.write(xml);
				}
				catch (IOException e) 
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} 
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("Complete!");
	}
	
	public static void main(String[] args) throws Exception 
	{
		//Check Args
		if (args.length != 3)
		{
			System.out.println("Usage: 	java -jar PanelSegEval.jar <method> <test image folder> <result image folder>");
			System.out.println("The program will read image files from <test image folder> and process them.");
			System.out.println("The results are saved into <result image folder>");
			System.out.println();
			System.out.println("CAUTION: If the <result image folder> exists, the program will delete all files in the <result image folder>");
			System.out.println();
			System.out.println("method:");
			System.out.println("		Jaylene		Jaylene's method based on cross uniform band");
			System.out.println("		Santosh		Santosh's method based on long line segments");
			System.out.println("		LabelRegMSER	MSER method for recognizing Label candidate regions");
			return;
		}

		Path src_path = Paths.get(args[1]), rst_path = Paths.get(args[2]);
		if (!Files.exists(src_path))
		{
			System.out.println(src_path + " does not exist.");
			return;
		}

		if (Files.exists(rst_path))	FileUtils.cleanDirectory(rst_path.toFile());
		else						Files.createDirectory(rst_path);
		
		String method = args[0];		PanelSeg segmentor = null;
		switch (method) 
		{
		case "Jaylene":
			segmentor = new PanelSegJaylene();			break;

		case "Santosh":
			segmentor = new PanelSegSantosh();			break;
			
		case "LabelRegMSER":
			segmentor = new PanelSegLabelRegMSER();		break;
			
		default:
			System.out.println(method + " is not known.");
			return;
		}
		
		
		PanelSegEval test = new PanelSegEval();
		test.Test(src_path, rst_path, segmentor);
	}
	
}
