package com.cocoonshu.coview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.cocoonshu.cobox.gestureimageview.R;
import com.cocoonshu.cobox.preferencedlistview.DataLoader;
import com.cocoonshu.cobox.preferencedlistview.SortedExpanableListView;

public class ExpendedListActivity extends AppCompatActivity {

    private SortedExpanableListView mLstContract = null;
    private DataLoader              mDataLoader  = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expended_list);

        findViews();
        fillData();
    }

    private void findViews() {
        mLstContract = (SortedExpanableListView) findViewById(R.id.ExpandableListView_Contract);
    }

    private void fillData() {
        if (mDataLoader != null) {
            mDataLoader.cancel(false);
        }
        mDataLoader = new DataLoader(getApplicationContext());
        mDataLoader.setHostView(mLstContract).execute();
    }

}
