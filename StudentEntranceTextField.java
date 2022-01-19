import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.util.concurrent.Executors;

import static java.lang.System.exit;

public class StudentEntranceTextField extends JFrame implements ActionListener {

    private static final String IP = "192.168.1.34";

    private FileOutputStream fileOutputStreamStream;
    private DataOutputStream dataOutputStream;
    private DataInputStream dataInputStream;
    private Socket socket;

    private JButton button;
    private JTextField textField;
    private JLabel label;
    private JLabel label2;
    private Integer classCode;

    StudentEntranceTextField() {
        /* adjusting starting position of the window */
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        this.setLocation(dim.width/2, dim.height/3);

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLayout(new GridLayout(3,1));
        this.setTitle("Join to a class");


        /* creating the question field */
        label = new JLabel("    Enter code for the class you are trying to join?    ");
        JPanel panel1 = new JPanel();
        panel1.setBackground(new Color(0X08c8ff));
        panel1.add(label);

        /* creating the join button and textfield */
        button = new JButton("Join");
        button.addActionListener(this);
        button.setFocusable(false);
        textField = new JTextField();
        textField.setPreferredSize(new Dimension(150,40));
        JPanel panel2 = new JPanel();
        panel2.setBackground(new Color(0X08c8ff));
        panel2.add(button);
        panel2.add(textField);

        /* creating the warning field */
        label2 = new JLabel();
        JPanel panel3 = new JPanel();
        panel3.setBackground(new Color(0X08c8ff));
        panel3.add(label2);

        this.add(panel1);
        this.add(panel2);
        this.add(panel3);

        this.pack();
        this.setVisible(true);
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        int notDigit = 0;
        /* checking if class code all consists of digits */
        if (e.getSource() == button && !textField.getText().isEmpty()) {
            for (int i = 0; i < textField.getText().length(); ++i) {
                if (!Character.isDigit(textField.getText().charAt(i))) {
                    notDigit = 1;
                    break;
                }
            }

            /* input does not contain of all digits */
            if (notDigit == 1) {
                textField.setText("");
                label2.setText("Warning! Only digits are allowed for class code.");
                label2.setForeground(Color.red);
                textField.setText("");

            }
            /* input contains of all digits */
            else {
                if (textField.getText().length() != 6) {
                    label2.setText("Warning! Class code consists of 6 digits.");
                    label2.setForeground(Color.red);
                    notDigit = 1;
                } else {
                    label2.setText("");
                    //System.out.println(Integer.parseInt(textField.getText()));
                }
            }

            if (notDigit == 0){
                try {
                    socket = new Socket(IP, 8541);
                    dataOutputStream = new DataOutputStream(socket.getOutputStream());
                    dataInputStream = new DataInputStream(socket.getInputStream());
                    /* sending userType */
                    String userType = new String("Student");
                    byte[] userTypeBytes = userType.getBytes();
                    dataOutputStream.writeInt(userTypeBytes.length);
                    dataOutputStream.write(userTypeBytes);

                    /* sending the classCode to server to get the PDF and connect to class */
                    dataOutputStream.writeInt(Integer.parseInt(textField.getText()));

                    /* reading message from server */
                    int messageLength = dataInputStream.readInt();
                    if (messageLength > 0) {
                        byte[] messageBytes = new byte[messageLength];
                        dataInputStream.readFully(messageBytes, 0, messageLength);
                        String message = new String(messageBytes);
                        if (message.equals("fail")) { // connection failed to class
                            label2.setText("You've entered an invalid classroom code!");
                        } else if (message.equals("success")) { // connected to class. get the pdf
                            int fileContentLength = dataInputStream.readInt();
                            if (fileContentLength > 0) {
                                byte[] fileContentBytes = new byte[fileContentLength];
                                dataInputStream.readFully(fileContentBytes, 0, fileContentLength);

                                classCode = Integer.valueOf(textField.getText());
                                String newFilename = textField.getText()+".pdf";
                                File fileToDownload = new File(newFilename);
                                FileOutputStream fileOutputStream = new FileOutputStream(fileToDownload);
                                fileOutputStream.write(fileContentBytes);
                                fileOutputStream.close();
                                System.out.println("file received");
                                /* starting stream in a different thread */
                                Executors.newSingleThreadExecutor().execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        startStreamToStudent(fileToDownload.getAbsolutePath());
                                    }
                                });
                                button.setEnabled(false);
                            }
                        } else {
                            closeEverything();
                        }


                    } else {
                        closeEverything();
                    }

                } catch (Exception exception) {
                    closeEverything();
                }

            }



        }
    }

    public void startStreamToStudent(String pdfPath){
        String fdfName = "";
        if(System.getProperty("os.name").toLowerCase().contains("windows")) // if OS is windows
            fdfName = pdfPath.substring(0, pdfPath.lastIndexOf("\\")+1);
        else
            fdfName = pdfPath.substring(0, pdfPath.lastIndexOf("/")+1);

        fdfName += classCode.toString() + ".fdf";
        String pdfPathtoUse = pdfPath.replace("\\","/");

        try {
            FDFOperations.createEmptyFDF(classCode.toString(), pdfPathtoUse);
            /* opening the file for the first time */
        //    Desktop.getDesktop().open(new File(pdfPath));
            Desktop.getDesktop().open(new File(fdfName));

            /* adjusting the path within fdf file according to student computer */
            File fdfToDownload = new File(fdfName);
        //    FDFOperations.changePDFDirectoryInFDF(classCode.toString(), fdfToDownload.getAbsolutePath());

            /* opening input and output streams */
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataInputStream = new DataInputStream(socket.getInputStream());

            /* getting annotations from teacher as long as program is open */
            while(socket.isConnected()) {
                int fileContentLength = dataInputStream.readInt(); // reading file length
                if (fileContentLength > 0) {
                    byte[] fileContentBytes = new byte[fileContentLength];
                    dataInputStream.readFully(fileContentBytes, 0, fileContentLength); // reading file content

                    /* saving the file */
                    fdfToDownload = new File(fdfName);
                    FileOutputStream fileOutputStream = new FileOutputStream(fdfToDownload);
                    fileOutputStream.write(fileContentBytes);
                    fileOutputStream.close();

                    /* adjusting the path within fdf file according to student computer */
                    FDFOperations.changePDFDirectoryInFDF(classCode.toString(), pdfPathtoUse);

                    /* opening the updated annotations on the screen */
                    Desktop.getDesktop().open(new File(fdfName));

                }
            }

        } catch (Exception e){
            closeEverything();
        }
        closeEverything();

    }


    /* it closes everything */
    private void closeEverything(){
        try {
            if (fileOutputStreamStream != null) {
                fileOutputStreamStream.close();
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
