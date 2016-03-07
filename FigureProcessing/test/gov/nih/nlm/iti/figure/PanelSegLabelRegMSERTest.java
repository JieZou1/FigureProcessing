package gov.nih.nlm.iti.figure;

public class PanelSegLabelRegMSERTest 
{
	public static void main(String[] args) throws Exception 
	{
		//Check Args
		if (args.length != 2)
		{
			System.out.println("Usage: java -jar PanelSegLabelRegMSERTest.jar <test image folder> <result image folder>");
			return;
		}
		
		PanelSegLabelRegMSER method = new PanelSegLabelRegMSER();
		
		PanelSegTest test = new PanelSegTest();
		test.Test(args[0], args[1], method);
	}
}
