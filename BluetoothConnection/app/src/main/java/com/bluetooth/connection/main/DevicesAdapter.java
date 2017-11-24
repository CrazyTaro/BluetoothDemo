package com.bluetooth.connection.main;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.bluetooth.connection.BlueStatus;
import com.bluetooth.connection.R;

import java.util.List;

/**
 * Created by taro on 2017/7/6.
 */

public class DevicesAdapter extends RecyclerView.Adapter<DevicesAdapter.DevicesHolder> implements View.OnClickListener {
    private List<BlueStatus> mDatas;
    private OnViewClickListener mListener;

    @Override
    public DevicesHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View contentView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_devices, parent, false);
        return new DevicesHolder(contentView);
    }

    @Override
    public void onBindViewHolder(DevicesHolder holder, int position) {
        BlueStatus status = getItem(position);
        if (status != null) {
            holder.mTvName.setText(String.format("设备名称:%s", status.name));
            holder.mTvAddr.setText(String.format("设备地址:%s", status.addr));
            holder.mTvStatus.setText(String.format("设备状态:%s", status.isConnected ? "已连接" : "未连接"));
        }
        holder.mBtnChangedStatus.setTag(position);
        holder.mBtnRemove.setTag(position);
        holder.mBtnChangedStatus.setOnClickListener(this);
        holder.mBtnRemove.setOnClickListener(this);
    }

    @Override
    public int getItemCount() {
        return mDatas == null ? 0 : mDatas.size();
    }

    public DevicesAdapter setDatas(List<BlueStatus> datas) {
        mDatas = datas;
        return this;
    }

    public List<BlueStatus> getDatas() {
        return mDatas;
    }

    public DevicesAdapter setOnViewClickListener(OnViewClickListener listener) {
        mListener = listener;
        return this;
    }

    public BlueStatus getItem(int position) {
        if (isPositionValid(position)) {
            return mDatas.get(position);
        } else {
            return null;
        }
    }

    public boolean isPositionValid(int position) {
        return (position >= 0 && position < getItemCount());
    }

    @Override
    public void onClick(View v) {
        if (v.getTag() != null && v.getTag() instanceof Integer) {
            int position = (int) v.getTag();
            if (mListener != null) {
                mListener.onViewClick(v, position);
            }
        }
    }

    public interface OnViewClickListener {
        public void onViewClick(View v, int position);
    }

    public static class DevicesHolder extends RecyclerView.ViewHolder {
        TextView mTvName;
        TextView mTvAddr;
        TextView mTvStatus;
        Button mBtnChangedStatus;
        Button mBtnRemove;

        public DevicesHolder(View itemView) {
            super(itemView);
            mTvName = (TextView) itemView.findViewById(R.id.tv_item_devices_name);
            mTvAddr = (TextView) itemView.findViewById(R.id.tv_item_devices_addr);
            mTvStatus = (TextView) itemView.findViewById(R.id.tv_item_devices_status);

            mBtnChangedStatus = (Button) itemView.findViewById(R.id.btn_item_devices_changed_status);
            mBtnRemove = (Button) itemView.findViewById(R.id.btn_item_devices_remove);
        }
    }
}
