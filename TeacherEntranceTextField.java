import com.foxit.sdk.common.Library;
import com.foxit.sdk.common.Range;
import com.foxit.sdk.fdf.FDFDoc;
import com.foxit.sdk.pdf.PDFDoc;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.concurrent.Executors;

import static com.foxit.sdk.common.Constants.e_ErrSuccess;
import static com.foxit.sdk.pdf.PDFDoc.e_Annots;
import static java.awt.event.KeyEvent.VK_CONTROL;
import static java.awt.event.KeyEvent.VK_S;
import static java.lang.System.exit;

public class TeacherEntranceTextField extends JFrame implements ActionListener {

    private static final String IP = "192.168.1.34";

    private FileInputStream fileInputStream;
    private DataOutputStream dataOutputStream;
    private DataInputStream dataInputStream;
    private Socket socket;

    private JButton fileChooser;
    private JButton createClass;
    private JButton goToClass;
    private  File file;
    private  JLabel label1;
    private JLabel label2;
    private JLabel label4;
    private  JLabel label6;
    private  JLabel label7;
    private int createClassCount = 0;
    private Integer classCode = 999999;

    TeacherEntranceTextField() {
        /* adjusting starting position of the window */
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        this.setLocation(dim.width/2, dim.height/3);

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLayout(new GridLayout(9,1));
        this.setTitle("Create a class");

        /* creating main layout */
        label1 = new JLabel();
        JPanel panel1 = new JPanel();
        panel1.setBackground(new Color(0X08c8ff));
        panel1.add(label1);

        label2 = new JLabel();
        label2.setText("                       Please choose the file you want to share.                       ");
        JPanel panel2 = new JPanel();
        panel2.setBackground(new Color(0X08c8ff));
        panel2.add(label2);

        fileChooser = new JButton();
        fileChooser.setText("Choose a file");
        fileChooser.setFocusable(false);
        fileChooser.addActionListener(this);
        JPanel panel3 = new JPanel();
        panel3.setBackground(new Color(0X08c8ff));
        panel3.add(fileChooser);

        label4 = new JLabel();
        JPanel panel4 = new JPanel();
        panel4.setBackground(new Color(0X08c8ff));
        panel4.add(label4);

        createClass = new JButton();
        createClass.setText("Create class");
        createClass.setFocusable(false);
        createClass.addActionListener(this);
        createClass.setVisible(false);
        JPanel panel5 = new JPanel();
        panel5.setBackground(new Color(0X08c8ff));
        panel5.add(createClass);

        label6 = new JLabel();
        JPanel panel6 = new JPanel();
        panel6.setBackground(new Color(0X08c8ff));
        panel6.add(label6);

        label7 = new JLabel();
        label7.setFont(new Font("Consolas",Font.BOLD,25));
        JPanel panel7 = new JPanel();
        panel7.setBackground(new Color(0X08c8ff));
        panel7.add(label7);


        goToClass = new JButton();
        goToClass.setText("Go to class");
        goToClass.setFocusable(false);
        goToClass.addActionListener(this);
        goToClass.setVisible(false);
        JPanel panel8 = new JPanel();
        panel8.setBackground(new Color(0X08c8ff));
        panel8.add(goToClass);

        JPanel panel9 = new JPanel();
        panel9.setBackground(new Color(0X08c8ff));


        /* adding panels to main frame */
        this.add(panel1);
        this.add(panel2);
        this.add(panel3);
        this.add(panel4);
        this.add(panel5);
        this.add(panel6);
        this.add(panel7);
        this.add(panel8);
        this.add(panel9);


        this.pack();
        this.setVisible(true);
    }




    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            /* file choosing button */
            if (e.getSource() == fileChooser) {
                //System.out.println("file will be chosen");
                JFileChooser jFileChooser = new JFileChooser();
                jFileChooser.setDialogTitle("Choose PDF to share");
                label4.setText("");
                createClass.setVisible(false);
                label7.setText("");
                goToClass.setVisible(false);
                createClassCount = 0;

                /* with this filter, teacher can only share pdf files */
                FileNameExtensionFilter filter = new FileNameExtensionFilter("PDF", "pdf");
                jFileChooser.setFileFilter(filter);


                /* choosing file  */
                if (jFileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    file = jFileChooser.getSelectedFile();
                    System.out.println(file);

                    label4.setText("File you are sharing: "+file.getName());
                    createClass.setVisible(true);


                }

            }

