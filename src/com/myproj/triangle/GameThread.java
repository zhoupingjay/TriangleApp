package com.myproj.triangle;

import android.content.Context;
import android.graphics.Paint;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;

public class GameThread extends Thread {
	static final String tag = "GameThread";
	SurfaceHolder m_surfaceHolder;
	Context m_context;
	Handler m_handler;
	Board m_board;
	Paint m_paint;
	
	public GameThread(SurfaceHolder surfaceHolder, Context context,	Handler handler) {
		m_surfaceHolder = surfaceHolder;
		m_context = context;
		m_handler = handler;
		m_paint = new Paint();
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		Log.i(tag, "run");
	}

}
