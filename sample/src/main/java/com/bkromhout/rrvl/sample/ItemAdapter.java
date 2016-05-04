package com.bkromhout.rrvl.sample;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bkromhout.rrvl.BubbleTextProvider;
import com.bkromhout.rrvl.RealmRecyclerViewAdapter;
import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Simple item adapter. Supports drag and drop and the fast scroller's bubble text.
 */
public class ItemAdapter extends RealmRecyclerViewAdapter<Item, ItemAdapter.ItemVH> implements BubbleTextProvider {
    private Context context;

    public ItemAdapter(Context context, RealmResults<Item> realmResults) {
        super(context, realmResults);
        setHasStableIds(true);
        this.context = context;
    }

    @Override
    public long getItemId(int position) {
        return realmResults.get(position).uniqueId;
    }

    @Override
    public ItemVH onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ItemVH(inflater.inflate(R.layout.item_card, parent, false));
    }

    @Override
    public void onBindViewHolder(final ItemVH holder, int position) {
        Item item = realmResults.get(position);
        holder.name.setText(item.name);
        // We set the unique ID as the tag on a view so that we will be able to get it
        // in the onMove() method.
        holder.content.setTag(item.uniqueId);
        // Grabbing the drag handle should trigger a drag.
        holder.dragHandle.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN)
                    startDragging(holder);
                return false;
            }
        });
    }

    @Override
    public boolean onMove(RecyclerView.ViewHolder dragging, RecyclerView.ViewHolder target) {
        // Get positions of items in adapter.
        int draggingPos = dragging.getAdapterPosition();
        int targetPos = target.getAdapterPosition();

        // Get the unique IDs of the items from the tag that we set in onBindViewHolder().
        long draggingId = (long) ((ItemVH) dragging).content.getTag();
        long targetId = (long) ((ItemVH) target).content.getTag();

        // Move the item up or down. The methods in ItemDragHelper will calculate and
        // assign a new position value for the item whose uniqueId == draggingId.
        if (draggingPos > targetPos) ItemDragHelper.moveItemToBefore(draggingId, targetId);
        else ItemDragHelper.moveItemToAfter(draggingId, targetId);

        return true;
    }

    @Override
    public String getFastScrollBubbleText(int position) {
        return String.valueOf(realmResults.get(position).name.charAt(0));
    }

    class ItemVH extends RecyclerView.ViewHolder {
        @Bind(R.id.content)
        RelativeLayout content;
        @Bind(R.id.drag_handle)
        ImageView dragHandle;
        @Bind(R.id.name)
        TextView name;
        @Bind(R.id.delete_button)
        ImageButton delete;

        public ItemVH(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new MaterialDialog.Builder(context)
                            .title(R.string.action_delete)
                            .negativeText(R.string.cancel)
                            .positiveText(R.string.ok)
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    final long uniqueId = (long) content.getTag();
                                    try (Realm realm = Realm.getDefaultInstance()) {
                                        realm.executeTransaction(new Realm.Transaction() {
                                            @Override
                                            public void execute(Realm realm) {
                                                realm.where(Item.class).equalTo("uniqueId", uniqueId).findFirst()
                                                     .deleteFromRealm();
                                            }
                                        });
                                    }
                                }
                            })
                            .show();
                }
            });
        }
    }
}
