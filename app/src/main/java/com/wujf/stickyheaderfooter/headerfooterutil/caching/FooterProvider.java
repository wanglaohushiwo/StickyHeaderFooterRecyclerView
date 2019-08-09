package com.wujf.stickyheaderfooter.headerfooterutil.caching;

import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Implemented by objects that provide header views for decoration
 */
public interface FooterProvider {

    /**
     * Will provide a header view for a given position in the RecyclerView
     *
     * @param recyclerView that will display the header
     * @param position     that will be headed by the header
     * @return a header view for the given position and list
     */
    public View getFooter(RecyclerView recyclerView, int position);

    public RecyclerView.ViewHolder getFooterViewHolder(int position);

    /**
     * TODO: describe this functionality and its necessity
     */
    void invalidate();
}
