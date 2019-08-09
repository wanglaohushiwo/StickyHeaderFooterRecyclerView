package com.wujf.stickyheaderfooter.headerfooterutil;

import android.graphics.Rect;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.wujf.stickyheaderfooter.headerfooterutil.caching.FooterProvider;
import com.wujf.stickyheaderfooter.headerfooterutil.caching.HeaderProvider;
import com.wujf.stickyheaderfooter.headerfooterutil.calculation.DimensionCalculator;
import com.wujf.stickyheaderfooter.headerfooterutil.util.OrientationProvider;


/**
 * Calculates the position and location of header views
 */
public class HeaderPositionCalculator {

    private final StickyRecyclerAdapter mAdapter;
    private final OrientationProvider mOrientationProvider;
    private final HeaderProvider mHeaderProvider;
    private final DimensionCalculator mDimensionCalculator;
    private FooterProvider mFooterProvider;

    /**
     * The following fields are used as buffers for internal calculations. Their sole purpose is to avoid
     * allocating new Rect every time we need one.
     */
    private final Rect mTempRect1 = new Rect();
    private final Rect mTempRect2 = new Rect();

    public HeaderPositionCalculator(StickyRecyclerAdapter adapter, HeaderProvider headerProvider, FooterProvider footerProvider,
                                    OrientationProvider orientationProvider, DimensionCalculator dimensionCalculator) {
        mAdapter = adapter;
        mHeaderProvider = headerProvider;
        mFooterProvider = footerProvider;
        mOrientationProvider = orientationProvider;
        mDimensionCalculator = dimensionCalculator;
    }

    /**
     * Determines if a view should have a sticky header.
     * The view has a sticky header if:
     * 1. It is the first element in the recycler view
     * 2. It has a valid ID associated to its position
     *
     * @param itemView    given by the RecyclerView
     * @param orientation of the Recyclerview
     * @param position    of the list item in question
     * @return True if the view should have a sticky header
     */
    public boolean hasStickyHeader(View itemView, int orientation, int position) {
        int offset, margin;
        mDimensionCalculator.initMargins(mTempRect1, itemView);
        if (orientation == LinearLayout.VERTICAL) {
            offset = itemView.getTop();
            margin = mTempRect1.top;
        } else {
            offset = itemView.getLeft();
            margin = mTempRect1.left;
        }

        return offset >= -itemView.getHeight() - mTempRect1.bottom && offset <= margin && mAdapter.getHeaderId(position) >= 0;
        //return offset <= margin&& mAdapter.getHeaderId(position) >= 0;
    }

    public boolean hasStickyFooter(RecyclerView parent, View itemView, int orientation, int position) {
        int offset = 0, margin = 0;
        mDimensionCalculator.initMargins(mTempRect1, itemView);
        if (orientation == LinearLayout.VERTICAL) {
            offset = itemView.getBottom();
            margin = mTempRect1.bottom;
        } else {

        }
        return parent.getHeight() - margin <= offset && mAdapter.getHeaderId(position) >= 0;
    }


    /**
     * Determines if an item in the list should have a header that is different than the item in the
     * list that immediately precedes it. Items with no headers will always return false.
     *
     * @param position        of the list item in questions
     * @param isReverseLayout TRUE if layout manager has flag isReverseLayout
     * @return true if this item has a different header than the previous item in the list
     */
    public boolean hasNewHeader(int position, boolean isReverseLayout) {
        if (indexOutOfBounds(position)) {
            return false;
        }

        long headerId = mAdapter.getHeaderId(position);

        if (headerId < 0) {
            return false;
        }

        long nextItemHeaderId = -1;
        int nextItemPosition = position + (isReverseLayout ? 1 : -1);
        if (!indexOutOfBounds(nextItemPosition)) {
            nextItemHeaderId = mAdapter.getHeaderId(nextItemPosition);
        }
        return headerId != nextItemHeaderId;
    }

    public boolean hasNewFooter(int position, boolean isReverseLayout) {
        if (indexOutOfBounds(position)) {
            return false;
        }

        long headerId = mAdapter.getHeaderId(position);

        if (headerId < 0) {
            return false;
        }

        long nextItemHeaderId = -1;
        int nextItemPosition = position + (isReverseLayout ? -1 : 1);
        if (!indexOutOfBounds(nextItemPosition)) {
            nextItemHeaderId = mAdapter.getHeaderId(nextItemPosition);
        }
        return headerId != nextItemHeaderId;
    }

    private boolean indexOutOfBounds(int position) {
        return position < 0 || position >= mAdapter.getItemCount();
    }

