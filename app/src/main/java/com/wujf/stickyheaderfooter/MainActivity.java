package com.wujf.stickyheaderfooter;

import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.wujf.stickyheaderfooter.R;
import com.wujf.stickyheaderfooter.headerfooterutil.StickyRecyclerAdapter;
import com.wujf.stickyheaderfooter.headerfooterutil.StickyRecyclerDecoration;
import com.wujf.stickyheaderfooter.headerfooterutil.StickyRecyclerTouchListener;

import java.security.SecureRandom;

public class MainActivity extends AppCompatActivity {
    private String[] mDatas;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        // Set adapter populated with example dummy data
        final AnimalsHeaderFooterAdapter adapter = new AnimalsHeaderFooterAdapter();
        mDatas = getDummyDataSet();
        recyclerView.setAdapter(adapter);
        // Set layout manager
        int orientation = getLayoutManagerOrientation(getResources().getConfiguration().orientation);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(this, orientation, false);
        recyclerView.setLayoutManager(layoutManager);

        // Add the sticky headers decoration
        final StickyRecyclerDecoration decor = new StickyRecyclerDecoration(adapter);
        recyclerView.addItemDecoration(decor);
        // Add other decoration for dividers between list items view cause errors;
        // Add touch listeners
        StickyRecyclerTouchListener touchListener =
                new StickyRecyclerTouchListener(recyclerView, decor);
        recyclerView.addOnItemTouchListener(touchListener);
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                decor.invalidateHeaders();
                decor.invalidateFooters();
            }
        });
    }

    private String[] getDummyDataSet() {
        return getResources().getStringArray(R.array.animals);
    }

    private int getLayoutManagerOrientation(int activityOrientation) {
        if (activityOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            return LinearLayoutManager.VERTICAL;
        } else {
            return LinearLayoutManager.HORIZONTAL;
        }
    }

    private class AnimalsHeaderFooterAdapter extends RecyclerView.Adapter<ItemViewHolder>
            implements StickyRecyclerAdapter<HeaderViewHolder, FooterViewHolder> {
        @NonNull
        @Override
        public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.view_item, parent, false);
            return new ItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
            holder.tvTitle.setText(position+"---->"+mDatas[position]);

        }

        @Override
        public int getItemCount() {
            return mDatas.length;
        }

        @Override
        public long getFooterId(int position) {
            return mDatas[position].charAt(0);
        }

        @Override
        public FooterViewHolder onCreateFooterViewHolder(ViewGroup parent) {
            View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.view_footer, parent, false);
            return new FooterViewHolder(view);
        }

        @Override
        public void onBindFooterViewHolder(FooterViewHolder holder, final int position) {
            holder.tvTitle.setText("footer "+String.valueOf(mDatas[position].charAt(0)));
            holder.tvTitle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final int itemPostion = (int)v.getTag();
                    Toast.makeText(MainActivity.this,"footer textview below item "+itemPostion+" be clicked",Toast.LENGTH_SHORT).show();
                }
            });

        }

        @Override
        public FooterViewHolder getFooterViewHolder(int position) {
            RecyclerView.ItemDecoration itemDecoration = recyclerView.getItemDecorationAt(0);
            if (itemDecoration instanceof StickyRecyclerDecoration) {
                return (FooterViewHolder) ((StickyRecyclerDecoration) itemDecoration).getFooterViewHolder(position);
            }
            return null;
        }

        @Override
        public long getHeaderId(int position) {
            return mDatas[position].charAt(0);
        }

        @Override
        public HeaderViewHolder onCreateHeaderViewHolder(ViewGroup parent) {
            View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.view_header, parent, false);
            return new HeaderViewHolder(view);
        }

        @Override
        public void onBindHeaderViewHolder(HeaderViewHolder holder, final int position) {
            holder.tvTitle.setText("header "+String.valueOf(mDatas[position].charAt(0)));
            holder.tvTitle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final int itemPosition = (int)v.getTag();
                    Toast.makeText(MainActivity.this,"header textview above item "+itemPosition+" be clicked",Toast.LENGTH_SHORT).show();

                }
            });
        }

        @Override
        public HeaderViewHolder getHeaderViewHolder(int position) {
            RecyclerView.ItemDecoration itemDecoration = recyclerView.getItemDecorationAt(0);
            if (itemDecoration instanceof StickyRecyclerDecoration) {
                return (HeaderViewHolder) ((StickyRecyclerDecoration) itemDecoration).getHeaderViewHolder(position);
            }
            return null;
        }
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private TextView tvTitle;

        public HeaderViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
        }
    }

    public static class FooterViewHolder extends RecyclerView.ViewHolder {
        private TextView tvTitle;

        public FooterViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
        }
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;

        public ItemViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
        }
    }
}
