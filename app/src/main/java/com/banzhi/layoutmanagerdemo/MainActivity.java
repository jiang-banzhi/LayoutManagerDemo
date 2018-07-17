package com.banzhi.layoutmanagerdemo;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.banzhi.layoutmanager.PageLayoutManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RecyclerView recyclerView = findViewById(R.id.recyvlerView);
//        recyclerView.setLayoutManager(new TestLayoutManager());
        PageLayoutManager layout = new PageLayoutManager(1, 5);
        recyclerView.setLayoutManager(layout);
        layout.setOnPageSelectListener(new PageLayoutManager.OnPageSelectListener() {
            @Override
            public void onPageSelectListener(int page) {
                Log.i("result", "onPageSelectListener: " + page);
            }
        });
        recyclerView.setAdapter(new TestAdapter());
    }

    static class TestAdapter extends RecyclerView.Adapter<TestViewHolder> {

        @Override
        public TestViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            View view = LayoutInflater.from(context).inflate(R.layout.item_sample, parent, false);
            return new TestViewHolder(view);
        }

        @Override
        public void onBindViewHolder(TestViewHolder holder, final int position) {
            holder.setText("第" + position + "项");
            holder.textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.e("result", "onClick:  点击 " + position);
                }
            });
        }

        @Override
        public int getItemCount() {
            return 100;
        }
    }

    static class TestViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public TestViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.textview);
        }

        public void setText(String text) {
            textView.setText(text);
        }
    }
}
