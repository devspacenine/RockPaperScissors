package com.devspacenine.rockpaperscissors;

import android.app.ExpandableListActivity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.SimpleCursorTreeAdapter;

public class GameStats extends ExpandableListActivity {
	
	private SimpleCursorTreeAdapter mTreeAdapter;
	private StatsDbAdapter mDbAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mDbAdapter = new StatsDbAdapter(this);
		mDbAdapter.open();
		
		setContentView(R.layout.game_stats);
		mTreeAdapter = new GameStatsTreeAdapter(
				this,
				mDbAdapter.fetchGames(), 
				R.layout.game_stats_group,
				new String[] { StatsDbAdapter.KEY_ROWID, StatsDbAdapter.KEY_GAME_DATE },
				new int[] { R.id.game_number, R.id.game_date },
				R.layout.game_stats_child,
				new String[] { StatsDbAdapter.KEY_PLAYER_CHOICE, StatsDbAdapter.KEY_OPPONENT_CHOICE, StatsDbAdapter.KEY_ROUND_RESULT },
				new int[] { R.id.player_choice, R.id.opponent_choice, R.id.match_result });
		
		setListAdapter(mTreeAdapter);
	}
	
	@Override
	public boolean onChildClick(ExpandableListView parent, View v,
			int groupPosition, int childPosition, long id) {
//		return super.onChildClick(parent, v, groupPosition, childPosition, id);
		return true;
	}
	
	public class GameStatsTreeAdapter extends SimpleCursorTreeAdapter{
		
		public GameStatsTreeAdapter(Context context, Cursor cursor, int groupLayout, String[] groupFrom, int[] groupTo, int childLayout, String[] childFrom, int[] childTo){
			super(context, cursor, groupLayout, groupFrom, groupTo, childLayout, childFrom, childTo);
		}

		@Override
		protected Cursor getChildrenCursor(Cursor groupCursor) {
			
			return mDbAdapter.fetchGameRounds(groupCursor.getInt(groupCursor.getColumnIndexOrThrow(StatsDbAdapter.KEY_ROWID)));
		}
	}
}
