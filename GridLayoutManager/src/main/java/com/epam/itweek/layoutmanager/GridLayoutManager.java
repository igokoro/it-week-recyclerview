package com.epam.itweek.layoutmanager;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import hugo.weaving.DebugLog;

/**
 * Simple grid layout manager.
 */
public class GridLayoutManager extends RecyclerView.LayoutManager {

    private static final boolean DEBUG = true;

    private static final String TAG = GridLayoutManager.class.getSimpleName();

    /**
     * Helper class that keeps temporary rendering state.
     * It does not keep state after rendering is complete but we still keep a reference to re-use
     * the same object.
     */
    private RenderState mRenderState;

    private final int columns;

    public GridLayoutManager(int columns) {
        this.columns = columns;
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
    }

    @Override
    public boolean canScrollVertically() {
        return true;
    }

    @Override @DebugLog
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        ensureRenderState();

        int anchorCoordinate, anchorItemPosition;
        if (getChildCount() == 0) {
            anchorCoordinate = getPaddingTop();
            anchorItemPosition = 0;
        } else {
            View referenceChild = getChildClosestToStart(); // first child

            anchorCoordinate = getDecoratedVerticalStart(referenceChild);
            anchorItemPosition = getPosition(referenceChild);
        }

        detachAndScrapAttachedViews(recycler);

        // fill towards end
        updateRenderStateToFillEnd(anchorItemPosition, anchorCoordinate);

