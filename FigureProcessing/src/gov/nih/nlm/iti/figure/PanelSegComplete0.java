package gov.nih.nlm.iti.figure;

import java.awt.Rectangle;
import java.util.ArrayList;

import org.bytedeco.javacpp.opencv_core.Mat;

import weka.gui.PropertyPanel;

public class PanelSegComplete0 extends PanelSegComplete 
{
	PanelSegPanelSplitJaylene jaylene;
	PanelSegLabelRegHoGSvmBeam labelHoGSvmBeam;

	public PanelSegComplete0() 
	{
		jaylene = new PanelSegPanelSplitJaylene();
		labelHoGSvmBeam = new PanelSegLabelRegHoGSvmBeam();
	}
	
	@Override
	public void segment(Mat image) 
	{
		preSegment(image);	//Common initializations for all segmentation method.
//		{
//		PanelSegInfo panel = new PanelSegInfo();
//		panel.panelRect = new Rectangle(0, 0, image.cols(), image.rows());
//		figure.panelSegResult.add(panel);
//		return;
//		}

		jaylene.segment(image);
		labelHoGSvmBeam.segment(image);
		
		if (labelHoGSvmBeam.figure.panelSegResult.size() == 0)
		{	//No label so we use jaylene's result
			figure.panelSegResult = jaylene.figure.panelSegResult;
			return;
		}
		
		MergeFromLabels();
		
		if (labelHoGSvmBeam.figure.panelSegResult.size() == 0)
		{	//For some reason, no panel is generated, we create a panel, include all figure.
			PanelSegInfo panel = new PanelSegInfo();
			panel.panelRect = new Rectangle(0, 0, image.cols(), image.rows());
			labelHoGSvmBeam.figure.panelSegResult.add(panel);
		}
	}
	
	private boolean MergeFromLabels()
	{
		//To here, the labels detected are trustworthy. We find the closest panels and assign it to the corresponding label
		//We may need to break or merge panels.
		//1. Find the closet jaylene-panel to each label-panel
		int[] closest_jaylene_panel_indexes = new int[labelHoGSvmBeam.figure.panelSegResult.size()];
		for (int i = 0; i < labelHoGSvmBeam.figure.panelSegResult.size(); i++)
		{
			PanelSegInfo labelPanel = labelHoGSvmBeam.figure.panelSegResult.get(i);			Rectangle labelRect = labelPanel.labelRect;
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
				PanelSegInfo label = labelHoGSvmBeam.figure.panelSegResult.get(index);
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
			for (int i = 0; i < labelHoGSvmBeam.figure.panelSegResult.size(); i++)		indexes.add(i);
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
				PanelSegInfo label = labelHoGSvmBeam.figure.panelSegResult.get(index);
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
}
