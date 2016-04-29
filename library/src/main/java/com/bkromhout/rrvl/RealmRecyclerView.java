package com.bkromhout.rrvl;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewStub;
import android.widget.RelativeLayout;
import io.realm.RealmBasedRecyclerViewAdapter;

/**
 * A RecyclerView that supports Realm.
 */
public class RealmRecyclerView extends RelativeLayout implements RealmBasedRecyclerViewAdapter.StartDragListener {
    // Views.
    private RecyclerView recyclerView;
    private FastScroller fastScroller;
    private ViewStub emptyContentContainer;

    // Attributes.
    private int emptyViewId;
    private boolean dragAndDrop;
    private boolean fastScrollEnabled;

    private RealmBasedRecyclerViewAdapter adapter;
    private ItemTouchHelper touchHelper;
    private RealmSimpleItemTouchHelperCallback touchHelperCallback;

    public RealmRecyclerView(Context context) {
        super(context);
        init(context, null);
    }

    public RealmRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public RealmRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        inflate(context, R.layout.realm_recycler_view, this);

        // Get views.
        recyclerView = (RecyclerView) findViewById(R.id.rrv_recycler_view);
        fastScroller = (FastScroller) findViewById(R.id.rrv_fast_scroller);
        emptyContentContainer = (ViewStub) findViewById(R.id.rrv_empty_content_container);

