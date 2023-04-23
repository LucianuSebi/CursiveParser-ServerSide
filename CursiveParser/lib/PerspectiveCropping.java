package org.example;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import nu.pattern.OpenCV;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class Main {
  static double width;
  
  static double height;
  
  public static int[][][] pixelMatrix(BufferedImage img, String verbose) {
    BufferedImage pixelImg = new BufferedImage(img.getWidth(), img.getHeight(), 1);
    Graphics2D pixel2D = pixelImg.createGraphics();
    pixel2D.drawImage(img, 0, 0, null);
    int[][][] rgbMatrix = new int[pixelImg.getHeight()][pixelImg.getWidth()][4];
    for (int i = 0; i < pixelImg.getHeight(); i++) {
      for (int j = 0; j < pixelImg.getWidth(); j++) {
        Color color = new Color(pixelImg.getRGB(j, i));
        rgbMatrix[i][j][1] = color.getRed();
        rgbMatrix[i][j][2] = color.getGreen();
        rgbMatrix[i][j][3] = color.getBlue();
        if (rgbMatrix[i][j][1] != 255)
          rgbMatrix[i][j][0] = 1; 
      } 
    } 
    return rgbMatrix;
  }
  
  private static BufferedImage cropImage(BufferedImage img, double width, double height) {
    int invers = 0;
    if (height < width) {
      double aux = height;
      height = width;
      width = aux;
      invers = 1;
    } 
    BufferedImage imgCropped = new BufferedImage((int)(width - 1.0D), (int)(height - 1.0D), 5);
    int[][][] rgbMatrix = pixelMatrix(img, "");
    int hg = (int)(height - 1.0D);
    int wd = (int)(width - 1.0D);
    for (int i = 0; i < hg; i++) {
      for (int j = 0; j < wd; j++) {
        if (invers == 0) {
          Color color = new Color(rgbMatrix[i][j][1], rgbMatrix[i][j][2], rgbMatrix[i][j][3]);
          int rgb = color.getRGB();
          imgCropped.setRGB(j, i, rgb);
        } else {
          Color color = new Color(rgbMatrix[j][i][1], rgbMatrix[j][i][2], rgbMatrix[j][i][3]);
          int rgb = color.getRGB();
          imgCropped.setRGB(j, i, rgb);
        } 
      } 
    } 
    return imgCropped;
  }
  
  static BufferedImage Mat2BufferedImage(Mat matrix) throws Exception {
    MatOfByte mob = new MatOfByte();
    Imgcodecs.imencode(".jpg", matrix, mob);
    byte[] ba = mob.toArray();
    return ImageIO.read(new ByteArrayInputStream(ba));
  }
  
  private static MatOfPoint findMaxContour(List<MatOfPoint> contours) {
    double maxVal = 0.0D;
    int maxValIdx = 0;
    for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++) {
      double contourArea = Imgproc.contourArea((Mat)contours.get(contourIdx));
      if (maxVal < contourArea) {
        maxVal = contourArea;
        maxValIdx = contourIdx;
      } 
    } 
    return contours.get(maxValIdx);
  }
  
  private static MatOfPoint2f re_arrange(MatOfPoint2f approx_corners) {
    Point[] pts = approx_corners.toArray();
    return new MatOfPoint2f(new Point[] { pts[0], pts[3], pts[1], pts[2] });
  }
  
  private static MatOfPoint2f get_destination_points(MatOfPoint2f approx_corners) {
    Point p0, p1, p2, p3, pts[] = approx_corners.toArray();
    double w1 = (int)Math.sqrt(Math.pow((pts[0]).x - (pts[1]).x, 2.0D) + Math.pow((pts[0]).y - (pts[1]).y, 2.0D));
    double w2 = (int)Math.sqrt(Math.pow((pts[2]).x - (pts[3]).x, 2.0D) + Math.pow((pts[2]).y - (pts[3]).y, 2.0D));
    width = Math.max(w1, w2);
    double h1 = (int)Math.sqrt(Math.pow((pts[0]).x - (pts[2]).x, 2.0D) + Math.pow((pts[0]).y - (pts[2]).y, 2.0D));
    double h2 = (int)Math.sqrt(Math.pow((pts[1]).x - (pts[3]).x, 2.0D) + Math.pow((pts[1]).y - (pts[3]).y, 2.0D));
    height = Math.max(h1, h2);
    if (width > height) {
      p0 = new Point(0.0D, 0.0D);
      p1 = new Point(0.0D, (int)(width - 1.0D));
      p2 = new Point((int)(height - 1.0D), 0.0D);
      p3 = new Point((int)(height - 1.0D), (int)(width - 1.0D));
      Point aux = p2;
      p2 = p0;
      p0 = aux;
      aux = p3;
      p3 = p1;
      p1 = aux;
    } else {
      p0 = new Point(0.0D, 0.0D);
      p1 = new Point((int)(width - 1.0D), 0.0D);
      p2 = new Point(0.0D, (int)(height - 1.0D));
      p3 = new Point((int)(width - 1.0D), (int)(height - 1.0D));
    } 
    return new MatOfPoint2f(new Point[] { p0, p1, p2, p3 });
  }
  
  public static void perspectiveCorrection(String imgName, String cache, String id) throws Exception {
    OpenCV.loadLocally();
    Mat img = Imgcodecs.imread(imgName, 1);
    Mat imgCopy = new Mat();
    img.copyTo(imgCopy);
    Imgproc.cvtColor(imgCopy, imgCopy, 6);
    Mat kernel = new Mat();
    Mat ones = Mat.ones(5, 5, 5);
    Core.multiply(ones, new Scalar(0.06666666666666667D), kernel);
    Mat filtered = new Mat();
    Imgproc.filter2D(imgCopy, filtered, -1, kernel);
    Imgproc.threshold(filtered, filtered, 250.0D, 255.0D, 8);
    List<MatOfPoint> contours = new ArrayList<>();
    Imgproc.findContours(filtered, contours, new Mat(), 3, 1);
    MatOfPoint maxContour = findMaxContour(contours);
    MatOfPoint2f maxCnt2f = new MatOfPoint2f(maxContour.toArray());
    double epsilon = 0.02D * Imgproc.arcLength(maxCnt2f, true);
    MatOfPoint2f approx_cornerns = new MatOfPoint2f();
    Imgproc.approxPolyDP(maxCnt2f, approx_cornerns, epsilon, true);
    approx_cornerns = re_arrange(approx_cornerns);
    MatOfPoint2f dest_cornerns = get_destination_points(approx_cornerns);
    Mat homography = Calib3d.findHomography(approx_cornerns, dest_cornerns, 8, 3.0D);
    Mat un_warped = new Mat();
    Imgproc.warpPerspective(img, un_warped, homography, img.size());
    BufferedImage imageConverted = Mat2BufferedImage(un_warped);
    BufferedImage imageCropped = cropImage(imageConverted, width - 1.0D, height - 1.0D);
    ImageIO.write(imageCropped, "png", new File(cache + "/corrected_" + cache));
  }
  
  public static void main(String[] args) throws Exception {
    String path = args[0], cache = args[1], imgname = path.substring(path.length() - 26, path.length());
    perspectiveCorrection(path, cache, imgname);
  }
}
