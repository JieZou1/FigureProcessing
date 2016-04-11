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
 *
 */
class ScoreComp implements Comparator<PanelSegInfo>
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

