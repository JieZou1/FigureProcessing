package gov.nih.nlm.iti.figure;

import java.util.ArrayList;

import org.bytedeco.javacpp.opencv_core.Mat;

import gov.nih.nlm.iti.figure.BeamSearch.BeamLines;

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

		HoGDetect();		//HoG Detection, detected patches are stored in figure.hogDetectionResult.
	
		mergeDetectedLabelsSimple(); //All detected patches are merged into figure.segmentationResult
		
		SvmClassification();	//SVM (RBF kernel) classification of each detected patch in figure.segmentationResult.

		mergeRecognitionLabelsBeam();
	}

	private void mergeRecognitionLabelsBeam() 
	{
		if (figure.segmentationResult.size() == 0) return;
		
		BeamLines horiLines = beamSearchHori();
		BeamLines vertLines = beamSearchVert();

		//Reset segmentationResult
		figure.segmentationResult = new ArrayList<PanelSegInfo>();
		
		if (horiLines == null && vertLines == null) return;
		
		BeamLines lines = null;
		if (horiLines == null)			lines = vertLines;
		else if (vertLines == null)		lines = horiLines;
		else 							lines = horiLines.logProb >= vertLines.logProb ? horiLines : vertLines;
		
		for (int i = 0; i < lines.labels.size(); i++)
		{
			ArrayList<Character> labels = lines.labels.get(i);
			ArrayList<PanelSegInfo> patches = lines.patchs.get(i);
			
			for (int j = 0; j < labels.size(); j++)
			{
				char label = labels.get(j);				if (label == '#') continue;
				PanelSegInfo patch = patches.get(j);
				patch.panelLabel = "" + label;
				figure.segmentationResult.add(patch);
			}
		}
	}
	
	private BeamLines beamSearchHori()
	{
		ArrayList<PanelSegInfo> candidates = figure.segmentationResult;
        candidates.sort(new LabelRectTopAscending()); //Sort detected patches according to their top

        //Break them into lines
        ArrayList<ArrayList<PanelSegInfo>> candidateLines = new ArrayList<ArrayList<PanelSegInfo>>();
        ArrayList<PanelSegInfo> candidateLine = new ArrayList<PanelSegInfo>();
    	PanelSegInfo candidate = candidates.get(0);
        candidateLine.add(candidate);
        int bottom = candidate.labelRect.y + candidate.labelRect.height;
        for (int i = 1; i < candidates.size(); i++)
        {
        	candidate = candidates.get(i);
        	if (candidate.labelRect.y <= bottom)
        	{
        		candidateLine.add(candidate);
        		bottom = Math.max(bottom, candidate.labelRect.y + candidate.labelRect.height);
        	}
        	else
        	{
        		candidateLines.add(candidateLine);
        		candidateLine = new ArrayList<PanelSegInfo>();
                candidateLine.add(candidate);
                bottom = candidate.labelRect.y + candidate.labelRect.height;
        	}
        }
        if (candidateLine.size() != 0) candidateLines.add(candidateLine);

        //Run beam search on these lines
        BeamSearch beam_search = new BeamSearch(500);
        BeamLines path = beam_search.search(candidateLines, false);
        
        return path;
	}
	
	private BeamLines beamSearchVert()
	{
		ArrayList<PanelSegInfo> candidates = figure.segmentationResult;
        candidates.sort(new LabelRectLeftAscending()); //Sort detected patches according to their left

        //Break them into lines
        ArrayList<ArrayList<PanelSegInfo>> candidateLines = new ArrayList<ArrayList<PanelSegInfo>>();
        ArrayList<PanelSegInfo> candidateLine = new ArrayList<PanelSegInfo>();
    	PanelSegInfo candidate = candidates.get(0);
        candidateLine.add(candidate);
        int right = candidate.labelRect.x + candidate.labelRect.width;
        for (int i = 1; i < candidates.size(); i++)
        {
        	candidate = candidates.get(i);
        	if (candidate.labelRect.x <= right)
        	{
        		candidateLine.add(candidate);
        		right = Math.max(right, candidate.labelRect.x + candidate.labelRect.width);
        	}
        	else
        	{
        		candidateLines.add(candidateLine);
        		candidateLine = new ArrayList<PanelSegInfo>();
                candidateLine.add(candidate);
                right = candidate.labelRect.x + candidate.labelRect.width;
        	}
        }
        if (candidateLine.size() != 0) candidateLines.add(candidateLine);

        //Run beam search on these lines
        BeamSearch beam_search = new BeamSearch(500);
        BeamLines path = beam_search.search(candidateLines, true);
        
        return path;
	}
	
}
