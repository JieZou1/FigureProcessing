package gov.nih.nlm.iti.figure;

import java.awt.List;
import java.awt.Rectangle;
import java.util.ArrayList;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Rect;

import weka.associations.LabeledItemSet;

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
		//We may need to break or merge panels.
		//1. Find the closet jaylene-panel to each label-panel
		int[] closet_jaylene_panel_indexes = new int[labelHoGSvm.figure.panelSegResult.size()];
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
			closet_jaylene_panel_indexes[i] = min_panel_index;
		}
		//2. Merge jaylene-panels, which doesn't have any label-panels to the closet sibling.
		ArrayList<ArrayList<Integer>> label_panel_indexes = new ArrayList<ArrayList<Integer>>();
		for (int i = 0; i < jaylene.figure.panelSegResult.size(); i++)
		{
			ArrayList<Integer> indexes = new ArrayList<Integer>();
			for (int j = 0; j < closet_jaylene_panel_indexes.length; j++)
			{
				if (closet_jaylene_panel_indexes[j] == i) indexes.add(j);				
			}
			label_panel_indexes.set(i, indexes);
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
		//3. Split jaylene-panels, which have more than one label-panels.
		figure.panelSegResult = new ArrayList<PanelSegInfo>();
		for (int i = 0; i < jaylene.figure.panelSegResult.size(); i++)
		{
			//Expand the panel to make it include label_rect
			PanelSegInfo panel = jaylene.figure.panelSegResult.get(i);
			ArrayList<PanelSegInfo> label_panels = new ArrayList<PanelSegInfo>();
			for (int j = 0; j < label_panel_indexes.get(i).size(); j++)
			{
				PanelSegInfo label = labelHoGSvm.figure.panelSegResult.get(label_panel_indexes.get(i).get(j));
				panel.panelLabel += label.panelLabel; 
				panel.panelRect = panel.panelRect.union(label.labelRect);
				label_panels.add(label);
			}

			if (label_panels.size() == 0)
				figure.panelSegResult.add(panel);
			else	
				figure.panelSegResult.addAll(SplitPanel(panel.panelRect, label_panels));
			
		}
		
		return true;
	}
	
	/**
	 * The panel contains more than one labels, we need to split the panel to contain each individual labels. 
	 * @param panel
	 * @param labels
	 * @return
	 */
	ArrayList<PanelSegInfo> SplitPanel(Rectangle panel_rect, ArrayList<PanelSegInfo> labels)
	{
		//Break them into lines
		ArrayList<ArrayList<PanelSegInfo>> lines = new ArrayList<ArrayList<PanelSegInfo>>();
		ArrayList<Rectangle> line_rects = new ArrayList<Rectangle>();
		for (int i = 0; i < labels.size(); i++)
		{
			PanelSegInfo label = labels.get(i);
			Rectangle label_rect = label.labelRect;
			
			int index = -1;
			for (int j = 0; j < line_rects.size(); j++)
			{
				Rectangle line_rect = line_rects.remove(j);
				int v_distance = vDistance(label_rect,line_rect);
				if (v_distance == 0)
				{
					index = j; break;
				}				
			}
			if (index >= 0)
			{
				Rectangle rect = label_rect.union(line_rects.get(index));
				line_rects.set(index, rect);
				lines.get(i).add(label);
			}
			else
			{
				Rectangle rect = (Rectangle) label_rect.clone();
				line_rects.add(rect);
				ArrayList<PanelSegInfo> line = new ArrayList<PanelSegInfo>(); line.add(label);
				lines.add(line);
			}
		}

		if (lines.size() > 1)
		{	//We need to do a vertical split
			line_rects.sort(new RectangleTopAscending()); //Sort line_rects
			
			Rectangle rect_first = line_rects.get(0);
			Rectangle rect_last = line_rects.get(line_rects.size()-1);
			int distance_to_top = rect_first.y - panel_rect.y;
			int distance_to_bottom = (panel_rect.y + panel_rect.height) - (rect_last.y + rect_last.height);
			int averge_line_height = panel_rect.height / lines.size();
			
			if (distance_to_top < averge_line_height / 4 && distance_to_bottom > averge_line_height / 2)
			{	//Aligned at top
				for (int i = 0; i < lines.size(); i++)
				{
					int y0 = i == 0 ? panel_rect.y : line_rects.get(i).y - distance_to_top; 
					int y1 = i == lines.size() - 1 ? panel_rect.y + panel_rect.height : line_rects.get(i+1).y - distance_to_top;
					Rectangle rect0 = new Rectangle(panel_rect.x, y0, panel_rect.width, y1 - y0);
					line_rects.set(i, rect0);
				}
			}
			else if (distance_to_bottom < averge_line_height / 4 && distance_to_top > averge_line_height / 2)
			{	//Aligned at bottom
				for (int i = 0; i < lines.size(); i++)
				{
					int y0 = i == 0 ? panel_rect.y : line_rects.get(i - 1).y + line_rects.get(i - 1).height + distance_to_bottom; 
					int y1 = i == lines.size() - 1 ? panel_rect.y + panel_rect.height : line_rects.get(i).y + line_rects.get(i).height + distance_to_bottom;
					Rectangle rect0 = new Rectangle(panel_rect.x, y0, panel_rect.width, y1 - y0);
					line_rects.set(i, rect0);
				}
			}
			else
			{	//Aligned in the middle or other cases
				for (int i = 0; i < lines.size(); i++)
				{
					int y0 = i == 0 ? panel_rect.y : (line_rects.get(i - 1).y + line_rects.get(i).y)/2; 
					int y1 = i == lines.size() - 1 ? panel_rect.y + panel_rect.height : (line_rects.get(i).y + line_rects.get(i + 1).y)/2;
					Rectangle rect0 = new Rectangle(panel_rect.x, y0, panel_rect.width, y1 - y0);
					line_rects.set(i, rect0);
				}				
			}			
		}
		
		for (int i = 0; i < lines.size(); i++)
		{	//Horizontal split in each line
			ArrayList<PanelSegInfo> line = lines.get(i);
			if (line.size() > 1)
			{
				
			}
			else
			{
				
			}
		}
		
		ArrayList<PanelSegInfo> panels = new ArrayList<PanelSegInfo>();
		
		return panels;
		
	}
	
	ArrayList<PanelSegInfo> SplitHorizontally(Rectangle panel_rect, ArrayList<Rectangle> label_rects)
	{
		ArrayList<PanelSegInfo> panels = new ArrayList<PanelSegInfo>();
		
		return panels;
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
	
	int vDistance(Rectangle label_rect, Rectangle panel_rect)
	{
		int v_distance = 0;
		if (label_rect.y >= panel_rect.y + panel_rect.height) v_distance =  label_rect.y - (panel_rect.y + panel_rect.height); //label rect is below panel rect
		else if (label_rect.y + label_rect.height <= panel_rect.y) v_distance =  panel_rect.y - (label_rect.y + label_rect.height); //label rect is above panel rect
		return v_distance;
	}

	int hDistance(Rectangle label_rect, Rectangle panel_rect)
	{
		int h_distance = 0;
		if (label_rect.x >= panel_rect.x + panel_rect.width) h_distance =  label_rect.x - (panel_rect.x + panel_rect.width); //label rect is to the right of panel rect
		else if (label_rect.x + label_rect.width <= panel_rect.x) h_distance =  panel_rect.x - (label_rect.x + label_rect.width); //label rect is to the left of panel rect
		
		return h_distance;
	}
}



