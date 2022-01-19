import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private ServerSocket serverSocket;

    public Server(ServerSocket serverSocket){
        this.serverSocket = serverSocket;
    }

    public void startServer(){
        while(!serverSocket.isClosed()){
            try {
                Socket socket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(socket);

                System.out.println("A client has been connected.");

                Thread thread = new Thread(clientHandler);
                thread.start();

            } catch (Exception e){
                System.out.println("server going down");
            //    closeServerSocket();
            }
        }
    }


    public void closeServerSocket(){
        try{
            if (serverSocket != null){
                serverSocket.close();
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(8541);
        Server server = new Server(serverSocket);
        server.startServer();
    }

}
