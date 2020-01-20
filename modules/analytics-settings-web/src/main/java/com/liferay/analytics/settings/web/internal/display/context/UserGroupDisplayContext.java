/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.analytics.settings.web.internal.display.context;

import com.liferay.analytics.settings.configuration.AnalyticsConfiguration;
import com.liferay.analytics.settings.web.internal.constants.AnalyticsSettingsWebKeys;
import com.liferay.analytics.settings.web.internal.search.UserGroupChecker;
import com.liferay.analytics.settings.web.internal.search.UserGroupSearch;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.model.UserGroup;
import com.liferay.portal.kernel.search.Sort;
import com.liferay.portal.kernel.search.SortFactoryUtil;
import com.liferay.portal.kernel.service.UserGroupLocalServiceUtil;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.SetUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.users.admin.kernel.util.UsersAdminUtil;

import java.util.LinkedHashMap;
import java.util.List;

import javax.portlet.PortletURL;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

/**
 * @author André Miranda
 */
public class UserGroupDisplayContext {

	public UserGroupDisplayContext(
		RenderRequest renderRequest, RenderResponse renderResponse) {

		_analyticsConfiguration =
			(AnalyticsConfiguration)renderRequest.getAttribute(
				AnalyticsSettingsWebKeys.ANALYTICS_CONFIGURATION);
		_renderRequest = renderRequest;
		_renderResponse = renderResponse;
	}

	public String getOrderByType() {
		if (Validator.isNotNull(_orderByType)) {
			return _orderByType;
		}

		_orderByType = ParamUtil.getString(
			_renderRequest, "orderByType", "asc");

		return _orderByType;
	}

	public PortletURL getPortletURL() {
		PortletURL portletURL = _renderResponse.createRenderURL();

		portletURL.setParameter(
			"mvcRenderCommandName", "/view_configuration_screen");
		portletURL.setParameter(
			"configurationScreenKey", "synced-contacts-groups");

		return portletURL;
	}

	public UserGroupSearch getUserGroupSearch() {
		UserGroupSearch userGroupSearch = new UserGroupSearch(
			_renderRequest, getPortletURL());

		userGroupSearch.setOrderByCol(_getOrderByCol());
		userGroupSearch.setOrderByType(getOrderByType());

		Sort sort = SortFactoryUtil.getSort(
			UserGroup.class, _getOrderByCol(), getOrderByType());

		try {
			List<UserGroup> userGroups = UsersAdminUtil.getUserGroups(
				UserGroupLocalServiceUtil.search(
					_getCompanyId(), _getKeywords(), _getUserGroupParams(),
					userGroupSearch.getStart(), userGroupSearch.getEnd(),
					sort));

			userGroupSearch.setResults(userGroups);
		}
		catch (PortalException pe) {
			throw new SystemException(pe);
		}

		userGroupSearch.setRowChecker(
			new UserGroupChecker(
				_renderResponse,
				SetUtil.fromArray(
					_analyticsConfiguration.syncedUserGroupIds())));

		int total = UserGroupLocalServiceUtil.searchCount(
			_getCompanyId(), _getKeywords(), _getUserGroupParams());

		userGroupSearch.setTotal(total);

		return userGroupSearch;
	}

	private long _getCompanyId() {
		ThemeDisplay themeDisplay = (ThemeDisplay)_renderRequest.getAttribute(
			WebKeys.THEME_DISPLAY);

		return themeDisplay.getCompanyId();
	}

	private String _getKeywords() {
		if (_keywords != null) {
			return _keywords;
		}

		_keywords = ParamUtil.getString(_renderRequest, "keywords");

		return _keywords;
	}

	private String _getOrderByCol() {
		if (Validator.isNotNull(_orderByCol)) {
			return _orderByCol;
		}

		_orderByCol = ParamUtil.getString(
			_renderRequest, "orderByCol", "user-group-name");

		return _orderByCol;
	}

	private LinkedHashMap<String, Object> _getUserGroupParams() {
		LinkedHashMap<String, Object> linkedHashMap = new LinkedHashMap<>();

		linkedHashMap.put("active", Boolean.TRUE);

		return linkedHashMap;
	}

	private final AnalyticsConfiguration _analyticsConfiguration;
	private String _keywords;
	private String _orderByCol;
	private String _orderByType;
	private final RenderRequest _renderRequest;
	private final RenderResponse _renderResponse;

}