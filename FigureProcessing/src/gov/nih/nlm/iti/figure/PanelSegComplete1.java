package gov.nih.nlm.iti.figure;

import java.awt.Rectangle;
import java.util.ArrayList;

import org.bytedeco.javacpp.opencv_core.*;

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
		
		Merge();
		
		//Reach Here, the panels and their labels are matched.
		//Either all panels contain labels or none panels contain labels.
		//If panel contain label, it contains only one label.
		
		//TODO: 
		//1. sort panels according to their labels
		//2. Make panelRect contains labelRect.
	}
	
	private void Merge()
	{
		if (FoundTrustworthyLabelSequence())
		{
			MergeFromLabels();
		}
		
		//Combine the results from the 3 methods
		if (MergeFromLabels()) return;
			
		MergeFromJaylene();
		
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
	
	private boolean FoundTrustworthyLabelSequence()
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
		
		{//If it is a sequence from 'a' continuously without any missing characters, it is trustworthy. 
			char ch; int i; 
			for (ch = 'a', i = 0; i < labels.size(); i++, ch++)
			{
				if (labels.get(i) != ch) break;
			}
			if (i == labels.size()) return true;
		}
		
		//Reach Here: it is not a continuous labels, we try find a trustworthy label sequence based on its positions.
		//Basically, we assume the adjacent labels must align horizontally or vertically.
		//1. We break into 'a', 'b', 'c', ...
		ArrayList<ArrayList<PanelSegInfo>> a_to_z = new ArrayList<ArrayList<PanelSegInfo>>();
		for (int i = 'a'; i <= 'z'; i++) {	a_to_z.add(new ArrayList<PanelSegInfo>());		}
		for (int i = 0; i < labels.size(); i++)
		{
			char label = labels.get(i);
			a_to_z.get(label).add(labelHoGSvm.figure.panelSegResult.get(i));
		}
		//2. We use beam search to try finding an optimal (minimum aligning distance) sequence
		ArrayList<ArrayList<ArrayList<PanelSegInfo>>> path = new ArrayList<ArrayList<ArrayList<PanelSegInfo>>>();
		ArrayList<ArrayList<Integer>> path_distance = new ArrayList<ArrayList<Integer>>();
		for (int i = 'a'; i <= 'z'; i++) 
		{	
			path.add(new ArrayList<ArrayList<PanelSegInfo>>());	
			path_distance.add(new ArrayList<Integer>());	
		}
		//Set the path of the first label
		int first_char = 0;	for (int i = 0; i < a_to_z.size(); i++)	if (a_to_z.get(i).size() > 0) {first_char = i; break;}
		for (int i = 0; i < a_to_z.get(first_char).size(); i++)
		{
			ArrayList<PanelSegInfo> segment = new ArrayList<PanelSegInfo>();			
			segment.add(a_to_z.get(first_char).get(i));
			path.get(first_char).add(segment);
			path_distance.get(first_char).add(0);
		}
		for (int k = first_char + 1; k < a_to_z.size(); k++)
		{
			ArrayList<ArrayList<PanelSegInfo>> prev_nodes = path.get(k - 1);
			ArrayList<Integer> prev_distances = path_distance.get(k - 1);
			for (int i = 0; i < a_to_z.get(k).size(); i++)
			{
				PanelSegInfo panel = a_to_z.get(k).get(i);
				for (int m = 0; m < prev_nodes.size(); m++)
				{
					ArrayList<PanelSegInfo> prev_node = prev_nodes.get(m);
					int min_distance = Integer.MAX_VALUE; int min_n = 0;
					for (int n = 0; n < prev_node.size(); n++)
					{
						PanelSegInfo prev_panel = prev_node.get(i);
						int dis_h = Math.abs(panel.labelRect.x - prev_panel.labelRect.x) + 
									Math.abs((panel.labelRect.x + panel.labelRect.width) - (prev_panel.labelRect.x + prev_panel.labelRect.width));
						int dis_v = Math.abs(panel.labelRect.y - prev_panel.labelRect.y) + 
									Math.abs((panel.labelRect.y + panel.labelRect.height) - (prev_panel.labelRect.y + prev_panel.labelRect.height));
						int distance = Math.min(dis_h, dis_v);
						if (distance < min_distance) { min_n = n; min_distance = distance; }
					}
					if (min_distance > panel.labelRect.width || min_distance > panel.labelRect.height) continue;
					
					ArrayList<PanelSegInfo> segment = new ArrayList<PanelSegInfo>();
					segment.addAll(prev_node);	segment.add(panel);
					
				}
				
				ArrayList<PanelSegInfo> segment = new ArrayList<PanelSegInfo>();			
				segment.add(a_to_z.get(first_char).get(i));
				path.get(first_char).add(segment);
				path_distance.get(first_char).add(0);
			}
		}
		
		ArrayList<PanelSegInfo> trustworthy_labels = new ArrayList<PanelSegInfo>();
		
		
		return false;
	}
	
	private boolean MergeFromLabels()
	{
		//To here, the labels detected are trustworthy. We find the closest panels and assign it to the corresponding label
		//We may need to break or merge panels.
		//1. Find the closet jaylene-panel to each label-panel
		int[] closest_jaylene_panel_indexes = new int[labelHoGSvm.figure.panelSegResult.size()];
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
			closest_jaylene_panel_indexes[i] = min_panel_index;
		}
		//2. Merge jaylene-panels, which doesn't have any label-panels to the closet sibling.
		ArrayList<ArrayList<Integer>> label_panel_indexes = new ArrayList<ArrayList<Integer>>();
		for (int i = 0; i < jaylene.figure.panelSegResult.size(); i++)
		{
			ArrayList<Integer> indexes = new ArrayList<Integer>();
			for (int j = 0; j < closest_jaylene_panel_indexes.length; j++)
			{
				if (closest_jaylene_panel_indexes[j] == i) indexes.add(j);				
			}
			label_panel_indexes.add(indexes);
		}
		for (int i = jaylene.figure.panelSegResult.size() - 1; i >= 0; i--)
		{
			if (label_panel_indexes.get(i).size() == 0)
			{
				PanelSegInfo panel_to_remove = jaylene.figure.panelSegResult.get(i);
				int min_distance = Integer.MAX_VALUE; int min_panel_index = 0;
				for (int j = 0; j < jaylene.figure.panelSegResult.size(); j++)
				{
					if (j == i) continue;
					int distance = distance(panel_to_remove.panelRect, jaylene.figure.panelSegResult.get(j).panelRect);
					if (distance < min_distance)
					{
						min_distance = distance;	min_panel_index = j;
					}
				}
				Rectangle union = panel_to_remove.panelRect.union(jaylene.figure.panelSegResult.get(min_panel_index).panelRect);
				jaylene.figure.panelSegResult.get(min_panel_index).panelRect = union;
				jaylene.figure.panelSegResult.remove(i);
				label_panel_indexes.remove(i);
			}
		}
		
		//3. Expand the panel to make it include label_rect
		for (int i = 0; i < jaylene.figure.panelSegResult.size(); i++)
		{
			PanelSegInfo panel = jaylene.figure.panelSegResult.get(i);
			for (int j = 0; j < label_panel_indexes.get(i).size(); j++)
			{
				int index = label_panel_indexes.get(i).get(j);
				PanelSegInfo label = labelHoGSvm.figure.panelSegResult.get(index);
				panel.panelLabel = j == 0? label.panelLabel : panel.panelLabel + label.panelLabel; 
				panel.panelRect = panel.panelRect.union(label.labelRect);
			}
			jaylene.figure.panelSegResult.set(i, panel);
		}
		
		//4. Detect Overlapping panels, Once we see overlapping panels, we merge all panels together
		boolean overlapping = false;
		for (int i = 0; i < jaylene.figure.panelSegResult.size(); i++)
		{
			PanelSegInfo panel1 = jaylene.figure.panelSegResult.get(i);
			Rectangle rect1 = panel1.panelRect;
			for (int j = i + 1; j < jaylene.figure.panelSegResult.size(); j++)
			{
				PanelSegInfo panel2 = jaylene.figure.panelSegResult.get(j);
				Rectangle rect2 = panel2.panelRect;
				if (rect2.intersects(rect1)) {overlapping = true; break;}
			}
			if (overlapping) break;
		}
		if (overlapping)
		{	//Overlap detected, we merge all as one panel
			PanelSegInfo panel = new PanelSegInfo();
			panel.panelRect = jaylene.figure.panelSegResult.get(0).panelRect;
			for (int i = 1; i < jaylene.figure.panelSegResult.size(); i++)
			{
				panel.panelRect = panel.panelRect.union(jaylene.figure.panelSegResult.get(i).panelRect);
			}
			jaylene.figure.panelSegResult = new ArrayList<PanelSegInfo>();
			jaylene.figure.panelSegResult.add(panel);

			ArrayList<Integer> indexes = new ArrayList<Integer>();
			for (int i = 0; i < labelHoGSvm.figure.panelSegResult.size(); i++)		indexes.add(i);
			label_panel_indexes = new ArrayList<ArrayList<Integer>>();
			label_panel_indexes.add(indexes);
		}
			
		
		//5. Split jaylene-panels, which have more than one label-panels.
		figure.panelSegResult = new ArrayList<PanelSegInfo>();
		for (int i = 0; i < jaylene.figure.panelSegResult.size(); i++)
		{
			//Expand the panel to make it include label_rect
			PanelSegInfo panel = jaylene.figure.panelSegResult.get(i);
			ArrayList<PanelSegInfo> label_panels = new ArrayList<PanelSegInfo>();
			for (int j = 0; j < label_panel_indexes.get(i).size(); j++)
			{
				int index = label_panel_indexes.get(i).get(j);
				PanelSegInfo label = labelHoGSvm.figure.panelSegResult.get(index);
				panel.panelLabel = j == 0? label.panelLabel : panel.panelLabel + label.panelLabel; 
				panel.panelRect = panel.panelRect.union(label.labelRect);
				label_panels.add(label);
			}

			if (label_panels.size() == 1)
			{
				PanelSegInfo p = label_panels.get(0);
				p.panelRect = panel.panelRect;
				figure.panelSegResult.add(p);
			}
			else	
				figure.panelSegResult.addAll(SplitPanel(panel.panelRect, label_panels));
			
		}
		
		return true;
	}
	
	/**
	 * We assume Jaylene's result (Panel Splitting) is correct, So 
	 * By forcing each Panel containing only one label, we can remove a lot of label false alarms.
	 */
	private boolean MergeFromJaylene()
	{
		//TODO:
		//1. For each panel, find its label candidates. Some panel may contain more than one labels, and some may not contain labels.
		
		for (int i = 0; i < labelHoGSvm.figure.panelSegResult.size(); i++)
		{
			PanelSegInfo label_panel = labelHoGSvm.figure.panelSegResult.get(i);
			
		}
		
		//2. Use beam search to find the optimal panel-label sequence (one panel could contain 0 or 1 labels only)
		
		return true;
	}
}



