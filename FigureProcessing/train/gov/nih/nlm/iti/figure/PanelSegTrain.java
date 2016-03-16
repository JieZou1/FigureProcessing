package gov.nih.nlm.iti.figure;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;

/**
 * Training routines for Panel Segmentation. 
 * 
 * @author Jie Zou
 *
 */
class PanelSegTrain 
{
	private Path srcFolder, rstFolder;
	ArrayList<Path> allPaths;
	ArrayList<PanelSegTrainMethod> methods;
	
	/**
	 * Prepare the Panel Segmentation evaluation. 
	 * @param method	The PanelSeg method
	 * @param srcFolder	The source folder
	 * @param rstFolder	The result folder
	 */
	PanelSegTrain(String method, Path srcFolder, Path rstFolder) 
	{
		this.srcFolder = srcFolder;		this.rstFolder = rstFolder;

		allPaths = new ArrayList<Path>();		methods = new ArrayList<PanelSegTrainMethod>();
		try (DirectoryStream<Path> dirStrm = Files.newDirectoryStream(this.srcFolder)) 
		{			
			for (Path path : dirStrm)
			{
				String filename = path.toString();
				if (!filename.endsWith(".jpg")) continue;
				
				allPaths.add(path);
				switch (method) 
				{
				case "GTViz": methods.add(new PanelSegTrainGTViz());			break;
				case "LabelPatch": methods.add(new PanelSegTrainLabelPatch());			break;
				}
			}
		}
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	void train(int i) throws Exception
	{
		Path path = allPaths.get(i);		PanelSegTrainMethod method = methods.get(i);
		
		String filename = path.toString();
		//if (!filename.endsWith("1471-2105-10-368-7.jpg")) return;
		System.out.println("Processing "+ filename);
		method.Train(path, rstFolder);
	}
	
	/**
	 * Do Training in multi-threads. Use seqThreshold to control how many tasks would be processed sequentially.
	 * The smaller the seqThreshold, the more tasks are parallel processed.
	 * @param seqThreshold
	 */
	public void trainMultiThreads(int seqThreshold)
	{
//		int level = ForkJoinPool.getCommonPoolParallelism();
//		int cores = Runtime.getRuntime().availableProcessors();

		PanelSegTrainTask task = new PanelSegTrainTask(this, 0, allPaths.size(), seqThreshold);
		task.invoke();
		System.out.println("Processing Completed!");
	}
	
	/**
	 * Do segmentation in a single thread.  Not very useful.
	 * Use large seqThreshold value, in segMultiThreads, can accomplish segSingleThread
	 * @throws Exception 
	 */
	public void trainSingleThread() throws Exception 
	{		
		for (int i = 0; i < allPaths.size(); i++)
		//for (int i = 0; i < 1; i++)
		{
			train(i);
		}
				
		System.out.println("Processing Completed!");
	}

	
	public static void main(String[] args) throws Exception 
	{
		//Check Args
		if (args.length != 3)
		{
			System.out.println("Usage: 	java -jar PanelSegTrain.jar <method> <Ground Truth folder> <result folder>");
			System.out.println("The program will read image and annotations from <ground-truth  folder> and generate some results to <result folder>.");
			System.out.println();
			System.out.println("CAUTION: If the <result folder> exists, the program will delete all files in the <result image folder>");
			System.out.println();
			System.out.println("method:");
			System.out.println("	GTViz		Generate Ground Truth Visualization, i.e., superimpose annotations on the original figure images");
			System.out.println("	LabelPatch	Crop the label patches and normalized them for training");
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
		case "GTViz": break;
		case "LabelPatch": break;
		default:
			System.out.println(method + " is not known.");
			return;
		}
		
		//Do Training
		PanelSegTrain train = new PanelSegTrain(method, src_path, rst_path);
		train.trainSingleThread();
		//train.trainMultiThreads(10);
		
	}	

}
