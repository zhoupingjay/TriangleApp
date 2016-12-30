package com.myproj.triangle;

import java.util.HashMap;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

public class SfxManager {
	private  SoundPool mSfxPool; 
	private  HashMap<Integer, Integer> mSfxMap; 
	private  AudioManager  mAudioManager;
	private  Context mContext;
	
	public void initSfx(Context context) { 
		 mContext = context;
	     mSfxPool = new SoundPool(4, AudioManager.STREAM_MUSIC, 0); 
	     mSfxMap = new HashMap<Integer, Integer>(); 
	     mAudioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE); 	     
	}
	
	public void addSfx(int index,int sndID)
	{
		mSfxMap.put(index, mSfxPool.load(mContext, sndID, 1));
	}
	
	public void playSfx(int index) { 
		
	     int streamVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_SYSTEM);
	     mSfxPool.play(mSfxMap.get(index), streamVolume, streamVolume, 1, 0, 1f); 
	}
}
