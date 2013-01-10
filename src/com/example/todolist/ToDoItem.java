package com.example.todolist;

public class ToDoItem {
    private final String mTask;
    private final Integer mItem;

    public String getTask() {
        return mTask;
    }

    public ToDoItem(String task, Integer item) {
        mTask = task;
        mItem = item;
    }
    
    public Integer getItem() {
    	return mItem;
    	
    }

    @Override
    public String toString() {
        return mTask;
    }
}