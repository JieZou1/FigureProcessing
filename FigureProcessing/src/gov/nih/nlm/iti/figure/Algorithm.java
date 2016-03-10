package gov.nih.nlm.iti.figure;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import static org.bytedeco.javacpp.opencv_core.*;

/**
 * The base class for all algorithms, which can apply to a Figure. <p>
 * 
 * This class is not intended to be instantiated, so we make it abstract. 
 * 
 * @author Jie Zou
 *
 */
abstract public class Algorithm 
{
	protected Figure figure;

	/**
	 * Very inefficient function to convert BufferImage between types
	 * @param src
	 * @param bufImgType
	 * @return
	 */
	static BufferedImage convert(BufferedImage src, int bufImgType) 
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
	public static BufferedImage mat2BufferdImg(Mat in)
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
}
