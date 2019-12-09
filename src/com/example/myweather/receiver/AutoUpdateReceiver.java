
package com.example.myweather.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.myweather.service.AutoUpdateService;

public class AutoUpdateReceiver extends BroadcastReceiver{
	public void onReceive(Context content,Intent intent){
		Intent i = new Intent(content, AutoUpdateService.class);
		content.startService(i);
	}
}