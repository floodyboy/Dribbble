package com.hunter.adapter;

import android.animation.Animator;
import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.LayoutParams;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.LinearLayout;

import com.hunter.adapter.animation.AlphaInAnimation;
import com.hunter.adapter.animation.BaseAnimation;

import java.util.ArrayList;
import java.util.List;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public abstract class BaseQuickAdapter<T, VH extends BaseViewHolder> extends RecyclerView.Adapter<VH> {

    public static final int ITEM_VIEW = 100;
    public static final int HEADER_VIEW = 101;
    public static final int FOOTER_VIEW = 102;
    public static final int EMPTY_VIEW = 103;
    public static final int ERROR_VIEW = 104;
    public static final int LOADING_VIEW = 105;
    public static final int LOAD_NO_MORE_VIEW = 106;
    public static final int LOAD_MORE_FAIL_VIEW = 107;

    public static final int STATUS_NORMAL = 0;
    public static final int STATUS_ERROR = 1;
    public static final int STATUS_EMPTY = 2;
    public static final int STATUS_LOADING = 3;
    public static final int STATUS_LOAD_NO_MORE = 4;
    public static final int STATUS_LOAD_MORE_FAIL = 5;

    public static final int DEF_ANIM_DURATION = 225;

    private boolean mIsOpenLoadEnable;

    private BaseAnimation mItemAnimation;
    private boolean mOpenAnimationEnable;

    private int mLastAddedAnimPos = -1;

    private LoadMoreListener mLoadMoreListener;

    private LinearLayout mHeaderView;

    private int mItemStatus;
    private View mEmptyView;
    private View mErrorView;

    private LinearLayout mFooterView;
    private View mLoadingView;
    private View mLoadNoMoreView;
    private View mLoadMoreFailedView;

    private int mPageSize = -1;

    protected Context mContext;
    protected int mLayoutResId;
    protected LayoutInflater mLayoutInflater;
    protected List<T> mDatas;

    protected RecyclerView mRecyclerView;

    public interface LoadMoreListener {

        void onLoadMoreRequested();
    }

    public BaseQuickAdapter(List<T> datas) {
        this(0, datas);
    }

    public BaseQuickAdapter(int layoutResId, List<T> datas) {
        mDatas = datas == null ? new ArrayList<T>() : datas;
        if (layoutResId != 0) mLayoutResId = layoutResId;
    }

    @Override
    public void onAttachedToRecyclerView(final RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mRecyclerView = recyclerView;
        RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
        /* 返回当前所在行数的最大列数，为了占满一行 */
        if (manager instanceof GridLayoutManager) {
            final GridLayoutManager gridManager = ((GridLayoutManager) manager);
            gridManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    int type = getItemViewType(position);
                    if (isNeedFullSpan(type)) return gridManager.getSpanCount();
                    return 1;
                }
            });
        }
    }

    @Override
    public void onViewAttachedToWindow(VH holder) {
        super.onViewAttachedToWindow(holder);
        int type = holder.getItemViewType();
        if (isNeedFullSpan(type)) setFullSpan(holder);
        addAnimation(holder);
    }

    private boolean isNeedFullSpan(int type) {
        return type == EMPTY_VIEW ||
                type == ERROR_VIEW ||
                type == HEADER_VIEW ||
                type == FOOTER_VIEW ||
                type == LOADING_VIEW ||
                type == LOAD_NO_MORE_VIEW ||
                type == LOAD_MORE_FAIL_VIEW;
    }

    /**
     * 当布局为 StaggeredGridLayoutManager 时需要占满整个宽度
     */
    protected void setFullSpan(RecyclerView.ViewHolder holder) {
        if (holder.itemView.getLayoutParams() instanceof StaggeredGridLayoutManager.LayoutParams) {
            StaggeredGridLayoutManager.LayoutParams params = (StaggeredGridLayoutManager.LayoutParams) holder
                    .itemView.getLayoutParams();
            params.setFullSpan(true);
        }
    }

    private void addAnimation(RecyclerView.ViewHolder holder) {
        if (mOpenAnimationEnable) {
            if (holder.getLayoutPosition() > mLastAddedAnimPos) {
                BaseAnimation animation;
                if (mItemAnimation != null) {
                    animation = mItemAnimation;
                    for (Animator anim : animation.getAnimators(holder.itemView)) {
                        anim.setDuration(DEF_ANIM_DURATION).start();
                        anim.setInterpolator(new AccelerateDecelerateInterpolator());
                    }
                }
                mLastAddedAnimPos = holder.getLayoutPosition();
            }
        }
    }

    @Override
    public int getItemCount() {
        boolean isLoadMore = isLoadMore();
        mItemStatus = isLoadMore ? STATUS_LOADING : mItemStatus;
        int count = mDatas.size() + getHeaderViewCount() + getFooterLayoutCount();
        if (mDatas.size() == 0) {
            if (mEmptyView != null && mItemStatus == STATUS_EMPTY) count++;
            else if (mErrorView != null && mItemStatus == STATUS_ERROR) count++;
        } else if (mLoadingView != null && isLoadMore) count++;
        else if (mLoadMoreFailedView != null && mItemStatus == STATUS_LOAD_MORE_FAIL) count++;
        else if (mLoadNoMoreView != null && mItemStatus == STATUS_LOAD_NO_MORE) count++;

        return count;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        if (mHeaderView != null && position == 0) return HEADER_VIEW;
        if (mFooterView != null && position == getItemCount() - (mItemStatus == STATUS_LOADING ? 1 : 0) - 1)
            return FOOTER_VIEW;
        if (mDatas.size() == 0) {
            if (position == 0) {
                if (mItemStatus == STATUS_EMPTY && mEmptyView != null) return EMPTY_VIEW;
                if (mItemStatus == STATUS_ERROR && mErrorView != null) return ERROR_VIEW;
            }
        }
        if (position == getItemCount() - 1) {
            if (mItemStatus == STATUS_LOAD_MORE_FAIL && mLoadMoreFailedView != null) return LOAD_MORE_FAIL_VIEW;
            if (mItemStatus == STATUS_LOAD_NO_MORE && mLoadNoMoreView != null) return LOAD_NO_MORE_VIEW;
            if (mItemStatus == STATUS_LOADING && mLoadingView != null) return LOADING_VIEW;
        }
        return getDefItemViewType(position - getHeaderViewCount());
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, final int viewType) {
        final VH viewHolder;
        mContext = parent.getContext();
        mLayoutInflater = LayoutInflater.from(mContext);
        switch (viewType) {
            case HEADER_VIEW:
                viewHolder = createViewHolder(mHeaderView);
                break;
            case FOOTER_VIEW:
                viewHolder = createViewHolder(mFooterView);
                break;
            case EMPTY_VIEW:
                viewHolder = createViewHolder(mEmptyView);
                break;
            case ERROR_VIEW:
                viewHolder = createViewHolder(mErrorView);
                break;
            case LOADING_VIEW:
                viewHolder = createViewHolder(mLoadingView);
                break;
            case LOAD_MORE_FAIL_VIEW:
                viewHolder = createViewHolder(mLoadMoreFailedView);
                break;
            case LOAD_NO_MORE_VIEW:
                viewHolder = createViewHolder(mLoadNoMoreView);
                break;
            default:
                viewHolder = createDefViewHolder(parent, viewType);
        }
        return viewHolder;
    }

    protected VH createViewHolder(View view) {
        return (VH) new BaseViewHolder(view);
    }

    protected VH createDefViewHolder(ViewGroup parent, int viewType) {
        return createBaseViewHolder(parent, mLayoutResId);
    }

    protected VH createBaseViewHolder(ViewGroup parent, int layoutResId) {
        return createViewHolder(getItemView(layoutResId, parent));
    }

    protected int getDefItemViewType(int position) {
        return ITEM_VIEW;
    }

    @Override
    public void onBindViewHolder(VH holder, int positions) {
        switch (holder.getItemViewType()) {
            case LOADING_VIEW:
                addLoadMore();
                break;
            case HEADER_VIEW:
            case FOOTER_VIEW:
            case EMPTY_VIEW:
            case ERROR_VIEW:
            case LOAD_MORE_FAIL_VIEW:
            case LOAD_NO_MORE_VIEW:
                break;
            default:
                int pos = holder.getLayoutPosition() - getHeaderViewCount();
                convert(holder, mDatas.get(pos));
                break;
        }
    }

    protected abstract void convert(VH helper, T item);

    public void setOnLoadMoreListener(LoadMoreListener loadMoreListener) {
        mLoadMoreListener = loadMoreListener;
    }

    public void openLoadMore(int pageSize) {
        mPageSize = pageSize;
        mIsOpenLoadEnable = true;
    }

    public void remove(int position) {
        mDatas.remove(position);
        notifyItemRemoved(position + getHeaderViewCount());
    }

    public void add(int position, T item) {
        mDatas.add(position, item);
        notifyItemInserted(position);
    }

    public void setNewData(List<T> data) {
        mDatas = data == null ? new ArrayList<T>() : data;
        if (mLoadMoreListener != null) {
            mIsOpenLoadEnable = true;
        }
        if (mLoadMoreFailedView != null) {
            removeFooterView(mLoadMoreFailedView);
        }
        mLastAddedAnimPos = -1;
        notifyDataSetChanged();
    }

    public void addData(int position, T data) {
        if (0 <= position && position < mDatas.size()) {
            mDatas.add(position, data);
            notifyItemInserted(position);
            notifyItemRangeChanged(position, mDatas.size() - position);
        }
    }

    public void addData(T data) {
        mDatas.add(data);
        notifyItemInserted(mDatas.size() - 1);
    }

    public void addData(int position, List<T> data) {
        if (0 <= position && position < mDatas.size()) {
            mDatas.addAll(position, data);
            notifyItemInserted(position);
            notifyItemRangeChanged(position, mDatas.size() - position - data.size());
        }
    }

    public void addData(List<T> newData) {
        mDatas.addAll(newData);
        if (mIsOpenLoadEnable) mItemStatus = STATUS_NORMAL;
        notifyItemRangeInserted(mDatas.size() - newData.size() + getHeaderViewCount(), newData.size());
    }

    public void setLoadingView(View loadingView) {
        mLoadingView = loadingView;
    }

    public List<T> getData() {
        return mDatas;
    }

    public T getItem(int position) {
        return mDatas.get(position);
    }

    public int getHeaderViewCount() {
        return mHeaderView == null ? 0 : 1;
    }

    public int getFooterLayoutCount() {
        return mFooterView == null ? 0 : 1;
    }

    private void addLoadMore() {
        if (isLoadMore() && mItemStatus == STATUS_LOADING) {
            mLoadMoreListener.onLoadMoreRequested();
        }
    }

    private boolean isLoadMore() {
        return mIsOpenLoadEnable && mLoadMoreListener != null && mDatas.size() >= mPageSize;
    }

    public LinearLayout getHeaderView() {
        return mHeaderView;
    }

    public void addHeaderView(View header) {
        addHeaderView(header, -1);
    }

    public void addHeaderView(View header, int index) {
        if (mHeaderView == null) {
            mHeaderView = new LinearLayout(header.getContext());
            mHeaderView.setOrientation(LinearLayout.VERTICAL);
            mHeaderView.setLayoutParams(new LayoutParams(MATCH_PARENT, WRAP_CONTENT));
        }
        index = index >= mHeaderView.getChildCount() ? -1 : index;
        mHeaderView.addView(header, index);
        notifyDataSetChanged();
    }

    public void removeHeaderView(View header) {
        if (mHeaderView == null) return;
        mHeaderView.removeView(header);
        if (mHeaderView.getChildCount() == 0) mHeaderView = null;
        notifyItemChanged(0);
    }

    public void removeAllHeaderView() {
        if (mHeaderView == null) return;
        mHeaderView.removeAllViews();
        mHeaderView = null;
        notifyItemChanged(0);
    }

    public LinearLayout getFooterView() {
        return mFooterView;
    }

    public void addFooterView(View footer) {
        addFooterView(footer, -1);
    }

    public void addFooterView(View footer, int index) {
        if (footer == null) return;
        if (mFooterView == null) {
            mFooterView = new LinearLayout(footer.getContext());
            mFooterView.setOrientation(LinearLayout.VERTICAL);
            mFooterView.setLayoutParams(new LayoutParams(MATCH_PARENT, WRAP_CONTENT));
        }
        index = index >= mFooterView.getChildCount() ? -1 : index;
        mFooterView.addView(footer, index);
        notifyItemChanged(getItemCount());
    }

    public void removeFooterView(View footer) {
        if (mFooterView == null) return;
        mFooterView.removeView(footer);
        if (mFooterView.getChildCount() == 0) mFooterView = null;
        notifyItemChanged(mDatas.size());
    }

    public void removeAllFooterView() {
        if (mFooterView == null) return;
        mFooterView.removeAllViews();
        mFooterView = null;
        notifyItemChanged(mDatas.size());
    }

    public void setLoadMoreFailedView(View view) {
        mLoadMoreFailedView = view;
    }

    public View getEmptyView() {
        return mEmptyView;
    }

    public void setEmptyView(View emptyView) {
        mEmptyView = emptyView;
    }

    public void setLoadNoMoreView(View loadNoMoreView) {
        mLoadNoMoreView = loadNoMoreView;
    }

    public void setErrorView(View errorView) {
        mErrorView = errorView;
    }

    public void notifyAllComplete(int itemStatus) {
        mIsOpenLoadEnable = false;
        mItemStatus = itemStatus;
        notifyDataSetChanged();
    }

    public void notifyLastComplete(int itemStatus) {
        mIsOpenLoadEnable = false;
        mItemStatus = itemStatus;
        notifyItemChanged(getItemCount());
    }

    public void loadSuccess(boolean isRefresh, List<T> datas) {
        if (datas == null) datas = new ArrayList<>();
        if (datas.size() != 0) mItemStatus = STATUS_NORMAL;
        if (isRefresh) {
            if (datas.size() == 0) {
                mItemStatus = STATUS_EMPTY;
            } else if (datas.size() < mPageSize) {
                mIsOpenLoadEnable = false;
                mItemStatus = STATUS_LOAD_NO_MORE;
            }
            setNewData(datas);
        } else {
            if (datas.size() == 0) {
                notifyLastComplete(STATUS_LOAD_NO_MORE);
            } else {
                addData(datas);
            }
        }
    }

    public void loadOnError() {
        mDatas.clear();
        notifyAllComplete(STATUS_ERROR);
    }

    public void showLoadMoreFailedView() {
        notifyLastComplete(STATUS_LOAD_MORE_FAIL);
    }

    public void showLoading() {
        mItemStatus = STATUS_LOADING;
        mIsOpenLoadEnable = true;
        notifyItemRemoved(getItemCount());
        notifyItemInserted(getItemCount());
        mRecyclerView.smoothScrollToPosition(getItemCount() - 1);
    }

    public void resetItemStatus() {
        mItemStatus = STATUS_NORMAL;
        mIsOpenLoadEnable = true;
        notifyItemRemoved(getItemCount());
    }

    protected View getItemView(int layoutResId, ViewGroup parent) {
        return mLayoutInflater.inflate(layoutResId, parent, false);
    }

    public void openItemAnimation() {
        openItemAnimation(new AlphaInAnimation());
    }

    public void openItemAnimation(BaseAnimation animation) {
        mOpenAnimationEnable = true;
        mItemAnimation = animation;
    }

}