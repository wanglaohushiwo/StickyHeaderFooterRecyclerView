package com.wujf.stickyheaderfooter.headerfooterutil.caching;

import android.support.v4.util.LongSparseArray;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.wujf.stickyheaderfooter.headerfooterutil.StickyRecyclerFootersAdapter;
import com.wujf.stickyheaderfooter.headerfooterutil.util.OrientationProvider;


/**
 * An implementation of {@link HeaderProvider} that creates and caches header views
 */
public class FooterViewCache implements FooterProvider {

    private final StickyRecyclerFootersAdapter mAdapter;
    private final LongSparseArray<RecyclerView.ViewHolder> mFooterViews = new LongSparseArray<>();
    private final OrientationProvider mOrientationProvider;

    public FooterViewCache(StickyRecyclerFootersAdapter adapter,
                           OrientationProvider orientationProvider) {
        mAdapter = adapter;
        mOrientationProvider = orientationProvider;
    }

    @Override
    public View getFooter(RecyclerView parent, int position) {
        long headerId = mAdapter.getFooterId(position);
        RecyclerView.ViewHolder viewHolder = getFooterViewHolder(position);
        if (viewHolder == null) {
            //TODO - recycle views
            viewHolder = mAdapter.onCreateFooterViewHolder(parent);
            mAdapter.onBindFooterViewHolder(viewHolder, position);
            View footer = viewHolder.itemView;
            if (footer.getLayoutParams() == null) {
                footer.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }

            int widthSpec;
            int heightSpec;

            if (mOrientationProvider.getOrientation(parent) == LinearLayoutManager.VERTICAL) {
                widthSpec = View.MeasureSpec.makeMeasureSpec(parent.getWidth(), View.MeasureSpec.EXACTLY);
                heightSpec = View.MeasureSpec.makeMeasureSpec(parent.getHeight(), View.MeasureSpec.UNSPECIFIED);
            } else {
                widthSpec = View.MeasureSpec.makeMeasureSpec(parent.getWidth(), View.MeasureSpec.UNSPECIFIED);
                heightSpec = View.MeasureSpec.makeMeasureSpec(parent.getHeight(), View.MeasureSpec.EXACTLY);
            }

            int childWidth = ViewGroup.getChildMeasureSpec(widthSpec,
                    parent.getPaddingLeft() + parent.getPaddingRight(), footer.getLayoutParams().width);
            int childHeight = ViewGroup.getChildMeasureSpec(heightSpec,
                    parent.getPaddingTop() + parent.getPaddingBottom(), footer.getLayoutParams().height);
            footer.measure(childWidth, childHeight);
            footer.layout(0, 0, footer.getMeasuredWidth(), footer.getMeasuredHeight());
            mFooterViews.put(headerId, viewHolder);
            footer.setTag(headerId);
        }
        return viewHolder.itemView;
    }

    public RecyclerView.ViewHolder getFooterViewHolder(int position) {
        long footerId = mAdapter.getFooterId(position);
        return mFooterViews.get(footerId);
    }

    @Override
    public void invalidate() {
        mFooterViews.clear();
    }
}
