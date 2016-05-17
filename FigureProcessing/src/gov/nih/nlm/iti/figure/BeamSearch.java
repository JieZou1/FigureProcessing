package gov.nih.nlm.iti.figure;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import org.apache.commons.lang3.ArrayUtils;

class BeamSearch 
{
	/**
	 * A helper class for Beam Search, representing a beam node.
	 * Holding a path up to this particular node (Detected and Classified patch) in a line. 
	 * @author Jie Zou
	 *
	 */
	class BeamNode
	{
		double logProb; 		//Use this to sort different paths
		ArrayList<Integer> labelIndexes = new ArrayList<Integer>();	//The path of label indexes to reach this node.
		ArrayList<Character> labels = new ArrayList<Character>();	//The path of labels to reach this node. For check IsLegal and for manually examine the path
		ArrayList<PanelSegInfo> patchs = new ArrayList<PanelSegInfo>();	//The path of patches to reach this node. For check IsLegal and for manually examine the path
	}
	
	/**
	 * Comparator for sorting BeamNode in descending order of logProb.
	 * @author Jie Zou
	 */
	class BeamNodeDescending implements Comparator<BeamNode>
	{
		@Override
		public int compare(BeamNode o1, BeamNode o2) 
		{
			double diff = o2.logProb - o1.logProb;
			if (diff > 0) return 1;
			else if (diff == 0) return 0;
			else return -1;
		}		
	}

	/**
	 * A helper class for Beam Search, representing beam lines.
	 * Holding a path to the current line.
	 * @author Jie Zou
	 *
	 */
	class BeamLines
	{
		double logProb; 		//Use this to sort different paths
		ArrayList<ArrayList<Character>> labels = new ArrayList<ArrayList<Character>>();	//The path of labels to reach this node. For check IsLegal and for manually examine the path
		ArrayList<ArrayList<PanelSegInfo>> patchs = new ArrayList<ArrayList<PanelSegInfo>>();	//The path of patches to reach this node. For check IsLegal and for manually examine the path
	}
	
