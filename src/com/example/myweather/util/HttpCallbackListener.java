
package com.example.myweather.util;

public interface HttpCallbackListener{
	
	void onFinish(String response);//���������ɹ���Ӧ��ʱ�����ø÷���
	
	void onError(Exception e);//������������������ʱ�����ø÷���
}