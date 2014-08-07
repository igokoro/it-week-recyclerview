package com.epam.itweek.basicrecyclerview;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class LinearLayoutManagerActivity extends Activity {

    @InjectView(android.R.id.list) RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_linear_layout_manager);
        ButterKnife.inject(this);
        setupRecyclerView();
    }

    private void setupRecyclerView() {
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(new ColorsAdapter(buildColors(), R.layout.list_item));
    }

    private List<Integer> buildColors() {
        ArrayList<Integer> colors = new ArrayList<Integer>();
        colors.add(0xFFe51c23);
        colors.add(0xFFe91e63);
        colors.add(0xFF9c27b0);
        colors.add(0xFF673ab7);
        colors.add(0xFF3f51b5);
        colors.add(0xFF5677fc);
        colors.add(0xFF03a9f4);
        colors.add(0xFF00bcd4);
        colors.add(0xFF009688);
        colors.add(0xFF259b24);
        colors.add(0xFF8bc34a);
        colors.add(0xFF8bc34a);
        colors.add(0xFFcddc39);
        return colors;
    }

}
