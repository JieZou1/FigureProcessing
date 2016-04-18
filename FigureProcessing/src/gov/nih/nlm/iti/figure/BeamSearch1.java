package gov.nih.nlm.iti.figure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

class BeamSearch1 
{
	class SearchBeam
	{
		double normalizedProb; 		//Use this to sort different paths
		ArrayList<Integer> path = new ArrayList<Integer>();	//The path to reach this node. 
	}
	
	/**
	 * Comparator for sorting SearchNode in reverse order of normalizedProb.
	 * @author Jie Zou
	 */
	class SearchNodeDescending implements Comparator<SearchBeam>
	{
		@Override
		public int compare(SearchBeam o1, SearchBeam o2) 
		{
			double diff = o2.normalizedProb - o1.normalizedProb;
			if (diff > 0) return 1;
			else if (diff == 0) return 0;
			else return -1;
		}		
	}
	
	int beamLength;
	ArrayList<PanelSegInfo> line;
	
	int sequenceLength;
	int labelLength;

	static double[][] labelTransProbs = null; //Label Transition Probs
	
	SearchBeam[][] paths;	//The paths that we keep track of during the beam search
	
	public BeamSearch1(int beamLength, ArrayList<PanelSegInfo> line) 
	{
		this.beamLength = beamLength;
		this.line = line;
		
		this.sequenceLength = line.size();
		this.labelLength = line.get(0).labelProbs.length;
		
		//Set label Transition Prob, in our case, we prohibit smaller letter,
		//We have the highest expectation to see the next label, and gradually smaller expectation to see the later letter.
		if (labelTransProbs == null)
		{
			labelTransProbs = new double[labelLength][];	//Last one is transition to "Non-Label" patch
			for (int i = 0; i < labelLength; i++)
			{
				labelTransProbs[i] = new double[labelLength];
				if (i == labelLength - 1)
				{	//Uniform distrubution is used for transition from "Non-Label" to all other labels.
					for (int j = 0; j < labelLength; j++)	labelTransProbs[i][j] = 1.0/labelLength;
				}
				else
				{
					char ch = PanelSeg.labelToReg[i]; 
					for (int j = 0; j < labelLength; j++)
					{
						char nextCh = PanelSeg.labelToReg[j];

						
						
						if (j <= i) labelTransProbs[i][j] = 0; //The label can only increase in a line
						else if (j == i + 1) labelTransProbs[i][j] = 1; //The next label has the highest prob
						else if (j == labelLength - 1) labelTransProbs[i][j] = 1; //Transition to "Non-Label" is also high
						else labelTransProbs[i][j] = 1.0/(j - i);
					}
				}
			}
		}
	}
	
	public void Search() 
	{
		paths = new SearchBeam[sequenceLength][];
		for (int i = 0; i < sequenceLength; i++)
		{
			SearchBeam[] candidates = null;
			if (i == 0)
			{
				candidates = new SearchBeam[labelLength];
				for (int j = 0; j < labelLength; j++)
				{
					candidates[j].normalizedProb = line.get(i).labelProbs[j];
					candidates[j].path.add(j);
				}
			}
			else
			{
				SearchBeam[] lastBeam =paths[i-1];
				int n = lastBeam.length;				
				candidates = new SearchBeam[labelLength * n];
				for (int k = 0; k < n; k++)
				{
					int lastLabel = lastBeam[k].path.get(lastBeam[k].path.size() - 1);
					for (int j = 0; j < labelLength; j++)
					{
						double labelTranProb = labelTransProbs[lastLabel][j];
						candidates[k*labelLength + j].normalizedProb = line.get(i).labelProbs[j] * lastBeam[k].normalizedProb * labelTranProb;
						candidates[k*labelLength + j].path = new ArrayList<Integer>();
						candidates[k*labelLength + j].path.addAll(lastBeam[k].path);
						candidates[k*labelLength + j].path.add(j);						
					}					
				}
				//Normalize candidates prob
				double sum = 0;
				for (int k = 0; k < candidates.length; k++)	sum += candidates[k].normalizedProb;
				for (int k = 0; k < candidates.length; k++)	candidates[k].normalizedProb /= sum;
			}
			
			Arrays.sort(candidates, new SearchNodeDescending());
			
			ArrayList<SearchBeam> updatedCandidates = new ArrayList<SearchBeam>();
			for (int k = 0; k < candidates.length; k++)
			{
				if (k == beamLength) break;
				if (candidates[k].normalizedProb == 0) break;
				updatedCandidates.add(candidates[k]);
			}
			paths[i] = updatedCandidates.toArray( new SearchBeam[0]);
		}		
	}
}
