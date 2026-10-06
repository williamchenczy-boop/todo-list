package csc207.todo_list;

public class TodoItem {
  private String title;
  private boolean completed;

  public TodoItem(String title) {
    this(title, false);
  }

  public TodoItem(String title, boolean completed) {
    this.title = title;
    this.completed = completed;
  }

  public String getTitle() {
    return title;
  }

  /** Updates the title without changing the item's completion state. */
  public void setTitle(String title) {
    if (title == null || title.isBlank()) {
      throw new IllegalArgumentException("A task title must not be blank.");
    }
    this.title = title.strip();
  }

  public boolean isCompleted() {
    return completed;
  }

  public void toggleCompleted() {
    completed = !completed;
  }
}
