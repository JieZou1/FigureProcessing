package gov.nih.nlm.iti.figure;

import static org.bytedeco.javacpp.opencv_imgcodecs.CV_LOAD_IMAGE_COLOR;
import static org.bytedeco.javacpp.opencv_imgcodecs.imread;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.cvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.rectangle;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang3.ArrayUtils;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.w3c.dom.*;


enum Orientation { Horizontal, Vertical }

/**
 * The base class for all panel segmentation algorithms, which can apply to a Figure. <p>
 * 
 * This class is not intended to be instantiated, so we make it abstract. 
 * 
 * @author Jie Zou
 *
 */
public abstract class PanelSeg extends gov.nih.nlm.iti.figure.Algorithm 
{
	/**
	 * Some common initialization functions for all extended panel segmentation algorithms, including: <p>
	 * 1. Construct Figure object, figure <p>
	 * 2. Generate gray image, imageGray <p>
	 * 3. Construct segmentation result, segmentationResult.
	 * 
	 * Generally, the segment function of all extended classes should call this super class function 
	 * @param image
	 */
	public void segment(Mat image) 
	{
		figure = new Figure(image);	//Construct a figure object for saving processing results
		figure.imageGray = new Mat();		cvtColor(figure.image, figure.imageGray, CV_BGR2GRAY);
		figure.segmentationResult = new ArrayList<PanelSegInfo>();		
	}
	/**
	 * The entrance function to perform panel segmentation. <p>
	 * It simply loads the image from the file, and then calls segment(Mat image) function.
	 * Call getSegmentationResult* functions to retrieve result in different format.
	 */
	public void segment(String image_file_path) 
	{
		Mat image = imread(image_file_path, CV_LOAD_IMAGE_COLOR);
		segment(image);		
	}
	
	/**
	 * The entrance function to perform segmentation.
	 * Call getSegmentationResult* functions to retrieve result in different format.
	 * It simply converts the buffered image to Mat, and then calls segment(Mat image) function.
	 * 
	 * NOTICE: because converting from BufferedImage to Mat requires actual copying of the image data, it is inefficient.  
	 * It is recommended to avoid using this function if Mat type can be used.
	 *  
	 */
	public void segment(BufferedImage buffered_image) throws Exception
	{
		Mat image = Algorithm.bufferdImg2Mat(buffered_image);
		segment(image);
	}
	
	/**
	 * Get the panel segmentation result
	 * @return The detected panels
	 */
	public ArrayList<PanelSegInfo> getSegmentationResult()	{	return figure.segmentationResult;	}

