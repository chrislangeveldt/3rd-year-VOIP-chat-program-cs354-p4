import java.io.*;
import java.net.*;
import java.util.*;

import javax.swing.DefaultListModel;
import javax.swing.JTextArea;

/**
 * Threads that handle each client
 */
public class ClientHandler implements Runnable {

    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<>();
    public static ArrayList<String> rooms = new ArrayList<>();
    private Socket socket;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    private String username;
    private String room;
    private JTextArea enteredText;
    private DefaultListModel<String> listModelUsers;
    private DefaultListModel<String> listModelRooms;

    /**
     * Constructor for this handler
     * 
     * @param socket         Is the socket that the client connects to
     * @param listModelRooms
     * @param listModelUsers
     * @param enteredText
     */
    public ClientHandler(Socket socket, JTextArea enteredText, DefaultListModel<String> listModelUsers,
            DefaultListModel<String> listModelRooms) {
        try {
            this.socket = socket;

            this.oos = new ObjectOutputStream(socket.getOutputStream());
            this.ois = new ObjectInputStream(socket.getInputStream());
            this.room = "General";
            this.enteredText = enteredText;
            this.listModelUsers = listModelUsers;
            this.listModelRooms = listModelRooms;

            if (rooms.size() == 0) {
                rooms.add(room);
                // add to list roomsList
                listModelRooms.addElement(room);
            }

            // this.username = ((Message) objectInputStream.readObject()).text();//waits for
            // message to be sent

        } catch (IOException e) {
            closeEverything();
        }
    }

    /**
     * The thread listens for messages from the client and handles it
     */
    @Override
    public void run() {
        // run on every thread
        // thread waiting and sending for each message
        Message msg;

        // check username uniqueness
        outer: while (socket.isConnected()) {
            try {
                String username = (String) ois.readObject();
                for (ClientHandler handler : clientHandlers) {
                    if (handler.username.equals(username)) {
                        oos.writeObject(new String("username exists"));
                        oos.flush();
                        continue outer;
                    }
                }
                this.username = username;
                // add to list of users
                listModelUsers.addElement(username);
                oos.writeObject(new String("username unique"));
                oos.flush();
                break;
            } catch (Exception e) {
                closeEverything();
                return;
            }
        }

        // send user list and new connection alert
        if (socket.isConnected()) {
            ArrayList<String> usernames = new ArrayList<String>();
            for (ClientHandler handler : clientHandlers) {
                if (handler.room.equals(room)) {
                    usernames.add(handler.username);
                }
            }
            try {
                oos.writeObject(usernames);
                oos.flush();// manual clear before it fills

                oos.writeObject(rooms);
                oos.flush();
            } catch (IOException e) {
                closeEverything();
                return;
            }
            clientHandlers.add(this);
            broadcastRoom(new Message(username + " has entered the chat!", "SERVER"));
        }

        while (socket.isConnected()) {
            try {
                msg = (Message) ois.readObject();
                if (msg.text().startsWith("@")) {
                    whisper(msg);
                } else if (msg.text().startsWith("/")) {
                    serverMessage(msg);
                } else {
                    broadcastRoom(msg);
                }
            } catch (Exception e) {
                closeEverything();
                return;
            }
        }
    }

    /**
     * @param msg from server message
     */
    public void serverMessage(Message msg) {
        String[] parts = msg.text().split(" ", 2);
        String cmd = parts[0];
        String text = parts[1];

        switch (cmd) {
            case "/create":
                if (rooms.contains(text)) {
                    try {
                        oos.writeObject(new Message("Room " + text + " already exists", "SERVER"));
                        oos.flush();
                        return;
                    } catch (IOException e) {
                        closeEverything();
                    }
                }
                rooms.add(text);
                listModelRooms.addElement(text);
                broadcast(new Message("Room created - " + text, "SERVER"));
                break;

            case "/join":
                if (rooms.contains(text)) {
                    broadcastRoom(new Message(username + " has left the room!", "SERVER"));
                    this.room = text;
                    broadcastRoom(new Message(username + " has entered the room!", "SERVER"));
                    ArrayList<String> usernames = new ArrayList<String>();
                    for (ClientHandler handler : clientHandlers) {
                        if (handler.room.equals(room)) {
                            usernames.add(handler.username);
                        }
                    }
                    try {
                        oos.writeObject(usernames);
                        oos.flush();// manual clear before it fills
                    } catch (IOException e) {
                        closeEverything();
                        return;
                    }
                    return;
                }

                try {
                    oos.writeObject(new Message("Room " + text + " does not exist", "SERVER"));
                    oos.flush();
                } catch (IOException e) {
                    closeEverything();
                    return;
                }
                break;

            // case "/listen":
            // msg.setText("/listen ");
            // System.out.println("listening");
            // broadcastRoom(msg);
            // break;

            default:
                broadcastRoom(msg);
                break;
        }
    }

    /**
     * @param msg broadcastRoom
     */
    public void broadcastRoom(Message msg) {
        for (ClientHandler clientHandler : clientHandlers) {
            if (clientHandler.room.equals(room)) {
                try {
                    clientHandler.oos.writeObject(msg);
                    clientHandler.oos.flush();// manual clear before it fills
                } catch (IOException e) {
                    closeEverything();
                }
            }
        }
    }

    /**
     * @param msg broadcast
     */
    public void broadcast(Message msg) {
        for (ClientHandler clientHandler : clientHandlers) {
            try {
                clientHandler.oos.writeObject(msg);
                clientHandler.oos.flush();// manual clear before it fills
            } catch (IOException e) {
                closeEverything();
            }
        }
    }

    /**
     * Send messages with and handles some exceptions
     * 
     * @param msg the object to send to clients
     */
    public void whisper(Message msg) {
        ArrayList<String> usernames = new ArrayList<String>();
        String text = msg.text();
        while (text.startsWith("@")) {
            String[] parts = text.split(" ", 2);
            usernames.add(parts[0].substring(1));
            text = parts[1];
        }

        // send to users an
        String txt = "whispers to";
        String errTxt = " the following users do not exist: ";
        ArrayList<ClientHandler> handlers = new ArrayList<ClientHandler>();
        outer: for (String name : usernames) {
            for (ClientHandler handler : clientHandlers) {
                if (handler.username.equals(name) && handler.room.equals(room)) {
                    handlers.add(handler);
                    txt += " " + name;
                    continue outer;
                }
            }
            errTxt += " " + name;
        }
        txt += ": " + text;
        if (usernames.size() > handlers.size()) {
            try {
                oos.writeObject(new Message(errTxt, "SERVER"));
                oos.flush();
            } catch (IOException e) {
                closeEverything();
                return;
            }
        }

        if (handlers.size() > 0) {
            try {
                oos.writeObject(new Message(txt, msg.from()));
                oos.flush();
                for (ClientHandler handler : handlers) {
                    handler.oos.writeObject(new Message(txt, msg.from()));
                    handler.oos.flush();
                }
            } catch (IOException e) {
                closeEverything();
                return;
            }
        }
    }

    /**
     * Neatly closes sockets and input output streams
     */
    public void closeEverything() {
        removeClientHandler();
        try {
            if (ois != null) {
                ois.close();
            }
            if (oos != null) {
                oos.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {

        }
    }

    /**
     * Remove client and send messages
     */
    public void removeClientHandler() {
        clientHandlers.remove(this);
        // list of users remove usernames from list
        listModelUsers.removeElement(username);
        broadcastRoom(new Message(username + " has left the chat!", "SERVER"));
        System.out.println("Client Disconnected!");
        enteredText.insert("Client " + username + " Disconnected!\n", enteredText.getText().length());

    }

}
