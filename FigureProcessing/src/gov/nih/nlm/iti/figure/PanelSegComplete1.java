package gov.nih.nlm.iti.figure;

import java.awt.List;
import java.awt.Rectangle;
import java.util.ArrayList;

import org.bytedeco.javacpp.opencv_core.Mat;

public class PanelSegComplete1 extends PanelSegComplete 
{
	PanelSegPanelSplitJaylene jaylene;
	PanelSegPanelSplitSantosh santosh;
	PanelSegLabelRegHoGSvm labelHoGSvm;

	public PanelSegComplete1() 
	{
		jaylene = new PanelSegPanelSplitJaylene();
		santosh = new PanelSegPanelSplitSantosh();
		labelHoGSvm = new PanelSegLabelRegHoGSvm();
		

	}
	
	@Override
	public void segment(Mat image) 
	{
		preSegment(image);	//Common initializations for all segmentation method.

		jaylene.segment(image);
		santosh.segment(image);
		labelHoGSvm.segment(image);
		
		//Combine the results from the 3 methods
		//MergeTrivial();
		MergeFromLabels();
		
	}

	/**
	 * Trivial method to combine results, simply to add results from all methods
	 */
	@SuppressWarnings("unused")
	private void MergeTrivial()
	{
		figure.panelSegResult = new ArrayList<PanelSegInfo>();
		
		figure.panelSegResult.addAll(jaylene.figure.panelSegResult);
		figure.panelSegResult.addAll(labelHoGSvm.figure.panelSegResult);
	}
	
	private boolean MergeFromLabels()
	{
		//collect labels from labelHoGSvm
		labelHoGSvm.figure.panelSegResult.sort(new PanelLabelAscending());
		ArrayList<Character> labels = new ArrayList<Character>();
		for (int i = 0; i < labelHoGSvm.figure.panelSegResult.size(); i++)
		{
			PanelSegInfo panel = labelHoGSvm.figure.panelSegResult.get(i);
			labels.add(Character.toLowerCase(panel.panelLabel.charAt(0)));
		}
		if (labels.size() <= 1) return false;
		
		char ch = 'a'; 
		for (int i = 0; i < labels.size(); i++)
		{
			if (labels.get(i) != ch) return false;
			ch++;
		}
		
		//To here, the labels detected are trustworthy. We find the closest panels and assign it to the corresponding label
		//We need to minimize the joint distances, and we may need to break or merge panels.
		//1. Find the closet jaylene-panel to each label-panel
		//2. Merge jaylene-panels, which doesn't have any label-panels to the closet sibling.
		//3. Split jaylene-panels, which have more than one label-panels.
		
		//TODO: rewrite the codes below
		
		ArrayList<ArrayList<Integer>> jaylenePanelsAssignedLabelRects = new ArrayList<ArrayList<Integer>>();
		for (int i = 0; i < jaylene.figure.panelSegResult.size(); i++) jaylenePanelsAssignedLabelRects.add(new ArrayList<Integer>());
		
		for (int i = 0; i < labelHoGSvm.figure.panelSegResult.size(); i++)
		{
			PanelSegInfo labelPanel = labelHoGSvm.figure.panelSegResult.get(i);			Rectangle labelRect = labelPanel.labelRect;
			
			int min_distance = Integer.MAX_VALUE; int min_panel_index = 0;
			for (int j = 0; j < jaylene.figure.panelSegResult.size(); j++)
			{
				PanelSegInfo jaylenePanel = jaylene.figure.panelSegResult.get(j);		Rectangle jayleneRect = jaylenePanel.panelRect;
				
				if (jayleneRect.intersects(labelRect))	{	min_panel_index = j; break;	}
				
				int distance = distance(labelRect, jayleneRect);
				if (distance < min_distance)
				{
					min_distance = distance;	min_panel_index = j;
				}
			}
			jaylenePanelsAssignedLabelRects.get(min_panel_index).add(i);
		}

		//2. Check jaylenePanelsAssignedLabelRects, which contains more than one labels
		//We 
		
		
		
		return true;
	}
	
	/**
	 * Calculate the distance between 2 rects. 
	 * rect_label and rect_panel do not intersect
	 * @param panel
	 */
	int distance(Rectangle label_rect, Rectangle panel_rect)
	{
		int v_distance = 0;
		if (label_rect.y >= panel_rect.y + panel_rect.height) v_distance =  label_rect.y - (panel_rect.y + panel_rect.height); //label rect is below panel rect
		else if (label_rect.y + label_rect.height <= panel_rect.y) v_distance =  panel_rect.y - (label_rect.y + label_rect.height); //label rect is above panel rect
		
		int h_distance = 0;
		if (label_rect.x >= panel_rect.x + panel_rect.width) h_distance =  label_rect.x - (panel_rect.x + panel_rect.width); //label rect is to the right of panel rect
		else if (label_rect.x + label_rect.width <= panel_rect.x) h_distance =  panel_rect.x - (label_rect.x + label_rect.width); //label rect is to the left of panel rect
		
		return v_distance + h_distance;
	}
	
}



