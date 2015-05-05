package com.applitools.eyes;

import com.applitools.utils.ArgumentGuard;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.internal.Coordinates;
import org.openqa.selenium.remote.FileDetector;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.RemoteWebElement;

import java.util.ArrayList;
import java.util.List;

public class EyesRemoteWebElement extends RemoteWebElement {
    private final Logger logger;
    private final EyesWebDriver eyesDriver;
    private final RemoteWebElement webElement;

    private final String JS_GET_COMPUTED_STYLE_FORMATTED_STR =
            "var elem = arguments[0]; " +
            "var styleProp = '%s'; " +
            "if (window.getComputedStyle) { " +
                "return window.getComputedStyle(elem, null)" +
                ".getPropertyValue(styleProp);" +
            "} else if (elem.currentStyle) { " +
                "return elem.currentStyle[styleProp];" +
            "} else { " +
                "return null;" +
            "}";

    public EyesRemoteWebElement(Logger logger, EyesWebDriver eyesDriver,
                                RemoteWebElement webElement) {
        super();

        ArgumentGuard.notNull(logger, "logger");
        ArgumentGuard.notNull(eyesDriver, "eyesDriver");
        ArgumentGuard.notNull(webElement, "webElement");

        this.logger = logger;
        this.eyesDriver = eyesDriver;
        this.webElement = webElement;
    }

    public Region getBounds() {
        int left = webElement.getLocation().getX();
        int top = webElement.getLocation().getY();
        int width = 0;
        int height = 0;

        try {
            width = webElement.getSize().getWidth();
            height = webElement.getSize().getHeight();
        } catch (Exception ex) {
            // Not supported on all platforms.
        }

        if (left < 0) {
            width = Math.max(0, width + left);
            left = 0;
        }

        if (top < 0) {
            height = Math.max(0, height + top);
            top = 0;
        }

        return new Region(left, top, width, height);
    }

    /**
     * Returns the computed value of the style property for the current
     * element.
     * @param propStyle The style property which value we would like to
     *                  extract.
     * @return The value of the style property of the element, or {@code null}.
     */
    public String getComputedStyle(String propStyle) {
        String scriptToExec = String.format
                (JS_GET_COMPUTED_STYLE_FORMATTED_STR, propStyle);
        return (String) eyesDriver.executeScript(scriptToExec, this);
    }

    @Override
    public void click() {

        // Letting the driver know about the current action.
        Region currentControl = getBounds();
        eyesDriver.getEyes().addMouseTrigger(MouseAction.Click, this);
        logger.verbose(String.format("click(%s)", currentControl));

        webElement.click();
    }

    @Override
    public WebDriver getWrappedDriver() {
        return eyesDriver;
    }

    @Override
    public String getId() {
        return webElement.getId();
    }

    @Override
    public void setParent(RemoteWebDriver parent) {
        webElement.setParent(parent);
    }

    @Override
    public void setId(String id) {
        webElement.setId(id);
    }

    @Override
    public void setFileDetector(FileDetector detector) {
        webElement.setFileDetector(detector);
    }

    @Override
    public void submit() {
        webElement.submit();
    }

    @Override
    public void sendKeys(CharSequence... keysToSend) {
        for(CharSequence keys : keysToSend) {
            String text = String.valueOf(keys);
            eyesDriver.getEyes().addTextTrigger(this, text);
        }

        webElement.sendKeys(keysToSend);
    }

    @Override
    public void clear() {
        webElement.clear();
    }

    @Override
    public String getTagName() {
        return webElement.getTagName();
    }

    @Override
    public String getAttribute(String name) {
        return webElement.getAttribute(name);
    }

    @Override
    public boolean isSelected() {
        return webElement.isSelected();
    }

    @Override
    public boolean isEnabled() {
        return webElement.isEnabled();
    }

    @Override
    public String getText() {
        return webElement.getText();
    }

    @Override
    public String getCssValue(String propertyName) {
        return webElement.getCssValue(propertyName);
    }

