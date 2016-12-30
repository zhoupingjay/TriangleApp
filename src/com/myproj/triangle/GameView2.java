package com.myproj.triangle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.TransitionDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ImageView.ScaleType;

public class GameView2 extends RelativeLayout {
	public static final String tag = "GameView";
	private static final int viewPadding = 8, viewPadding_landscape = 24;

	private Board board;
	private int N = 0;			// Board size (# of checkers on each edge)
	private int D = 0;			// D: distance between centers of two slots
	private int checkerRadius = 0;			// R: radius of checkers (R < D/2)
	private Area boardArea = null;
	private int viewWidth, viewHeight;
	//private XY slotSelected = null;
	private SlotImageView slotSelected = null;
	private boolean gameOver = false;

	private TextView txtCheckerCount, txtStatus;
	private Button btnReplay, btnExit;
	private ArrayList<SlotImageView> imgSlots;
	private HashMap<XY, SlotImageView> imgSlotMap;
	private FrameLayout parentLayout;
	
	public LinearLayout layoutStatus;
	public TriangleActivity mainActivity = null;
    
    class Area {
		public int x1, y1, x2, y2;
		public Area() {	}
		public Area(int left, int top, int right, int bottom) {
			x1 = left;
			y1 = top;
			x2 = right;
			y2 = bottom;
		}
		public boolean isIn(int x, int y) {
			return (x >= x1 && x <= x2 && y >= y1 && y <= y2);
		}
		public String toString() {
			return "[(" + x1 + ", " + y1 + ") - (" + x2 + ", " + y2 + ")]";
		}
	};
	
	class SlotImageView extends ImageView {
		private int col, row;
		private int cx, cy;

		public SlotImageView(Context context) {
			super(context);
			// TODO Auto-generated constructor stub
		}
		
		public void setSlot(int col, int row) {
			this.col = col;
			this.row = row;
		}
		
		public XY getSlot() {
			return new XY(col, row);
		}
		
		public void setCoord(int x, int y) {
			cx = x;
			cy = y;
		}
		
		public XY getCoord() {
			return new XY(cx, cy);
		}

	};
	
	class SlotOnClickListener implements OnClickListener {
		private GameView2 parentView;

