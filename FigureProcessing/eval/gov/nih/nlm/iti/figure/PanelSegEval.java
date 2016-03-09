package gov.nih.nlm.iti.figure;

import static org.bytedeco.javacpp.opencv_imgcodecs.imwrite;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.*;

import org.apache.commons.io.FileUtils;
import org.bytedeco.javacpp.opencv_core.Mat;

import com.thoughtworks.xstream.XStream;

/**
 * Implementing multi-threading processing in Fork/Join framework
 * 
 * It takes a PanelSegEval, and uses divide-and-conquer strategy to run multi-tasks in commonPool
 * 
 * @author Jie Zou
 *
 */
class PanelSegTask extends RecursiveAction
{
	private static final long serialVersionUID = 1L;

	int seqThreshold;
	
	PanelSegEval segEval;	int start, end;
	
	PanelSegTask(PanelSegEval segEval, int start, int end, int seqThreshold) 
	{
		this.segEval = segEval;		this.start = start;		this.end = end; this.seqThreshold = seqThreshold;
	}
	
	@Override
	protected void compute()
	{
		if (end - start < seqThreshold)
		{
			for (int i = start; i < end; i++)
			{
				Path path = segEval.allPaths.get(i);
				PanelSeg segmentor = segEval.segmentors.get(i);
				try 
				{
					segEval.segment(segmentor, path);
				} catch (Exception e) 
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		else
		{
			int middle = (start + end)/2;
			invokeAll(	new PanelSegTask(segEval, start, middle, this.seqThreshold), 
						new PanelSegTask(segEval, middle, end, this.seqThreshold));
		}
	}
}

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
	PanelSegEval(String method, Path srcFolder, Path rstFolder) 
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
	
	/**
	 * Panel Segmentation for a figure
	 * @param path The figure file to be segmented.
	 */
	void segment(PanelSeg segmentor, Path path) 
	{
		String filename = path.toString();
		System.out.println("Processing "+ filename);
		segmentor.segment(filename);

		//Save result in images
		Mat img_result = segmentor.getSegmentationResultInMat();
		String img_file = rstFolder.resolve(path.getFileName()).toString();
		imwrite(img_file, img_result);
		
		//Save result in xml files
		ArrayList<PanelSegResult> xml_result = segmentor.getSegmentationResult();
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
	 * Do evaluation in multi-threads. Use seqThreshold to control how many tasks would be processed sequentially.
	 * The smaller the seqThreshold, the more tasks are parallel processed.
	 * @param seqThreshold
	 */
	public void evalMultiThreads(int seqThreshold)
	{
//		int level = ForkJoinPool.getCommonPoolParallelism();
//		int cores = Runtime.getRuntime().availableProcessors();

		PanelSegTask task = new PanelSegTask(this, 0, allPaths.size(), seqThreshold);
		task.invoke();
		System.out.println("Processing Completed!");
		
	}
	
	/**
	 * Do evaluation in a single thread.  Not very useful.
	 * Use large seqThreshold value, in evalMultiThreads, can accomplish evalSingleThread
	 */
	public void evalSingleThread() 
	{		
		for (int i = 0; i < allPaths.size(); i++)
		{
			Path path = allPaths.get(i);
			PanelSeg segmentor = segmentors.get(i);
			segment(segmentor, path);
		}
				
		System.out.println("Processing Completed!");
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
			System.out.println("	Jaylene		Jaylene's method based on cross uniform band");
			System.out.println("	Santosh		Santosh's method based on long line segments");
			System.out.println("	LabelRegMSER	MSER method for recognizing Label candidate regions");
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
		
		String method = args[0];
		switch (method) 
		{
		case "Jaylene": break;
		case "Santosh": break;
		case "LabelRegMSER": break;
		default:
			System.out.println(method + " is not known.");
			return;
		}
		
		PanelSegEval eval = new PanelSegEval(method, src_path, rst_path);
		//eval.evalSingleThread();
		eval.evalMultiThreads(10);
	}
	
}
