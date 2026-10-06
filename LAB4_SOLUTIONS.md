# CSC207 Lab 4 — Completed Work

This file records the completed analysis, design work, scenario walk-throughs, comparison, implementation decisions, and reflection for **Lab 4: From Specifications to Design**.

The starter code is preserved on `main`. The instructor's supplied entity design is preserved on `entity-refactor`. The completed work is on `lab4-complete`.

---

## Part 1 — What Can the Program Do?

### User-story support in the original `main` branch

| # | User story | Support | Evidence / observation |
|---|---|---|---|
| 1 | Add a todo item | Yes | Typing a task in the text field and pressing Enter appends it to the list. |
| 2 | View the todo list | Yes | Items are displayed in the Swing `JList`. |
| 3 | Mark an item as done | Yes | Select an item and press Space. |
| 4 | Unmark an item as done | Yes | Pressing Space again removes the done marker. |
| 5 | Remove an item | Yes | Select an item and press Delete/Backspace. |
| 6 | Edit an item | Partially | Selecting an item copies its text into the text field, but pressing Enter creates a new item instead of updating the selected one. |
| 7 | Filter completed/incomplete items | No | There is no filtering behaviour. |
| 8 | Sort by priority or due date | No | Priority and due date are not represented in the original model. |
| 9 | Save the todo list | Yes | The Save button writes the list to `saves/todo_list.json`. |
| 10 | Reopen the app and see saved items | Yes | The constructor loads the JSON file when the panel is created. |

### What happens when an existing task is selected?

The program copies the selected task text into the input field. This makes the interface look as though editing may be possible, but the existing Enter action still adds a new item instead of changing the selected one.

### Tracing “mark a todo item as done” in the original design

The relevant method is `toggleDone` in `TodoListPanel`.

1. The GUI gets the selected list index.
2. It reads the selected value from `DefaultListModel<String>`.
3. If the string already ends with `" (done)"`, that suffix is removed.
4. Otherwise, `" (done)"` is appended.
5. The modified string replaces the previous string in the list model.

#### What Java type represents a todo item?

A todo item is represented as a **`String`**, stored in a `DefaultListModel<String>`.

#### How is completion status represented?

Completion is encoded directly into the task's display string using the suffix:

```text
 (done)
```

For example:

```text
Study CSC207
Study CSC207 (done)
```

#### How does saving determine whether an item is completed?

The save logic checks:

```java
item.endsWith(DONE)
```

It then stores the result as the JSON `completed` boolean and strips the display suffix from the saved task title.

#### Evaluation of the original representation

This representation works, but it mixes **domain state** with **presentation text**. Whether a task is completed is part of the task itself, not part of how the GUI chooses to display the task. A better design uses a todo-item object with separate fields such as `title` and `completed`.

---

## Part 2 — From Specification to Design

### Noun–verb analysis

The important nouns in the specification are:

- **todo list** — candidate entity class: `TodoList`
- **task / todo item** — candidate entity class: `TodoItem`
- **title** — attribute of `TodoItem`
- **description** — attribute of `TodoItem`
- **due date** — attribute of `TodoItem`
- **priority level** — attribute of `TodoItem`; the priority values could be represented by an enum
- **completion state** — boolean attribute of `TodoItem`
- **persistent storage** — important system responsibility, but not necessarily a domain entity; it would be better separated into a persistence/data-access component in a larger design

The important verb phrases and the responsibilities they suggest are:

| Verb / behaviour | Suggested responsibility |
|---|---|
| create/add a task | `TodoList.addItem` |
| edit a task | `TodoItem` changes its editable state, with `TodoList` locating the item |
| delete a task | `TodoList.removeItem` |
| mark a task completed | `TodoItem.markCompleted` / `toggleCompleted` |
| mark a task incomplete | `TodoItem.markIncomplete` / `toggleCompleted` |
| filter tasks | `TodoList` |
| sort tasks | `TodoList` |
| save/load tasks | persistence component rather than the entity itself |

### Proposed entity design

The proposed design is stored in:

`plantuml/StudentTodoListDesign.plantuml`

The central relationship is:

```text
TodoList 1 ---- * TodoItem
```

`TodoList` owns the collection. Each `TodoItem` owns its own task-specific state.

A `TodoItem` has:

- `title: String`
- `description: String`
- `dueDate: LocalDate`
- `priority: Priority`
- `completed: boolean`

A `TodoList` has:

- `items: List<TodoItem>`

This design keeps task state independent of Swing or any other user interface.

---

## Part 3 — Scenario Walk-Through

### Scenario

> A user marks an incomplete todo item as completed.

### Walk-through

1. The UI determines which task the user selected.
2. The UI asks the `TodoList` to change the completion state of that item.
3. `TodoList` locates the corresponding `TodoItem`.
4. `TodoList` delegates the state change to that `TodoItem`.
5. `TodoItem` changes `completed` from `false` to `true`.
6. The UI reads the updated state and refreshes the displayed list.

