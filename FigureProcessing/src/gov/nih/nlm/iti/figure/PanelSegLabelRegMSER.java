package gov.nih.nlm.iti.figure;

import java.awt.Rectangle;

import org.bytedeco.javacpp.opencv_core.*;
import org.bytedeco.javacpp.opencv_features2d.MSER;

class PanelSegLabelRegMSER extends PanelSeg 
{
	public void segment(Mat image) throws Exception 
	{
		super.segment(image);
		
		PointVectorVector points = new PointVectorVector();
		RectVector boxes = new RectVector();
		MSER mser = MSER.create(5, 60, 4400, 0.25, .2, 200, 1.01, 0.003, 5);
		mser.detectRegions(figure.imageGray, points, boxes);
		
		long n = points.size();		Rect rect; 
		for (int i = 0; i < n; i++)
		{
			rect = boxes.get(i);
			if (rect.width() > Figure.label_box_max_width) continue;
			if (rect.height() > Figure.label_box_max_height) continue;
			
			PanelSegResult panel = new PanelSegResult();
			panel.labelRect = new Rectangle(rect.x(), rect.y(), rect.width(), rect.height());
			figure.segmentationResult.add(panel);
		}		
	}

	
}
