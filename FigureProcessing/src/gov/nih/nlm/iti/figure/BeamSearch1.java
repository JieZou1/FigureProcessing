package gov.nih.nlm.iti.figure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

class BeamSearch1 
{
	class BeamNode
	{
		double normalizedProb; 		//Use this to sort different paths
		ArrayList<Integer> labelIndexes = new ArrayList<Integer>();	//The path of label indexes to reach this node.
		ArrayList<Character> labels = new ArrayList<Character>();	//The path of labels to reach this node. For check IsLegal and for manually examine the path
		ArrayList<PanelSegInfo> patchs = new ArrayList<PanelSegInfo>();	//The path of patches to reach this node. For check IsLegal and for manually examine the path
	}
	
	/**
	 * Comparator for sorting BeamNode in reverse order of normalizedProb.
	 * @author Jie Zou
	 */
	class BeamNodeDescending implements Comparator<BeamNode>
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

	class BeamLine
	{
		double normalizedProb; 		//Use this to sort different paths
		ArrayList<ArrayList<Character>> labels = new ArrayList<ArrayList<Character>>();	//The path of labels to reach this node. For check IsLegal and for manually examine the path
		ArrayList<ArrayList<PanelSegInfo>> patchs = new ArrayList<ArrayList<PanelSegInfo>>();	//The path of patches to reach this node. For check IsLegal and for manually examine the path
	}
	
	/**
	 * Comparator for sorting BeamLine in reverse order of normalizedProb.
	 * @author Jie Zou
	 */
	class BeamLineDescending implements Comparator<BeamLine>
	{
		@Override
		public int compare(BeamLine o1, BeamLine o2) 
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

	public BeamLine Search()
	{
		linePaths = new BeamNode[nLines][][];
		lineReversePaths = new BeamNode[nLines][][];
        for (int i = 0; i < nLines; i++)
        {
        	ArrayList<PanelSegInfo> line = lines.get(i);
        	if (line.size() == 1)
        	{	//If there is only one patch in the line, we don't need to try reverse.
            	linePaths[i] = Search(lines.get(i));
        	}
        	else
        	{
            	linePaths[i] = Search(lines.get(i));
            	lineReversePaths[i] = Search(linesReverse.get(i));
        	}
        }
        
        return SearchLines();
	}

	private BeamNode[][] Search(ArrayList<PanelSegInfo> line) 
	{
		int sequenceLength = line.size();
		BeamNode[][] beamNodePaths = new BeamNode[sequenceLength][];
		
		for (int i = 0; i < sequenceLength; i++)
		{
			PanelSegInfo patch = line.get(i);
			ArrayList<BeamNode> candidates = new ArrayList<BeamNode>();
			if (i == 0)
			{
				for (int j = 0; j < labelLength; j++)
				{
					BeamNode bn = new BeamNode();
					bn.normalizedProb = patch.labelProbs[j];
					bn.labelIndexes.add(j);
					bn.patchs.add(patch);
					if (j < PanelSeg.labelToReg.length)	bn.labels.add(PanelSeg.labelToReg[j]);
					else bn.labels.add('#'); //We use # to indicate no-label patch.
					candidates.add(bn);
				}
			}
			else
			{
				BeamNode[] lastBeam = beamNodePaths[i-1];				int n = lastBeam.length;				
				for (int k = 0; k < n; k++)
				{
					for (int j = 0; j < labelLength; j++)
					{
						BeamNode bn = new BeamNode();
						bn.normalizedProb = patch.labelProbs[j] * lastBeam[k].normalizedProb;
						bn.labelIndexes.addAll(lastBeam[k].labelIndexes);		bn.labelIndexes.add(j);	
						bn.patchs.add(patch);
						if (j < PanelSeg.labelToReg.length)	bn.labels.add(PanelSeg.labelToReg[j]);
						else bn.labels.add('#'); //We use # to indicate no-label patch.
						
						if (IsLegal(bn))	candidates.add(bn);
					}					
				}
			}
			
			//Normalize candidates prob
			BeamNode[] candidatesArray = candidates.toArray(new BeamNode[0]);
			double sum = 0;
			for (int k = 0; k < candidatesArray.length; k++)	sum += candidatesArray[k].normalizedProb;
			for (int k = 0; k < candidatesArray.length; k++)	candidatesArray[k].normalizedProb /= sum;
			Arrays.sort(candidatesArray, new BeamNodeDescending());

			beamNodePaths[i] = candidatesArray.length < beamLength ? candidatesArray: Arrays.copyOfRange(candidatesArray, 0, beamLength);			
		}
		return beamNodePaths;
	}

