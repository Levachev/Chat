package org.example.Client;

import javax.swing.*;
import java.awt.*;

public class GUI extends JFrame {
    public JTextField inputField;
    private JTextArea data;
    public JTextArea helperArea;
    public JTextArea nicNameArea;
    private JPanel headerPanel;
    private JScrollPane jScrollPane;

    GUI(){
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = (int) screenSize.getWidth();
        int height = (int) screenSize.getHeight();
        this.setBounds(width / 6, height / 6, width * 2 / 3, height * 2 / 3);
        this.setTitle("Chat");
        this.setResizable(false);

        data = new JTextArea();
        data.setEditable(false);
        helperArea = new JTextArea("enter message");
        helperArea.setEditable(false);
        nicNameArea = new JTextArea("enter nickname:");
        nicNameArea.setEditable(false);
        inputField = new JTextField("", 12);
        headerPanel = new JPanel();

        headerPanel.add(helperArea);
        headerPanel.add(inputField);
        headerPanel.add(nicNameArea);

        jScrollPane = new JScrollPane(data);

        this.add(headerPanel, BorderLayout.NORTH);
        this.add(jScrollPane, BorderLayout.CENTER);

        this.setVisible(true);
    }

    public void setNickname(String nickname){
        nicNameArea.setText(nickname);
    }

    public String getMessage(){
        return inputField.getText();
    }

    public void addMessage(String message){
        data.append(message+"\n");
    }
}
