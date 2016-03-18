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
	private String method;
	private Path srcFolder, rstFolder;
	ArrayList<Path> allPaths;
	ArrayList<PanelSegTrainMethod> methods;
	ArrayList<Boolean> flags;
	
	/**
	 * Prepare the Panel Segmentation training. 
	 * @param method	The PanelSeg method
	 * @param srcFolder	The source folder
	 * @param rstFolder	The result folder
	 */
	PanelSegTrain(String method, Path srcFolder, Path rstFolder) 
	{
		this.method = method; 	this.srcFolder = srcFolder;		this.rstFolder = rstFolder;

		allPaths = new ArrayList<Path>();		methods = new ArrayList<PanelSegTrainMethod>();

		if (method.equals("LabelPatchHoG"))
		{
			flags = new ArrayList<Boolean>();
			
			//Positive samples
			try (DirectoryStream<Path> dirStrm = Files.newDirectoryStream(this.srcFolder)) 
			{			
				for (Path path : dirStrm)
				{
					String filename = path.toString();
					if (!filename.endsWith(".bmp")) continue;
					allPaths.add(path);
					methods.add(new PanelSegTrainLabelPatchHoG());
					flags.add(true);
				}
			}
			catch (IOException e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			//Negative samples
			Path negPath = srcFolder.resolve("neg");
			try (DirectoryStream<Path> dirStrm = Files.newDirectoryStream(negPath)) 
			{			
				for (Path path : dirStrm)
				{
					String filename = path.toString();
					if (!filename.endsWith(".bmp")) continue;
					allPaths.add(path);
					methods.add(new PanelSegTrainLabelPatchHoG());
					flags.add(false);
				}
			}
			catch (IOException e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return;
		}
		
		if (method.equals("Svm2SingleVec"))
		{
			for (int i = 0 ; i < PanelSeg.labelArray.length; i++)
			{
				Path path = srcFolder.resolve("svm_model_" + PanelSeg.labelArray[i]);
				allPaths.add(path);
				methods.add(new PanelSegTrainSvm2SingleVec());
			}
			return;
		}
		
		try (DirectoryStream<Path> dirStrm = Files.newDirectoryStream(this.srcFolder)) 
		{			
			for (Path path : dirStrm)
			{
				String filename = path.toString();
				if (!filename.endsWith(".jpg")) continue;
				
				allPaths.add(path);
				switch (method) 
				{
				case "GTViz": methods.add(new PanelSegTrainGTViz());				break;
				case "LabelPatch": methods.add(new PanelSegTrainLabelPatch());		break;
				case "LabelPatchNeg": methods.add(new PanelSegTrainLabelPatchNeg());break;
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
	void trainMultiThreads(int seqThreshold)
	{
//		int level = ForkJoinPool.getCommonPoolParallelism();
//		int cores = Runtime.getRuntime().availableProcessors();

		PanelSegTrainTask task = new PanelSegTrainTask(this, 0, allPaths.size(), seqThreshold);
		task.invoke();
		
		if (method.equals("LabelPatchHoG")) 
			PanelSegTrainLabelPatchHoG.generateLabelPatchHoGTrain(flags, methods, rstFolder.resolve("train.txt").toString());
		else if (method.equals("Svm2SingleVec"))
			PanelSegTrainSvm2SingleVec.generateSingleVec(methods, rstFolder.resolve("PanelSegLabelRegHoGModels.java").toString());
		
		System.out.println("Processing Completed!");
	}
	
	/**
	 * Do segmentation in a single thread.  Not very useful.
	 * Use large seqThreshold value, in segMultiThreads, can accomplish segSingleThread
	 * @throws Exception 
	 */
	void trainSingleThread() throws Exception 
	{		
		for (int i = 0; i < allPaths.size(); i++)
		//for (int i = 0; i < 1; i++)
		{
			train(i);
		}
		
		if (method.equals("LabelPatchHoG")) 
			PanelSegTrainLabelPatchHoG.generateLabelPatchHoGTrain(flags, methods, rstFolder.resolve("train.txt").toString());
		else if (method.equals("Svm2SingleVec"))
			PanelSegTrainSvm2SingleVec.generateSingleVec(methods, "PanelSegLabelRegHoGModels.java");
		
		System.out.println("Processing Completed!");
	}

	public static void main(String[] args) throws Exception 
	{
		//Check Args
		if (args.length != 3)
		{
			System.out.println("Usage: 	java -jar PanelSegTrain.jar <method> <source folder> <result folder>");
			System.out.println("The program will read image and annotations from <source  folder> and generate some results to <result folder>.");
			System.out.println();
			System.out.println("CAUTION: If the <result folder> exists, the program will delete all files in the <result image folder>");
			System.out.println();
			System.out.println("method:");
			System.out.println("	GTViz		Generate Ground Truth Visualization, i.e., superimpose annotations on the original figure images");
			System.out.println("	LabelPatch	Crop the label patches and normalized them for training");
			System.out.println("	LabelPatchNeg	Randomly generate some negative patches for label recognition");
			System.out.println("	LabelPatchHoG	Compute HoG features for label detection");
			System.out.println("	Svm2SingleVec	Convert Linear SVM model to Single Vector");
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
		case "LabelPatchNeg": break;
		case "LabelPatchHoG": break;
		case "Svm2SingleVec": break;
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