	private BeamLine SearchLines()
	{
		BeamLine[][] beamLinePaths = new BeamLine[nLines][];
		
		for (int i = 0; i < nLines; i++)
		{
			ArrayList<BeamLine> candidates = new ArrayList<BeamLine>();
			if (i == 0)
			{	//We assume the first line must be from left to right, so we don't include linePathsReverse
				BeamNode[] paths = linePaths[i][linePaths[i].length - 1]; 
				for (int j = 0; j < paths.length; j++)
				{
					BeamLine bl = new BeamLine();
					bl.normalizedProb = paths[j].normalizedProb;
					bl.labels.add(paths[j].labels);
					bl.patchs.add(paths[j].patchs);
					candidates.add(bl);
				}
			}
			else
			{
				BeamLine[] lastLine = beamLinePaths[i-1];		int n = lastLine.length;				
				BeamNode[] paths = linePaths[i][linePaths[i].length - 1]; 
				for (int k = 0; k < n; k++)
				{
					for (int j = 0; j < paths.length; j++)
					{
						BeamLine bl = new BeamLine();
						bl.normalizedProb = paths[j].normalizedProb * lastLine[k].normalizedProb;
						bl.labels.addAll(lastLine[k].labels); bl.labels.add(paths[j].labels);
						bl.patchs.addAll(lastLine[k].patchs); bl.patchs.add(paths[j].patchs);
						
						if (IsLegal(bl))	candidates.add(bl);
					}
				}
			}
			
			//Normalize candidates prob
			BeamLine[] candidatesArray = candidates.toArray(new BeamLine[0]);
			double sum = 0;
			for (int k = 0; k < candidatesArray.length; k++)	sum += candidatesArray[k].normalizedProb;
			for (int k = 0; k < candidatesArray.length; k++)	candidatesArray[k].normalizedProb /= sum;
			Arrays.sort(candidatesArray, new BeamLineDescending());

			beamLinePaths[i] = candidatesArray.length < beamLength ? candidatesArray: Arrays.copyOfRange(candidatesArray, 0, beamLength);			
		}
		
		return beamLinePaths[beamLinePaths.length-1][0];
	}
	
	private boolean IsLegal(BeamNode bn) 
	{
		//Get label sequence
		ArrayList<Character> labels = new ArrayList<Character>();
		ArrayList<PanelSegInfo> labelPatches = new ArrayList<PanelSegInfo>();
		for (int i = 0; i < bn.labelIndexes.size(); i++)
		{
			int index = bn.labelIndexes.get(i);
			if (index == PanelSeg.labelToReg.length) continue; //This is non-label case, ignore
			
			char ch = PanelSeg.labelToReg[index];
			labels.add(ch);
			labelPatches.add(bn.patchs.get(i));
		}
		
		if (labels.size() == 0 || labels.size() == 1) return true;
		
		return IsLegal(labels, labelPatches);
	}

	private boolean IsLegal(BeamLine bl)
	{
		//Get label sequence
		ArrayList<Character> labels = new ArrayList<Character>();
		ArrayList<PanelSegInfo> labelPatches = new ArrayList<PanelSegInfo>();
		for (int i = 0; i < bl.labels.size(); i++)
		{
			for (int j = 0; j < bl.labels.get(i).size(); j++)
			{
				char ch = bl.labels.get(i).get(j);
				if (ch == '#') continue;
				labels.add(ch);
				labelPatches.add(bl.patchs.get(i).get(j));
			}
		}
		
		if (labels.size() == 0 || labels.size() == 1) return true;

		return IsLegal(labels, labelPatches);
	}

	private boolean IsLegal(ArrayList<Character> labels, ArrayList<PanelSegInfo> patches)
	{
		//Check Label Sequence
		char prevCh = labels.get(0);
		for (int i = 1; i < labels.size(); i++)
		{
			char currCh = labels.get(i);
			if (Character.toLowerCase(prevCh) >= Character.toLowerCase(currCh)) return false; //It could be in ascending order only.
			prevCh = currCh;
		}
		
		//Check overlapping Patches
		
		return true;
	}
}

