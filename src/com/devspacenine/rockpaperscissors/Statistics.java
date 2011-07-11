package com.devspacenine.rockpaperscissors;

import com.google.ads.AdRequest;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

public class Statistics extends Activity {
	
	private StatsDbAdapter mStatsDbAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Add test devices for development so we don't look fishy
		AdRequest request = new AdRequest();
		request.addTestDevice(AdRequest.TEST_EMULATOR);
		request.addTestDevice("HT07NHL01089"); // My Evo test phone
		request.addTestDevice("3332BBFD4E5200EC"); // My Samsung tablet
		
		setContentView(R.layout.rps_statistics);
		
		mStatsDbAdapter = new StatsDbAdapter(this);
		mStatsDbAdapter.open();
		updateGameStats();
	}
	
	private void updateGameStats(){
		TextView winPercentView = (TextView)findViewById(R.id.win_percent);
		TextView losePercentView = (TextView)findViewById(R.id.loss_percent);
		
		try{
			Cursor game_cursor = mStatsDbAdapter.fetchGames();
			startManagingCursor(game_cursor);
			
			int totalWins = 0;
			float winPercent;
			while(!game_cursor.isLast()){
				game_cursor.move(1);
				if(mStatsDbAdapter.fetchGameWinner(game_cursor.getInt(
						game_cursor.getColumnIndexOrThrow("_id")))){
					totalWins++;
				}
			}
			winPercent = ((float)totalWins) / game_cursor.getCount();
			winPercentView.setText(String.format("%.2f%%", winPercent * 100));
			losePercentView.setText(String.format("%.2f%%", (1-winPercent) * 100));			
		}
		catch(SQLException ex) {
			Log.d("database", ex.getMessage());
		}
	}
	
	public void onViewHistory(View v){
		Intent intent = new Intent(this, GameStats.class);
		startActivity(intent);
	}
	
	public void onPlayChoiceHistory(View v){
		
	}
	
	@Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        Window window = getWindow();
        window.setFormat(PixelFormat.RGBA_8888);
    }
}
