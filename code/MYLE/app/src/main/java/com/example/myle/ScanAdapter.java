package com.example.myle;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class ScanAdapter extends ArrayAdapter<MyleDevice>{
	private Context context; 
    private int layoutResourceId;    
    private ArrayList<MyleDevice> data = new ArrayList<MyleDevice>();
    
    public ScanAdapter(Context context, int layoutResourceId, ArrayList<MyleDevice> data) {
    	super(context, layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.data = data;
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        DeviceHolder holder = null;
        
        if (convertView == null)
        {
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            convertView = inflater.inflate(layoutResourceId, parent, false);
            
            holder = new DeviceHolder(convertView);
            convertView.setTag(holder);
        }
        else
        {
            holder = (DeviceHolder)convertView.getTag();
        }
        
        MyleDevice device = data.get(position);
        holder.name.setText(device.getName());
        holder.address.setText(device.getDevice().getAddress());
        
        return convertView;
    }
    
    static class DeviceHolder {
    	private TextView name;
    	private TextView address;
        
        public DeviceHolder(View v) {
        	 this.name = (TextView)v.findViewById(R.id.tv_device_name);
        	 this.address = (TextView)v.findViewById(R.id.tv_device_address);
        }
    }
}

