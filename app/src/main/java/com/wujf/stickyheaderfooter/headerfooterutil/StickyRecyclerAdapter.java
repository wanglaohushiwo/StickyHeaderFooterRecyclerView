package com.wujf.stickyheaderfooter.headerfooterutil;

import android.support.v7.widget.RecyclerView;

public interface StickyRecyclerAdapter<HEADERHOLDER extends RecyclerView.ViewHolder, FOOTERHOLDER extends RecyclerView.ViewHolder> extends StickyRecyclerHeadersAdapter<HEADERHOLDER>, StickyRecyclerFootersAdapter<FOOTERHOLDER> {
    int getItemCount();
}
