package gov.nih.nlm.iti.figure;

public class PanelSegSantoshTest 
{
	public static void main(String[] args) throws Exception 
	{
		//Check Args
		if (args.length != 2)
		{
			System.out.println("Usage: java -jar PanelSegSantoshTest.jar <test image folder> <result image folder>");
			return;
		}
		
		PanelSegSantosh method = new PanelSegSantosh();
		
		PanelSegTest test = new PanelSegTest();
		test.Test(args[0], args[1], method);
	}

}
