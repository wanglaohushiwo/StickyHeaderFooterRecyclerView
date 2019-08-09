package com.wujf.stickyheaderfooter.headerfooterutil;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;

import com.wujf.stickyheaderfooter.headerfooterutil.caching.FooterProvider;
import com.wujf.stickyheaderfooter.headerfooterutil.caching.FooterViewCache;
import com.wujf.stickyheaderfooter.headerfooterutil.caching.HeaderProvider;
import com.wujf.stickyheaderfooter.headerfooterutil.caching.HeaderViewCache;
import com.wujf.stickyheaderfooter.headerfooterutil.calculation.DimensionCalculator;
import com.wujf.stickyheaderfooter.headerfooterutil.rendering.HeaderRenderer;
import com.wujf.stickyheaderfooter.headerfooterutil.util.LinearLayoutOrientationProvider;
import com.wujf.stickyheaderfooter.headerfooterutil.util.OrientationProvider;

public class StickyRecyclerDecoration extends RecyclerView.ItemDecoration {

    private final StickyRecyclerAdapter mAdapter;
    private final ItemVisibilityAdapter mVisibilityAdapter;
    private final SparseArray<Rect> mHeaderRects = new SparseArray<>();
    private final SparseArray<Rect> mFooterRects = new SparseArray<>();
    private final HeaderProvider mHeaderProvider;
    private final FooterProvider mFooterProvider;
    private final OrientationProvider mOrientationProvider;
    private final HeaderPositionCalculator mHeaderPositionCalculator;
    private final HeaderRenderer mRenderer;
    private final DimensionCalculator mDimensionCalculator;

    /**
     * The following field is used as a buffer for internal calculations. Its sole purpose is to avoid
     * allocating new Rect every time we need one.
     */
    private final Rect mTempRect = new Rect();

    // TODO: Consider passing in orientation to simplify orientation accounting within calculation
    public StickyRecyclerDecoration(StickyRecyclerAdapter adapter) {
        this(adapter, new LinearLayoutOrientationProvider(), new DimensionCalculator(), null);
    }

    public StickyRecyclerDecoration(StickyRecyclerAdapter adapter, ItemVisibilityAdapter visibilityAdapter) {
        this(adapter, new LinearLayoutOrientationProvider(), new DimensionCalculator(), visibilityAdapter);
    }

    private StickyRecyclerDecoration(StickyRecyclerAdapter adapter, OrientationProvider orientationProvider,
                                     DimensionCalculator dimensionCalculator, ItemVisibilityAdapter visibilityAdapter) {
        this(adapter, orientationProvider, dimensionCalculator, new HeaderRenderer(orientationProvider),
                new HeaderViewCache(adapter, orientationProvider), new FooterViewCache(adapter, orientationProvider), visibilityAdapter);
    }

    private StickyRecyclerDecoration(StickyRecyclerAdapter adapter, OrientationProvider orientationProvider,
                                     DimensionCalculator dimensionCalculator, HeaderRenderer headerRenderer, HeaderProvider headerProvider, FooterProvider footerProvider, ItemVisibilityAdapter visibilityAdapter) {
        this(adapter, headerRenderer, orientationProvider, dimensionCalculator, headerProvider, footerProvider,
                new HeaderPositionCalculator(adapter, headerProvider, footerProvider, orientationProvider,
                        dimensionCalculator), visibilityAdapter);
    }

    private StickyRecyclerDecoration(StickyRecyclerAdapter adapter, HeaderRenderer headerRenderer,
                                     OrientationProvider orientationProvider, DimensionCalculator dimensionCalculator, HeaderProvider headerProvider, FooterProvider footerProvider,
                                     HeaderPositionCalculator headerPositionCalculator, ItemVisibilityAdapter visibilityAdapter) {
        mAdapter = adapter;
        mHeaderProvider = headerProvider;
        mFooterProvider = footerProvider;
        mOrientationProvider = orientationProvider;
        mRenderer = headerRenderer;
        mDimensionCalculator = dimensionCalculator;
        mHeaderPositionCalculator = headerPositionCalculator;
        mVisibilityAdapter = visibilityAdapter;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        int itemPosition = parent.getChildAdapterPosition(view);
        if (itemPosition == RecyclerView.NO_POSITION) {
            return;
        }
        if (mHeaderPositionCalculator.hasNewHeader(itemPosition, mOrientationProvider.isReverseLayout(parent))) {
            View header = getHeaderView(parent, itemPosition);
            setItemOffsetsForHeader(outRect, header, mOrientationProvider.getOrientation(parent));
        }
        if (mHeaderPositionCalculator.hasNewFooter(itemPosition, mOrientationProvider.isReverseLayout(parent))) {
            View footer = getFooterView(parent, itemPosition);
            setItemOffsetsForFooter(outRect, footer, mOrientationProvider.getOrientation(parent));
        }
    }

    /**
     * Sets the offsets for the first item in a section to make room for the header view
     *
     * @param itemOffsets rectangle to define offsets for the item
     * @param header      view used to calculate offset for the item
     * @param orientation used to calculate offset for the item
     */
    private void setItemOffsetsForHeader(Rect itemOffsets, View header, int orientation) {
        mDimensionCalculator.initMargins(mTempRect, header);
        if (orientation == LinearLayoutManager.VERTICAL) {
            itemOffsets.top = itemOffsets.top + header.getHeight() + mTempRect.top + mTempRect.bottom;
        } else {
            itemOffsets.left = itemOffsets.left + header.getWidth() + mTempRect.left + mTempRect.right;
        }
    }

