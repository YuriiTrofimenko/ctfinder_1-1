package org.tyaa.ctfinder.api;

import static org.tyaa.ctfinder.common.ObjectifyQueryLauncher.objectifyRun;
import static org.tyaa.ctfinder.common.ObjectifyQueryLauncher.objectifyRun2;
import static org.tyaa.ctfinder.common.ObjectifyQueryLauncher.objectifyRun3;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
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
import org.tyaa.ctfinder.common.SessionAttributes;
import org.tyaa.ctfinder.controller.CityDAO;
import org.tyaa.ctfinder.controller.CountryDAO;
import org.tyaa.ctfinder.controller.LanguageDAO;
import org.tyaa.ctfinder.controller.Static_titleDAO;
import org.tyaa.ctfinder.entity.City;
import org.tyaa.ctfinder.entity.Country;
import org.tyaa.ctfinder.entity.Language;
import org.tyaa.ctfinder.entity.Static_title;
import org.tyaa.ctfinder.model.RespData;

import com.google.gson.Gson;
import com.googlecode.objectify.ObjectifyService;

/**
 * Servlet implementation class TasksServlet
 */
@WebServlet("/city")
public class CityServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
	private static Gson gson;
	
	static {
		
		ObjectifyService.register(Country.class);
		ObjectifyService.register(City.class);
		ObjectifyService.register(Static_title.class);
		ObjectifyService.register(Language.class);
	}
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public CityServlet() {
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
		    	
		    	//Если сессия существует
		    	//и содержит атрибут с UserId,
		    	//то проверяем параметры запроса от клиента
		    	if(SessionAttributes.isSessionAttrSet(session, SessionAttributes.userId)) {
		    		
		    		if (req.getParameterMap().keySet().contains(HttpReqParams.action)) {
	
						String actionString = req.getParameter(HttpReqParams.action);
						
						Long currentLanguageId =
								(Long)session.getAttribute(
									SessionAttributes.languageId
								);
						
						switch(actionString) {
						
							case HttpReqParams.create : {
								
								ArrayList<String> al = new ArrayList<>();
								al.add(HttpRespWords.created);
								RespData rd = new RespData(al);
								String successJson = gson.toJson(rd);
								out.print(successJson);
								break;
							}
							case HttpReqParams.autocomplete : {
								
								//Get English lang
								//TODO get current lang
								/*Language englishLanguage = new Language();
								objectifyRun2(
										"en"
										, englishLanguage
										, LanguageDAO::getLangByCode
										, out
										, gson
									);*/
								
								//Get static title by content and lang for selected Country
								/*Static_title selectedCountrySt = new Static_title();
								objectifyRun3(
										req.getParameter(HttpReqParams.country)
										, englishLanguage.getId()
										, selectedCountrySt
										, Static_titleDAO::getStaticTitleByContentAndLang
										, out
										, gson
									);*/
								Static_title selectedCountrySt = LocalizeHelper.getLocSTitleObjByContent(
										req.getParameter(HttpReqParams.country)
										, currentLanguageId
										, out
										, gson
									);
								
								//Get selected country
								Country selectedCountry = new Country();
								objectifyRun2(
										selectedCountrySt.getKey()
										, selectedCountry
										, CountryDAO::getCountryByTitleKey
										, out
										, gson
									);
								
								List<City> countryCitiesList = new ArrayList<>();
								objectifyRun2(
										selectedCountry.getId()
										, countryCitiesList
										, CityDAO::getCitiesByCountryId
										, out
										, gson
									);
								//get city titles by key n lang
								List<String> countryCitiesNameList =
										countryCitiesList.stream()
										.map(
											c -> {
												/*Static_title st = new Static_title();
												objectifyRun2(
													((City)c).getTitle_key()
													, st
													, Static_titleDAO::getStaticTitleByKey
													, out
													, gson
												);
												return st.getContent();*/
												return LocalizeHelper.getLoclizedSTitle(
														((City)c).getTitle_key()
														, currentLanguageId
														, out
														, gson
													);
											})
										.filter(
											cName ->
												((String)cName)
													.toLowerCase()
													.contains(
														req.getParameter(HttpReqParams.substring)
															.toLowerCase()
													)
										)
										.collect(Collectors.toList());
								
		                    	RespData result = new RespData(countryCitiesNameList);
		                        String resultJsonString = gson.toJson(result);
		                        out.print(resultJsonString);
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
		    	} //иначе передаем клиенту сообщение об ошибке "нет сессии"
		    	else {
		    		
		    		RespData rd = new RespData(ErrorStrings.noSession);
					String errorJson = gson.toJson(rd);
					out.print(errorJson);
		    	}
		    } catch (Exception ex) {
				// TODO Auto-generated catch block
				try (PrintWriter out = resp.getWriter()) {
					
					/*String errorTrace = "";
					for(StackTraceElement el: ex.getStackTrace()) {
						errorTrace += el.toString();
					}
					RespData rd = new RespData(errorTrace);*/
					
					RespData rd = new RespData("error");
					String errorJson = gson.toJson(rd);
					out.print(errorJson);
				}
				ex.printStackTrace();
			}
	    } catch (Exception ex) {
			// TODO Auto-generated catch block
			try (PrintWriter out = resp.getWriter()) {
				
				/*String errorTrace = "";
				for(StackTraceElement el: ex.getStackTrace()) {
					errorTrace += el.toString();
				}
				RespData rd = new RespData(errorTrace);*/
				
				RespData rd = new RespData("nosession");
				String errorJson = gson.toJson(rd);
				out.print(errorJson);
			}
			ex.printStackTrace();
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
