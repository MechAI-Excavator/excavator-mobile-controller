package com.capstone.excavator;

import android.graphics.Rect;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * 尺寸页「选择机型」列表：布局由 {@link R.layout#item_dimension_model_card} 与
 * {@link R.integer#dim_model_grid_span_count} / {@link R.dimen#dim_model_grid_gap} 维护；
 * 本类仅负责数据绑定与选中态。
 */
public final class DimensionModelCardAdapter extends RecyclerView.Adapter<DimensionModelCardAdapter.VH> {

    public interface OnModelClickListener {
        void onModelSelected(DimensionModelCatalog.Entry entry);
    }

    private final List<DimensionModelCatalog.Entry> entries;
    private final OnModelClickListener listener;
    @Nullable
    private String selectedModelId;

    public DimensionModelCardAdapter(@NonNull OnModelClickListener listener) {
        this.entries = DimensionModelCatalog.ENTRIES;
        this.listener = listener;
    }

    public void setSelectedModelId(@Nullable String modelId) {
        this.selectedModelId = modelId;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View root = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_dimension_model_card, parent, false);
        return new VH(root);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        DimensionModelCatalog.Entry e = entries.get(position);
        bindEntry(holder, e);
        boolean sel = e.modelId != null && e.modelId.equals(selectedModelId);
        holder.check.setVisibility(sel ? View.VISIBLE : View.GONE);
        float d = holder.itemView.getResources().getDisplayMetrics().density;
        holder.itemView.setBackgroundResource(sel
                ? R.drawable.model_card_selected_bg
                : R.drawable.card_light_bg);
        holder.itemView.setElevation(sel ? 2f * d : 1f * d);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onModelSelected(e);
            }
        });
    }

    private static void bindEntry(VH h, DimensionModelCatalog.Entry e) {
        if (e == null) {
            return;
        }
        h.image.setImageResource(e.imageResId);
        h.title.setText(e.displayName);
        h.title.setTextSize(TypedValue.COMPLEX_UNIT_SP, e.titleTextSp);
        h.tonnage.setText(e.tonnageLabel);
        h.boom.setText(e.boomCardText);
        h.stick.setText(e.stickCardText);
    }

    static final class VH extends RecyclerView.ViewHolder {
        final ImageView image;
        final TextView title;
        final TextView tonnage;
        final TextView boom;
        final TextView stick;
        final TextView check;

        VH(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.dimModelImage);
            title = itemView.findViewById(R.id.dimModelTitle);
            tonnage = itemView.findViewById(R.id.dimModelTonnage);
            boom = itemView.findViewById(R.id.dimModelBoom);
            stick = itemView.findViewById(R.id.dimModelStick);
            check = itemView.findViewById(R.id.dimModelCheck);
        }
    }

    /**
     * 网格项间距；{@code spanCount} 须与 {@link R.integer#dim_model_grid_span_count} 一致。
     */
    public static final class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {
        private final int spanCount;
        private final int spacingPx;
        private final boolean includeEdge;

        public GridSpacingItemDecoration(int spanCount, int spacingPx, boolean includeEdge) {
            this.spanCount = Math.max(1, spanCount);
            this.spacingPx = spacingPx;
            this.includeEdge = includeEdge;
        }

        @Override
        public void getItemOffsets(
                @NonNull Rect outRect,
                @NonNull View view,
                @NonNull RecyclerView parent,
                @NonNull RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            if (position < 0) {
                return;
            }
            int column = position % spanCount;

            if (includeEdge) {
                outRect.left = spacingPx - column * spacingPx / spanCount;
                outRect.right = (column + 1) * spacingPx / spanCount;
                if (position < spanCount) {
                    outRect.top = spacingPx;
                }
                outRect.bottom = spacingPx;
            } else {
                outRect.left = column * spacingPx / spanCount;
                outRect.right = spacingPx - (column + 1) * spacingPx / spanCount;
                if (position >= spanCount) {
                    outRect.top = spacingPx;
                }
                outRect.bottom = spacingPx;
            }
        }
    }
}
