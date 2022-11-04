import java.io.*;
import java.net.*;
import java.awt.*;
import javax.swing.*;

/**
 * Class for Server
 * 
 * Compiling in terminal: javac Server.java
 * Usage in terminal: java Server <port_number>
 */
public class Server {

    private ServerSocket serverSocket;
    private String username;
    private JFrame frame;
    private JTextArea enteredText;
    private JTextField typedText;
    private DefaultListModel<String> listModelUsers;
    private JList<String> usersList;
    private DefaultListModel<String> listModelRooms;
    private JList<String> roomsList;

    /**
     * Constructor for ServerSocket
     * 
     * @param serverSocket assign the socket to this instance of server
     */
    public Server(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;

        frame = new JFrame();

        // create button with image record.png
        enteredText = new JTextArea(10, 32);
        typedText = new JTextField(32);

        listModelUsers = new DefaultListModel<String>();
        // set background color of list
        listModelUsers.addElement("Online Users:");
        listModelRooms = new DefaultListModel<String>();
        listModelRooms.addElement("Rooms:   ");

        usersList = new JList<String>(listModelUsers);
        roomsList = new JList<String>(listModelRooms);

        enteredText.setEditable(false);
        usersList.setEnabled(false);
        roomsList.setEnabled(false);
        // set text Color
        enteredText.setForeground(Color.WHITE);
        enteredText.setBackground(Color.BLACK);
        usersList.setForeground(Color.WHITE);
        usersList.setBackground(Color.BLACK);
        roomsList.setForeground(Color.WHITE);
        roomsList.setBackground(Color.BLACK);
        typedText.setForeground(Color.WHITE);
        typedText.setBackground(Color.BLACK);

        Container content = frame.getContentPane();
        content.setBackground(Color.BLACK);
        content.setForeground(Color.BLACK);
        content.add(new JScrollPane(enteredText), BorderLayout.CENTER);
        // content.add(typedText, BorderLayout.SOUTH);
        // add button next to typedText
        content.add(usersList, BorderLayout.EAST);
        content.add(roomsList, BorderLayout.WEST);
        enteredText.setPreferredSize(new Dimension(300, 50));

        content.add(typedText, BorderLayout.SOUTH);
        // frame.add(button, BorderLayout.SOUTH);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
        frame.setTitle("SERVER");
        typedText.requestFocusInWindow();
    }

    /**
     * The method that creates threads for handling each client
     */
    public void startServer() {
        try {
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();

                ClientHandler clientHandler = new ClientHandler(socket, enteredText, listModelUsers,
                        listModelRooms);
                // clientHandler.checkUsername();
                System.out.println("New Client Connected!");
                enteredText.insert(
                        "New Client Connected!\n",
                        enteredText.getText().length());
                Thread thread = new Thread(clientHandler);
                thread.start();

                // create thread to listen for incoming files to receive

            }

        } catch (IOException e) {
            closeServerSocket();
        }

    }

    /**
     * Method that closes server socket
     */
    public void closeServerSocket() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks for valid port number
     * 
     * @param args should only contain port number
     * @throws IOException regarding serverSocket creation
     */
    public static void main(String[] args) throws IOException {
        int port = 12345;

        ServerSocket serverSocket = new ServerSocket(port);
        Server server = new Server(serverSocket);
        server.startServer();
    }
}
