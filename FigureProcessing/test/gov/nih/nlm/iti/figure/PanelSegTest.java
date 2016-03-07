package gov.nih.nlm.iti.figure;

import static org.bytedeco.javacpp.opencv_imgcodecs.imwrite;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.bytedeco.javacpp.opencv_core.Mat;

import com.thoughtworks.xstream.XStream;

public class PanelSegTest 
{
	public void Test(String srcFolder, String rstFolder, PanelSeg segMethod) throws Exception 
	{
		//Setup XStream
		XStream xStream = new XStream();
//		xStream.alias("PanelSegmentationResult", SegmentationResult.class);
//		xStream.alias("Rectangle", Rectangle.class);
		
		//String folder = "\\Users\\jie\\Openi\\Panel\\data\\dataset_sample";
		try (DirectoryStream<Path> dirStrm = Files.newDirectoryStream(Paths.get(srcFolder))) 
		{			
			for (Path path : dirStrm)
			{
				String filename = path.toString();
				if (!filename.endsWith(".jpg")) continue;

				System.out.println("Processing "+ filename);
				segMethod.segment(filename);
				
				//Save result in images
				Mat img_result = segMethod.getSegmentationResultInMat();
				String img_file = Paths.get(rstFolder).resolve(path.getFileName()).toString();
				imwrite(img_file, img_result);
				
				//Save result in xml files
				ArrayList<PanelSegResult> xml_result = segMethod.getSegmentationResult();
				String xml = xStream.toXML(xml_result);
				String xml_file = Paths.get(rstFolder).resolve(path.getFileName()).toString().replace(".jpg", ".xml");
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
}
