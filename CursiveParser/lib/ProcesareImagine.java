package org.example;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.HashMap;
import java.util.Map;

public class Main {
    static String cache;
    public static int[][][] createPixelMatrix(BufferedImage img) {

        //Creating the pixel value matrix from the contrasted image
        BufferedImage pixelImg = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D pixel2D = pixelImg.createGraphics();
        pixel2D.drawImage(img ,0,0,null);
        int[][][] rgbMatrix = new int[pixelImg.getHeight()][pixelImg.getWidth()][4];
        for(int i=0; i<pixelImg.getHeight(); i++)
            for(int j=0; j<pixelImg.getWidth(); j++) {
                Color color = new Color(pixelImg.getRGB(j, i));
                rgbMatrix[i][j][1] = color.getRed();
                rgbMatrix[i][j][2] = color.getGreen();
                rgbMatrix[i][j][3] = color.getBlue();
                if(rgbMatrix[i][j][1]!=255) {rgbMatrix[i][j][0]=1;}
                //System.out.println(rgbMatrix[i][j][0] + ","+ rgbMatrix[i][j][1]+","+rgbMatrix[i][j][2]+","+rgbMatrix[i][j][3]);
            }
        return rgbMatrix;
    }

    public static int[] createHistogramHorizontal(int [][][] matrix){
        //Getting the number of pixels on each row
        int [] pixelsToLeft =new int[matrix.length];
        for(int i=0; i<matrix.length; i++)
            for(int j=0; j<matrix[0].length; j++)
                if(matrix[i][j][0]==1) {pixelsToLeft[i]++;};
        //for(int i=0;i<rgbMatrix.length;i++)
        //System.out.println(leftPixelsTemp[i]);
        return pixelsToLeft;
    }

    public static BufferedImage createHistogramHorizontalGraphic(int[] leftPixels, BufferedImage pixelImg) {
        //Pushing all pixels that aren't white to the left and generates an image
        int[][] pushedPixelsMatrix_Left = new int[pixelImg.getHeight()][pixelImg.getWidth()];
        for(int i=0; i<pixelImg.getHeight(); i++)
            for(int j=0; j<pixelImg.getWidth(); j++)
                if(leftPixels[i]>0){
                    pushedPixelsMatrix_Left[i][j]=1;
                    leftPixels[i]--;
                }

        //Generating the new image with the pixels to the left
        Color white = new Color(255, 255, 255);
        int rgb = white.getRGB();
        BufferedImage pushedPixelImg = new BufferedImage(pixelImg.getWidth(), pixelImg.getHeight(), BufferedImage.TYPE_INT_RGB);
        for(int i=0; i<pixelImg.getHeight(); i++)
            for(int j=0; j<pixelImg.getWidth(); j++)
                if(pushedPixelsMatrix_Left[i][j]==1)
                    pushedPixelImg.setRGB(j, i, rgb);

        return pushedPixelImg;
    }

    //Removes the minimum value from all rows
    public static int[] clearMin(int[] pixels) {
        int min=1921;
        for(int i=0; i<pixels.length; i++)
            if(pixels[i]<min && pixels[i]!=0)
                min=pixels[i];
        for(int i=0; i<pixels.length; i++)
            pixels[i]=pixels[i]-min;
        return pixels;
    }

    //calculates the number of rows
    public static int countRows(int[] pixels) {
        int nrRows=0;
        for(int i=1; i<pixels.length; i++)
            if(pixels[i]==0 && pixels[i-1]>0) {
                nrRows++;
            }
        if(pixels[pixels.length-1]>0)
            nrRows++;
        return nrRows;
    }