    /**
     * For RemoteWebElement object, the function returns an
     * EyesRemoteWebElement object. For all other types of WebElement,
     * the function returns the original object.
     */
    private WebElement wrapElement(WebElement elementToWrap) {
        WebElement resultElement = elementToWrap;
        if (elementToWrap instanceof RemoteWebElement) {
            resultElement = new EyesRemoteWebElement(logger, eyesDriver,
                    (RemoteWebElement) elementToWrap);
        }
        return resultElement;
    }

    /**
     * For RemoteWebElement object, the function returns an
     * EyesRemoteWebElement object. For all other types of WebElement,
     * the function returns the original object.
     */
    private List<WebElement> wrapElements(List<WebElement>
                                                  elementsToWrap) {
        // This list will contain the found elements wrapped with our class.
        List<WebElement> wrappedElementsList =
                new ArrayList<WebElement>(elementsToWrap.size());

        for (WebElement currentElement : elementsToWrap) {
            if (currentElement instanceof RemoteWebElement) {
                wrappedElementsList.add(new EyesRemoteWebElement(logger,
                        eyesDriver, (RemoteWebElement) currentElement));
            } else {
                wrappedElementsList.add(currentElement);
            }
        }

        return wrappedElementsList;
    }

    @Override
    public List<WebElement> findElements(By by) {
        return wrapElements(webElement.findElements(by));
    }

    @Override
    public WebElement findElement(By by) {
        return wrapElement(webElement.findElement(by));
    }

    @Override
    public WebElement findElementById(String using) {
        return wrapElement(webElement.findElementById(using));
    }

    @Override
    public List<WebElement> findElementsById(String using) {
        return wrapElements(webElement.findElementsById(using));
    }

    @Override
    public WebElement findElementByLinkText(String using) {
        return wrapElement(webElement.findElementByLinkText(using));
    }

    @Override
    public List<WebElement> findElementsByLinkText(String using) {
        return wrapElements(webElement.findElementsByLinkText(using));
    }

    @Override
    public WebElement findElementByName(String using) {
        return wrapElement(webElement.findElementByName(using));
    }

    @Override
    public List<WebElement> findElementsByName(String using) {
        return wrapElements(webElement.findElementsByName(using));
    }

    @Override
    public WebElement findElementByClassName(String using) {
        return wrapElement(webElement.findElementByClassName(using));
    }

    @Override
    public List<WebElement> findElementsByClassName(String using) {
        return wrapElements(webElement.findElementsByClassName(using));
    }

    @Override
    public WebElement findElementByCssSelector(String using) {
        return wrapElement(webElement.findElementByCssSelector(using));
    }

    @Override
    public List<WebElement> findElementsByCssSelector(String using) {
        return wrapElements(webElement.findElementsByCssSelector(using));
    }

    @Override
    public WebElement findElementByXPath(String using) {
        return wrapElement(webElement.findElementByXPath(using));
    }

    @Override
    public List<WebElement> findElementsByXPath(String using) {
        return wrapElements(webElement.findElementsByXPath(using));
    }

    @Override
    public WebElement findElementByPartialLinkText(String using) {
        return wrapElement(webElement.findElementByPartialLinkText(using));
    }

    @Override
    public List<WebElement> findElementsByPartialLinkText(String using) {
        return wrapElements(webElement.findElementsByPartialLinkText(using));
    }

    @Override
    public WebElement findElementByTagName(String using) {
        return wrapElement(webElement.findElementByTagName(using));
    }

    @Override
    public List<WebElement> findElementsByTagName(String using) {
        return wrapElements(webElement.findElementsByTagName(using));
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof  RemoteWebElement) && webElement.equals(obj);
    }

    @Override
    public int hashCode() {
        return webElement.hashCode();
    }

    @Override
    public boolean isDisplayed() {
        return webElement.isDisplayed();
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public Point getLocation() {
        return webElement.getLocation();
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public Dimension getSize() {
        return webElement.getSize();
    }

    @Override
    public Coordinates getCoordinates() {
        return webElement.getCoordinates();
    }

    @Override
    public String toString() {
        return "EyesRemoteWebElement:" + webElement.toString();
    }
}