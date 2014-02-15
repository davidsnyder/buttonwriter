package com.buttonlabs.buttonwriter;

import com.parse.Parse;
import com.parse.ParseACL;

import android.app.Application;

public class ButtonWriterApplication extends Application {

	@Override
	public void onCreate() {
		super.onCreate();

		// Add your initialization code here
		Parse.initialize(this, "EaM52knWMXVxdqxvJk2zKGruxEv1eKxFeorP2Vul", "jL5CeZGZUoBUtnHRYA4E0YnRoDtbezxX5VgqXqcS");

		ParseACL defaultACL = new ParseACL();
	    
		// If you would like all objects to be private by default, remove this line.
		defaultACL.setPublicReadAccess(true);
		
		ParseACL.setDefaultACL(defaultACL, true);
	}

}