            /* button to create class - it will get the classCode from server and show the class code to teacher */
            if (e.getSource() == createClass){
                createClassCount++;
                if (createClassCount == 1) {
                    /* -------------------------- class code will be given by server *******************************/


                    /* sending file to server */
                    fileInputStream = new FileInputStream(file.getAbsolutePath());
                    socket = new Socket(IP, 8541);

                    dataOutputStream = new DataOutputStream(socket.getOutputStream());
                    dataInputStream = new DataInputStream(socket.getInputStream());

                    String userType = new String("Teacher");
                    byte[] userTypeBytes = userType.getBytes();

                    String filename = new String(file.getName());
                    byte[] filenameBytes = filename.getBytes();

                    byte[] fileContentByte = new byte[(int) file.length()];
                    fileInputStream.read(fileContentByte);

                    /* sending userType first */
                    dataOutputStream.writeInt(userTypeBytes.length);
                    dataOutputStream.write(userTypeBytes);

                    /* sending filename */
                    dataOutputStream.writeInt(filenameBytes.length);
                    dataOutputStream.write(filenameBytes);

                    /* sending file */
                    dataOutputStream.writeInt(fileContentByte.length);
                    dataOutputStream.write(fileContentByte);

                    /* reading class code from server */
                    classCode = dataInputStream.readInt();



                    /* classCode will be given by server */
                    label7.setText("Class code: " + classCode);
                    goToClass.setVisible(true);
                }

            }

            /* button to go class to change the window and start to sharing pdf */
            if (e.getSource() == goToClass){
                System.out.println("go to class");
                /* starting stream in a different thread */
                Executors.newSingleThreadExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        startStreamFromTeacher();
                    }
                });
                goToClass.setEnabled(false);
            }




        }
        catch (Exception exception){
            closeEverything();
        }

    }

    public void startStreamFromTeacher(){

        /* unsatisfiedlinkerror */
        System.load(System.getProperty("user.dir")+"\\lib\\fsdk_java_win64.dll");

        String sn = ""; // initialize the sn
        String key = ""; // initialize the key
        int error_code = Library.initialize(sn, key);
        if (error_code != e_ErrSuccess) {
            System.out.println("error");
            closeEverything();
        }

        try {
            Desktop.getDesktop().open(file); // opening the pdf viewer

            /* creating path for pdf file to create fdf file */
            String pdf_path_for_fdf_creation = file.getAbsolutePath().replace('\\','/');

            /* creating path for fdf creation */
            String fdf_path = file.getAbsolutePath();
            if(System.getProperty("os.name").toLowerCase().contains("windows")){ // if OS is windows
                fdf_path =  fdf_path.substring(0, fdf_path.lastIndexOf("\\")+1);
                fdf_path += classCode.toString() + ".fdf";
            }
            else{ //if OS is not windows
                fdf_path =  fdf_path.substring(0, fdf_path.lastIndexOf("/")+1);
                fdf_path += classCode.toString() + ".fdf";
            }


            FDFOperations.createEmptyFDF(classCode.toString(), pdf_path_for_fdf_creation);
            File fdfFile = new File(fdf_path);
            byte[] f1 = Files.readAllBytes(fdfFile.toPath());
            byte[] f2;
            boolean isEqual;
            FileInputStream fis;


            /* as long as program is open, streaming will continue from teacher whether PDF viewer is on or off */
            while (socket.isConnected()){
                /* saving the pdf in pdf viewer with CTRL+S */
                Robot robot = new Robot();
                robot.keyPress(VK_CONTROL);
                Thread.sleep(10);
                robot.keyPress(VK_S);
                Thread.sleep(10);
                robot.keyRelease(VK_S);
                robot.keyRelease(VK_CONTROL);

                Range emptyRange = new Range();
                {
                    /* creating FDFDoc object */
                    FDFOperations.createEmptyFDF(classCode.toString(), pdf_path_for_fdf_creation);
                    FDFDoc fdf_doc = new FDFDoc(fdf_path);
                    PDFDoc pdf_doc = new PDFDoc(file.getAbsolutePath());
                    pdf_doc.load(null);

                    /* export to created fdf file - fill the fdf with annotations */
                    pdf_doc.exportToFDF(fdf_doc, e_Annots, emptyRange);

                    fdf_doc.saveAs(fdf_path);
                }

                f2 = Files.readAllBytes(fdfFile.toPath());

                /* checking if there is a new annotation */
                isEqual = Arrays.equals(f1, f2);

                /* updating f1 for next round */
                f1 = f2;

                /* sending fdf file to server if there are new annotations*/
                if(!isEqual) {
                    fis = new FileInputStream(fdfFile.getAbsolutePath());
                    byte[] fileContentByte = new byte[(int) fdfFile.length()];
                    fis.read(fileContentByte);
                    /* sending file */
                    dataOutputStream.writeInt(fileContentByte.length);
                    dataOutputStream.write(fileContentByte);
                }
                /* sleeping 2 seconds */
                Thread.sleep(2000);

            }
        } catch (Exception e) {
            closeEverything();
        }
        closeEverything();
    }

    /* it closes everything */
    private void closeEverything(){
        try {
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            if (dataOutputStream != null) {
                dataOutputStream.close();
            }
            if (dataInputStream != null) {
                dataInputStream.close();
            }
            if (socket != null) {
                socket.close();
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }
        exit(1);
    }


}
