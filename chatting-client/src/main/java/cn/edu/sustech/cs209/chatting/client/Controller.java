package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
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
    @FXML
    VBox rootLayout;
    @FXML
    Button exitButton;

    String username;

    String chatWith;

    ObservableList<String> onlineFriends = FXCollections.observableArrayList();
    ObservableList<String> chattingFriends = FXCollections.observableArrayList();

    HashMap<String, ObservableList<Message>> chatHistory = new HashMap<>();

    DatagramSocket socket;

    ClientThread clientThread;

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
            clientThread = new ClientThread(this, socket);
            clientThread.start();

        } else {
            System.out.println("Invalid username " + input + ", exiting");
            Platform.exit();
        }

        chatContentList.setCellFactory(new MessageCellFactory());
        chatList.setItems(chattingFriends);
        chatList.setOnMouseClicked(mouseEvent -> {
            if(mouseEvent.getClickCount() == 1){
                chatWith = chatList.getSelectionModel().getSelectedItem();
                chatContentList.setItems(chatHistory.get(chatWith));
            }
        });

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
        Stage stage = new Stage();
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 20, 20));


        // Create ListView for user selection
        ListView<String> listView = new ListView<>();
        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listView.setItems(onlineFriends);

        // Create OK button
        Button okBtn = new Button("OK");
        okBtn.setOnAction(event -> {
            // Get selected users
            List<String> selectedUsers = listView.getSelectionModel().getSelectedItems();
            System.out.println("hi");
            for (String selectedUser : selectedUsers) {
                System.out.println(selectedUser);
            }
            List<String> list = new ArrayList<>(selectedUsers);
            for (String selectedUser : list) {
                System.out.println("late " + selectedUser);
            }

            // Generate group chat name
            StringBuilder groupNameBuilder = new StringBuilder();
            list.sort(String::compareTo);
            if (list.size() > 3) {
                for (int i = 0; i < 3; i++) {
                    groupNameBuilder.append(list.get(i)).append(", ");
                }
                groupNameBuilder.append("... (").append(list.size()).append(")");
            } else {
                groupNameBuilder.append(String.join(", ", list)).append(" (").append(list.size()).append(")");
            }


            String groupName = groupNameBuilder.toString();
            for (int i = 0; i < list.size(); i++) {
                groupName = groupName + "," + list.get(i);
            }
            System.out.println(groupName);
            // Create new group chat item and add it to the left panel
            byte[] mes = ("formingGroup," + groupName).getBytes();
            DatagramPacket packet = new DatagramPacket(mes, mes.length, address, SERVER_PORT);
            try {
                socket.send(packet);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            stage.close();
        });

        // Add nodes to grid
        grid.add(listView, 0, 0);
        grid.add(okBtn, 0, 1);

        stage.setScene(new Scene(grid));
        stage.show();
    }

    @FXML
    public void handleClose(){
        byte[] mes = ("closeCon," + username).getBytes();
        DatagramPacket packet = new DatagramPacket(mes, mes.length, address, SERVER_PORT);
        try {
            socket.send(packet);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        clientThread.setRunning(false);
        //socket.close();

    }

    public void offUser(String offName){
        onlineFriends.remove(offName);
        if(chattingFriends.contains(offName)){
            if(chatWith.equals(offName)){
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Information Dialog");
                alert.setHeaderText(null);
                alert.setContentText("User " + offName + " has offlined");
                alert.showAndWait();
                chattingFriends.remove(offName);
            }
        }


    }

    public void offGroup(){
        if(chatWith.split(",").length == 1 && chatWith.charAt(chatWith.length() - 1) != ')'){
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information Dialog");
            alert.setHeaderText(null);
            alert.setContentText("No group selected");
            alert.showAndWait();
        }
        else if(chatWith.split(",").length == 1 && chatWith.charAt(chatWith.length() - 1) == ')'){
            chattingFriends.remove(chatWith);
            chatHistory.remove(chatWith);
            chatContentList.setItems(null);
            byte[] mes = ("deleteGroup,"+chatWith).getBytes();
            DatagramPacket packet = new DatagramPacket(mes, mes.length, address, SERVER_PORT);
            try {
                socket.send(packet);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            chatWith = null;
        }
        else{
            chattingFriends.remove(chatWith);
            chatHistory.remove(chatWith);
            chatContentList.setItems(null);
            if(!chatWith.contains(username)){
                int leftIndex = chatWith.indexOf("(");
                int rightIndex = chatWith.indexOf(")");
                int number = Integer.parseInt(chatWith.substring(leftIndex + 1, rightIndex)) - 1;
                String newGroup = chatWith.substring(0, leftIndex + 1) + number + chatWith.substring(rightIndex);
                byte[] mes = ("updateGroup;" + chatWith + ";" + newGroup + ";" + username).getBytes();
                DatagramPacket packet = new DatagramPacket(mes, mes.length, address, SERVER_PORT);
                try {
                    socket.send(packet);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            else{
                int nameIndex = chatWith.indexOf(username);
                String newGroup = "";
                if(chatWith.charAt(nameIndex + username.length() + 1)  == '('){
                    newGroup = chatWith.replaceFirst(", "+username, "");
                }
                else{
                    newGroup = chatWith.replaceFirst(username + ", ", "");
                }
                int leftIndex = newGroup.indexOf("(");
                int rightIndex = newGroup.indexOf(")");
                int number = Integer.parseInt(newGroup.substring(leftIndex + 1, rightIndex)) - 1;
                newGroup = newGroup.substring(0, leftIndex + 1) + number + newGroup.substring(rightIndex);
                byte[] mes = ("updateGroup;" + chatWith + ";" + newGroup + ";" + username).getBytes();
                DatagramPacket packet = new DatagramPacket(mes, mes.length, address, SERVER_PORT);
                try {
                    socket.send(packet);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            chatWith = null;
        }
    }

    public void updateGroup(String oldGroup, String newGroup){
        ObservableList<Message> list = chatHistory.get(oldGroup);
        chatHistory.remove(oldGroup);
        chatHistory.put(newGroup, list);
        chattingFriends.remove(oldGroup);
        chattingFriends.add(newGroup);
        if(chatWith.equals(oldGroup)){
            chatWith = newGroup;
        }
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
        byte[] mes = null;
        if(sentTo.contains("(")){
            mes = ("groupMessage,"+timeStamp+"," + sentBy+","+sentTo+","+data).getBytes();
        }
        else mes = ("normal,"+timeStamp+"," + sentBy+","+sentTo+","+data).getBytes();
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
        inputArea.clear();


    }
    public void addUser(String user){
//        if(!user.equals(username)){
//            onlineFriends.add(user);
//        }
        onlineFriends.add(user);
    }
    public void receiveMes(String sendBy, Message mes){
        if(chatHistory.containsKey(sendBy)){
            System.out.println(1);
            chatHistory.get(sendBy).add(mes);
        }
        else{
            System.out.println(2);
            chatHistory.put(sendBy, FXCollections.observableArrayList());
            chatHistory.get(sendBy).add(mes);
        }
        if(chattingFriends.size() == 0){
            System.out.println(3);
            chattingFriends.add(sendBy);
            chatContentList.setItems(chatHistory.get(sendBy));
            chatWith = sendBy;
        }
        else if(!chattingFriends.contains(sendBy)){
            System.out.println(4);
            chattingFriends.add(sendBy);
        }

    }

    public void receiveGroupMes(Message mes, String groupName){
        if(chatHistory.containsKey(groupName)){
            chatHistory.get(groupName).add(mes);
        }
        else{
            chatHistory.put(groupName, FXCollections.observableArrayList());
            chatHistory.get(groupName).add(mes);
        }
        if(chattingFriends.size() == 0){
            chattingFriends.add(groupName);
            chatContentList.setItems(chatHistory.get(groupName));
            chatWith = groupName;
        }
        else if(!chattingFriends.contains(groupName)){
            chattingFriends.add(groupName);
        }

    }

    public void groupMessage(String name){
        if(!chatHistory.containsKey(name)){
            chatHistory.put(name, FXCollections.observableArrayList());
        }
        if(chattingFriends.size() == 0){
            chattingFriends.add(name);
            chatContentList.setItems(chatHistory.get(name));
            chatWith = name;
        }
        else if(!chattingFriends.contains(name)){
            chattingFriends.add(name);
        }
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
