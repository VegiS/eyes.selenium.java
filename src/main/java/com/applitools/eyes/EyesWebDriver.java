package com.applitools.eyes;

import com.applitools.utils.ArgumentGuard;
import com.applitools.utils.GeneralUtils;
import com.applitools.utils.ImageUtils;
import com.applitools.utils.NetworkUtils;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.remote.MobileCapabilityType;
import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.interactions.*;
import org.openqa.selenium.internal.*;
import org.openqa.selenium.remote.*;

import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * An Eyes implementation of the interfaces implemented by
 * {@link org.openqa.selenium.remote.RemoteWebDriver}.
 * Used so we'll be able to return the users an object with the same
 * functionality as {@link org.openqa.selenium.remote.RemoteWebDriver}.
 */
public class EyesWebDriver implements HasCapabilities, HasInputDevices,
        FindsByClassName, FindsByCssSelector, FindsById, FindsByLinkText,
        FindsByName, FindsByTagName, FindsByXPath, JavascriptExecutor,
        SearchContext, TakesScreenshot, WebDriver, HasTouchScreen {

    // This should pretty much cover all scroll bars (and some fixed position
    // footer elements :).
    private static final int MAX_SCROLL_BAR_SIZE = 50;
    private static final int MIN_SCREENSHOT_PART_HEIGHT = 10;

    // See Applitools WiKi for explanation.
    private static final String JS_GET_VIEWPORT_WIDTH =
            "var width = undefined;" +
            " if (window.innerWidth) {width = window.innerWidth;}" +
            " else if (document.documentElement " +
                    "&& document.documentElement.clientWidth) " +
                        "{width = document.documentElement.clientWidth;}" +
            " else { var b = document.getElementsByTagName('body')[0]; " +
                    "if (b.clientWidth) {" +
                        "width = b.clientWidth;}" +
                    "};" +
            "return width;";

    private static final String JS_GET_VIEWPORT_HEIGHT =
            "var height = undefined;" +
            "  if (window.innerHeight) {height = window.innerHeight;}" +
            "  else if (document.documentElement " +
                    "&& document.documentElement.clientHeight) " +
                        "{height = document.documentElement.clientHeight;}" +
            "  else { var b = document.getElementsByTagName('body')[0]; " +
                        "if (b.clientHeight) {height = b.clientHeight;}" +
                    "};" +
            "return height;";

    private final Logger logger;
    private final Eyes eyes;
    private final RemoteWebDriver driver;
    private final TouchScreen touch;
    private final ScreenshotTaker screenshotTaker;
    private final Map<String, WebElement> elementsIds;
    private final FrameChain frameChain;
    private ImageRotation rotation;

    /**
     * Rotates the image as necessary. The rotation is either manually forced
     * by passing a non-null ImageRotation, or automatically inferred.
     *
     * @param driver The driver which produced the screenshot.
     * @param image The image to normalize.
     * @param rotation The degrees by which to rotate the image:
     *                 positive values = clockwise rotation,
     *                 negative values = counter-clockwise,
     *                 0 = force no rotation, null = rotate automatically
     *                 when needed.
     * @return A normalized image.
     */
    public static BufferedImage normalizeRotation(EyesWebDriver driver,
                                                  BufferedImage image,
                                                  ImageRotation rotation) {
        ArgumentGuard.notNull(driver, "driver");
        ArgumentGuard.notNull(image, "image");
        BufferedImage normalizedImage = image;
        if (rotation != null) {
            if (rotation.getRotation() != 0) {
                normalizedImage = ImageUtils.rotateImage(image,
                        rotation.getRotation());
            }
        } else { // Do automatic rotation if necessary
            if (driver.isMobileDevice() && driver.isLandscapeOrientation() &&
                    image.getHeight() > image.getWidth()) {
                // For Android, we need to rotate images to the right, and for
                // iOS to the left.
                int degrees = driver.isAndroid() ? 90 : -90;
                normalizedImage = ImageUtils.rotateImage(image, degrees);
            }
        }

        return normalizedImage;
    }

    public EyesWebDriver(Logger logger, Eyes eyes, RemoteWebDriver driver)
            throws EyesException {
        ArgumentGuard.notNull(logger, "logger");
        ArgumentGuard.notNull(eyes, "eyes");
        ArgumentGuard.notNull(driver, "driver");

        this.logger = logger;
        this.eyes = eyes;
        this.driver = driver;
        elementsIds = new HashMap<String, WebElement>();
        this.frameChain = new FrameChain(logger);

        // initializing "touch" if possible
        ExecuteMethod executeMethod = null;
        try {
            executeMethod = new RemoteExecuteMethod(driver);
        } catch (Exception e) {
            // If an exception occurred, we simply won't instantiate "touch".
        }
        if (null != executeMethod) {
            touch = new EyesTouchScreen(logger, this,
                    new RemoteTouchScreen(executeMethod));
        } else {
            touch = null;
        }

        URL remoteWebDriverServerUrl = getRemoteWebDriverServerUrl();
        String remoteWebDriverSessionId = getSessionId();

        logger.verbose("EyesWebDriver(): Driver session is "
                + remoteWebDriverSessionId + " @ " + remoteWebDriverServerUrl);

        logger.verbose("EyesWebDriver(): Driver can take screenshots");
        screenshotTaker = null;

        // TODO - remove the block below if indeed unnecesssary
//        logger.verbose("EyesWebDriver(): Driver can't take screenshots");
//        try {
//            screenshotTaker = new ScreenshotTaker(
//                    logger,
//                    remoteWebDriverServerUrl.toURI(),
//                    remoteWebDriverSessionId
//            );
//        } catch (URISyntaxException ex) {
//            String errMsg = "Can't take screenshots!";
//            logger.log(errMsg);
//            throw new EyesException(errMsg, ex);
//        }
    }

    public Eyes getEyes() {
        return eyes;
    }

    @SuppressWarnings("UnusedDeclaration")
    public RemoteWebDriver getRemoteWebDriver() {
        return driver;
    }

    public TouchScreen getTouch() {
        return touch;
    }

    /**
     *
     * @return The image rotation data.
     */
    public ImageRotation getRotation() {
        return rotation;
    }

    /**
     *
     * @param rotation The image rotation data.
     */
    public void setRotation(ImageRotation rotation) {
        this.rotation = rotation;
    }

    /**
     *
     * @return {@code true} if the platform running the test is a mobile
     * platform. {@code false} otherwise.
     */
    public boolean isMobileDevice() {
        return driver instanceof AppiumDriver;
    }

    /**
     *
     * @return {@code true} if the driver is an Android driver.
     * {@code false} otherwise.
     */
    public boolean isAndroid() {
        return driver instanceof AndroidDriver;
    }

    /**
     *
     * @return {@code true} if the driver is an iOS driver.
     * {@code false} otherwise.
     */
    public boolean isIOS() {
        return driver instanceof IOSDriver;
    }

    /**
     *
     * @return {@code true} if the
     */
    public boolean isLandscapeOrientation() {
        if (driver instanceof AppiumDriver) {
            try {
                return ((AppiumDriver) driver).getOrientation() ==
                        ScreenOrientation.LANDSCAPE;
            } catch (WebDriverException e) {
                // Ignored. Some drivers have no 'orientation' attribute, and
                // that's fine.
            }
        }
        logger.verbose(
                "driver has no 'orientation' attribute. Assuming Portrait.");
        return false;
    }

    /**
     *
     * @return The plaform version or {@code null} if it is undefined.
     */
    public String getPlatformVersion() {
        Capabilities capabilities = getCapabilities();
        Object platformVersionObj =
                capabilities.getCapability
                        (MobileCapabilityType.PLATFORM_VERSION);

        return platformVersionObj == null ?
                 null : String.valueOf(platformVersionObj);
    }

    public void get(String s) {
        frameChain.clear();
        driver.get(s);
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    public String getTitle() {
        return driver.getTitle();
    }

    public List<WebElement> findElements(By by) {
        List<WebElement> foundWebElementsList = driver.findElements(by);

        // This list will contain the found elements wrapped with our class.
        List<WebElement> resultElementsList =
                new ArrayList<WebElement>(foundWebElementsList.size());

        for (WebElement currentElement : foundWebElementsList) {
            if (currentElement instanceof RemoteWebElement) {
                resultElementsList.add(new EyesRemoteWebElement(logger, this,
                        (RemoteWebElement) currentElement));

                // For Remote web elements, we can keep the IDs
                elementsIds.put(((RemoteWebElement) currentElement).getId(),
                        currentElement);

            } else {
                throw new EyesException(String.format(
                        "findElements: element is not a RemoteWebElement: %s",
                        by));
            }
        }

        return resultElementsList;
    }

    public WebElement findElement(By by) {
        WebElement webElement = driver.findElement(by);
        if (webElement instanceof RemoteWebElement) {
            webElement = new EyesRemoteWebElement(logger, this,
                    (RemoteWebElement) webElement);

            // For Remote web elements, we can keep the IDs,
            // for Id based lookup (mainly used for Javascript related
            // activities).
            elementsIds.put(((RemoteWebElement) webElement).getId(),
                    webElement);
        } else {
            throw new EyesException(String.format(
                    "findElement: Element is not a RemoteWebElement: %s", by));
        }

        return webElement;
    }

    /**
     * Found elements are sometimes accessed by their IDs (e.g. tapping an
     * element in Appium).
     * @return Maps of IDs for found elements.
     */
    @SuppressWarnings("UnusedDeclaration")
    public Map<String, WebElement> getElementIds () {
        return elementsIds;
    }

    public String getPageSource() {
        return driver.getPageSource();
    }

    public void close() {
        driver.close();
    }

    public void quit() {
        driver.quit();
    }

    public Set<String> getWindowHandles() {
        return driver.getWindowHandles();
    }

    public String getWindowHandle() {
        return driver.getWindowHandle();
    }

    public TargetLocator switchTo() {
        logger.verbose("switchTo()");
        return new EyesTargetLocator(logger, this, driver.switchTo(),
                new EyesTargetLocator.OnWillSwitch() {
                    public void willSwitchToFrame(
                            EyesTargetLocator.TargetType targetType,
                            WebElement targetFrame) {
                        logger.verbose("willSwitchToFrame()");
                        switch(targetType) {
                            case DEFAULT_CONTENT:
                                logger.verbose("Default content.");
                                frameChain.clear();
                                break;
                            case PARENT_FRAME:
                                logger.verbose("Parent frame.");
                                frameChain.pop();
                                break;
                            default: // Switching into a frame
                                logger.verbose("Frame");
                                String frameId = ((EyesRemoteWebElement)
                                        targetFrame).getId();
                                Point pl = targetFrame.getLocation();
                                Dimension ds = targetFrame.getSize();
                                frameChain.push(new Frame(logger, targetFrame,
                                        frameId,
                                        new Location(pl.getX(), pl.getY()),
                                        new RectangleSize(ds.getWidth(),
                                                ds.getHeight()),
                                        getCurrentScrollPosition()));
                        }
                        logger.verbose("Done!");
                    }

                    public void willSwitchToWindow(String nameOrHandle) {
                        logger.verbose("willSwitchToWindow()");
                        frameChain.clear();
                        logger.verbose("Done!");
                    }
                });
    }

    public Navigation navigate() {
        return driver.navigate();
    }

    public Options manage() {
        return driver.manage();
    }

    public Mouse getMouse() {
        return new EyesMouse(logger, this,
                driver.getMouse());
    }

    public Keyboard getKeyboard() {
        return new EyesKeyboard(logger, this, driver.getKeyboard());
    }

    public WebElement findElementByClassName(String className) {
        return findElement(By.className(className));
    }

    public List<WebElement> findElementsByClassName(String className) {
        return findElements(By.className(className));
    }

    public WebElement findElementByCssSelector(String cssSelector) {
        return findElement(By.cssSelector(cssSelector));
    }

    public List<WebElement> findElementsByCssSelector(String cssSelector) {
        return findElements(By.cssSelector(cssSelector));
    }

    public WebElement findElementById(String id) {
        return findElement(By.id(id));
    }

    public List<WebElement> findElementsById(String id) {
        return findElements(By.id(id));
    }

    public WebElement findElementByLinkText(String linkText) {
        return findElement(By.linkText(linkText));
    }

    public List<WebElement> findElementsByLinkText(String linkText) {
        return findElements(By.linkText(linkText));
    }

    public WebElement findElementByPartialLinkText(String partialLinkText) {
        return findElement(By.partialLinkText(partialLinkText));
    }

    public List<WebElement> findElementsByPartialLinkText(String
                                                                  partialLinkText) {
        return findElements(By.partialLinkText(partialLinkText));
    }

    public WebElement findElementByName(String name) {
        return findElement(By.name(name));
    }

    public List<WebElement> findElementsByName(String name) {
        return findElements(By.name(name));
    }

    public WebElement findElementByTagName(String tagName) {
        return findElement(By.tagName(tagName));
    }

    public List<WebElement> findElementsByTagName(String tagName) {
        return findElements(By.tagName(tagName));
    }

    public WebElement findElementByXPath(String path) {
        return findElement(By.xpath(path));
    }

    public List<WebElement> findElementsByXPath(String path) {
        return findElements(By.xpath(path));
    }

    public Capabilities getCapabilities() {
        return driver.getCapabilities();
    }

    public Object executeScript(String script, Object... args) {

        // Appium commands are sometimes sent as Javascript
        if (AppiumJsCommandExtractor.isAppiumJsCommand(script)) {
            Trigger trigger =
                    AppiumJsCommandExtractor.extractTrigger(elementsIds,
                            driver.manage().window().getSize(), script, args);

            if (trigger != null) {
                // TODO - Daniel, additional type of triggers
                if (trigger instanceof MouseTrigger) {
                    MouseTrigger mt = (MouseTrigger) trigger;
                    eyes.addMouseTrigger(mt.getMouseAction(),
                            mt.getControl(), mt.getLocation());
                }
            }
        }
        logger.verbose("Execute script...");
        Object result = driver.executeScript(script, args);
        logger.verbose("Done!");
        return result;
    }

    public Object executeAsyncScript(String script, Object... args) {

        // Appium commands are sometimes sent as Javascript
        if (AppiumJsCommandExtractor.isAppiumJsCommand(script)) {
            Trigger trigger =
                    AppiumJsCommandExtractor.extractTrigger(elementsIds,
                            driver.manage().window().getSize(), script, args);

            if (trigger != null) {
                // TODO - Daniel, additional type of triggers
                if (trigger instanceof MouseTrigger) {
                    MouseTrigger mt = (MouseTrigger) trigger;
                    eyes.addMouseTrigger(mt.getMouseAction(),
                            mt.getControl(), mt.getLocation());
                }
            }
        }

        return driver.executeAsyncScript(script, args);
    }

    /**
     * Sets the overflow of the current context's document element
     * @param value The overflow value to set.
     * @return The previous overflow value (could be {@code null} if undefined).
     */
    public String setOverflow(String value) {
        logger.verbose("setOverflow()");
        String script;
        if (value == null) {
            script =
                "var origOverflow = document.documentElement.style.overflow; " +
                "document.documentElement.style.overflow = undefined; " +
                "return origOverflow";
        } else {
            script = String.format(
                "var origOverflow = document.documentElement.style.overflow; " +
                        "document.documentElement.style.overflow = \"%s\"; " +
                        "return origOverflow",
                value);
        }
        String originalOverflow = (String) executeScript(script);

        logger.verbose("Done!");
        return originalOverflow;
    }

    /**
     * Hides the scrollbars of the current context's document element.
     * @return The previous value of the overflow property (could be
     *          {@code null}).
     */
    public String hideScrollbars() {
        return setOverflow("hidden");
    }

    protected int extractViewportWidth() {
        logger.verbose("extractViewportWidth()");
        int viewportWidth = Integer.parseInt(
                executeScript(JS_GET_VIEWPORT_WIDTH).toString()
        );
        logger.verbose("Done!");
        return viewportWidth;
    }

    protected int extractViewportHeight() {
        logger.verbose("extractViewportHeight()");
        int result = Integer.parseInt(
                executeScript(JS_GET_VIEWPORT_HEIGHT).toString()
        );
        logger.verbose("Done!");
        return result;
    }

    /**
     * @return The scroll position of the current frame.
     */
    public Location getCurrentScrollPosition() {
        logger.verbose("getCurrentScrollPosition()");
        int x,y;
        Object xo, yo;

        xo = executeScript("return window.scrollX");
        if (xo == null) {
            // IE
            xo = executeScript("var doc = document.documentElement; var left = (window.pageXOffset || doc.scrollLeft) - (doc.clientLeft || 0); return left");
            if (xo == null) {
                throw new EyesException(
                        "Could not get left scroll position!");
            }
        }

        yo = executeScript("return window.scrollY");
        if (yo == null) {
            // For IE
            yo = executeScript("var doc = document.documentElement; var top = (window.pageYOffset || doc.scrollTop)  - (doc.clientTop || 0); return top");
            if (yo == null) {
                throw new EyesException(
                        "Could not get top scroll position");
            }
        }

        x = Integer.parseInt(xo.toString());
        y = Integer.parseInt(yo.toString());

        Location result = new Location(x, y);
        logger.verbose(String.format("Current position: %s", result));
        return result;
    }

    /**
     *
     * @return The size of the entire page based on the scroll width/height.
     */
    public RectangleSize getEntirePageSize() {
        logger.verbose("getEntirePageSize()");
        int scrollWidth =
                Integer.parseInt(executeScript
                        ("return document.documentElement.scrollWidth")
                        .toString());

        int bodyScrollWidth =
                Integer.parseInt(executeScript
                        ("return document.body.scrollWidth")
                        .toString());

        int totalWidth = Math.max(scrollWidth, bodyScrollWidth);

        // IMPORTANT: Notice there's a major difference between scrollWidth
        // and scrollHeight. While scrollWidth is the maximum between an
        // element's width and its content width, scrollHeight might be
        // smaller (!) than the clientHeight, which is why we take the
        // maximum between them.
        int clientHeight =
                Integer.parseInt(executeScript
                        ("return document.documentElement.clientHeight")
                        .toString());
        int bodyClientHeight =
                Integer.parseInt(executeScript
                        ("return document.body.clientHeight")
                        .toString());
        int scrollHeight =
                Integer.parseInt(executeScript
                        ("return document.documentElement.scrollHeight")
                        .toString());
        int bodyScrollHeight =
                Integer.parseInt(executeScript
                        ("return document.body.scrollHeight")
                        .toString());
        int maxDocumentElementHeight = Math.max(clientHeight, scrollHeight);
        int maxBodyHeight = Math.max(bodyClientHeight, bodyScrollHeight);
        int totalHeight = Math.max(maxDocumentElementHeight, maxBodyHeight);


        RectangleSize result = new RectangleSize(totalWidth, totalHeight);
        logger.verbose(String.format("Entire size: %s", result));
        return result;
    }

    /**
     *
     * @return The viewport size of the default content (outer most frame).
     */
    public RectangleSize getDefaultContentViewportSize() {
        logger.verbose("getDefaultContentViewportSize()");
        RectangleSize viewportSize;
        FrameChain currentFrames = new FrameChain(logger, frameChain);
        switchTo().defaultContent();
        try {
            logger.verbose("Getting viewport size...");
            viewportSize = new RectangleSize(extractViewportWidth(),
                    extractViewportHeight());
            logger.verbose("Done!");
        } catch (Exception e) {
            // There are platforms for which we can't extract the viewport size
            // (e.g. Appium)
            logger.verbose(
                    "Can't get viewport size, using window size instead..");
            Dimension windowSize = manage().window().getSize();
            viewportSize = new RectangleSize(windowSize.getWidth(),
                    windowSize.getHeight());
        }
        ((EyesTargetLocator) switchTo()).frames(currentFrames);
        return viewportSize;
    }

    /**
     * Scrolls to the given position.
     * @param scrollPosition The position to scroll to.
     */
    public void scrollTo(Location scrollPosition) {
        logger.verbose(String.format("Scrolling to %s", scrollPosition));
        executeScript(String.format("window.scrollTo(%d,%d)",
                scrollPosition.getX(), scrollPosition.getY()));
        logger.verbose("Done scrolling!");
    }

    /**
     *
     * @return A copy of the current frame chain.
     */
    public FrameChain getFrameChain() {
        return new FrameChain(logger, frameChain);
    }

    /**
     * Returns a stitching of a region.
     * @param regionProvider A provider of the region to stitch. If {@code
     *                       getRegion} returns {@code Region.EMPTY}, the
     *                       entire image will be stitched.
     * @return An image which represents the stitched region.
     */
    public BufferedImage getStitchedRegion(RegionProvider regionProvider) {
        logger.verbose("getStitchedRegion()");

        ArgumentGuard.notNull(regionProvider, "regionProvider");

        // Saving the original scroll (in case we were already in the
        // outermost frame).
        Location originalScrollPosition = getCurrentScrollPosition();
        Location currentScrollPosition;

        int scrollRetries = 3;
        do {
            scrollTo(new Location(0, 0));
            // Give the scroll time to stabilize
            GeneralUtils.sleep(150);
            currentScrollPosition = getCurrentScrollPosition();
        } while (currentScrollPosition.getX() != 0
                && currentScrollPosition.getY() !=0
                && (--scrollRetries > 0));

        if (currentScrollPosition.getX() != 0
                || currentScrollPosition.getY() != 0) {
            scrollTo(originalScrollPosition);
            throw new EyesException(
                    "Couldn't scroll to the top/left of the frame!");
        }

        RectangleSize entirePageSize = getEntirePageSize();


        logger.verbose("Getting top/left screenshot as base64...");
        String screenshot64 = getScreenshotAs(OutputType.BASE64);
        logger.verbose("Done getting base64! Getting BufferedImage..");
        BufferedImage image = ImageUtils.imageFromBase64(screenshot64);
        logger.verbose("Done! Creating screenshot object...");
        // We need the screenshot to be able to convert the region to
        // screenshot coordinates.
        EyesWebDriverScreenshot screenshot =
                new EyesWebDriverScreenshot(logger, this, image);
        logger.verbose("Done! Getting region in screenshot...");
        Region regionInScreenshot =
                screenshot.convertRegionLocation(regionProvider.getRegion(),
                        regionProvider.getCoordinatesType(),
                        CoordinatesType.SCREENSHOT_AS_IS);
        logger.verbose("Done!");
        if (!regionInScreenshot.isEmpty()) {
            image = ImageUtils.getImagePart(image, regionInScreenshot);
        }

        if (image.getWidth() >= entirePageSize.getWidth() &&
                image.getHeight() >= entirePageSize.getHeight()) {
            scrollTo(originalScrollPosition);

            return image;
        }

        // The screenshot part is a bit smaller than the screenshot size,
        // in order to eliminate duplicate bottom scroll bars, as well as fixed
        // position footers.
        RectangleSize partImageSize =
                new RectangleSize(image.getWidth(),
                        Math.max(image.getHeight() - MAX_SCROLL_BAR_SIZE,
                                MIN_SCREENSHOT_PART_HEIGHT));

        logger.verbose(String.format("Total size: %s, image part size: %s",
                entirePageSize, partImageSize));

        // Getting the list of viewport regions composing the page (we'll take
        // screenshot for each one).
        Region entirePage = new Region(Location.ZERO, entirePageSize);
        Iterable<Region> imageParts =
                entirePage.getSubRegions(partImageSize);

        logger.verbose("Creating stitchedImage container...");
        //Notice stitchedImage uses the same type of image as the screenshots.
        BufferedImage stitchedImage = new BufferedImage(
                entirePageSize.getWidth(), entirePageSize.getHeight(),
                image.getType());
        logger.verbose("Done! Adding initial screenshot..");
        // Starting with the screenshot we already captured at (0,0).
        stitchedImage.getRaster().setRect(0, 0, image.getData());
        logger.verbose("Done!");

        // Take screenshot and stitch for each screenshot part.
        logger.verbose("Getting the rest of the image parts...");
        BufferedImage partImage;
        for (Region partRegion: imageParts) {
            // Skipping screenshot for 0,0 (already taken)
            if (partRegion.getLeft() == 0 && partRegion.getTop() == 0) {
                continue;
            }
            logger.verbose(String.format("Taking screenshot for %s",
                    partRegion));
            // Scroll to the part's top/left.
            scrollTo(partRegion.getLocation());
            // Giving the scroll time to stabilize.
            GeneralUtils.sleep(100);
            // Screen size may cause the scroll to only reach part of the way.
            currentScrollPosition = getCurrentScrollPosition();
            logger.verbose(String.format("Scrolled to %s",
                    currentScrollPosition));

            // Actually taking the screenshot.
            logger.verbose("Get screenshot as base64...");
            String part64 = getScreenshotAs(OutputType.BASE64);
            logger.verbose("Done! Creating the image object...");
            partImage = ImageUtils.imageFromBase64(part64);
            logger.verbose("Done!");

            if (!regionInScreenshot.isEmpty()) {
                partImage = ImageUtils.getImagePart(partImage,
                        regionInScreenshot);
            }

            // Stitching the current part.
            logger.verbose("Stitching part into the image container...");
            stitchedImage.getRaster().setRect(currentScrollPosition.getX(),
                    currentScrollPosition.getY(), partImage.getData());
            logger.verbose("Done!");
        }

        logger.verbose("Stitching done!");
        scrollTo(originalScrollPosition);
        return stitchedImage;
    }

    /**
     * Creates a full page image by scrolling the viewport and "stitching"
     * the screenshots to each other.
     *
     * @return The image of the entire page.
     */
    public BufferedImage getFullPageScreenshot() {
        logger.verbose("Getting full page screenshot..");

        // Save the current frame path.
        FrameChain originalFrame = getFrameChain();

        switchTo().defaultContent();

        BufferedImage fullPageImage = getStitchedRegion(new RegionProvider() {
            public Region getRegion() {
                return Region.EMPTY;
            }

            public CoordinatesType getCoordinatesType() {
                return null;
            }
        });

        ((EyesTargetLocator)switchTo()).frames(originalFrame);

        return fullPageImage;
    }

    public <X> X getScreenshotAs(OutputType<X> xOutputType)
            throws WebDriverException {
        // Get the image as base64.
        String screenshot64 = driver.getScreenshotAs(OutputType.BASE64);
        BufferedImage screenshot = ImageUtils.imageFromBase64(screenshot64);
        screenshot = normalizeRotation(this, screenshot, rotation);

        // Return the image in the requested format.
        screenshot64 = ImageUtils.base64FromImage(screenshot);
        return xOutputType.convertFromBase64Png(screenshot64);

        // TODO - Remove the code block below if indeed unnecessary.
//        if (screenshotTaker == null) {
//            return driver.getScreenshotAs(xOutputType);
//        }
//
//        // Get base64 screenshot using the Wire protocol.
//        if (xOutputType != OutputType.BASE64) {
//            throw new EyesException("Screenshot OutputType not supported");
//        }
//
//        String screenshot = screenshotTaker.getScreenshot();
//        return xOutputType.convertFromBase64Png(screenshot);
    }

    /**
     * Firefox local instances do not support the HttpCommandExecutor
     * (though they use it internally).
     *
     * @return The url of the remote web server (which implements the web
     *         driver wire protocol).
     * @throws EyesException
     */
    private static URL getFirefoxServerUrl(RemoteWebDriver driver) throws
            EyesException {
        FirefoxDriver firefoxDriver = (FirefoxDriver) driver;
        CommandExecutor firefoxCommandExecutor =
                firefoxDriver.getCommandExecutor();
        URL remoteWebDriverServerUrl;
        try {
            Field connectionField = firefoxCommandExecutor.getClass()
                    .getDeclaredField("connection");
            connectionField.setAccessible(true);
            Object connectionValue = connectionField.get(firefoxCommandExecutor);

            Field delegateField = connectionValue.getClass()
                    .getDeclaredField("delegate");
            delegateField.setAccessible(true);
            HttpCommandExecutor httpCommandExecutor =
                    (HttpCommandExecutor) delegateField.get(connectionValue);
            remoteWebDriverServerUrl =
                    httpCommandExecutor.getAddressOfRemoteServer();
        } catch (NoSuchFieldException e) {
            throw new EyesException(
                    "Failed to Remote Server Address for Firefox", e);
        } catch (IllegalAccessException e) {
            throw new EyesException(
                    "Failed to Remote Server Address for Firefox", e);
        }
        return remoteWebDriverServerUrl;
    }

    /**
     * Gets the web driver's server URL.
     */
    private URL getRemoteWebDriverServerUrl() {
        CommandExecutor commandExecutor = driver.getCommandExecutor();

        URL remoteWebDriverServerUrl;

        if (driver instanceof FirefoxDriver) {
            remoteWebDriverServerUrl = getFirefoxServerUrl(driver);
        } else {
            remoteWebDriverServerUrl = ((HttpCommandExecutor) commandExecutor)
                    .getAddressOfRemoteServer();
        }

        // We must have the web driver's URL.
        if (remoteWebDriverServerUrl == null) {
            throw new EyesException(
                    "Failed to get remote web driver URL!");
        }

        String webDriverHost = remoteWebDriverServerUrl.getHost();
        boolean convertToGlobalIP = webDriverHost.equals("127.0.0.1")
                || webDriverHost.equals("localhost");
        boolean isIE = driver instanceof InternetExplorerDriver;
        boolean isFireFox = driver instanceof FirefoxDriver;
        if (convertToGlobalIP && !isIE && !isFireFox) {
            String localIP;

            try {
                localIP = NetworkUtils.getLocalIp();
            } catch (Exception e) {
                throw new EyesException("Failed to get local IP!", e);
            }

            // if there's no network available, we assume localhost
            if (localIP == null) {
                localIP = "localhost";
            }

            try {
                remoteWebDriverServerUrl =
                        new URL(remoteWebDriverServerUrl.getProtocol(), localIP,
                                remoteWebDriverServerUrl.getPort(),
                                remoteWebDriverServerUrl.getPath());
            } catch (MalformedURLException e) {
                throw new EyesException("Could not compose URL for " +
                        "the remote web driver!", e);
            } catch (NullPointerException e) {
                throw new EyesException("Could not compose URL for " +
                        "the remote web driver!", e);
            }
        }
        return remoteWebDriverServerUrl;
    }

    public String getUserAgent() {
        String userAgent;
        try {
            userAgent = (String) this.driver.executeScript(
                    "return navigator.userAgent");
            logger.verbose("getUserAgent(): '" + userAgent + "'");
        } catch (Exception e) {
            logger.verbose("getUserAgent(): Failed to obtain user-agent string");
            userAgent = null;
        }

        return userAgent;
    }

    private String getSessionId() {
        // extract remote web driver information
        return driver.getSessionId().toString();
    }
}
