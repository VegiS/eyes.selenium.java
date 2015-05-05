/*
 * Applitools SDK for Selenium integration.
 */
package com.applitools.eyes;

import com.applitools.utils.ArgumentGuard;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

class ScreenshotTaker extends RestClient {

    public ScreenshotTaker(Logger logger, URI driverServerUri,
                           String driverSessionId) {
        super(logger, driverServerUri);

        ArgumentGuard.notNullOrEmpty(driverSessionId, "driverSessionId");

        endPoint = endPoint.path("/session/" + driverSessionId +
                "/screenshot");
    }

    public String getScreenshot() {
        Response response;
        WebDriverScreenshot screenshot;
        // Performing the call to get the screenshot
        response = endPoint.request(MediaType.APPLICATION_JSON_TYPE).get();

        // Only OK is acceptable for us.
        List<Integer> validStatusCodes = new ArrayList<Integer>(1);
        validStatusCodes.add(Response.Status.OK.getStatusCode());

        screenshot = parseResponseWithJsonData(response, validStatusCodes,
                WebDriverScreenshot.class);

        return screenshot.getValue();
    }
}