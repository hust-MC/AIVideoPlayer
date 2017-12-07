package com.baidu.smartvideoplayer;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileManagerActivity extends ListActivity {
    private List<Map<String, Object>> mData;
    private final String ROOT_DIRECTORY = Environment.getExternalStorageDirectory().getPath();

    private String mDir;
    MyAdapter adapter;

    public static final String RESULT_FILE_PATH = "filepath";
    public static final String RESULT_FILE_NAME = "filename";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getListView().setBackgroundColor(getResources().getColor(android.R.color.background_dark));

        mDir = getIntent().getStringExtra("firstDir");
        if (mDir == null)
            mDir = ROOT_DIRECTORY;

        adapter = new MyAdapter(this);
        mData = getData();

        setListAdapter(adapter);
        registerForContextMenu(getListView());            // 注册上下文菜单
        WindowManager m = getWindowManager();
        Display d = m.getDefaultDisplay();
        LayoutParams p = getWindow().getAttributes();
        p.height = (int) (d.getHeight() * 0.8);
        p.width = (int) (d.getWidth() * 0.95);
        getWindow().setAttributes(p);
    }


    private List<Map<String, Object>> getData() {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        Map<String, Object> map = null;

        File f = new File(mDir);
        if (!f.exists()) {
            Toast.makeText(FileManagerActivity.this, "U盘设备无法识别", Toast.LENGTH_SHORT).show();
            return list;
        }

        File[] files = f.listFiles();
        if (!mDir.equals(ROOT_DIRECTORY)) {
            map = new HashMap<String, Object>();
            map.put("title", "..");
            map.put("info", f.getParent());
            map.put("img", R.mipmap.folder);
            list.add(map);
        }
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                map = new HashMap<String, Object>();
                map.put("title", files[i].getName());
                map.put("info", files[i].getPath());
                if (files[i].isDirectory())
                    map.put("img", R.mipmap.folder);
                else
                    map.put("img", R.mipmap.doc);
                list.add(map);
            }
        }
        return list;
    }

    /**
     * 刷新列表数据
     */
    private void refreshListView() {
        mData = getData();
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        if ((Integer) mData.get(position).get("img") == R.mipmap.folder)        // Object对象只能转为Integer
        {
            mDir = (String) mData.get(position).get("info");
            refreshListView();
        } else {
            finishWithResult((String) mData.get(position).get("info"), (String) mData.get(position).get("title"));
        }
    }

    public class MyAdapter extends BaseAdapter {
        private LayoutInflater mInflater;

        public MyAdapter(Context context) {
            this.mInflater = LayoutInflater.from(context);
        }

        public int getCount() {
            return mData.size();
        }

        public Object getItem(int arg0) {
            return null;
        }

        public long getItemId(int arg0) {
            return 0;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = mInflater.inflate(R.layout.list_view, null);
                holder.img = convertView.findViewById(R.id.img);
                holder.title = convertView.findViewById(R.id.title);
                holder.info = convertView.findViewById(R.id.info);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.img.setBackgroundResource((Integer) mData.get(position).get(
                    "img"));
            holder.title.setText((String) mData.get(position).get("title"));
            holder.info.setText((String) mData.get(position).get("info"));
            return convertView;
        }
    }

    public final class ViewHolder {
        public ImageView img;
        public TextView title;
        public TextView info;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        int menuOrder = Menu.FIRST;
        menu.setHeaderTitle("文件操作");
        menu.add(0, 1, menuOrder++, "新建文件夹");
        if (((AdapterContextMenuInfo) menuInfo).position == 0
                && mDir != ROOT_DIRECTORY) {
            return;
        }
        menu.add(0, 2, menuOrder++, "重命名");
        menu.add(0, 3, menuOrder, "删除");
    }

    private void finishWithResult(String path, String name) {
        Intent intent = new Intent();
        intent.putExtra(RESULT_FILE_PATH, path);
        intent.putExtra(RESULT_FILE_NAME, name);
        setResult(RESULT_OK, intent);
        finish();
    }
}