package org.tyaa.ctfinder.api.publ;

import static org.tyaa.ctfinder.common.ObjectifyQueryLauncher.objectifyRun;
import static org.tyaa.ctfinder.common.ObjectifyQueryLauncher.objectifyRun2;
import static org.tyaa.ctfinder.common.ObjectifyQueryLauncher.objectifyRun3;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.mail.MessagingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.tyaa.ctfinder.common.*;
import org.tyaa.ctfinder.controller.*;
import org.tyaa.ctfinder.entity.*;
import org.tyaa.ctfinder.filter.OfferFilter;
import org.tyaa.ctfinder.filter.TitleFilter;
import org.tyaa.ctfinder.model.*;
import org.tyaa.ctfinder.projection.OfferProjections;

import com.google.appengine.api.datastore.Blob;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.googlecode.objectify.ObjectifyService;

/**
 * Servlet implementation class TasksServlet
 */
@WebServlet("/offer")
public class OfferServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
	private static final Integer unbounded = -1;
	private static final Logger log = Logger.getLogger(OfferServlet.class.getName());
	
	static {
		
		ObjectifyService.register(Language.class);
		ObjectifyService.register(Title.class);
		ObjectifyService.register(Description.class);
		ObjectifyService.register(Offer.class);
		ObjectifyService.register(User.class);
		ObjectifyService.register(State.class);
		ObjectifyService.register(Country.class);
		ObjectifyService.register(City.class);
		ObjectifyService.register(Static_title.class);
		ObjectifyService.register(Static_description.class);
		ObjectifyService.register(Offer_type.class);
		ObjectifyService.register(Subscription.class);
	}	
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public OfferServlet() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		resp.setContentType("application/json");
	    resp.setCharacterEncoding("UTF-8");
	    
	    //Преобразователь из объектов Java в строки JSON
	    Gson gson =// new Gson();
	    		new GsonBuilder()
	    		.setDateFormat("yyyy-MM-dd").create();
	    
	    HttpSession session = null;
	    try {
	    	
	    	//Если сессия существует - получаем ее,
	    	//если нет - получаем новую пустую сессию
	    	session = req.getSession(true);
		    
		    //Открываем выходной поток - с сервера к клиенту (браузеру/мобильному приложению)
		    try (PrintWriter out = resp.getWriter()) {
		    	
		    	//Получаем из сессии значение текущего языка
		    	//(по умолчанию выдает английский язык)
		    	Long currentLanguageId =
						(Long)session.getAttribute(
							SessionAttributes.languageId
						);
		    	
		    	//Если сессия содержит атрибут с UserId,
		    	//то проверяем параметры запроса от клиента
		    	if(SessionAttributes.isSessionAttrSet(session, SessionAttributes.userId)) {
		    		
		    		if (req.getParameterMap().keySet().contains(HttpReqParams.action)) {
	
						String actionString = req.getParameter(HttpReqParams.action);
						
						//Готовим формат для парсинга дат из строк
						/*DateFormat reversedFormat =
							new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);*/
						
						switch(actionString) {
						
							case HttpReqParams.create : {
																
								//Получаем из БД объект английского языка
								Language englishLanguage = new Language();
								objectifyRun2(
										"en"
										, englishLanguage
										, LanguageDAO::getLangByCode
										, out
										, gson);
								
								//
								try {
									//1. Идентификатор типа задания
									Long offerTypeId = Long.parseLong(req.getParameter("offer_type_id"));
									
									//3. 
									//Получаем из параметра запроса содержимое заголовка предложения
									String title =
											req.getParameter("title");

									//4. 
									//Выполняем то же самое для основного содержания предложения
									String description =
											req.getParameter("content");
									
									//6
									Long countryId = null;
									String countryName = req.getParameter("country_name");
									try {
										if(countryName != null && !countryName.equals("")) {
											
											List<Country> countryList = new ArrayList<>();
											objectifyRun(
													countryList
													, CountryDAO::getAllCountries
													, out
													, gson
												);
											List<Country> filteredCountryList =
												countryList.stream()
													.filter(
														c -> {
															Static_title st = LocalizeHelper.getLoclizedSTitleObject(
																	((Country)c).getTitle_key()
																	, currentLanguageId
																	, out
																	, gson
																);
															return st.getContent().equals(countryName);
														})
													.collect(Collectors.toList());
											if(filteredCountryList.size() > 0) {
												countryId =
														filteredCountryList.get(0).getId();
											} else {
												countryId =
														createCountry(countryName, currentLanguageId, out, gson);
											}
										}
									} catch(Exception ex) {}
									
									//7
									Long cityId = null;
									String cityName = req.getParameter("city_name");
									try {
										if(cityName != null && !cityName.equals("") && countryId != null) {
											List<City> countryCitiesList = new ArrayList<>();
											objectifyRun2(
													countryId
													, countryCitiesList
													, CityDAO::getCitiesByCountryId
													, out
													, gson
												);
											//
											List<City> filteredCountryCitiesList =
													countryCitiesList.stream()
													.filter(
														c -> {
															/*Static_title st = new Static_title();
															objectifyRun2(
																((City)c).getTitle_key()
																, st
																, Static_titleDAO::getStaticTitleByKey
																, out
																, gson
															);*/
															Static_title st = LocalizeHelper.getLoclizedSTitleObject(
																	((City)c).getTitle_key()
																	, currentLanguageId
																	, out
																	, gson
																);
															return st.getContent().equals(cityName);
														})
													.collect(Collectors.toList());
											//
											if(filteredCountryCitiesList.size() > 0) {
												cityId =
														filteredCountryCitiesList.get(0).getId();
											} else {
												cityId =
														createCity(cityName, countryId, currentLanguageId, out, gson);
											}
										}
									} catch(Exception ex) {}
									
									//8
									Integer collaboratorsCount = OfferServlet.unbounded;
									try {
										collaboratorsCount =
												Integer.parseInt(req.getParameter("c-count"));
									}
									catch(Exception ex){}/* finally {
										al1.add(Integer.toString(collaboratorsCount));
									}*/
									
									//9
									Blob image =
											new Blob(req.getParameter("image").getBytes());
									
									//10
									String startDate = null;
									try {
										startDate =
											DateTransform.DirectToReversed(req.getParameter("start-date"));
									}
									catch(Exception ex){}
									
									//11
									String finishDate = null;
									try {
										finishDate =
											DateTransform.DirectToReversed(req.getParameter("finish-date"));
									} catch(Exception ex){}
									
									//Форматтер даты с обратной записью
									DateFormat reversedFormat =
											new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
									
									//16
									String updatedAt = reversedFormat.format(new Date());
									
									//Создание или обновление
									if (req.getParameterMap().keySet().contains(HttpReqParams.id)) {
										//Поиск и обновление записи о предложении в БД
										
										//
										Offer o = new Offer();
										//
										String offerId =
												req.getParameter(HttpReqParams.id);
										
										//Получение объекта из БД
										objectifyRun2(
												offerId
												, o
												, OfferDAO::getOffer
												, out
												, gson
											);
										
										o.setOffer_type_id(offerTypeId);
										o.setCountry_id(countryId);
										o.setCity_id(cityId);
										o.setCollaborators_count(collaboratorsCount);
										
										//Получаем из БД старый объект заголовка
										Title titleEntity =
												LocalizeHelper.getLoclizedTitleObject(
														o.getTitle_key()
														, currentLanguageId
														, out
														, gson
													);
										//Из старого объекта заголовка узнаем старый заголовок предложения
										String oldTitleString = titleEntity.getContent();
										
										//Если новый заголовок предложения отличается от старого
										if(!oldTitleString.equals(title)) {
											//Записываем в объект заголовка новое содержание
											titleEntity.setContent(title);
											//Обновляем объект заголовка в БД
											objectifyRun(
													titleEntity
													, TitleDAO::createTitle
													, out
													, gson
												);
										}
										
										//Получаем из БД старый объект
										Description descriptionEntity =
												LocalizeHelper.getLoclizedDescriptionObject(
														o.getDescription_key()
														, currentLanguageId
														, out
														, gson
													);
										String oldDescriptionString = descriptionEntity.getContent();
										if(!oldDescriptionString.equals(description)) {
											//Записываем в объект новое содержание
											descriptionEntity.setContent(description);
											//Обновляем объект в БД
											objectifyRun(
													descriptionEntity
													, DescriptionDAO::createDescriprion
													, out
													, gson
												);
										}
										
										//Если нового значения желаемого старта нет -
										//извлекаем старое
										if(startDate == null) {
											startDate = o.getStart_date();
										}
										//Если нового значения желаемой даты окончания нет -
										//извлекаем старое значение
										if(finishDate == null) {
											finishDate = o.getFinish_date();
										}
										
										//12
										String urgencyInDays = null;
										if(startDate != null && finishDate != null) {
											
											urgencyInDays =
													DateTransform.ReversedToDiff(startDate, finishDate);
										} else if(finishDate != null) {
											
											urgencyInDays =
													DateTransform.ReversedToDiff(o.getCreated_at(), finishDate);
										} else {
											//unbounded
											urgencyInDays = "36500";
										}
										
										o.setStart_date(startDate);
										o.setFinish_date(finishDate);
										o.setUrgency_in_days(urgencyInDays);
										
										if(image.getBytes().length > 0) {
											o.setImage(image);
										}
										
										o.setUpdated_at(updatedAt);
										
										objectifyRun(
												o
												, OfferDAO::createOffer
												, out
												, gson
											);
										
										ArrayList<String> al = new ArrayList<>();
										al.add(HttpRespWords.updated);
										RespData rd = new RespData(al);
										String successJson = gson.toJson(rd);
										out.print(successJson);
									} else {
										//Создание записи о предложении в БД с нуля
										
										//2. Получаем из БД объект сотояния "создано"
										State createdState = new State();
										objectifyRun2(
												"created_state_static_title"
												, createdState
												, StateDAO::getStateByTitleKey
												, out
												, gson
											);
										Long createdStateId = createdState.getId();
										
										//Создаем и заполняем сущность заголовка предложения
										Title newOfferTitle =
											new Title(
													KeyGen.text2KeyText(title) + "_offer_title"
													, englishLanguage.getId()
													, title);
										objectifyRun(
												newOfferTitle
												, TitleDAO::createTitle
												, out
												, gson
											);
										String offerTitleKey = newOfferTitle.getKey();
										
										//
										Description newOfferDescription =
												new Description(
														KeyGen.text2KeyText(description) + "_offer_description"
														, englishLanguage.getId()
														, description);
										objectifyRun(
												newOfferDescription
												, DescriptionDAO::createDescriprion
												, out
												, gson
											);
										String offerDescriptionKey = newOfferDescription.getKey();
										
										//5
										Long userId =
												(Long)session.getAttribute(
														SessionAttributes.userId
													);
										
										//13
										String startedAt = null;
										
										//14
										String completedAt = null;
										
										//15
										String createdAt = reversedFormat.format(new Date());
										
										//12
										String urgencyInDays = null;
										if(startDate != null && finishDate != null) {
											
											urgencyInDays =
													DateTransform.ReversedToDiff(startDate, finishDate);
										} else if(finishDate != null) {
											
											urgencyInDays =
													DateTransform.ReversedToDiff(createdAt, finishDate);
										} else {
											//unbounded
											urgencyInDays = "36500";
										}
										
										Offer offer =
												new Offer(
														//offer type id
														offerTypeId
														//created-state's id from db
														, createdStateId
														//offer title
														, offerTitleKey
														//offer description
														, offerDescriptionKey
														//current user id
														, userId
														//country_id
														, countryId
														//city_id
														, cityId
														//count of collaborators
														, collaboratorsCount
														// image base64 string to blob
														, image
														//desired start date
														, startDate
														//desired finish date
														, finishDate
														//urgency in days
														, urgencyInDays
														//started_at = null
														, startedAt
														//completed_at = null
														, completedAt
														//created_at = now date
														, createdAt
														//updated_at = now date
														, updatedAt
													);
											
											objectifyRun(
													offer
													, OfferDAO::createOffer
													, out
													, gson
												);
											
											//Send created message to all subscribers
											Long authorId = userId;
											//log.info("authorId " + authorId);
											User authorUser = new User();
											objectifyRun2(
													authorId
													, authorUser
													, UserDAO::getUser
													, out
													, gson
												);
											
											List<Subscription> subscriptions = new ArrayList<>();
											
											objectifyRun2(
													authorId
													, subscriptions
													, (_object1, _object2) -> {
														try {
															SubscriptionDAO.findListByAuthor(_object1, _object2);
														} catch (IllegalArgumentException | IllegalAccessException ex) {
															ObjectifyQueryLauncher.printException(ex, out, gson);
														} 
													}
													, out
													, gson
												);
											
											Mailer mailer = new Mailer();
											String messageString =
													"User "
													+ authorUser.getName()
													+ " published new offer: '"
													+ title
													+ "' (URL: https://creativetfinder.appspot.com/#find:"
													+ offer.getId()
													+ "). "
													+ "Unsubscription link: https://creativetfinder.appspot.com/pages/static/unsubscription.html?author-id="
														+ authorId
														+ "&subscriber-id=";
											String subjectString = "New offer";
											String fromAddressString = "tyaamariupol@gmail.com";
											String fromNameString = "CTFinder";
											
											//log.info("messageString " + messageString);
											//log.info("subscriptions " + subscriptions.size());
											
											subscriptions.forEach((s) -> {
												
												Long subscriberId = s.getSubscriber_id();
												//log.info("subscriberId " + subscriberId);
												
												User subscriberUser = new User();
												objectifyRun2(
														subscriberId
														, subscriberUser
														, UserDAO::getUser
														, out
														, gson
													);
												
												String toNameString = subscriberUser.getName();
												String toAddressString = subscriberUser.getEmail();
												
												//log.info("toNameString " + toNameString);
												//log.info("toAddressString " + toAddressString);
												String customMessageString = 
														messageString
														+ subscriberUser.getId();
												
												// Отправляем сообщение
												try {
													mailer.sendPlainMsg(
															customMessageString
															, subjectString
															, fromAddressString
															, fromNameString
															, toAddressString
															, toNameString);
												} catch (UnsupportedEncodingException | MessagingException ex) {
													
													ObjectifyQueryLauncher.printException(ex, out, gson);
												}
											});
											
											ArrayList<String> al = new ArrayList<>();
											al.add(HttpRespWords.created);
											RespData rd = new RespData(al);
											String successJson = gson.toJson(rd);
											out.print(successJson);
									}
								} catch (Exception ex) {
									
									ObjectifyQueryLauncher.printException(ex, out, gson);
								}
								break;
							}
							//Получить одну из моделей,
							//заполненную на основании данных одного предложения,
							//заданного параметром id
							case HttpReqParams.get : {
								
								try {
									
									//Получаем из БД объект английского языка
									Language englishLanguage = new Language();
									objectifyRun2(
											"en"
											, englishLanguage
											, LanguageDAO::getLangByCode
											, out
											, gson);
									
									//
									Offer o = new Offer();
									//
									String offerId =
											req.getParameter(HttpReqParams.id);
									
									//Задание проекции
									String projectionString  =
											req.getParameter(HttpReqParams.projection);
									
									//Получение объекта из БД
									objectifyRun2(
											offerId
											, o
											, OfferDAO::getOffer
											, out
											, gson
										);
									
									//Возврат только идентификатора состояния предложения
									if(projectionString.equals(HttpReqParams.stateProjection)) {
										
										List al = new ArrayList<>();
										al.add(o.getState_id());
										RespData rd = new RespData(al);
										String successJson = gson.toJson(rd);
										out.print(successJson);
										break;
									}
																		
									//Image
									int imageBytesCount = o.getImage().getBytes().length;
									String noimageUriString = "/img/no-image.png";
									String imageBase64 =
											(imageBytesCount > 0)
											? new String(o.getImage().getBytes())
											: noimageUriString;
									
									if(projectionString.equals(HttpReqParams.imageProjection)) {
										
										List al = new ArrayList<>();
										al.add(imageBase64);
										RespData rd = new RespData(al);
										String successJson = gson.toJson(rd);
										out.print(successJson);
									} else {
									
										//Находим объект названия типа предлажения
										//и его реализацию на текущем языке
										String offerTypeDescriptionString = "-";
										if(o.getOffer_type_id() != null) {
											Offer_type offerType = new Offer_type();
											objectifyRun2(
													o.getOffer_type_id()
													, offerType
													, Offer_typeDAO::getOffer_type
													, out
													, gson
												);
											//offerTypeDescriptionString = "+";
											/*Static_description offerTypeDescription =
													new Static_description();
											objectifyRun3(
												offerType.getDescription_key()
												, englishLanguage.getId()
												, offerTypeDescription
												, Static_descriprionDAO::getStaticDescriptionByKeyAndLang
												, out
												, gson
											);*/
											Static_description offerTypeDescription =
													LocalizeHelper.getLocalizedSDescriptionObject(
															offerType.getDescription_key()
															, currentLanguageId
															, out
															, gson
														);
											if(offerTypeDescription.getContent() != null) {
												offerTypeDescriptionString =
														offerTypeDescription.getContent();
											}
										}
										//Находим заголовок на текущем языке
										String titleString = "-";
										if(o.getTitle_key() != null
												&& !o.getTitle_key().equals("")) {
											Title title = new Title();
											objectifyRun3(
												o.getTitle_key()
												, englishLanguage.getId()
												, title
												, TitleDAO::getTitleByKeyAndLang
												, out
												, gson
											);
											titleString = title.getContent();
										}
										//Находим описание на текущем языке
										String descriptionString = "-";
										if(o.getDescription_key() != null
												&& !o.getDescription_key().equals("")) {
											Description description = new Description();
											objectifyRun3(
												o.getDescription_key()
												, englishLanguage.getId()
												, description
												, DescriptionDAO::getDescriptionByKeyAndLang
												, out
												, gson
											);
											descriptionString = description.getContent();
										}
												
										//Country
										String countryString = "-";
										if(o.getCountry_id() != null) {
											Country offerCountry = new Country();
											objectifyRun2(
													o.getCountry_id()
													, offerCountry
													, CountryDAO::getCountry
													, out
													, gson
												);
											countryString =
													LocalizeHelper.getLoclizedSTitle(
															offerCountry.getTitle_key()
															, currentLanguageId
															, out
															, gson);
										}
										
										//City
										String cityString = "-";
										if(o.getCity_id() != null) {
											City offerCity = new City();
											objectifyRun2(
													o.getCity_id()
													, offerCity
													, CityDAO::getCity
													, out
													, gson
												);
											cityString =
													LocalizeHelper.getLoclizedSTitle(
															offerCity.getTitle_key()
															, currentLanguageId
															, out
															, gson);
										}
										
										switch(projectionString) {
										
											case HttpReqParams.gridItemDetailsProjection:{
														
												
												//Находим объект типа состояния
												//и его реализацию на текущем языке
												String offerStateString = "-";
												if(o.getState_id() != null) {
													State state = new State();
													objectifyRun2(
															o.getState_id()
															, state
															, StateDAO::getState
															, out
															, gson
														);
													Static_title stateTitle = new Static_title();
													objectifyRun3(
														state.getTitle_key()
														, englishLanguage.getId()
														, stateTitle
														, Static_titleDAO::getStaticTitleByKeyAndLang
														, out
														, gson
													);
													if(stateTitle.getContent() != null) {
														offerStateString = stateTitle.getContent();
													}
												}
												
												//Created at - direct format
												String createdAtString =
														DateTransform.ReversedToDirect(o.getCreated_at());
												
												//Author-user info
												Long userAuthorId = o.getUser_id();
												User userAuthor = new User();
												objectifyRun2(
														userAuthorId
														, userAuthor
														, UserDAO::getUser
														, out
														, gson
													);
												String userAuthorName = userAuthor.getName();
												
												Long subscriberId =
													(Long)session.getAttribute(
														SessionAttributes.userId
													);
												
												//
												Subscription subscription = new Subscription();
												objectifyRun3(
														userAuthorId
														, subscriberId
														, subscription
														, (_object1, _object2, _object3) -> {
															try {
																SubscriptionDAO.findByAuthorAndSubscriber(_object1, _object2, _object3);
															} catch (IllegalArgumentException | IllegalAccessException ex) {
																ObjectifyQueryLauncher.printException(ex, out, gson);
															} 
														}
														, out
														, gson
													);
												Boolean subscribed =
														(subscription.getId() != null)
														? true
														: false;
												
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
																"locales.ApplicationResourcesCommon"
																, loc
															);
												String collaboratorsCount =
														(o.getCollaborators_count() != null
																&& !o.getCollaborators_count().equals(-1))
														? o.getCollaborators_count().toString()
														: bundle.getString("common_unbounded");
												
												String desiredStartDateString =
														(o.getStart_date() != null && !o.getStart_date().equals(""))
														? DateTransform.ReversedToDirect(o.getStart_date())
														: "";
												
												String desiredFinishDateString =
														(o.getFinish_date() != null && !o.getFinish_date().equals(""))
														? DateTransform.ReversedToDirect(o.getFinish_date())
														: "";
												
												//Populate the response object
												OfferGridItemDetails offerDetails =
													new OfferGridItemDetails (
														o.getId()
														, offerTypeDescriptionString
														, titleString
														, offerStateString
														, imageBase64
														, descriptionString
														, createdAtString
														
														, countryString
														, cityString
														
														, userAuthorName
														, userAuthorId
														, subscribed
														
														, collaboratorsCount
														, desiredStartDateString
														, desiredFinishDateString
													);
												
												//
												List al = new ArrayList<>();
												al.add(offerDetails);
												//String nextCursorString = (cursorStr[0] != null) ? cursorStr[0] : "end";
												//al.add(new ContinuData(gridItems, nextCursorString));
												RespData rd = new RespData(al);
												String successJson = gson.toJson(rd);
												out.print(successJson);
												break;
											}
											case HttpReqParams.tableRowEditProjection:{
													
												//
												String collaboratorsCount =
														o.getCollaborators_count().toString();
												
												String desiredStartDateString =
														(o.getStart_date() != null && !o.getStart_date().equals(""))
														? DateTransform.ReversedToDirect(o.getStart_date())
														: "";
												
												String desiredFinishDateString =
														(o.getFinish_date() != null && !o.getFinish_date().equals(""))
														? DateTransform.ReversedToDirect(o.getFinish_date())
														: "";
												
												//Находим объект типа состояния
												//и его реализацию на текущем языке
												String offerStateString = "-";
												if(o.getState_id() != null) {
													State state = new State();
													objectifyRun2(
															o.getState_id()
															, state
															, StateDAO::getState
															, out
															, gson
														);
													
													Static_title stateTitle = new Static_title();
													objectifyRun3(
														state.getTitle_key()
														, englishLanguage.getId()
														, stateTitle
														, Static_titleDAO::getStaticTitleByKeyAndLang
														, out
														, gson
													);
													if(stateTitle.getContent() != null) {
														offerStateString = stateTitle.getContent();
													}
												}
												
												//Created at - direct format
												String createdAtString =
														DateTransform.ReversedToDirect(o.getCreated_at());
												
												//Populate the response object
												OfferTableRowEdit offerTableRowEdit =
													new OfferTableRowEdit (
														o.getId()
														, offerTypeDescriptionString
														, titleString
														, descriptionString
														
														, collaboratorsCount
														, countryString
														, cityString
														, desiredStartDateString
														, desiredFinishDateString);
												
												//
												List al = new ArrayList<>();
												al.add(offerTableRowEdit);
												RespData rd = new RespData(al);
												String successJson = gson.toJson(rd);
												out.print(successJson);
												break;
											}
										}
									}
								} catch (Exception ex) {
									
									ObjectifyQueryLauncher.printException(ex, out, gson);
								}
								
								break;
							}
							case HttpReqParams.getRange : {
								
								try {
									
									//Получаем из БД объект английского языка
									Language englishLanguage = new Language();
									objectifyRun2(
											"en"
											, englishLanguage
											, LanguageDAO::getLangByCode
											, out
											, gson);
									
									//Фильтрация, сортировка и проектция преимущественно
									//средствами Java Stream API
									if(req.getParameterMap().keySet().contains(HttpReqParams.inMemory)) {
										
										List<Offer> offers = new ArrayList<>();
										List<OfferTableRow> offersOut = new ArrayList<>();
										
										Integer limit =
												Integer.parseInt(req.getParameter(HttpReqParams.limit));
										Integer startFrom =
												Integer.parseInt(req.getParameter(HttpReqParams.startFrom));
										
										OfferFilter offerFilter = new OfferFilter();
										
										Long userId =
												(Long)session.getAttribute(
														SessionAttributes.userId
													);
										
										Map<OfferDAO.Params, Object> paramsMap = new HashMap<>();
										paramsMap.put(OfferDAO.Params.OfferList, offers);
										paramsMap.put(OfferDAO.Params.UserId, userId);
										paramsMap.put(OfferDAO.Params.InMemory, true);
										objectifyRun(
												paramsMap
												, OfferDAO::getOffersRange
												, out
												, gson
											);
										
										Stream<Offer> offerStream = offers.stream()
												.sorted(Comparator.comparing(Offer::getCreated_at).reversed()) ;
										
										if(req.getParameterMap().keySet().contains(HttpReqParams.createdDateFrom)) {
											
											offerFilter.createdDateFrom =
													DateTransform.DirectToReversed(
															req.getParameter(HttpReqParams.createdDateFrom)
														);
											offerStream =
												offerStream.filter(
														o ->
															((Offer)o).getCreated_at().compareTo(offerFilter.createdDateFrom)
															 	>= 0
													);
										}
										
										if(req.getParameterMap().keySet().contains(HttpReqParams.createdDateTo)) {
											
											offerFilter.createdDateTo =
													DateTransform.DirectToReversed(
															req.getParameter(HttpReqParams.createdDateTo)
														);
											offerStream =
													offerStream.filter(
															o ->
																((Offer)o).getCreated_at().compareTo(offerFilter.createdDateTo)
																 	<= 0
														);
										}
										
										if(startFrom != null && startFrom != 0) {
											
											offerStream =
													offerStream.skip(startFrom);
										}
										
										if(limit != null && limit != 0) {
											
											offerStream =
													offerStream.limit(limit);
										}
										
										Stream<OfferTableRow> offerTableRowStream =
												offerStream.map(
													
														o -> {
															
															String offerTypeDescriptionString = "-";
															if(o.getOffer_type_id() != null) {
																Offer_type offerType = new Offer_type();
																objectifyRun2(
																		o.getOffer_type_id()
																		, offerType
																		, Offer_typeDAO::getOffer_type
																		, out
																		, gson
																	);
																/*Static_description offerTypeDescription =
																		new Static_description();
																objectifyRun3(
																	offerType.getDescription_key()
																	, englishLanguage.getId()
																	, offerTypeDescription
																	, Static_descriprionDAO::getStaticDescriptionByKeyAndLang
																	, out
																	, gson
																);*/
																Static_description offerTypeDescription =
																		LocalizeHelper.getLocalizedSDescriptionObject(
																				offerType.getDescription_key()
																				, currentLanguageId
																				, out
																				, gson
																			);
																if(offerTypeDescription.getContent() != null) {
																	offerTypeDescriptionString =
																			offerTypeDescription.getContent();
																}
															}
															
															String titleString = "-";
															if(o.getTitle_key() != null
																	&& !o.getTitle_key().equals("")) {
																Title title = new Title();
																objectifyRun3(
																	o.getTitle_key()
																	, englishLanguage.getId()
																	, title
																	, TitleDAO::getTitleByKeyAndLang
																	, out
																	, gson
																);
																titleString = title.getContent();
															}
															
															String offerStateString = "-";
															if(o.getState_id() != null) {
																State state = new State();
																objectifyRun2(
																		o.getState_id()
																		, state
																		, StateDAO::getState
																		, out
																		, gson
																	);
																
																Static_title stateTitle = new Static_title();
																objectifyRun3(
																	state.getTitle_key()
																	, englishLanguage.getId()
																	, stateTitle
																	, Static_titleDAO::getStaticTitleByKeyAndLang
																	, out
																	, gson
																);
																if(stateTitle.getContent() != null) {
																	offerStateString = stateTitle.getContent();
																}
																
															}
															
															try {
																return new OfferTableRow(
																		o.getId()
																		, offerTypeDescriptionString
																		, titleString
																		, offerStateString
																		, DateTransform.ReversedToDirect(o.getCreated_at()));
															} catch (ParseException ex) {
																
																ObjectifyQueryLauncher.printException(ex, out, gson);
																return new OfferTableRow();
															}
														}
													);
										
										offersOut =
												offerTableRowStream.collect(Collectors.toList());
										
										
										RespData rd = new RespData(offersOut);
										String successJson = gson.toJson(rd);
										out.print(successJson);
									} else {
										//Фильтрация, сортировка и проектция преимущественно
										//средствами СУБД
										List<Offer> offers = new ArrayList<>();
										List<Offer> filteredOffers = new ArrayList<>();
										Integer limit =
												Integer.parseInt(req.getParameter(HttpReqParams.limit));
										String[] cursorStr =
												(req.getParameterMap().keySet().contains(HttpReqParams.cursor))
												? new String[] {req.getParameter(HttpReqParams.cursor)}
												: new String[] {null};
										
										OfferFilter offerFilter = new OfferFilter();
										
										//Filter by ...
										
										//by date range
										if(req.getParameterMap().keySet().contains(HttpReqParams.createdDateFrom)) {
											
											offerFilter.createdDateFrom =
													DateTransform.DirectToReversed(req.getParameter(HttpReqParams.createdDateFrom));
										}
										
										if(req.getParameterMap().keySet().contains(HttpReqParams.createdDateTo)) {
											
											offerFilter.createdDateTo =
													DateTransform.DirectToReversed(req.getParameter(HttpReqParams.createdDateTo));
										}
										
										//Order by ...
										if(req.getParameterMap().keySet().contains(HttpReqParams.orderBy)) {
											
											String orderBy = req.getParameter(HttpReqParams.orderBy);
											switch(orderBy) {
											
												case HttpReqParams.sortCreatedAsc : {
													
													offerFilter.orderByCreated = OfferFilter.Order.Asc;
													break;
												}
												case HttpReqParams.sortCreatedDesc : {
													
													offerFilter.orderByCreated = OfferFilter.Order.Desc;
													break;
												}
												case HttpReqParams.sortUrgencyAsc : {
													
													offerFilter.orderByUrgency = OfferFilter.Order.Asc;
													break;
												}
												case HttpReqParams.sortUrgencyDesc : {
													
													offerFilter.orderByUrgency = OfferFilter.Order.Desc;
													break;
												}
												case HttpReqParams.sortStartAsc : {
													
													offerFilter.orderByStart = OfferFilter.Order.Asc;
													break;
												}
												case HttpReqParams.sortStartDesc : {
													
													offerFilter.orderByStart = OfferFilter.Order.Desc;
													break;
												}
												case HttpReqParams.sortFinishAsc : {
													
													offerFilter.orderByFinish = OfferFilter.Order.Asc;
													break;
												}
												case HttpReqParams.sortFinishDesc : {
													
													offerFilter.orderByFinish = OfferFilter.Order.Desc;
													break;
												}
											}
										}
										
										Map<OfferDAO.Params, Object> paramsMap = new HashMap<>();
										paramsMap.put(OfferDAO.Params.OfferList, offers);
										paramsMap.put(OfferDAO.Params.Limit, limit);
										paramsMap.put(OfferDAO.Params.CursorStringArray, cursorStr);
										paramsMap.put(OfferDAO.Params.Filter, offerFilter);
										do {
											objectifyRun(
													paramsMap
													, OfferDAO::getOffersRange
													, out
													, gson
												);
											
											//Если в параметрах запроса от клиета присутствует подстрока заголовка,
											//то после получения из БД диапазона частично отфильтрованных предложений
											//отбираем только те, у которых заголовок содержит заданную подстроку
											//или совпадает с ней
											if(req.getParameterMap().keySet().contains(HttpReqParams.substring)) {
												
												filteredOffers =
													offers.stream().filter((o) -> {
														
														List<Title> offerTitles = new ArrayList<>();
														objectifyRun2(
																o.getTitle_key()
																, offerTitles
																, TitleDAO::getTitlesByKey
																, out
																, gson
															);
														boolean substringPresents = false;
														for (Title title : offerTitles) {
															if (title.getContent().toLowerCase()
																	.contains(
																			req.getParameter(HttpReqParams.substring)
																				.toLowerCase()
																			)
																) {
																substringPresents = true;
																break;
															}
														}
														return substringPresents;
													}).collect(Collectors.toList());
											} else {
												filteredOffers.addAll(offers);
											}
											//Удаляем из выборки предложения, состояние которых равно
											//"полностью выполнено" или "отменено"
											//или число вакансий равно 0
											State fulfilledState = new State();
											objectifyRun2(
													"fulfilled_state_static_title"
													, fulfilledState
													, StateDAO::getStateByTitleKey
													, out
													, gson
												);
											Long fulfilledStateId = fulfilledState.getId();
											State cancelledState = new State();
											objectifyRun2(
													"cancelled_state_static_title"
													, cancelledState
													, StateDAO::getStateByTitleKey
													, out
													, gson
												);
											Long cancelledStateId = cancelledState.getId();
											filteredOffers.removeIf((o) -> {
												
												return o.getState_id().equals(fulfilledStateId)
														|| o.getState_id().equals(cancelledStateId)
														|| (o.getCollaborators_count() != null
															&& o.getCollaborators_count().equals(0));
											});
											//Если в параметрах запроса от клиета присутствует список категорий,
											//то после получения из БД диапазона частично отфильтрованных предложений
											//отбираем только те, которые содержат идентификатор любой из категорий списка
											if(req.getParameterMap().keySet().contains(HttpReqParams.categories)) {

												String[] categories = req.getParameterValues(HttpReqParams.categories);
												filteredOffers.removeIf((o) -> {
													
													boolean categoryIdAbsents = true;
													for (String categoryId : categories) {
														if(((Offer)o).getOffer_type_id().equals(Long.parseLong(categoryId))) {
															categoryIdAbsents = false;
															break;
														}
													}
													return categoryIdAbsents;
												});
											}
											//Дополнительный фильтр по стране
											if(req.getParameterMap().keySet().contains(HttpReqParams.country)) {
												
												filteredOffers.removeIf((o) -> {
													
													if(((Offer)o).getCountry_id() != null) {
														
														Static_title countrySTitle = new Static_title();
														objectifyRun2(
																req.getParameter(HttpReqParams.country)
																, countrySTitle
																, Static_titleDAO::getStaticTitleByContent
																, out
																, gson
															);
														if(countrySTitle.getId() != null) {
															
															Country selectedCountry = new Country();
															objectifyRun2(
																	countrySTitle.getKey()
																	, selectedCountry
																	, CountryDAO::getCountryByTitleKey
																	, out
																	, gson
																);
															return !((Offer)o).getCountry_id()
																		.equals(selectedCountry.getId());
														} else {
															
															return true;
														}
													} else {
													
														return true;
													}
												});
											}
											
											//Дополнительный фильтр по городу
											if(req.getParameterMap().keySet().contains(HttpReqParams.city)) {
												
												filteredOffers.removeIf((o) -> {
													
													if(((Offer)o).getCity_id() != null) {
														
														Static_title citySTitle = new Static_title();
														objectifyRun2(
																req.getParameter(HttpReqParams.city)
																, citySTitle
																, Static_titleDAO::getStaticTitleByContent
																, out
																, gson
															);
														if(citySTitle.getId() != null) {
															
															City selectedCity = new City();
															objectifyRun2(
																	citySTitle.getKey()
																	, selectedCity
																	, CityDAO::getCityByTitleKey
																	, out
																	, gson
																);
															return !((Offer)o).getCity_id()
																		.equals(selectedCity.getId());
														} else {
															
															return true;
														}
													} else {
													
														return true;
													}
												});
											}
											
											//Дополнительный фильтр по текущему числу вакантных мест в предложении
											if(req.getParameterMap().keySet().contains(HttpReqParams.collaboratorsCount)) {
												
												filteredOffers.removeIf((o) -> {
													
													if(((Offer)o).getCollaborators_count() != null) {
																													
														return !((Offer)o).getCollaborators_count()
																		.equals(Integer.parseInt(req.getParameter(HttpReqParams.collaboratorsCount)));
														
													} else {
													
														return true;
													}
												});
											}
											
											//Если после всех фильтров список оказался пуст,
											//но курсор еще существует -
											//делаем повторную попытку получить список,
											//снова дополнительно отфильтровать его по содержанию
											//заданной подстроки в заголовке и снова проверяем
										} while ((cursorStr[0] != null)
												&& filteredOffers.size() == 0);
										
										
										//Задание проекции, чтобы получить из БД значения только для части
										//полей объекта модели
										/*if(req.getParameterMap().keySet().contains(HttpReqParams.projection)) {
											
											String projectionString  = req.getParameter(HttpReqParams.projection);
											switch(projectionString) {
												case HttpReqParams.tableRowProjection:{
													//offerFilter.projection = OfferProjections.tableRow;
													
												}
											}
										}*/
										
										List<OfferGridItem> gridItems =
												filteredOffers.stream()
												.map((o) -> {
													
													//Находим объект названия типа предлажения
													//и его реализацию на текущем языке
													String offerTypeDescriptionString = "-";
													if(o.getOffer_type_id() != null) {
														Offer_type offerType = new Offer_type();
														objectifyRun2(
																o.getOffer_type_id()
																, offerType
																, Offer_typeDAO::getOffer_type
																, out
																, gson
															);
														//offerTypeDescriptionString = "+";
														/*Static_description offerTypeDescription =
																new Static_description();
														objectifyRun3(
															offerType.getDescription_key()
															, englishLanguage.getId()
															, offerTypeDescription
															, Static_descriprionDAO::getStaticDescriptionByKeyAndLang
															, out
															, gson
														);*/
														Static_description offerTypeDescription =
																LocalizeHelper.getLocalizedSDescriptionObject(
																		offerType.getDescription_key()
																		, currentLanguageId
																		, out
																		, gson
																	);
														if(offerTypeDescription.getContent() != null) {
															offerTypeDescriptionString =
																	offerTypeDescription.getContent();
														}
													}
													//Находим заголовок на текущем языке
													String titleString = "-";
													if(o.getTitle_key() != null
															&& !o.getTitle_key().equals("")) {
														Title title = new Title();
														objectifyRun3(
															o.getTitle_key()
															, englishLanguage.getId()
															, title
															, TitleDAO::getTitleByKeyAndLang
															, out
															, gson
														);
														titleString = title.getContent();
													}
													//Находим описание на текущем языке
													String descriptionString = "-";
													if(o.getDescription_key() != null
															&& !o.getDescription_key().equals("")) {
														Description description = new Description();
														objectifyRun3(
															o.getDescription_key()
															, englishLanguage.getId()
															, description
															, DescriptionDAO::getDescriptionByKeyAndLang
															, out
															, gson
														);
														descriptionString = description.getContent();
													}
													//Находим объект типа состояния
													//и его реализацию на текущем языке
													String offerStateString = "-";
													if(o.getState_id() != null) {
														State state = new State();
														objectifyRun2(
																o.getState_id()
																, state
																, StateDAO::getState
																, out
																, gson
															);
														Static_title stateTitle = new Static_title();
														objectifyRun3(
															state.getTitle_key()
															, englishLanguage.getId()
															, stateTitle
															, Static_titleDAO::getStaticTitleByKeyAndLang
															, out
															, gson
														);
														if(stateTitle.getContent() != null) {
															offerStateString = stateTitle.getContent();
														}
														
													}
													
													int imageBytesCount = o.getImage().getBytes().length;
													String noimageUriString = "/img/no-image.png";
													String imageBase64 =
															(imageBytesCount > 0)
															? new String(o.getImage().getBytes())
															: noimageUriString;
													
													try {
														return new OfferGridItem(
																o.getId()
																, offerTypeDescriptionString
																, titleString.length() > 30
																	? titleString.substring(0, 30) + " ..."
																			: titleString
																, offerStateString
																, imageBase64
																, descriptionString.length() > 75
																		? descriptionString.substring(0, 75) + " ..."
																		: descriptionString
																, DateTransform.ReversedToDirect(o.getCreated_at())
															);
													} catch (ParseException ex) {
														
														ObjectifyQueryLauncher.printException(ex, out, gson);
														return new OfferGridItem();
													}
												})
												.collect(Collectors.toList());
										
										//
										List al = new ArrayList<>();
										String nextCursorString = (cursorStr[0] != null) ? cursorStr[0] : "end";
										al.add(new ContinuData(gridItems, nextCursorString));
										RespData rd = new RespData(al);
										String successJson = gson.toJson(rd);
										out.print(successJson);
									}
								} catch (Exception ex) {
									
									ObjectifyQueryLauncher.printException(ex, out, gson);
								}	
								break;
							}
							case HttpReqParams.join : {
								
								try {
									//Получаем из БД объект английского языка
									Language englishLanguage = new Language();
									objectifyRun2(
											"en"
											, englishLanguage
											, LanguageDAO::getLangByCode
											, out
											, gson);
									
									//
									Offer o = new Offer();
									String offerId =
											req.getParameter(HttpReqParams.id);
									objectifyRun2(
											offerId
											, o
											, OfferDAO::getOffer
											, out
											, gson
										);
									//Находим заголовок на текущем языке
									String titleString = "-";
									if(o.getTitle_key() != null
											&& !o.getTitle_key().equals("")) {
										Title title = new Title();
										objectifyRun3(
											o.getTitle_key()
											, englishLanguage.getId()
											, title
											, TitleDAO::getTitleByKeyAndLang
											, out
											, gson
										);
										titleString = title.getContent();
									}
									
									User authorUser = new User();
									objectifyRun2(
											o.getUser_id()
											, authorUser
											, UserDAO::getUser
											, out
											, gson
										);
									
									Long candidateUserId =
										(Long)session.getAttribute(
												SessionAttributes.userId
											);
									User candidateUser = new User();
									objectifyRun2(
											candidateUserId
											, candidateUser
											, UserDAO::getUser
											, out
											, gson
										);
									
									//Помощник по отправке сообщений на электронную почту
									Mailer mailer = new Mailer();
									String messageString =
											"User "
											+ candidateUser.getName()
											+ " wants to join your project: '"
											+ titleString
											+ "' (URL: https://creativetfinder.appspot.com/#find:"
											+ o.getId()
											+ ")"
											+ ". Get in touch with him by email: "
											+ candidateUser.getEmail();
									String subjectString = "Request for join";
									String fromAddressString = "tyaamariupol@gmail.com";
									String fromNameString = "CTFinder";
									String toAddressString = authorUser.getEmail();
									String toNameString = authorUser.getName();
									
									// Отправляем сообщение
									mailer.sendPlainMsg(
											messageString
											, subjectString
											, fromAddressString
											, fromNameString
											, toAddressString
											, toNameString);
									
									// Делаем попытку создать новый узел в веб-приложении "MySkillsOrganizer"
									// (сработает, если туда хотя бы раз заходил тот же пользователь Google,
									// который сейчас кликнул "Присоединиться" в CTFinder)
									
									//Находим описание на текущем языке
									/* String descriptionString = "-";
									if(o.getDescription_key() != null
											&& !o.getDescription_key().equals("")) {
										Description description = new Description();
										objectifyRun3(
											o.getDescription_key()
											, englishLanguage.getId()
											, description
											, DescriptionDAO::getDescriptionByKeyAndLang
											, out
											, gson
										);
										descriptionString = description.getContent();
									}
									
									CTFinder2MySkillsOrganizer.createNode(
											candidateUser.getEmail()
											, titleString
											, descriptionString
										); */
									
									List al = new ArrayList<>();
									al.add(HttpRespWords.sent);
									RespData rd = new RespData(al);
									String successJson = gson.toJson(rd);
									out.print(successJson);
								} catch (Exception ex) {
									
									ObjectifyQueryLauncher.printException(ex, out, gson);
								}	
								break;
							}
							case HttpReqParams.update : {
								//Действие только для обновления состояния предложения.
								//Другие параметры обновляются веткой создания предложения.
								try {
									//Получаем из БД объект английского языка
									Language englishLanguage = new Language();
									objectifyRun2(
											"en"
											, englishLanguage
											, LanguageDAO::getLangByCode
											, out
											, gson);
									
									//
									Offer o = new Offer();
									//
									String offerId =
											req.getParameter(HttpReqParams.id);
									
									//
									String newState  =
											req.getParameter(HttpReqParams.newState);
									
									//Получение объекта из БД
									objectifyRun2(
											offerId
											, o
											, OfferDAO::getOffer
											, out
											, gson
										);
									
									o.setState_id(Long.parseLong(newState));
									DateFormat reversedFormat =
											new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
									String updatedAt = reversedFormat.format(new Date());
									o.setUpdated_at(updatedAt);
									
									objectifyRun(
											o
											, OfferDAO::createOffer
											, out
											, gson
										);
									
									ArrayList<String> al = new ArrayList<>();
									al.add(HttpRespWords.updated);
									RespData rd = new RespData(al);
									String successJson = gson.toJson(rd);
									out.print(successJson);
								} catch (Exception ex) {
									
									ObjectifyQueryLauncher.printException(ex, out, gson);
								}
								break;
							}
							//Отправка вариантов автодополнения для строки поиска по заголовку предложения
							case HttpReqParams.autocomplete : {
								
								List<Offer> offers = new ArrayList<>();
								List<String> titleStrings = new ArrayList<>();
								OfferFilter offerFilter = new OfferFilter();
								TitleFilter titleFilter = new TitleFilter();
								
								final Integer limit = 1000;
								String[] cursorStr = new String[] {null};
								//Integer resultsCount = 0;
								final Integer resultsCountMax = 10;
								
								if(req.getParameterMap().keySet().contains(HttpReqParams.startstring)) {
									
									titleFilter.startString =
											req.getParameter(HttpReqParams.startstring);
								}
										
								if(req.getParameterMap().keySet().contains(HttpReqParams.createdDateFrom)) {
									
									offerFilter.createdDateFrom =
											DateTransform.DirectToReversed(req.getParameter(HttpReqParams.createdDateFrom));
								}
								
								if(req.getParameterMap().keySet().contains(HttpReqParams.createdDateTo)) {
									
									offerFilter.createdDateTo =
											DateTransform.DirectToReversed(req.getParameter(HttpReqParams.createdDateTo));
								}
								
								//Задание проекции, чтобы получить из БД значения только для части
								//полей объекта модели - title_key
								if(req.getParameterMap().keySet().contains(HttpReqParams.projection)) {
									
									String projectionString  = req.getParameter(HttpReqParams.projection);
									switch(projectionString) {
										case HttpReqParams.titleProjection:{
											offerFilter.projection = OfferProjections.title;
											break;
										}
									}
								}
								
								Map<OfferDAO.Params, Object> paramsMap = new HashMap<>();
								//Постоянные параметры запроса
								paramsMap.put(OfferDAO.Params.OfferList, offers);
								paramsMap.put(OfferDAO.Params.Filter, offerFilter);
								paramsMap.put(OfferDAO.Params.Limit, limit);
								//Сменные параметры запроса
								paramsMap.put(OfferDAO.Params.CursorStringArray, cursorStr);
								//Пока не закончились отфильтрованные предложения
								//или пока не получено максимальное число загловков 
								SEARCH_LOOP : do {
									
									objectifyRun(
											paramsMap
											, OfferDAO::getOffersRange
											, out
											, gson
										);
									//Для каждого отфильтрованного предложения
									for (Offer offer : offers) {
										//Находим заголовки на любом языке
										//по ключу
										if(offer.getTitle_key() != null
												&& !offer.getTitle_key().equals("")) {
											List<Title> titles = new ArrayList<>();
											objectifyRun3(
												offer.getTitle_key()
												, titleFilter.startString
												, titles
												, TitleDAO::getTitleByKeyAndStart
												, out
												, gson
											);
											for (Title title : titles) {
												titleStrings.add(title.getContent());
												if(titleStrings.size() == resultsCountMax) {
													break SEARCH_LOOP;
												}
											}
										}
									}
								} while ((cursorStr[0] != null)
										&& offers.size() > 0
										&& titleStrings.size() < resultsCountMax
									);
								
								RespData rd = new RespData(titleStrings);
								String successJson = gson.toJson(rd);
								out.print(successJson);
								break;
							}
							case HttpReqParams.delete : {
								
								try {
									//Get offer's id
									String offerId =
											req.getParameter(HttpReqParams.id);
									Offer o = new Offer();
									//Получение объекта из БД
									objectifyRun2(
											offerId
											, o
											, OfferDAO::getOffer
											, out
											, gson
										);
									
									//Получаем из БД список объектов заголовков
									List<Title> titles = new ArrayList<>();
									objectifyRun2(
										o.getTitle_key()
										, titles
										, TitleDAO::getTitlesByKey
										, out
										, gson
									);
									//Удаление всех объектов заголовков из БД
									for (Title title : titles) {
										objectifyRun(
												title
												, TitleDAO::delete
												, out
												, gson
											);
									}
									//Получаем из БД список объектов Description
									List<Description> descriptions = new ArrayList<>();
									objectifyRun2(
										o.getDescription_key()
										, descriptions
										, DescriptionDAO::getDescriptionsByKey
										, out
										, gson
									);
									//Удаление всех объектов описаний предложения из БД
									for (Description description : descriptions) {
										objectifyRun(
												description
												, DescriptionDAO::delete
												, out
												, gson
											);
									}
									//Удаление объекта предложения из БД
									objectifyRun(
											o
											, OfferDAO::delete
											, out
											, gson
										);
									
									ArrayList<String> al = new ArrayList<>();
									al.add(HttpRespWords.deleted);
									RespData rd = new RespData(al);
									String successJson = gson.toJson(rd);
									out.print(successJson);
								} catch (Exception ex) {
									
									ObjectifyQueryLauncher.printException(ex, out, gson);
								}
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
					
					ObjectifyQueryLauncher.printException(ex, out, gson);
				}
				//ex.printStackTrace();
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
		doGet(request, response);
	}

	//Функция создания объекта новой страны и объекта ее названия в БД
	private Long createCountry(
			String _countryName
			, Long _currentLanguageId
			, PrintWriter _out
			, Gson _gson
			) {
		//Получаем объект английского языка
		Language englishLanguage = new Language();
		objectifyRun2(
				"en"
				, englishLanguage
				, LanguageDAO::getLangByCode
				, _out
				, _gson);
		//Создаем объект английского статического заголовка для новой страны
		//и сохраняем его в БД
		Static_title newCountrySt =
				new Static_title(
						KeyGen.text2KeyText(_countryName) + "_country"
						, englishLanguage.getId()
						, _countryName);		
		objectifyRun(
				newCountrySt
				, Static_titleDAO::createStatic_title
				, _out
				, _gson
			);
		//Создаем объект новой страны с указанием английского статического заголовка
		//и сохраняем его в БД
		Country newCountry =
				new Country(newCountrySt.getKey());
		
		objectifyRun(
				newCountry
				, CountryDAO::createCountry
				, _out
				, _gson
			);
		
		//Если текущий язык - не английский,
		//то дополнительно создаем статический заголовок страны для текущего языка,
		//заносим в него то же самое название новой страны
		//и сохраняем его в БД
		if(!_currentLanguageId.equals(englishLanguage.getId())) {
			
			Static_title newCountryStLocal =
					new Static_title(
							newCountrySt.getKey()
							, _currentLanguageId
							, _countryName);		
			objectifyRun(
					newCountryStLocal
					, Static_titleDAO::createStatic_title
					, _out
					, _gson
				);
		}
		return newCountry.getId();
	}
	
	//Функция создания объекта новой страны и объекта ее названия в БД
	private Long createCity(
			String _cityName
			, Long _countryId
			, Long _currentLanguageId
			, PrintWriter _out
			, Gson _gson
			) {
		//Получаем объект английского языка
		Language englishLanguage = new Language();
		objectifyRun2(
				"en"
				, englishLanguage
				, LanguageDAO::getLangByCode
				, _out
				, _gson);
		//Создаем объект английского статического заголовка для нового города
		//и сохраняем его в БД
		Static_title newCitySt =
				new Static_title(
						KeyGen.text2KeyText(_cityName) + "_city"
						, englishLanguage.getId()
						, _cityName);		
		objectifyRun(
				newCitySt
				, Static_titleDAO::createStatic_title
				, _out
				, _gson
			);
		//Создаем объект нового города с указанием английского статического заголовка
		//и сохраняем его в БД
		City newCity =
				new City(_countryId, newCitySt.getKey());
		objectifyRun(
				newCity
				, CityDAO::createCity
				, _out
				, _gson
			);
		
		//Если текущий язык - не английский,
		//то дополнительно создаем статический заголовок города для текущего языка,
		//заносим в него то же самое название нового города
		//и сохраняем его в БД
		if(!_currentLanguageId.equals(englishLanguage.getId())) {
			
			Static_title newCityStLocal =
					new Static_title(
							newCitySt.getKey()
							, _currentLanguageId
							, _cityName);		
			objectifyRun(
					newCityStLocal
					, Static_titleDAO::createStatic_title
					, _out
					, _gson
				);
		}
		return newCity.getId();
	}
}
