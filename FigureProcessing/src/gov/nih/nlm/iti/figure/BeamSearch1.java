package gov.nih.nlm.iti.figure;

import java.util.ArrayList;

import org.apache.commons.math3.distribution.BinomialDistribution;

class BeamSearch1 
{
	int beamLength;
	ArrayList<PanelSegInfo> nodes;
	
	int sequenceLength;
	int labelLength; 
	
	public BeamSearch1(int beamLength, ArrayList<PanelSegInfo> nodes) 
	{
		this.beamLength = beamLength;
		this.nodes = nodes;
		
		this.sequenceLength = nodes.size();
		this.labelLength = nodes.get(0).labelProbs.length;
		
		//Set label Transition Prob, in our case, we prohibit smaller letter,
		//We have the highest expectation to see the next label, and gradually smaller expectation to see the later letter.
		//We use BinomialDistribution 
		BinomialDistribution bd = new BinomialDistribution(labelLength - 1, 0.5);
	}
	
	double[][] labelTranProb;
	
	public void Search() 
	{
	}
}
