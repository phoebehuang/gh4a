/*
 * Copyright 2011 Azwan Adli Abdullah
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh4a;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.github.core.User;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.actionbarsherlock.app.ActionBar;
import com.gh4a.adapter.UserAdapter;
import com.gh4a.utils.StringUtils;

public class UserListActivity extends BaseActivity implements OnItemClickListener {

    protected String mSearchKey;
    protected UserAdapter mUserAdapter;
    protected ListView mListViewUsers;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.generic_list);

        setRequestData();
        
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getTitleBar());
        actionBar.setSubtitle(getSubTitle());
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        
        mListViewUsers = (ListView) findViewById(R.id.list_view);
        mListViewUsers.setOnItemClickListener(this);

        mUserAdapter = new UserAdapter(this, new ArrayList<User>(), getRowLayout(), getShowExtraData());
        mListViewUsers.setAdapter(mUserAdapter);

        new LoadUserListTask(this).execute();
    }

    private static class LoadUserListTask extends AsyncTask<Void, Integer, List<User>> {

        private WeakReference<UserListActivity> mTarget;
        private boolean mException;

        public LoadUserListTask(UserListActivity activity) {
            mTarget = new WeakReference<UserListActivity>(activity);
        }

        @Override
        protected List<User> doInBackground(Void... params) {
            if (mTarget.get() != null) {
                try {
                    return mTarget.get().getUsers();
                }
                catch (IOException e) {
                    Log.e(Constants.LOG_TAG, e.getMessage(), e);
                    mException = true;
                    return null;
                }
            }
            else {
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onPostExecute(List<User> result) {
            if (mTarget.get() != null) {
                if (mException) {
                    mTarget.get().showError();
                }
                else {
                    mTarget.get().fillData(result);
                }
            }
        }
    }

    protected void fillData(List<User> users) {
        if (users != null && users.size() > 0) {
            mUserAdapter.notifyDataSetChanged();
            for (User user : users) {
                mUserAdapter.add(user);
            }
        }
        mUserAdapter.notifyDataSetChanged();
    }

    protected List<User> getUsers() throws IOException {
        return new ArrayList<User>();
    }

    protected void setRequestData() {
        mSearchKey = getIntent().getExtras().getString("searchKey");
    }

    protected int getRowLayout() {
        return R.layout.row_gravatar_1;
    }

    protected String getTitleBar() {
        return null;
    }
    
    protected String getSubTitle() {
        return null;
    }
    
    protected boolean getShowExtraData() {
        return true;
    }
    
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        User user = (User) adapterView.getAdapter().getItem(position);
        if (!StringUtils.isBlank(user.getLogin())) {
            Intent intent = new Intent().setClass(UserListActivity.this, UserActivity.class);
            intent.putExtra(Constants.User.USER_LOGIN, (String) user.getLogin());
            intent.putExtra(Constants.User.USER_NAME, (String) user.getName());
            startActivity(intent);
        }
    }
}