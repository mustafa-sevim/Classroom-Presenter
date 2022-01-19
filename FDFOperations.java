import java.io.*;
import java.util.ArrayList;

public class FDFOperations{
    public static String createEmptyFDF(String FDFName, String PDFPath){
        try {
            String path = PDFPath.substring(0,PDFPath.lastIndexOf("/")) + "/" + FDFName + ".fdf";
            PrintWriter writer = new PrintWriter(path, "UTF-8");
            writer.println("%FDF-1.2");
            writer.println("1 0 obj");
            writer.println("<</FDF<</F(" + PDFPath + ")/Annots[]>>>>");
            writer.println("endobj");
            writer.println();
            writer.println("trailer");
            writer.println("<</Root 1 0 R>>");
            writer.println("%%EOF");
            writer.println();
            writer.close();
            return path.replace("/","\\");
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static void changePDFDirectoryInFDF(String FDFName, String PDFPath){
        try
        {
            ArrayList<String> lines = new ArrayList<>();
            String path = PDFPath.substring(0,PDFPath.lastIndexOf("/")) + "/" + FDFName + ".fdf";
            if (System.getProperty("os.name").toLowerCase().contains("windows"))
                path.replace("/","\\");

            File file = new File(path);    //creates a new file instance
            FileReader fr = new FileReader(file);   //reads the file
            BufferedReader br = new BufferedReader(fr);  //creates a buffering character input stream
            StringBuffer sb = new StringBuffer();    //constructs a string buffer with no characters
            String line = "";
            while((line = br.readLine()) != null) {
                if(line.startsWith("<</FDF<</F(")){
                    line = "<</FDF<</F(" + PDFPath + ")" + line.substring(line.lastIndexOf("/"));
                }
                lines.add(line);
            }
            fr.close();    //closes the stream and release the resources

            // clear the file content
            new FileWriter(path, false).close();

            PrintWriter writer = new PrintWriter(path, "UTF-8");
            for(String i: lines)
                writer.println(i);
            writer.close();

        }
        catch(IOException e) {
            e.printStackTrace();
        }

    }
}