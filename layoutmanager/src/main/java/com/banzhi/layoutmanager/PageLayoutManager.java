package com.banzhi.layoutmanager;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.MotionEvent;
import android.view.View;

/**
 * <pre>
 * @author : No.1
 * @time : 2018/7/16.
 * @desciption :
 * @version :
 * </pre>
 */

public class PageLayoutManager extends RecyclerView.LayoutManager {

    //行数
    int rows;
    //列数
    int columns;
    //每页的显示item个数
    int onePageSize;
    //页数
    int page;

    //保存所有的Item的上下左右的偏移量信息
    private SparseArray<Rect> allItemFrames = new SparseArray<>();
    //记录Item是否出现过屏幕且还没有回收。true表示出现过屏幕上，并且还没被回收
    private SparseBooleanArray hasAttachedItems = new SparseBooleanArray();

    //item的平均宽度
    int itemWidth;
    //item的平均高度
    int itemHeight;

    int totalHeight = 0;
    int totalWidth = 0;

    public PageLayoutManager(int rows, int columns) {
        this.rows = rows;
        this.columns = columns;
        this.onePageSize = rows * columns;
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(RecyclerView.LayoutParams.WRAP_CONTENT, RecyclerView.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public RecyclerView.LayoutParams generateLayoutParams(Context c, AttributeSet attrs) {
        Log.i(TAG, "generateLayoutParams: " + System.currentTimeMillis());
        return super.generateLayoutParams(c, attrs);
    }

    private int getVerticalSpace() {
        return getHeight() - getPaddingBottom() - getPaddingTop();
    }

    private int getHorizontalSpace() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    int itemWidthUsed;
    int itemHeightUsed;

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (getItemCount() == 0) {
            return;
        }
        if (state.isPreLayout()) {
            return;
        }
        //获取每个Item的平均宽高
        itemWidth = getHorizontalSpace() / columns;
        itemHeight = getVerticalSpace() / rows;
        computePage(state);
        //计算可以横向滚动的最大值
        totalWidth = (page - 1) * getWidth();
        //计算宽高已经使用的量，主要用于后期测量
        itemWidthUsed = (columns - 1) * itemWidth;
        itemHeightUsed = (rows - 1) * itemHeight;
        //在布局之前，将所有的子View先Detach掉，放入到Scrap缓存中
        detachAndScrapAttachedViews(recycler);

        int itemCount = getItemCount();
        for (int i = 0; i < page; i++) {//按页数绘制 绘制的总数量=page*rows*columns ,多余数量未空白占位
            for (int j = 0; j < rows; j++) {//先绘制行
                for (int k = 0; k < columns; k++) {//绘制每列的item
                    int index = i * onePageSize + j * columns + k;
                    if (index >= itemCount) {
                        break;
                    }
                    //这里就是从缓存里面取出
                    View view = recycler.getViewForPosition(index);
                    //将View加入到RecyclerView中
                    addView(view);
                    //对子View进行测量
                    measureChildWithMargins(view, itemWidthUsed, itemHeightUsed);

                    int width = getDecoratedMeasuredWidth(view);
                    int height = getDecoratedMeasuredHeight(view);

                    //记录显示范围
                    Rect rect = allItemFrames.get(index);
                    if (rect == null) {
                        rect = new Rect();
                    }
                    int x = i * getHorizontalSpace() + k * width;
                    int y = j * height;
                    rect.set(x, y, x + width, y + height);
                    allItemFrames.put(index, rect);
                }
            }
            //每一页循环以后就回收一页的View用于下一页的使用
            removeAndRecycleAllViews(recycler);
        }
        recycleAndFillViews(recycler, state);
    }

    /**
     * 计算页数
     *
     * @param state
     */
    private void computePage(RecyclerView.State state) {
        int itemCount = state.getItemCount();
        page = itemCount / onePageSize + (itemCount % onePageSize == 0 ? 0 : 1);
    }

    int offsetX = 0;

    /**
     * 回收和填充视图
     *
     * @param recycler
     * @param state
     */
    private void recycleAndFillViews(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (state.isPreLayout()) {
            return;
        }

        Rect displayRect = new Rect(getPaddingLeft() + offsetX, getPaddingTop(), getWidth() - getPaddingLeft() - getPaddingRight() + offsetX, getHeight() - getPaddingTop() - getPaddingBottom());
        Rect childRect = new Rect();
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            childRect.left = getDecoratedLeft(child);
            childRect.top = getDecoratedTop(child);
            childRect.right = getDecoratedRight(child);
            childRect.bottom = getDecoratedBottom(child);
            if (!Rect.intersects(displayRect, childRect)) {
                removeAndRecycleView(child, recycler);
            }
        }

        for (int i = 0; i < getItemCount(); i++) {
            if (Rect.intersects(displayRect, allItemFrames.get(i))) {

                View view = recycler.getViewForPosition(i);
                addView(view);
                measureChildWithMargins(view, itemWidthUsed, itemHeightUsed);

                Rect rect = allItemFrames.get(i);
                //将这个item布局出来
                layoutDecorated(view,
                        rect.left - offsetX,
                        rect.top,
                        rect.right - offsetX,
                        rect.bottom);
            }
        }

    }