        // Read attributes and set things up.
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.RealmRecyclerView);
        emptyViewId = typedArray.getResourceId(R.styleable.RealmRecyclerView_emptyLayoutId, 0);
        // Drag and drop.
        dragAndDrop = typedArray.getBoolean(R.styleable.RealmRecyclerView_dragAndDrop, false);
        touchHelperCallback = new RealmSimpleItemTouchHelperCallback(dragAndDrop,
                typedArray.getBoolean(R.styleable.RealmRecyclerView_longClickTriggersDrag, false));
        touchHelper = new ItemTouchHelper(touchHelperCallback);
        touchHelper.attachToRecyclerView(recyclerView);
        // Fast scroll.
        setFastScroll(typedArray.getBoolean(R.styleable.RealmRecyclerView_fastScroll, false));
        fastScroller.setAutoHideHandle(typedArray.getBoolean(R.styleable.RealmRecyclerView_autoHideFastScrollHandle,
                false));
        fastScroller.setAutoHideDelay(typedArray.getInt(R.styleable.RealmRecyclerView_handleAutoHideDelay,
                FastScroller.DEFAULT_HANDLE_HIDE_DELAY));
        fastScroller.setUseBubble(typedArray.getBoolean(R.styleable.RealmRecyclerView_useFastScrollBubble, false));
        fastScroller.setRecyclerView(recyclerView);
        typedArray.recycle();

        // Inflate empty view if present.
        if (emptyViewId != 0) {
            emptyContentContainer.setLayoutResource(emptyViewId);
            emptyContentContainer.inflate();
        }

        // Set LinearLayoutManager, override the onLayoutChildren() method.
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false) {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                super.onLayoutChildren(recycler, state);
                if (!fastScrollEnabled) return;

                final int firstVisibleItemPosition = findFirstVisibleItemPosition();
                if (firstVisibleItemPosition != 0) {
                    // Hide the fast scroller if not initialized, or no items are shown.
                    if (firstVisibleItemPosition == -1) fastScroller.setVisibility(View.GONE);
                    return;
                }
                final int lastVisibleItemPosition = findLastVisibleItemPosition();
                int itemsShown = lastVisibleItemPosition - firstVisibleItemPosition + 1;
                // Hide fast scroller if all items are visible in the viewport currently.
                fastScroller.setVisibility(adapter != null && adapter.getItemCount() > itemsShown
                        ? View.VISIBLE : View.GONE);
            }
        });
        recyclerView.setHasFixedSize(true);
    }

    private void updateEmptyContentContainerVisibility(RecyclerView.Adapter adapter) {
        if (emptyViewId == 0) return;
        emptyContentContainer.setVisibility(adapter != null && adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    @Override
    public final void startDragging(RecyclerView.ViewHolder viewHolder) {
        if (dragAndDrop && touchHelper != null) touchHelper.startDrag(viewHolder);
    }

    /**
     * Set the adapter for this RealmRecyclerView.
     * @param adapter {@link RealmBasedRecyclerViewAdapter}.
     */
    public void setAdapter(final RealmBasedRecyclerViewAdapter adapter) {
        this.adapter = adapter;
        recyclerView.setAdapter(adapter);

        touchHelperCallback.setListener(adapter);

        if (adapter != null) {
            adapter.setOnStartDragListener(this);

            adapter.registerAdapterDataObserver(
                    new RecyclerView.AdapterDataObserver() {
                        @Override
                        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
                            super.onItemRangeMoved(fromPosition, toPosition, itemCount);
                            update();
                        }

                        @Override
                        public void onItemRangeRemoved(int positionStart, int itemCount) {
                            super.onItemRangeRemoved(positionStart, itemCount);
                            update();
                        }

                        @Override
                        public void onItemRangeInserted(int positionStart, int itemCount) {
                            super.onItemRangeInserted(positionStart, itemCount);
                            update();
                        }

                        @Override
                        public void onItemRangeChanged(int positionStart, int itemCount) {
                            super.onItemRangeChanged(positionStart, itemCount);
                            update();
                        }

                        @Override
                        public void onChanged() {
                            super.onChanged();
                            update();
                        }

                        private void update() {
                            updateEmptyContentContainerVisibility(adapter);
                        }
                    }
            );
        }
        updateEmptyContentContainerVisibility(adapter);
    }

    /**
     * Get whether drag and drop is enabled.
     * @return Whether drag and drop is enabled or not.
     */
    public final boolean getDragAndDrop() {
        return dragAndDrop;
    }

    /**
     * Enable/Disable drag and drop.
     * @param enabled Whether to allow drag and drop.
     */
    public final void setDragAndDrop(boolean enabled) {
        this.dragAndDrop = enabled;
        touchHelperCallback.setDragAndDrop(enabled);
    }

    /**
     * Get whether long click triggers drags.
     * @return Whether long click triggers drags or not.
     */
    public final boolean getLongClickTriggersDrag() {
        return touchHelperCallback.getLongClickTriggersDrag();
    }

    /**
     * Whether to use long click to trigger the drag or not.
     * @param longClickTriggersDrag Whether to allow long clicks to start drags.
     */
    public final void setLongClickTriggersDrag(boolean longClickTriggersDrag) {
        touchHelperCallback.setLongClickTriggersDrag(longClickTriggersDrag);
    }

    /**
     * Get whether fast scrolling is enabled.
     * @return Whether fast scrolling is enabled or not.
     */
    public final boolean getFastScroll() {
        return fastScrollEnabled;
    }

    /**
     * Enable/Disable the fast scroller. The system-drawn scrollbars will be enabled if the fast scroller isn't (and
     * vice versa).
     * @param enabled Whether to enable the fast scroller or not.
     */
    @SuppressWarnings("unused")
    public final void setFastScroll(boolean enabled) {
        this.fastScrollEnabled = enabled;
        recyclerView.setVerticalScrollBarEnabled(!enabled);
        fastScroller.setVisibility(enabled ? VISIBLE : GONE);
    }

    /**
     * Get whether the fast scroller's handle is set to auto-hide.
     * @return Whether the fast scroller's handle is set to auto-hide or not.
     */
    public final boolean getAutoHideFastScrollHandle() {
        return fastScroller.getAutoHideHandle();
    }

    /**
     * Set whether the fast scroller handle will auto-hide or stay visible.
     * @param autoHide Whether to auto-hide the fast scroller handle or not.
     */
    @SuppressWarnings("unused")
    public final void setAutoHideFastScrollHandle(boolean autoHide) {
        fastScroller.setAutoHideHandle(autoHide);
    }

    /**
     * Get the delay (in milliseconds) before which the fast scroller handle with auto-hide.
     * @return Auto-hide delay.
     */
    public final int getHandleAutoHideDelay() {
        return fastScroller.getAutoHideDelay();
    }

    /**
     * Set the delay (in milliseconds) before which the fast scroller handle will auto-hide. Default is {@link
     * FastScroller#DEFAULT_HANDLE_HIDE_DELAY}.
     * @param autoHideDelay Auto-hide delay. If < 0, will use the default.
     */
    @SuppressWarnings("unused")
    public final void setHandleAutoHideDelay(int autoHideDelay) {
        fastScroller.setAutoHideDelay(autoHideDelay);
    }

    /**
     * Get whether the fast scroller's bubble is being used.
     * @return Whether the fast scroller's bubble is being used or not.
     */
    public final boolean getUseFastScrollBubble() {
        return fastScroller.getUseBubble();
    }

    /**
     * Set whether to use the fast scroller bubble or not.
     * <p>
     * If set to true, you need to have a class implement {@link BubbleTextProvider#getFastScrollBubbleText(int)} and
     * pass it to this {@link RealmRecyclerView} using {@link #setBubbleTextProvider(BubbleTextProvider)} so that the
     * fast scroller will know what text to put into the bubble.
     * @param useBubble Whether to use the fast scroller bubble or not.
     */
    @SuppressWarnings("unused")
    public final void setUseFastScrollBubble(boolean useBubble) {
        fastScroller.setUseBubble(useBubble);
    }

    /**
     * Set the bubble text provider to use.
     * @param bubbleTextProvider Bubble text provider.
     */
    @SuppressWarnings("unused")
    public final void setBubbleTextProvider(BubbleTextProvider bubbleTextProvider) {
        fastScroller.setBubbleTextProvider(bubbleTextProvider);
    }

    /**
     * Set the fast scroll handle state listener to use.
     * @param handleStateListener Fast scroll handle state listener.
     */
    @SuppressWarnings("unused")
    public final void setFastScrollHandleStateListener(FastScrollHandleStateListener handleStateListener) {
        fastScroller.setHandleStateListener(handleStateListener);
    }

    /**
     * Get the actual RecyclerView which backs this {@link RealmRecyclerView}.
     * @return Internal RecyclerView.
     */
    @SuppressWarnings("unused")
    public RecyclerView getRecyclerView() {
        return recyclerView;
    }

    /**
     * Get the LinearLayoutManager attached to the RealmRecyclerView.
     * @return LinearLayoutManager.
     */
    @SuppressWarnings("unused")
    public LinearLayoutManager getLayoutManager() {
        return (LinearLayoutManager) recyclerView.getLayoutManager();
    }
}
