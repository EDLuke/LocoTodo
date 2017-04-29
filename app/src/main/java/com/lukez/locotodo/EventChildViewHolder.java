package com.lukez.locotodo;

import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.bignerdranch.expandablerecyclerview.ChildViewHolder;
import com.lukez.locotodo_db.LocoTodoEvent;

/**
 * Created by lukez_000 on 04/28/2017.
 */

public class EventChildViewHolder extends ChildViewHolder {
  protected TextView mTxtLocation;
  protected ImageButton mImbEdit;
  protected ImageButton mImbNav;

  public EventChildViewHolder(View itemView){
    super(itemView);

    mTxtLocation = (TextView) itemView.findViewById(R.id.txtLocation);
    mImbEdit = (ImageButton) itemView.findViewById(R.id.imbEdit);
    mImbNav = (ImageButton) itemView.findViewById(R.id.imbNav);
  }

  public void bind(LocoTodoEvent event){
    mTxtLocation.setText(event.getLocation());
  }
}
