package gov.nih.nlm.iti.figure;

import java.util.ArrayList;

import org.bytedeco.javacpp.opencv_core.Mat;

public class PanelSegLabelRegHoGSvmBeam extends PanelSegLabelRegHoGSvm 
{
	public PanelSegLabelRegHoGSvmBeam()
	{
		super(); //Call base constructor
	}

	/**
	 * The main entrance function to perform segmentation.
	 * Call getResult* functions to retrieve result in different format.
	 */
	public void segment(Mat image)
	{
		preSegment(image);	//Common initializations for all segmentation method.

		HoGDetect();		//HoG Detection, detected patches are stored in figure.segmentationResultIndividualLabel.
	
		SvmClassification();	//SVM (RBF kernel) classification of each detected patch in figure.segmentationResultIndividualLabel.

		mergeRecognitionLabelsViterbi();
	}

	private void mergeRecognitionLabelsViterbi() 
	{
		mergeDetectedLabelsSimple(); //All detected patches are merged into figure.segmentationResult
		
		if (figure.segmentationResult.size() == 0) return;
		
		sortPatchesHori();
	}
	
	private void sortPatchesHori()
	{
		ArrayList<PanelSegInfo> candidates = figure.segmentationResult;
        candidates.sort(new LabelRectTopAscending());

        //Break them into lines
        ArrayList<ArrayList<PanelSegInfo>> candidateLines = new ArrayList<ArrayList<PanelSegInfo>>();
        ArrayList<PanelSegInfo> candidateLine = new ArrayList<PanelSegInfo>();
    	PanelSegInfo candidate = candidates.get(0);
        candidateLine.add(candidate);
        int bottom = candidate.labelRect.y + candidate.labelRect.height;
        for (int i = 1; i < candidates.size(); i++)
        {
        	candidate = candidates.get(i);
        	if (candidate.labelRect.y > bottom)
        	{
        		candidateLine.add(candidate);
        		bottom = Math.max(bottom, candidate.labelRect.y + candidate.labelRect.height);
        	}
        	else
        	{
        		candidateLines.add(candidateLine);
        		candidateLine = new ArrayList<PanelSegInfo>();
        		i++; if (i >= candidates.size()) break;
            	candidate = candidates.get(i);
                candidateLine.add(candidate);
                bottom = candidate.labelRect.y + candidate.labelRect.height;
        	}
        }
        if (candidateLine.size() != 0) candidateLines.add(candidateLine);
		
        //Run viterbi for each line
        for (int i = 0; i < candidateLines.size(); i++)
        {
        	candidateLine = candidateLines.get(i);
        	candidateLine.sort(new LabelRectLeftAscending());
        	
        	
        }
	}
	
	
}
