package csc207.todo_list;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TodoListTest {
    @Test
    void editingCompletedItemPreservesStateAndPosition() {
        TodoList list = new TodoList();
        list.addItem("First");
        list.addItem("Old title", true);
        list.addItem("Last");

        list.updateTitle(1, "  New title  ");

        assertEquals(3, list.getSize());
        assertEquals("First", list.getTitle(0));
        assertEquals("New title", list.getTitle(1));
        assertEquals("Last", list.getTitle(2));
        assertTrue(list.isCompleted(1));
    }

    @Test
    void duplicateTitlesAreEditedByPosition() {
        TodoList list = new TodoList();
        list.addItem("Same");
        list.addItem("Same");

        list.updateTitle(1, "Different");

        assertEquals("Same", list.getTitle(0));
        assertEquals("Different", list.getTitle(1));
        assertFalse(list.isCompleted(1));
    }

    @Test
    void invalidTitlesLeaveTheOriginalItemUnchanged() {
        TodoItem item = new TodoItem("Original", true);
        for (String invalid : new String[]{null, "", " \t\n ", "\u2003"}) {
            assertThrows(IllegalArgumentException.class, () -> item.setTitle(invalid));
            assertEquals("Original", item.getTitle());
            assertTrue(item.isCompleted());
        }
    }

    @Test
    void doneTextInTitleIsNotCompletionState() {
        TodoList list = new TodoList();
        list.addItem("Old");
        list.updateTitle(0, "Read about (done)");
        assertEquals("Read about (done)", list.getTitle(0));
        assertFalse(list.isCompleted(0));
        list.toggleCompleted(0);
        assertEquals("Read about (done)", list.getTitle(0));
        assertTrue(list.isCompleted(0));
    }

    @Test
    void invalidIndexDoesNotEditAnotherItem() {
        TodoList list = new TodoList();
        list.addItem("Original");
        assertThrows(IndexOutOfBoundsException.class, () -> list.updateTitle(-1, "Changed"));
        assertThrows(IndexOutOfBoundsException.class, () -> list.updateTitle(1, "Changed"));
        assertEquals("Original", list.getTitle(0));
        assertEquals(1, list.getSize());
    }
}
