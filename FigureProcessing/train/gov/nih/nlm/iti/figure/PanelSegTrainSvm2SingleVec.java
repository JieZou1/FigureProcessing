package gov.nih.nlm.iti.figure;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;

/**
 * A PanelSegTrainMethod class for converting Linear SVM model to single float vector
 * @author Jie Zou
 *
 */
final class PanelSegTrainSvm2SingleVec extends PanelSegTrainMethod 
{
	/**
	 * Prepare the Panel Segmentation training. 
	 * @param method	The PanelSeg method
	 * @param srcFolder	The source folder
	 * @param rstFolder	The result folder
	 *
	 * @return a PanelSegTrain instance with all the parameters are set 
	 */
	static PanelSegTrain createPanelSegTrain(String method, Path srcFolder, Path rstFolder)
	{
		PanelSegTrain segTrain = new PanelSegTrain(method, srcFolder, rstFolder);

//		for (int i = 0 ; i < PanelSeg.labelToDetect.length; i++)
//		{
//			char ch = PanelSeg.labelToDetect[i];
//			Path path = Character.isLowerCase(ch) ? srcFolder.resolve("svm_model_" + PanelSeg.labelToDetect[i]) : srcFolder.resolve("svm_model_" + PanelSeg.labelToDetect[i] +"_");
//			segTrain.allPaths.add(path);
//			segTrain.methods.add(new PanelSegTrainSvm2SingleVec());
//		}

		for (int i = 0 ; i < PanelSeg.labelsToDetect.length; i++)
		{
			Path path = srcFolder.resolve("svm_model_" + PanelSeg.labelsToDetect[i]);
			segTrain.allPaths.add(path);
			segTrain.methods.add(new PanelSegTrainSvm2SingleVec());
		}
		
		return segTrain;
	}

	float[] singleVector;
	
	@Override
	public void Train(Path imageFilePath, Path resultFolder) throws Exception 
	{
		singleVector = LibSvmEx.ToSingleVector(imageFilePath.toString());
	}
	
	static void generateSingleVec(ArrayList<PanelSegTrainMethod> methods, String filename)
	{
    	try (PrintWriter pw = new PrintWriter(filename))
    	{
    		pw.println("package gov.nih.nlm.iti.figure;");
    		pw.println();

    		int n = PanelSeg.labelsToDetect.length;
    		
    		for (int i = 0; i < n; i++)
    		{
    			PanelSegTrainSvm2SingleVec method = (PanelSegTrainSvm2SingleVec)methods.get(i);
    			float[] singleVector = method.singleVector;
    			
    			String classname = filename.substring(0, filename.lastIndexOf('.') - 1); 
    			classname =	classname + "_" + PanelSeg.labelsToDetect[i];

	            pw.println("final class " + classname);
	            pw.println("{");
	    		
	            pw.println("	public static float[] svmModel = ");
	    		
                pw.println("    	{");
                for (int k = 0; k < singleVector.length; k++)
                {
                    pw.print(singleVector[k] + "f,");
                }
        		pw.println();
	            pw.println("    };");
	            pw.println("}");
    		}
    	}
    	catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
}