    @Override
    public boolean canScrollHorizontally() {
        return true;
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {

        detachAndScrapAttachedViews(recycler);
        int newX = offsetX + dx;
        int result = dx;
        if (newX > totalWidth) {
            result = totalWidth - offsetX;
        } else if (newX < 0) {
            result = 0 - offsetX;
        }
        Log.e(TAG, "scrollHorizontallyBy: " + result);
        offsetX += result;
        offsetChildrenHorizontal(-result);
        recycleAndFillViews(recycler, state);

        return result;
    }

    public static final String TAG = "tag";
    MyOnFlingListener mOnFlingListener;

    @Override
    public void onAttachedToWindow(RecyclerView view) {
        super.onAttachedToWindow(view);
        mOnFlingListener = new MyOnFlingListener(view);
        view.setOnFlingListener(mOnFlingListener);
        //设置滚动监听，记录滚动的状态，和总的偏移量
        view.addOnScrollListener(new MyScrollListener());
        //记录滚动开始的位置
        view.setOnTouchListener(mOnTouchListener);
    }

    @Override
    public void onDetachedFromWindow(RecyclerView view, RecyclerView.Recycler recycler) {
        super.onDetachedFromWindow(view, recycler);
        offsetX = 0;
    }

    ValueAnimator mAnimator;
    int startX;
    int startY;
    int scrollOffsetX = 0;

    class MyOnFlingListener extends RecyclerView.OnFlingListener {
        RecyclerView mRecyclerView;

        public MyOnFlingListener(RecyclerView recyclerView) {
            this.mRecyclerView = recyclerView;
        }

        @Override
        public boolean onFling(int velocityX, int velocityY) {
            Log.i(TAG, "onFling: velocityX=" + velocityX + "  velocityY=" + velocityY);
            int startPoint;
            int endPoint;
            int page = startX / getWidth();
            if (velocityX < 0) {//上页
                page--;
            } else if (velocityX > 0) {//下页
                page++;
            } else {
                return false;
            }
            startPoint = scrollOffsetX;
            endPoint = page * getWidth();
            endPoint = endPoint < 0 ? 0 : endPoint;
            //使用动画处理滚动
            if (mAnimator == null) {
                mAnimator = new ValueAnimator().ofInt(startPoint, endPoint);

                mAnimator.setDuration(200);
                mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        int nowPoint = (int) animation.getAnimatedValue();
                        int dx = nowPoint - scrollOffsetX;
                        mRecyclerView.scrollBy(dx, 0);

                    }
                });
                mAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
//                        //回调监听
                        if (null != onPageSelectListener) {
                            onPageSelectListener.onPageSelectListener(getPageIndex());
                        }
                        mRecyclerView.stopScroll();
                        startX = scrollOffsetX;
                    }
                });

            } else {
                mAnimator.cancel();
                mAnimator.setIntValues(startPoint, endPoint);
            }

            mAnimator.start();

            return true;
        }
    }

    /**
     * 获取当前页
     *
     * @return
     */
    public int getPageIndex() {
        return scrollOffsetX / getWidth();
    }

    public void scrollToPage(int pageIndex) {
        pageIndex = pageIndex > page ? page : pageIndex;
        if (mAnimator == null) {
            mOnFlingListener.onFling(0, 0);
        }
        if (mAnimator != null) {
            int startPoint = scrollOffsetX;
            int endPoint = getWidth() * pageIndex;
            if (startPoint != endPoint) {
                mAnimator.setIntValues(startPoint, endPoint);
                mAnimator.start();
            }
        }
    }

    /**
     * 处理回滚
     */
    class MyScrollListener extends OnScrollListener {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {//滚动停止
                int vx = 0;
                int absX = Math.abs(scrollOffsetX - startX);
                if (absX > getWidth() / 5) {
                    vx = scrollOffsetX - startX < 0 ? -1000 : 1000;
                }
                mOnFlingListener.onFling(vx, 0);
            }
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            scrollOffsetX += dx;
        }
    }

    View.OnTouchListener mOnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            //手指按下的时候记录开始滚动的坐标
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                startX = scrollOffsetX;
            }
            return false;
        }
    };


    OnPageSelectListener onPageSelectListener;

    public void setOnPageSelectListener(OnPageSelectListener onPageSelectListener) {
        this.onPageSelectListener = onPageSelectListener;
    }

    public interface OnPageSelectListener {
        void onPageSelectListener(int page);
    }

}
