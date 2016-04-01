package gov.nih.nlm.iti.figure;

import java.io.IOException;
import java.util.concurrent.RecursiveAction;

/**
 * Implementing multi-threading processing in Fork/Join framework for Panel Segmentation Evaluation
 * 
 * It takes a PanelSegEval, and uses divide-and-conquer strategy to run multi-tasks in commonPool
 * 
 * @author Jie Zou
 *
 */
class PanelSegEvalTask extends RecursiveAction
{
	private static final long serialVersionUID = 1L;

	int seqThreshold;
	
	PanelSegEval segEval;	int start, end;
	
	PanelSegEvalTask(PanelSegEval segEval, int start, int end, int seqThreshold) 
	{
		this.segEval = segEval;		this.start = start;		this.end = end; this.seqThreshold = seqThreshold;
	}
	
	@Override
	protected void compute()
	{
		if (end - start < seqThreshold)
		{
			for (int i = start; i < end; i++)
			{
				try {
					segEval.segment(i);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		else
		{
			int middle = (start + end)/2;
			invokeAll(	new PanelSegEvalTask(segEval, start, middle, this.seqThreshold), 
						new PanelSegEvalTask(segEval, middle, end, this.seqThreshold));
		}
	}
}