    // Separates the stuck rows, t-tolerance
    public static void separateStuckRows(int[] pixels, int t) {

        int st=0,dr=-1,s=0,nrRows=1;
        for(int i=0;i<pixels.length;i++) {
            if(pixels[i]>0) {
                st=i;
                for(dr=st+1; dr<pixels.length && pixels[dr]>0;dr++) {}
                i=dr;
                s=s+dr-st;
                st=0;
                dr=-1;
                nrRows++;
            }
        }
        s=s/(nrRows-1)+t;
        for(int i=0;i<pixels.length;i++) {
            if(pixels[i]>0) {
                st=i;
                for(dr=st+1; dr<pixels.length && pixels[dr]>0;dr++) {}
                i=dr;
                if(dr-st>s) {
                    int[] section = new int[dr-st];
                    for(int j=0;j<section.length ;j++)
                        section[j]=pixels[j+st];
                    clearMin(section);
                    for(int j=0;j<section.length ;j++)
                        pixels[j+st]=section[j];
                    i=0;
                }
                st=0;
                dr=-1;
            }
        }

    }
    // x-multiplyer
    // b-bias to add
    // t-treshold
    // a-treshold for line breakup
    public static int[] filterNoise(int[] pixels,int x,int b, int t, int a) {
        //filtering noise by multiplying and subtracting
        for(int i=0; i<pixels.length; i++)
            pixels[i]=pixels[i]*x-b;
        for(int i=0; i<pixels.length; i++)
            if(pixels[i]>0){pixels[i]=(pixels[i]+b)/x;}

        //removing rows smaller than "t"
        int j=0;
        for(int i=0; i<pixels.length; i++) {
            if(pixels[i]>0) {
                for(j=i; pixels[j]>0 && j<pixels.length-1; j++) {}
                if(j-i<t)
                    for(int k=i; k<j+1 && k<pixels.length-1; k++)
                        pixels[k]=0;
                i=j;
            }
        }
        //uses separateStuckRows method and then cleans up the useless row
        separateStuckRows(pixels, a);
        j=0;
        for(int i=0; i<pixels.length; i++) {
            if(pixels[i]>0) {
                for(j=i; pixels[j]>0 && j<pixels.length-1; j++) {}
                if(j-i<t)
                    for(int k=i; k<j+1 && k<pixels.length-1; k++)
                        pixels[k]=0;
                i=j;
            }
        }

        // clears random values
        for(int i=0; i<pixels.length; i++) {
            if(pixels[i]>0) {pixels[i]=30;}
            if(pixels[i]<0) {pixels[i]=0;}
        }

        return pixels;
    }

    // expands the rows upwards and downwards to minimise loss from overly agressive filtering
    public static int[] widenRows(int[] pixels, int a) {
        int b=0;
        for(int i=1; i<pixels.length-1; i++) {
            if(pixels[i]==0 && pixels[i-1]==30 && pixels[i+1]==0) {
                for(b=i; b-i+1<a && b<pixels.length && pixels[b+1]==0; b++)
                    pixels[b]=40;
                i=b;
            }
            if(pixels[i]==0 && pixels[i+1]==30 && pixels[i-1]==0) {
                for(b=i;i-b+1<a && b>0 && pixels[b-1]==0;b--)
                    pixels[b]=40;
                i++;
            }
        }
        return pixels;
    }

    // "cuts" the rows from the rgbMatrix and puts them in an image
    public static void findRows(int[][][] rgbMatrix, int[] pixels, String imgname) throws IOException {

        int st=0,dr=-1,nrRow=0;
        for(int i=0;i<pixels.length;i++) {
            if(pixels[i]>0) {
                st=i;
                for(dr=st+1; dr<pixels.length && pixels[dr]>0;dr++) {}
                i=dr;
                BufferedImage newImg = new BufferedImage(rgbMatrix[0].length , dr-st, BufferedImage.TYPE_INT_RGB);
                for(int k=0;k<dr-st;k++) {
                    for(int j=0;j<rgbMatrix[0].length;j++) {
                        Color color = new Color(rgbMatrix[k+st][j][1], rgbMatrix[k+st][j][2], rgbMatrix[k+st][j][3]);
                        int rgb = color.getRGB();
                        newImg.setRGB(j, k, rgb);
                        //System.out.println(rgb);
                        //System.out.println(k+" "+j+": "+rgbMatrix[k+st][j][1]+" "+ rgbMatrix[k+st][j][2]+" "+ rgbMatrix[k+st][j][3]);
                    }
                }
                //System.out.println(st+" "+ dr);
                ImageIO.write(newImg, "png", new File(cache + "/Row"+ nrRow +"_" + imgname));
                st=0;dr=-1;
                nrRow++;
                newImg.flush();
            }
        }

    }

