package com.jc.gyromorse;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.inputmethodservice.KeyboardView;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.TableLayout;
import android.widget.TextView;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@SuppressLint("ClickableViewAccessibility")
public class DotDashKeyboardView extends KeyboardView {

	private String TAG = this.getClass().getSimpleName();

	private DotDashIMEService service;
	private Dialog cheatsheetDialog;
	private TableLayout cheatsheet1;
	private TableLayout cheatsheet2;
	private int mSwipeThreshold;
	private GestureDetector gestureDetector;
	
	private Set<Key> pressedKeys = new HashSet<Key>();

	public static final int KBD_NONE = 0;
	public static final int KBD_DOTDASH = 1;
	public static final int KBD_UTILITY = 2;

	public boolean mEnableUtilityKeyboard = true;

	public boolean singletap = true;

    private static final int REPEAT_INTERVAL = 50; // ~20 keys per second
    private static final int REPEAT_START_DELAY = 400;
    private static final int LONGPRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout();
    private static final int DEBOUNCE_TIMEOUT = 50; //70;
    private static final int IAMBIC_DOTLENGTH = 100;
	public static final long AUTOCOMMIT_DELAY = IAMBIC_DOTLENGTH * 4;
    // TODO: Make this into a SparseArray somehow?
    public Map<Key, Long> bouncewaits = new HashMap<Key, Long>();

    private static final int MSG_KEY_REPEAT = 1;
    public static final int MSG_IAMBIC_PLAYING = 2;
	public static final int MSG_AUTOCOMMIT = 3;
    // TODO: according to this documentation: http://www.morsecode.nl/iambic.PDF
    // ... it appears that the logic is supposed to be that it "locks" if the
    // opposite key is still held down at the halfway point of the preceeding
    // signal. So I will need to add some more logic there to get the timing
    // just right.
    boolean iambic_both_pressed = false;

    Handler handler = new Handler(
		new Handler.Callback() {

			@Override
			public boolean handleMessage(Message msg) {
				switch (msg.what) {
					case MSG_KEY_REPEAT:
						Keyboard.Key repeatkey = (Keyboard.Key) msg.obj;
						if (!repeatkey.pressed) {
							return true;
						}

						if (repeatkey == service.dotDashKeyboard.leftDotdashKey || repeatkey == service.dotDashKeyboard.rightDotdashKey) {

						} else {
							getOnKeyboardActionListener().onKey(repeatkey.codes[0], repeatkey.codes);
							handler.sendMessageDelayed(handler.obtainMessage(MSG_KEY_REPEAT, repeatkey), REPEAT_INTERVAL);
						}
						break;
					case MSG_IAMBIC_PLAYING:
						Keyboard.Key lastkeysent = (Keyboard.Key) msg.obj;
						Keyboard.Key nextkeytosend = null;
						boolean leftkeypressed = service.dotDashKeyboard.leftDotdashKey.pressed;
						boolean rightkeypressed = service.dotDashKeyboard.rightDotdashKey.pressed;

						// Iambic signal has just ended. Check to see if dot and/or dash are still held down
						if (leftkeypressed && rightkeypressed) {
							// Both are pressed, so send the opposite signal from what we just sent
							if (lastkeysent == service.dotDashKeyboard.leftDotdashKey) {
								nextkeytosend = service.dotDashKeyboard.rightDotdashKey;
							} else {
								nextkeytosend = service.dotDashKeyboard.leftDotdashKey;
							}
						} else if (leftkeypressed || rightkeypressed) {
							// Only one is pressed. Send its signal.
							if (leftkeypressed) {
								nextkeytosend = service.dotDashKeyboard.leftDotdashKey;
							} else {
								nextkeytosend = service.dotDashKeyboard.rightDotdashKey;
							}
							iambic_both_pressed = false;
						} else if (service.iambicmodeb && iambic_both_pressed) {
							// Mode b. Send one more signal, with the opposite of the last key
							if (lastkeysent == service.dotDashKeyboard.leftDotdashKey) {
								nextkeytosend = service.dotDashKeyboard.rightDotdashKey;
							} else {
								nextkeytosend = service.dotDashKeyboard.leftDotdashKey;
							}
							iambic_both_pressed = false;
						}

						if (nextkeytosend != null) {
							getOnKeyboardActionListener().onKey(nextkeytosend.codes[0], nextkeytosend.codes);
							handler.sendMessageDelayed(handler.obtainMessage(MSG_IAMBIC_PLAYING, nextkeytosend),
									DotDashKeyboardView.get_iambic_delay(nextkeytosend));
						} else {
							// Iambic is done, so start the autocommit timer.
							if (service.autocommit) {
								long delay = DotDashKeyboardView.AUTOCOMMIT_DELAY;
								// If audio is playing, we want to wait until the end of the tone before
								// we start counting down for autocommit.
								if (service.isAudio()) {
									delay += DotDashKeyboardView.get_iambic_delay(lastkeysent);
								}
								handler.removeMessages(DotDashKeyboardView.MSG_AUTOCOMMIT);
								handler.sendMessageDelayed(
										handler.obtainMessage(DotDashKeyboardView.MSG_AUTOCOMMIT),
										delay
								);
							}
						}
						break;
					case MSG_AUTOCOMMIT:
						service.commitCodeGroup(true);
						break;
				}

				return true;
			}
		}
	);