	/**
	 * Comparator for sorting BeamLine in descending order of logProb.
	 * @author Jie Zou
	 */
	class BeamLineDescending implements Comparator<BeamLines>
	{
		@Override
		public int compare(BeamLines o1, BeamLines o2) 
		{
			double diff = o2.logProb - o1.logProb;
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
	BeamNode[][] lineCandidatePaths;	//The candidate paths collected by beam search in each line.
	BeamLines[][] beamLinePaths;			//The final path candidate after combining lines

	
	public BeamSearch(int beamLength) 
	{
		this.beamLength = beamLength;		
	}

	/**
	 * Do Beam Search for all the lines
	 * @param lns The lines
	 * @param vertical Whether they are vertical or horizontal lines.
	 * @return
	 */
	public BeamLines search(ArrayList<ArrayList<PanelSegInfo>> lns, boolean vertical)
	{
		this.nLines = lns.size();
		this.lines = new ArrayList<ArrayList<PanelSegInfo>>();
		this.linesReverse = new ArrayList<ArrayList<PanelSegInfo>>();
		
		for (int i = 0; i < nLines; i++) 
		{
			ArrayList<PanelSegInfo> line = lns.get(i);
			if (vertical)  	line.sort(new LabelRectTopAscending()); //In each line, sort according to their Top
			else			line.sort(new LabelRectLeftAscending()); //In each line, sort according to their Left
        	
        	this.lines.add(line);
        
        	ArrayList<PanelSegInfo> lineReverse = new ArrayList<PanelSegInfo>();
    		for (int k = line.size() - 1; k >= 0; k--) lineReverse.add(line.get(k));
        	
    		this.linesReverse.add(lineReverse);
		}
			
		this.labelLength = lines.get(0).get(0).labelProbs.length;
		
		lineCandidatePaths = new BeamNode[nLines][];
        for (int i = 0; i < nLines; i++)
        {
        	ArrayList<PanelSegInfo> line = lines.get(i);
        	if (i == 0 || line.size() == 1)
        	{	//If there is only one patch in the line, we don't need to try reverse.
        		//We also assume the first line has to be from left to right or from top to bottom
        		BeamNode[][] path = searchInLine(lines.get(i));
        		lineCandidatePaths[i] = path[path.length - 1];
        	}
        	else
        	{
        		BeamNode[][] path = searchInLine(lines.get(i));
        		BeamNode[][] reversePath = searchInLine(linesReverse.get(i));

        		lineCandidatePaths[i] = ArrayUtils.addAll(path[path.length - 1], reversePath[reversePath.length - 1]); 
    			Arrays.sort(lineCandidatePaths[i], new BeamNodeDescending());
        	}
        }
        
        return SearchLines();
	}

	/**
	 * Beam Search within a line
	 * @param line
	 * @return
	 */
	private BeamNode[][] searchInLine(ArrayList<PanelSegInfo> line) 
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
					bn.logProb = Math.log(patch.labelProbs[j]);
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
						bn.logProb = Math.log(patch.labelProbs[j]) + lastBeam[k].logProb;
						bn.labelIndexes.addAll(lastBeam[k].labelIndexes);		bn.labelIndexes.add(j);	
						bn.patchs.addAll(lastBeam[k].patchs); bn.patchs.add(patch);
						bn.labels.addAll(lastBeam[k].labels); 
						if (j < PanelSeg.labelToReg.length)	bn.labels.add(PanelSeg.labelToReg[j]);
						else bn.labels.add('#'); //We use # to indicate no-label patch.
						
						if (!IsLegal(bn)) continue;
						
						candidates.add(bn);
					}					
				}
			}
			
			BeamNode[] candidatesArray = candidates.toArray(new BeamNode[0]);
			Arrays.sort(candidatesArray, new BeamNodeDescending());

			beamNodePaths[i] = candidatesArray.length < beamLength ? candidatesArray : Arrays.copyOfRange(candidatesArray, 0, beamLength);			
		}
		return beamNodePaths;
	}

	/**
	 * Beam Search of all possible lines
	 * @return
	 */
	private BeamLines SearchLines()
	{
		beamLinePaths = new BeamLines[nLines][];
		
		for (int i = 0; i < nLines; i++)
		{
			ArrayList<BeamLines> candidates = new ArrayList<BeamLines>();
			if (i == 0)
			{	
				BeamNode[] paths = lineCandidatePaths[i]; 
				for (int j = 0; j < paths.length; j++)
				{
					BeamLines bl = new BeamLines();
					bl.logProb = paths[j].logProb;
					bl.labels.add(paths[j].labels);
					bl.patchs.add(paths[j].patchs);
					
					if (IsLegal(bl)) 	candidates.add(bl);
				}
			}
			else
			{
				BeamLines[] lastLine = beamLinePaths[i-1];		int n = lastLine.length;				
				BeamNode[] paths = lineCandidatePaths[i]; 
				for (int k = 0; k < n; k++)
				{
					for (int j = 0; j < paths.length; j++)
					{
						BeamLines bl = new BeamLines();
						bl.logProb = paths[j].logProb + lastLine[k].logProb;
						bl.labels.addAll(lastLine[k].labels); bl.labels.add(paths[j].labels);
						bl.patchs.addAll(lastLine[k].patchs); bl.patchs.add(paths[j].patchs);
						
						if (IsLegal(bl))	candidates.add(bl);
					}
				}
			}
			
			BeamLines[] candidatesArray = candidates.toArray(new BeamLines[0]);
			Arrays.sort(candidatesArray, new BeamLineDescending());

			beamLinePaths[i] = candidatesArray.length < beamLength ? candidatesArray: Arrays.copyOfRange(candidatesArray, 0, beamLength);			
		}
		
		if (beamLinePaths[beamLinePaths.length-1].length == 0) return null;
		
		return beamLinePaths[beamLinePaths.length-1][0];
	}
	
	/**
	 * Check whether a single line path is legal.
	 * 
	 * @param bn
	 * @return
	 */
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
		
		return IsLegal(labels, labelPatches, false);
	}

	/**
	 * Check whether a multiple line path is legal
	 * @param bl
	 * @return
	 */
	private boolean IsLegal(BeamLines bl)
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
		
		if (labels.size() == 0) return true;

		return IsLegal(labels, labelPatches, true);
	}

	/**
	 * Check whether the label sequence is legitimate.
	 * This function shared by both inline and multiline IsLegal Functions.
	 * @param labels
	 * @param patches
	 * @return
	 */
	private boolean IsLegal(ArrayList<Character> labels, ArrayList<PanelSegInfo> patches, Boolean multiplelines)
	{
		{//*** We assume labels are continuous and in ascending order only and allow at most missing 2 labels.
			char prevCh = Character.toLowerCase(labels.get(0));
			if (multiplelines)
			{
				if (prevCh - 'a' > 2) return false;
			}
			for (int i = 1; i < labels.size(); i++)
			{
				char currCh = Character.toLowerCase(labels.get(i));
				if (currCh - prevCh <= 0) return false;	//Has to be in ascending order 
				if (currCh - prevCh > 2) return false; 	//Has to be within missing 3 labels
				prevCh = currCh;
			}
		}
		
		//*** Check overlapping patches, we don't allow the label patches to be overlapping
		for (int i = 0; i < patches.size(); i++)
		{
			Rectangle rect1 = patches.get(i).labelRect;
			for (int j = i+1; j < patches.size(); j++)
			{
				Rectangle rect2 = patches.get(j).labelRect;
				if (rect1.intersects(rect2)) return false;
			}
		}
		
		
		// *** If 'A' or 'a' is in the labels, all other patches must not be at the top-left of 'a' or 'A'
		char firstCh = labels.get(0);
		if (firstCh == 'a' || firstCh == 'A')
		{
			Rectangle aRectangle = patches.get(0).labelRect;
			for (int i = 1; i < patches.size(); i++)
			{
				Rectangle rectangle = patches.get(i).labelRect;
				if (rectangle.y + rectangle.height < aRectangle.y &&
					rectangle.x + rectangle.width < aRectangle.x) 
					return false;
			}
		}
		
		return true;
	}
}

