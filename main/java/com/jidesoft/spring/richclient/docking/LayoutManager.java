/*
 * Copyright 2005 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.jidesoft.spring.richclient.docking;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.Resource;
import org.springframework.richclient.application.Application;

import com.jidesoft.docking.DockingManager;
import com.jidesoft.spring.richclient.perspective.Perspective;

/**
 * A simple manager of JIDE layouts that has the ability to save and restore
 * layouts based on page ids and a perspective id, allowing multiple saved
 * layouts per page.
 * 
 * @author Jonny Wray
 *
 */
public class LayoutManager {
	private static final Log logger = LogFactory.getLog(LayoutManager.class);

	private static final String PAGE_LAYOUT = "page_{0}_layout_{1}.layout";
	
	public static boolean isValidLayout(DockingManager manager, String pageId, Perspective perspective){

		String pageLayout = MessageFormat.format(PAGE_LAYOUT, new Object[]{pageId, perspective.getId()});
		return manager.isLayoutAvailable(pageLayout) && 
			manager.isLayoutDataVersionValid(pageLayout);
	}
	
	/**
	 * Loads a the previously saved layout for the current page. If no 
	 * previously persisted layout exists for the given page the built 
	 * in default layout is used.
	 * 
	 * @param manager The docking manager to use
	 * @param pageId The page to get the layout for
	 * @return a boolean saying if the layout requested was previously saved
	 */
	public static boolean loadPageLayoutData(DockingManager manager, String pageId, Perspective perspective){
		manager.beginLoadLayoutData();
		try{
			//if(isValidLayout(manager, pageId, perspective)){
                   try {
				String pageLayout = MessageFormat.format(PAGE_LAYOUT, new Object[]{pageId, perspective.getId()});
				//manager.setLayoutDirectory(".");
				manager.setUsePref(false);
				manager.loadLayoutDataFromFile(pageLayout);
				logger.info("Used existing layout");
				return true;
                   }
                   catch (Exception exc)
			//}
//                        else{
                   {
				logger.info("Using default layout");
                Resource r = Application.instance().getApplicationContext().getResource("classpath:layout/default.layout");
                manager.loadLayoutFrom(r.getInputStream());
				return false;
			}
		}
		catch(Exception e){
			logger.info("Using default layout");
			manager.setUsePref(true);
			manager.loadLayoutData();
			return false;
		}
	}
	
	/**
	 * Saves the current page layout.
	 * 
	 * @param manager The current docking manager
	 * @param pageId The page to saved the layout for
	 */
	public static void savePageLayoutData(DockingManager manager, String pageId, String perspectiveId){
		logger.info("Saving layout for page "+pageId);
		//manager.setLayoutDirectory(".");
		manager.setUsePref(false);
		manager.saveLayoutDataToFile(MessageFormat.format(PAGE_LAYOUT, 
				new Object[]{pageId, perspectiveId}));
	}
}
