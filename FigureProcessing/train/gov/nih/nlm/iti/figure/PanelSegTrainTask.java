package gov.nih.nlm.iti.figure;

import java.util.concurrent.RecursiveAction;

/**
 * Implementing multi-threading processing in Fork/Join framework for Panel Segmentation Training
 * 
 * It takes a PanelSegTrainMethod, and uses divide-and-conquer strategy to run multi-tasks in commonPool
 * 
 * @author Jie Zou
 *
 */
public class PanelSegTrainTask extends RecursiveAction
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	int seqThreshold;
	
	PanelSegTrain seqTrain;	int start, end;
	
	PanelSegTrainTask(PanelSegTrain segTrain, int start, int end, int seqThreshold) 
	{
		this.seqTrain = segTrain;		this.start = start;		this.end = end; this.seqThreshold = seqThreshold;
	}
	
	@Override
	protected void compute()
	{
		if (end - start < seqThreshold)
		{
			for (int i = start; i < end; i++)
			{
				seqTrain.train(i);
			}
		}
		else
		{
			int middle = (start + end)/2;
			invokeAll(	new PanelSegTrainTask(seqTrain, start, middle, this.seqThreshold), 
						new PanelSegTrainTask(seqTrain, middle, end, this.seqThreshold));
		}
	}

}