        fill(recycler, mRenderState, state);
    }

    /**
     * The magic functions :). Fills the given layout, defined by the renderState. This is fairly
     * independent from the rest of the {@link android.support.v7.widget.LinearLayoutManager}
     * and with little change, can be made publicly available as a helper class.
     *
     * @param recycler        Current recycler that is attached to RecyclerView
     * @param renderState     Configuration on how we should fill out the available space.
     * @param state           Context passed by the RecyclerView to control scroll steps.
     * @return Number of pixels that it added. Useful for scroll functions.
     */
    @DebugLog
    private int fill(RecyclerView.Recycler recycler, RenderState renderState, RecyclerView.State state) {
        if (renderState.mScrollingOffset != RenderState.SCOLLING_OFFSET_NaN) {
            // TODO ugly bug fix. should not happen
            if (renderState.mAvailable < 0) {
                renderState.mScrollingOffset += renderState.mAvailable;
            }
            Log.d(TAG, "fill: initial recycler pass");
            recycleByRenderState(recycler, renderState);
        }

        final int start = renderState.mAvailable;

        int remainingSpace = renderState.mAvailable;
        while (remainingSpace > 0 && renderState.hasMore(state)) {
            // new row - we need to layout it
            int consumed = layoutRow(recycler, renderState, state);
            remainingSpace -= consumed;
            renderState.mAvailable -= consumed;
            renderState.mOffset += consumed * renderState.mLayoutDirection;

            if (renderState.mScrollingOffset != RenderState.SCOLLING_OFFSET_NaN) {
                renderState.mScrollingOffset += consumed;
                if (renderState.mAvailable < 0) {
                    renderState.mScrollingOffset += renderState.mAvailable;
                }
                Log.d(TAG, "fill: subsequent recycler pass");
                recycleByRenderState(recycler, renderState);
            }
        }

        return start - renderState.mAvailable;
    }

    /**
     * Layout a row single row of data.
     * @param recycler        Current recycler that is attached to RecyclerView
     * @param renderState     Configuration on how we should fill out the available space.
     * @param state           Context passed by the RecyclerView to control scroll steps.
     *
     * @return Number of pixels that it added. Useful for scroll functions.
     */
    private int layoutRow(RecyclerView.Recycler recycler, RenderState renderState, RecyclerView.State state) {
        int numberOfColumns = 0;
        int cellSize = getWidth() / columns;
        boolean backwardLayout = mRenderState.mLayoutDirection == RenderState.LAYOUT_START;
        int top;
        int bottom;
        if (backwardLayout) {
            bottom = renderState.mOffset;
            top = bottom - cellSize;
        } else {
            top = renderState.mOffset;
            bottom = top + cellSize;
        }
        while (renderState.hasMore(state) && numberOfColumns < columns) {
            int column = renderState.mCurrentPosition % columns;

            int left = cellSize * column;
            int right = left + cellSize;

            numberOfColumns++;

            View view = renderState.next(recycler);
            if (view == null) {
                break;
            }
            if (backwardLayout) {
                addView(view, 0);
            } else {
                addView(view);
            }
            // measure view
            measureChildWithMargins(view, 0, 0);
            // layout view
            // We calculate everything with View's bounding box (which includes decor and margins)
            // To calculate correct layout position, we subtract margins.
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();
            layoutDecorated(view, left + params.leftMargin, top + params.topMargin
                    , right - params.rightMargin, bottom - params.bottomMargin);
        }

        return cellSize;
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        return scrollBy(dy, recycler, state);
    }

    @DebugLog
    private int scrollBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (getChildCount() == 0 || dy == 0) {
            return 0;
        }
        ensureRenderState();
        final int layoutDirection = dy > 0 ? RenderState.LAYOUT_END : RenderState.LAYOUT_START;
        final int absDy = Math.abs(dy);
        updateRenderState(layoutDirection, absDy);
        // this is amount of edge block that is off-screen - so that we can simply translate existing children and not create/layout new
        final int freeScroll = mRenderState.mScrollingOffset;
        int consumed = freeScroll + fill(recycler, mRenderState, state);
        int directionalPaddingOffset;
        // calculate additional amount of space we need to scroll after last view and before first to cover system decorations:
        // action, system and navigation bars
        if (layoutDirection == RenderState.LAYOUT_END) {
            directionalPaddingOffset = getPaddingBottom() - (getHeight() - getDecoratedVerticalEnd(getChildClosestToEnd()));
        } else {
            directionalPaddingOffset = getPaddingTop() - getDecoratedVerticalStart(getChildClosestToStart());
        }
        // if we reached last/first element use additional amount calculated before
        if (consumed <= directionalPaddingOffset) {
            consumed = directionalPaddingOffset;
        }
        if (consumed <= 0) {
            if (DEBUG) {
                Log.d(TAG, "Don't have any more elements to scroll");
            }
            return 0;
        }
        final int scrolled = absDy > consumed ? layoutDirection * consumed : dy;
        // translate children vertically so that scrolling is actually happening
        offsetChildrenVertical( - scrolled);
        if (DEBUG) {
            Log.d(TAG, "scroll req: " + dy + " scrolled: " + scrolled);
        }
        return scrolled;
    }

    /**
     * Configures {@link #mRenderState} so that {@code fill(...)} knows how to proceed with layout of views on scrolling.
     *
     * @param layoutDirection direction in which layout should happen - for scrolling from start to end - this is LAYOUT_END, in reverse - LAYOUT_START.
     * @param requiredSpace amount of scrolled space to cover with views
     */
    private void updateRenderState(int layoutDirection, int requiredSpace) {
        mRenderState.mLayoutDirection = layoutDirection;
        // this is amount of edge block that is off-screen - so that we can simply translate existing children and not create/layout new
        int fastScrollSpace;
        View child;
        if (layoutDirection == RenderState.LAYOUT_END) {
            // scrolling from start to end
            // get the first child in the direction we are going
            child = getChildClosestToEnd();
            mRenderState.mItemDirection = RenderState.ITEM_DIRECTION_TAIL;
            int decorateVerticalEnd = getDecoratedVerticalEnd(child);
            mRenderState.mOffset = decorateVerticalEnd;
            // calculate how much we can scroll without adding new children (independent of layout)
            fastScrollSpace = decorateVerticalEnd - getEnd();
        } else {
            mRenderState.mItemDirection = RenderState.ITEM_DIRECTION_HEAD;
            child = getChildClosestToStart();
            int decoratedVerticalStart = getDecoratedVerticalStart(child);
            fastScrollSpace = getStart() - decoratedVerticalStart;
            mRenderState.mOffset = decoratedVerticalStart;
        }

        mRenderState.mCurrentPosition = getPosition(child) + mRenderState.mItemDirection;
        if (fastScrollSpace >= requiredSpace) {
            // we have enough free scrolling space to not even calculate further
            mRenderState.mAvailable = 0;
            mRenderState.mScrollingOffset = requiredSpace;
        } else {
            mRenderState.mAvailable = requiredSpace - fastScrollSpace;
            mRenderState.mScrollingOffset = fastScrollSpace;
        }
    }

    /**
     * Measure child according to rules of this layout manager:
     *  - children are square
     *  - size equals to a width of a container divided by the number of columns
     *  - layout params of a child is not taken into consideration
     *
     * @param child child to measure
     * @param widthUsed Width in pixels currently consumed by other views, if relevant
     * @param heightUsed Height in pixels currently consumed by other views, if relevant
     */
    @Override
    public void measureChildWithMargins(View child, int widthUsed, int heightUsed) {
        final RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) child.getLayoutParams();

        int size = (int) (getWidth() / (float)columns);
        lp.width = size;
        lp.height = size;

        super.measureChildWithMargins(child, widthUsed, heightUsed);
    }

    /**
     * Helper method to call appropriate recycle method depending on current render layout
     * direction
     *
     * @param recycler    Current recycler that is attached to RecyclerView
     * @param renderState Current render state. Right now, this object does not change but
     *                    we may consider moving it out of this view so passing around as a
     *                    parameter for now, rather than accessing {@link #mRenderState}
     * @see #recycleViewsFromStart(android.support.v7.widget.RecyclerView.Recycler, int)
     * @see #recycleViewsFromEnd(android.support.v7.widget.RecyclerView.Recycler, int)
     * @see android.support.v7.widget.LinearLayoutManager.RenderState#mLayoutDirection
     */
    @DebugLog
    private void recycleByRenderState(RecyclerView.Recycler recycler, RenderState renderState) {
        int childCountBefore = getChildCount();
        if (renderState.mLayoutDirection == RenderState.LAYOUT_START) {
            recycleViewsFromEnd(recycler, renderState.mScrollingOffset);
        } else {
            recycleViewsFromStart(recycler, renderState.mScrollingOffset);
        }
        int childCountAfter = getChildCount();
        Log.d(TAG, "recycleByRenderState: recycled items = " + (childCountBefore - childCountAfter));
    }

    /**
     * Recycles views that went out of bounds after scrolling towards the end of the layout.
     *
     * We fill RecyclerView with rows of views, so we have to recycle whole rows as well.
     *
     * Iterate over children starting from first child.
     * Row should be recycled if all children of a block are above {@code getStart() + dt}.
     * Stop iteration once we found the first view with bottom edge below {@code getStart() + dt}
     * - this means the whole row and everything below it can't be recycled.
     *
     * RecyclerView features 2 types of view cache:
     *   1. Small number of cached views that are not used for binding to new data right away.
     *      These can be used to remember view that has just went out of viewport on scrolling
     *      and might return if user decides to slightly scroll in the opposite direction.
     *      This way there is no need to rebind them.
     *      The most recently recycled views are subject to be cached this way.
     *      Size of the cache is controlled with {@link android.support.v7.widget.RecyclerView.Recycler#setViewCacheSize(int)}
     *
     *   2. Actual recycled views that are detached from their data and can be used to display new data.
     *      Older views from view cache are moved to recycling pool once newer items arrive into cache.
     *
     * In order to fully leverage recycler semantics we recycle views from start to end
     * so that views closed to viewport go into the first level view cache.
     *
     * @param recycler Recycler instance of {@link android.support.v7.widget.RecyclerView}
     * @param dt       This can be used to add additional padding to the visible area. This is used
     *                 to detect children that will go out of bounds after scrolling, without actually
     *                 moving them.
     */
    private void recycleViewsFromStart(RecyclerView.Recycler recycler, int dt) {
        if (dt < 0) {
            if (DEBUG) {
                Log.d(TAG, "Called recycle from start with a negative value. This might happen"
                        + " during layout changes but may be sign of a bug");
            }
        }
        // limit that block must not cross to be recycled
        final int limit = getStart() + dt;
        final int childCount = getChildCount();

        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(0);

            int decoratedVerticalEnd = getDecoratedVerticalEnd(child);
            if (decoratedVerticalEnd <= limit) {
                removeAndRecycleViewAt(0, recycler);
            } else {
                // do not look further - here we already know that this block can't be recycled
                break;
            }
        }
    }

    /**
     * Recycles views that went out of bounds after scrolling towards the end of the layout.
     *
     * We fill RecyclerView with rows of views, so we have to recycle whole rows as well.
     *
     * Iterate over children starting from last child.
     * Row should be recycled if all children of a row are below {@code getEnd() - dt}.
     * Stop iteration once we found the first view with top edge above {@code getEnd() - dt}
     * - this means the whole block and everything above it can't be recycled.
     *
     * RecyclerView features 2 types of view cache:
     *   1. Small number of cached views that are not used for binding to new data right away.
     *      These can be used to remember view that has just went out of viewport on scrolling
     *      and might return if user decides to slightly scroll in the opposite direction.
     *      This way there is no need to rebind them.
     *      The most recently recycled views are subject to be cached this way.
     *      Size of the cache is controlled with {@link android.support.v7.widget.RecyclerView.Recycler#setViewCacheSize(int)}
     *
     *   2. Actual recycled views that are detached from their data and can be used to display new data.
     *      Older views from view cache are moved to recycling pool once newer items arrive into cache.
     *
     * In order to fully leverage recycler semantics we recycle views from end to start
     * so that views closed to viewport go into the first level view cache.
     *
     * @param recycler Recycler instance of {@link android.support.v7.widget.RecyclerView}
     * @param dt       This can be used to add additional padding to the visible area. This is used
     *                 to detect children that will go out of bounds after scrolling, without actually
     *                 moving them.
     *
     */
    private void recycleViewsFromEnd(RecyclerView.Recycler recycler, int dt) {
        if (dt < 0) {
            if (DEBUG) {
                Log.d(TAG, "Called recycle from end with a negative value. This might happen"
                        + " during layout changes but may be sign of a bug");
            }
        }
        // limit that block must not cross to be recycled
        final int limit = getEnd() - dt;
        final int childCount = getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            View child = getChildAt(i);

            int decoratedVerticalStart = getDecoratedVerticalStart(child);
            if (decoratedVerticalStart >= limit) {
                removeAndRecycleViewAt(i, recycler);
            } else {
                // do not look further - here we already know that this block can't be recycled
                break;
            }
        }
    }

    /**
     * Prepare {@link #mRenderState} for regular start to end layout.
     *
     * @param itemPosition first element position to layout
     * @param offset offset in px where children should start drawing themselves
     */
    private void updateRenderStateToFillEnd(int itemPosition, int offset) {
        mRenderState.mAvailable = getEnd() - offset;
        mRenderState.mCurrentPosition = itemPosition;
        mRenderState.mLayoutDirection = RenderState.LAYOUT_END;
        mRenderState.mItemDirection = RenderState.ITEM_DIRECTION_TAIL;
        mRenderState.mOffset = offset;
        mRenderState.mScrollingOffset = RenderState.SCOLLING_OFFSET_NaN;
    }

    private void ensureRenderState() {
        if (mRenderState == null) {
            mRenderState = new RenderState();
        }
    }

    private View getChildClosestToStart() {
        return getChildAt(0);
    }

    private View getChildClosestToEnd() {
        return getChildAt(getChildCount() - 1);
    }

    public int getDecoratedVerticalStart(View view) {
        final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                view.getLayoutParams();
        return getDecoratedTop(view) - params.topMargin;
    }

    private int getDecoratedVerticalEnd(View view) {
        final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                view.getLayoutParams();
        return getDecoratedBottom(view) + params.bottomMargin;
    }

    private int getStart() {
        return 0;
    }

    private int getEnd() {
        return getHeight();
    }

    /**
     * Helper class that keeps temporary state while {@link android.support.v7.widget.RecyclerView.LayoutManager} is filling out the empty
     * space.
     */
    private static class RenderState {

        final static int LAYOUT_START = -1;

        final static int LAYOUT_END = 1;

        final static int ITEM_DIRECTION_HEAD = -1;

        final static int ITEM_DIRECTION_TAIL = 1;

        final static int SCOLLING_OFFSET_NaN = Integer.MIN_VALUE;

        /**
         * Pixel offset where rendering should start
         */
        int mOffset;

        /**
         * Number of pixels that we should fill, in the layout direction.
         */
        int mAvailable;

        /**
         * Current position on the adapter to get the next item.
         */
        int mCurrentPosition;

        /**
         * Defines the direction in which the layout is filled.
         * Should be {@link #LAYOUT_START} or {@link #LAYOUT_END}
         */
        int mLayoutDirection;

        /**
         * Used when RenderState is constructed in a scrolling state.
         * It should be set the amount of scrolling we can make without creating a new view.
         * Settings this is required for efficient view recycling.
         */
        int mScrollingOffset;

        /**
         * Defines the direction in which the data adapter is traversed.
         * Should be {@link #ITEM_DIRECTION_HEAD} or {@link #ITEM_DIRECTION_TAIL}
         */
        int mItemDirection;


        /**
         * @return true if there are more items in the data adapter
         */
        boolean hasMore(RecyclerView.State state) {
            return mCurrentPosition >= 0 && mCurrentPosition < state.getItemCount();
        }

        /**
         * Gets the view for the next element that we should render.
         * Also updates current item index to the next item
         *
         * @return The next element that we should render.
         */
        View next(RecyclerView.Recycler recycler) {
            final View view = recycler.getViewForPosition(mCurrentPosition);
            mCurrentPosition += mItemDirection;
            return view;
        }

    }
}