    private void setItemOffsetsForFooter(Rect itemOffsets, View header, int orientation) {
        mDimensionCalculator.initMargins(mTempRect, header);
        if (orientation == LinearLayoutManager.VERTICAL) {
            itemOffsets.bottom = header.getHeight() + mTempRect.top + mTempRect.bottom;
        } else {
            itemOffsets.left = header.getWidth() + mTempRect.left + mTempRect.right;
        }
    }

    @Override
    public void onDrawOver(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
        super.onDrawOver(canvas, parent, state);

        final int childCount = parent.getChildCount();
        if (childCount <= 0 || mAdapter.getItemCount() <= 0) {
            return;
        }

        for (int i = 0; i < childCount; i++) {
            View itemView = parent.getChildAt(i);
            int position = parent.getChildAdapterPosition(itemView);
            if (position == RecyclerView.NO_POSITION) {
                continue;
            }

            boolean hasStickyHeader = mHeaderPositionCalculator.hasStickyHeader(itemView, mOrientationProvider.getOrientation(parent), position);
            if (hasStickyHeader || mHeaderPositionCalculator.hasNewHeader(position, mOrientationProvider.isReverseLayout(parent))) {
                View header = mHeaderProvider.getHeader(parent, position);
                //re-use existing Rect, if any.
                Rect headerOffset = mHeaderRects.get(position);
                if (headerOffset == null) {
                    headerOffset = new Rect();
                    mHeaderRects.put(position, headerOffset);
                }
                mHeaderPositionCalculator.initHeaderBounds(headerOffset, parent, header, itemView, hasStickyHeader);
                mRenderer.drawHeader(parent, canvas, header, headerOffset);
            }
            boolean hasStickyFooter = mHeaderPositionCalculator.hasStickyFooter(parent, itemView, mOrientationProvider.getOrientation(parent), position);
            if (hasStickyFooter || mHeaderPositionCalculator.hasNewFooter(position, mOrientationProvider.isReverseLayout(parent))) {
                View footer = mFooterProvider.getFooter(parent, position);
                Rect footerOffset = mFooterRects.get(position);
                if (footerOffset == null) {
                    footerOffset = new Rect();
                    mFooterRects.put(position, footerOffset);
                }
                mHeaderPositionCalculator.initFooterBounds(footerOffset, parent, footer, itemView, hasStickyFooter);
                mRenderer.drawHeader(parent, canvas, footer, footerOffset);
            }
        }
    }

    /**
     * Gets the position of the header under the specified (x, y) coordinates.
     *
     * @param x x-coordinate
     * @param y y-coordinate
     * @return position of header, or -1 if not found
     */
    public int findHeaderPositionUnder(int x, int y) {
        int tempPosition = -1; // Added
        for (int i = 0; i < mHeaderRects.size(); i++) {
            Rect rect = mHeaderRects.get(mHeaderRects.keyAt(i));
            if (rect.contains(x, y)) {
                int position = mHeaderRects.keyAt(i);
                if (mVisibilityAdapter == null || mVisibilityAdapter.isPositionVisible(position)) {
                    tempPosition = position; // Added
                } else {
                    if (tempPosition != -1) {
                        break;//my Added
                    }
                }
            }
        }
        return tempPosition; // Added
    }

    /**
     * Gets the position of the footer under the specified (x, y) coordinates.
     *
     * @param x x-coordinate
     * @param y y-coordinate
     * @return position of header, or -1 if not found
     */
    public int findFooterPositionUnder(int x, int y) {
        int tempPosition = -1; // Added
        for (int i = 0; i < mFooterRects.size(); i++) {
            Rect rect = mFooterRects.get(mFooterRects.keyAt(i));
            if (rect.contains(x, y)) {
                int position = mFooterRects.keyAt(i);
                if (mVisibilityAdapter == null || mVisibilityAdapter.isPositionVisible(position)) {
                    tempPosition = position; // Added
                } else {
                    if (tempPosition != -1) {
                        break;//my Added
                    }
                }
            }
        }
        return tempPosition; // Added
    }

    /**
     * Gets the header view for the associated position.  If it doesn't exist yet, it will be
     * created, measured, and laid out.
     *
     * @param parent   the recyclerview
     * @param position the position to get the header view for
     * @return Header view
     */
    public View getHeaderView(RecyclerView parent, int position) {
        return mHeaderProvider.getHeader(parent, position);
    }

    public View getFooterView(RecyclerView parent, int position) {
        return mFooterProvider.getFooter(parent, position);
    }

    public RecyclerView.ViewHolder getHeaderViewHolder(int position) {
        return mHeaderProvider.getHeaderViewHolder(position);
    }

    public RecyclerView.ViewHolder getFooterViewHolder(int position) {
        return mFooterProvider.getFooterViewHolder(position);
    }

    /**
     * Invalidates cached headers.  This does not invalidate the recyclerview, you should do that manually after
     * calling this method.
     */
    public void invalidateHeaders() {
        mHeaderProvider.invalidate();
        mHeaderRects.clear();
    }

    public void invalidateFooters() {
        mFooterProvider.invalidate();
        mFooterRects.clear();
    }
}
