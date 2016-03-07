/* Author: Daekeun You (January, 2013) 
 * 
 * utilFunctions
 * 			a collection of functions for basic computation.  												
 * 	
 */

package gov.nih.nlm.iti.figure;

import org.bytedeco.javacpp.opencv_core.*;
import org.bytedeco.javacpp.opencv_imgproc;
import org.bytedeco.javacpp.opencv_core;

public class UtilFunctions
{

	public UtilFunctions(){}
	
	public boolean decisionMade = false;
			
	static public class BBoxCoor{
		public int left, top, right, bottom;		
	}
			
	public int sorting_max_2_min(double [] a, double [] b, int len){

		int i, j, k;
		double max;
		int max_idx;

		k = 0;
		for(i=0; i<len; i++){
			max = -10000.0;
			max_idx = 0;
			for(j=0; j<len; j++){
				if( a[j] > max ){
					max = a[j];
					max_idx = j;
				}
			}
			a[max_idx] = -10000.0;
			b[k++] = max;
		}

		return 1;
	}


	public int sorting_max_2_min_w_index(double [] a, double [] b, int [] index, int len){

		int i, j, k;
		double max;
		int max_idx;

		k = 0;
		for(i=0; i<len; i++){
			max = -10000.0;
			max_idx = 0;
			for(j=0; j<len; j++){
				if( a[j] > max ){
					max = a[j];
					max_idx = j;
				}
			}
			a[max_idx] = -10000.0;
			index[k] = max_idx;
			b[k++] = max;
		
		}

		return 1;
	}


	public int sorting_max_2_min_no_sorted_data(double [] a, int [] index, int len){

		int i, j, k;
		double max;
		int max_idx;

		k = 0;
		for(i=0; i<len; i++){
			max = -10000.0;
			max_idx = 0;
			for(j=0; j<len; j++){
				if( a[j] > max ){
					max = a[j];
					max_idx = j;
				}
			}
			a[max_idx] = -10000.0;
			index[k++] = max_idx;	
		}

		return 1;
	}

	public int sorting_max_2_min_INT(int [] a, int [] index, int len){

		int i, j, k;
		int max;
		int max_idx;

		k = 0;
		for(i=0; i<len; i++){
			max = -10000;
			max_idx = 0;
			for(j=0; j<len; j++){
				if( a[j] > max ){
					max = a[j];
					max_idx = j;
				}
			}
			a[max_idx] = -10000;
			index[k++] = max_idx;	
		}

		return 1;
	}


	public int sorting_min_2_max(int [] a, int [] index, int len){

		int i, j, k;
		int min;
		int min_idx;

		k = 0;
		for(i=0; i<len; i++){
			min = 10000;
			min_idx = 0;
			for(j=0; j<len; j++){
				if( a[j] < min ){
					min = a[j];
					min_idx = j;
				}
			}
			a[min_idx] = 10000;
			index[k++] = min_idx;	
		}

		return 1;
	}


	public int negative_image(IplImage img)
	{
		opencv_imgproc.cvThreshold(img, img, 0, 255, opencv_imgproc.THRESH_BINARY_INV);
		return 1;
		
//		int i, j;
//		int height, width;
//		CvMat srcMat = img.asCvMat();
//		
//		height = img.height();
//		width = img.width();
//		
//	//	CvMat negativeMat = cvCreateMat(srcMat.rows(), srcMat.cols(), CV_8UC1);
//	//	cvSetZero(negativeMat);
//				
//		for(i=0; i<height; i++){
//			for(j=0; j<width; j++){
//				
//				if( srcMat.get(i, j) > 0 )
//					srcMat.put(i, j, 0);
//				else
//					srcMat.put(i, j, 255);
//			}
//		}
//
//		img = srcMat.asIplImage();
//		
//		return 1;
	}
	