		public SlotOnClickListener(GameView2 p) {
			parentView = p;
		}

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			assert(v instanceof SlotImageView);
			SlotImageView siv = (SlotImageView)v;
			parentView.onTouch(siv);
		}
		
	};

	public GameView2(Context context, AttributeSet attrs) {
		super(context, attrs);
		imgSlots = new ArrayList<SlotImageView>();
		imgSlotMap = new HashMap<XY, SlotImageView>();
		ViewParent p = (this.getParent());
		assert(p instanceof FrameLayout);
		parentLayout = (FrameLayout)p;
	}
	
	public void initBoard(int boardSize, boolean redraw) {
		if(boardSize >= Board.MinBoardSize && boardSize <= Board.MaxBoardSize) {
			N = boardSize;
			board = new Board(N);
			board.setEmpty(0, 0, true);
			gameOver = false;
			updateStatus();
			if(redraw) {
				computeCoords();
				createImageViews();
				invalidate();
			}
		}
	}
	
	public void initBoard(int boardSize) {
		initBoard(boardSize, false);
	}
	
	public Board getBoard() {
		return board;
	}
	
	public int getGameSize() {
		return N;
	}
	
	public void computeCoords() {
		// Compute the size and distance of slots
		// Assuming D=3*R, (N-1)*D-2*R <= Width --> R = W/(3*N-1)
		if(N >= Board.MinBoardSize && N <= Board.MaxBoardSize && board != null) {
			//int W = (viewWidth < viewHeight) ? viewWidth : (viewHeight - txtCheckerCount.getHeight() - btnReplay.getHeight());
			int W = (viewWidth < viewHeight) ? viewWidth : (viewHeight);
			W -= (2 * viewPadding);

			checkerRadius = W / (3 * N - 1);
			D = (3 * checkerRadius) & 0xFFFFFFFE;		// D must be even number
			if(checkerRadius < 1 || D < 2) {
				Log.e(tag, "surface too small: R=" + checkerRadius + " D=" + D);
			}
			else {
				board.computeCoords(D, checkerRadius);
				int boardViewSize = Board.GetViewSize(N, D, checkerRadius);
				int boardViewHeight = (int)((double)boardViewSize * Board.sine60 + 0.5 + checkerRadius);
				if(boardArea == null)
					boardArea = new Area();
				boardArea.x1 = (viewWidth - boardViewSize) / 2;
				boardArea.y1 = (viewHeight - boardViewHeight) / 2;
				boardArea.x2 = boardArea.x1 + boardViewSize;
				boardArea.y2 = boardArea.y1 + boardViewHeight;
				//boardArea.y2 = this.getBottom();
				//boardArea.y1 = boardArea.y2 - boardViewHeight - checkerRadius;
				//boardArea.y1 = getTop();
				Log.i(tag, "viewWidth=" + viewWidth + " viewHeight=" + viewHeight);
				Log.i(tag, "boardWidth=" + boardViewSize + " boardHeight=" + boardViewHeight);
				Log.i(tag, "W=" + W + " N=" + N + ", D=" + D + ", R=" + checkerRadius);
				Log.i(tag, "label height " + txtCheckerCount.getHeight());
				Log.i(tag, "button height " + btnReplay.getHeight() + " at" + btnReplay.getTop());
				Log.i(tag, "boardArea: " + boardArea.toString());
				//createImageViews();
			}
		}
	}
	
	private SlotImageView findSlotImage(int col, int row) {
		Iterator<SlotImageView> it = imgSlots.iterator();
		while(it.hasNext()) {
			SlotImageView img = it.next();
			if(img.col == col && img.row == row) {
				return img;
			}
		}
		return null;
	}
	
	private void createImageViews() {
		Log.i(tag, "createImageViews");
		int row, col;
		int l = getLeft();
		int t = getTop();
		Resources res = this.getContext().getResources();

		this.removeAllViews();
		imgSlots.clear();

		Log.i(tag, "left=" + l + ", top=" + t);
		for(row = 0; row < N; row++) {
			for(col = 0; col <= row; col++) {
				XY slotPos = board.getSlotCoord(col, row);
				int cx = (slotPos.x + boardArea.x1);
				int cy = (slotPos.y + boardArea.y1);
				Log.i(tag, "image at " + cx + ", " + cy);
				SlotImageView img = new SlotImageView(this.getContext());
				img.setSlot(col, row);
				img.setCoord(cx, cy);
				img.setOnClickListener(new SlotOnClickListener(this));
				
				RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(2*checkerRadius, 2*checkerRadius);
				params.leftMargin = cx - checkerRadius;
				params.topMargin = cy - checkerRadius;
				img.setScaleType(ScaleType.FIT_XY);
				img.setLayoutParams(params);
				
				if(board.isEmpty(col, row)) {
					img.setImageDrawable(res.getDrawable(R.drawable.gray));
				}
				else {
					int state = board.getState(col, row);
					if(state == Board.SLOT_SELECTED) {
						img.setImageDrawable(res.getDrawable(R.drawable.green2));
					}
					else if(state == Board.SLOT_HIGHLIGHED) {
						img.setImageDrawable(res.getDrawable(R.drawable.grayvalid));
					}
					else if(state == Board.SLOT_EMPTY) {
						img.setImageDrawable(res.getDrawable(R.drawable.gray));
					}
					else {
						img.setImageDrawable(res.getDrawable(R.drawable.green));
					}
				}
				//parentLayout.addView(child)
				imgSlots.add(img);
				//img.setVisibility(VISIBLE);
				this.addView(img);
				imgSlotMap.put(img.getSlot(), img);
			}
		}
		//placeImages();
		//invalidate();
	}

	private void updateImageViews() {
		Log.i(tag, "updateImageViews");
		Iterator<SlotImageView> it = imgSlots.iterator();
		Resources res = this.getContext().getResources();

		while(it.hasNext()) {
			SlotImageView img = it.next();
			if(board.isEmpty(img.col, img.row)) {
				img.setImageDrawable(res.getDrawable(R.drawable.gray));
			}
			else {
				//Log.i(tag, "circle at (" + cx + ", " + cy + ") radius=" + checkerRadius);
				int state = board.getState(img.col, img.row);
				if(state == Board.SLOT_SELECTED) {
					img.setImageDrawable(res.getDrawable(R.drawable.green2));
				}
				else if(state == Board.SLOT_HIGHLIGHED) {
					img.setImageDrawable(res.getDrawable(R.drawable.grayvalid));
				}
				else if(state == Board.SLOT_EMPTY) {
					img.setImageDrawable(res.getDrawable(R.drawable.gray));
				}
				else {
					img.setImageDrawable(res.getDrawable(R.drawable.green));
				}
			}
		}
	}

	private void updateStatus() {
		// Update remaining slots
		Resources res = this.getResources();
    	String stringRunning, stringGameOver, stringCheckerCount;
    	stringRunning = res.getString(R.string.status_running);
    	stringGameOver = res.getString(R.string.status_gameover);
    	stringCheckerCount = res.getString(R.string.checker_count);

		if(txtCheckerCount != null) {
			Log.i(tag, "updateStatus: checker count");
			txtCheckerCount.setText(stringCheckerCount + " " + board.countRemaining());
			txtStatus.setVisibility(VISIBLE);
		}
		// Update status
		if(txtStatus != null) {
			Log.i(tag, "updateStatus: status label");
			if(gameOver) {
				txtStatus.setTextColor(Color.RED);
				txtStatus.setText(stringGameOver);
			}
			else {
				txtStatus.setTextColor(Color.rgb(0xf0, 0xf0, 0xf0));
				txtStatus.setText(stringRunning + " " + N);
			}
			txtStatus.setVisibility(VISIBLE);
		}
		Log.i(tag, "updateStatus finished");
	}
	
	private void drawMove(SlotImageView from, SlotImageView mid, SlotImageView to) {
		Resources res = this.getContext().getResources();
		from.setImageDrawable(res.getDrawable(R.drawable.gray));
		//mid.setImageDrawable(res.getDrawable(R.drawable.gray));
		to.setImageDrawable(res.getDrawable(R.drawable.green));
		
		TransitionDrawable trans_mid = (TransitionDrawable) res.getDrawable(R.drawable.transition_remove);
		mid.setImageDrawable(trans_mid);
		trans_mid.startTransition(200);
		TransitionDrawable trans_mid2 = (TransitionDrawable) res.getDrawable(R.drawable.transition_remove2);
		mid.setImageDrawable(trans_mid2);
		trans_mid2.startTransition(350);
		//mid.setImageDrawable(res.getDrawable(R.drawable.gray));
	}
	
	private void drawStatusChange(SlotImageView slot, int status) {
		Resources res = this.getContext().getResources();
		if(status == Board.SLOT_IDLE) {
			slot.setImageDrawable(res.getDrawable(R.drawable.green));
		}
		else if(status == Board.SLOT_SELECTED) {
			slot.setImageDrawable(res.getDrawable(R.drawable.green2));
		}
		else if(status == Board.SLOT_HIGHLIGHED) {
			slot.setImageDrawable(res.getDrawable(R.drawable.grayvalid));
		}
		else if(status == Board.SLOT_EMPTY) {
			slot.setImageDrawable(res.getDrawable(R.drawable.gray));
		}
		else {
			slot.setImageDrawable(res.getDrawable(R.drawable.green));
		}
	}
	
	private void onTouch(SlotImageView siv) {
		if(gameOver) return;
		if(siv == null) return;
		
		XY newSelectedXY = new XY(siv.col, siv.row);
		int col, row;

		for(row = 0; row < N; row++) {
			for(col = 0; col <= row; col++) {
				if(!board.isEmpty(col, row) && board.getState(col, row) == Board.SLOT_SELECTED
						&& (siv.col != col || siv.row != row)) {
					SlotImageView tmpImg = imgSlotMap.get(new XY(col, row));
					assert(tmpImg != null);
					board.setState(col, row, Board.SLOT_IDLE);
					drawStatusChange(tmpImg, Board.SLOT_IDLE);
				}
			}
		}
		
		if(slotSelected != null) {
			XY oldSelectedXY = new XY(slotSelected.col, slotSelected.row);
			
			if(slotSelected.col == siv.col && slotSelected.row == siv.row) {
				// Touching the same slot
				board.setState(slotSelected.col, slotSelected.row, Board.SLOT_IDLE);
				drawStatusChange(slotSelected, Board.SLOT_IDLE);
				slotSelected = null;
			}
			else {
				// See if it's a valid move
				int dir = board.validMove(oldSelectedXY, newSelectedXY);
				if(dir >= 0) {
					// Make the move
					Log.i(tag, "onTouch: moving from " + slotSelected + " to " + newSelectedXY);
					XY mid = XY.middle(oldSelectedXY, newSelectedXY);
					SlotImageView imgMid = findSlotImage(mid.x, mid.y);
					assert(imgMid != null);
					//assert(board.isEmpty(newSelectedXY.x, newSelectedXY.y));
					//assert(!board.isEmpty(oldSelectedXY.x, oldSelectedXY.y));
					//assert(!board.isEmpty(mid.x, mid.y));
					// State will be cleared in this call
					board.makeMove(oldSelectedXY, newSelectedXY);
					// Play notification sound
					if(mainActivity != null) {
						if(board.isGameOver()) {
							gameOver = true;
							mainActivity.playGameoverSound();
							Log.i(tag, "onTouch: play gameover sound");
						}
						else {
							mainActivity.playClickSound();
							Log.i(tag, "onTouch: play sound");
						}
					}
					// Draw the movement
					drawMove(slotSelected, imgMid, siv);
					// Clear selection
					slotSelected = null;
					// See if it's game over
//					if(board.isGameOver()) {
//						gameOver = true;
//					}
				}
				else if(!board.isEmpty(newSelectedXY.x, newSelectedXY.y)) {
					// Change the selection
					board.setState(oldSelectedXY.x, oldSelectedXY.y, Board.SLOT_IDLE);
					board.setState(newSelectedXY.x, newSelectedXY.y, Board.SLOT_SELECTED);
					// Draw the selection change
					drawStatusChange(slotSelected, Board.SLOT_IDLE);
					drawStatusChange(siv, Board.SLOT_SELECTED);
					slotSelected = siv;
				}
			}
		}
		else if(!board.isEmpty(newSelectedXY.x, newSelectedXY.y)) {
			// No slot previously selected, save the selected slot
			slotSelected = siv;
			board.setState(siv.col, siv.row, Board.SLOT_SELECTED);
			// Draw the selection change
			drawStatusChange(siv, Board.SLOT_SELECTED);
		}
		
		for(row = 0; row < N; row++) {
			for(col = 0; col <= row; col++) {
				if(board.isEmpty(col, row) && board.getState(col, row) == Board.SLOT_HIGHLIGHED) {
					SlotImageView emptyImg = imgSlotMap.get(new XY(col, row));
					assert(emptyImg != null);
					board.setState(col, row, Board.SLOT_EMPTY);
					drawStatusChange(emptyImg, Board.SLOT_EMPTY);
				}
			}
		}
		// For selected checker, highlight the valid moves
		if(slotSelected != null) {
			ArrayList<XY> validMoves = board.getValidMoves(newSelectedXY.x, newSelectedXY.y);
			for(XY mv : validMoves) {
				assert(board.isEmpty(mv.x, mv.y));
				SlotImageView validImg = imgSlotMap.get(mv);
				assert(validImg != null);
				board.setState(mv.x, mv.y, Board.SLOT_HIGHLIGHED);
				drawStatusChange(validImg, Board.SLOT_HIGHLIGHED);
			}
		}
		this.invalidate();
	}
	
	public Bundle saveState() {
        Bundle map = new Bundle();

        map.putIntArray("board", board.boardToIntArray());

        return map;
    }
	
	public void restoreState(Bundle map) {
		if(board == null) {
			initBoard(5);
		}
		if(map != null) {
			assert(board != null);
			Log.i(tag, "restore state");
			board.intArrayToBoard(map.getIntArray("board"));
			gameOver = board.isGameOver();
			slotSelected = null;
			//computeCoords();
			//createImageViews();
		}
    }
	
	public void setLabels(TextView checkerCount, TextView status) {
		Log.i(tag, "setLabels");
		txtCheckerCount = checkerCount;
		txtStatus = status;
	}
	
	public void setButtons(Button replay, Button exit) {
		btnReplay = replay;
		btnExit = exit;
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		// TODO Auto-generated method stub
		updateStatus();
		super.onLayout(changed, l, t, r, b);
		
		Log.i(tag, "onLayout: " + l + ", " + t + ", " + r + ", " + b);
		Log.i(tag, "onLayout: getLeft=" + getLeft() + " getTop=" + getTop() + " getHeight=" + getHeight());
		
		if(changed) {
			viewHeight = b - t;
			viewWidth = r - l;
			Log.i(tag, "onLayout: width=" + viewWidth + " height=" + viewHeight);
	
			if(N == 0 || board == null) {
				Log.w(tag, "onLayout: board not initialized, create with default size 5");
				initBoard(5);
			}
			computeCoords();
			createImageViews();
			Iterator<SlotImageView> it = imgSlots.iterator();
			while(it.hasNext()) {
				SlotImageView img = it.next();
				XY slotPos = img.getCoord();
				img.layout(slotPos.x - checkerRadius, slotPos.y - checkerRadius, slotPos.x + checkerRadius, slotPos.y + checkerRadius);
				//img.layout(0, 0, 2 * checkerRadius, 2 * checkerRadius);
				//img.layout(l + boardArea.x1 + slotPos.x - checkerRadius, 
				//		t + boardArea.y1 + slotPos.y - checkerRadius,
				//		l + boardArea.x1 + slotPos.x + checkerRadius,
				//		t + boardArea.y1 + slotPos.y + checkerRadius);
				//Log.i(tag, "onLayout: image at " + (l + boardArea.x1 + slotPos.x) + ", " + (t + boardArea.y1 + slotPos.y));
				//img.setVisibility(INVISIBLE);
			}
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		// TODO Auto-generated method stub
		Log.i(tag, "onDraw");
		super.onDraw(canvas);
		
		updateImageViews();
		updateStatus();
	}

}
