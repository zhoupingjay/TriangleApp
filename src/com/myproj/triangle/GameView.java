package com.myproj.triangle;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {
	public static final String tag = "GameView";
	private static final int viewPadding = 4;

	private Board board;
	private int N = 0;			// Board size (# of checkers on each edge)
	private int D = 0;			// D: distance between centers of two slots
	private int checkerRadius = 0;			// R: radius of checkers (R < D/2)
	private Area boardArea = null;
	private int viewWidth, viewHeight;
	private RedrawHandler redrawHandler = new RedrawHandler();
	private SurfaceHolder surfaceHolder;
	private Paint paintOccupied, paintEmpty, paintSelected, paintHighlighted;
	private Bitmap imgOccupied, imgEmpty, imgSelected, imgHighlighed;
	private XY slotSelected = null;
	private boolean gameOver = false;

	private TextView txtCheckerCount, txtStatus;
	private Button btnReplay, btnExit;
	private Spinner gameSizeSelector;
	
	class RedrawHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            GameView.this.updateCanvas();
            GameView.this.invalidate();
        }

        public void sleep(long delayMillis) {
        	this.removeMessages(0);
            sendMessageDelayed(obtainMessage(0), delayMillis);
        }
    };
    
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

	public GameView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		surfaceHolder = getHolder();
		surfaceHolder.addCallback(this);
		paintOccupied = new Paint();
		paintEmpty = new Paint();
		paintSelected = new Paint();

		paintOccupied.setColor(Color.RED);
		paintOccupied.setAntiAlias(true);
		
		paintEmpty.setColor(Color.GRAY);
		paintEmpty.setAntiAlias(true);
		
		paintSelected.setColor(Color.WHITE);
		paintSelected.setAntiAlias(true);
		paintSelected.setStyle(Style.FILL);
		//paintSelected.setStrokeWidth(2);
		// board = new Board();
	}
	
	public void initBoard(int boardSize, boolean redraw) {
		if(boardSize >= Board.MinBoardSize && boardSize <= Board.MaxBoardSize) {
			N = boardSize;
			board = new Board(N);
			board.setEmpty(0, 0, true);
			if(redraw) {
				computeCoords();
				redrawHandler.sleep(0);
			}
		}
	}
	
	public void initBoard(int boardSize) {
		initBoard(boardSize, false);
	}
	
	public void computeCoords() {
		// Compute the size and distance of slots
		// Assuming D=3*R, (N-1)*D-2*R <= Width --> R = W/(3*N-1)
		if(N >= Board.MinBoardSize && N <= Board.MaxBoardSize && board != null) {
			int W = (viewWidth < viewHeight) ? viewWidth : (viewHeight - txtCheckerCount.getHeight() - btnReplay.getHeight());
			W -= (2 * viewPadding);
			checkerRadius = W / (3 * N - 1);
			D = (3 * checkerRadius) & 0xFFFFFFFE;		// D must be even number
			if(checkerRadius < 1 || D < 2) {
				Log.e(tag, "surface too small: R=" + checkerRadius + " D=" + D);
			}
			else {
				board.computeCoords(D, checkerRadius);
				int boardViewSize = Board.GetViewSize(N, D, checkerRadius);
				if(boardArea == null)
					boardArea = new Area();
				boardArea.x1 = (viewWidth - boardViewSize) / 2;
				boardArea.y1 = (viewHeight - boardViewSize) / 2 + txtCheckerCount.getHeight();
				boardArea.x2 = boardArea.x1 + boardViewSize;
				boardArea.y2 = boardArea.y1 + boardViewSize;
				Log.i(tag, "N=" + N + ", D=" + D + ", R=" + checkerRadius);
				Log.i(tag, "boardArea: " + boardArea.toString());
				loadBitmaps();
			}
		}
	}
	
	private void loadBitmaps() {
		Resources r = this.getContext().getResources();
		
		imgOccupied = loadBitmap(r.getDrawable(R.drawable.green));
		imgEmpty = loadBitmap(r.getDrawable(R.drawable.gray));
		imgHighlighed = loadBitmap(r.getDrawable(R.drawable.green));
		imgSelected = loadBitmap(r.getDrawable(R.drawable.green2));
		// imgEmpty = Bitmap.createBitmap(2*R, 2*R, Bitmap.Config.ARGB_8888);
		// imgHighlighed = Bitmap.createBitmap(2*R, 2*R, Bitmap.Config.ARGB_8888);
		//imgSelected = Bitmap.createBitmap(2*R, 2*R, Bitmap.Config.ARGB_8888);
	}
	
	private Bitmap loadBitmap(Drawable res) {
        Bitmap bitmap = Bitmap.createBitmap(2*checkerRadius, 2*checkerRadius, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        res.setBounds(0, 0, 2*checkerRadius, 2*checkerRadius);
        res.draw(canvas);
        return bitmap;
    }
	
	// Draw the board on canvas
	private void updateCanvas() {
		Canvas c = null;
		try {
			c = surfaceHolder.lockCanvas(null);
			synchronized (surfaceHolder) {
				doDraw(c);
			}
		} finally {
			if (c != null)
				surfaceHolder.unlockCanvasAndPost(c);
		}
	}
	
	private void doDraw(Canvas canvas) {
		int row, col;
		// Clear the entire canvas
		canvas.save();
		canvas.drawARGB(255, 0, 0, 0);
		canvas.restore();

		canvas.save();
		for(row = 0; row < N; row++) {
			for(col = 0; col <= row; col++) {
				XY slotPos = board.getSlotCoord(col, row);
				float cx = (float)(slotPos.x + boardArea.x1);
				float cy = (float)(slotPos.y + boardArea.y1);
				if(board.isEmpty(col, row)) {
					//Log.i(tag, "circle at (" + cx + ", " + cy + ") radius=" + checkerRadius);
					//canvas.drawCircle(cx, cy, (float)checkerRadius, paintEmpty);
					canvas.drawBitmap(imgEmpty, cx - checkerRadius, cy - checkerRadius, null);
				}
				else {
					//Log.i(tag, "circle at (" + cx + ", " + cy + ") radius=" + checkerRadius);
					int state = board.getState(col, row);
					if(state == Board.SLOT_IDLE) {
						//canvas.drawCircle(cx, cy, (float)checkerRadius, paintOccupied);
						canvas.drawBitmap(imgOccupied, cx - checkerRadius, cy - checkerRadius, null);
					}
					else if(state == Board.SLOT_SELECTED) {
						//canvas.drawCircle(cx, cy, (float)checkerRadius, paintSelected);
						canvas.drawBitmap(imgSelected, cx - checkerRadius, cy - checkerRadius, null);
					}
					else if(state == Board.SLOT_HIGHLIGHED) {
						//canvas.drawCircle(cx, cy, (float)checkerRadius, paintHighlighted);
						canvas.drawBitmap(imgHighlighed, cx - checkerRadius, cy - checkerRadius, null);
					}
				}
			}
		}
		canvas.restore();
		
		// Update remaining slots
		if(txtCheckerCount != null) {
			txtCheckerCount.setText("Checkers: " + Integer.toString(board.countRemaining()));
		}
		// Update status
		if(txtStatus != null) {
			if(gameOver) {
				txtStatus.setTextColor(Color.RED);
				txtStatus.setText("Game Over");
			}
			else {
				txtStatus.setTextColor(Color.rgb(0xf0, 0xf0, 0xf0));
				txtStatus.setText("Board size: " + N);
			}
			txtStatus.setVisibility(VISIBLE);
		}
		Log.i(tag, "doDraw finished");
	}
	
	private void onTouch(int x, int y) {
		if(gameOver) return;
		if(!boardArea.isIn(x, y)) return;
		
		XY touchCoord = new XY(x - boardArea.x1, y - boardArea.y1);
		XY newSelected = board.getTouchedSlot(touchCoord);

		if(slotSelected != null && newSelected != null) {
			if(slotSelected.sameAs(newSelected)) {
				// Touching the same slot
				board.setState(slotSelected.x, slotSelected.y, Board.SLOT_IDLE);
				slotSelected = null;
			}
			else {
				// See if it's a valid move
				int dir = board.validMove(slotSelected, newSelected);
				if(dir >= 0) {
					// Make the move
					Log.i(tag, "onTouch: moving from " + slotSelected + " to " + newSelected);
					XY mid = XY.middle(slotSelected, newSelected);
					assert(board.isEmpty(newSelected.x, newSelected.y));
					assert(!board.isEmpty(slotSelected.x, slotSelected.y));
					assert(!board.isEmpty(mid.x, mid.y));
					// State will be cleared in this call
					board.makeMove(slotSelected, newSelected);
					slotSelected = null;
					// See if it's game over
					if(board.isGameOver()) {
						gameOver = true;
					}
				}
				else {
					// Change the selection
					board.setState(slotSelected.x, slotSelected.y, Board.SLOT_IDLE);
					board.setState(newSelected.x, newSelected.y, Board.SLOT_SELECTED);
					slotSelected = newSelected;
				}
			}
		}
		else if(newSelected != null && slotSelected == null) {
			// Save the selected slot
			slotSelected = newSelected;
			board.setState(newSelected.x, newSelected.y, Board.SLOT_SELECTED);
		}
		else if(newSelected == null && slotSelected != null){
			// Cancel the current selection
			board.setState(slotSelected.x, slotSelected.y, Board.SLOT_IDLE);
			slotSelected = null;
		}
		redrawHandler.sleep(0);
	}
	
	public Bundle saveState() {
        Bundle map = new Bundle();

        map.putIntArray("board", board.boardToIntArray());

        return map;
    }
	
	public void restoreState(Bundle map) {
		board.intArrayToBoard(map.getIntArray("board"));
		computeCoords();
    }
	
	public void setLabels(TextView checkerCount, TextView status) {
		txtCheckerCount = checkerCount;
		txtStatus = status;
	}
	
	public void setButtons(Spinner gamesize, Button replay, Button exit) {
		gameSizeSelector = gamesize;
		btnReplay = replay;
		btnExit = exit;
	}
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub
		Log.i(tag, "surfaceChanged: width=" + width + " height=" + height);
		viewWidth = width;
		viewHeight = height;
		// initBoard(5);
		if(N == 0 || board == null) {
			Log.w(tag, "surfaceChanged: board not initialized, create with default size 5");
			initBoard(5);
		}
		computeCoords();
		if(txtCheckerCount != null && board != null) {
			txtCheckerCount.setText("Checkers: " + Integer.toString(board.countRemaining()));
			txtCheckerCount.setVisibility(VISIBLE);
		}
		
		redrawHandler.sleep(0);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		Log.i(tag, "surfaceCreated");
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		Log.i(tag, "surfaceDestroyed");
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		int tx = (int)event.getX();
		int ty = (int)event.getY();
		Log.i(tag, "onTouchEvent: X=" + tx + " Y=" + ty);
		onTouch(tx, ty);
		return super.onTouchEvent(event);
	}

}
