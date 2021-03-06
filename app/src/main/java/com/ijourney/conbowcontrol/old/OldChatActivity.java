package com.ijourney.conbowcontrol.old;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.util.ArraySet;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Toast;

import com.blankj.utilcode.util.StringUtils;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.ijourney.conbowcontrol.R;
import com.ijourney.conbowcontrol.adapter.GridFixedListAdapter;
import com.ijourney.conbowcontrol.adapter.GridListAdapter;
import com.ijourney.conbowcontrol.bean.FeaturesBean;
import com.ijourney.conbowcontrol.bean.FixedBean;
import com.ijourney.conbowcontrol.bean.SharedPreferencesUtils;
import com.ijourney.conbowcontrol.databinding.ActivityChatBinding;
import com.ijourney.conbowcontrol.view.ChatPresent;
import com.ijourney.conbowcontrol.view.IChatView;
import com.ijourney.conbowcontrol.view.MyGridView;
import com.ijourney.p2plib.connect.NearConnect;
import com.ijourney.p2plib.model.Host;

import org.litepal.crud.DataSupport;

import java.util.ArrayList;
import java.util.List;

public class OldChatActivity extends Activity implements IChatView, OnClickListener, MessageDialog.onMessageListener {
    private EditText ed_content;
    private MyGridView rv_list, rv_fixed_list;


    private ImageView img_tune_add;
    private ScrollView scrollView;

    public static final String BUNDLE_PARTICIPANT = "bundle_participant";
    public static final String MSG_PREFIX = "msg:";
    private static final String STATUS_EXIT_CHAT = "status:exit_chat";
    private ActivityChatBinding binding;
    private NearConnect mNearConnect;
    private Host mParticipant;


    private boolean discoveryStarted;
    private ChatPresent present;
    private BaseQuickAdapter<FeaturesBean, BaseViewHolder> adapter;
    private List<FeaturesBean> featuresBeans = new ArrayList<>();

    public static void start(Activity activity, Host participant) {
        Intent intent = new Intent(activity, OldChatActivity.class);
        intent.putExtra(BUNDLE_PARTICIPANT, participant);
        activity.startActivityForResult(intent, 1234);
    }


    private void initNearConnect() {
        ArraySet<Host> peers = new ArraySet<>();
        peers.add(mParticipant);
        mNearConnect = new NearConnect.Builder()
                .forPeers(peers)
                .setContext(this)
                .setListener(getNearConnectListener(), Looper.getMainLooper()).build();
        mNearConnect.startReceiving();
    }

