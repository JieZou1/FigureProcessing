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
		
		//Combine the results from the 3 methods
		//MergeTrivial();
		if (!MergeFromLabels())
		{
			MergeOtherCases();
		}
		
		
	}

	/**
	 * Trivial method to combine results, simply to add results from all methods
	 */
	//@SuppressWarnings("unused")
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
	 * The panel contains more than one labels, we need to split the panel to contain each individual labels. 
	 * @param panel
	 * @param labels
	 * @return
	 */
	private ArrayList<PanelSegInfo> SplitPanel(Rectangle panel_rect, ArrayList<PanelSegInfo> labels)
	{
		//Break them into lines
		ArrayList<ArrayList<PanelSegInfo>> lines = new ArrayList<ArrayList<PanelSegInfo>>();
		ArrayList<Rectangle> line_rects = new ArrayList<Rectangle>();
		for (int i = 0; i < labels.size(); i++)
		{
			PanelSegInfo label = labels.get(i);			Rectangle label_rect = label.labelRect;			int index = -1;
			for (int j = 0; j < line_rects.size(); j++)
			{
				Rectangle line_rect = line_rects.get(j);
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
				lines.get(index).add(label);
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
					int y0 = i == 0 ? panel_rect.y : line_rects.get(i - 1).y + line_rects.get(i - 1).height; 
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
		else
		{
			line_rects.set(0, panel_rect);
		}
		
		//Horizontal split in each line, and complete the process
		ArrayList<PanelSegInfo> panels = new ArrayList<PanelSegInfo>();
		for (int i = 0; i < lines.size(); i++)
		{
			Rectangle line_rect = line_rects.get(i);
			ArrayList<PanelSegInfo> line = lines.get(i);
			if (line.size() > 1)
			{	//Horizontal split
				//Break into columns
				ArrayList<Rectangle> column_rects = new ArrayList<Rectangle>();
				for (int j = 0; j < line.size(); j++)	column_rects.add(line.get(j).labelRect);
				Rectangle rect_first = column_rects.get(0);
				Rectangle rect_last = column_rects.get(column_rects.size()-1);
				int distance_to_left = rect_first.x - panel_rect.x;
				int distance_to_right = (panel_rect.x + panel_rect.width) - (rect_last.x + rect_last.width);
				int averge_column_width = line_rect.width / line.size();
				
				if (distance_to_left < averge_column_width / 4 && distance_to_right > averge_column_width / 2)
				{	//Aligned at left
					for (int j = 0; j < line.size(); j++)
					{
						int x0 = j == 0 ? line_rect.x : column_rects.get(j).x - distance_to_left; 
						int x1 = j == line.size() - 1 ? line_rect.x + line_rect.width : column_rects.get(j+1).x - distance_to_left;
						Rectangle rect0 = new Rectangle(x0, line_rect.y, x1 - x0, line_rect.height);
						column_rects.set(j, rect0);
					}
				}
				else if (distance_to_right < averge_column_width / 4 && distance_to_left > averge_column_width / 2)
				{	//Aligned at right
					for (int j = 0; j < line.size(); j++)
					{
						int x0 = j == 0 ? line_rect.x : column_rects.get(j - 1).x + column_rects.get(j - 1).width; 
						int x1 = j == line.size() - 1 ? line_rect.x + line_rect.width : column_rects.get(j).x + column_rects.get(j).width + distance_to_right;
						Rectangle rect0 = new Rectangle(x0, line_rect.y, x1-x0, line_rect.height);
						column_rects.set(j, rect0);
					}
				}
				else
				{	//Aligned in the middle or other cases
					for (int j = 0; j < line.size(); j++)
					{
						int x0 = j == 0 ? line_rect.x : (column_rects.get(j - 1).x + column_rects.get(j - 1).width); 
						int x1 = j == line.size() - 1 ? line_rect.x + line_rect.width : (column_rects.get(j).x + column_rects.get(j + 1).x)/2;
						Rectangle rect0 = new Rectangle(x0, line_rect.y, x1 - x0, line_rect.height);
						column_rects.set(j, rect0);
					}				
				}
				
				for (int j = 0; j < line.size(); j++)
				{
					PanelSegInfo panel = line.get(j);
					panel.panelRect = column_rects.get(j);
					panels.add(panel);
				}
			}
			else 
			{
				PanelSegInfo panel = line.get(0);
				panel.panelRect = line_rect;
				panels.add(panel);
			}
		}
		
		return panels;
		
	}

	private void MergeOtherCases()
	{
		MergeTrivial();
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



