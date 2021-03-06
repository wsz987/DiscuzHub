package com.kidozh.discuzhub.activities.ui.bbsDetailedInformation;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.kidozh.discuzhub.database.UserDatabase;
import com.kidozh.discuzhub.entities.User;

import java.util.List;

public class bbsShowInformationViewModel extends AndroidViewModel {
    public LiveData<List<User>> bbsUserInfoLiveDataList;

    public bbsShowInformationViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<List<User>> getBbsUserInfoLiveDataList() {

        return bbsUserInfoLiveDataList;
    }

    public void setBbsUserInfoLiveDataList(LiveData<List<User>> bbsUserInfoLiveDataList) {
        this.bbsUserInfoLiveDataList = bbsUserInfoLiveDataList;
    }

    public void loadUserList(int bbs_id){
        bbsUserInfoLiveDataList = UserDatabase.getInstance(getApplication())
                .getforumUserBriefInfoDao()
                .getAllUserByBBSID(bbs_id);

    }
}
