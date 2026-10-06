package csc207.todo_list;

import java.util.ArrayList;
import java.util.List;

public class TodoList {
  private final List<TodoItem> items;

  public TodoList() {
    items = new ArrayList<>();
  }

  public void addItem(String title) {
    items.add(new TodoItem(title));
  }

  public void addItem(String title, boolean completed) {
    items.add(new TodoItem(title, completed));
  }

  public void removeItem(int index) {
    items.remove(index);
  }

  /** Edits the existing item, preserving its position and completion state. */
  public void updateTitle(int index, String title) {
    items.get(index).setTitle(title);
  }

  public void toggleCompleted(int index) {
    items.get(index).toggleCompleted();
  }

  public String getTitle(int index) {
    return items.get(index).getTitle();
  }

  public boolean isCompleted(int index) {
    return items.get(index).isCompleted();
  }

  public int getSize() {
    return items.size();
  }
}