	/**
	 * Get the panel segmentation result by drawing the panel boundaries on the image
	 * @return the image with panel boundaries superimposed on it.
	 */
	public Mat getSegmentationResultInMat()
	{
		Mat img = figure.image.clone();
		for (PanelSegInfo panel : figure.segmentationResult)
		{
			if (panel.panelRect != null )
			{
				Rectangle rect = panel.panelRect;
				rectangle(img, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), Scalar.RED, 3, 8, 0);
			}
			if (panel.labelRect != null)
			{
				Rectangle rect = panel.labelRect;
				rectangle(img, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), Scalar.BLUE, 1, 8, 0);
			}
		}

		return img;		
	}
	
	/**
	 * Load Ground Truth Annotations of panel segmentation
	 * @param xml_file
	 * @return
	 * @throws Exception
	 */
	static ArrayList<PanelSegInfo> LoadPanelSegGt(String gt_xml_file) throws Exception
	{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(gt_xml_file);
		
		ArrayList<PanelSegInfo> panels = new ArrayList<PanelSegInfo>();
		ArrayList<Rectangle> labelRects = new ArrayList<Rectangle>();
		ArrayList<String> labelNames = new ArrayList<String>();
		
		NodeList shapeNodes = doc.getElementsByTagName("Shape");
		
		for (int i = 0; i < shapeNodes.getLength(); i++)
		{
			Node shapeNode = shapeNodes.item(i);
			Node blockTextNode = getChildNode(shapeNode, "BlockText");
			Node textNode = getChildNode(blockTextNode, "Text");
			String text = textNode.getTextContent().toLowerCase();
			
			if (text.startsWith("panel "))
			{	//It is a panel
				Node dataNode = getChildNode(shapeNode, "Data");
				Node extentNode = getChildNode(dataNode, "Extent");
				NamedNodeMap attributes = extentNode.getAttributes();
				int x = (int)(Double.parseDouble(attributes.getNamedItem("X").getTextContent()));
				int y = (int)(Double.parseDouble(attributes.getNamedItem("Y").getTextContent()));
				int width = (int)(Double.parseDouble(attributes.getNamedItem("Width").getTextContent()));
				int height = (int)(Double.parseDouble(attributes.getNamedItem("Height").getTextContent()));
				
				PanelSegInfo panel = new PanelSegInfo();
				panel.panelRect = new Rectangle(x, y, width + 1, height + 1); //Looks like that iPhotoDraw uses [] for range instead of [)
				String[] words = text.split("\\s+"); 
				panel.panelLabel = String.join(" ", ArrayUtils.remove(words, 0));
				panels.add(panel);
			}
			else
			{	//It is a label
				Node dataNode = getChildNode(shapeNode, "Data");
				Node extentNode = getChildNode(dataNode, "Extent");
				NamedNodeMap attributes = extentNode.getAttributes();
				int x = (int)(Double.parseDouble(attributes.getNamedItem("X").getTextContent()));
				int y = (int)(Double.parseDouble(attributes.getNamedItem("Y").getTextContent()));
				int width = (int)(Double.parseDouble(attributes.getNamedItem("Width").getTextContent()));
				int height = (int)(Double.parseDouble(attributes.getNamedItem("Height").getTextContent()));
			
				Rectangle labelRect = new Rectangle(x, y, width + 1, height + 1); //Looks like that iPhotoDraw uses [] for range instead of [)
				labelRects.add(labelRect);
				String[] words = text.split("\\s+"); 
				labelNames.add(String.join(" ", ArrayUtils.remove(words, 0)));				
			}			
		}
		
		//Match labels to panels
		for (int i = 0; i < labelRects.size(); i++)
		{
			String labelName = labelNames.get(i);			
			boolean found = false;
			for (int j = 0; j < panels.size(); j++)
			{
				String panelName = panels.get(j).panelLabel;
				if (labelName.equals(panelName))
				{
					panels.get(j).labelRect = labelRects.get(i);
					found = true;
					break;
				}
			}
			
			if (found) continue;
			
			//Not found by matching labels, we check with intersections
			Rectangle labelRect = labelRects.get(i);
			for (int j = 0; j < panels.size(); j++)
			{
				Rectangle panelRect = panels.get(j).panelRect;
				if (panelRect.intersects(labelRect))
				{
					panels.get(j).labelRect = labelRect;
					panels.get(j).panelLabel = labelName;
					found = true;
					break;
				}
			}
			
			if (found) continue;
			
			//Not found by matching labels, and checking intersections, we check with union
			int min_area = Integer.MAX_VALUE; int min_j = -1;
			for (int j = 0; j < panels.size(); j++)
			{
				Rectangle panelRect = panels.get(j).panelRect;
				Rectangle union = panelRect.union(labelRect);
				int area = union.width * union.height;
				if (area < min_area)
				{
					min_area = area; min_j = j;
				}
			}
			panels.get(min_j).labelRect = labelRect;
			panels.get(min_j).panelLabel = labelName;
		}
		
		return panels;
	}
	
	/**
	 * Load Panel Segmentation result, for evaluation purposes.
	 * @param xml_file
	 * @return
	 * @throws Exception
	 */
	static PanelSegInfo[] loadPanelSegResult(String xml_file) throws Exception
	{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(xml_file);

		NodeList panelNodes = doc.getElementsByTagName("gov.nih.nlm.iti.figure.PanelSegResult");
		PanelSegInfo[] panels = new PanelSegInfo[panelNodes.getLength()];

		for (int i = 0; i < panelNodes.getLength(); i++)
		{
			Node panelNode = panelNodes.item(i);
			PanelSegInfo panel = new PanelSegInfo();
			
			Node panelRectNode = getChildNode(panelNode, "panelRect");
			if (panelRectNode != null)
			{
				Node xNode = getChildNode(panelRectNode, "x");
				Node yNode = getChildNode(panelRectNode, "y");
				Node widthNode = getChildNode(panelRectNode, "width");
				Node heightNode = getChildNode(panelRectNode, "height");
				
				int x = Integer.parseInt(xNode.getTextContent());
				int y = Integer.parseInt(yNode.getTextContent());
				int width = Integer.parseInt(widthNode.getTextContent());
				int height = Integer.parseInt(heightNode.getTextContent());
				panel.panelRect = new Rectangle(x, y, width, height);
			}
			
			Node panelLabelNode = getChildNode(panelNode, "panelLabel");
			if (panelLabelNode != null)
			{
				panel.panelLabel = panelLabelNode.getTextContent();
			}
			
			Node labelRectNode = getChildNode(panelNode, "labelRect");
			if (labelRectNode != null)
			{
				Node xNode = getChildNode(labelRectNode, "x");
				Node yNode = getChildNode(labelRectNode, "y");
				Node widthNode = getChildNode(labelRectNode, "width");
				Node heightNode = getChildNode(labelRectNode, "height");
				
				int x = Integer.parseInt(xNode.getTextContent());
				int y = Integer.parseInt(yNode.getTextContent());
				int width = Integer.parseInt(widthNode.getTextContent());
				int height = Integer.parseInt(heightNode.getTextContent());
				panel.labelRect = new Rectangle(x, y, width, height);
			}
			
			Node labelScoreNode = getChildNode(panelNode, "labelScore");
			if (labelScoreNode != null)
			{
				panel.labelScore = Double.parseDouble(labelScoreNode.getTextContent());
			}
			
			panels[i] = panel;
		}
		return panels;
	}
	
}
