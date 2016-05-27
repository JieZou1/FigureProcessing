package gov.nih.nlm.iti.figure;

import java.awt.Rectangle;
import java.util.ArrayList;

/**
 * The base class for complete Panel Segmentation method, including Panel Split and Label Recognition <p>
 * 
 * This class is not intended to be instantiated, so we make it abstract. 
 * 
 * @author Jie Zou
 *
 */
public abstract class PanelSegComplete extends PanelSeg 
{
	/**
	 * Calculate the distance between 2 rects. 
	 * rect_label and rect_panel do not intersect
	 * @param panel
	 */
	protected int distance(Rectangle label_rect, Rectangle panel_rect)
	{
		int v_distance = 0;
		if (label_rect.y >= panel_rect.y + panel_rect.height) v_distance =  label_rect.y - (panel_rect.y + panel_rect.height); //label rect is below panel rect
		else if (label_rect.y + label_rect.height <= panel_rect.y) v_distance =  panel_rect.y - (label_rect.y + label_rect.height); //label rect is above panel rect
		
		int h_distance = 0;
		if (label_rect.x >= panel_rect.x + panel_rect.width) h_distance =  label_rect.x - (panel_rect.x + panel_rect.width); //label rect is to the right of panel rect
		else if (label_rect.x + label_rect.width <= panel_rect.x) h_distance =  panel_rect.x - (label_rect.x + label_rect.width); //label rect is to the left of panel rect
		
		return v_distance + h_distance;
	}
	
	protected int vDistance(Rectangle label_rect, Rectangle panel_rect)
	{
		int v_distance = 0;
		if (label_rect.y >= panel_rect.y + panel_rect.height) v_distance =  label_rect.y - (panel_rect.y + panel_rect.height); //label rect is below panel rect
		else if (label_rect.y + label_rect.height <= panel_rect.y) v_distance =  panel_rect.y - (label_rect.y + label_rect.height); //label rect is above panel rect
		return v_distance;
	}

	protected int hDistance(Rectangle label_rect, Rectangle panel_rect)
	{
		int h_distance = 0;
		if (label_rect.x >= panel_rect.x + panel_rect.width) h_distance =  label_rect.x - (panel_rect.x + panel_rect.width); //label rect is to the right of panel rect
		else if (label_rect.x + label_rect.width <= panel_rect.x) h_distance =  panel_rect.x - (label_rect.x + label_rect.width); //label rect is to the left of panel rect
		
		return h_distance;
	}

	/**
	 * The panel contains more than one labels, we need to split the panel to contain each individual labels. 
	 * @param panel
	 * @param labels
	 * @return
	 */
	protected ArrayList<PanelSegInfo> SplitPanel(Rectangle panel_rect, ArrayList<PanelSegInfo> labels)
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
	
}