    // colors the outside of rows
    public static BufferedImage graphicFoundRows(BufferedImage rotatedImg, int[] pixels) throws IOException {

        BufferedImage finalImg = rotatedImg;

        int nrlinii=0;
        int[] poz=new int[pixels.length];

        Color color = new Color(255,0,0);
        int rgb = color.getRGB();
        for(int i=0;i<pixels.length;i++) {

            if(i==pixels.length-1 || i==0) {
                if((i==pixels.length-1) && pixels[i]==0 && pixels[i-1]>0) {
                    nrlinii++;
                    poz[nrlinii]=i;
                    nrlinii++;
                    poz[nrlinii]=i;
                    for(int j=0;j<finalImg.getWidth();j++)
                        finalImg.setRGB(j, i, rgb);
                }
                else
                if(i==0 && pixels[i]==0 && pixels[i+1]>0) {
                    nrlinii++;
                    poz[nrlinii]=i;
                    nrlinii++;
                    poz[nrlinii]=i;
                    for(int j=0;j<finalImg.getWidth();j++)
                        finalImg.setRGB(j, i, rgb);
                }
                else
                if((i==pixels.length-1) && pixels[i]==0 && pixels[i-1]==0) {
                    nrlinii++;
                    poz[nrlinii]=i;
                    for(int j=0;j<finalImg.getWidth();j++)
                        finalImg.setRGB(j, i, rgb);
                }
                else
                if(i==0 && pixels[i]==0 && pixels[i+1]==0) {
                    nrlinii++;
                    poz[nrlinii]=i;
                    for(int j=0;j<finalImg.getWidth();j++)
                        finalImg.setRGB(j, i, rgb);
                }
            }
            else
            if(pixels[i]==0 && (pixels[i-1]>0 && pixels[i+1]>0)) {
                nrlinii++;
                poz[nrlinii]=i;
                nrlinii++;
                poz[nrlinii]=i;
                for(int j=0;j<finalImg.getWidth();j++)
                    finalImg.setRGB(j, i, rgb);
            }
            else
            if(pixels[i]==0 && (pixels[i-1]>0 || pixels[i+1]>0)) {
                nrlinii++;
                poz[nrlinii]=i;
                for(int j=0;j<finalImg.getWidth();j++)
                    finalImg.setRGB(j, i, rgb);
            }
            else
            if(pixels[i]==0)
                for(int j=0;j<finalImg.getWidth()-i%5;j++)
                    if(j%5==0)
                        finalImg.setRGB(j+i%5, i, rgb);

        }


        String text;
        text = nrlinii+"\n";
        for(int i=1;i<=nrlinii;i++)
            text=text+poz[i]+"\n";

        File textPoz=new File(cache + "/pozitii temporare.txt");
        textPoz.createNewFile();
        FileWriter myWriter = new FileWriter(textPoz);
        myWriter.write(text);
        myWriter.close();

        return finalImg;
    }

    //rotates the image idk how it works i copied it
    public static BufferedImage rotateImageByDegrees(BufferedImage img, double angle) {
        double rads = Math.toRadians(angle);
        double sin = Math.abs(Math.sin(rads)), cos = Math.abs(Math.cos(rads));
        int w = img.getWidth();
        int h = img.getHeight();
        int newWidth = (int) Math.floor(w * cos + h * sin);
        int newHeight = (int) Math.floor(h * cos + w * sin);

        BufferedImage rotated = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = rotated.createGraphics();
        AffineTransform at = new AffineTransform();
        at.translate((newWidth - w) / 2, (newHeight - h) / 2);

        int x = w / 2;
        int y = h / 2;

        at.rotate(rads, x, y);
        g2d.setTransform(at);
        g2d.drawImage(img,0,0,null);
        g2d.setColor(Color.RED);
        g2d.dispose();

        return rotated;
    }

