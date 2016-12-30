package com.myproj.triangle;

import java.util.ArrayList;
import android.util.Log;

class XY {
	public int x, y;

	public XY() {
		x = y = 0;
	}
	public XY(int _x, int _y) {
		x = _x;
		y = _y;
	}
	public String toString() {
		return "(" + x + ", " + y + ")";
	}
	public boolean sameAs(XY a) {
		if(a != null) {
			return (x == a.x && y == a.y);
		}
		else return false;
	}
	public static XY middle(XY a, XY b) {
		return new XY((a.x + b.x)/2, (a.y + b.y)/2);
	}
	
	public static double distance(XY a, XY b) {
		return Math.sqrt((a.x - b.x)*(a.x - b.x) + (a.y-b.y)*(a.y-b.y));
	}
	@Override
	public boolean equals(Object o) {
		if(this == o) return true;
		if(o == null) return false;
		if(!(o instanceof XY)) return false;
		XY that = (XY)o;
		return (x == that.x && y == that.y);
	}
	@Override
	public int hashCode() {
		return (x * 16 + y);
	}
}

class Slot {
	public int x, y;			// For drawing (not the index to slots[][]!)
	public boolean empty;
	public int state;
	
	public Slot() {
		x = y = 0;
		empty = false;
		state = Board.SLOT_IDLE;
	}
}

public class Board {
	public static final int MinBoardSize = 5; 
	public static final int MaxBoardSize = 8;
	static final String tag = "Board";
	public static final double sine60 = 0.8660254;
	
	public static final int SLOT_IDLE = 0;
	public static final int SLOT_SELECTED = 1;
	public static final int SLOT_HIGHLIGHED = 2;
	public static final int SLOT_EMPTY = 3;
	
	/*
	 * LU(-1, -1)        RU(0, -1)
	 * L(-1, 0)             *             R(1, 0)
	 *                   LD(0, 1)         RD(1, 1) 
	 */
	public static final int Moves_X[] = {-1,  0, 1, 1, 0, -1};
	public static final int Moves_Y[] = {-1, -1, 0, 1, 1,  0};
	public static final int MOVE_LU = 0;
	public static final int MOVE_RU = 1;
	public static final int MOVE_R  = 2;
	public static final int MOVE_RD = 3;
	public static final int MOVE_LD = 4;
	public static final int MOVE_L  = 5;
	public static final int NumMoveDirs = 6;
	
	public static int GetViewSize(int N, int D, int R) {
		return (N-1) * D + 2 * R;
	}
	
	int boardSize;		// How many slots on each edge (N)
	int slotDist;		// Distance between centers of adjacent slots (D)
	int checkerRadius;	// Radius of the checkers (R)
	Slot slots[][];

	public Board(int N) {
		assert(N <= MaxBoardSize && N >= MinBoardSize);
		boardSize = N;
		slots = new Slot[MaxBoardSize][MaxBoardSize];
		for(int i = 0; i < MaxBoardSize; i++) {
			for(int j = 0; j < MaxBoardSize; j++) {
				slots[i][j] = new Slot();
			}
		}
	}
	
	public boolean indexValid(int col, int row) {
		if(col < 0 || row < 0 || col >= boardSize || row >= boardSize || col > row) { 
			return false;
		}
		else return true;
	}
	
	// Compute positions of each slot (center) for drawing purpose 
	public void computeCoords(int D, int R) {
		assert((D & 0x1) == 0);					// D must be even number
		assert(D > 0 && R > 0 && D > R*2);

		slotDist = D;
		checkerRadius = R;

		int row, col;
		int x_off = 0;
		int y_gap = (int)((double)slotDist * sine60 + 0.5);
		Log.i(tag, "computeCoords: x_off=" + x_off + " y_gap=" + y_gap);

		for(row = boardSize - 1; row >= 0; row--) {
			for(col = 0; col <= row; col++) {
				slots[row][col].x = x_off + R + (D * col);
				slots[row][col].y = y_gap * row + R;
			}
			x_off += (D / 2);
		}
	}
	
	public void setEmpty(int col, int row, boolean empty) {
		if(indexValid(col, row)) {
			slots[row][col].empty = empty;
			slots[row][col].state = SLOT_EMPTY;
		}
		else {
			Log.w(tag, "setEmpty: invalid index " + col + "," + row);
		}
	}
	
	public boolean isEmpty(int col, int row) {
		assert(indexValid(col, row));
		return slots[row][col].empty;
	}
	
	public void setState(int col, int row, int state) {
		if(indexValid(col, row)) {
			slots[row][col].state = state; 
		}
		else {
			Log.w(tag, "setEmpty: invalid index " + col + "," + row);
		}
	}
	
	public int getState(int col, int row) {
		assert(indexValid(col, row));
		return slots[row][col].state;
	}
	
	public Slot getSlot(int col, int row) {
		assert(indexValid(col, row));
		return slots[row][col];
	}
	
	public XY getSlotCoord(int col, int row) {
		assert(indexValid(col, row));
		return new XY(slots[row][col].x, slots[row][col].y);
	}
	
