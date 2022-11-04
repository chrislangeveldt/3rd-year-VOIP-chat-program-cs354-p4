import java.awt.Component;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import javax.swing.*;

/**
 * This class is for the thread that waits for messages
 */
public class ClientListenerThread implements Runnable {
    private String username;
    private Socket socket;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    private JTextArea enteredText;
    private DefaultListModel<String> listModelUsers;
    private DefaultListModel<String> listModelRooms;

    /**
     * Constructor sets up useful properties
     * 
     * @param socket      for connection to server
     * @param ois         for receiving messages
     * @param oos         for sending messages
     * @param enteredText for displaying received messages
     * @param listModel   for displaying list of users
     * @param frame       for displaying everything
     */
    public ClientListenerThread(String username, Socket socket, ObjectInputStream ois, ObjectOutputStream oos,
            JTextArea enteredText, DefaultListModel<String> listModelUsers, DefaultListModel<String> listModelRooms) {
        this.username = username;
        this.socket = socket;
        this.ois = ois;
        this.oos = oos;
        this.enteredText = enteredText;
        this.listModelUsers = listModelUsers;
        this.listModelRooms = listModelRooms;
    }

    /**
     * Thread execution that waits for messages while connected to server
     */
    @Override
    public void run() {
        if (socket.isConnected()) {
            try {
                ArrayList<String> usernames = (ArrayList<String>) ois.readObject();
                for (String name : usernames) {
                    listModelUsers.addElement(name);
                }

                ArrayList<String> rooms = (ArrayList<String>) ois.readObject();
                for (String room : rooms) {
                    listModelRooms.addElement(room);
                }
            } catch (ClassNotFoundException e) {
                closeEverything();
            } catch (IOException e) {
                closeEverything();
            }
        }

        while (socket.isConnected()) {
            try {
                printMessage((Message) ois.readObject());
            } catch (Exception e) {
                closeEverything();
            }
        }
    }

    /**
     * Handles the output of received messages
     * 
     * @param message The object received form server
     */
    public void printMessage(Message message) {
        String msg = message.from();
        if (msg.equals("SERVER")) {
            if (message.text().endsWith("has entered the chat!")) {
                listModelUsers.addElement(message.text().split(" has entered the chat", 2)[0]);
            } else if (message.text().endsWith("has entered the room!")) {
                if (message.text().split(" has entered the room!", 2)[0].equals(username)) {
                    listModelUsers.clear();
                    listModelUsers.addElement("Online Users:");
                    ArrayList<String> usernames = null;
                    try {
                        usernames = (ArrayList<String>) ois.readObject();
                    } catch (Exception e) {
                        closeEverything();
                    }
                    for (String name : usernames) {
                        listModelUsers.addElement(name);
                    }
                } else {
                    listModelUsers.addElement(message.text().split(" has entered the room", 2)[0]);
                }
            } else if (message.text().endsWith("has left the chat!")) {
                listModelUsers.removeElement(message.text().split(" has left the chat!", 2)[0]);
            } else if (message.text().endsWith("has left the room!")) {
                listModelUsers.removeElement(message.text().split(" has left the room!", 2)[0]);
            } else if (message.text().startsWith("Room created - ")) {
                listModelRooms.addElement(message.text().split(" - ", 2)[1]);
            }
        }
        if (message.text().startsWith("/vn")) {
            String encodedString = message.text().substring(4);
            byte[] decodedBytes = Base64.getDecoder().decode(encodedString);
            try {
                // writing file after being sent

                Files.write(Paths.get("received.wav"), decodedBytes);
            } catch (IOException e) {
                System.out.println("Error writing file");
            }
            // set message to voice note received
            // msg = "Voice note received - type /listen to listen";
            enteredText.insert("Voice note received - type /listen to listen\n", enteredText.getText().length());
        } else if (message.text().endsWith("/call") && message.text().startsWith("whispers to")
                && !message.from().equals(username)) {
            int result = JOptionPane.showConfirmDialog((Component) null,
                    "Click OK to accept call from " + message.from(),
                    "Incoming call", JOptionPane.OK_CANCEL_OPTION);
            if (result == 0) {
                String ip = "";
                while (ip.isBlank()) {
                    ip = JOptionPane.showInputDialog("Enter the callee IP address: ");
                }
                CallerThread caller = new CallerThread(ip);
                Thread thread = new Thread(caller);
                thread.start();

                ReceiverThread receiver = new ReceiverThread();
                Thread rthread = new Thread(receiver);
                rthread.start();
            }
        } else {
            msg += ": " + message.text();
            System.out.println(msg);
            enteredText.insert(msg + "\n", enteredText.getText().length());
        }
    }

    /**
     * Closes socket and streams neatly and exits
     */
    public void closeEverything() {
        try {
            if (ois != null) {
                ois.close();
            }
        } catch (IOException e) {
        }

        try {
            if (oos != null) {
                oos.close();
            }
        } catch (IOException e) {
        }

        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
        }

        System.out.println("SERVER: Shut down");
        enteredText.insert("SERVER: Shut down" + "\n",
                enteredText.getText().length());
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
        }
        System.exit(0);
    }
}
