package com.kyletung.simplebookmovie.ui.search;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.kyletung.simplebookmovie.R;
import com.kyletung.simplebookmovie.adapter.search.SearchBookAdapter;
import com.kyletung.simplebookmovie.client.request.BookClient;
import com.kyletung.simplebookmovie.client.IResponse;
import com.kyletung.simplebookmovie.data.book.BookSubject;
import com.kyletung.simplebookmovie.data.search.SearchBookData;
import com.kyletung.simplebookmovie.event.BaseEvent;
import com.kyletung.simplebookmovie.event.EventCode;
import com.kyletung.simplebookmovie.ui.BaseFragment;
import com.kyletung.simplebookmovie.ui.book.BookDetailActivity;
import com.kyletung.simplebookmovie.utils.BaseToast;
import com.kyletung.simplebookmovie.view.recycler.LinearOnScrollListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

/**
 * All rights reserved by Author<br>
 * Author: Dong YuHui<br>
 * Email: <a href="mailto:dyh920827@gmail.com">dyh920827@gmail.com</a><br>
 * Blog: <a href="http://www.kyletung.com">www.kyletung.com</a><br>
 * Create Time: 2016/07/14 at 20:52<br>
 * <br>
 * FixMe
 */
public class SearchBookFragment extends BaseFragment {

    private String mContent;
    private boolean mHasMore = true;

    private SearchBookAdapter mAdapter;
    private SwipeRefreshLayout mRefreshLayout;
    private LinearOnScrollListener mOnScrollListener;

    public static SearchBookFragment newInstance() {
        return new SearchBookFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        EventBus.getDefault().register(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected int getContentLayout() {
        return R.layout.layout_refresh_recycler;
    }

    @Override
    protected void init(View view) {
        // init views
        mRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.refresh);
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recycler);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);
        mAdapter = new SearchBookAdapter(getActivity(), R.layout.recycler_book_item, this);
        recyclerView.setAdapter(mAdapter);
        mOnScrollListener = new LinearOnScrollListener(layoutManager, mAdapter);
        recyclerView.addOnScrollListener(mOnScrollListener);
        // set listener
        setListener();
    }

    private void setListener() {
        mAdapter.setOnItemClickListener(new SearchBookAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position, String bookId) {
                Intent intent = new Intent(getActivity(), BookDetailActivity.class);
                intent.putExtra("userId", "");
                intent.putExtra("bookId", bookId);
                startActivity(intent);
            }
        });
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mHasMore = true;
                getData(mContent, 0);
            }
        });
        mOnScrollListener.setOnLoadMore(new LinearOnScrollListener.OnLoadMore() {
            @Override
            public void onLoadMore() {
                getData(mContent, mAdapter.getItemCount());
            }
        });
    }

    public void onBookSuccess(ArrayList<BookSubject> list) {
        mRefreshLayout.setRefreshing(false);
        mAdapter.putList(list);
    }

    public void onBookError(String error) {
        mRefreshLayout.setRefreshing(false);
        BaseToast.toast(getActivity(), error);
    }

    public void onMoreSuccess(ArrayList<BookSubject> list) {
        mOnScrollListener.loadComplete();
        mAdapter.addList(list);
        if (list == null || list.size() == 0) {
            mHasMore = false;
        }
    }

    public void onMoreError(String error) {
        mOnScrollListener.loadComplete();
        BaseToast.toast(getActivity(), error);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(BaseEvent event) {
        if (event.getWhat() == EventCode.WHAT_SEARCH && event.getCode() == EventCode.CODE_SEARCH_ALL) {
            mContent = (String) event.getObject();
            mRefreshLayout.setRefreshing(true);
            getData(mContent, 0);
            mHasMore = true;
        }
    }

    /**
     * 搜索书籍
     *
     * @param content 搜索内容
     * @param start   开始点
     */
    private void getData(String content, final int start) {
        if (!mHasMore) {
            if (start == 0) {
                mRefreshLayout.setRefreshing(false);
            } else {
                mOnScrollListener.loadComplete();
            }
            return;
        }
        BookClient.getInstance().getBookSearch(content, start, new IResponse<SearchBookData>() {

            @Override
            public void onResponse(SearchBookData result) {
                if (start == 0) {
                    onBookSuccess(result.getBooks());
                } else {
                    onMoreSuccess(result.getBooks());
                }
            }

            @Override
            public void onError(int code, String reason) {
                if (start == 0) {
                    onBookError(reason);
                } else {
                    onMoreError(reason);
                }
            }

        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

}