	public int cvImgThreshold(IplImage src, IplImage dst, int level)
	{
		opencv_imgproc.cvThreshold(src, dst, level, 255, opencv_imgproc.THRESH_BINARY);
		return 1;
		
//		int i, j;
//		int height, width;
//
//		width = src.width();
//		height = src.height();
//
//		CvMat in_data = src.asCvMat();
//		CvMat out_data = dst.asCvMat();
//
//		for(i = 0; i<height; i++){
//			for(j = 0; j<width; j++){
//				if( in_data.get(i, j) > level )
//					out_data.put(i, j, 255);
//				else
//					out_data.put(i, j, 0);
//			}
//		}
//
//		return 1;
	}
	
	
	public int featureExtractionFromContour36(CvSeq contour, double [] feature, int featureDim, int featureType, PanelLabelDetection.objRect chBndBox)
	{

		int left, top, right, bottom;
		int i;
		int mesh_width, mesh_height;
		int dir, dir_x, dir_y;
		CvSeq inner_contour = new CvSeq(null);
		double aspect_ratio;
		boolean is_narrow_char = false;
		
		int [] mesh_sum = new int [9];
		double [] comp_max = new double [4];
		
		left = 10000;
		right = 0;
		top = 10000;
		bottom = 0;
		CvPoint pt, pt_prev, pt_next;

		this.decisionMade = false;
		
		for(i=0; i<contour.total(); i++){
			pt = new CvPoint(opencv_core.cvGetSeqElem(contour, i));

			if( pt.x() > right )
				right = pt.x();
			if( pt.x() < left )
				left = pt.x();
			if( pt.y() > bottom )
				bottom = pt.y();
			if( pt.y() < top )
				top = pt.y();
		}
		
		chBndBox.left = left;
			
		chBndBox.bottom = bottom;
		chBndBox.right = right;
		chBndBox.top = top;

		mesh_width = (right - left + 1)/3;
		mesh_height = (bottom - top + 1)/3;
		
		if( mesh_width < 3 || mesh_height < 3 )
			return -1;
		
		aspect_ratio = (right-left)/(double)(bottom-top);
		
		// this can't be letter.
		if( aspect_ratio >= 1.5 )
			return -1;

//		if( aspect_ratio < 0.3 )
//			is_narrow_char = 1;

		// process exterior contour first.
		pt_prev = new CvPoint(opencv_core.cvGetSeqElem(contour, 0));
		
		for(i=1; i<contour.total(); i++){
			pt_next = new CvPoint(opencv_core.cvGetSeqElem(contour, i));

			dir = -1;
			if( pt_prev.x() == pt_next.x() )
				dir = 0;

			if( pt_prev.y() == pt_next.y() && dir == -1 )
				dir = 2;

			if( pt_prev.y() > pt_next.y() && dir == -1 ){
				if( pt_prev.x() > pt_next.x() )
					dir = 3;
				else
					dir = 1;
			}

			if( pt_prev.y() < pt_next.y() && dir == -1 ){
				if( pt_prev.x() > pt_next.x() )
					dir = 1;
				else
					dir = 3;
			}
			
			dir_x = (pt_prev.x() - left)/mesh_width;
			dir_y = (pt_prev.y() - top)/mesh_height;
			
			if( dir_x > 2 )
				dir_x = 2;
			if( dir_y > 2 )
				dir_y = 2;

			if( is_narrow_char )
				dir_x = 1;

			feature[12*dir_y + 4*dir_x + dir] += 1.0;

			pt_prev.put(pt_next);			
		
		}

		inner_contour = contour.v_next();
		
		while( inner_contour != null ){

			if( inner_contour.total() < 10 ){
				inner_contour = inner_contour.h_next();
				continue;
			}

			this.decisionMade  = true;
			pt_prev = new CvPoint(opencv_core.cvGetSeqElem(inner_contour, 0));
			
			for(i=1; i<inner_contour.total(); i++){
				pt_next = new CvPoint(opencv_core.cvGetSeqElem(inner_contour, i));
					
				dir = -1;
				if( pt_prev.x() == pt_next.x() )
					dir = 0;

				if( pt_prev.y() == pt_next.y() && dir == -1 )
					dir = 2;

				if( pt_prev.y() > pt_next.y() && dir == -1 ){
					if( pt_prev.x() > pt_next.x() )
						dir = 3;
					else
						dir = 1;
				}

				if( pt_prev.y() < pt_next.y() && dir == -1 ){
					if( pt_prev.x() > pt_next.x() )
						dir = 1;
					else
						dir = 3;
				}
				
				dir_x = (pt_prev.x() - left)/mesh_width;
				dir_y = (pt_prev.y() - top)/mesh_height;
				
				if( dir_x > 2 )
					dir_x = 2;
				if( dir_y > 2 )
					dir_y = 2;

				if( is_narrow_char )
					dir_x = 1;

				feature[12*dir_y + 4*dir_x + dir] += 1.0;

				pt_prev.put(pt_next);
				
			
			}

			inner_contour = inner_contour.h_next();
		}
				
		// Normalize

		for(i=0; i<36; i++)
			mesh_sum[i/4] += (int)feature[i];
				
		for(i=0; i<36; i++){
			if( mesh_sum[i/4] > 0 )
				feature[i] /= (mesh_sum[i/4]*1.0);					
		}

		for(i=0; i<36; i++){
			if( feature[i] > comp_max[i%4] )
				comp_max[i%4] = feature[i];
		}

		for(i=0; i<36; i++){
			if( comp_max[i%4] > 0 )
				feature[i] /= comp_max[i%4];
		}

		return 1;
	}


	public int drawContour(IplImage image, CvSeq contour, int intensity){
		int k;
		CvPoint pt;
		
		CvMat srcMat = image.asCvMat();

		for(k = 0; k < contour.total(); k++){
			pt = new CvPoint(opencv_core.cvGetSeqElem(contour, k));
			
			srcMat.put(pt.y(), pt.x(), intensity);			
		}
		
		image = srcMat.asIplImage();

		return 1;
	}
	
}