    public void initHeaderBounds(Rect bounds, RecyclerView recyclerView, View header, View firstView, boolean firstHeader) {
        int orientation = mOrientationProvider.getOrientation(recyclerView);
        initDefaultHeaderOffset(bounds, recyclerView, header, firstView, orientation);
        if (firstHeader && isStickyHeaderBeingPushedOffscreen(recyclerView, header)) {
            View lastViewObscuredByHeader = getLastViewObscuredByHeader(recyclerView, header);
            translateHeaderWithLastViewObscureByHeader(recyclerView, mOrientationProvider.getOrientation(recyclerView), bounds,
                    header, lastViewObscuredByHeader);
        }
    }

    public void initFooterBounds(Rect bounds, RecyclerView recyclerView, View footer, View firstView, boolean firstFooter) {
        int orientation = mOrientationProvider.getOrientation(recyclerView);
        initDefaultFooterOffset(bounds, recyclerView, footer, firstView, orientation);
        if (firstFooter && isStickyFooterBeingPushedOffscreen(recyclerView, footer)) {
            View lastViewObscuredByFooter = getLastViewObscuredByFooter(recyclerView, footer);
            translateFooterWithLastViewObscureByHeader(recyclerView, mOrientationProvider.getOrientation(recyclerView), bounds,
                    footer, lastViewObscuredByFooter);
        }
    }

    private void initDefaultHeaderOffset(Rect headerMargins, RecyclerView recyclerView, View header, View firstView, int orientation) {
        int translationX, translationY;
        mDimensionCalculator.initMargins(mTempRect1, header);

        ViewGroup.LayoutParams layoutParams = firstView.getLayoutParams();
        int leftMargin = 0;
        int topMargin = 0;
        if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) layoutParams;
            leftMargin = marginLayoutParams.leftMargin;
            topMargin = marginLayoutParams.topMargin;
        }

        if (orientation == LinearLayoutManager.VERTICAL) {
            translationX = firstView.getLeft() - leftMargin + mTempRect1.left;
            translationY = Math.max(
                    firstView.getTop() - topMargin - header.getHeight() - mTempRect1.bottom,
                    getListTop(recyclerView) + mTempRect1.top);
        } else {
            translationY = firstView.getTop() - topMargin + mTempRect1.top;
            translationX = Math.max(
                    firstView.getLeft() - leftMargin - header.getWidth() - mTempRect1.right,
                    getListLeft(recyclerView) + mTempRect1.left);
        }

        headerMargins.set(translationX, translationY, translationX + header.getWidth(),
                translationY + header.getHeight());
    }

    private void initDefaultFooterOffset(Rect footerMargins, RecyclerView recyclerView, View footer, View firstView, int orientation) {
        int translationX = -1, translationY = -1;
        mDimensionCalculator.initMargins(mTempRect1, footer);
        ViewGroup.LayoutParams layoutParams = firstView.getLayoutParams();
        int leftMargin = 0;
        int bottomMargin = 0;
        if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) layoutParams;
            leftMargin = marginLayoutParams.leftMargin;
            bottomMargin = marginLayoutParams.bottomMargin;
        }

        if (orientation == LinearLayoutManager.VERTICAL) {
            translationX = firstView.getLeft() - leftMargin + mTempRect1.left;
            translationY = Math.min(
                    firstView.getBottom() + bottomMargin + mTempRect1.top + footer.getHeight(),
                    getListBottom(recyclerView) - mTempRect1.bottom);
        } else {
//      translationY = firstView.getTop() - topMargin + mTempRect1.top;
//      translationX = Math.max(
//              firstView.getLeft() - leftMargin - header.getWidth() - mTempRect1.right,
//              getListLeft(recyclerView) + mTempRect1.left);
        }
        footerMargins.set(translationX, translationY - footer.getHeight(), translationX + footer.getWidth(),
                translationY);
    }

    //  private boolean isStickyHeaderBeingPushedOffscreen(RecyclerView recyclerView, View stickyHeader) {
