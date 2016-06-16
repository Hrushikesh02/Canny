package application;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.opencv.contrib.*;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.*;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.*;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;



public class CannyController {
		@FXML
		private Button cameraButton;
		@FXML
		private ImageView originalFrame;
		@FXML
		private CheckBox canny;
		@FXML
		private Slider threshold;
		@FXML
		private CheckBox dilateErode;
		@FXML
		private CheckBox inverse;
		
		private ScheduledExecutorService timer;
		private VideoCapture capture = new VideoCapture();
		private boolean cameraActive;
		private FeatureDetector blobDetector;
		//private Mat Templ = Imgcodecs.imread("head.png");
		private Mat Template = Imgcodecs.imread("head.png");
		//Mat templateblobs = Blobs(Template);

		
		@FXML
		protected void startCamera()
		{
			originalFrame.setFitWidth(380);
			originalFrame.setPreserveRatio(true);
			
			if (!this.cameraActive)
			{
				this.canny.setDisable(true);
				this.dilateErode.setDisable(true);
				
				this.capture.open(1);
				
				if (this.capture.isOpened())
				{
					this.cameraActive = true;
					Template.convertTo(Template, CvType.CV_8UC1);
					Imgproc.GaussianBlur(Template, Template, new Size(3, 3),0);
					Template.convertTo(Template, CvType.CV_8UC1);
					Imgproc.Canny(Template, Template, this.threshold.getValue(), this.threshold.getValue() * 3);
					
					Runnable frameGrabber = new Runnable() {
						
						@Override
						public void run()
						{
							Image imageToShow = grabFrame();
							originalFrame.setImage(imageToShow);
						}
					};
					
					this.timer = Executors.newSingleThreadScheduledExecutor();
					this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);
					
					this.cameraButton.setText("Stop Camera");
				}
				else
				{
					System.err.println("Failed to open the camera connection...");
				}
			}
			else
			{
				this.cameraActive = false;
				this.cameraButton.setText("Start Camera");
				this.canny.setDisable(false);
				this.dilateErode.setDisable(false);
				try
				{
					this.timer.shutdown();
					this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
				}
				catch (InterruptedException e)
				{
					System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
				}
				
				this.capture.release();
				this.originalFrame.setImage(null);
			}
		}
		private Image grabFrame()
		{
			Image imageToShow = null;
			Mat frame = new Mat();
			
			if (this.capture.isOpened())
			{
				try
				{
					this.capture.read(frame);
					
					if (!frame.empty())
					{
						if (this.canny.isSelected())
						{
							frame = this.doCanny(frame);
						}
						imageToShow = mat2Image(frame);
					}
					
				}
				catch (Exception e)
				{
					System.err.print("ERROR iin grab frame()" );
					e.printStackTrace();
				}
			}
			return imageToShow;
		}
		private Mat doCanny(Mat frame)
		{
			Mat grayImage = new Mat();
			Mat detectedEdges = new Mat();
			
			Imgproc.cvtColor(frame, grayImage, Imgproc.COLOR_BGR2GRAY);
			Imgproc.GaussianBlur(grayImage, detectedEdges, new Size(3, 3),0);
			detectedEdges.convertTo(detectedEdges, CvType.CV_8UC1);
			Imgproc.Canny(detectedEdges, detectedEdges, this.threshold.getValue(), this.threshold.getValue() * 3);
			System.out.println("canny Threshold: "+this.threshold.getValue());
			Chamferdrawing(detectedEdges,frame);
			return detectedEdges;
			//Mat dest = new Mat(detectedEdges.cols(),detectedEdges.rows(),CvType.CV_8UC1,Scalar.all(0));
			//Core.add(dest, Scalar.all(0), dest);
			//frame.copyTo(dest, detectedEdges);
			///Mat dest3 = new Mat();
			//Imgproc.threshold(dest, dest3, 0, 255, Imgproc.THRESH_BINARY);
			//Mat dest2 = new Mat();
			//Imgproc.distanceTransform(dest3, dest2, Imgproc.CV_DIST_L2, 3);
//			dest.convertTo(dest, CvType.CV_8UC1);
//			Mat matOut = Blobs(dest);
//			matOut.convertTo(matOut, CvType.CV_8UC1);
				//return detectedEdges;
			//return matOut;
			//return frame;
 		//	double value = contourMatching(matOut);
			//return matOut;
		}
		private void Chamferdrawing(Mat matOut,Mat frame){
			MatOfFloat costs = new MatOfFloat();
			List<MatOfPoint> results = null; 
			System.out.println("here:");
			int best = Contrib.chamerMatching(matOut, Template, results, costs,1,20,1.0,3,3,2,0.8,1.2,0.5,20); 
			System.out.println("chamfer MAtching best :" + best);
			if(best>=0){
				List<Point> bestResults = results.get(best).toList();
				int n = bestResults.size();
				int i=0;
				Point pt;
				System.out.println("Here___________" + n);
				while(i<n){
					pt = bestResults.get(i);
					System.out.println("here @ 172 2bfb");
					if(pt.inside(new Rect(0,0,frame.cols(),frame.rows()))){
						System.out.println("Inside_________________");
						double[] data = matOut.get((int)pt.x, (int)pt.y);
						data[0] = 255;
//						double[] data = frame.get((int)pt.x, (int)pt.y);
//						data[0] = 255;
//						data[1] = 0;
//						data[2] = 0;
					}
					i++;
				}
				
			}
		}
		
		private double contourMatching(Mat templateEdgeImg){
			
			List<MatOfPoint> contourT1 = getAllContours(templateEdgeImg);
			List<MatOfPoint> contourT2 = getAllContours(Template);
			
			System.out.println(contourT1.size());
			 ArrayList<Double> values = new ArrayList<>();
		        int j = 0;
		        for(MatOfPoint contour1 : contourT1){
		        	 double minValue = Double.MAX_VALUE;
		             double tempValue = 0.0;
		             for(MatOfPoint contour2 : contourT2){
		            	 tempValue = Imgproc.matchShapes(contour1, contour2, Imgproc.CV_CONTOURS_MATCH_I2, 0);

		                 if (tempValue < minValue) {
		                     minValue = tempValue;
		                 }
		             }
		             System.out.println("i : "+ j +" ;tempValue: " + tempValue);
		             j++;
		             values.add(tempValue);
		        }
				double totalValue = 0.0;
				int i =0;
		        while(i<values.size()){
		        	totalValue += values.get(i);
		        	i++;
		        }
				System.out.println("totalValue : "+totalValue + "______________________________________________________");
				drawContours(templateEdgeImg,contourT1);
				return totalValue;

		}
		public static void drawContours(Mat img, List<MatOfPoint> contours)
	    {
	        for(MatOfPoint contour: contours) {
	            Rect rect = Imgproc.boundingRect(contour);
	            Imgproc.rectangle(img, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 0, 255));
	        }
	    }

		private static List<MatOfPoint> getAllContours(Mat mat)
	    {

	        Mat image = mat.clone();
	        Mat imageHSV = new Mat();

//	        Imgproc.cvtColor(image, imageHSV, Imgproc.COLOR_BGR2HSV);
//	        imageHSV = decolor(image);


//	        Imgcodecs.imwrite("./output/testn.png",image);
//	        Imgproc.cvtColor(imageHSV, imageHSV, Imgproc.COLOR_BGR2GRAY);
//	        Imgproc.medianBlur(imageHSV, imageHSV, 9);

	        List<Mat> src = new ArrayList<Mat>();
	        src.add(image);
	        List<Mat> dest = new ArrayList<Mat>();
	        Mat gray0 = new Mat(image.size(), CvType.CV_8U);
	        dest.add(gray0);
	        MatOfInt convert;

	        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
	        for(int i=0; i<3; i++) {
	            convert = new MatOfInt(i, 0);
	            Core.mixChannels(src, dest, convert);
//	            Imgcodecs.imwrite("./output/test4-" + String.valueOf(i) + ".jpeg", dest.get(0));

	            imageHSV = dest.get(0);
//	        Imgproc.GaussianBlur(imageHSV, imageHSV, new Size(5,5), 0);
//	        Imgproc.adaptiveThreshold(imageHSV, imageA, 255,Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY,7, 0);
//	        Imgproc.threshold(imageHSV,imageHSV,15,255, Imgproc.THRESH_BINARY);
//	            Imgproc.Canny(imageHSV, imageHSV, 70, 150, 3, true);

	        Imgproc.Canny(imageHSV, imageHSV, 20, 80, 3, true );
//	        Photo.fastNlMeansDenoising(imageHSV, imageHSV);
//	        Imgproc.dilate(imageHSV, imageHSV, new Mat(), new Point(-1, -1), 2);//Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2)));
//	        Imgproc.erode(imageHSV, imageHSV, new Mat(),new Point(-1, -1), 2);// Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2,2)));

	            Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2));
	        Imgproc.morphologyEx(imageHSV, imageHSV, Imgproc.MORPH_CLOSE, element);

	            Imgproc.morphologyEx(imageHSV, imageHSV, Imgproc.MORPH_DILATE, element, new Point(-1, -1), 2);


