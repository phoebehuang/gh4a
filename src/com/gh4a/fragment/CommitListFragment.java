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
package com.gh4a.fragment;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.service.CommitService;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockFragment;
import com.gh4a.CommitActivity;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.adapter.CommitAdapter;
import com.gh4a.loader.PageIteratorLoader;
import com.gh4a.utils.StringUtils;

public class CommitListFragment extends SherlockFragment 
    implements LoaderManager.LoaderCallbacks<List<RepositoryCommit>>, OnItemClickListener {

    private Repository mRepository;
    private String mRef;
    private ListView mListView;
    private CommitAdapter mAdapter;
    private PageIterator<RepositoryCommit> mDataIterator;
    
    public static CommitListFragment newInstance(Repository repository, String ref) {
        
        CommitListFragment f = new CommitListFragment();

        Bundle args = new Bundle();
        args.putString(Constants.Object.REF, ref);
        args.putSerializable("REPOSITORY", repository);
        f.setArguments(args);
        
        return f;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRepository = (Repository) getArguments().getSerializable("REPOSITORY");
        mRef = getArguments().getString(Constants.Object.REF);
        if (StringUtils.isBlank(mRef)) {
            mRef = mRepository.getMasterBranch();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.generic_list, container, false);
        return v;
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        mListView = (ListView) getView().findViewById(R.id.list_view);
        mAdapter = new CommitAdapter(getSherlockActivity(), new ArrayList<RepositoryCommit>());
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
        
        loadData();
        
        getLoaderManager().initLoader(0, null, this);
        getLoaderManager().getLoader(0).forceLoad();
    }
    
    public void loadData() {
        Gh4Application app = (Gh4Application) getSherlockActivity().getApplication();
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(app.getAuthToken());
        CommitService commitService = new CommitService(client);
        mDataIterator = commitService.pageCommits(new RepositoryId(mRepository.getOwner().getLogin(), mRepository.getName()), 
                mRef, null);
    }
    
    private void fillData(List<RepositoryCommit> commits) {
        if (commits != null && commits.size() > 0) {
            mAdapter.addAll(commits);
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public Loader<List<RepositoryCommit>> onCreateLoader(int id, Bundle args) {
        return new PageIteratorLoader<RepositoryCommit>(getSherlockActivity(), mDataIterator);
    }

    @Override
    public void onLoadFinished(Loader<List<RepositoryCommit>> loader, List<RepositoryCommit> commits) {
        fillData(commits);
    }

    @Override
    public void onLoaderReset(Loader<List<RepositoryCommit>> arg0) {
        // TODO Auto-generated method stub
    }
    
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        RepositoryCommit commit = (RepositoryCommit) adapterView.getAdapter().getItem(position);
        Intent intent = new Intent().setClass(getSherlockActivity(), CommitActivity.class);
        String[] urlPart = commit.getUrl().split("/");
        
        intent.putExtra(Constants.Repository.REPO_OWNER, urlPart[4]);
        intent.putExtra(Constants.Repository.REPO_NAME, urlPart[5]);
        intent.putExtra(Constants.Object.OBJECT_SHA, commit.getSha());
        intent.putExtra(Constants.Object.TREE_SHA, commit.getCommit().getTree().getSha());

        startActivity(intent);
    }
}