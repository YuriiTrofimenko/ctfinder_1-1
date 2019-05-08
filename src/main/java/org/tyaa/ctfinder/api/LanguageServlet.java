package org.tyaa.ctfinder.api;

import static org.tyaa.ctfinder.common.ObjectifyQueryLauncher.objectifyRun;
import static org.tyaa.ctfinder.common.ObjectifyQueryLauncher.objectifyRun2;
import static org.tyaa.ctfinder.common.ObjectifyQueryLauncher.objectifyRun3;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.tyaa.ctfinder.common.ErrorStrings;
import org.tyaa.ctfinder.common.HttpReqParams;
import org.tyaa.ctfinder.common.HttpRespWords;
import org.tyaa.ctfinder.common.LocalizeHelper;
import org.tyaa.ctfinder.common.ObjectifyQueryLauncher;
import org.tyaa.ctfinder.common.SessionAttributes;
import org.tyaa.ctfinder.controller.LanguageDAO;
import org.tyaa.ctfinder.controller.StateDAO;
import org.tyaa.ctfinder.controller.Static_titleDAO;
import org.tyaa.ctfinder.entity.Language;
import org.tyaa.ctfinder.entity.State;
import org.tyaa.ctfinder.entity.Static_title;
import org.tyaa.ctfinder.model.DictionaryItem;
import org.tyaa.ctfinder.model.LanguageInfo;
import org.tyaa.ctfinder.model.RespData;
import org.tyaa.ctfinder.model.StateItem;

import com.google.gson.Gson;
import com.googlecode.objectify.ObjectifyService;

/**
 * Servlet implementation class TasksServlet
 */
@WebServlet("/language")
public class LanguageServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
	private static Gson gson;
	
	static {
		
		ObjectifyService.register(Language.class);
		ObjectifyService.register(Static_title.class);
	}
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public LanguageServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

	@Override
	public void init() throws ServletException {
		// TODO Auto-generated method stub
		super.init();
		//Преобразователь из объектов Java в строки JSON
	    gson = new Gson();
    		//new GsonBuilder()
    		//.setDateFormat("yyyy-MM-dd").create();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		resp.setContentType("application/json");
	    resp.setCharacterEncoding("UTF-8");
	    
	    HttpSession session = null;
	    try {
	    	
	    	session = req.getSession(true);
		    
		    //Открываем выходной поток - с сервера к клиенту (браузеру/мобильному приложению)
		    try (PrintWriter out = resp.getWriter()) {
		    	
		    	//Если 
		    	//то 
	    		if (req.getParameterMap().keySet().contains(HttpReqParams.action)) {

					String actionString = req.getParameter(HttpReqParams.action);
					
					switch(actionString) {
					
						case HttpReqParams.create : {
							
							ArrayList<String> al = new ArrayList<>();
							al.add(HttpRespWords.created);
							RespData rd = new RespData(al);
							String successJson = gson.toJson(rd);
							out.print(successJson);
							break;
						}
						case HttpReqParams.set : {
							
							String languageCodeString  =
									req.getParameter(HttpReqParams.language);
							
							Language selectedLanguage = new Language();
							objectifyRun2(
									languageCodeString
									, selectedLanguage
									, LanguageDAO::getLangByCode
									, out
									, gson);
							
							session.setAttribute(SessionAttributes.languageId, selectedLanguage.getId());
							
							ArrayList<String> al = new ArrayList<>();
							al.add(HttpRespWords.set);
							RespData rd = new RespData(al);
							String successJson = gson.toJson(rd);
							out.print(successJson);
							break;
						}
						case HttpReqParams.get : {
							
							Long currentLanguageId =
									(Long)session.getAttribute(
										SessionAttributes.languageId
									);
							Language currentLanguage = new Language();
							objectifyRun2(
									currentLanguageId
									, currentLanguage
									, LanguageDAO::getLang
									, out
									, gson);
							
							String currentLanguageSTitle =
									LocalizeHelper.getLoclizedSTitle(
											currentLanguage.getTitle_key()
											, currentLanguageId
											, out
											, gson);
							
							LanguageInfo languageInfo =
									new LanguageInfo(currentLanguage.getCode(), currentLanguageSTitle, true);
							
							List al = new ArrayList();
							al.add(languageInfo);
							RespData rd = new RespData(al);
							String successJson = gson.toJson(rd);
							out.print(successJson);
							break;
						}
						case HttpReqParams.getAll : {
							
							try {
								//Current lang id
								Long currentLanguageId =
										(Long)session.getAttribute(
											SessionAttributes.languageId
										);
								
								//All langs list
								List<Language> languages = new ArrayList<>();
								objectifyRun(
										languages
										, LanguageDAO::getAll
										, out
										, gson);
								//
								List<LanguageInfo> languageInfoList =
										languages.stream()
										.map(
											lang -> {
												String languageSTitle =
														LocalizeHelper.getLoclizedSTitle(
																lang.getTitle_key()
																, currentLanguageId
																, out
																, gson);
												return new LanguageInfo(
														lang.getCode()
														, languageSTitle
														, lang.getId().equals(currentLanguageId)
													);
											})
										.collect(Collectors.toList());
								
		                    	RespData result = new RespData(languageInfoList);
		                        String resultJsonString = gson.toJson(result);
		                        out.print(resultJsonString);
							} catch (Exception ex) {
								
								ObjectifyQueryLauncher.printException(ex, out, gson);
							}    
                        	break;
						}
						case HttpReqParams.dictionary : {
							try {
								
								String pageName = "";
								
								if (req.getParameterMap().keySet().contains(HttpReqParams.page)) {
									pageName = req.getParameter(HttpReqParams.page).toUpperCase();
								}
								
								Long currentLanguageId =
										(Long)session.getAttribute(
											SessionAttributes.languageId
										);
								Language currentLanguage = new Language();
								objectifyRun2(
										currentLanguageId
										, currentLanguage
										, LanguageDAO::getLang
										, out
										, gson);
								
								Locale loc = new Locale(currentLanguage.getCode());
								ResourceBundle bundle =
										ResourceBundle.getBundle(
												"locales.ApplicationResources" + pageName
												, loc
											);
								List<DictionaryItem> dictionaryItems =
										LocalizeHelper.appResourceToList(bundle);
								
								//ArrayList<String> al = new ArrayList<>();
								//al.add(HttpRespWords.deleted);
								RespData rd = new RespData(dictionaryItems);
								String successJson = gson.toJson(rd);
								out.print(successJson);
							} catch (Exception ex) {
										
									ObjectifyQueryLauncher.printException(ex, out, gson);
							 }
							break;
						}
						case HttpReqParams.delete : {
							
							ArrayList<String> al = new ArrayList<>();
							al.add(HttpRespWords.deleted);
							RespData rd = new RespData(al);
							String successJson = gson.toJson(rd);
							out.print(successJson);
							break;
						}
					}
	    		}
		    } catch (Exception ex) {
				// TODO Auto-generated catch block
				try (PrintWriter out = resp.getWriter()) {
					
					ObjectifyQueryLauncher.printException(ex, out, gson);
				}
			}
	    } catch (Exception ex) {
			try (PrintWriter out = resp.getWriter()) {
				
				ObjectifyQueryLauncher.printException(ex, out, gson);
			}
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}
}
