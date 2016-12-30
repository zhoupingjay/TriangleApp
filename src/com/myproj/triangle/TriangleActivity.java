package com.myproj.triangle;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;
import com.google.ads.*;
import com.google.ads.AdRequest.ErrorCode;

enum SCREEN_STATE {
	SCREEN_GAME,
	SCREEN_HELP
};

public class TriangleActivity extends Activity implements AdListener {
    private static final String tag = "TriangleActivity";
    private static final String publisherId = "a14dd9e0565131e"; 

	/** Called when the activity is first created. */
	private GameView2 gameView;
	private TextView txtCheckerCount, txtStatus;
	private Button btnReplay, btnExit;
	//private Spinner gameSizeSelector;
	private int gameSizeSelected = 0;
	private LinearLayout layoutStatus;
	private RelativeLayout layoutButtons;
	private LinearLayout layoutHelp;
	private LinearLayout layoutGame;
	private LinearLayout layoutAd;
	private SCREEN_STATE screenState = SCREEN_STATE.SCREEN_GAME;
	private AdView adView = null;
	private boolean soundEnabled = true;
	private SfxManager mSfxManager;

	class MyOnItemSelectedListener implements OnItemSelectedListener {

		@Override
		public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
			// TODO Auto-generated method stub
			Log.i(tag, "item " + arg2 + " selected");
			gameSizeSelected = Board.MinBoardSize + arg2;
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
			// TODO Auto-generated method stub
		}

	}
	
	@Override
    public void onDismissScreen(Ad arg0) {
      Log.d("MY_LOG", "onDismissScreen");
    }

    @Override
    public void onLeaveApplication(Ad arg0) {
      Log.d("MY_LOG", "onLeaveApplication");

    }

    @Override
    public void onPresentScreen(Ad arg0) {
      Log.d("MY_LOG", "onPresentScreen");
      
    }

    @Override
    public void onReceiveAd(Ad arg0) {
      Log.d("MY_LOG", "Did Receive Ad");
    }
    
	@Override
	public void onFailedToReceiveAd(Ad arg0, ErrorCode arg1) {
		// TODO Auto-generated method stub
		Log.d("MY_LOG", "failed to receive ad (" + arg1 + ")");
	}

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.main);

        screenState = SCREEN_STATE.SCREEN_GAME;

        gameView = (GameView2)findViewById(R.id.game_view);
        txtCheckerCount = (TextView)findViewById(R.id.slot_count);
        txtStatus = (TextView)findViewById(R.id.status);
        btnReplay = (Button)findViewById(R.id.replay);
        btnExit = (Button)findViewById(R.id.exit);
        //gameSizeSelector = (Spinner)findViewById(R.id.game_size);
        layoutStatus = (LinearLayout)findViewById(R.id.layout_status);
        layoutButtons = (RelativeLayout)findViewById(R.id.layout_buttons);
        layoutHelp = (LinearLayout)findViewById(R.id.layout_help);
        layoutGame = (LinearLayout)findViewById(R.id.layout_game);
        layoutAd = (LinearLayout)findViewById(R.id.layout_ad);
        
        //adView = new AdView(this, AdSize.BANNER, publisherId);
        //layoutGame.addView(adView);
        //adView = (AdView)this.findViewById(R.id.adView);
        //AdRequest adRequest = new AdRequest();
        //adRequest.addTestDevice(AdRequest.TEST_EMULATOR);
        //adView.loadAd(adRequest);

        gameView.setLabels(txtCheckerCount, txtStatus);
        gameView.setButtons(btnReplay, btnExit);
        
        //ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
        //        this, R.array.game_sizes, android.R.layout.simple_spinner_item);
        //adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //gameSizeSelector.setAdapter(adapter);
        //gameSizeSelector.setSelection(0);
        //gameSizeSelected = Board.MinBoardSize;
        //gameSizeSelector.setOnItemSelectedListener(new MyOnItemSelectedListener());
        
        layoutHelp.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Context ctx = v.getContext();
				assert(ctx instanceof TriangleActivity);
				
				TriangleActivity act = (TriangleActivity)ctx;
				act.showGameScreen();
			}
        	
        });
        
        btnReplay.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				int gameSize = gameView.getGameSize();
				if(gameSize >= Board.MinBoardSize && gameSize <= Board.MaxBoardSize) {
					gameView.initBoard(gameSize, true);
					layoutStatus.invalidate();
				}
			}
        });
        
        btnExit.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				finish();
			}
        });

        if (savedInstanceState == null) {
        	gameView.initBoard(5);
            Log.i(tag, "onCreate: init game");
        }
        else {
        	Bundle map = savedInstanceState.getBundle(tag);
        	if (map != null) {
        		gameView.restoreState(map);
        	}
        	else {
        		gameView.initBoard(5);
        		Log.w(tag, "onCreate: failed to restore state, create a new game");
        	}
        }
        
        Resources res = this.getResources();
    	String stringStatus, stringCheckerCount;
    	stringStatus = res.getString(R.string.status_running);
    	stringCheckerCount = res.getString(R.string.checker_count);
        txtStatus.setText(stringStatus + " " + Integer.toString(gameView.getGameSize()));
		txtCheckerCount.setText(stringCheckerCount + " " + gameView.getBoard().countRemaining());
		
		gameView.mainActivity = this;
		
		mSfxManager = new SfxManager();
		mSfxManager.initSfx(getApplicationContext());
		mSfxManager.addSfx(1, R.raw.click);
		mSfxManager.addSfx(2, R.raw.gameover);

		screenState = SCREEN_STATE.SCREEN_GAME;
    }
	
	private void showGameScreen() {
		layoutHelp.setVisibility(View.INVISIBLE);
		layoutGame.setVisibility(View.VISIBLE);
		//gameView.setVisibility(View.VISIBLE);
		//layoutStatus.setVisibility(View.VISIBLE);
		//layoutButtons.setVisibility(View.VISIBLE);
		screenState = SCREEN_STATE.SCREEN_GAME;
	}
	
	private void showHelpScreen() {
		layoutHelp.setVisibility(View.VISIBLE);
		layoutGame.setVisibility(View.INVISIBLE);
		//gameView.setVisibility(View.INVISIBLE);
		//layoutStatus.setVisibility(View.INVISIBLE);
		//layoutButtons.setVisibility(View.INVISIBLE);
		screenState = SCREEN_STATE.SCREEN_HELP;
	}
	
	public void playClickSound() {
		if(soundEnabled && mSfxManager != null) {
			mSfxManager.playSfx(1);
		}
	}
	
	public void playGameoverSound() {
		if(soundEnabled && mSfxManager != null) {
			mSfxManager.playSfx(2);
		}
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		Log.d("Triangle", "onDestroy");
		super.onDestroy();
	}
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}
	@Override
	protected void onRestart() {
		// TODO Auto-generated method stub
		super.onRestart();
	}
	@Override
	protected void onResume() {
		Log.d("TriangleActivity", "onResume");
		super.onResume();
		if(adView == null) {
			adView = new AdView(this, AdSize.BANNER, publisherId);
			layoutAd.addView(adView);
	        //adView = (AdView)this.findViewById(R.id.adView);
	        AdRequest adRequest = new AdRequest();
	        adRequest.addTestDevice(AdRequest.TEST_EMULATOR);
	        adView.loadAd(adRequest);
		}
	}
	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		Log.i(tag, "OnStart");
		if(screenState == SCREEN_STATE.SCREEN_GAME) {
			showGameScreen();
		}
		super.onStart();
	}
	@Override
	protected void onStop() {
		Log.d("TriangleActivity", "onStop");
		super.onStop();
		if(adView != null) {
			layoutAd.removeView(adView);
			adView.destroy();
			adView = null;
		}
	}
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBundle(tag, gameView.saveState());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		//menu.add(groupId, itemId, order, title);
		
		//menu.addSubMenu(groupId, itemId, order, titleRes)
		SubMenu restartMenu = menu.addSubMenu(Menu.NONE, Menu.NONE, 1, R.string.gamesize);
		restartMenu.add(2, 5, 1, R.string.gamesize5);
		restartMenu.add(2, 6, 2, R.string.gamesize6);
		restartMenu.add(2, 7, 3, R.string.gamesize7);
		restartMenu.add(2, 8, 4, R.string.gamesize8);
		
		menu.add(1, 1, 2, R.string.sound);
		menu.add(1, 2, 3, R.string.help);
		menu.add(1, 3, 4, R.string.exit);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int gid = item.getGroupId();
		int id = item.getItemId();

		Log.i(tag, "menu group " + gid + " item " + id);
		if(gid == 1 && id == 3) {
			finish();
		}
		else if(gid == 1 && id == 2) {
			showHelpScreen();
		}
		else if(gid == 1 && id == 1) {
			soundEnabled = (!soundEnabled);
			if(soundEnabled) {
				Toast.makeText(getApplicationContext(), R.string.soundEnabled, Toast.LENGTH_SHORT).show();
			}
			else {
				Toast.makeText(getApplicationContext(), R.string.soundDisabled, Toast.LENGTH_SHORT).show();
			}
		}
		else if(gid == 2 && id >= Board.MinBoardSize && id <= Board.MaxBoardSize) {
			gameView.initBoard(id, true);
			layoutStatus.invalidate();
			showGameScreen();
		}
		return super.onOptionsItemSelected(item);
	}

}