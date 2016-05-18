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
		if (figure.panelSegResult.size() == 0) return;

		BeamLines horiLines = beamSearchHori();
		BeamLines vertLines = beamSearchVert();

		//Reset segmentationResult
		figure.panelSegResult = new ArrayList<PanelSegInfo>();
		
		if (horiLines == null && vertLines == null) return;
		
		BeamLines lines = null;
		if (horiLines == null)			lines = vertLines;
		else if (vertLines == null)		lines = horiLines;
		else 							lines = horiLines.logProb >= vertLines.logProb ? horiLines : vertLines;
		
		ArrayList<PanelSegInfo> panels = FinalCheck(lines);
		if (panels != null)	figure.panelSegResult.addAll(panels);
	}
	
	private BeamLines beamSearchHori()
	{
		ArrayList<PanelSegInfo> candidates = figure.panelSegResult;
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
		ArrayList<PanelSegInfo> candidates = figure.panelSegResult;
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

	private ArrayList<PanelSegInfo> FinalCheck(BeamLines lines)
	{
        //Do some final checking, reject certain cases
        if (lines == null) return null;

		//Get label and patch sequences
		ArrayList<Character> labels = new ArrayList<Character>();
		ArrayList<PanelSegInfo> patches = new ArrayList<PanelSegInfo>();
		for (int i = 0; i < lines.labels.size(); i++)
		{
			for (int j = 0; j < lines.labels.get(i).size(); j++)
			{
				char ch = lines.labels.get(i).get(j);				if (ch == '#') continue;
				labels.add(ch);
				
				PanelSegInfo patch = lines.patchs.get(i).get(j);
				patch.panelLabel = "" + ch;
				patches.add(lines.patchs.get(i).get(j));
			}
		}

		//If there is only a single label, there must be something wrong, we ignore it.
		if (labels.size() <= 1) return null; 

		//We remove patches, which does not align with other patches either horizontally or vertically
		ArrayList<PanelSegInfo> panels = new ArrayList<PanelSegInfo>();
		
		for (int i = 0; i < patches.size(); i++)
		{
			PanelSegInfo curr_patch = patches.get(i);
			panels.add(curr_patch);
		}
		
//		for (int i = 0; i < patches.size(); i++)
//		{
//			PanelSegInfo curr_patch = patches.get(i);
//			Rectangle curr_rect = curr_patch.labelRect;
//			Rectangle rect_hori = new Rectangle(0, curr_rect.y, figure.imageWidth, curr_rect.height);
//			Rectangle rect_vert = new Rectangle(curr_rect.x, 0, curr_rect.width, figure.imageHeight);
//			
//			boolean found_aligned = false;
//			for (int j = 0; j < patches.size(); j++)
//			{
//				if (i == j) continue;
//				PanelSegInfo patch = patches.get(j);
//				Rectangle rect = patch.labelRect;
//				
//				{	//Check horizontally
//					Rectangle intersection = rect.intersection(rect_hori);
//					if (!intersection.isEmpty())
//					{
//						double intersection_area = intersection.width * intersection.height;
//						double rect_area = rect.width * rect.height;
//						if (intersection_area > rect_area / 2)	{	found_aligned = true; break;}
//					}
//				}
//				{	//Check vertically
//					Rectangle intersection = rect.intersection(rect_vert);
//					if (!intersection.isEmpty())
//					{
//						double intersection_area = intersection.width * intersection.height;
//						double rect_area = rect.width * rect.height;
//						if (intersection_area > rect_area / 2)	{	found_aligned = true; break;}
//					}
//				}
//			}
//			if (found_aligned)	panels.add(curr_patch);
//		}
		
		return panels;
		
	}

}
