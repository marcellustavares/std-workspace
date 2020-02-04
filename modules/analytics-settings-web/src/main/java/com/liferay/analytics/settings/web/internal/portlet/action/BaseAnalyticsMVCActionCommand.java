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

package com.liferay.analytics.settings.web.internal.portlet.action;

import com.liferay.analytics.settings.configuration.AnalyticsConfiguration;
import com.liferay.analytics.settings.configuration.AnalyticsConfigurationTracker;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCActionCommand;
import com.liferay.portal.kernel.security.auth.PrincipalException;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.security.permission.PermissionThreadLocal;
import com.liferay.portal.kernel.service.CompanyService;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.settings.SettingsFactory;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.WebKeys;

import java.nio.charset.Charset;

import java.util.Dictionary;
import java.util.Objects;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;

import org.osgi.service.component.annotations.Reference;

/**
 * @author Marcellus Tavares
 */
public abstract class BaseAnalyticsMVCActionCommand
	extends BaseMVCActionCommand {

	protected void checkPermissions(ThemeDisplay themeDisplay)
		throws PrincipalException {

		PermissionChecker permissionChecker =
			PermissionThreadLocal.getPermissionChecker();

		if (!permissionChecker.isCompanyAdmin(themeDisplay.getCompanyId())) {
			throw new PrincipalException();
		}
	}

	protected void disconnectDataSource(
			long companyId, HttpResponse httpResponse)
		throws Exception {

		HttpEntity httpEntity = httpResponse.getEntity();

		JSONObject responseJSONObject = JSONFactoryUtil.createJSONObject(
			EntityUtils.toString(httpEntity, Charset.defaultCharset()));

		String message = responseJSONObject.getString("message");

		if (message.equals("INVALID_TOKEN")) {
			removeCompanyPreferences(companyId);

			analyticsConfigurationTracker.deleteCompanyConfiguration(companyId);
		}
		else {
			throw new PortalException("Invalid token");
		}
	}

	@Override
	protected void doProcessAction(
			ActionRequest actionRequest, ActionResponse actionResponse)
		throws Exception {

		try {
			ThemeDisplay themeDisplay =
				(ThemeDisplay)actionRequest.getAttribute(WebKeys.THEME_DISPLAY);

			checkPermissions(themeDisplay);

			saveCompanyConfiguration(actionRequest, themeDisplay);
		}
		catch (PrincipalException pe) {
			SessionErrors.add(actionRequest, pe.getClass());

			actionResponse.setRenderParameter("mvcPath", "/error.jsp");
		}
	}

	protected void removeCompanyPreferences(long companyId) throws Exception {
		companyService.removePreferences(
			companyId,
			new String[] {
				"liferayAnalyticsConnectionType",
				"liferayAnalyticsDataSourceId", "liferayAnalyticsEndpointURL",
				"liferayAnalyticsFaroBackendSecuritySignature",
				"liferayAnalyticsFaroBackendURL", "liferayAnalyticsGroupIds",
				"liferayAnalyticsURL"
			});
	}

	protected void removeConfigurationProperties(
			long companyId, Dictionary<String, Object> configurationProperties)
		throws Exception {

		configurationProperties.remove("token");

		analyticsConfigurationTracker.deleteCompanyConfiguration(companyId);
	}

	protected void saveCompanyConfiguration(
			ActionRequest actionRequest, ThemeDisplay themeDisplay)
		throws Exception {

		Dictionary<String, Object> configurationProperties =
			analyticsConfigurationTracker.getAnalyticsConfigurationProperties(
				themeDisplay.getCompanyId());

		updateConfigurationProperties(actionRequest, configurationProperties);

		try {
			AnalyticsConfiguration analyticsConfiguration =
				analyticsConfigurationTracker.getAnalyticsConfiguration(
					themeDisplay.getCompanyId());

			if (Objects.equals(
					analyticsConfiguration.liferayAnalyticsDataSourceId(),
					"") &&
				Objects.equals(
					analyticsConfiguration.liferayAnalyticsEndpointURL(), "") &&
				Objects.equals(analyticsConfiguration.token(), "")) {

				return;
			}
		}
		catch (Exception e) {
			if (_log.isInfoEnabled()) {
				_log.info("Analytics configuration not found");
			}

			return;
		}

		String token = (String)configurationProperties.get("token");

		if ((token != null) && !token.isEmpty()) {
			analyticsConfigurationTracker.saveCompanyConfiguration(
				themeDisplay.getCompanyId(), configurationProperties);
		}
	}

	protected abstract void updateConfigurationProperties(
			ActionRequest actionRequest,
			Dictionary<String, Object> configurationProperties)
		throws Exception;

	@Reference
	protected AnalyticsConfigurationTracker analyticsConfigurationTracker;

	@Reference
	protected CompanyService companyService;

	@Reference
	protected SettingsFactory settingsFactory;

	private static final Log _log = LogFactoryUtil.getLog(
		BaseAnalyticsMVCActionCommand.class);

}