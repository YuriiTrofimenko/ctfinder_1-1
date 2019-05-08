package org.tyaa.ctfinder.common;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class CTFinder2MySkillsOrganizer {

	static {
		FileInputStream serviceAccount = null;
		try {
			serviceAccount = new FileInputStream("WEB-INF/firebaseServiceAccount.json");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		FirebaseOptions options = null;
		try {
			options = new FirebaseOptions.Builder()
			  .setCredentials(GoogleCredentials.fromStream(serviceAccount))
			  .setDatabaseUrl("https://my-skills-organizer.firebaseio.com")
			  .build();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		FirebaseApp.initializeApp(options);
	}
	
	public static void createNode(String email, String title, String description) {
		
		UserRecord userRecord = null;
		try {
			userRecord =
					FirebaseAuth.getInstance().getUserByEmail(email);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		if (userRecord != null) {
			String uId = userRecord.getUid();
			final FirebaseDatabase database = FirebaseDatabase.getInstance();
			DatabaseReference ref = database.getReference("nodes");
			MySkillsOrganizerNode node =
					new MySkillsOrganizerNode(uId, title, "2", description, "1", false, 0, 0, 50);
			ref.push().setValueAsync(node);
		}
	}
}
