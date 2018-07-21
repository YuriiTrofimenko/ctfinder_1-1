package org.tyaa.ctfinder.common;

import static org.tyaa.ctfinder.common.ObjectifyQueryLauncher.objectifyRun2;
import static org.tyaa.ctfinder.common.ObjectifyQueryLauncher.objectifyRun3;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

import org.tyaa.ctfinder.controller.LanguageDAO;
import org.tyaa.ctfinder.controller.Static_titleDAO;
import org.tyaa.ctfinder.entity.Language;
import org.tyaa.ctfinder.entity.Static_title;
import org.tyaa.ctfinder.model.DictionaryItem;

import com.google.gson.Gson;

public class LocalizeHelper {

public static String getLoclizedSTitle(
		String _staticTitleKey
		, Long _langId
		, PrintWriter _out
		, Gson _gson) {
		
		Static_title staticTitle = new Static_title();
		objectifyRun3(
				_staticTitleKey
				, _langId
				, staticTitle
				, Static_titleDAO::getStaticTitleByKeyAndLang
				, _out
				, _gson
			);
		if(staticTitle.getId() == null) {
		
			Language englishLanguage = new Language();
			objectifyRun2(
					"en"
					, englishLanguage
					, LanguageDAO::getLangByCode
					, _out
					, _gson);
			objectifyRun3(
					_staticTitleKey
					, englishLanguage.getId()
					, staticTitle
					, Static_titleDAO::getStaticTitleByKeyAndLang
					, _out
					, _gson
				);
		}
		return staticTitle.getContent();
	}

	public static List<DictionaryItem> appResourceToList(ResourceBundle _resourceBundle) {
		
		List<DictionaryItem> dictionaryItems = new ArrayList<>();
		for (String key : Collections.list(_resourceBundle.getKeys())) {
			dictionaryItems.add(new DictionaryItem(key, _resourceBundle.getString(key)));
		}
		return dictionaryItems;
	}
}