    public static int[] createHistogramVertical(int[][][] matrix) {
        int[] pixels = new int[matrix[0].length];
        //System.out.println(matrix[0].length);

        for(int i=0; i<matrix.length; i++)
            for(int j=0; j<matrix[0].length; j++)
                if(matrix[i][j][0]==1) {pixels[j]++;}


        return pixels;
    }

    public static void createHistogramVerticalGraphic(int[] downPixels, BufferedImage pixelImg, int nrRow, String imgname) throws IOException {
        //Pushing all pixels that aren't white down and generates an image
        int[][] pushedPixelsMatrix_Left = new int[pixelImg.getHeight()][pixelImg.getWidth()];
        for(int i=pixelImg.getHeight()-1; i>=0; i--)
            for(int j=0; j<pixelImg.getWidth(); j++)
                if(downPixels[j]>0){
                    pushedPixelsMatrix_Left[i][j]=1;
                    downPixels[j]--;
                }

        //Generating the new image with the pixels pushed down
        Color white = new Color(255, 255, 255);
        int rgb = white.getRGB();
        BufferedImage pushedPixelImg = new BufferedImage(pixelImg.getWidth(), pixelImg.getHeight(), BufferedImage.TYPE_INT_RGB);
        for(int i=0; i<pixelImg.getHeight(); i++)
            for(int j=0; j<pixelImg.getWidth(); j++)
                if(pushedPixelsMatrix_Left[i][j]==1)
                    pushedPixelImg.setRGB(j, i, rgb);

        ImageIO.write(pushedPixelImg, "png", new File(cache + "/RowPixelGraph"+ nrRow+"_" + imgname));
        pushedPixelImg.flush();
    }

    public static void removeWordsSmallerThan(int[] pixels, int a) {

        int j=0;
        for(int i=0; i<pixels.length; i++) {
            if(pixels[i]>0) {
                for(j=i; pixels[j]>0 && j<pixels.length-1; j++) {}
                if(j-i<a)
                    for(int k=i; k<=j && k<pixels.length; k++)
                        pixels[k]=0;
                i=j;
            }
        }

    }

    //remove random spaces between letters
    public static int[] removeSpacesSmallerThan(int[] pixels, int t) {

        int st=0,dr=-1;
        for(int i=0;i<pixels.length;i++) {
            if(pixels[i]==0) {
                st=i;
                for(dr=st+1; dr<pixels.length && pixels[dr]==0;dr++) {}
                i=dr;
                if(dr-st<t) {
                    for(int j=st;j<dr;j++)
                        pixels[j]=90;
                }
                st=0;
                dr=-1;
            }
        }

        return pixels;
    }

