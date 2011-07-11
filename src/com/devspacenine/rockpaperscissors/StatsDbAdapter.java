package com.devspacenine.rockpaperscissors;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class StatsDbAdapter {
	/**
	 * There are two tables. The first is overall statistics for each game, the second
	 * entails each play made throughout the game and its result.
	 */
	public static final String KEY_ROWID = "_id";
	public static final String KEY_GAME_RESULT = "game_result";
	public static final String KEY_BESTOF = "best_of";
	public static final String KEY_OPPONENT = "opponent_type";
	public static final String KEY_GAME_DATE = "start_date";
	
	public static final String KEY_GAMEID = "game_id";
	public static final String KEY_PLAYER_CHOICE = "player_choice";
	public static final String KEY_OPPONENT_CHOICE = "opponent_choice";
	public static final String KEY_ROUND_RESULT = "round_result";
	public static final String KEY_ROUND_TIME = "round_time";
	
	private static final String TAG = "StatsDbAdapter";
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;
	
	// Table creation strings
	private static final String DATABASE_GAME_CREATE = 
		"CREATE TABLE game_results (_id INTEGER PRIMARY KEY AUTOINCREMENT, "
		+ "start_date DATE NOT NULL, opponent_type CHAR(1) NOT NULL, best_of INTEGER NOT NULL);";
	private static final String DATABASE_ROUND_CREATE = 
		"CREATE TABLE round_results (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
		"game_id INTEGER NOT NULL, round_time TIME NOT NULL, player_choice CHAR(8) NOT NULL, "
		+ "opponent_choice CHAR(8) NOT NULL, round_result CHAR(1) NOT NULL);";
	
	//TODO: Add Cross Reference For Friend Name & Game ID Table
	
	
	// Relevant database information
	private static final String DATABASE_NAME = "rps_data";
	private static final String DATABASE_GAME_TABLE = "game_results";
	private static final String DATABASE_ROUND_TABLE = "round_results";
	private static final int DATABASE_VERSION = 2;
	
	private final Context mCtx;
	
	private static class DatabaseHelper extends SQLiteOpenHelper{
		DatabaseHelper(Context ctx){
			super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_GAME_CREATE);
			db.execSQL(DATABASE_ROUND_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS game_results");
            db.execSQL("DROP TABLE IF EXISTS round_results");
            onCreate(db);
			
		}
	}
	
	public StatsDbAdapter(Context ctx){
		mCtx = ctx;
	}
	
	/**
	 * Opens a connection to the rps_data database. If it can't be opened, it will 
	 * attempt to create a new instance of the database. If it can't be created, it
	 * throws an exception. This is done through the parent class of DatabaseHelper.
	 * 
	 * @return this for chainability
	 * @throws SQLException
	 */
	public StatsDbAdapter open() throws SQLException{
		mDbHelper = new DatabaseHelper(mCtx);
		mDb = mDbHelper.getWritableDatabase();
		return this;
	}
	
	public void close(){
		mDbHelper.close();
	}
	
	/**
	 * Creates a new game in the game_results table. Returns the rowid if the insert
	 * is successful, otherwise it returns -1.
	 * 
	 * @param opponent_type "C" for computer, "F" for friend, "S" for stranger  
	 * @param best_of integer value for best of # of games
	 * @return rowid or -1 if failure
	 */
	public long createGame(String opponent_type, int best_of){
		ContentValues vals = new ContentValues();
		vals.put(KEY_OPPONENT, opponent_type);
		vals.put(KEY_BESTOF, best_of);
		// this is kind of annoying but apparently one of they only
		// ways to insert a date with this contentvalues.
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date date = new Date();
		vals.put(KEY_GAME_DATE, dateFormat.format(date));
		
		return mDb.insert(DATABASE_GAME_TABLE, null, vals);
	}
	
	/**
	 * Inserts the results of a round of RPS. Returns the rowid if the insert
	 * was successful, otherwise it returns -1.
	 * 
	 * @param game_id integer id of the game from game_results table
	 * @param player_choice "R" for rock, "P" for paper, "S" for scissors
	 * @param opponent_choice see above
	 * @param result "W" for win, "L" for loss, "D" for draw
	 * @return rowid or -1
	 */
	public long insertRound(int game_id, String player_choice, String opponent_choice, String result){
		ContentValues vals = new ContentValues();
		vals.put(KEY_GAMEID, game_id);
		vals.put(KEY_PLAYER_CHOICE, player_choice);
		vals.put(KEY_OPPONENT_CHOICE, opponent_choice);
		vals.put(KEY_ROUND_RESULT, result);
		// insert time of round
		SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
		Date date = new Date();
		vals.put(KEY_ROUND_TIME, timeFormat.format(date));
		
		return mDb.insert(DATABASE_ROUND_TABLE, null, vals);
	}
	
	/**
	 * This will return a cursor with the game id, game date, and opponent type columns.
	 * 
	 * @return Cursor to the list of the played games in the database 
	 */
	public Cursor fetchGames(){
//		String query = String.format()
		Cursor cursor = 
			mDb.query(DATABASE_GAME_TABLE, new String[] { KEY_ROWID, KEY_GAME_DATE, KEY_OPPONENT },
					null, null, null, null, KEY_ROWID + " DESC");

		return cursor;
	}
	
	public Cursor fetchGameRounds(Integer game_id){
		Cursor cursor =
			mDb.rawQuery(
					"SELECT _id, player_choice, opponent_choice, round_result FROM round_results "
					+ "WHERE game_id=?", new String[] {game_id.toString()});
		
		return cursor;
	}
	
	/**
	 * Function for determining the winner of a game.
	 * 
	 * @param game_id the game_id for the 
	 * @return true if the user won the game, false otherwise
	 */
	public boolean fetchGameWinner(Integer game_id){
		// Doing a raw query because it's easier than figuring out how to
		// do the parameters for the other type for now.
		Cursor cursor = 
			mDb.rawQuery(
					"SELECT best_of, (SELECT COUNT(*) FROM round_results WHERE "
					+ "game_id=? AND round_result='W') AS win_count FROM game_results",
					new String[] { game_id.toString() });
		if(cursor != null){
			cursor.moveToFirst();
		}
		return cursor.getInt(cursor.getColumnIndexOrThrow(KEY_BESTOF)) / 2 + 1 > 
			cursor.getInt(cursor.getColumnIndexOrThrow("win_count")) ? false : true;
	}
	
	public void rebuildTables()
	{
		mDb.execSQL("DROP TABLE IF EXISTS game_results");
		mDb.execSQL("DROP TABLE IF EXISTS round_results");
		mDb.execSQL(DATABASE_GAME_CREATE);
		mDb.execSQL(DATABASE_ROUND_CREATE);
	}
	
}
