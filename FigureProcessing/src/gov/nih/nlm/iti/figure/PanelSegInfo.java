package gov.nih.nlm.iti.figure;

import java.awt.Rectangle;
import java.util.Comparator;

/**
 * A simple class for holding panel segmentation result. We want to make this simple. 
 * Put all complicated algorithm related stuffs into the algorithm classes.  
 * The major reason for this is to separate data and algorithms. 
 * Such that we could have a clean data structure for result, not embedded in the various actual algorithms. 
 * This also makes serialization to XML much easier. 
 * 
 * @author Jie Zou
 *
 */
public class PanelSegInfo 
{
	Rectangle panelRect;
	
	String panelLabel;
	Rectangle labelRect;
	double labelScore;
	
	double[] labelProbs;	//The posterior probabilities of all possible classes. Mostly used to find an optimal label set.
	
	boolean labelInverted; //label is inverted if the character is black. Otherwise it is not inverted.
}

/**
 * Comparator for sorting PanelSegInfo in reverse order of labelScore.
 * @author Jie Zou
 */
class LabelScoreDescending implements Comparator<PanelSegInfo>
{
	@Override
	public int compare(PanelSegInfo o1, PanelSegInfo o2) 
	{
		double diff = o2.labelScore - o1.labelScore;
		if (diff > 0) return 1;
		else if (diff == 0) return 0;
		else return -1;
	}
	
}

/**
* Comparator for sorting PanelSegInfo vertically based on the LabelRect.Left
* @author Jie Zou
*/
class LabelRectLeftAscending implements Comparator<PanelSegInfo>
{
	@Override
	public int compare(PanelSegInfo o1, PanelSegInfo o2) 
	{
		double diff = o1.labelRect.x - o2.labelRect.x;
		if (diff > 0) return 1;
		else if (diff == 0) return 0;
		else return -1;
	}
	
}

/**
 * Comparator for sorting PanelSegInfo horizontally based on the LabelRect.Top
 * @author Jie Zou
 */
class LabelRectTopAscending implements Comparator<PanelSegInfo>
{
	@Override
	public int compare(PanelSegInfo o1, PanelSegInfo o2) 
	{
		double diff = o1.labelRect.y - o2.labelRect.y;
		if (diff > 0) return 1;
		else if (diff == 0) return 0;
		else return -1;
	}
	
}

class PanelLabelAscending implements Comparator<PanelSegInfo>
{
	@Override
	public int compare(PanelSegInfo o1, PanelSegInfo o2) 
	{
		int diff = o1.panelLabel.compareTo(o2.panelLabel);
		if (diff > 0) return 1;
		else if (diff == 0) return 0;
		else return -1;
	}
}