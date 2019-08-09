package com.wujf.stickyheaderfooter.headerfooterutil;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.nio.file.attribute.PosixFileAttributes;

public class StickyRecyclerTouchListener implements RecyclerView.OnItemTouchListener {
    private final GestureDetector mTapDetector;
    private final RecyclerView mRecyclerView;
    private final StickyRecyclerDecoration mDecor;
    private OnHeaderClickListener mOnHeaderClickListener;

    public interface OnHeaderClickListener {
        void onHeaderClick(View header, int position, long headerId);
    }

    public StickyRecyclerTouchListener(final RecyclerView recyclerView,
                                       final StickyRecyclerDecoration decor) {
        mTapDetector = new GestureDetector(recyclerView.getContext(), new SingleTapDetector());
        mRecyclerView = recyclerView;
        mDecor = decor;
    }

    public StickyRecyclerAdapter getAdapter() {
        if (mRecyclerView.getAdapter() instanceof StickyRecyclerHeadersAdapter) {
            return (StickyRecyclerAdapter) mRecyclerView.getAdapter();
        } else {
            throw new IllegalStateException("A RecyclerView with " +
                    StickyRecyclerTouchListener.class.getSimpleName() +
                    " requires a " + StickyRecyclerHeadersAdapter.class.getSimpleName());
        }
    }


    public void setOnHeaderClickListener(OnHeaderClickListener listener) {
        mOnHeaderClickListener = listener;
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView view, MotionEvent e) {
        boolean tapDetectorResponse = this.mTapDetector.onTouchEvent(e);
        if (tapDetectorResponse) {
            // Don't return false if a single tap is detected
            return true;
        }
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            int position = mDecor.findHeaderPositionUnder((int) e.getX(), (int) e.getY());
            return position != -1;
        }
        return false;
    }

    @Override
    public void onTouchEvent(RecyclerView view, MotionEvent e) { /* do nothing? */ }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        // do nothing
    }

    private class SingleTapDetector extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            int position = mDecor.findHeaderPositionUnder((int) e.getX(), (int) e.getY());
            if (position != -1) {
                View headerView = mDecor.getHeaderView(mRecyclerView, position);
                performClick(headerView, e ,position);
                return true;
            }
            position = mDecor.findFooterPositionUnder((int) e.getX(), (int) e.getY());
            if (position != -1) {
                View footerView = mDecor.getFooterView(mRecyclerView, position);
                performClick(footerView, e,position);
                return true;
            }
            return false;
        }

        private void performClick(View view, MotionEvent e,int position) {
            if (view instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) view;
                for (int i = 0; i < viewGroup.getChildCount(); i++) {
                    View child = viewGroup.getChildAt(i);
                    performClick(child, e ,position);
                }
            }

            containsBounds(view, e ,position);
        }

        private View containsBounds(View view, MotionEvent e ,int position) {
            int x = (int) e.getX();
            int y = (int) e.getY();
            Rect rect = new Rect();
            view.getHitRect(rect);
            if (view.getVisibility() == View.VISIBLE
                    && view.dispatchTouchEvent(e)
                    && rect.left < rect.right && rect.top < rect.bottom && x >= rect.left && x < rect.right && y >= rect.top) {
                view.setTag(position);
                view.performClick();
                return view;
            }
            return null;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            return true;
        }
    }

}
