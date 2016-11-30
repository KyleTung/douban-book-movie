package com.kyletung.simplebookmovie.ui.search;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.kyletung.commonlib.main.BaseFragment;
import com.kyletung.commonlib.utils.ToastUtil;
import com.kyletung.simplebookmovie.R;
import com.kyletung.simplebookmovie.adapter.movie.MovieTopAdapter;
import com.kyletung.simplebookmovie.client.request.MovieClient;
import com.kyletung.simplebookmovie.data.movie.MovieSubject;
import com.kyletung.simplebookmovie.event.BaseEvent;
import com.kyletung.simplebookmovie.event.EventCode;
import com.kyletung.simplebookmovie.ui.movie.MovieDetailActivity;
import com.kyletung.simplebookmovie.view.LinearOnScrollListener;

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
 * 搜索电影页面
 */
public class SearchMovieFragment extends BaseFragment {

    private String mContent;
    private boolean mHasMore = true;

    private MovieTopAdapter mAdapter;
    private SwipeRefreshLayout mRefreshLayout;
    private LinearOnScrollListener mOnScrollListener;

    public static SearchMovieFragment newInstance() {
        return new SearchMovieFragment();
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
    protected void initView(View view) {
        // init views
        mRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.refresh);
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recycler);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);
        mAdapter = new MovieTopAdapter(getActivity(), R.layout.recycler_movie_item, this);
        recyclerView.setAdapter(mAdapter);
        mOnScrollListener = new LinearOnScrollListener(layoutManager, mAdapter);
        recyclerView.addOnScrollListener(mOnScrollListener);
    }

    @Override
    protected void business(View view) {
        mAdapter.setOnItemClickListener((position, movieId) -> {
            Intent intent = new Intent(getActivity(), MovieDetailActivity.class);
            intent.putExtra("movieId", movieId);
            startActivity(intent);
        });
        mRefreshLayout.setOnRefreshListener(() -> {
            mHasMore = true;
            getData(mContent, 0);
        });
        mOnScrollListener.setOnLoadMore(() -> getData(mContent, mAdapter.getItemCount()));
    }

    public void onMovieSuccess(ArrayList<MovieSubject> list) {
        mRefreshLayout.setRefreshing(false);
        mAdapter.putList(list);
    }

    public void onMovieError(String error) {
        mRefreshLayout.setRefreshing(false);
        ToastUtil.showToast(getActivity(), error);
    }

    public void onMoreSuccess(ArrayList<MovieSubject> list) {
        mOnScrollListener.loadComplete();
        mAdapter.addList(list);
        if (list == null || list.size() == 0) mHasMore = false;
    }

    public void onMoreError(String error) {
        mOnScrollListener.loadComplete();
        ToastUtil.showToast(getActivity(), error);
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
     * 搜索影视
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
        MovieClient.getInstance().getMovieSearch(content, start).subscribe(newSubscriber(movieTopData -> {
            if (start == 0) {
                onMovieSuccess(movieTopData.getSubjects());
            } else {
                onMoreSuccess(movieTopData.getSubjects());
            }
        }, throwable -> {
            if (start == 0) {
                onMovieError(throwable.getMessage());
            } else {
                onMoreError(throwable.getMessage());
            }
        }));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

}
