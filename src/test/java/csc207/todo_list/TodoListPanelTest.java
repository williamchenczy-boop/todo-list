package csc207.todo_list;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class TodoListPanelTest {
    private final Path saveFile = Path.of(TodoListPanel.SAVEFILE_TODO_LIST_JSON);
    private byte[] originalSave;
    private boolean directoryExisted;

    @BeforeEach
    void prepareIsolatedSave() throws IOException {
        // Maven runs in target/. Also preserve data if run directly from an IDE.
        directoryExisted = Files.exists(saveFile.getParent());
        originalSave = Files.exists(saveFile) ? Files.readAllBytes(saveFile) : null;
        Files.createDirectories(saveFile.getParent());
        Files.writeString(saveFile, "[]");
    }

    @AfterEach
    void restoreSave() throws IOException {
        if (originalSave == null) {
            Files.deleteIfExists(saveFile);
        } else {
            Files.write(saveFile, originalSave);
        }
        if (!directoryExisted) {
            Files.deleteIfExists(saveFile.getParent());
        }
    }

    @Test
    void resizingKeepsInputOneLineAndGivesExtraHeightToTheList() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            TodoListPanel panel = new TodoListPanel();
            JTextField field = find(panel, JTextField.class);
            JScrollPane scroll = find(panel, JScrollPane.class);
            panel.setSize(520, 360);
            layoutTree(panel);
            assertEquals(field.getPreferredSize().height, field.getHeight());
            int initialListHeight = scroll.getHeight();

            panel.setSize(1200, 800);
            layoutTree(panel);
            assertEquals(field.getPreferredSize().height, field.getHeight());
            assertEquals(initialListHeight + 440, scroll.getHeight());
        });
    }

    private static void layoutTree(Container parent) {
        parent.doLayout();
        for (Component child : parent.getComponents()) {
            if (child instanceof Container) {
                layoutTree((Container) child);
            }
        }
    }

    @Test
    void completedEditSurvivesSaveAndReload() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            TodoListPanel panel = new TodoListPanel();
            JTextField field = find(panel, JTextField.class);
            JList<?> list = find(panel, JList.class);
            JButton update = button(panel, "Update Selected");
            assertFalse(update.isEnabled());

            field.setText("Original");
            field.postActionEvent();
            list.setSelectedIndex(0);
            pressKey(list, KeyEvent.VK_SPACE);
            field.setText("  Revised (done)  ");
            update.doClick();

            assertEquals(1, list.getModel().getSize());
            assertEquals("Revised (done) (done)", list.getModel().getElementAt(0));
            assertEquals(0, list.getSelectedIndex());
            assertEquals("Revised (done)", field.getText());
            button(panel, "Save").doClick();

            TodoListPanel reopened = new TodoListPanel();
            JList<?> restored = find(reopened, JList.class);
            assertEquals(1, restored.getModel().getSize());
            assertEquals("Revised (done) (done)", restored.getModel().getElementAt(0));
            restored.setSelectedIndex(0);
            pressKey(restored, KeyEvent.VK_SPACE);
            assertEquals("Revised (done)", restored.getModel().getElementAt(0));
        });
    }

    @Test
    void blankEditCanBeCorrectedAndEnterStillAdds() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            TodoListPanel panel = new TodoListPanel();
            JTextField field = find(panel, JTextField.class);
            JList<?> list = find(panel, JList.class);
            JButton update = button(panel, "Update Selected");
            field.setText("Keep me");
            field.postActionEvent();
            list.setSelectedIndex(0);
            field.setText(" \t ");
            update.doClick();
            assertEquals("Keep me", list.getModel().getElementAt(0));
            assertEquals(0, list.getSelectedIndex());
            assertTrue(hasLabel(panel, "A task title must not be blank."));

            field.setText("Corrected");
            update.doClick();
            assertEquals("Corrected", list.getModel().getElementAt(0));
            field.setText("Second");
            field.postActionEvent();
            assertEquals(2, list.getModel().getSize());
            assertEquals("Corrected", list.getModel().getElementAt(0));
            assertEquals("Second", list.getModel().getElementAt(1));
            assertFalse(update.isEnabled());
        });
    }

    @Test
    void changingSelectionDiscardsDraftAndDeletionClearsEditTarget() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            TodoListPanel panel = new TodoListPanel();
            JTextField field = find(panel, JTextField.class);
            JList<?> list = find(panel, JList.class);
            field.setText("First");
            field.postActionEvent();
            field.setText("Second");
            field.postActionEvent();
            list.setSelectedIndex(0);
            field.setText("Unconfirmed draft");
            list.setSelectedIndex(1);
            assertEquals("Second", field.getText());
            assertEquals("First", list.getModel().getElementAt(0));
            pressKey(list, KeyEvent.VK_DELETE);
            assertEquals(1, list.getModel().getSize());
            assertFalse(button(panel, "Update Selected").isEnabled());
            list.setSelectedIndex(0);
            field.setText("Remaining");
            button(panel, "Update Selected").doClick();
            assertEquals("Remaining", list.getModel().getElementAt(0));
        });
    }

    private static void pressKey(JList<?> list, int key) {
        KeyEvent event = new KeyEvent(list, KeyEvent.KEY_PRESSED,
                System.currentTimeMillis(), 0, key, KeyEvent.CHAR_UNDEFINED);
        for (KeyListener listener : list.getKeyListeners()) {
            listener.keyPressed(event);
        }
    }

    private static <T extends Component> T find(Container parent, Class<T> type) {
        for (Component child : parent.getComponents()) {
            if (type.isInstance(child)) {
                return type.cast(child);
            }
            if (child instanceof Container) {
                T found = find((Container) child, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static JButton button(Container parent, String text) {
        for (Component child : parent.getComponents()) {
            if (child instanceof JButton && text.equals(((JButton) child).getText())) {
                return (JButton) child;
            }
            if (child instanceof Container) {
                JButton found = button((Container) child, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static boolean hasLabel(Container parent, String text) {
        for (Component child : parent.getComponents()) {
            if (child instanceof JLabel && text.equals(((JLabel) child).getText())) {
                return true;
            }
            if (child instanceof Container && hasLabel((Container) child, text)) {
                return true;
            }
        }
        return false;
    }
}
