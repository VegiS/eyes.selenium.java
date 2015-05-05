/*
 * Applitools SDK for Selenium integration.
 */
package com.applitools.eyes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Encapsulation for the WebDriver wire protocol "screenshot" command response.
 */

// Different browsers return different parameters in addition to "value".
@JsonIgnoreProperties(ignoreUnknown = true)
class WebDriverScreenshot {
    private String value;

    void setValue(String value) {
        this.value = value;
    }

    String getValue() {
        return value;
    }

}