package com.cocoonshu.cobox.preferencedlistview;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * A data loader for constract.json
 * @author Cocoonshu
 * @date 2016-11-09 16:37:45
 */
public class DataLoader extends AsyncTask<String, Integer, List> {

    public static final int      BUFFER_SIZE               = 1024;
    public static final String   DATA_FILE                 = "contract.json";
    public static final String   KEY_CONTRACT_LIST         = "contract_list";
    public static final String   KEY_LAYOUT_CONFIG         = "layout_config";
    public static final String   KEY_LAYOUT_PADDING_LEFT   = "padding_left_dp";
    public static final String   KEY_LAYOUT_PADDING_TOP    = "padding_top_dp";
    public static final String   KEY_LAYOUT_PADDING_RIGHT  = "padding_right_dp";
    public static final String   KEY_LAYOUT_PADDING_BOTTOM = "padding_bottom_dp";
    public static final String   KEY_TITLE_ITEM            = "title_item";

    public static class LayoutConfig {
        public Rect   padding  = new Rect();
        public String titleKey = null;
    }

    private Context                 mContext      = null;
    private SortedExpanableListView mHostView     = null;
    private List                    mContentData  = null;
    private LayoutConfig            mLayoutConfig = null;

    public DataLoader(Context context) {
        mContext = context;
    }

    public DataLoader setHostView(SortedExpanableListView hostView) {
        mHostView = hostView;
        return this;
    }

    @Override
    protected List doInBackground(String[] params) {
        if (mContext == null) {
            return null;
        }

        try {
            String     jsonString = getJsonStringFromAsset(mContext, DATA_FILE);
            JSONObject json       = new JSONObject(jsonString);

            // Read content
            {
                List      dataset          = new ArrayList<>();
                JSONArray jsonContractList = json.optJSONArray(KEY_CONTRACT_LIST);
                if (jsonContractList != null) {
                    int size = jsonContractList.length();
                    for (int i = 0; i < size; i++) {
                        JSONObject jsonContract = jsonContractList.optJSONObject(i);
                        if (jsonContract != null) {
                            HashMap<String, Object> node = new HashMap<>();
                            Iterator<String> keyIterator = jsonContract.keys();
                            while (keyIterator.hasNext()) {
                                String key = keyIterator.next();
                                Object value = jsonContract.opt(key);
                                node.put(key, value);
                            }
                            dataset.add(node);
                        }
                    }
                }
                mContentData = dataset;
            }

            // Read configuration
            {
                Resources    resources  = mContext.getResources();
                float        density    = resources.getDisplayMetrics().density;
                LayoutConfig config     = new LayoutConfig();
                JSONObject   jsonConfig = json.optJSONObject(KEY_LAYOUT_CONFIG);
                if (jsonConfig != null) {
                    if (jsonConfig.has(KEY_TITLE_ITEM)) {
                        config.titleKey = jsonConfig.optString(KEY_TITLE_ITEM);
                    }
                    if (jsonConfig.has(KEY_LAYOUT_PADDING_LEFT)) {
                        config.padding.left = (int) (jsonConfig.optInt(KEY_LAYOUT_PADDING_LEFT, 0) * density);
                    }
                    if (jsonConfig.has(KEY_LAYOUT_PADDING_TOP)) {
                        config.padding.top = (int) (jsonConfig.optInt(KEY_LAYOUT_PADDING_TOP, 0) * density);
                    }
                    if (jsonConfig.has(KEY_LAYOUT_PADDING_RIGHT)) {
                        config.padding.right = (int) (jsonConfig.optInt(KEY_LAYOUT_PADDING_RIGHT, 0) * density);
                    }
                    if (jsonConfig.has(KEY_LAYOUT_PADDING_BOTTOM)) {
                        config.padding.bottom = (int) (jsonConfig.optInt(KEY_LAYOUT_PADDING_BOTTOM, 0) * density);
                    }
                }
                mLayoutConfig = config;
            }

        } catch (JSONException exp) {
            return null;
        } catch (IOException exp) {
            return null;
        } finally {
            return null;
        }
    }

    private String getJsonStringFromAsset(Context context, String fileName) throws IOException {
        AssetManager  assetManager = context.getAssets();
        InputStream   fileStream   = assetManager.open(fileName);
        StringBuilder builder      = new StringBuilder();

        int    readCount = 0;
        byte[] buffer    = new byte[BUFFER_SIZE];
        do {
            readCount = fileStream.read(buffer);
            if (readCount > 0) {
                builder.append(new String(buffer, 0, readCount));
            }
        } while(readCount > -1);

        return builder.toString();
    }

    @Override
    protected void onPostExecute(List nullArg) {
        if (mHostView != null && !isCancelled()) {
            mHostView.setDataSource(mContentData)
                     .setConfigSet(mLayoutConfig)
                     .updateListViewItems();
        }
    }
}
