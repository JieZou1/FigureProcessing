package gov.nih.nlm.iti.figure;

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
	String method;
	Path srcFolder, rstFolder;
	ArrayList<Path> allPaths;
	ArrayList<PanelSegTrainMethod> methods;
	
	ArrayList<Boolean> flags; //Used for indicating training/test samples, ...
	
	/**
	 * Prepare the Panel Segmentation training. 
	 * @param method	The PanelSeg method
	 * @param srcFolder	The source folder
	 * @param rstFolder	The result folder
	 */
	PanelSegTrain(String method, Path srcFolder, Path rstFolder) 
	{
		this.method = method; 	
		this.srcFolder = srcFolder;		
		this.rstFolder = rstFolder;

		allPaths = new ArrayList<Path>();		
		methods = new ArrayList<PanelSegTrainMethod>();
	}
	
	void train(int i) throws Exception
	{
		Path path = allPaths.get(i);		PanelSegTrainMethod method = methods.get(i);
		
		String filename = path.toString();
//		if (!filename.endsWith("1471-2326-3-3-1.jpg") &&
//				!filename.endsWith("1477-7827-6-39-3.jpg")	&&
//				!filename.endsWith("1471-2210-10-14-11.jpg")	&&
//				!filename.endsWith("1471-2474-4-20-3.jpg")	&&
//				!filename.endsWith("1472-6890-9-4-1.jpg")	&&
//				!filename.endsWith("1477-7827-6-16-5.jpg")	&&
//				!filename.endsWith("1741-7007-7-28-6.jpg"))
//			return;
		System.out.println("Processing "+ i + " "  + filename);
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
		
		if (method.equals("LabelHoG")) 
			PanelSegTrainLabelHoG.generateLabelPatchHoGTrain(flags, methods, rstFolder.resolve("train.txt").toString());
		else if (method.equals("Svm2SingleVec"))
			PanelSegTrainSvm2SingleVec.generateSingleVec(methods, "PanelSegLabelRegHoGModels.java");
		
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
		
		if (method.equals("LabelHoG")) 
			PanelSegTrainLabelHoG.generateLabelPatchHoGTrain(flags, methods, rstFolder.resolve("train.txt").toString());
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
			System.out.println("GTViz	Generate Ground Truth Visualization, i.e., superimpose annotations on the original figure images");
			System.out.println("LabelCrop	Crop the label patches and normalized them for training");
			System.out.println("LabelNeg	Randomly generate some negative patches for label recognition");
			System.out.println("LabelHoG	Compute HoG features for label detection");
			System.out.println("Svm2SingleVec	Convert Linear SVM model to Single Vector");
			System.out.println("LabelBootStrap	BootStrap Neg Patchs for HoG method of Label Recongition");
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
		
		String method = args[0]; PanelSegTrain train = null;
		switch (method) 
		{
		case "GTViz": 
			train = PanelSegTrainGTViz.createPanelSegTrain(method, src_path, rst_path);
			break;
		case "LabelCrop": 
			train = PanelSegTrainLabelCrop.createPanelSegTrain(method, src_path, rst_path);
			break;
		case "LabelNeg": 
			train = PanelSegTrainLabelNeg.createPanelSegTrain(method, src_path, rst_path);
			break;
		case "LabelHoG":
			train = PanelSegTrainLabelHoG.createPanelSegTrain(method, src_path, rst_path);
			break;
		case "Svm2SingleVec": 
			train = PanelSegTrainSvm2SingleVec.createPanelSegTrain(method, src_path, rst_path);
			break;
		case "LabelBootStrap": 
			train = PanelSegTrainLabelBootStrap.createPanelSegTrain(method, src_path, rst_path);
			break;
		default:
			System.out.println(method + " is not known.");
			return;
		}
		
		//Do Training
		train.trainSingleThread();
		//train.trainMultiThreads(10);
		
	}	

}
