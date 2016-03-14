package gov.nih.nlm.iti.figure;

import java.nio.file.Path;

/**
 * The base class for all panel segmentation Training routines. <p>
 * 
 * This class is not intended to be instantiated, so we make it abstract. 
 * 
 * @author Jie Zou
 *
 */
public abstract class PanelSegTrainMethod 
{
	public abstract void Train(Path imageFilePath, Path resultFolder);
}