//    View viewAfterHeader = getFirstViewUnobscuredByHeader(recyclerView, stickyHeader);
//    int firstViewUnderHeaderPosition = recyclerView.getChildAdapterPosition(viewAfterHeader);
//    if (firstViewUnderHeaderPosition == RecyclerView.NO_POSITION) {
//        return false;
//    }
//
//    boolean isReverseLayout = mOrientationProvider.isReverseLayout(recyclerView);
//    if (firstViewUnderHeaderPosition > 0 && hasNewHeader(firstViewUnderHeaderPosition, isReverseLayout)) {
//      View nextHeader = mHeaderProvider.getHeader(recyclerView, firstViewUnderHeaderPosition);
//      mDimensionCalculator.initMargins(mTempRect1, nextHeader);
//      mDimensionCalculator.initMargins(mTempRect2, stickyHeader);
//
//      if (mOrientationProvider.getOrientation(recyclerView) == LinearLayoutManager.VERTICAL) {
//        int topOfNextHeader = viewAfterHeader.getTop() - mTempRect1.bottom - nextHeader.getHeight() - mTempRect1.top;
//        int bottomOfThisHeader = recyclerView.getPaddingTop() + stickyHeader.getBottom() + mTempRect2.top + mTempRect2.bottom;
//        if (topOfNextHeader < bottomOfThisHeader) {
//          return true;
//        }
//      } else {
//        int leftOfNextHeader = viewAfterHeader.getLeft() - mTempRect1.right - nextHeader.getWidth() - mTempRect1.left;
//        int rightOfThisHeader = recyclerView.getPaddingLeft() + stickyHeader.getRight() + mTempRect2.left + mTempRect2.right;
//        if (leftOfNextHeader < rightOfThisHeader) {
//          return true;
//        }
//      }
//    }
//
//
//    return false;
//  }
    private boolean isStickyHeaderBeingPushedOffscreen(RecyclerView recyclerView, View stickyHeader) {
        View viewAfterHeader = getLastViewObscuredByHeader(recyclerView, stickyHeader);
        int lastViewUnderHeaderPosition = recyclerView.getChildAdapterPosition(viewAfterHeader);
        if (lastViewUnderHeaderPosition == RecyclerView.NO_POSITION) {
            return false;
        }
        boolean isReverseLayout = mOrientationProvider.isReverseLayout(recyclerView);
        boolean hasNewFooter = hasNewFooter(lastViewUnderHeaderPosition, isReverseLayout);
        if (lastViewUnderHeaderPosition > 0 && hasNewFooter) {
            View nextFooter = mFooterProvider.getFooter(recyclerView, lastViewUnderHeaderPosition);
            mDimensionCalculator.initMargins(mTempRect1, nextFooter);
            mDimensionCalculator.initMargins(mTempRect2, stickyHeader);

            if (mOrientationProvider.getOrientation(recyclerView) == LinearLayoutManager.VERTICAL) {
                int topOfNextFooter = viewAfterHeader.getBottom() + mTempRect1.top;
                int bottomOfThisHeader = recyclerView.getPaddingTop() + stickyHeader.getBottom() + mTempRect2.top + mTempRect2.bottom;
                if (topOfNextFooter < bottomOfThisHeader) {
                    return true;
//        }
                }
            } else {
//        int leftOfNextHeader = viewAfterHeader.getLeft() - mTempRect1.right - nextHeader.getWidth() - mTempRect1.left;
//        int rightOfThisHeader = recyclerView.getPaddingLeft() + stickyHeader.getRight() + mTempRect2.left + mTempRect2.right;
//        if (leftOfNextHeader < rightOfThisHeader) {
//          return true;
//        }
            }
        }
        return false;
    }

    private boolean isStickyFooterBeingPushedOffscreen(RecyclerView recyclerView, View stickyFooter) {
        View lastViewObscuredByFooter = getLastViewObscuredByFooter(recyclerView, stickyFooter);
        int lastViewObscuredByFooterPostion = recyclerView.getChildAdapterPosition(lastViewObscuredByFooter);
        if (lastViewObscuredByFooterPostion == RecyclerView.NO_POSITION) {
            return false;
        }
        boolean isReverseLayout = mOrientationProvider.isReverseLayout(recyclerView);
        boolean hasNewHeader = hasNewHeader(lastViewObscuredByFooterPostion, isReverseLayout);
        if (lastViewObscuredByFooterPostion > 0 && hasNewHeader) {
            View nextHeader = mHeaderProvider.getHeader(recyclerView, lastViewObscuredByFooterPostion);
            mDimensionCalculator.initMargins(mTempRect1, nextHeader);
            mDimensionCalculator.initMargins(mTempRect2, stickyFooter);
            ViewGroup.LayoutParams layoutParams = lastViewObscuredByFooter.getLayoutParams();
            int leftMargin = 0;
            int topMargin = 0;
            if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) layoutParams;
                leftMargin = marginLayoutParams.leftMargin;
                topMargin = marginLayoutParams.topMargin;
            }
            if (mOrientationProvider.getOrientation(recyclerView) == LinearLayoutManager.VERTICAL) {
                int bottomOfNextHeader = lastViewObscuredByFooter.getTop() - topMargin;
                int footerHeight = stickyFooter.getHeight();
                int topOfThisFooter = getListBottom(recyclerView) - footerHeight - mTempRect2.bottom - mTempRect2.top;
                if (topOfThisFooter < bottomOfNextHeader) {
                    return true;
//        }
                }
            } else {
//        int leftOfNextHeader = viewAfterHeader.getLeft() - mTempRect1.right - nextHeader.getWidth() - mTempRect1.left;
//        int rightOfThisHeader = recyclerView.getPaddingLeft() + stickyHeader.getRight() + mTempRect2.left + mTempRect2.right;
//        if (leftOfNextHeader < rightOfThisHeader) {
//          return true;
//        }
            }
        }


        return false;
    }

    private void translateHeaderWithNextHeader(RecyclerView recyclerView, int orientation, Rect translation,
                                               View currentHeader, View viewAfterNextHeader, View nextHeader) {
        mDimensionCalculator.initMargins(mTempRect1, nextHeader);
        mDimensionCalculator.initMargins(mTempRect2, currentHeader);
        if (orientation == LinearLayoutManager.VERTICAL) {
            int topOfStickyHeader = getListTop(recyclerView) + mTempRect2.top + mTempRect2.bottom;
            int shiftFromNextHeader = viewAfterNextHeader.getTop() - nextHeader.getHeight() - mTempRect1.bottom - mTempRect1.top - currentHeader.getHeight() - topOfStickyHeader;
            if (shiftFromNextHeader < topOfStickyHeader) {
                translation.top += shiftFromNextHeader;
            }
        } else {
            int leftOfStickyHeader = getListLeft(recyclerView) + mTempRect2.left + mTempRect2.right;
            int shiftFromNextHeader = viewAfterNextHeader.getLeft() - nextHeader.getWidth() - mTempRect1.right - mTempRect1.left - currentHeader.getWidth() - leftOfStickyHeader;
            if (shiftFromNextHeader < leftOfStickyHeader) {
                translation.left += shiftFromNextHeader;
            }
        }
    }

    private void translateHeaderWithLastViewObscureByHeader(RecyclerView recyclerView, int orientation, Rect translation, View currentHeader, View lastViewUnderHeader) {
        mDimensionCalculator.initMargins(mTempRect2, currentHeader);
        if (orientation == LinearLayoutManager.VERTICAL) {
            int shiftFromlastViewUnderHeader = lastViewUnderHeader.getBottom() + mTempRect2.bottom - currentHeader.getHeight() - getListTop(recyclerView) - mTempRect2.top - mTempRect2.bottom;
            translation.top += shiftFromlastViewUnderHeader;
        } else {

        }
    }

    private void translateFooterWithLastViewObscureByHeader(RecyclerView recyclerView, int orientation, Rect translation, View currentFooter, View lastViewUnderFooter) {
        mDimensionCalculator.initMargins(mTempRect2, currentFooter);
        if (orientation == LinearLayoutManager.VERTICAL) {
            ViewGroup.LayoutParams layoutParams = lastViewUnderFooter.getLayoutParams();
            int leftMargin = 0;
            int topMargin = 0;
            if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) layoutParams;
                leftMargin = marginLayoutParams.leftMargin;
                topMargin = marginLayoutParams.bottomMargin;
            }