    private NearConnect.Listener getNearConnectListener() {
        return new NearConnect.Listener() {
            @Override
            public void onReceive(byte[] bytes, Host sender) {
                if (bytes != null) {
                    String data = new String(bytes);
                    switch (data) {
                        case STATUS_EXIT_CHAT:
                            binding.msgLl.setVisibility(View.GONE);
                            new AlertDialog.Builder(OldChatActivity.this)
                                    .setMessage(String.format("%s has left the control.", sender.getName()))
                                    .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            mNearConnect.stopReceiving(true);
                                            OldChatActivity.this.setResult(RESULT_OK);
                                            OldChatActivity.this.finish();
                                        }
                                    }).create().show();
                            break;
                        default:
//                            if (data.startsWith(MSG_PREFIX)) {
//                                mChatAdapter.addMessage(data.substring(4), sender.getName());
//                            }
                            break;
                    }
                }
            }

            @Override
            public void onSendComplete(long jobId) {
//                update UI with sent status if necessary
            }

            @Override
            public void onSendFailure(Throwable e, long jobId) {
                e.printStackTrace();
            }

            @Override
            public void onStartListenFailure(Throwable e) {
                e.printStackTrace();
            }
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_old_chat);
        present = new ChatPresent(this, this);
        present.setListener();
        initView();
    }

    private void initView() {
        mParticipant = getIntent().getParcelableExtra(BUNDLE_PARTICIPANT);
        featuresBeans = DataSupport.findAll(FeaturesBean.class);

        findViewById(R.id.img_tune_add).setOnClickListener(this);
        scrollView = (ScrollView) findViewById(R.id.scrollView);
        rv_fixed_list = (MyGridView) findViewById(R.id.rv_fixed_list);
        rv_list = (MyGridView) findViewById(R.id.rv_list);
        findViewById(R.id.tx_send).setOnClickListener(this);
        findViewById(R.id.tx_save).setOnClickListener(this);
        findViewById(R.id.btn_add).setOnClickListener(this);
        ed_content = (EditText) findViewById(R.id.ed_content);


        initAdapter();
        initFixedAdapter();
        clearListState();
        initNearConnect();

    }

    List<FixedBean> fixedBeans = new ArrayList<>();
    FixedBean fixedBean;

    private void initFixedAdapter() {

        fixedBeans = present.getFixedTop();
        fixedListAdapter = new GridFixedListAdapter(this, fixedBeans);
        rv_fixed_list.setAdapter(fixedListAdapter);
        rv_fixed_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                fixedBean = (FixedBean) fixedListAdapter.getItem(position);
                fixedPositon = position;
                bean = null;
                clearListState();
                for (FixedBean features : fixedBeans) {
                    features.setCheck(false);
                }
                fixedBeans.get(position).setCheck(true);
                fixedListAdapter.setData(fixedBeans);
                if (!StringUtils.isEmpty(fixedBean.getContent())) {
                    sendMessage(fixedBean.getContent());
                }
                if (!StringUtils.isEmpty(fixedBean.getSocket_position()) && !StringUtils.isEmpty(fixedBean.getSocket_page())) {
                    present.sendMsgData(fixedBean.getSocket_position(), fixedBean.getSocket_page());

                }
                ed_content.setText(fixedBean.getContent());
            }
        });
    }

    FeaturesBean bean;
    GridListAdapter gridListAdapter;
    GridFixedListAdapter fixedListAdapter;

    private void initAdapter() {
        rv_list.setVisibility(featuresBeans.size() <= 0 ? View.GONE : View.VISIBLE);
        gridListAdapter = new GridListAdapter(this, featuresBeans);
        rv_list.setAdapter(gridListAdapter);

        rv_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                bean = (FeaturesBean) gridListAdapter.getItem(position);
                fixedBean = null;
                clearFixedState();
                for (FeaturesBean features : featuresBeans) {
                    features.setCheck(false);
                    features.save();
                }
                bean.setCheck(true);
                bean.save();
                initAdapter();
                ed_content.setText(bean.getContent());
                if (!StringUtils.isEmpty(bean.getContent())) {
//                    sendChatMessage(bean.getContent());
                }


            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        ed_content.setText("");
        featuresBeans = DataSupport.findAll(FeaturesBean.class);
        initAdapter();
    }

    @Override
    public void onPause() {
        super.onPause();
    }


    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.img_tune_add:
                startActivity(new Intent(this, FeaturesActivity.class));
                break;

            case R.id.tx_send:
                if (!StringUtils.isEmpty(ed_content.getText().toString())) {
                    sendMessage(ed_content.getText().toString());
                } else {
                    Toast.makeText(this, "请输入命令类容!", Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.tx_save:
                String edContentMsg = ed_content.getText().toString().trim();
                msgSave(edContentMsg);
                break;

            case R.id.btn_add:
                MessageDialog messageDialog = new MessageDialog(this, new FeaturesBean());
                messageDialog.setOnMessageListener(this);
                messageDialog.show();
                break;

        }
    }

    private int fixedPositon = 0;

    public void msgSave(String msg) {
        if (bean == null) {//选择保存的是按钮
            SharedPreferencesUtils.init(this).put(fixedBean.getTag(), msg);
            fixedBeans = present.getFixedTop();
            fixedBeans.get(fixedPositon).setCheck(true);
            fixedListAdapter.setData(fixedBeans);
            Toast.makeText(this, "保存成功!", Toast.LENGTH_LONG).show();
        } else {//item 内容保存
            if (!StringUtils.isEmpty(msg) && bean != null) {
                bean.setContent(msg);
                Toast.makeText(this, bean.save() ? "保存成功!" : "保存失败!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "保存类容为空!", Toast.LENGTH_LONG).show();
            }
        }

    }


    @Override
    public void sendMessage(FeaturesBean bean) {
        featuresBeans = DataSupport.findAll(FeaturesBean.class);
        initAdapter();
    }

    @Override
    public void showMsg() {

    }


    @Override
    public void sendChatMsg(String s, String s1, String type, String s2) {

    }


    public void clearListState() {
        if (featuresBeans != null) {
            for (FeaturesBean featuresBean : featuresBeans) {
                featuresBean.setCheck(false);
                featuresBean.save();
            }
        }
        gridListAdapter.setData(featuresBeans);

    }

    public void clearFixedState() {
        if (fixedBeans != null) {
            for (FixedBean featuresBean : fixedBeans) {
                featuresBean.setCheck(false);
            }
        }
        fixedListAdapter.setData(fixedBeans);
    }

    private void sendMessage(String message) {
        mNearConnect.send((MSG_PREFIX + message).getBytes(), mParticipant);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        mNearConnect.send(STATUS_EXIT_CHAT.getBytes(), mParticipant);
        mNearConnect.stopReceiving(true);
    }
}
