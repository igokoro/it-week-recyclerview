package com.epam.itweek.basicrecyclerview;

import android.support.annotation.LayoutRes;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class ColorsAdapter extends RecyclerView.Adapter<ColorsAdapter.ColorViewHolder> {

    private List<Integer> colors;
    private int layout;

    public ColorsAdapter(List<Integer> colors, @LayoutRes int layout) {
        this.colors = colors;
        this.layout = layout;
    }

    @Override
    public ColorViewHolder onCreateViewHolder(ViewGroup parent, int position) {
        final View v = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        ColorViewHolder colorViewHolder = new ColorViewHolder(v);
        v.setBackground(new RoundRectDrawable(colorViewHolder.color.getRadius()));
        return colorViewHolder;
    }

    @Override
    public void onBindViewHolder(ColorViewHolder viewHolder, int position) {
        Integer color = colors.get(position);
        setItemBackgroundColor(viewHolder, color);
        setColorText(viewHolder, color);
    }

    private void setColorText(ColorViewHolder viewHolder, Integer color) {
        viewHolder.colorName.setText(viewHolder.itemView.getContext().getString(R.string.colorPresentation, color & 0xFFFFFF));
    }

    private void setItemBackgroundColor(ColorViewHolder viewHolder, Integer color) {
        ((RoundRectDrawable)(viewHolder.color.getBackground())).setColor(color);
    }

    @Override
    public int getItemCount() {
        return colors.size();
    }

    static class ColorViewHolder extends RecyclerView.ViewHolder {
        @InjectView(R.id.color) CardView color;
        @InjectView(R.id.colorName) TextView colorName;

        public ColorViewHolder(View itemView) {
            super(itemView);
            ButterKnife.inject(this, itemView);
        }
    }
}