### Design check

The first draft must therefore provide:

- a relationship from `TodoList` to its `TodoItem` objects;
- a way for `TodoList` to locate a specific item;
- a completion-changing responsibility on `TodoItem`;
- a way to read the updated state.

If `TodoItem` had only a `completed` field but no responsibility such as `markCompleted` or `toggleCompleted`, the scenario would reveal a design gap. The method must be added before the scenario can be completed using only the UML.

### Design decision to explain to the TA

**Completion state belongs to `TodoItem`, not to the GUI.**

The GUI may display completed tasks differently, but the task's completion status should exist even if the application later replaces Swing with another interface. Keeping the state in the entity separates domain information from presentation.

---

## Part 4 — Compare with the Supplied `entity-refactor` Design

The instructor's branch introduces two entity classes:

- `TodoItem`
- `TodoList`

### Similarity

Both the proposed design and the supplied design separate an individual task from the list that contains tasks. They also place completion state on the todo-item entity rather than encoding it in the display string.

### Difference

The proposed design follows the full written specification, so `TodoItem` includes title, description, due date, priority, and completion state.

The supplied lab design intentionally implements a smaller subset:

- `title`
- `completed`

The supplied design is enough to demonstrate the entity refactor without implementing every future feature in the specification.

### Where the GUI interacts with the entities

In the supplied branch, `TodoListPanel` owns:

```java
private final TodoList todoList;
```

Instead of changing the stored display string directly, the GUI calls methods on `TodoList`, such as:

```java
todoList.addItem(title);
todoList.removeItem(selectedIndex);
todoList.toggleCompleted(selectedIndex);
```

The panel then rebuilds the Swing list model from entity data. The dependency is therefore:

```text
TodoListPanel -> TodoList -> TodoItem
```

This is cleaner than using Swing's `DefaultListModel<String>` as the application's domain model.

---

## Part 5 — Edit a Todo Item

### Scenario walk-through before implementation

Chosen interaction for this implementation:

1. The user selects an existing item.
2. The existing selection behaviour copies its title into the text field.
3. The user changes the title in the text field.
4. The user clicks **Update Selected**.
5. `TodoListPanel` asks `TodoList` to update the selected item.
6. `TodoList` locates the `TodoItem`.
7. `TodoItem` updates its title.
8. The panel refreshes the Swing list while preserving the item's completed state.

### Design gap discovered

The instructor entity design originally contains:

```java
private final String title;
```

and provides no title-changing method.

Therefore, the edit scenario cannot be completed using the original entity design. The entity model needs a new responsibility before the GUI can implement the story cleanly.

### Minimal design changes

The implementation on `lab4-complete` makes the smallest required changes:

- `TodoItem.title` becomes mutable.
- `TodoItem` receives `setTitle(String title)`.
- `TodoList` receives `updateTitle(int index, String title)`.
- `TodoListPanel` receives an **Update Selected** button that delegates the change through `TodoList`.

The completed flag is not recreated or inferred from text, so editing a completed task preserves its completion state.

### Why this responsibility placement?

The GUI should not reach inside `TodoItem` directly.

`TodoListPanel` knows which list position the user selected. `TodoList` owns the collection and can find that item. `TodoItem` owns its own title and changes it. This keeps each responsibility close to the state it controls.

---

## Stop and Reflect

### 1. Did the scenario walk-through reveal a needed class-design change?

Yes. The original supplied `TodoItem` makes `title` final and exposes no edit operation, so the edit user story cannot be completed without changing the entity design.

### 2. Decisions not specified by the user story

The short user story does not answer questions such as:

- What user action confirms an edit?
- Can a task be edited to an empty title?
- What happens if the user changes the text and selects another task before confirming the edit?
- Should editing affect completion status?
- Should editing support only the title or all task details?

For the implemented version, **Update Selected** is used as the explicit confirmation action, and changing the title does not change completion status.

### 3. Which decisions describe what the user should observe?

Examples of user-observable behavioural decisions:

- whether editing is confirmed with Enter, a button, or automatically;
- what happens with an empty title;
- what happens to unsaved text when selection changes;
- whether the item remains selected after editing.

### 4. Which decisions are internal design/implementation decisions?

Examples:

- making `TodoItem.title` mutable;
- placing `setTitle` on `TodoItem`;
- placing `updateTitle` on `TodoList`;
- having the Swing panel delegate rather than mutate entity internals.

### Two behavioural questions that should be answered before confidently implementing the feature

1. **What exact user action should commit an edit: pressing Enter, clicking an Update button, or changing focus?**
2. **What should happen if the user tries to save an empty or blank task title?**

These questions affect externally visible behaviour and therefore should be clarified as requirements rather than silently treated as Java implementation details.

---

## Final structure

- `main` — untouched original starter program.
- `entity-refactor` — untouched instructor-provided entity solution.
- `lab4-complete` — completed written analysis, student UML design, and working edit feature.
