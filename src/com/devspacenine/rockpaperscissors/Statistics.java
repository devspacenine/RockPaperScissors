package com.devspacenine.rockpaperscissors;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class Statistics extends Activity {
	
	private StatsDbAdapter mStatsDbAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
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
}
