package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class Controller implements Initializable {
    private static final int SERVER_PORT = 8000;
    private static final InetAddress address;

    static {
        try {
            address = InetAddress.getByName("localhost");
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
    @FXML
    ListView<Message> chatContentList;
    @FXML
    ListView<String> chatList;
    @FXML
    TextArea inputArea;
    @FXML
    Label currentUsername;
    @FXML
    Label currentOnlineCnt;

    String username;

    String chatWith;

    ObservableList<String> onlineFriends = FXCollections.observableArrayList();
    ObservableList<String> chattingFriends = FXCollections.observableArrayList();

    HashMap<String, ObservableList<Message>> chatHistory = new HashMap<>();

    DatagramSocket socket;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        Dialog<String> dialog = new TextInputDialog();
        dialog.setTitle("Login");
        dialog.setHeaderText(null);
        dialog.setContentText("Username:");

        Optional<String> input = dialog.showAndWait();
        if (input.isPresent() && !input.get().isEmpty()) {
            /*
               TODO: Check if there is a user with the same name among the currently logged-in users,
                     if so, ask the user to change the username
             */
            username = input.get();
            //判断是否合法
            try {
                socket = new DatagramSocket();
            } catch (SocketException e) {
                throw new RuntimeException(e);
            }
            byte[] init = ("init," + socket.getLocalPort()+ "," + (address + "").split("/")[1]
                    + "," + username).getBytes();
            DatagramPacket initialize = new DatagramPacket(init, init.length, address, SERVER_PORT);
            try {
                socket.send(initialize);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            ClientThread clientThread = new ClientThread(this, socket);
            clientThread.start();

        } else {
            System.out.println("Invalid username " + input + ", exiting");
            Platform.exit();
        }

        chatContentList.setCellFactory(new MessageCellFactory());
        chatList.setItems(chattingFriends);

    }

    @FXML
    public void createPrivateChat() {
        AtomicReference<String> user = new AtomicReference<>();

        Stage stage = new Stage();
        ComboBox<String> userSel = new ComboBox<>();

        // FIXME: get the user list from server, the current user's name should be filtered out
        userSel.setItems(onlineFriends);
        //userSel.getItems().addAll("item1", "item2");

        Button okBtn = new Button("OK");
        okBtn.setOnAction(e -> {
            user.set(userSel.getSelectionModel().getSelectedItem());
            stage.close();
        });

        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20, 20, 20, 20));
        box.getChildren().addAll(userSel, okBtn);
        stage.setScene(new Scene(box));
        stage.showAndWait();

        // TODO: if the current user already chatted with the selected user, just open the chat with that user
        // TODO: otherwise, create a new chat item in the left panel, the title should be the selected user's name
        if(chatHistory.containsKey(user.get())){
            chatContentList.setItems(chatHistory.get(user.get()));
        }
        else{
            chattingFriends.add(user.get());
            chatWith = user.get();
            System.out.println(chatWith);
            chatHistory.put(user.get(), FXCollections.observableArrayList());
            chatContentList.setItems(chatHistory.get(user.get()));
        }
    }

    /**
     * A new dialog should contain a multi-select list, showing all user's name.
     * You can select several users that will be joined in the group chat, including yourself.
     * <p>
     * The naming rule for group chats is similar to WeChat:
     * If there are > 3 users: display the first three usernames, sorted in lexicographic order, then use ellipsis with the number of users, for example:
     * UserA, UserB, UserC... (10)
     * If there are <= 3 users: do not display the ellipsis, for example:
     * UserA, UserB (2)
     */
    @FXML
    public void createGroupChat() {

    }

    /**
     * Sends the message to the <b>currently selected</b> chat.
     * <p>
     * Blank messages are not allowed.
     * After sending the message, you should clear the text input field.
     */
    @FXML
    public void doSendMessage() {
        String data = inputArea.getText();
        Long timeStamp = System.currentTimeMillis();
        String sentBy = username;
        String sentTo = chatWith;
        byte[] mes = ("normal,"+timeStamp+"," + sentBy+","+sentTo+","+data).getBytes();
        DatagramPacket packet = new DatagramPacket(mes, mes.length, address, SERVER_PORT);
        try {
            socket.send(packet);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Message message = new Message(timeStamp, sentBy, sentTo, data);
        if(chatHistory.containsKey(sentTo)){
            chatHistory.get(sentTo).add(message);
        }
        else{
            chatHistory.put(sentTo, FXCollections.observableArrayList());
            chatHistory.get(sentTo).add(message);
        }


    }
    public void addUser(String user){
//        if(!user.equals(username)){
//            onlineFriends.add(user);
//        }
        onlineFriends.add(user);
    }
    public void receiveMes(String sendBy, Message mes){
        if(chatHistory.containsKey(sendBy)){
            chatHistory.get(sendBy).add(mes);
        }
        else{
            chatHistory.put(sendBy, FXCollections.observableArrayList());
            chatHistory.get(sendBy).add(mes);
        }
        chatWith = sendBy;

    }

    /**
     * You may change the cell factory if you changed the design of {@code Message} model.
     * Hint: you may also define a cell factory for the chats displayed in the left panel, or simply override the toString method.
     */
    private class MessageCellFactory implements Callback<ListView<Message>, ListCell<Message>> {
        @Override
        public ListCell<Message> call(ListView<Message> param) {
            return new ListCell<Message>() {

                @Override
                public void updateItem(Message msg, boolean empty) {
                    super.updateItem(msg, empty);
                    if (empty || Objects.isNull(msg)) {
                        setText(null);
                        setGraphic(null);
                        return;
                    }

                    HBox wrapper = new HBox();
                    Label nameLabel = new Label(msg.getSentBy());
                    Label msgLabel = new Label(msg.getData());

                    nameLabel.setPrefSize(50, 20);
                    nameLabel.setWrapText(true);
                    nameLabel.setStyle("-fx-border-color: black; -fx-border-width: 1px;");

                    if (username.equals(msg.getSentBy())) {
                        wrapper.setAlignment(Pos.TOP_RIGHT);
                        wrapper.getChildren().addAll(msgLabel, nameLabel);
                        msgLabel.setPadding(new Insets(0, 20, 0, 0));
                    } else {
                        wrapper.setAlignment(Pos.TOP_LEFT);
                        wrapper.getChildren().addAll(nameLabel, msgLabel);
                        msgLabel.setPadding(new Insets(0, 0, 0, 20));
                    }

                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    setGraphic(wrapper);
                }
            };
        }
    }
}
