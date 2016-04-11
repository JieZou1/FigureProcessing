package gov.nih.nlm.iti.figure;

import static org.bytedeco.javacpp.opencv_core.CV_8UC1;
import static org.bytedeco.javacpp.opencv_core.CV_8UC3;
import static org.bytedeco.javacpp.opencv_core.findNonZero;
import static org.bytedeco.javacpp.opencv_imgproc.boundingRect;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class AlgorithmEx 
{
	/**
	 * Very inefficient function to convert BufferImage between types
	 * @param src
	 * @param bufImgType
	 * @return
	 */
	private static BufferedImage convert(BufferedImage src, int bufImgType) 
	{
	    BufferedImage img= new BufferedImage(src.getWidth(), src.getHeight(), bufImgType);
	    Graphics2D g2d= img.createGraphics();
	    g2d.drawImage(src, 0, 0, null);
	    g2d.dispose();
	    return img;
	}
	
	/**
	 * To convert BufferedImage to Mat format. Currently, used a very inefficient method. 
	 * @param in
	 * @return Mat image in BGR format
	 */
	static Mat bufferdImg2Mat(BufferedImage in)
	{
		if (in.getType() != BufferedImage.TYPE_INT_RGB)		in = convert(in, BufferedImage.TYPE_INT_RGB);
		
		Mat out;
		byte[] data;         int r, g, b;          int height = in.getHeight(), width = in.getWidth();
		if(in.getType() == BufferedImage.TYPE_INT_RGB || in.getType() == BufferedImage.TYPE_INT_ARGB)
        {
			out = new Mat(height, width, CV_8UC3);
			data = new byte[height * width * (int)out.elemSize()];
			int[] dataBuff = in.getRGB(0, 0, width, height, null, 0, width);
			for(int i = 0; i < dataBuff.length; i++)
			{
				data[i*3 + 2] = (byte) ((dataBuff[i] >> 16) & 0xFF);
				data[i*3 + 1] = (byte) ((dataBuff[i] >> 8) & 0xFF);
				data[i*3] = (byte) ((dataBuff[i] >> 0) & 0xFF);
			}
			out.data().put(data);
        }
		else
		{
			out = new Mat(height, width,  CV_8UC1);
			data = new byte[height * width * (int)out.elemSize()];
			int[] dataBuff = in.getRGB(0, 0, width, height, null, 0, width);
			for(int i = 0; i < dataBuff.length; i++)
			{
				r = (byte) ((dataBuff[i] >> 16) & 0xFF);
                g = (byte) ((dataBuff[i] >> 8) & 0xFF);
                b = (byte) ((dataBuff[i] >> 0) & 0xFF);
                data[i] = (byte)((0.21 * r) + (0.71 * g) + (0.07 * b)); //luminosity
			}
			out.data().put(data);
		}
		return out;
    }

	/**
	 * Convert Mat image to BufferedImage <p>
	 * BufferedImage is either in TYPE_BYTE_GRAY or TYPE_INT_RGB format.
	 * @param in
	 * @return 
	 */
	static BufferedImage mat2BufferdImg(Mat in)
    {
		int width = in.cols(), height = in.rows();
        BufferedImage out;
        byte[] data = new byte[width * height * (int)in.elemSize()];
        int type;
        in.data().get(data);

        if(in.channels() == 1)
            type = BufferedImage.TYPE_BYTE_GRAY;
        else
            type = BufferedImage.TYPE_3BYTE_BGR;

        out = new BufferedImage(width, height, type);

        out.getRaster().setDataElements(0, 0, width, height, data);
        out = convert(out, BufferedImage.TYPE_INT_RGB);
//        try {
//			ImageIO.write(out, "jpg", new File("temp.jpg"));
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
        return out;
    } 

	/**
	 * return a child node of the parent node based on its tag
	 * @param parent
	 * @param tag
	 * @return
	 */
	static Node getChildNode(Node parent, String tag)
	{
		String nodeName;
		NodeList children = parent.getChildNodes();
		for (int j = 0; j < children.getLength(); j++)
		{
			Node child = children.item(j);
			nodeName = child.getNodeName();
			if (tag != nodeName) continue;
			
			return child;
		}
		return null;
	}

	/**
	 * Find the boundingbox of a binary image. White as foreground
	 * @param bina
	 * @return
	 */
	static Rect findBoundingbox(Mat bina)
	{
		Mat points = new Mat();	findNonZero(bina,points);
		Rect minRect=boundingRect(points);	
		return minRect;
	}
	
	/**
	 * Crop the ROI from the image
	 * @param image
	 * @param roi
	 * @return
	 */
	static Mat cropImage(Mat image, Rectangle roi)
	{
		Rect rect = new Rect(roi.x, roi.y, roi.width, roi.height);
		return image.apply(rect);
	}

	static int findMaxIndex(double[] array)
	{
		int maxIndex= 0; double maxValue = array[0];
		for (int i = 1; i < array.length; i++)
		{
			if (array[i] > maxValue)
			{
				maxIndex = i; maxValue = array[i];
			}
		}
		return maxIndex;
	}
}
