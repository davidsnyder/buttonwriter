package com.buttonlabs.buttonwriter;

import java.io.IOException;
//import java.nio.charset.Charset;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.View.OnClickListener;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.buttonlabs.buttonwriter.R;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseObject;

public class WriterActivity extends Activity implements OnClickListener {
	
	private String BUTTON_APP_PACKAGE_NAME = "com.antares.nfc"; // replace with button app package name
	
	private NfcAdapter mAdapter;
	private boolean mInWriteMode;
	private Button mWriteTagButton;
	private TextView mTextView;
	
	/** Called when the activity is first created. */
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.main);
		
	       // grab our NFC Adapter
        mAdapter = NfcAdapter.getDefaultAdapter(this);
        
        // button that starts the tag-write procedure
        mWriteTagButton = (Button)findViewById(R.id.write_tag_button);
        mWriteTagButton.setOnClickListener(this);
        
        // TextView that we'll use to output messages to screen
        mTextView = (TextView)findViewById(R.id.text_view);
	}
	
	public void onClick(View v) {
		if(v.getId() == R.id.write_tag_button) {
			mTextView.setText("Touch and hold button against phone to write.");
			enableWriteMode();
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		mAdapter.disableForegroundDispatch(this); // disableWriteMode()
	}
	
	/**
	 * Called when our blank tag is scanned executing the PendingIntent
	 */
	@Override
    public void onNewIntent(Intent intent) {
		if(mInWriteMode) {
			mInWriteMode = false;
			
			// write to newly scanned tag
			Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			
			//TODO Skip if tag is already written?
			writeTag(tag);
		}
    }
	
	/**
	 * Force this Activity to get NFC events first
	 */
	private void enableWriteMode() {
		mInWriteMode = true;
		
		// set up a PendingIntent to open the app when a tag is scanned
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
            new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        IntentFilter[] filters = new IntentFilter[] { tagDetected };
        
		mAdapter.enableForegroundDispatch(this, pendingIntent, filters, null);
	}

	/**
	 * 
	 * @param color 
	 * @return unsaved Button ParseObject
	 */
	private ParseObject getNewButton(int color) {
		ParseObject button = new ParseObject("Button");
		ParseACL writeAcl = new ParseACL();
		writeAcl.setPublicWriteAccess(true);
		writeAcl.setPublicReadAccess(true);  //this should be limited to only users that have bumped the button?
		button.setACL(writeAcl);
		button.put("color", color);
		button.put("isBurned", false);
		return button;
	}
	
	private ParseObject updateIsBurned(ParseObject button) {
		button.put("isBurned", true);
		ParseACL readAcl = new ParseACL();
		readAcl.setPublicReadAccess(true);
		button.setACL(readAcl);
		try {
			button.save();
		} catch (ParseException e1) {
			mTextView.setText("Failed to update isBurned on Parse Server");
			e1.printStackTrace();
		}
	 return button;	
	}
	
	/**
	 * Format a tag and write our NDEF message
	 */
	private boolean writeTag(Tag tag) {
		// launches Play Store if app is not installed
		NdefRecord appRecord = NdefRecord.createApplicationRecord(BUTTON_APP_PACKAGE_NAME); 
		ParseObject button = getNewButton(0); // All buttons are color 0 for now
		try {
			button.save();
		} catch (ParseException e1) {
			mTextView.setText("Failed to getNewButton from Parse Server");
			e1.printStackTrace();
		}

		NdefRecord buttonRecord = NdefRecord.createUri(button.getObjectId());
		NdefMessage message = new NdefMessage(new NdefRecord[] { buttonRecord, appRecord});
        
		try {
			// see if tag is already NDEF formatted
			Ndef ndef = Ndef.get(tag);
			if (ndef != null) {
				ndef.connect();

				if (!ndef.isWritable()) {
					mTextView.setText("Read-only tag.");
					return false;
				}
				
				// work out how much space we need for the data
				int size = message.toByteArray().length;
				if (ndef.getMaxSize() < size) {
					mTextView.setText("Tag doesn't have enough free space.");
					//wasteful...should query for existing buttons with isBurned = false before even creating new ones.
					button.deleteInBackground(); 
					return false;
				}
				ndef.writeNdefMessage(message);
				updateIsBurned(button);
				mTextView.setText("Button "+ button.getObjectId() +" written successfully");
				if (ndef.canMakeReadOnly()) {
				   //ndef.makeReadOnly();
				}
				return true;
			} else {
				// attempt to format tag
				NdefFormatable format = NdefFormatable.get(tag);
				if (format != null) {
					try {
						format.connect();
						//format.formatReadOnly(message);
						format.format(message);
						updateIsBurned(button);
						mTextView.setText("Button "+ button.getObjectId() +" written successfully");

						return true;
					} catch (IOException e) {
						mTextView.setText("Unable to format tag to NDEF.");
						button.deleteInBackground();
						return false;
					}
				} else {
					mTextView.setText("Tag doesn't appear to support NDEF format.");
					button.deleteInBackground();
					return false;
				}
			}
		} catch (Exception e) {
			button.deleteInBackground();
			mTextView.setText("Failed to write tag");
		}

        return false;
    }
	
}
