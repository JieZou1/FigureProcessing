package gov.nih.nlm.iti.figure;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;

import libsvm.*;

/**
 * Some extension functions of LibSVM
 * @author Jie Zou
 *
 */
final class LibSvmEx 
{
    public static void SaveInLibSVMFormat(String filename, double[] targets, float[][] features)
    {
    	try (PrintWriter pw = new PrintWriter(filename))
        {
            for (int i = 0; i < targets.length; i++)
            {
                pw.print(targets[i]);
                for (int j = 0; j < features[i].length; j++)
                {
                    if (Double.isNaN(features[i][j])) continue;

                    int index = j + 1; float value = features[i][j];
                    pw.print(" " + index + ":" + value);
                }
                pw.println();
            }
        } catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    /**
     * Get all Support Vectors from loaded svm_model
     * @param svModel
     * @return svm_model.SV
     */
    public static libsvm.svm_node[][] getSV(svm_model svModel) 
    {
    	Field svField = null;
    	
		 try {
			svField = svModel.getClass().getDeclaredField("SV");
		} catch (NoSuchFieldException | SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 svField.setAccessible(true);
		 
		 svm_node[][] sv = null;
		try {
			sv = (svm_node[][]) svField.get(svModel);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		 return sv;
	}

    /**
     * Retrieve rho from loaded SVM model
     * @param svModel
     * @return svm_model.rho
     */
    public static double[] getRho(svm_model svModel) 
    {
    	Field rhoField = null;
    	
		try
		{
			rhoField = svModel.getClass().getDeclaredField("rho");
		} catch (NoSuchFieldException | SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 rhoField.setAccessible(true);
		 
		 double[] rho = null;
		try {
			rho = (double[]) rhoField.get(svModel);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		 return rho;
	}
    
    /**
     * Retrieve sv_coef from loaded SVM model
     * @param svModel
     * @return svm_model.sv_coef
     */
    public static double[][] getSvCoef(svm_model svModel) 
    {
    	Field svCoefField = null;
    	
		try
		{
			svCoefField = svModel.getClass().getDeclaredField("sv_coef");
		} catch (NoSuchFieldException | SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		svCoefField.setAccessible(true);
		 
		 double[][] svCoef = null;
		try {
			svCoef = (double[][]) svCoefField.get(svModel);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		 return svCoef;
	}
    
    /// <summary>
    /// Convert the SVM Linear model to single vector representation
    /// </summary>
    public static float[] ToSingleVector(String svm_model_file)
    {
    	svm_model svModel = null;
    	try {
    		svModel = svm.svm_load_model(svm_model_file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	svm_node[][] support_vectors = getSV(svModel);
    	double rho = getRho(svModel)[0];
    	double[] coef = getSvCoef(svModel)[0];
    	int nrFeature = support_vectors[0].length;

        float[] single_vector = new float[nrFeature + 1]; int index; double value;
        for (int i = 0; i < support_vectors.length; i++)
        {
            for (int j = 0; j < support_vectors[i].length; j++)
            {
                index = support_vectors[i][j].index;
                if (index == -1) break;
                value = support_vectors[i][j].value;

                single_vector[index - 1] += (float)(value * coef[i]);
            }
        }
        single_vector[nrFeature] = -(float)rho;

        return single_vector;
    }
}
