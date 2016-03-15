package gov.nih.nlm.iti.figure;

import java.awt.Rectangle;

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
}
