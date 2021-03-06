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
import java.util.List;

import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.RepositoryTag;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.RepositoryService;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.gh4a.adapter.TagAdapter;
import com.gh4a.db.Bookmark;
import com.gh4a.db.BookmarkParam;
import com.gh4a.db.DbHelper;

/**
 * The TagList activity.
 */
public class TagListActivity extends BaseActivity {

    /** The user login. */
    protected String mUserLogin;

    /** The repo name. */
    protected String mRepoName;

    /** The loading dialog. */
    protected LoadingDialog mLoadingDialog;

    /**
     * Called when the activity is first created.
     * 
     * @param savedInstanceState the saved instance state
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.branch_tag_list);
        setUpActionBar();

        Bundle data = getIntent().getExtras();
        mUserLogin = data.getString(Constants.Repository.REPO_OWNER);
        mRepoName = data.getString(Constants.Repository.REPO_NAME);

        new LoadBranchTagListTask(this).execute();
    }

    /**
     * An asynchronous task that runs on a background thread
     * to load branch tag list.
     */
    private static class LoadBranchTagListTask extends
            AsyncTask<Void, Integer, List<RepositoryTag>> {

        /** The target. */
        private WeakReference<TagListActivity> mTarget;

        /** The exception. */
        private boolean mException;

        /**
         * Instantiates a new load tree list task.
         * 
         * @param activity the activity
         */
        public LoadBranchTagListTask(TagListActivity activity) {
            mTarget = new WeakReference<TagListActivity>(activity);
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected List<RepositoryTag> doInBackground(Void... params) {
            if (mTarget.get() != null) {
                try {
                    GitHubClient client = new GitHubClient();
                    client.setOAuth2Token(mTarget.get().getAuthToken());
                    RepositoryService repoService = new RepositoryService(client);
                    return repoService.getTags(new RepositoryId(mTarget.get().mUserLogin, 
                            mTarget.get().mRepoName));
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

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            if (mTarget.get() != null) {
                mTarget.get().mLoadingDialog = LoadingDialog.show(mTarget.get(), true, true);
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(List<RepositoryTag> result) {
            if (mTarget.get() != null) {
                if (mException) {
                    mTarget.get().showError();
                }
                else {
                    mTarget.get().fillData(result);
                    mTarget.get().mLoadingDialog.dismiss();
                }
            }
        }
    }

    /**
     * Fill data into UI components.
     *
     * @param map the map
     */
    protected void fillData(List<RepositoryTag> tags) {
        ListView branchListView = (ListView) findViewById(R.id.list_view);

        if (tags.size() == 0) {
            getApplicationContext().notFoundMessage(this, "Tags");
            return;
        }

        TagAdapter tagAdapter = new TagAdapter(this, tags, mUserLogin, mRepoName);
        branchListView.setAdapter(tagAdapter);
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (isAuthorized()) {
            menu.clear();
            MenuInflater inflater = getSupportMenuInflater();
            inflater.inflate(R.menu.bookmark_menu, menu);
        }
        return true;
    }
    
    @Override
    public void openBookmarkActivity() {
        Intent intent = new Intent().setClass(this, BookmarkListActivity.class);
        intent.putExtra(Constants.Bookmark.NAME, "Tags at " + mUserLogin + "/" + mRepoName);
        intent.putExtra(Constants.Bookmark.OBJECT_TYPE, Constants.Bookmark.OBJECT_TYPE_REPO);
        startActivityForResult(intent, 100);
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 100) {
           if (resultCode == Constants.Bookmark.ADD) {
               DbHelper db = new DbHelper(this);
               Bookmark b = new Bookmark();
               b.setName("Tags at " + mUserLogin + "/" + mRepoName);
               b.setObjectType(Constants.Bookmark.OBJECT_TYPE_REPO);
               b.setObjectClass(TagListActivity.class.getName());
               long id = db.saveBookmark(b);
               
               BookmarkParam[] params = new BookmarkParam[2];
               BookmarkParam param = new BookmarkParam();
               param.setBookmarkId(id);
               param.setKey(Constants.Repository.REPO_OWNER);
               param.setValue(mUserLogin);
               params[0] = param;
               
               param = new BookmarkParam();
               param.setBookmarkId(id);
               param.setKey(Constants.Repository.REPO_NAME);
               param.setValue(mRepoName);
               params[1] = param;
               
               db.saveBookmarkParam(params);
           }
        }
     }

}
