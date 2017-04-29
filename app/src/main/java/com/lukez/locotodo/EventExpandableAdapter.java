package com.lukez.locotodo;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bignerdranch.expandablerecyclerview.ExpandableRecyclerAdapter;
import com.lukez.locotodo_db.LocoTodoEvent;

import java.util.ArrayList;

/**
 * Created by lukez_000 on 04/28/2017.
 */

public class EventExpandableAdapter extends ExpandableRecyclerAdapter<LocoTodoEvent, LocoTodoEvent, EventParentViewHolder, EventChildViewHolder> {
  LayoutInflater mInflater;

  public EventExpandableAdapter(Context context, ArrayList parentEvents){
    super(parentEvents);


    mInflater = LayoutInflater.from(context);
  }

  @Override
  public EventParentViewHolder onCreateParentViewHolder(ViewGroup parentViewGroup, int viewType) {
    View parentView = mInflater.inflate(R.layout.item_event_parent, parentViewGroup, false);
    return new EventParentViewHolder(parentView);
  }

  @Override
  public EventChildViewHolder onCreateChildViewHolder(@NonNull ViewGroup childViewGroup, int viewType) {
    View childView = mInflater.inflate(R.layout.item_event_child, childViewGroup, false);
    return new EventChildViewHolder(childView);
  }

  @Override
  public void onBindParentViewHolder(@NonNull EventParentViewHolder parentViewHolder, int parentPosition, @NonNull LocoTodoEvent parent) {
    parentViewHolder.bind(parent);
  }

  @Override
  public void onBindChildViewHolder(@NonNull EventChildViewHolder childViewHolder, int parentPosition, int childPosition, @NonNull LocoTodoEvent child) {
    childViewHolder.bind(child);
  }

}
