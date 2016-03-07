package gov.nih.nlm.iti.figure;

public class PanelSegJayleneTest 
{
	
	
	public static void main(String[] args) throws Exception 
	{
		//Check Args
		if (args.length != 2)
		{
			System.out.println("Usage: java -jar PanelSegJayleneTest.jar <test image folder> <result image folder>");
			return;
		}
		
		PanelSegJaylene method = new PanelSegJaylene();
		
		PanelSegTest test = new PanelSegTest();
		test.Test(args[0], args[1], method);
	}
}
