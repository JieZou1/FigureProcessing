package gov.nih.nlm.iti.figure;

import static org.bytedeco.javacpp.opencv_imgcodecs.imwrite;

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

/**
 * Panel Segmentation Evaluation
 * 
 * @author Jie Zou
 * 
 */
public class PanelSegEval 
{
	private Path srcFolder, rstFolder;
	ArrayList<Path> allPaths;
	ArrayList<PanelSeg> segmentors;
	XStream xStream;
	
	/**
	 * Prepare the Panel Segmentation evaluation. 
	 * @param method	The PanelSeg method
	 * @param srcFolder	The source folder
	 * @param rstFolder	The result folder
	 */
	PanelSegEval(String method, Path srcFolder, Path rstFolder, Path evaluationFile) 
	{
		this.srcFolder = srcFolder;		this.rstFolder = rstFolder;

		allPaths = new ArrayList<Path>();		segmentors = new ArrayList<PanelSeg>();
		try (DirectoryStream<Path> dirStrm = Files.newDirectoryStream(this.srcFolder)) 
		{			
			for (Path path : dirStrm)
			{
				String filename = path.toString();
				if (!filename.endsWith(".jpg")) continue;
				
				allPaths.add(path);
				switch (method) 
				{
				case "Jaylene": segmentors.add(new PanelSegJaylene());			break;
				case "Santosh": segmentors.add(new PanelSegSantosh());			break;
				case "LabelRegMSER": segmentors.add(new PanelSegLabelRegMSER());break;
				case "LabelRegHoG": segmentors.add(new PanelSegLabelRegHoG());break;
				}
			}
		}
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		xStream = new XStream();
//		xStream.alias("PanelSegmentationResult", SegmentationResult.class);
//		xStream.alias("Rectangle", Rectangle.class);		
	}
	
	void loadPanelSegResult() throws Exception
	{
		ArrayList<String> allXMLPaths = new ArrayList<String>();
		try (DirectoryStream<Path> dirStrm = Files.newDirectoryStream(rstFolder)) 
		{			
			for (Path path : dirStrm)
			{
				String filename = path.toString();
				if (!filename.endsWith(".xml")) continue;
				allXMLPaths.add(filename);
			}
		}
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for (int i = 0; i < allXMLPaths.size(); i++)
		{
			PanelSeg.loadPanelSegResult(allXMLPaths.get(i));
		}
	}
	
	ArrayList<ArrayList<PanelSegInfo>> LoadPanelSegGt() throws Exception
	{
		ArrayList<String> allXMLPaths = new ArrayList<String>();
		try (DirectoryStream<Path> dirStrm = Files.newDirectoryStream(this.srcFolder)) 
		{			
			for (Path path : dirStrm)
			{
				String filename = path.toString();
				if (!filename.endsWith(".xml")) continue;
				allXMLPaths.add(filename);
			}
		}
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ArrayList<ArrayList<PanelSegInfo>> gtPanels = new ArrayList<ArrayList<PanelSegInfo>>();		
		for (int i = 0; i < allXMLPaths.size(); i++)
		{
			ArrayList<PanelSegInfo> panels = PanelSeg.LoadPanelSegGt(allXMLPaths.get(i));
			gtPanels.add(panels);
		}
		return gtPanels;
	}
	
	/**
	 * Panel Segmentation for a figure
	 * @param path The figure file to be segmented.
	 */
	void segment(int i) 
	{
		Path path = allPaths.get(i);		PanelSeg segmentor = segmentors.get(i);
		
		// 1. Do Segmentation
		String filename = path.toString();
		System.out.println("Processing "+ filename);
		segmentor.segment(filename);

		// 2.1 Save result in images
		Mat img_result = segmentor.getSegmentationResultInMat();
		String img_file = rstFolder.resolve(path.getFileName()).toString();
		imwrite(img_file, img_result);
		
		// 2.2 Save result in xml files
		ArrayList<PanelSegInfo> xml_result = segmentor.getSegmentationResult();
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
	
	/**
	 * Do segmentation in multi-threads. Use seqThreshold to control how many tasks would be processed sequentially.
	 * The smaller the seqThreshold, the more tasks are parallel processed.
	 * @param seqThreshold
	 */
	public void segMultiThreads(int seqThreshold)
	{
//		int level = ForkJoinPool.getCommonPoolParallelism();
//		int cores = Runtime.getRuntime().availableProcessors();

		PanelSegEvalTask task = new PanelSegEvalTask(this, 0, allPaths.size(), seqThreshold);
		task.invoke();
		System.out.println("Processing Completed!");
	}
	
	/**
	 * Do segmentation in a single thread.  Not very useful.
	 * Use large seqThreshold value, in segMultiThreads, can accomplish segSingleThread
	 */
	public void segSingleThread() 
	{		
		for (int i = 0; i < allPaths.size(); i++)
		//for (int i = 0; i < 1; i++)
		{
			segment(i);
		}
				
		System.out.println("Processing Completed!");
	}
	
	/**
	 * Panel Segmentation Evaluation Main Function
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception 
	{
		//Check Args
		if (args.length != 4)
		{
			System.out.println("Usage: 	java -jar PanelSegEval.jar <method> <test image folder> <result image folder> <evaluation file>");
			System.out.println("The program will read image files from <test image folder> and process them.");
			System.out.println("The segmentation results are saved into <result image folder>");
			System.out.println("The evaluation is saved into <evaluation file>");
			System.out.println();
			System.out.println("CAUTION: If the <result image folder> exists, the program will delete all files in the <result image folder>");
			System.out.println();
			System.out.println("method:");
			System.out.println("Jaylene		Jaylene's method based on cross uniform band");
			System.out.println("Santosh		Santosh's method based on long line segments");
			System.out.println("LabelRegMSER	MSER method for recognizing Label candidate regions");
			System.out.println("LabelRegHoG	HoG method for recognizing Label candidate regions");
			return;
		}
		
		Path src_path = Paths.get(args[1]), rst_path = Paths.get(args[2]), evaluation_file = Paths.get(args[3]);
		if (!Files.exists(src_path))
		{
			System.out.println(src_path + " does not exist.");
			return;
		}

		if (Files.exists(rst_path))	FileUtils.cleanDirectory(rst_path.toFile());
		else						Files.createDirectory(rst_path);
		
		String method = args[0];
		switch (method) 
		{
		case "Jaylene": break;
		case "Santosh": break;
		case "LabelRegMSER": break;
		case "LabelRegHoG": break;
		default:
			System.out.println(method + " is not known.");
			return;
		}
		
		//Do segmentation
		PanelSegEval eval = new PanelSegEval(method, src_path, rst_path, evaluation_file);
		//eval.segSingleThread();
		eval.segMultiThreads(10);
		
		//Do Evaluation
		//eval.LoadPanelSegGt();
		//eval.loadPanelSegResult();
		
	}
	
}
