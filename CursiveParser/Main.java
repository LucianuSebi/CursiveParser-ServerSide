package org.example;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;

public class Main
{
    public static void main(String[] args) throws Exception
    {
        String path = Main.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String processType = args[0], imageToBeProcessed = args[1], basePath = URLDecoder.decode(path.substring(0, path.lastIndexOf("/") + 1), "UTF-8");

        if((processType.equalsIgnoreCase("learn") || processType.equalsIgnoreCase("find")) && imageToBeProcessed.isEmpty())
        {
            System.out.println("No file specified for " + processType + " type action!");
        }
        else
        {
            if (processType.equalsIgnoreCase("learn"))
            {
                System.out.println("--------------------Starting PerspectiveCropping.jar--------------------");
                ProcessBuilder perspectiveCropping= new ProcessBuilder("java", "-jar", (basePath +"lib/PerspectiveCropping.jar"), imageToBeProcessed, basePath+"cache");
                Process process1 = perspectiveCropping.start();

                InputStream stderr1 = process1.getInputStream();
                InputStreamReader isr1 = new InputStreamReader(stderr1);
                BufferedReader br1 = new BufferedReader(isr1);
                String line1 = null;
                while ((line1 = br1.readLine()) != null) {
                    System.out.println(line1);
                }
                process1.waitFor();
                System.out.println("Returned Value :" + process1.exitValue());

                System.out.println("--------------------Starting ProcesareImagine.jar--------------------");
                String imgname = imageToBeProcessed.substring(imageToBeProcessed.length() - 26, imageToBeProcessed.length());
                ProcessBuilder procesareImagine= new ProcessBuilder("java", "-jar", (basePath +"lib/ProcesareImagine.jar"), basePath+"cache/corrected_"+imgname, basePath+"cache", "");
                Process process2 = procesareImagine.start();

                InputStream stderr2 = process2.getInputStream();
                InputStreamReader isr2 = new InputStreamReader(stderr2);
                BufferedReader br2 = new BufferedReader(isr2);
                String line2 = null;
                int nrLetters=0;
                while ((line2 = br2.readLine()) != null) {
                    nrLetters=Integer.parseInt(line2);
                }
                System.out.println("Detected "+ nrLetters+" letters");
                process2.waitFor();
                System.out.println("Returned Value :" + process2.exitValue());
                System.out.println("--------------------Starting LearningNeural.jar--------------------");
            }
            else if (processType.equalsIgnoreCase("find"))
            {

            }
            else if (processType.equalsIgnoreCase("generate"))
            {

            }
        }
    }

}
