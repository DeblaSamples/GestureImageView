package com.cocoonshu.cobox.preferencedlistview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Preferenced ListView
 * @author Cocoonshu
 * @date 2016-11-09 15:55:52
 */
public class SortedExpanableListView extends ExpandableListView {

    private PreferencedAdapter mAdapter = null;

    public SortedExpanableListView(Context context) {
        this(context, null);
    }

    public SortedExpanableListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SortedExpanableListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setupComponents(context);
    }

    public SortedExpanableListView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setupComponents(context);
    }

    private void setupComponents(Context context) {
        mAdapter = new PreferencedAdapter(context);
        setAdapter(mAdapter);
    }

    public SortedExpanableListView setDataSource(List<HashMap<String, Object>> dataset) {
        mAdapter.setDataSet(dataset);
        return this;
    }

    public SortedExpanableListView setConfigSet(DataLoader.LayoutConfig configSet) {
        mAdapter.setConfigSet(configSet);
        return this;
    }

    public void updateListViewItems() {
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Preferenced Adapter
     * @author Cocoonshu
     * @date 2016-11-09 15:55:52
     */
    private class PreferencedAdapter extends BaseExpandableListAdapter {

        public static final int DefaultSubItemIndentDp = 30;

        private Context                       mContext       = null;
        private List<HashMap<String, Object>> mDataSet       = null;
        private DataLoader.LayoutConfig       mConfigSet     = null;
        private int                           mSubItemIndent = 0;

        public PreferencedAdapter(Context context) {
            mContext       = context;
            mDataSet       = new ArrayList<HashMap<String, Object>>();
            mSubItemIndent = (int) mContext.getResources().getDisplayMetrics().density * DefaultSubItemIndentDp;
        }

        public void setDataSet(List<HashMap<String, Object>> dataSet) {
            mDataSet = dataSet;
        }

        public void setConfigSet(DataLoader.LayoutConfig configSet) {
            mConfigSet = configSet;
        }

        @Override
        public int getGroupCount() {
            return mDataSet == null ? 0 : mDataSet.size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            if (mDataSet == null) {
                return 0;
            } else {
                HashMap map = mDataSet.get(groupPosition);
                if (map != null) {
                    return map.keySet().size();
                } else {
                    return 0;
                }
            }
        }

        @Override
        public Object getGroup(int groupPosition) {
            return mDataSet == null ? null : mDataSet.get(groupPosition);
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            if (mDataSet != null) {
                HashMap<String, Object> map = mDataSet.get(groupPosition);
                if (map != null) {
                    synchronized (map) {
                        int counter = 0;
                        Set<String> keyset = map.keySet();
                        for (String key : keyset) {
                            if (counter == childPosition) {
                                return map.get(key);
                            } else {
                                counter++;
                            }
                        }
                    }
                }
            }
            return null;
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return (groupPosition << 32) | childPosition;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            DataLoader.LayoutConfig config    = mConfigSet;
            TextView                groupView = null;
            if (convertView != null) {
                groupView = (TextView) convertView;
            } else {
                groupView = new TextView(mContext);
                groupView.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
                groupView.setTextColor(0xFFFFFFFF);
                convertView = groupView;

                if (config != null) {
                    groupView.setPadding(
                            config.padding.left, config.padding.top,
                            config.padding.right, config.padding.bottom);
                } else {
                    groupView.setPadding(0, 0, 0, 0);
                }
            }

            if (config.titleKey != null) {
                HashMap<String, Object> map = (HashMap) getGroup(groupPosition);
                Object value = map.get(config.titleKey);
                if (value != null && value instanceof String) {
                    groupView.setText((String) value);
                } else {
                    groupView.setText(String.format("#%d", groupPosition));
                }
            } else {
                groupView.setText(String.format("#%d", groupPosition));
            }

            return convertView;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            DataLoader.LayoutConfig config    = mConfigSet;
            TextView                childView = null;
            if (convertView != null) {
                childView = (TextView) convertView;
            } else {
                childView = new TextView(mContext);
                childView.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
                childView.setTextColor(0xFFFFFFFF);
                convertView = childView;

                if (config != null) {
                    childView.setPadding(
                            config.padding.left + mSubItemIndent, config.padding.top,
                            config.padding.right, config.padding.bottom);
                } else {
                    childView.setPadding(mSubItemIndent, 0, 0, 0);
                }
            }

            Object value = getChild(groupPosition, childPosition);
            if (value == null) {
                childView.setText("");
            } else {
                childView.setText(value.toString());
            }

            return convertView;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return false;
        }

    }

}
