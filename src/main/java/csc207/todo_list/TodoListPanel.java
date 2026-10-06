package csc207.todo_list;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TodoListPanel extends JPanel implements ActionListener {
    public static final String DONE = " (done)";
    public static final String SAVE_DIR = "saves";
    public static final String SAVEFILE_TODO_LIST_JSON =
        SAVE_DIR + File.separator + "todo_list.json";

    private final JTextField textField;
    private final DefaultListModel<String> textModel;
    private final TodoList todoList;
    private final JButton updateSelected;
    private final JLabel editStatus;

    public TodoListPanel() {
        this.setLayout(new BorderLayout(8, 8));
        this.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        todoList = new TodoList();

        textField = new JTextField(20);
        textField.addActionListener(this);

        textModel = new DefaultListModel<>();

        loadJsonFromFile();
        updateTodoModel();

        JList<String> textList = new JList<>(textModel);
        JScrollPane scrollPane = new JScrollPane(textList);
        scrollPane.setPreferredSize(new Dimension(500, 240));

        updateSelected = new JButton("Update Selected");
        updateSelected.setEnabled(false);
        updateSelected.addActionListener(e -> updateSelectedItem(textList));
        editStatus = new JLabel(" ");

        ListSelectionModel listSelectionModel = textList.getSelectionModel();
        listSelectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listSelectionModel.addListSelectionListener(
            e -> {
                if (!e.getValueIsAdjusting()) {
                    selectItem(textList);
                }
            }
        );

        textList.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent evt) {
                if (evt.getKeyCode() == KeyEvent.VK_DELETE
                    || evt.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                    deleteItem(textList);
                } else if (evt.getKeyCode() == KeyEvent.VK_SPACE) {
                    toggleDone(textList);
                }
            }
        });

        JButton save = new JButton("Save");
        save.addActionListener(e -> save());

        JPanel buttons = new JPanel();
        buttons.add(updateSelected);
        buttons.add(save);

        // Only the central list grows vertically when the window is enlarged.
        JPanel input = new JPanel(new BorderLayout(0, 4));
        input.add(new JLabel("Task title (Enter adds; Update Selected edits):"),
                BorderLayout.NORTH);
        input.add(textField, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.add(buttons, BorderLayout.NORTH);
        footer.add(editStatus, BorderLayout.SOUTH);

        add(input, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
    }

    private void loadJsonFromFile() {
        ensureJsonExists();

        JSONArray jsonArray = readJsonFile();

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);

            String title = jsonObject.getString("task");
            boolean completed = jsonObject.getBoolean("completed");

            todoList.addItem(title, completed);
        }
    }

    private static void ensureJsonExists() {
        Path saveDirectory = Paths.get(SAVE_DIR);

        if (!Files.exists(saveDirectory)) {
            try {
                Files.createDirectories(saveDirectory);
            } catch (IOException e) {
                throw new RuntimeException(
                    "Failed to create save file directory", e);
            }
        }

        Path saveFile = Paths.get(SAVEFILE_TODO_LIST_JSON);

        if (!Files.exists(saveFile)) {
            try {
                Files.createFile(saveFile);
                Files.write(saveFile, "[]".getBytes());
            } catch (IOException e) {
                throw new RuntimeException(
                    "Failed to create todo_list.json file", e);
            }
        }
    }

    private JSONArray readJsonFile() {
        String jsonString;

        try {
            jsonString = Files.readString(
                Paths.get(SAVEFILE_TODO_LIST_JSON));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new JSONArray(jsonString);
    }

    private void save() {
        JSONArray jsonArray = new JSONArray();

        for (int i = 0; i < todoList.getSize(); i++) {
            JSONObject jsonObject = new JSONObject();

            jsonObject.put("task", todoList.getTitle(i));
            jsonObject.put("completed", todoList.isCompleted(i));

            jsonArray.put(jsonObject);
        }

        try {
            FileWriter fileWriter =
                new FileWriter(SAVEFILE_TODO_LIST_JSON);

            fileWriter.write(jsonArray.toString());
            fileWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void toggleDone(JList<String> textList) {
        int selectedIndex = textList.getSelectedIndex();

        if (selectedIndex != -1) {
            todoList.toggleCompleted(selectedIndex);

            updateTodoModel();
            textList.setSelectedIndex(selectedIndex);
        }
    }

    private void deleteItem(JList<String> textList) {
        int selectedIndex = textList.getSelectedIndex();

        if (selectedIndex != -1) {
            todoList.removeItem(selectedIndex);
            updateTodoModel();
        }
    }

    private void selectItem(JList<String> textList) {
        int selectedIndex = textList.getSelectedIndex();
        updateSelected.setEnabled(selectedIndex != -1);
        editStatus.setText(" ");

        if (selectedIndex != -1) {
            textField.setText(todoList.getTitle(selectedIndex));
        }
    }

    private void updateSelectedItem(JList<String> textList) {
        int selectedIndex = textList.getSelectedIndex();

        if (selectedIndex == -1) {
            return;
        }

        try {
            todoList.updateTitle(selectedIndex, textField.getText());
        } catch (IllegalArgumentException e) {
            editStatus.setText(e.getMessage());
            textField.requestFocusInWindow();
            return;
        }

        updateTodoModel();
        textList.setSelectedIndex(selectedIndex);
        editStatus.setText("Title updated. Click Save to keep it.");
        textField.requestFocusInWindow();
        textField.selectAll();
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        String title = textField.getText();

        todoList.addItem(title);

        updateTodoModel();
        textField.selectAll();
    }

    private void updateTodoModel() {
        textModel.clear();

        for (int i = 0; i < todoList.getSize(); i++) {
            String displayText = todoList.getTitle(i);

            if (todoList.isCompleted(i)) {
                displayText += DONE;
            }

            textModel.addElement(displayText);
        }
    }
}
