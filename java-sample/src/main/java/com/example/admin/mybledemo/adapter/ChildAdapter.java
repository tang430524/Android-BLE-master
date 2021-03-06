package com.example.admin.mybledemo.adapter;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.admin.mybledemo.R;
import com.example.admin.mybledemo.Utils;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.UUID;

import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.callback.BleNotifyCallback;
import cn.com.heaton.blelibrary.ble.callback.BleReadCallback;
import cn.com.heaton.blelibrary.ble.callback.BleReadDescCallback;
import cn.com.heaton.blelibrary.ble.callback.BleWriteCallback;
import cn.com.heaton.blelibrary.ble.callback.BleWriteDescCallback;
import cn.com.heaton.blelibrary.ble.model.BleDevice;
import cn.com.heaton.blelibrary.ble.utils.ByteUtils;
import cn.com.heaton.blelibrary.ble.utils.ThreadUtils;

public class ChildAdapter extends RecyclerAdapter<BluetoothGattCharacteristic> {

    public ChildAdapter(Context context, List<BluetoothGattCharacteristic> datas) {
        super(context, R.layout.item_deviceinfo_child, datas);
    }

    @Override
    public RecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerViewHolder holder = super.onCreateViewHolder(parent, viewType);

        return holder;
    }

    @Override
    public void convert(RecyclerViewHolder holder, BluetoothGattCharacteristic characteristic) {
        TextView tvCharUuid = holder.getView(R.id.tv_char_uuid);
        TextView tvCharProperties = holder.getView(R.id.tv_properties);
        TextView tvReadValue = holder.getView(R.id.tv_read_value);
        TextView tvCharHandle = holder.getView(R.id.tv_char_handle);
        ImageView ivRead = holder.getView(R.id.iv_read);
        ImageView ivWrite = holder.getView(R.id.iv_write);
        ImageView ivNotify = holder.getView(R.id.iv_notify);

        UUID characteristicUuid = characteristic.getUuid();
        tvCharUuid.setText(Utils.getUuid(characteristicUuid.toString()));
        tvCharHandle.setText("Handle:"+characteristic.getInstanceId());

        ivRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<BleDevice> connetedDevices = Ble.getInstance().getConnectedDevices();
                BluetoothGattService characteristicService = characteristic.getService();
                UUID serviceUuid = characteristicService.getUuid();
                UUID characteristicUuid = characteristic.getUuid();
                int serviceInstanceId = characteristicService.getInstanceId();
                if (!connetedDevices.isEmpty()){
                    Ble.getInstance().readByUuid(
                            connetedDevices.get(0),
                            serviceUuid,
                            serviceInstanceId,
                            characteristicUuid,
                            new BleReadCallback<BleDevice>() {
                                @Override
                                public void onReadSuccess(BleDevice dedvice, BluetoothGattCharacteristic characteristic) {
                                    super.onReadSuccess(dedvice, characteristic);
                                    ThreadUtils.ui(new Runnable() {
                                        @Override
                                        public void run() {
                                            tvReadValue.setVisibility(View.VISIBLE);
                                            tvReadValue.setText(String.format("value: %s%s","(0x)", ByteUtils.bytes2HexStr(characteristic.getValue())));
                                        }
                                    });
                                }

                                @Override
                                public void onReadFailed(BleDevice device, int failedCode) {
                                    super.onReadFailed(device, failedCode);
                                    toast("??????????????????:"+failedCode);
                                }
                            });
                }
            }
        });
        ivWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showWriteDialog(characteristic, null);
            }
        });
        ivNotify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<BleDevice> connetedDevices = Ble.getInstance().getConnectedDevices();
                UUID serviceUuid = characteristic.getService().getUuid();
                UUID characteristicUuid = characteristic.getUuid();
                int serviceInstanceId = characteristic.getService().getInstanceId();
                boolean enable = true;
                if (ivNotify.getTag() != null){
                    enable = false;
                }
                Ble.getInstance().enableNotifyByUuid(
                        connetedDevices.get(0),
                        enable,
                        serviceUuid,
                        serviceInstanceId,
                        characteristicUuid,
                        new BleNotifyCallback<BleDevice>() {
                            @Override
                            public void onChanged(BleDevice device, BluetoothGattCharacteristic characteristic) {
                                ThreadUtils.ui(new Runnable() {
                                    @Override
                                    public void run() {
                                        tvReadValue.setVisibility(View.VISIBLE);
                                        tvReadValue.setText(String.format("Notifications enabled\nvalue: %s%s", "(0x)", ByteUtils.bytes2HexStr(characteristic.getValue())));
                                    }
                                });
                            }

                            @Override
                            public void onNotifySuccess(BleDevice device) {
                                super.onNotifySuccess(device);
                                ThreadUtils.ui(new Runnable() {
                                    @Override
                                    public void run() {
                                        ivNotify.setTag(true);
                                        ivNotify.setImageResource(R.drawable.ic_notifications);
                                        tvReadValue.setText("Notifications enabled");
                                    }
                                });
                            }

                            @Override
                            public void onNotifyCanceled(BleDevice device) {
                                super.onNotifyCanceled(device);
                                ThreadUtils.ui(new Runnable() {
                                    @Override
                                    public void run() {
                                        ivNotify.setTag(null);
                                        ivNotify.setImageResource(R.drawable.ic_notifications_off);
                                        tvReadValue.setText("Notifications and indications disabled");
                                    }
                                });
                            }
                        });
            }
        });

        LinearLayout llDesc = holder.getView(R.id.ll_desc);



        int charaProp = characteristic.getProperties();
        StringBuilder builder = new StringBuilder();
        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_READ) != 0) {
            builder.append("READ,");
            ivRead.setVisibility(View.VISIBLE);
        }
        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0) {
            builder.append("WRITE,");
            ivWrite.setVisibility(View.VISIBLE);
        }
        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) {
            builder.append("WRITE_NO_RESPONSE,");
            ivWrite.setVisibility(View.VISIBLE);
        }
        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
            builder.append("NOTIFY,");
            ivNotify.setVisibility(View.VISIBLE);
        }
        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
            builder.append("INDICATE,");
            ivNotify.setVisibility(View.VISIBLE);
        }
        if (builder.length() > 0){
            builder.deleteCharAt(builder.length()-1);
            tvCharProperties.setText(String.format("Properties: %s", builder.toString()));
        }

        llDesc.removeAllViews();
        List<BluetoothGattDescriptor> descriptors = characteristic.getDescriptors();
        Log.e("descriptors", "convert: "+descriptors.size() );
        if (descriptors.size() > 0){
            TextView tvTitle = new TextView(mContext);
            tvTitle.setTextColor(ContextCompat.getColor(mContext, R.color.text_gray));
            tvTitle.setText("Descriptors:");
            llDesc.addView(tvTitle);

            for (BluetoothGattDescriptor descriptor: descriptors){
                View desc_view = LayoutInflater.from(mContext).inflate(R.layout.item_desc, llDesc, false);
                ImageView ivDescRead = desc_view.findViewById(R.id.iv_read);
                ImageView ivDescWrite = desc_view.findViewById(R.id.iv_write);
                TextView tvDescType = desc_view.findViewById(R.id.tv_desc_type);
                TextView tvDescUuid = desc_view.findViewById(R.id.iv_desc_uuid);
                TextView tvDescHandle = desc_view.findViewById(R.id.iv_desc_handle);
                TextView tvDescReadValue = desc_view.findViewById(R.id.tv_read_value);
                String descUuid = Utils.getUuid(descriptor.getUuid().toString());
                if (descUuid.contains("0x2902")){
                    ivDescWrite.setVisibility(View.INVISIBLE);
                    tvDescType.setText("Client Characteristic Configuration");
                }
                tvDescUuid.setText(descUuid);
                tvDescHandle.setText("handle:????????????");
                llDesc.addView(desc_view);
                ivDescRead.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        List<BleDevice> connetedDevices = Ble.getInstance().getConnectedDevices();
                        BluetoothGattService characteristicService = characteristic.getService();
                        UUID serviceUuid = characteristicService.getUuid();
                        UUID characteristicUuid = characteristic.getUuid();
                        int serviceInstanceId = characteristicService.getInstanceId();
                        if (!connetedDevices.isEmpty()){
                            Ble.getInstance().readDesByUuid(
                                    connetedDevices.get(0),
                                    serviceUuid,
                                    serviceInstanceId,
                                    characteristicUuid,
                                    descriptor.getUuid(),
                                    new BleReadDescCallback<BleDevice>() {
                                        @Override
                                        public void onDescReadSuccess(BleDevice device, BluetoothGattDescriptor descriptor) {
                                            super.onDescReadSuccess(device, descriptor);
                                            if (TextUtils.isEmpty(descriptor.getUuid().toString()))return;
                                            ThreadUtils.ui(new Runnable() {
                                                @Override
                                                public void run() {
                                                    tvDescReadValue.setVisibility(View.VISIBLE);
//                                                    tvReadValue.setText("value: "+ByteUtils.bytes2HexStr(descriptor.getValue()));
                                                    try {
                                                        tvDescReadValue.setText(String.format("value: %s", new String(descriptor.getValue(), "UTF-8")));
                                                    } catch (UnsupportedEncodingException e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                            });
                                        }

                                        @Override
                                        public void onDescReadFailed(BleDevice device, int failedCode) {
                                            super.onDescReadFailed(device, failedCode);
                                            toast("??????????????????:"+failedCode);
                                        }
                                    });
                        }
                    }
                });
                ivDescWrite.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showWriteDialog(characteristic, descriptor);
                    }
                });
            }
        }

    }

    @SuppressLint("RestrictedApi")
    private void showWriteDialog(BluetoothGattCharacteristic characteristic, BluetoothGattDescriptor descriptor){
        final EditText edit = new EditText(mContext);
        edit.setHintTextColor(ContextCompat.getColor(mContext, R.color.text));
        edit.setHint("New value");
        AlertDialog alertDialog = new AlertDialog.Builder(mContext)
                .setTitle("Write value")
                .setPositiveButton("SEND",null)
                .setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss())
                .setView(edit, Utils.dp2px(20), 0, Utils.dp2px(20), 0)
                .create();
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button btnPositive = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                btnPositive.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (TextUtils.isEmpty(edit.getText().toString())){
                            edit.requestFocus();
                            edit.setError("???????????????!");
                        }else {
                            try {
                                write(characteristic, descriptor, ByteUtils.hexStr2Bytes(edit.getText().toString().trim()));
                                dialog.dismiss();
                            }catch (NumberFormatException e){
                                e.printStackTrace();
                                edit.requestFocus();
                                edit.setError("???????????????????????????!");
                            }
                        }
                    }
                });
            }
        });
        alertDialog.show();
    }

    private void write(BluetoothGattCharacteristic characteristic, BluetoothGattDescriptor descriptor, byte[] bytes){
        List<BleDevice> connetedDevices = Ble.getInstance().getConnectedDevices();
        UUID serviceUuid = characteristic.getService().getUuid();
        UUID characteristicUuid = characteristic.getUuid();
        int serviceInstanceId = characteristic.getService().getInstanceId();
        if (!connetedDevices.isEmpty()){
            BleDevice bleDevice = connetedDevices.get(0);
            if (descriptor == null){
                writeChar(bleDevice, bytes, serviceUuid,serviceInstanceId, characteristicUuid);
            }else {
                writeDes(bleDevice, bytes, serviceUuid,serviceInstanceId, characteristicUuid ,descriptor);
            }
        }
    }

    private void writeChar(BleDevice bleDevice, byte[] bytes, UUID serviceUuid,int serviceInstanceId, UUID characteristicUuid){
        Ble.getInstance().writeByUuid(
                bleDevice,
                bytes,
                serviceUuid,
                serviceInstanceId,
                characteristicUuid,
                new BleWriteCallback<BleDevice>() {
                    @Override
                    public void onWriteSuccess(BleDevice device, BluetoothGattCharacteristic characteristic) {
                        toast("??????????????????");
                    }

                    @Override
                    public void onWriteFailed(BleDevice device, int failedCode) {
                        super.onWriteFailed(device, failedCode);
                        toast("??????????????????:"+failedCode);
                    }
                });
    }

    private void writeDes(BleDevice bleDevice, byte[] bytes, UUID serviceUuid,int serviceInstanceId, UUID characteristicUuid, BluetoothGattDescriptor descriptor){
        Ble.getInstance().writeDesByUuid(
                bleDevice,
                bytes,
                serviceUuid,
                serviceInstanceId,
                characteristicUuid,
                descriptor.getUuid(),
                new BleWriteDescCallback<BleDevice>() {
                    @Override
                    public void onDescWriteSuccess(BleDevice device, BluetoothGattDescriptor descriptor) {
                        super.onDescWriteSuccess(device, descriptor);
                        toast("??????????????????");
                    }

                    @Override
                    public void onDescWriteFailed(BleDevice device, int failedCode) {
                        super.onDescWriteFailed(device, failedCode);
                        toast("??????????????????:"+failedCode);
                    }
                });
    }

    void toast(String msg){
        ThreadUtils.ui(new Runnable() {
            @Override
            public void run() {
                Utils.showToast(msg);
            }
        });
    }

}
