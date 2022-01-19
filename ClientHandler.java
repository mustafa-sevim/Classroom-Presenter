import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

import static java.lang.System.exit;

public class ClientHandler implements Runnable{

    public static HashSet<HashSet<ClientHandler>> clientHandlers = new HashSet<>();
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
     Integer classCode;
     String userType;
    private Connection connection;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private FileInputStream fileInputStream;

    public ClientHandler(Socket socket) {

        try {
            this.socket = socket;
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

        } catch (Exception e){
            closeEverything();
        }

    }

    public void streamFromTeacher(){
        try{
            dataInputStream = new DataInputStream(socket.getInputStream());
            Integer cc = classCode;
            while(socket.isConnected()){
                int fileContentLength = dataInputStream.readInt();
                if (fileContentLength > 0) {
                    byte[] fileContentBytes = new byte[fileContentLength];
                    dataInputStream.readFully(fileContentBytes, 0, fileContentLength);

                    String newFilename = System.getProperty("user.dir") + "\\files\\" + classCode.toString() +".fdf";
                    File fileToDownload = new File(newFilename);
                    FileOutputStream fileOutputStream = new FileOutputStream(fileToDownload);
                    fileOutputStream.write(fileContentBytes);
                    fileOutputStream.close();
                    System.out.println("fdf received from class " + classCode);

                    /* send the new fdf file to students in a separate thread */
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                File fileToSend = new File(newFilename);
                                FileInputStream fis = new FileInputStream(fileToSend);

                                byte[] fileContentByte = new byte[(int) fileToSend.length()];
                                fis.read(fileContentByte); // creating byte[] of file to send
                            //    System.out.println("clientHandlers size: "+clientHandlers.size());
                                for (HashSet<ClientHandler> hs: clientHandlers) {
                            //        System.out.println("size: "+hs.size());
                                    for(ClientHandler ch: hs){
                                        if(ch.classCode.equals(cc) && ch.getUserType().equals("Student")) {
                                            /* getting the dataoutput stream to student */
                                            DataOutputStream dos = new DataOutputStream(ch.getSocket().getOutputStream());
                                            dos.writeInt(fileContentByte.length); // sending fdf file length
                                            dos.write(fileContentByte); // sending fdf file
                                        }
                                    }
                                }
                            } catch (Exception e){
                                closeEverything();
                            }
                        }
                    }).start();

                }

            }
        } catch (Exception e){
            closeEverything();
        }

        closeEverything();

    }



    @Override
    public void run() {
        try {
            if (socket.isConnected()) {
                while (socket.isConnected()) {
                    try {
                        /* reading length of userType */
                        dataInputStream = new DataInputStream(socket.getInputStream());
                        int userTypeLength = dataInputStream.readInt();
                        if (userTypeLength > 0) {
                            byte[] userTypeBytes = new byte[userTypeLength];
                            dataInputStream.readFully(userTypeBytes, 0, userTypeLength);
                            this.userType = new String(userTypeBytes);

                            if (userType.equals("Teacher")) { // if user type is Teacher
                                /* reading filename */
                                int filenameLength = dataInputStream.readInt();
                                if (filenameLength > 0) {
                                    byte[] filenameBytes = new byte[filenameLength];
                                    dataInputStream.readFully(filenameBytes, 0, filenameLength);
                                    String filename = new String(filenameBytes, StandardCharsets.UTF_8);

                                    /* reading file */
                                    int fileContentLength = dataInputStream.readInt();
                                    if (fileContentLength > 0) {
                                        byte[] fileContentBytes = new byte[fileContentLength];
                                        dataInputStream.readFully(fileContentBytes, 0, fileContentLength);

                                        try {
                                            String query;

                                            /* get a connection to db */
                                            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/classroompresenter", "root", "password");
                                            /* prepare a statement */
                                            Statement statement = connection.createStatement();
                                            ResultSet resultSet;

                                            Random rand = new Random();
                                            classCode = rand.nextInt(900000);
                                            classCode += 100000;

                                            int isExist = 0;
                                            while (isExist == 0) {
                                                /* execute query */
                                                query = "select class_code from classroomfiles";
                                                resultSet = statement.executeQuery(query);
                                                while (resultSet.next()) {
                                                    isExist = 0;
                                                    /* there is another class with this classCode */
                                                    if (resultSet.getInt("class_code") == classCode) {
                                                        isExist = 1;
                                                    }
                                                }
                                                if (isExist == 1) {
                                                    /* create another classCode */
                                                    classCode = rand.nextInt(900000);
                                                    classCode += 100000;
                                                } else {
                                                    break;
                                                }

                                            }

                                            File fileToDownload = new File(System.getProperty("user.dir") + "\\files\\" + classCode.toString() + ".pdf");
                                            FileOutputStream fileOutputStream = new FileOutputStream(fileToDownload);
                                            fileOutputStream.write(fileContentBytes);
                                            fileOutputStream.close();
                                            System.out.println("New class file: " + fileToDownload);

                                            /* add the file into db */
                                            query = "insert into classroomfiles values(?,?,?)";
                                            PreparedStatement preparedStatement = connection.prepareStatement(query);
                                            preparedStatement.setInt(1, this.classCode);
                                            preparedStatement.setString(2, filename);
                                            preparedStatement.setString(3, fileToDownload.getAbsolutePath());
                                            preparedStatement.executeUpdate();

                                            /* sending back the class code to teacher */
                                            dataOutputStream = new DataOutputStream(socket.getOutputStream());
                                            dataOutputStream.writeInt(classCode);




                                        } catch (Exception e) {
                                            closeEverything();
                                        }

                                    }
                                }
                            } else if (userType.equals("Student")) {
                                try {
                                    ResultSet resultSet;
                                    String query;
                                    /* get a connection to db */
                                    connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/classroompresenter", "root", "password");

                                    /* read the classCode that student want to connect */
                                    dataInputStream = new DataInputStream(socket.getInputStream());
                                    dataOutputStream = new DataOutputStream(socket.getOutputStream());
                                    classCode = dataInputStream.readInt();

                                    /* prepare a statement */
                                    query = "select * from classroomfiles where class_code=?";
                                    PreparedStatement preparedStatement = connection.prepareStatement(query);
                                    preparedStatement.setInt(1, classCode);
                                    resultSet = preparedStatement.executeQuery();

                                    String filePath = "none";
                                    String messageToSend;
                                    while (resultSet.next()) {
                                        filePath = resultSet.getString("file_path");
                                    }

                                    /* connected to class. pdf will be shared */
                                    if (resultSet != null && !filePath.equals("none")) {
                                        /* sending 'success' message */
                                        messageToSend = "success";
                                        byte[] messageBytes = messageToSend.getBytes();
                                        dataOutputStream.writeInt(messageBytes.length);
                                        dataOutputStream.write(messageBytes);

                                        /* sending the file */
                                        File file = new File(filePath);
                                        fileInputStream = new FileInputStream(file);

                                        byte[] fileContentByte = new byte[(int) file.length()];
                                        fileInputStream.read(fileContentByte);

                                        dataOutputStream.writeInt(fileContentByte.length);
                                        dataOutputStream.write(fileContentByte); // message and file has been sent.

                                    }
                                    /* not connected to class. warning will be shared */
                                    else {
                                        /* sending 'fail' message */
                                        messageToSend = "fail";
                                        byte[] messageBytes = messageToSend.getBytes();
                                        dataOutputStream.writeInt(messageBytes.length);
                                        dataOutputStream.write(messageBytes);
                                        System.out.println("failed to connect");
                                    }

                                    /*------------------!!!!! here, student's sharing page will be called !!!!!------------------*/


                                } catch (Exception e) {
                                    closeEverything();
                                }


                            } else { // that means that there is a problem because userType must be either Teacher or Student
                                closeEverything();
                            }

                        }

                        /* teachers are at 0th index. other than index zero, there are students for each class */
                        /* adding clients into hashset */
                        if (userType.equals("Teacher")) {
                            boolean found = false;
                            for (HashSet<ClientHandler> hs : clientHandlers) {
                                if (hs.contains(this)) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                HashSet<ClientHandler> newSet = new HashSet<>();
                                newSet.add(this);
                                clientHandlers.add(newSet);
                            }
                        }

                        /* adding student to class */
                        else if (userType.equals("Student")) {
                            for (HashSet<ClientHandler> hs : clientHandlers) {
                                for (ClientHandler ch : hs) {
                                    if (ch.classCode.equals(this.classCode)) {
                                        hs.add(this);
                                        System.out.println("student added");
                                    }

                                    break;
                                }

                            }

                        }

                        if (userType.equals("Teacher")) {
                            streamFromTeacher();
                        } else if (userType.equals("Student")) {
                            // streamToStudent();
                        }

                    } catch (Exception e) {
                        closeEverything();
                        break;
                    }
                }
            }
        } finally {
            /* thread to call on exit */
            ExitThread exitThread = new ExitThread(userType);
            exitThread.start();
        }
    }


    private void closeEverything(){
        try {
            if (bufferedWriter != null){
                bufferedWriter.close();
            }
            if (bufferedReader != null){
                bufferedReader.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            if(userType.equals("Teacher"))
                System.out.println("\nprogram executed for class " + classCode);
        //    Thread.currentThread().interrupt();
        }

    }

    public Socket getSocket() {
        return this.socket;
    }

    public String getUserType() {
        return userType;
    }

    public class ExitThread extends Thread {
        private String userType;

        public ExitThread(String userType){
            this.userType = userType;
        }

        @Override
        public void run() {
            /* fdf and pdf files will be deleted at program exit */
            try {
                if(userType.isBlank())
                    return;
                else if (userType.equals("Student")){
                    /* remove student from class */
                    Iterator<HashSet<ClientHandler>> iter = clientHandlers.iterator();
                    while(iter.hasNext()){
                        HashSet<ClientHandler> hs = iter.next();
                        Iterator<ClientHandler> iter2= hs.iterator();
                        while(iter2.hasNext()){
                            ClientHandler ch = iter2.next();
                            if(ch.classCode.equals(classCode)){
                                hs.remove(this);
                                System.out.println("A student has left the class "+classCode);
                            }
                            break;
                        }
                    }
                    return;
                }

                File pdfToRemove = new File(System.getProperty("user.dir") + "\\files\\" + classCode.toString() + ".pdf");
                if (pdfToRemove.delete())
                    System.out.println("\n" + classCode + ".pdf has been removed from server");


                /* remove the informations from database */
                String query = "delete from classroomfiles where class_code=?";
                PreparedStatement preparedStatement = connection.prepareStatement(query);
                preparedStatement.setInt(1, classCode);
                preparedStatement.executeUpdate();
                System.out.println("Class " + classCode + " record's has been removed from server database");


                /* remove class from clientHandlers - iterator is used because of concurrentmodificationexception */
                Iterator<HashSet<ClientHandler>> iter = clientHandlers.iterator();
                while(iter.hasNext()){
                    HashSet<ClientHandler> hs = iter.next();
                    Iterator<ClientHandler> iter2= hs.iterator();
                    while(iter2.hasNext()){
                        if(iter2.next().classCode.equals(classCode)){
                            clientHandlers.remove(hs);
                        }
                        break;
                    }
                }
                System.out.println("Class " + classCode + " removed from server.\n");


            } catch (Exception e){
                // do nothing
                System.out.println("exception thrown");
            }

        }
    }

}
