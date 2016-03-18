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
	float[] singleVector;

	@Override
	public void Train(Path imageFilePath, Path resultFolder) throws Exception 
	{
		singleVector = LibSvmEx.ToSingleVector(imageFilePath.toString());
	}
	
	static void generateSingleVec(ArrayList<PanelSegTrainMethod> methods, String filename)
	{
		int n = PanelSeg.labelArray.length;
		
    	try (PrintWriter pw = new PrintWriter(filename))
        {
    		pw.println("package gov.nih.nlm.iti.figure;");
    		pw.println();

            pw.println("final class " + filename.substring(0, filename.indexOf('.')));
            pw.println("{");
    		
            pw.println("	protected static float[][] svmModels = ");
            pw.println("    {");
    		
    		for (int i = 0; i < n; i++)
    		{
    			PanelSegTrainSvm2SingleVec method = (PanelSegTrainSvm2SingleVec)methods.get(i);
    			float[] singleVector = method.singleVector;
                
                pw.println("    	{");
                for (int k = 0; k < singleVector.length; k++)
                {
                    pw.print(singleVector[k] + "f,");
                }
        		pw.println();
                pw.println("    	},");
    		}
    		
            pw.println("    };");
            pw.println("}");
        } catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
	}
	
}