	public void setService(DotDashIMEService service) {
		this.service = service;
	}

	public DotDashKeyboardView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setEverythingUp();
	}

	public DotDashKeyboardView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setEverythingUp();
	}

	@SuppressWarnings("deprecation")
	private void setEverythingUp() {
		mSwipeThreshold = (int) (300 * getResources().getDisplayMetrics().density);
		setPreviewEnabled(false);
		gestureDetector = new GestureDetector(
				new GestureDetector.SimpleOnGestureListener() {

					/**
					 * This function mostly copied from LatinKeyboardBaseView in
					 * the Hacker's Keyboard project: http://code.google.com/p/hackerskeyboard/
					 *
				 	 * Copyright (C) 2010, authors of the Hacker's Keyboard project: http://code.google.com/p/hackerskeyboard/
				 	 * Copyright (c) 2011, Aaron Wells
					 *
					 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
					 * use this file except in compliance with the License. You may obtain a copy of
					 * the License at
					 *
					 * http://www.apache.org/licenses/LICENSE-2.0
					 */
					@Override
					public boolean onFling(MotionEvent e1, MotionEvent e2,
                                           float velocityX, float velocityY) {

						// If they swipe up off the keyboard, launch the cheat
						// sheet. This was originally a check for e2.getY() < 0,
						// but that didn't work in ICS. Possibly ICS stops
						// sending you events after you go past the edge of the
						// window. So I changed it to 10 instead.
						if (e2.getY() <= 10) {
							// If they swipe up off the keyboard, launch the
							// cheat sheet
							service.handleshift();
							Log.e(TAG, "here here");
							return true;
						} else  {
							final float absX = Math.abs(velocityX);
							final float absY = Math.abs(velocityY);
							float deltaX = e2.getX() - e1.getX();
							int travelMin = Math.min((getWidth() / 3),
									(getHeight() / 3));

							if (velocityX > mSwipeThreshold && absY < absX
									&& deltaX > travelMin) {
								//showCheatSheet();

								service.handleSpace();
								return true;
							} else if (velocityX < -mSwipeThreshold
									&& absY < absX && deltaX < -travelMin) {
								service.handleBackspace();
								Log.e(TAG, "here 2here");

								//service.logdata();

								return true;
							}
						}
						return false;
					}

					public boolean onSingleTapConfirmed(MotionEvent e) {
						//Log.d(TAG, "singleTap");
						if (singletap==true)
							service.handledotkey();
						else
							service.handleddkey();
						return true;
					}

					public boolean onDoubleTap(MotionEvent e) {
						//Log.d(TAG, "doubleTap");
						if (singletap==true)
								service.handledashkey();
						else
								service.handdoublekey();

						return true;
					}

				});

//
	}




	/**
	 * Updates the newline code printed in the cheat sheet, based on the user's
	 * current preference.
	 */
	public void updateNewlineCode() {
		if (cheatsheet2 == null) {
			return;
		}

		String newCode = service.getText(R.string.newline_disabled).toString();
		if (service.newlineGroups != null && service.newlineGroups.length > 0) {
			newCode = service.newlineGroups[0].replace(".", DotDashIMEService.UNICODE_DOT).replace("-",  DotDashIMEService.UNICODE_DASH);
		}
		((TextView) cheatsheet2.findViewById(R.id.newline_code))
				.setText(newCode);
	}



	@Override
	protected boolean onLongPress(Key key) {
		//shiftKey.icon = shiftIcon;
		//super.setShifted(true);
/*		if (singletap==true)
				singletap=false;
		else
			singletap=true;*/


		//if(key.codes[0]==keybo)

		return super.onLongPress(key);

	}

	@Override
	public boolean onTouchEvent(MotionEvent me) {
		Log.d(TAG, "onTouchEvent");

		// TODO: Unfortunately, since I send the character when you first press
		// the key, all the keys you press while swiping still count as getting
		// pressed.
		//
		// Not sure what I could do about that... maybe a more sensitive
		// swipe detector?
		if (gestureDetector.onTouchEvent(me)) {
			for(Keyboard.Key k : pressedKeys) {
				k.onReleased(false);
			}
			invalidateAllKeys();
			pressedKeys.clear();
			return true;
		}

		// Let KeyboardView handle the utility keyboard.
/*		if (whichKeyboard() == DotDashKeyboardView.KBD_UTILITY) {
			return super.onTouchEvent(me);
		}*/

		int actionmasked = me.getActionMasked();
		int actionindex = me.getActionIndex();
		Set<Key> curPressedKeys = new HashSet<Key>();

		for (int i=0; i < me.getPointerCount(); i++) {

			// Find out which key the pointer is on
			int x = (int) me.getX(i);
			int y = (int) me.getY(i);
			int[] keys = service.dotDashKeyboard.getNearestKeys(x, y);
			Keyboard.Key touchedKey = null;
			for (int k : keys) {
				Keyboard.Key key = service.dotDashKeyboard.getKeys().get(k);
				// TODO: This continues to detect it even after you've moved off the keyboard. :-P
				if (key.isInside(x, y)) {
					touchedKey = key;
				}
			}

			if (touchedKey != null) {
				if (i == actionindex) {
					switch (actionmasked) {
						case MotionEvent.ACTION_DOWN:
						case MotionEvent.ACTION_MOVE:
						case MotionEvent.ACTION_POINTER_DOWN:
							curPressedKeys.add(touchedKey);
							break;
						case MotionEvent.ACTION_UP:
						case MotionEvent.ACTION_POINTER_UP:
						case MotionEvent.ACTION_OUTSIDE:
						// TODO: The docs say about ACTION_CANCEL: "You should treat this as an
						// up event, but not perform any action that you normally would".
						// So to really do that I'll need to put some further logic into this.
						// (How do you cancel a keypress?)
						case MotionEvent.ACTION_CANCEL:
							curPressedKeys.remove(touchedKey);
							break;
					}
				} else {
					curPressedKeys.add(touchedKey);
				}
			}
		}

		// Now that we know which keys have fingers on 'em this time,
		// let's check to see how that has changed from last time.

		// Keys that are in curPressedKeys but not in pressedKeys
		// are newly pressed.
		Set<Key> newlyPressed = new HashSet<Key>(curPressedKeys);
		newlyPressed.removeAll(pressedKeys);
		for (Keyboard.Key k : newlyPressed) {
			if (k.pressed) {
				continue;
			}
			Long bouncewait = this.bouncewaits.get(k);
			if (bouncewait != null && bouncewait > SystemClock.elapsedRealtime()) {
				continue;
			}

//			k.onPressed();
			k.pressed = true;

			getOnKeyboardActionListener().onPress(k.codes[0]);

			if (service.iambic && (k == service.dotDashKeyboard.leftDotdashKey || k == service.dotDashKeyboard.rightDotdashKey)) {
				// In iambic mode, we only process the key if there's not already a signal going
				// What matters is in the message handler, where we check the state of the keys
				// when the current message ends.
				//
				// TODO: Actually... it might make more sense if the iambic timing was
				// over in DotDashIMEService, using the onPress() method to trigger it.
				if (!handler.hasMessages(MSG_IAMBIC_PLAYING)) {
					getOnKeyboardActionListener().onKey(k.codes[0], k.codes);

					handler.sendMessageDelayed(
							handler.obtainMessage(MSG_IAMBIC_PLAYING, k),
							DotDashKeyboardView.get_iambic_delay(k)
					);
				}

				// Iambic mode B needs to know if both keys got pressed simultaneously while an Iambic message
				// was in progress.
				if (service.iambicmodeb && service.dotDashKeyboard.leftDotdashKey.pressed && service.dotDashKeyboard.rightDotdashKey.pressed) {
					this.iambic_both_pressed = true;
				}
			} else {
				getOnKeyboardActionListener().onKey(k.codes[0], k.codes);

				if (k.repeatable && !handler.hasMessages(MSG_KEY_REPEAT, k)) {
					handler.sendMessageDelayed(
							handler.obtainMessage(MSG_KEY_REPEAT, k),
							REPEAT_START_DELAY
					);
				}
			}

			invalidateKey(service.dotDashKeyboard.getKeys().indexOf(k));
		}

		// Keys that are in pressedKeys but not curPressedKeys
		// are newly released.
		Set<Key> newlyReleased = new HashSet<Key>(pressedKeys);
		newlyReleased.removeAll(curPressedKeys);
		for (Keyboard.Key k : newlyReleased) {
			if (!k.pressed) {
				continue;
			}

			k.pressed = false;
			getOnKeyboardActionListener().onRelease(k.codes[0]);
			if (k.repeatable) {
				handler.removeMessages(MSG_KEY_REPEAT, k);
			}
			this.bouncewaits.put(k, SystemClock.elapsedRealtime() + DotDashKeyboardView.DEBOUNCE_TIMEOUT);
			invalidateKey(service.dotDashKeyboard.getKeys().indexOf(k));

			// If we're not in iambic mode, then the release of a key is probably a decent time to start
			// the autocommit timer.
			if (
					!service.iambic
					&& service.isAudio()
					&& (k == service.dotDashKeyboard.leftDotdashKey || k == service.dotDashKeyboard.rightDotdashKey)
					&& service.dotDashKeyboard.leftDotdashKey.pressed == false
					&& service.dotDashKeyboard.rightDotdashKey.pressed == false
			) {
				long delay = DotDashKeyboardView.AUTOCOMMIT_DELAY;
				// If audio is playing, we want to wait until the end of the tone before
				// we start counting down for autocommit.
				if (service.isAudio()) {
					delay += DotDashKeyboardView.get_iambic_delay(k);
				}
				handler.removeMessages(DotDashKeyboardView.MSG_AUTOCOMMIT);
				handler.sendMessageDelayed(
						handler.obtainMessage(DotDashKeyboardView.MSG_AUTOCOMMIT),
						delay
				);
			}
		}

		pressedKeys = curPressedKeys;

		for (Keyboard.Key k : service.dotDashKeyboard.getKeys()) {
			Log.d(TAG, "Key " + String.valueOf(k.codes[0]) + " " + (k.pressed ? "down" : "up"));
		}

		return true;
	}

	public static long get_iambic_delay(Keyboard.Key k) {
		if (k.codes[0] == DotDashKeyboard.KEYCODE_DOT) {
			// a dot and the space after it
			return IAMBIC_DOTLENGTH * 2;
		} else {
			// a dash (three dot lengths) and the space after it
			return IAMBIC_DOTLENGTH * 4;
		}
	}
}