    public static int findWords(String imgname, int nrRow) throws IOException {

        BufferedImage currentRow = ImageIO.read(new File(cache + "/Row"+ nrRow +"_" + imgname));

        BufferedImage grayImg = new BufferedImage(currentRow.getWidth(), currentRow.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D grayScale2D = grayImg.createGraphics();
        grayScale2D.drawImage(currentRow ,0,0,null);

        BufferedImage contrastedImg = new BufferedImage(grayImg.getWidth(), grayImg.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D contrast2D = contrastedImg.createGraphics();
        contrast2D.drawImage(grayImg ,0,0,null);
        RescaleOp contrast = new RescaleOp(1.8f, 30, null);
        contrast.filter(contrastedImg, contrastedImg);

        int[][][] rgbMatrix = createPixelMatrix(contrastedImg);

        int[] pixels = createHistogramVertical(rgbMatrix);
        removeWordsSmallerThan(pixels, grayImg.getWidth()/88);
        removeSpacesSmallerThan(pixels, 10);
        widenRows(pixels,14);
        createHistogramVerticalGraphic(pixels,currentRow,nrRow,imgname);

        pixels = createHistogramVertical(rgbMatrix);
        removeWordsSmallerThan(pixels, grayImg.getWidth()/88);
        removeSpacesSmallerThan(pixels, 10);
        widenRows(pixels,14);


        rgbMatrix = createPixelMatrix(currentRow);

        int st=0,dr=-1,nrWord=0;
        for(int i=0;i<pixels.length;i++) {
            if(pixels[i]>0) {
                st=i;
                for(dr=st+1; dr<pixels.length && pixels[dr]>0;dr++) {}
                i=dr;
                BufferedImage newImg = new BufferedImage(dr-st, rgbMatrix.length,  BufferedImage.TYPE_INT_BGR);
                for(int j=0;j<rgbMatrix.length;j++) {
                    for(int k=0;k<dr-st;k++) {
                        //System.out.println(nrRow+"/"+pixels.length+"/"+st+" "+dr+"/"+j+" "+ k +"/" + rgbMatrix.length +" "+rgbMatrix[0].length);

                        Color color = new Color(rgbMatrix[j][k+st][1], rgbMatrix[j][k+st][2], rgbMatrix[j][k+st][3]);
                        int rgb = color.getRGB();
                        newImg.setRGB(k, j, rgb);					}
                }
                ImageIO.write(newImg, "png", new File(cache + "/Word"+nrRow+"-"+ nrWord +"_" + imgname));
                st=0;dr=-1;
                nrWord++;
                newImg.flush();
            }
        }

        return nrWord;
    }

    public static void deleteReqPhp() throws MalformedURLException, IOException {

        HttpClient client = HttpClient.newHttpClient();

        Map<String, String> values = new HashMap<>();
        values.put("upload", "resetrequests");
        values.put("username", "121");
        values.put("userpassword", "admin");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://34.116.217.29/api_server.php"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(buildFormData(values)))
                .build();


    }

    private static String buildFormData(Map<String, String> data) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            if (builder.length() > 0) {
                builder.append("&");
            }
            builder.append(entry.getKey());
            builder.append("=");
            builder.append(entry.getValue());
        }
        return builder.toString();
    }

