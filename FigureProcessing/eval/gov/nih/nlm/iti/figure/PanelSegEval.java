package gov.nih.nlm.iti.figure;

import static org.bytedeco.javacpp.opencv_imgcodecs.imwrite;
import static org.bytedeco.javacpp.opencv_imgproc.resize;

import java.awt.Rectangle;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.stream.IntStream;

import org.apache.commons.io.FileUtils;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Size;

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
	private ArrayList<Path> allPaths;
	private ArrayList<PanelSeg> segmentors;
	private XStream xStream;

	private long startTime, endTime;
	
	private ArrayList<String> autoXMLPaths; ArrayList<ArrayList<PanelSegInfo>> autoPanels; //Automatic segmentation files and results
	private ArrayList<String> gtXMLPaths; ArrayList<ArrayList<PanelSegInfo>> gtPanels; //Ground truth data
	private ArrayList<String> matchedIDs; ArrayList<ArrayList<PanelSegInfo>> matchedAutoPanels; ArrayList<ArrayList<PanelSegInfo>> matchedGtPanels; //Matched set
	private Path  evaluationFile;
	
	/**
	 * Prepare the Panel Segmentation evaluation. 
	 * @param method	The PanelSeg method
	 * @param srcFolder	The source folder
	 * @param rstFolder	The result folder
	 */
	PanelSegEval(String method, Path srcFolder, Path rstFolder, Path evaluationFile) 
	{
		this.srcFolder = srcFolder;		this.rstFolder = rstFolder; this.evaluationFile = evaluationFile;

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
				case "LabelRegHoGSvm": segmentors.add(new PanelSegLabelRegHoGSvm());break;
				case "LabelRegHoGSvmBeam": segmentors.add(new PanelSegLabelRegHoGSvmBeam());break;
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
	 * Load Panel Segmentation results from rstFolder, and saved to autoPanels. 
	 * All XML files where the segmentation results are loaded from is saved in autoXMLPaths.
	 * @throws Exception
	 */
	void loadPanelSegResult() throws Exception
	{
		autoXMLPaths = new ArrayList<String>();
		try (DirectoryStream<Path> dirStrm = Files.newDirectoryStream(rstFolder)) 
		{			
			for (Path path : dirStrm)
			{
				String filename = path.toString();
				if (!filename.endsWith(".xml")) continue;
				autoXMLPaths.add(filename);
			}
		}
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		autoPanels = new ArrayList<ArrayList<PanelSegInfo>>();
		for (int i = 0; i < autoXMLPaths.size(); i++)
		{
			autoPanels.add(PanelSeg.loadPanelSegResult(autoXMLPaths.get(i)));
		}
	}
	
	/**
	 * Load Segmentation ground truth from srcFolder, and saved to gtPanels. 
	 * All XML files where the ground truth are loaded from is saved in gtXMLPaths.
	 * @throws Exception
	 */
	void LoadPanelSegGt() throws Exception
	{
		gtXMLPaths = new ArrayList<String>();
		try (DirectoryStream<Path> dirStrm = Files.newDirectoryStream(this.srcFolder)) 
		{			
			for (Path path : dirStrm)
			{
				String filename = path.toString();
				if (!filename.endsWith(".xml")) continue;
				gtXMLPaths.add(filename);
			}
		}
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		gtPanels = new ArrayList<ArrayList<PanelSegInfo>>();		
		for (int i = 0; i < gtXMLPaths.size(); i++)
		{
			ArrayList<PanelSegInfo> panels = PanelSeg.LoadPanelSegGt(gtXMLPaths.get(i));
			gtPanels.add(panels);
		}
	}
	
	/**
	 * Compare gtPanels and autoPanels to generate evaluation result and save to evaluationFile.
	 * Notice that the number of gtPanels and autoPanels may not be the same, and may not be in the same order. 
	 * So, we need to search gtXMLPaths and autoXMLPaths to match. 
	 * The matched results are saved in matchedIDs, matchedAutoPanels, and matchedGtPanels. 
	 * The comparison is actually conducted in matchedAutoPanels and matchedGtPanels.
	 * @throws Exception 
	 */
	void Evaluate() throws Exception
	{
		//Load Ground Truth and Auto Segmentation results from XML files in srcFolder and rstFolder
		LoadPanelSegGt();
		loadPanelSegResult();
		
		//Match auto and ground truth image samples (ID's, bascailly their filenames)
		matchAutoGt();

		//Evaluate, save the result to evaluationFile
		EvaluateLabelRecog();
	}
	
	/**
	 * Evaluate Panel Label Recognition. 
	 * The precision and recall of Each individual Panel Label Character are calculated. 
	 * The overall precision and recall are also calculated. 
	 * NOTE: it is case insensitive, i.e., 'A' and 'a' are merged as one entry of 'a'. 
	 */
	private void EvaluateLabelRecog()
	{
		char 	lastChar = Character.toLowerCase(PanelSeg.labelToDetect[PanelSeg.labelToDetect.length-1]), 
				firstChar = Character.toLowerCase(PanelSeg.labelToDetect[0]);
		int n = lastChar - firstChar + 1;
		
		int[] countIndividualLabelGT = new int[n];
		int[] countIndividualLabelAuto = new int[n];
		int[] countIndividualLabelCorrect = new int[n];
		ArrayList<ArrayList<String>> autoLabels = new ArrayList<ArrayList<String>>();
		ArrayList<ArrayList<String>> gtLabels = new ArrayList<ArrayList<String>>();
		ArrayList<ArrayList<String>> missingLabels = new ArrayList<ArrayList<String>>();
		ArrayList<ArrayList<String>> falseAlarmLabels = new ArrayList<ArrayList<String>>();
		
		for (int i = 0; i < matchedIDs.size(); i++)
		{
			//count Auto results
			ArrayList<PanelSegInfo> auto = matchedAutoPanels.get(i);
			ArrayList<String> autoLabel = new ArrayList<String>();
			for (int j = 0; j < auto.size(); j++)
			{
				PanelSegInfo panel = auto.get(j);
				char ch = Character.toLowerCase(panel.panelLabel.charAt(0));			if (ch > lastChar) continue;
				autoLabel.add(""+ch);
				int labelArrayIndex = ch - firstChar;
				countIndividualLabelAuto[labelArrayIndex]++;
			}
			autoLabels.add(autoLabel);

			//count GT results
			ArrayList<PanelSegInfo> gt = matchedGtPanels.get(i);
			ArrayList<String> gtLabel = new ArrayList<String>();
			for (int j = 0; j < gt.size(); j++)
			{
				PanelSegInfo panel = gt.get(j);
				if (panel.labelRect == null) continue; //In this panel of GT data, there is no label.
				
				char ch = Character.toLowerCase(panel.panelLabel.charAt(0));			if (ch > lastChar) continue;
				gtLabel.add(""+ch);
				int labelArrayIndex = ch - firstChar;
				countIndividualLabelGT[labelArrayIndex]++;
			}
			gtLabels.add(gtLabel);
			
			//Match Auto to GT to find false alarms, 
			ArrayList<String> falseAlarmLabel = new ArrayList<String>();
			for (int j = 0; j < auto.size(); j++)
			{
				PanelSegInfo autoPanel = auto.get(j); boolean found = false;
				char chAuto = Character.toLowerCase(autoPanel.panelLabel.charAt(0));
				for (int k = 0; k < gt.size(); k++)
				{
					PanelSegInfo gtPanel = gt.get(k);
					if (gtPanel.labelRect == null) continue; //In this panel of GT data, there is no label.
					
					char chGt = Character.toLowerCase(gtPanel.panelLabel.charAt(0));	//if (chGt > lastChar) continue;
					Rectangle intersect = gtPanel.labelRect.intersection(autoPanel.labelRect);
					double area_intersect = intersect.isEmpty() ? 0 : intersect.width * intersect.height;
					double area_gt = gtPanel.labelRect.width * gtPanel.labelRect.height;
					double area_auto = autoPanel.labelRect.width * autoPanel.labelRect.height;
					if (chAuto == chGt && area_intersect > area_gt / 2 && area_intersect > area_auto / 2)  //Label matches and also the intersection to gt is at least half of gt region and half of itself.
					{
						found = true; break;
					}
				}
				if (found)
				{
					int labelArrayIndex = chAuto - firstChar;
					countIndividualLabelCorrect[labelArrayIndex]++;
				}
				else falseAlarmLabel.add("" + chAuto);
			}
			falseAlarmLabels.add(falseAlarmLabel);
			
			//Match GT to Auto to find missing 
			ArrayList<String> missingLabel = new ArrayList<String>();
			for (int j = 0; j < gt.size(); j++)
			{
				PanelSegInfo gtPanel = gt.get(j); boolean found = false;
				if (gtPanel.labelRect == null) continue; //In this panel of GT data, there is no label.
				
				char chGt = Character.toLowerCase(gtPanel.panelLabel.charAt(0));
				for (int k = 0; k < auto.size(); k++)
				{
					PanelSegInfo autoPanel = auto.get(k);
					
					char chAuto = Character.toLowerCase(autoPanel.panelLabel.charAt(0));	//if (chGt > lastChar) continue;
					Rectangle intersect = autoPanel.labelRect.intersection(gtPanel.labelRect);
					double area_intersect = intersect.isEmpty() ? 0 : intersect.width * intersect.height;
					double area_gt = gtPanel.labelRect.width * gtPanel.labelRect.height;
					double area_auto = autoPanel.labelRect.width * autoPanel.labelRect.height;
					if (chAuto == chGt && area_intersect > area_gt / 2 && area_intersect > area_auto / 2)  //Label matches and also the intersection to gt is at least half of gt region and half of itself.
					{
						found = true; break;
					}
				}
				if (found)
				{
//					int labelArrayIndex = chGt - firstChar;
//					countIndividualLabelCorrect[labelArrayIndex]++;
				}
				else missingLabel.add("" + chGt);
			}
			missingLabels.add(missingLabel);
		}
		
		int countTotalLabelsGT = IntStream.of(countIndividualLabelGT).sum();
		int countTotalLabelsAuto = IntStream.of(countIndividualLabelAuto).sum();
		int countTotalLabelsCorrect = IntStream.of(countIndividualLabelCorrect).sum();

		try (PrintWriter pw = new PrintWriter(evaluationFile.toString()))
    	{
			float precision, recall; int countGT, countAuto, countCorrect; String item;
			
    		pw.println("Total images processed: " + allPaths.size());
    		pw.println("Total processing time: " + (endTime - startTime)/1000.0 + " secondes.");
    		pw.println("Average processing time: " + ((endTime - startTime)/1000.0)/allPaths.size() + " secondes.");

    		pw.println();
    		pw.println("Item\tGT\tAuto\tCorrect\tPrecision\tRecall");
    		
    		item = "Total";
    		countGT = countTotalLabelsGT; countAuto = countTotalLabelsAuto; countCorrect = countTotalLabelsCorrect;
    		precision = (float)countCorrect / countAuto; precision = (float) (((int)(precision*1000+0.5))/10.0);
    		recall = (float)countCorrect / countGT; recall = (float) (((int)(recall*1000+0.5))/10.0);
    		pw.println(item + "\t" + countGT + "\t" + countAuto + "\t" + countCorrect + "\t" + precision + "\t" + recall);
    		
    		pw.println();

    		for (int i = 0; i < countIndividualLabelGT.length; i++)
    		{
    			item = "" + (char)(PanelSeg.labelToDetect[0] + i);
        		countGT = countIndividualLabelGT[i]; countAuto = countIndividualLabelAuto[i]; countCorrect = countIndividualLabelCorrect[i];
        		precision = (float)countCorrect / countAuto; precision = (float) (((int)(precision*1000+0.5))/10.0);
        		recall = (float)countCorrect / countGT; recall = (float) (((int)(recall*1000+0.5))/10.0);
        		pw.println(item + "\t" + countGT + "\t" + countAuto + "\t" + countCorrect + "\t" + precision + "\t" + recall);
    		}

    		pw.println();
    		
    		//Missing Labels:
    		int totalMissing = 0, totalFalseAlarm = 0;
    		for (int i = 0; i < missingLabels.size(); i++) totalMissing += missingLabels.get(i).size(); 
    		for (int i = 0; i < falseAlarmLabels.size(); i++) totalFalseAlarm += falseAlarmLabels.get(i).size(); 
    		pw.println("Total Missing: " + totalMissing);
    		pw.println("Total False Alarm: " + totalFalseAlarm);
    		for (int i = 0; i < missingLabels.size(); i++)
    		{
    			pw.println(matchedIDs.get(i));
    			pw.print("\t" + "GT Labels:\t");	for (int k = 0; k < gtLabels.get(i).size(); k++) pw.print(gtLabels.get(i).get(k) + " "); pw.println();
    			pw.print("\t" + "Auto Labels:\t");	for (int k = 0; k < autoLabels.get(i).size(); k++) pw.print(autoLabels.get(i).get(k) + " "); pw.println();
    			pw.print("\t" + "Missing Labels:\t");	for (int k = 0; k < missingLabels.get(i).size(); k++) pw.print(missingLabels.get(i).get(k) + " "); pw.println();
    			pw.print("\t" + "False Alarm Labels:\t");	for (int k = 0; k < falseAlarmLabels.get(i).size(); k++) pw.print(falseAlarmLabels.get(i).get(k) + " "); pw.println();
    		}
    		
    	} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	/**
	 * Match have auto segmentation and gt segmentation pairs. 
	 * Results are save in matchedIDs, matchedAutoPanels, and matchedGtPanels.
	 */
	private void matchAutoGt() 
	{
		//keep only filenames from autoXMLPaths and gtXMLPaths
		for (int i = 0; i < autoXMLPaths.size(); i++)
		{
			String path = autoXMLPaths.get(i);
			int start = path.lastIndexOf('\\') + 1, end = path.lastIndexOf(".xml");
			String filename = path.substring(start, end);
			autoXMLPaths.set(i, filename);
		}
		for (int i = 0; i < gtXMLPaths.size(); i++)
		{
			String path = gtXMLPaths.get(i);
			int start = path.lastIndexOf('\\') + 1, end = path.lastIndexOf("_data.xml");
			String filename = path.substring(start, end);
			gtXMLPaths.set(i, filename);
		}
		//Match gtXMLPaths and autoXMLPaths
		matchedIDs = new ArrayList<>();
		matchedAutoPanels = new ArrayList<ArrayList<PanelSegInfo>>();
		matchedGtPanels = new ArrayList<ArrayList<PanelSegInfo>>();
		
		for (int i = 0; i < autoXMLPaths.size(); i++)
		{
			String autoID = autoXMLPaths.get(i); int index = -1;
			for (int j = 0; j < gtXMLPaths.size(); j++)
			{
				String gtID = gtXMLPaths.get(j);
				if (autoID.equals(gtID)) {index = j; break;}
			}
			if (index >= 0)
			{
				matchedIDs.add(autoID);
				matchedAutoPanels.add(autoPanels.get(i));
				matchedGtPanels.add(gtPanels.get(index));
			}
		}		
	}
	
	/**
	 * Panel Segmentation for a figure
	 * @param i The index of the figure file to be segmented.
	 * @throws IOException 
	 */
	void segment(int i) throws IOException 
	{
		Path path = allPaths.get(i);		PanelSeg segmentor = segmentors.get(i);
		
		String filename = path.toString();
//		if (!filename.endsWith("1465-9921-11-35-5.jpg"))
//			return;
		
		System.out.println("Processing "+ i + " "  + filename);
		segmentor.segment(filename);
	
		//synchronized(segmentors)
		{
			//Save detected patches
			for (int k = 0; k < segmentor.figure.segmentationResultIndividualLabel.size(); k++)
			{
				ArrayList<PanelSegInfo> segmentationResult = segmentor.figure.segmentationResultIndividualLabel.get(k);
				if (segmentationResult == null) continue;
				
				for (int j = 0; j < segmentationResult.size(); j++)
				{
					if (j == 2) break; //We just save the top patches for training, in order to avoiding collecting a very large negative training set at the beginning.
					
					PanelSegInfo segInfo = segmentationResult.get(j);
					Rectangle rectangle = segInfo.labelRect;
					
					Mat patch = segInfo.labelInverted ? AlgorithmEx.cropImage(segmentor.figure.imageGrayInverted, rectangle) : 
						AlgorithmEx.cropImage(segmentor.figure.imageGray, rectangle);
					resize(patch, patch, new Size(64, 64)); //Resize to 64x64 for easy browsing the results
					
					//Construct filename
					Path resultPatchFolder = Character.isUpperCase(PanelSeg.labelToDetect[k])? rstFolder.resolve(segInfo.panelLabel + "_") : rstFolder.resolve(segInfo.panelLabel);	
					if (!Files.exists(resultPatchFolder))	Files.createDirectory(resultPatchFolder);
					String resultFilename = path.getFileName().toString();
					int pos = resultFilename.lastIndexOf('.');
					
					resultFilename = resultFilename.substring(0, pos) + "." + rectangle.toString() + "." + segInfo.labelInverted + ".bmp";
					Path resultPatchFile = resultPatchFolder.resolve(resultFilename);
					imwrite(resultPatchFile.toString(), patch);
				}
			}
			
			//Save Final Segmentation Result
			//2.1 Save result in images
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
	}
	
	@SuppressWarnings("unused")
	private void saveResults() 
	{
		for (int i = 0; i < allPaths.size(); i++)
		{
			Path path = allPaths.get(i);		PanelSeg segmentor = segmentors.get(i);
			
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
	 * @throws IOException 
	 */
	public void segSingleThread() throws IOException 
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
			System.out.println("LabelRegHoGSvm	HoG method followed by SVM classification for recognizing Label candidate regions");
			System.out.println("LabelRegHoGSvmBeam	HoG method followed by SVM classification and then Beam Search for recognizing Label sequences and their regions");
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

		System.out.println("Initialize ... ");
		
		String method = args[0];
		switch (method) 
		{
		case "Jaylene": break;
		case "Santosh": break;
		case "LabelRegMSER": break;
		case "LabelRegHoG": break;
		case "LabelRegHoGSvm": break;
		case "LabelRegHoGSvmBeam": break;
		default: System.out.println(method + " is not known.");	return;
		}
		
		PanelSegEval eval = new PanelSegEval(method, src_path, rst_path, evaluation_file);

		System.out.println("Start Segmentation ... ");
		eval.startTime = System.currentTimeMillis();
		//eval.segSingleThread();
		eval.segMultiThreads(10);
		eval.endTime = System.currentTimeMillis();

//		System.out.println("Save segmentation results ... ");
		//eval.saveResults();
		
		System.out.println("Save evaluation result ... ");
		eval.Evaluate();
		
		System.out.println("DONE!");
	}
	
}
