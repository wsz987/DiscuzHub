package com.kidozh.discuzhub.activities.ui.HotThreads;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.kidozh.discuzhub.R;
import com.kidozh.discuzhub.entities.Discuz;
import com.kidozh.discuzhub.entities.ErrorMessage;
import com.kidozh.discuzhub.entities.User;
import com.kidozh.discuzhub.entities.Thread;
import com.kidozh.discuzhub.results.DisplayThreadsResult;
import com.kidozh.discuzhub.services.DiscuzApiService;
import com.kidozh.discuzhub.utilities.URLUtils;
import com.kidozh.discuzhub.utilities.NetworkUtils;

import java.util.ArrayList;
import java.util.List;


import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;

public class HotThreadsViewModel extends AndroidViewModel {
    private String TAG = HotThreadsViewModel.class.getSimpleName();
    private MutableLiveData<String> mText;

    Discuz bbsInfo;
    User userBriefInfo;
    private OkHttpClient client = new OkHttpClient();
    @NonNull
    public MutableLiveData<Integer> pageNum = new MutableLiveData<Integer>(1);
    public MutableLiveData<Boolean> isLoading;
    public MutableLiveData<List<Thread>> threadListLiveData;
    public MutableLiveData<ErrorMessage> errorMessageMutableLiveData = new MutableLiveData<>(null);;
    public MutableLiveData<DisplayThreadsResult> resultMutableLiveData = new MutableLiveData<>();




    public HotThreadsViewModel(Application application) {
        super(application);
        isLoading = new MutableLiveData<Boolean>();
        isLoading.postValue(false);

    }

    public void setBBSInfo(@NonNull Discuz bbsInfo, User userBriefInfo){
        this.bbsInfo = bbsInfo;
        this.userBriefInfo = userBriefInfo;
        URLUtils.setBBS(bbsInfo);
        client = NetworkUtils.getPreferredClientWithCookieJarByUser(getApplication(),userBriefInfo);
    }



    public LiveData<List<Thread>> getThreadListLiveData() {
        if(threadListLiveData == null){
            threadListLiveData = new MutableLiveData<>(new ArrayList<>());
            getThreadList(pageNum.getValue() == null ? 1 : pageNum.getValue());
        }
        return threadListLiveData;
    }

    public void setPageNumAndFetchThread(int page){
        pageNum.setValue(page);
        getThreadList(page);
    }

    private void getThreadList(int page){
        // init page
        if(!NetworkUtils.isOnline(getApplication())){
            isLoading.postValue(false);
            errorMessageMutableLiveData.postValue(NetworkUtils.getOfflineErrorMessage(getApplication()));

            return;
        }
        isLoading.postValue(true);

        Retrofit retrofit = NetworkUtils.getRetrofitInstance(bbsInfo.base_url,client);
        DiscuzApiService service = retrofit.create(DiscuzApiService.class);
        Call<DisplayThreadsResult> displayThreadsResultCall = service.hotThreadResult(page);
        Log.d(TAG,"Get hot thread page "+page+" url "+displayThreadsResultCall.request().url().toString());
        displayThreadsResultCall.enqueue(new Callback<DisplayThreadsResult>() {
            @Override
            public void onResponse(Call<DisplayThreadsResult> call, retrofit2.Response<DisplayThreadsResult> response) {
                if(response.isSuccessful() && response.body()!=null){
                    isLoading.postValue(false);
                    DisplayThreadsResult threadsResult = response.body();
                    resultMutableLiveData.postValue(threadsResult);

                    List<Thread> currentThread = new ArrayList<>();

                    if(threadsResult.forumVariables !=null){
                        List<Thread> threads = threadsResult.forumVariables.forumThreadList;
                        if(threads != null){
                            currentThread.addAll(threads);
                        }
                        errorMessageMutableLiveData.postValue(null);
                    }
                    else {

                        if(threadsResult.message !=null){
                            errorMessageMutableLiveData.postValue(threadsResult.message.toErrorMessage());

                        }
                        else if(threadsResult.error.length()!=0){
                            errorMessageMutableLiveData.postValue(new ErrorMessage(
                                    getApplication().getString(R.string.discuz_api_error),
                                    threadsResult.error
                            ));

                        }
                        else {
                            errorMessageMutableLiveData.postValue(new ErrorMessage(
                                    getApplication().getString(R.string.empty_result),
                                    getApplication().getString(R.string.discuz_network_result_null)
                            ));
                        }

                        if(page != 1){
                            // not at initial state
                            pageNum.postValue(pageNum.getValue() == null ?1:pageNum.getValue()-1);
                        }
                    }

                    threadListLiveData.postValue(currentThread);
                }
                else {
                    errorMessageMutableLiveData.postValue(new ErrorMessage(String.valueOf(response.code()),
                            getApplication().getString(R.string.discuz_network_unsuccessful,response.message())));
                }

            }

            @Override
            public void onFailure(Call<DisplayThreadsResult> call, Throwable t) {
                isLoading.postValue(false);
                errorMessageMutableLiveData.postValue(new ErrorMessage(
                        getApplication().getString(R.string.discuz_network_failure_template),
                        t.getLocalizedMessage() == null?t.toString():t.getLocalizedMessage()
                ));

            }
        });


    }
}