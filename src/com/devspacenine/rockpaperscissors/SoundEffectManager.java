package com.devspacenine.rockpaperscissors;

import java.util.HashMap;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

public class SoundEffectManager {
	static private SoundEffectManager _instance;
	private static SoundPool mSoundPool;
	private static HashMap<Integer, Integer> mSoundPoolMap;
	private static AudioManager mAudioManager;
	private static Context mContext;
	
	private SoundEffectManager(){
	}
	
	/**
	 * Requests the instance of the Sound Effect Manager and creates
	 * it if it does not exist
	 * 
	 * @return Returns the single instance of the SoundEffectManager
	 */
	static synchronized public SoundEffectManager getInstance() {
		if(_instance == null)
			_instance = new SoundEffectManager();
		return _instance;
	}
	
	/**
	 * Initializes the storage for the sounds
	 * 
	 * @param theContext The Application context
	 */
	public static void initSounds(Context theContext) {
		mContext = theContext;
		mSoundPool = new SoundPool(4, AudioManager.STREAM_MUSIC, 0);
		mSoundPoolMap = new HashMap<Integer, Integer>();
		mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
	}
	
	/**
	 * Add a new Sound to the SoundPool
	 * 
	 * @param index
	 * @param SoundID
	 */
	public static void addSound(int index, int SoundID) {
		mSoundPoolMap.put(index, mSoundPool.load(mContext, SoundID, index));
	}
	
	/**
	 * Loads the various Sound assets
	 */
	public static void loadSounds() {
		mSoundPoolMap.put(1, mSoundPool.load(mContext, R.raw.grunt, 1));
		mSoundPoolMap.put(2, mSoundPool.load(mContext, R.raw.win, 1));
		mSoundPoolMap.put(3, mSoundPool.load(mContext, R.raw.lose, 1));
		mSoundPoolMap.put(4, mSoundPool.load(mContext, R.raw.single_grunt, 1));
	}
	
	/**
	 * Plays a Sound
	 * 
	 * @param index - The Index of the Sound to be played
	 * @param speed - The Speed to play the Sound at
	 */
	public static void playSound(int index, float speed) {
		float streamVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		streamVolume = streamVolume / mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		mSoundPool.play(mSoundPoolMap.get(index), streamVolume, streamVolume, 1, 0, 1f);
	}
	
	/**
	 * Plays a sound multiple times in a row
	 * 
	 * @param index - The Index of the Sound to be played
	 * @param loops - The amount of times the Sound will repeat
	 * @param speed - The Speed to play the Sound at
	 */
	public static void playLoopedSound(int index, int loops, float speed) {
		float streamVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		streamVolume = streamVolume / mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		mSoundPool.play(mSoundPoolMap.get(index), streamVolume, streamVolume, 1, loops, 1f);
	}
	
	/**
	 * Stop a Sound
	 * 
	 * @param index - Index of the Sound to be stopped
	 */
	public static void stopSound(int index) {
		mSoundPool.stop(mSoundPoolMap.get(index));
	}
	
	/**
	 * Deallocates the resources and Instance of SoundEffectManager
	 */
	public static void cleanup() {
		mSoundPool.release();
		mSoundPool = null;
		mSoundPoolMap.clear();
		mAudioManager.unloadSoundEffects();
		_instance = null;
	}
}