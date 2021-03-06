package com.kidozh.discuzhub.activities.ui.HotThreads;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.kidozh.discuzhub.R;
import com.kidozh.discuzhub.activities.ui.DashBoard.DashBoardViewModel;
import com.kidozh.discuzhub.adapter.NetworkIndicatorAdapter;
import com.kidozh.discuzhub.adapter.ThreadAdapter;
import com.kidozh.discuzhub.databinding.FragmentHotThreadBinding;
import com.kidozh.discuzhub.entities.Discuz;
import com.kidozh.discuzhub.entities.ErrorMessage;
import com.kidozh.discuzhub.entities.User;
import com.kidozh.discuzhub.entities.Thread;
import com.kidozh.discuzhub.interact.BaseStatusInteract;
import com.kidozh.discuzhub.utilities.AnimationUtils;
import com.kidozh.discuzhub.utilities.ConstUtils;

import java.util.List;


public class HotThreadsFragment extends Fragment {
    private static final String TAG = HotThreadsFragment.class.getSimpleName();
    @NonNull
    private HotThreadsViewModel hotThreadsViewModel;
    @NonNull
    private DashBoardViewModel dashBoardViewModel;
    FragmentHotThreadBinding binding;
    

    ThreadAdapter forumThreadAdapter;
    NetworkIndicatorAdapter networkIndicatorAdapter = new NetworkIndicatorAdapter();
    ConcatAdapter concatAdapter;
    Discuz bbsInfo;
    User userBriefInfo;


    public static HotThreadsFragment newInstance(@NonNull Discuz Discuz, User userBriefInfo){
        HotThreadsFragment fragment = new HotThreadsFragment();
        Bundle args = new Bundle();
        args.putSerializable(ConstUtils.PASS_BBS_ENTITY_KEY, Discuz);
        args.putSerializable(ConstUtils.PASS_BBS_USER_KEY,userBriefInfo);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            bbsInfo = (Discuz) getArguments().getSerializable(ConstUtils.PASS_BBS_ENTITY_KEY);
            userBriefInfo = (User)  getArguments().getSerializable(ConstUtils.PASS_BBS_USER_KEY);

        }
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHotThreadBinding.inflate(inflater,container,false);
        hotThreadsViewModel = new ViewModelProvider(this).get(HotThreadsViewModel.class);
        dashBoardViewModel = new ViewModelProvider(this).get(DashBoardViewModel.class);
        getIntentInfo();

        configureClient();

        configureThreadRecyclerview();
        configureSwipeRefreshLayout();
        bindVieModel();
        return binding.getRoot();
    }

    private void configureClient(){
        if(bbsInfo !=null){
            hotThreadsViewModel.setBBSInfo(bbsInfo,userBriefInfo);
        }

    }

    private void configureThreadRecyclerview(){
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        binding.fragmentHotThreadRecyclerview.setLayoutManager(linearLayoutManager);
        forumThreadAdapter = new ThreadAdapter(null,null,bbsInfo,userBriefInfo);
        forumThreadAdapter.ignoreDigestStyle = true;
        concatAdapter = new ConcatAdapter(forumThreadAdapter,networkIndicatorAdapter);
        binding.fragmentHotThreadRecyclerview.setAdapter(AnimationUtils.INSTANCE.getAnimatedAdapter(getContext(),concatAdapter));
        binding.fragmentHotThreadRecyclerview.setItemAnimator(AnimationUtils.INSTANCE.getRecyclerviewAnimation(getContext()));
        // binding.fragmentHotThreadRecyclerview.addItemDecoration(new DividerItemDecoration(getContext(),DividerItemDecoration.VERTICAL));
        binding.fragmentHotThreadRecyclerview.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if(!recyclerView.canScrollVertically(1) && newState == RecyclerView.SCROLL_STATE_IDLE){
                    if(hotThreadsViewModel.pageNum.getValue() == null){
                        hotThreadsViewModel.setPageNumAndFetchThread(1);
                    }
                    else {
                        hotThreadsViewModel.setPageNumAndFetchThread(hotThreadsViewModel.pageNum.getValue()+1);
                    }
                    // getPageInfo(globalPage);

                }
            }


        });
    }

    private void configureSwipeRefreshLayout(){
        binding.swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                hotThreadsViewModel.setPageNumAndFetchThread(1);
            }
        });
    }

    private void getIntentInfo(){
        hotThreadsViewModel.setBBSInfo(bbsInfo,userBriefInfo);
    }

    private void bindVieModel(){
        hotThreadsViewModel.getThreadListLiveData().observe(getViewLifecycleOwner(), new Observer<List<Thread>>() {
            @Override
            public void onChanged(List<Thread> threads) {
                int page = hotThreadsViewModel.pageNum.getValue();
                Log.d(TAG,"Recv list page "+page +" size : "+forumThreadAdapter.threadList.size());
                if(page == 1 && forumThreadAdapter.threadList.size() != 0){
                    Log.d(TAG,"Clear adapter list "+forumThreadAdapter.threadList.size());
                    forumThreadAdapter.clearList();
                }
                forumThreadAdapter.addThreadInfoList(threads,null);
                if(page == 1){
                    binding.fragmentHotThreadRecyclerview.scrollToPosition(0);
                }
                dashBoardViewModel.hotThreadCountMutableLiveData.postValue(forumThreadAdapter.getItemCount());
                if(forumThreadAdapter.threadList == null || forumThreadAdapter.threadList.size() == 0){
                    networkIndicatorAdapter.setErrorStatus(new ErrorMessage(getString(R.string.empty_result),
                            getString(R.string.empty_hot_threads),R.drawable.ic_empty_hot_thread_64px
                    ));
                }
                else {
                    
                }
            }
        });
        hotThreadsViewModel.isLoading.observe(getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if(aBoolean){
                    networkIndicatorAdapter.setLoadingStatus();
                }
                else {
                    networkIndicatorAdapter.setLoadSuccessfulStatus();
                }
                binding.swipeRefreshLayout.setRefreshing(aBoolean);
            }
        });
        hotThreadsViewModel.errorMessageMutableLiveData.observe(getViewLifecycleOwner(),errorMessage -> {
            if(errorMessage!=null){
                networkIndicatorAdapter.setErrorStatus(errorMessage);
            }
        });
        
        hotThreadsViewModel.resultMutableLiveData.observe(getViewLifecycleOwner(), displayThreadsResult -> {
            if(getContext() instanceof BaseStatusInteract && displayThreadsResult!=null){
                ((BaseStatusInteract) getContext()).setBaseResult(displayThreadsResult,
                        displayThreadsResult.forumVariables);
            }
        });

    }

}