//      int shiftFromlastViewUnderFooter = lastViewUnderFooter.getBottom()+mTempRect2.bottom-(getListBottom(recyclerView)- currentFooter.getHeight()-mTempRect2.bottom-mTempRect2.top);
            int shiftFormlastViewUnderFooter = lastViewUnderFooter.getTop() - topMargin - (getListBottom(recyclerView) - currentFooter.getHeight() - mTempRect2.bottom - mTempRect2.top);
            if (shiftFormlastViewUnderFooter > 0) {
                translation.top += shiftFormlastViewUnderFooter;
            }

        } else {

        }
    }


    /**
     * Returns the first item currently in the RecyclerView that is not obscured by a header.
     *
     * @param parent Recyclerview containing all the list items
     * @return first item that is fully beneath a header
     */
    private View getFirstViewUnobscuredByHeader(RecyclerView parent, View firstHeader) {
        boolean isReverseLayout = mOrientationProvider.isReverseLayout(parent);
        int step = isReverseLayout ? -1 : 1;
        int from = isReverseLayout ? parent.getChildCount() - 1 : 0;
        for (int i = from; i >= 0 && i <= parent.getChildCount() - 1; i += step) {
            View child = parent.getChildAt(i);
            if (!itemIsObscuredByHeader(parent, child, firstHeader, mOrientationProvider.getOrientation(parent))) {
                return child;
            }
        }
        return null;
    }

    private View getLastViewObscuredByHeader(RecyclerView parent, View firstHeader) {
        boolean isReverseLayout = mOrientationProvider.isReverseLayout(parent);
        int step = isReverseLayout ? -1 : 1;
        int from = isReverseLayout ? parent.getChildCount() - 1 : 0;
        for (int i = from; i >= 0 && i <= parent.getChildCount() - 1; i += step) {
            View child = parent.getChildAt(i);
            if (!itemIsObscuredByHeader(parent, child, firstHeader, mOrientationProvider.getOrientation(parent))) {
                int lastViewObscuredByHeaderPosition = i - 1;
                if (lastViewObscuredByHeaderPosition >= 0) {
                    return parent.getChildAt(lastViewObscuredByHeaderPosition);
                } else {
                    return null;
                }
            }
        }
        return null;

    }

    private View getLastViewObscuredByFooter(RecyclerView parent, View firstFooter) {
        boolean isReverseLayout = mOrientationProvider.isReverseLayout(parent);
        int step = isReverseLayout ? -1 : +1;
        int from = isReverseLayout ? parent.getChildCount() - 1 : 0;
        for (int i = from; i >= 0 && i <= parent.getChildCount() - 1; i += step) {
            View child = parent.getChildAt(i);
            if (itemIsObscuredByFooter(parent, child, firstFooter, mOrientationProvider.getOrientation(parent))) {
                return child;
            }
        }
        return null;

    }

    /**
     * Determines if an item is obscured by a header
     *
     * @param parent
     * @param item        to determine if obscured by header
     * @param header      that might be obscuring the item
     * @param orientation of the {@link RecyclerView}
     * @return true if the item view is obscured by the header view
     */
    private boolean itemIsObscuredByHeader(RecyclerView parent, View item, View header, int orientation) {
        RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) item.getLayoutParams();
        mDimensionCalculator.initMargins(mTempRect1, header);

        int adapterPosition = parent.getChildAdapterPosition(item);
        if (adapterPosition == RecyclerView.NO_POSITION || mHeaderProvider.getHeader(parent, adapterPosition) != header) {
            // Resolves https://github.com/timehop/sticky-headers-recyclerview/issues/36
            // Handles an edge case where a trailing header is smaller than the current sticky header.
            return false;
        }

        if (orientation == LinearLayoutManager.VERTICAL) {
            int itemTop = item.getTop() - layoutParams.topMargin;
            int headerBottom = getListTop(parent) + header.getBottom() + mTempRect1.bottom + mTempRect1.top;
            if (itemTop >= headerBottom) {
                return false;
            }
        } else {
            int itemLeft = item.getLeft() - layoutParams.leftMargin;
            int headerRight = getListLeft(parent) + header.getRight() + mTempRect1.right + mTempRect1.left;
            if (itemLeft >= headerRight) {
                return false;
            }
        }

        return true;
    }

    private boolean itemIsObscuredByFooter(RecyclerView parent, View item, View footer, int orientation) {
        RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) item.getLayoutParams();
        mDimensionCalculator.initMargins(mTempRect1, footer);

        int adapterPosition = parent.getChildAdapterPosition(item);
        if (adapterPosition == RecyclerView.NO_POSITION || mFooterProvider.getFooter(parent, adapterPosition) != footer) {
            // Resolves https://github.com/timehop/sticky-headers-recyclerview/issues/36
            // Handles an edge case where a trailing header is smaller than the current sticky header.
            if (parent.getChildAdapterPosition(item) == 583) {
            }
            return false;
        }

        if (orientation == LinearLayoutManager.VERTICAL) {
            int itemBottom = item.getBottom() + layoutParams.bottomMargin;
            int footerTop = getListBottom(parent) - footer.getHeight() - mTempRect1.bottom - mTempRect1.top;
            if (itemBottom >= footerTop) {
                return true;
            }
        } else {
//      int itemLeft = item.getLeft() - layoutParams.leftMargin;
//      int headerRight = getListLeft(parent) + header.getRight() + mTempRect1.right + mTempRect1.left;
//      if (itemLeft >= headerRight) {
//        return false;
//      }
        }

        return false;
    }

    private int getListTop(RecyclerView view) {
        if (view.getLayoutManager().getClipToPadding()) {
            return view.getPaddingTop();
        } else {
            return 0;
        }
    }

    private int getListBottom(RecyclerView view) {
        if (view.getLayoutManager().getClipToPadding()) {
            return view.getHeight() - view.getPaddingBottom();
        } else {
            return view.getHeight();
        }
    }

    private int getListLeft(RecyclerView view) {
        if (view.getLayoutManager().getClipToPadding()) {
            return view.getPaddingLeft();
        } else {
            return 0;
        }
    }
}