    public static int findLetters(String imgname, int row, int word, int totalLetter) throws IOException {


        BufferedImage currentWord = ImageIO.read(new File(cache + "/Word"+row+"-"+ word +"_" + imgname));

        BufferedImage grayImg = new BufferedImage(currentWord.getWidth(), currentWord.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D grayScale2D = grayImg.createGraphics();
        grayScale2D.drawImage(currentWord ,0,0,null);

        BufferedImage contrastedImg = new BufferedImage(grayImg.getWidth(), grayImg.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D contrast2D = contrastedImg.createGraphics();
        contrast2D.drawImage(grayImg ,0,0,null);
        RescaleOp contrast = new RescaleOp(1.9f, 50, null);
        contrast.filter(contrastedImg, contrastedImg);

        int[][][] rgbMatrix = createPixelMatrix(contrastedImg);

        int[] pixels = createHistogramVertical(rgbMatrix);
        clearMin(pixels);
        widenRows(pixels,14);
        rgbMatrix = createPixelMatrix(currentWord);

        int st=0,dr=-1,nrLetter=0;
        for(int i=0;i<pixels.length;i++) {
            if(pixels[i]>0) {
                st=i;
                for(dr=st+1; dr<pixels.length && pixels[dr]>0;dr++) {}
                i=dr;
                BufferedImage newImg = new BufferedImage(dr-st, rgbMatrix.length,  BufferedImage.TYPE_INT_BGR);
                for(int j=0;j<rgbMatrix.length;j++) {
                    for(int k=0;k<dr-st;k++) {
                        //System.out.println(nrRow+"/"+pixels.length+"/"+st+" "+dr+"/"+j+" "+ k +"/" + rgbMatrix.length +" "+rgbMatrix[0].length);

                        Color color = new Color(rgbMatrix[j][k+st][1], rgbMatrix[j][k+st][2], rgbMatrix[j][k+st][3]);
                        int rgb = color.getRGB();
                        newImg.setRGB(k, j, rgb);					}
                }
                ImageIO.write(newImg, "png", new File(cache + "/Letter"+(nrLetter+1+totalLetter)+"_" + imgname));
                st=0;dr=-1;
                nrLetter++;
                newImg.flush();
            }
        }

        return nrLetter;

    }

    public static void main(String[] args) throws URISyntaxException, IOException, InterruptedException {

        //Getting the arguments
        String path=args[0], verbose=args[2];
        cache=args[1];

        if(verbose.equalsIgnoreCase("vv")|| verbose.equalsIgnoreCase("v")){System.out.println("------------------------------------------");}

        //Creating the Strings used in paths
        String pathjar=new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
        String imgname = path.substring(path.length() - 26, path.length());

        //Loading the raw image
        if(verbose.equalsIgnoreCase("vv")){System.out.println("Loading the image...");}
        BufferedImage crudeImg = ImageIO.read(new File(path));
        ImageIO.write(crudeImg, "png", new File(cache + "/Crude_" + imgname));
        if(verbose.equalsIgnoreCase("vv")){System.out.println("Loaded the image succesfully!");}

        //Rotating the crudeImg
        if(verbose.equalsIgnoreCase("vv")){System.out.println("Rotating the image...");}
        BufferedImage rotatedImg = rotateImageByDegrees(crudeImg, 90);
        if(verbose.equalsIgnoreCase("vv") || verbose.equalsIgnoreCase("v")){System.out.println("Rotated the image succesfully!");}
        crudeImg.flush();	//!!!

        //Saving the raw rotated image in cache
        ImageIO.write(rotatedImg, "png", new File(cache + "/RawRotated_" + imgname));
        if(verbose.equalsIgnoreCase("vv") || verbose.equalsIgnoreCase("v")){System.out.println("Saved the rotated raw image in cache!");}

        //Grayscaling the rotated image and saving it
        if(verbose.equalsIgnoreCase("vv")){System.out.println("Grayscaling the image...");}
        BufferedImage grayImg = new BufferedImage(rotatedImg.getWidth(), rotatedImg.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D grayScale2D = grayImg.createGraphics();
        grayScale2D.drawImage(rotatedImg ,0,0,null);
        ImageIO.write(grayImg, "png", new File(cache + "/GrayScale_" + imgname));
        if(verbose.equalsIgnoreCase("vv") || verbose.equalsIgnoreCase("v")){System.out.println("Saved the grayscale image in cache!");}
        grayScale2D.dispose();	//!!!

        //Contrasting the grayscale image and saving it
        if(verbose.equalsIgnoreCase("vv")){System.out.println("Contrasting the image...");}
        BufferedImage contrastedImg = new BufferedImage(rotatedImg.getWidth(), rotatedImg.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D contrast2D = contrastedImg.createGraphics();
        contrast2D.drawImage(grayImg ,0,0,null);
        RescaleOp contrast = new RescaleOp(1.9f, 50, null);
        contrast.filter(contrastedImg, contrastedImg);
        ImageIO.write(contrastedImg, "png", new File(cache + "/Contrasted_" + imgname));
        if(verbose.equalsIgnoreCase("vv") || verbose.equalsIgnoreCase("v")){System.out.println("Saved the contrasted image in cache!");}
        grayImg.flush();		//!!!
        contrast2D.dispose();	//!!!

        //Creating the pixel value matrix from the contrasted image
        int[][][] grayMatrix = createPixelMatrix(contrastedImg);

        //Getting the number of pixels to the left and clen the minimum value
        int[] leftPixelsTemp1 = createHistogramHorizontal(grayMatrix);
        if(verbose.equalsIgnoreCase("vv")){System.out.println("Further processing the spikes of the image...");}
        int[] leftPixelsTemp2 = clearMin(leftPixelsTemp1);
        int[] leftPixelsTemp3 = filterNoise(leftPixelsTemp2, 4, (contrastedImg.getWidth()/80), (contrastedImg.getHeight()/65),-(contrastedImg.getHeight()/100));
        int[] leftPixels = widenRows(leftPixelsTemp3, contrastedImg.getHeight()/160);
        if(verbose.equalsIgnoreCase("vv") || verbose.equalsIgnoreCase("v")){System.out.println("Removed noise!");}

        //Generating an image representing the clusters of pixels filtered and unfiltered
        leftPixelsTemp1 = createHistogramHorizontal(grayMatrix);
        leftPixelsTemp2 = clearMin(leftPixelsTemp1);
        BufferedImage pushedPixelImg = createHistogramHorizontalGraphic(leftPixels, contrastedImg);
        BufferedImage pushedPixelImgCrude = createHistogramHorizontalGraphic(leftPixelsTemp2, contrastedImg);
        ImageIO.write(pushedPixelImg, "png", new File(cache + "/PushedLeft_" + imgname));
        ImageIO.write(pushedPixelImgCrude, "png", new File(cache + "/PushedLeftCrude_" + imgname));
        if(verbose.equalsIgnoreCase("vv") || verbose.equalsIgnoreCase("v")){System.out.println("Saved the new filtered and unfiltered images in cache!");}

        //Cutting up the image into rows
        if(verbose.equalsIgnoreCase("vv") || verbose.equalsIgnoreCase("v")){System.out.println("Cutting up the image into rows...");}
        leftPixelsTemp1 = createHistogramHorizontal(grayMatrix);
        leftPixelsTemp2 = clearMin(leftPixelsTemp1);
        leftPixelsTemp3 = filterNoise(leftPixelsTemp2,  4, (contrastedImg.getWidth()/80), (contrastedImg.getHeight()/65),-(contrastedImg.getHeight()/100));
        leftPixels = widenRows(leftPixelsTemp3, 12);
        int[][][] rgbMatrix = createPixelMatrix(rotatedImg);
        int nrRows = countRows(leftPixels);
        if(verbose.equalsIgnoreCase("vv")){System.out.println("Detected "+nrRows+" rows!");}
        findRows(rgbMatrix, leftPixels, imgname);

        //Creating the return image for the phone terminal
        BufferedImage phoneTerminalImg = graphicFoundRows(rotatedImg, leftPixels);
        ImageIO.write(phoneTerminalImg, "png", new File(cache + "/PhoneTerminalImg_" + imgname));

        //Splitting the rows into separate words
        int nrWords=0;
        int[] words_row = new int[nrRows];
        for(int i=0; i<nrRows; i++) {
            words_row[i]= findWords(imgname, i);
            nrWords = nrWords + words_row[i];
        }
        if(verbose.equalsIgnoreCase("vv") || verbose.equalsIgnoreCase("v")){System.out.println("Found " + nrWords +" words!");}

        //Splitting the words into o letters
        int nrLetters=0;
        for(int i=0;i<nrRows;i++)
            for(int j=0; j<words_row[i];j++) {
                nrLetters = nrLetters + findLetters(imgname, i, j, nrLetters);
            }
        if(verbose.equalsIgnoreCase("vv") || verbose.equalsIgnoreCase("v")){System.out.println("Found " + nrLetters +" letters!");}
        System.out.println(nrLetters);
        //Delete the request
        deleteReqPhp();

        if(verbose.equalsIgnoreCase("vv")|| verbose.equalsIgnoreCase("v")){System.out.println("------------------------------------------");}
    }

}
