package com.hwqgooo.gankio.utils;

import android.app.Activity;
import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.ScrollView;

public class SwipeBackLayout extends ViewGroup {
    private static final String TAG = SwipeBackLayout.class.getSimpleName();
    private static final double AUTO_FINISHED_SPEED_LIMIT = 2000.0;
    private static final float BACK_FACTOR = 0.5f;
    public static final int SENSING_AREA_FULL = -1;

    private int draggingState = 0;

    private int draggingOffset;
    /**
     * Whether allow to pull this layout.
     */
    private boolean enablePullToBack = true;
    private boolean enableFlingBack = true;

    public enum DragEdge {
        TOP, BOTTOM,
        LEFT, RIGHT,
    }

    private DragEdge dragEdge = DragEdge.LEFT;
    /**
     * the anchor of calling finish.
     */
    private float finishAnchor = 0;
    private int horizontalDragRange = 0;
    private int verticalDragRange = 0;
    private boolean inSensingArean;
    private int sensingArean;

    private View scrollChild;
    private View target;
    private SwipeBackListener swipeBackListener;
    private final ViewDragHelper viewDragHelper;

    public SwipeBackLayout(Context context) {
        this(context, null);
    }

    public SwipeBackLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        viewDragHelper = ViewDragHelper.create(this, 1.0f, new ViewDragHelperCallBack());
        sensingArean = (int) (((float) ScreenUtils.getScreenWidth(context)) * 0.15f);
    }

    public void setDragEdge(DragEdge dragEdge) {
        this.dragEdge = dragEdge;
    }

    /**
     * Set the anchor of calling finish.
     *
     * @param offset
     */
    public void setFinishAnchor(float offset) {
        finishAnchor = offset;
    }

    /**
     * Whether allow to finish activity by fling the layout.
     *
     * @param b
     */
    public void setEnableFlingBack(boolean b) {
        enableFlingBack = b;
    }

    @Deprecated
    public void setOnPullToBackListener(SwipeBackListener listener) {
        swipeBackListener = listener;
    }

    public void setOnSwipeBackListener(SwipeBackListener listener) {
        swipeBackListener = listener;
    }

    public void setScrollChild(View view) {
        scrollChild = view;
    }

    public void setEnablePullToBack(boolean b) {
        enablePullToBack = b;
    }

    private void ensureTarget() {
        if (target != null) {
            return;
        }
        if (getChildCount() > 1) {
            throw new IllegalStateException("SwipeBackLayout must contains only one direct child");
        }
        target = getChildAt(0);

        if (scrollChild == null && target != null) {
            if (target instanceof ViewGroup) {
                findScrollView((ViewGroup) target);
            } else {
                scrollChild = target;
            }
        }
    }

    /**
     * Find out the scrollable child view from a ViewGroup.
     *
     * @param viewGroup
     */
    private void findScrollView(ViewGroup viewGroup) {
        scrollChild = viewGroup;
        if (viewGroup.getChildCount() <= 0) {
            return;
        }
        int count = viewGroup.getChildCount();
        View child;
        for (int i = 0; i < count; i++) {
            child = viewGroup.getChildAt(i);
            if (child instanceof AbsListView ||
                    child instanceof ScrollView ||
                    child instanceof ViewPager
                    || child instanceof WebView) {
                scrollChild = child;
                return;
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        if (getChildCount() == 0) {
            return;
        }
        View child = getChildAt(0);

        int childWidth = width - getPaddingLeft() - getPaddingRight();
        int childHeight = height - getPaddingTop() - getPaddingBottom();
        int childLeft = getPaddingLeft();
        int childTop = getPaddingTop();
        child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (getChildCount() > 1) {
            throw new IllegalStateException("SwipeBackLayout must contains only one direct child.");
        }

        if (getChildCount() > 0) {
            int measureWidth = MeasureSpec.makeMeasureSpec(
                    getMeasuredWidth() - getPaddingLeft() - getPaddingRight(),
                    MeasureSpec.EXACTLY);
            int measureHeight = MeasureSpec.makeMeasureSpec(
                    getMeasuredHeight() - getPaddingTop() - getPaddingBottom(),
                    MeasureSpec.EXACTLY);
            getChildAt(0).measure(measureWidth, measureHeight);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        verticalDragRange = h;
        horizontalDragRange = w;

        switch (dragEdge) {
        case TOP:
        case BOTTOM:
            finishAnchor = finishAnchor > 0 ? finishAnchor : verticalDragRange * BACK_FACTOR;
            break;
        case LEFT:
        case RIGHT:
            finishAnchor = finishAnchor > 0 ? finishAnchor : horizontalDragRange * BACK_FACTOR;
            break;
        }
    }

    private int getDragRange() {
        switch (dragEdge) {
        case TOP:
        case BOTTOM:
            return verticalDragRange;
        case LEFT:
        case RIGHT:
            return horizontalDragRange;
        default:
            return verticalDragRange;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        isInSensingArean(ev);
        if (!inSensingArean) {
            return super.onInterceptHoverEvent(ev);
        }
        boolean handled = false;
        ensureTarget();
        if (isEnabled()) {
            try {
                handled = viewDragHelper.shouldInterceptTouchEvent(ev);
            } catch (ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
                handled = false;
            }
        } else {
            this.viewDragHelper.cancel();
        }
        return handled ? handled : super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        isInSensingArean(event);
        if (!inSensingArean) {
            return super.onInterceptHoverEvent(event);
        }
        viewDragHelper.processTouchEvent(event);
        return true;
    }

    void isInSensingArean(MotionEvent ev) {
        boolean z = true;
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            if (sensingArean == SENSING_AREA_FULL) {
                inSensingArean = true;
            }
            float Y = ev.getY();
            float X = ev.getX();
            int left = getLeft();
            int top = getTop();
            int right = getRight();
            int bottom = getBottom();
            switch (dragEdge) {
            case TOP:
                if (Y >= ((float) (sensingArean + top))) {
                    z = false;
                }
                inSensingArean = z;
                break;
            case BOTTOM:
                if (Y <= ((float) (bottom - sensingArean))) {
                    z = false;
                }
                inSensingArean = z;
                break;
            case LEFT:
                if (X >= ((float) (sensingArean + left))) {
                    z = false;
                }
                inSensingArean = z;
                break;
            case RIGHT:
                if (X <= ((float) (right - sensingArean))) {
                    z = false;
                }
                inSensingArean = z;
                break;
            default:
            }
        }
    }

    @Override
    public void computeScroll() {
        if (viewDragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    public boolean canChildScrollUp() {
        return ViewCompat.canScrollVertically(scrollChild, -1);
    }

    public boolean canChildScrollDown() {
        return ViewCompat.canScrollVertically(scrollChild, 1);
    }

    private boolean canChildScrollRight() {
        return ViewCompat.canScrollHorizontally(scrollChild, -1);
    }

    private boolean canChildScrollLeft() {
        return ViewCompat.canScrollHorizontally(scrollChild, 1);
    }

    private void finish() {
        Activity act = (Activity) getContext();
        act.finish();
        act.overridePendingTransition(0, android.R.anim.fade_out);
    }

    private class ViewDragHelperCallBack extends ViewDragHelper.Callback {
        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return child == target && enablePullToBack;
        }

        @Override
        public int getViewVerticalDragRange(View child) {
            return verticalDragRange;
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            return horizontalDragRange;
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            int result = 0;

            if (dragEdge == DragEdge.TOP && !canChildScrollUp() && top > 0) {
                final int topBound = getPaddingTop();
                result = Math.min(Math.max(top, topBound), verticalDragRange);
            } else if (dragEdge != DragEdge.BOTTOM || canChildScrollDown() || top >= 0) {
                return 0;
            } else {
                return Math.min(Math.max(top, -verticalDragRange), getPaddingTop());
            }

            return result;
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            int result = 0;
            if (dragEdge == DragEdge.LEFT && !canChildScrollRight() && left > 0) {
                result = Math.min(Math.max(left, getPaddingLeft()), horizontalDragRange);
            } else if (dragEdge != DragEdge.RIGHT || canChildScrollLeft() || left >= 0) {
                return 0;
            } else {
                return Math.min(Math.max(left, -horizontalDragRange), getPaddingLeft());
            }

            return result;
        }

        @Override
        public void onViewDragStateChanged(int state) {
            if (state != draggingState) {
                if ((draggingState == ViewDragHelper.STATE_DRAGGING
                        || draggingState == ViewDragHelper.STATE_SETTLING)
                        && state == ViewDragHelper.STATE_IDLE
                        && draggingOffset == getDragRange()) {
                    finish();
                }
                draggingState = state;
            }
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            switch (dragEdge) {
            case TOP:
            case BOTTOM:
                draggingOffset = Math.abs(top);
                break;
            case LEFT:
            case RIGHT:
                draggingOffset = Math.abs(left);
                break;
            default:
                break;
            }

            //The proportion of the sliding.
            float fractionAnchor = (float) draggingOffset / finishAnchor;
            if (fractionAnchor >= 1) {
                fractionAnchor = 1;
            }

            float fractionScreen = (float) draggingOffset / (float) getDragRange();
            if (fractionScreen >= 1) {
                fractionScreen = 1;
            }

//            ivShadow.setAlpha(1 - fractionScreen);
            if (swipeBackListener != null) {
                swipeBackListener.onViewPositionChanged(fractionAnchor, fractionScreen);
            }
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            if (draggingOffset == 0 || draggingOffset == getDragRange()) {
                return;
            }

            boolean isBack = false;

            if (enableFlingBack && backBySpeed(xvel, yvel)) {
                isBack = !canChildScrollUp();
            } else if (draggingOffset >= finishAnchor) {
                isBack = true;
            } else if (draggingOffset < finishAnchor) {
                isBack = false;
            }

            int finalLeft;
            int finalTop;
            switch (dragEdge) {
            case LEFT:
                finalLeft = isBack ? horizontalDragRange : 0;
                smoothScrollToX(finalLeft);
                break;
            case RIGHT:
                finalLeft = isBack ? -horizontalDragRange : 0;
                smoothScrollToX(finalLeft);
                break;
            case TOP:
                finalTop = isBack ? verticalDragRange : 0;
                smoothScrollToY(finalTop);
                break;
            case BOTTOM:
                finalTop = isBack ? -verticalDragRange : 0;
                smoothScrollToY(finalTop);
                break;
            }
        }
    }

    private boolean backBySpeed(float xvel, float yvel) {
        switch (dragEdge) {
        case TOP:
        case BOTTOM:
            if (Math.abs(yvel) > Math.abs(xvel)
                    && Math.abs(yvel) > AUTO_FINISHED_SPEED_LIMIT) {
                return dragEdge == DragEdge.TOP ? !canChildScrollUp() : !canChildScrollDown();
            }
        case LEFT:
        case RIGHT:
            if (Math.abs(xvel) > Math.abs(yvel)
                    && Math.abs(xvel) > AUTO_FINISHED_SPEED_LIMIT) {
                return dragEdge == DragEdge.LEFT ? !canChildScrollLeft() : !canChildScrollRight();
            }
            break;
        }
        return false;
    }

    private void smoothScrollToX(int finalLeft) {
        if (viewDragHelper.settleCapturedViewAt(finalLeft, 0)) {
            ViewCompat.postInvalidateOnAnimation(SwipeBackLayout.this);
        }
    }

    private void smoothScrollToY(int finalTop) {
        if (viewDragHelper.settleCapturedViewAt(0, finalTop)) {
            ViewCompat.postInvalidateOnAnimation(SwipeBackLayout.this);
        }
    }

    public interface SwipeBackListener {
        /**
         * Return scrolled fraction of the layout.
         *
         * @param fractionAnchor relative to the anchor.
         * @param fractionScreen relative to the screen.
         */
        void onViewPositionChanged(float fractionAnchor, float fractionScreen);
    }
}