//	        Imgproc.adaptiveThreshold(imageHSV, imageHSV, 255,Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY,7, 0);
//	        Imgproc.threshold(imageHSV,imageHSV,50,255, Imgproc.THRESH_BINARY);
//	            Imgcodecs.imwrite("./output/test1-" + String.valueOf(i) + ".jpeg", imageHSV);


	            List<MatOfPoint> temp = new ArrayList<MatOfPoint>();
	            Imgproc.findContours(imageHSV, temp, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

	            contours.addAll(temp);
	        }

	        return contours;
	    }

		private Mat Blobs(Mat dest) {
			FeatureDetector blobDetector = FeatureDetector.create(FeatureDetector.GFTT);

			Mat matOut = new Mat();
			MatOfKeyPoint matOfKey = new MatOfKeyPoint();
			blobDetector.detect(dest, matOfKey);
			Scalar cores = new Scalar(0,0,255);
			org.opencv.features2d.Features2d.drawKeypoints(dest,matOfKey,matOut,cores,2);
			return matOut;
		}
		private Mat MatchTemplate(Mat matOut) {
			
			Mat result = new Mat();
			int result_cols =  matOut.cols() - Template.cols() + 1;
			int result_rows = matOut.rows() - Template.rows() + 1;

			result.create( result_rows, result_cols, CvType.CV_32FC1 );

			Imgproc.matchTemplate( matOut, Template, result, Imgproc.TM_SQDIFF_NORMED );
			Core.normalize( result, result, 0, 1, Core.NORM_MINMAX, -1, new Mat() );
			
			MinMaxLocResult mmr = Core.minMaxLoc(result);
	        Point matchLoc = mmr.minLoc;
	        Double min = mmr.maxVal; 
	        System.out.println("result rows :" + result_rows + "cols: " + result_cols);
	        System.out.println("Min : " + min);
	        Imgproc.rectangle(matOut, matchLoc, new Point(matchLoc.x+Template.cols(),matchLoc.y+Template.rows()),new Scalar(255, 0, 0, 255),3); 

       
			
			return matOut;
		}
		@FXML
		protected void cannySelected()
		{
			// check whether the other checkbox is selected and deselect it
			if (this.dilateErode.isSelected())
			{
				this.dilateErode.setSelected(false);
				this.inverse.setDisable(true);
			}
			
			// enable the threshold slider
			if (this.canny.isSelected())
				this.threshold.setDisable(false);
			else
				this.threshold.setDisable(true);
				
			this.cameraButton.setDisable(false);
		}
		
		@FXML
		protected void dilateErodeSelected()
		{
			// check whether the canny checkbox is selected, deselect it and disable
			// its slider
			if (this.canny.isSelected())
			{
				this.canny.setSelected(false);
				this.threshold.setDisable(true);
			}
			
			if (this.dilateErode.isSelected())
				this.inverse.setDisable(false);
			else
				this.inverse.setDisable(true);
				
			// now the capture can start
			this.cameraButton.setDisable(false);
		}
		
		private Image mat2Image(Mat frame)
		{
			// create a temporary buffer
			MatOfByte buffer = new MatOfByte();
			// encode the frame in the buffer, according to the PNG format
			Imgcodecs.imencode(".png", frame, buffer);
			// build and return an Image created from the image encoded in the
			// buffer
			return new Image(new ByteArrayInputStream(buffer.toArray()));
		}
		
}
