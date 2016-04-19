package gov.nih.nlm.iti.figure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

class BeamSearch1 
{
	class BeamNode
	{
		double normalizedProb; 		//Use this to sort different paths
		ArrayList<Integer> path = new ArrayList<Integer>();	//The path to reach this node. 
	}
	
	/**
	 * Comparator for sorting SearchNode in reverse order of normalizedProb.
	 * @author Jie Zou
	 */
	class SearchNodeDescending implements Comparator<BeamNode>
	{
		@Override
		public int compare(BeamNode o1, BeamNode o2) 
		{
			double diff = o2.normalizedProb - o1.normalizedProb;
			if (diff > 0) return 1;
			else if (diff == 0) return 0;
			else return -1;
		}		
	}

	int nLines;
	int beamLength;				
	int labelLength;
	
	//static double[][] labelTransProbs = null; //Label Transition Probs; 
	
	ArrayList<ArrayList<PanelSegInfo>> lines; //The patch lines to be labeled (beam searched).
	ArrayList<ArrayList<PanelSegInfo>> linesReverse; //The patch lines to be labeled (beam searched).
	BeamNode[][][] linePaths;
	BeamNode[][][] lineReversePaths;
	
	public BeamSearch1(int beamLength, ArrayList<ArrayList<PanelSegInfo>> lines) 
	{
		this.beamLength = beamLength;
		
		this.nLines = lines.size();
		this.lines = new ArrayList<ArrayList<PanelSegInfo>>();
		this.linesReverse = new ArrayList<ArrayList<PanelSegInfo>>();
		
		for (int i = 0; i < nLines; i++) 
		{
			ArrayList<PanelSegInfo> line = lines.get(i);
        	line.sort(new LabelRectLeftAscending()); //In each line, sort according to their left
        	
        	this.lines.add(line);
        
        	ArrayList<PanelSegInfo> lineReverse = new ArrayList<PanelSegInfo>();
    		for (int k = line.size() - 1; k >= 0; k--) lineReverse.add(line.get(k));
        	
    		this.linesReverse.add(lineReverse);
		}
			
		this.labelLength = lines.get(0).get(0).labelProbs.length;
		
		//Set label Transition Prob, in our case, we prohibit smaller letter,
		//We have the highest expectation to see the next label, and gradually smaller expectation to see the later letter.
//		if (labelTransProbs == null)
//		{
//			labelTransProbs = new double[labelLength][];	//Last one is transition to "Non-Label" patch
//			for (int i = 0; i < labelLength; i++)
//			{
//				labelTransProbs[i] = new double[labelLength];
//				if (i == labelLength - 1)
//				{	//Uniform distrubution is used for transition from "Non-Label" to all other labels.
//					for (int j = 0; j < labelLength; j++)	labelTransProbs[i][j] = 1.0/labelLength;
//				}
//				else
//				{
//					char ch = PanelSeg.labelToReg[i]; 
//					for (int j = 0; j < labelLength; j++)
//					{
//						char nextCh = PanelSeg.labelToReg[j];
//
//						
//						
//						if (j <= i) labelTransProbs[i][j] = 0; //The label can only increase in a line
//						else if (j == i + 1) labelTransProbs[i][j] = 1; //The next label has the highest prob
//						else if (j == labelLength - 1) labelTransProbs[i][j] = 1; //Transition to "Non-Label" is also high
//						else labelTransProbs[i][j] = 1.0/(j - i);
//					}
//				}
//			}
//		}
	}

	public void Search()
	{
		linePaths = new BeamNode[nLines][][];
		lineReversePaths = new BeamNode[nLines][][];
        for (int i = 0; i < nLines; i++)
        {
        	linePaths[i] = Search(lines.get(i));
        	lineReversePaths[i] = Search(linesReverse.get(i));
        }
        
        SearchLines();
	}

	private BeamNode[][] Search(ArrayList<PanelSegInfo> line) 
	{
		int sequenceLength = line.size();
		BeamNode[][] paths = new BeamNode[sequenceLength][];
		
		for (int i = 0; i < sequenceLength; i++)
		{
			ArrayList<BeamNode> candidates = new ArrayList<BeamNode>();
			if (i == 0)
			{
				for (int j = 0; j < labelLength; j++)
				{
					BeamNode sb = new BeamNode();
					sb.normalizedProb = line.get(i).labelProbs[j];
					sb.path.add(j);
					candidates.add(sb);
				}
			}
			else
			{
				BeamNode[] lastBeam =paths[i-1];				int n = lastBeam.length;				
				for (int k = 0; k < n; k++)
				{
					for (int j = 0; j < labelLength; j++)
					{
						BeamNode sb = new BeamNode();
						sb.normalizedProb = line.get(i).labelProbs[j] * lastBeam[k].normalizedProb;
						sb.path.addAll(lastBeam[k].path);		sb.path.add(j);	
						if (IsLegal(sb))
							candidates.add(sb);
					}					
				}
			}
			
			//Normalize candidates prob
			BeamNode[] candidatesArray = candidates.toArray(new BeamNode[0]);
			double sum = 0;
			for (int k = 0; k < candidatesArray.length; k++)	sum += candidatesArray[k].normalizedProb;
			for (int k = 0; k < candidatesArray.length; k++)	candidatesArray[k].normalizedProb /= sum;
			Arrays.sort(candidatesArray, new SearchNodeDescending());

			paths[i] = candidatesArray.length < beamLength ? candidatesArray: Arrays.copyOfRange(candidatesArray, 0, beamLength);			
		}
		return paths;
	}

	private void SearchLines()
	{
		
	}
	
	private boolean IsLegal(BeamNode sb) 
	{
		//Get label sequence
		ArrayList<Character> chSequence = new ArrayList<Character>();
		for (int i = 0; i < sb.path.size(); i++)
		{
			int index = sb.path.get(i);
			if (index == PanelSeg.labelToReg.length) continue; //This is non-label case, ignore
			char ch = PanelSeg.labelToReg[index];
			chSequence.add(ch);
		}
		
		if (chSequence.size() == 0 || chSequence.size() == 1) return true;
		
		//Check Label Sequence
		char prevCh = chSequence.get(0);
		for (int i = 1; i < chSequence.size(); i++)
		{
			char currCh = chSequence.get(i);
			if (Character.toLowerCase(prevCh) >= Character.toLowerCase(currCh)) return false; //It could be in ascending order only.
			prevCh = currCh;
		}
		
		return true;
	}

	
}
