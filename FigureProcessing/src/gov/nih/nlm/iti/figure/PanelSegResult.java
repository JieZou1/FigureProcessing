package gov.nih.nlm.iti.figure;

import java.awt.Rectangle;

/**
 * A simple class for holding panel segmentation result. We want to make this simple. 
 * Put all complicated algorithm related stuffs into the algorithm classes.  
 * All other panel related information should use Panel class. 
 * The major reason for this is to separate data and algorithms. 
 * Such that we could have a clean data structure for result, not embedded in the various actual algorithms. 
 * This also makes serialization to XML much easier. 
 * 
 * @author Jie Zou
 *
 */
public class PanelSegResult 
{
	Rectangle panelRect;
	
	char panelLabel;
	Rectangle labelRect;
	double labelScore;
}