	public ArrayList<XY> getAdjacentSlots(int col, int row) {
		assert(indexValid(col, row));
		
		ArrayList<XY> adjSlots = new ArrayList<XY>();
		for(int i = 0; i < NumMoveDirs; i++) {
			int ncol, nrow;
			ncol = col + Moves_X[i];
			nrow = row + Moves_Y[i];
			if(indexValid(ncol, nrow)) {
				adjSlots.add(new XY(ncol, nrow));
			}
		}
		
		return adjSlots;
	}
	
	public ArrayList<XY> getValidMoves(int col, int row) {
		assert(indexValid(col, row));
		assert(!isEmpty(col, row));

		ArrayList<XY> validMoves = new ArrayList<XY>();
		for(int i = 0; i < NumMoveDirs; i++) {
			int ncol, nrow;
			int ncol2, nrow2;
			ncol = col + Moves_X[i];
			nrow = row + Moves_Y[i];
			ncol2 = ncol + Moves_X[i];
			nrow2 = nrow + Moves_Y[i];
			if(indexValid(ncol, nrow) && indexValid(ncol2, nrow2)) {
				if(!isEmpty(ncol, nrow) && isEmpty(ncol2, nrow2)) {
					validMoves.add(new XY(ncol2, nrow2));
				}
			}
		}
		return validMoves;
	}
	
	// Return a boolean vector indicating validness of each direction
	public boolean[] getValidMoveDirs(int col, int row) {
		assert(indexValid(col, row));
		assert(!isEmpty(col, row));

		boolean validDirs[] = new boolean[NumMoveDirs]; 
		for(int i = 0; i < NumMoveDirs; i++) {
			int ncol, nrow;
			int ncol2, nrow2;
			ncol = col + Moves_X[i];
			nrow = row + Moves_Y[i];
			ncol2 = ncol + Moves_X[i];
			nrow2 = nrow + Moves_Y[i];
			validDirs[i] = false;
			if(indexValid(ncol, nrow) && indexValid(ncol2, nrow2)) {
				if(!isEmpty(ncol, nrow) && isEmpty(ncol2, nrow2)) {
					validDirs[i] = true; 
				}
			}
		}
		return validDirs;
	}
	
	// Return the move direction (0~6), or -1 if invalid
	public int validMove(XY from, XY to) {
		if(!indexValid(from.x, from.y) || !indexValid(to.x, to.y))
			return -1;
		for(int i = 0; i < NumMoveDirs; i++) {
			if(isEmpty(to.x, to.y) && !isEmpty(from.x, from.y) &&
					(to.x - from.x) == (Moves_X[i]*2) && (to.y - from.y) == (Moves_Y[i]*2)) {
				XY mid = XY.middle(from, to);
				if(!isEmpty(mid.x, mid.y))
					return i;
			}
		}
		return -1;
	}

	public void makeMove(XY from, XY to) {
		assert(validMove(from, to) >= 0);

		XY mid = XY.middle(from, to);
		setEmpty(from.x, from.y, true);
		setEmpty(mid.x, mid.y, true);
		setEmpty(to.x, to.y, false);
		setState(from.x, from.y, SLOT_IDLE);
		setState(to.x, to.y, SLOT_IDLE);
		setState(mid.x, mid.y, SLOT_IDLE);
	}
	
	public int countRemaining() {
		int col, row, remain = 0;
		for(row = boardSize - 1; row >= 0; row--) {
			for(col = 0; col <= row; col++) {
				if(!isEmpty(col, row))
					remain++;
			}
		}
		return remain;
	}
	
	// Check if game is over (no more possible move)
	public boolean isGameOver() {
		int col, row;
		for(row = boardSize - 1; row >= 0; row--) {
			for(col = 0; col <= row; col++) {
				if(!isEmpty(col, row) && !getValidMoves(col, row).isEmpty()) {
					return false;
				}
			}
		}
		return true;
	}
	
	// Return: row and column of the slot or null if not found
	public XY getTouchedSlot(XY touchCoord) {
		int row, col;
		XY slotCenter = new XY();
		for(row = boardSize - 1; row >= 0; row--) {
			for(col = 0; col <= row; col++) {
				slotCenter.x = slots[row][col].x;
				slotCenter.y = slots[row][col].y;
				if(XY.distance(touchCoord, slotCenter) <= (double)checkerRadius) {
					return new XY(col, row);
				}
			}
		}
		return null;
	}

	// Return: integer array containing board size and slot emptiness 
	public int[] boardToIntArray() {
		int size = (boardSize + 1) * boardSize / 2 + 1;
		int[] rawArray = new int[size];
		int row, col, idx;
		
		rawArray[0] = boardSize;
		idx = 1;
		for(row = boardSize - 1; row >= 0; row--) {
			for(col = 0; col <= row; col++) {
				rawArray[idx] = (slots[row][col].empty ? 0 : 1);
				idx++;
			}
		}
		return rawArray;
	}
	
	public void intArrayToBoard(int[] rawArray) {
		if(rawArray == null || rawArray.length < 1) {
			Log.e(tag, "intArrayToBoard: invalid data");
			return;
		}
		boardSize = rawArray[0];
		int row, col, idx;
		idx = 1;
		for(row = boardSize - 1; row >= 0; row--) {
			for(col = 0; col <= row; col++) {
				slots[row][col].empty = (rawArray[idx] == 0);
				slots[row][col].state = SLOT_IDLE;
				idx++;
			}
		}
		// Note: D and R is not changed, need to recompute after this!
	}
}
