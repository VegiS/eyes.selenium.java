/*
 * Applitools SDK for Selenium integration.
 */
package com.applitools.eyes;

import com.applitools.utils.ArgumentGuard;
import com.applitools.utils.GeneralUtils;
import com.applitools.utils.ImageUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.awt.image.BufferedImage;
import java.net.URI;

/**
 * The main API gateway for the SDK.
 */
public class Eyes extends EyesBase {

    private static final int USE_DEFAULT_MATCH_TIMEOUT = -1;

    private EyesWebDriver driver;
    private boolean dontGetTitle;


    // Tells Eyes whether to ask for a stitched full page screenshot if the
    // browser only provides a screenshot of the viewport.
    private boolean forceFullPageScreenshot;
    private boolean checkFrame;
    private Region frameWindowToCheck;
    private boolean hideScrollbars;
	private ImageRotation rotation;

    /**
     * Creates a new (possibly disabled) Eyes instance that interacts with the
     * Eyes Server at the specified url.
     *
     * @param serverUrl  The Eyes server URL.
     */
    public Eyes(URI serverUrl) {
        super(serverUrl);

        checkFrame = false;
        frameWindowToCheck = null;
        forceFullPageScreenshot = false;
        dontGetTitle = false;
        hideScrollbars = false;
    }

    @SuppressWarnings("UnusedDeclaration")
    /**
     * Creates a new Eyes instance that interacts with the Eyes cloud
     * service.
     */
    public Eyes() {
        this(getDefaultServerUrl());
    }

    @Override
    public String getBaseAgentId() {
        return "eyes.selenium.java/2.16";
    }

    @SuppressWarnings("UnusedDeclaration")
    /**
     * ﻿Forces a full page screenshot (by scrolling and stitching) if the
     * browser only ﻿supports viewport screenshots).
     *
     * @param shouldForce Whether to force a full page screenshot or not.
     */
    public void setForceFullPageScreenshot(boolean shouldForce) {
        forceFullPageScreenshot = shouldForce;
    }

    @SuppressWarnings("UnusedDeclaration")
    /**
     * @return Whether Eyes should force a full page screenshot.
     */
    public boolean getForceFullPageScreenshot() {
        return forceFullPageScreenshot;
    }

    @SuppressWarnings("UnusedDeclaration")
    /**
     * Hide the scrollbars when taking screenshots.
     * @param shouldHide Whether to hide the scrollbars or not.
     */
    public void setHideScrollbars(boolean shouldHide) {
        hideScrollbars = shouldHide;
    }

    @SuppressWarnings("UnusedDeclaration")
    /**
     *
     * @return Whether or not scrollbars are hidden when taking screenshots.
     */
    public boolean getHideScrollbars() {
        return hideScrollbars;
    }

    @SuppressWarnings("unused")
    /**
     *
     * @return The image rotation data.
     */
    public ImageRotation getRotation() {
        return rotation;
    }

    @SuppressWarnings("unused")
    /**
     *
     * @param rotation The image rotation data.
     */
    public void setRotation(ImageRotation rotation) {
		this.rotation = rotation;
		if (driver != null) {
			driver.setRotation(rotation);
		}
    }

    /**
     * Starts a test.
     *
     * @param driver         The web driver that controls the browser hosting the
     *                       application under test.
     * @param appName        The name of the application under test.
     * @param testName       The test name.
     * @param viewportSize   The required browser's viewport size
     *                       (i.e., the visible part of the document's body) or
     *                       {@code null} to allow any viewport size.
     * @return A wrapped WebDriver which enables Eyes trigger recording and
     * frame handling.
     */
    public WebDriver open(WebDriver driver, String appName, String testName,
                          RectangleSize viewportSize) {

        if (getIsDisabled()) {
            logger.verbose("open(): Ignored");
            return driver;
        }

        openBase(appName, testName, viewportSize);

        ArgumentGuard.notNull(driver, "driver");

        if (driver instanceof RemoteWebDriver) {
            this.driver = new EyesWebDriver(logger, this,
                    (RemoteWebDriver) driver);
        } else if (driver instanceof EyesWebDriver) {
            this.driver = (EyesWebDriver) driver;
        } else {
            String errMsg = "Driver is not a RemoteWebDriver (" +
                    driver.getClass().getName() + ")";
            logger.log(errMsg);
            throw new EyesException(errMsg);
        }
		this.driver.setRotation(rotation);
        return this.driver;
    }

    @SuppressWarnings("UnusedDeclaration")
    /**
     * @see #open(org.openqa.selenium.WebDriver, String, String, RectangleSize).
     * {@code viewportSize} defaults to {@code null}.
     */
    public WebDriver open(WebDriver driver, String appName, String testName) {
        return open(driver, appName, testName, null);
    }

    @SuppressWarnings("UnusedDeclaration")
    /**
     * @see #checkWindow(String). {@code tag} defaults to {@code null}.
     * Default match timeout is used.
     */
    public void checkWindow() {
        checkWindow(null);
    }

    /**
     * @param tag An optional tag to be associated with the snapshot.
     *
     * @see #checkWindow(int, String) . Default match timeout is used.
     */
    public void checkWindow(String tag) {
        checkWindow(USE_DEFAULT_MATCH_TIMEOUT, tag);
    }

    /**
     * Takes a snapshot of the application under test and matches it with
     * the expected output.
     *
     * @param matchTimeout The amount of time to retry matching
     *                     (Milliseconds).
     * @param tag An optional tag to be associated with the snapshot.
     * @throws TestFailedException Thrown if a mismatch is detected and
     *                             immediate failure reports are enabled.
     */
    public void checkWindow(int matchTimeout, String tag) {

        if (getIsDisabled()) {
            logger.log(String.format("CheckWindow(%d, '%s'): Ignored",
                    matchTimeout, tag));
            return;
        }

        logger.log(String.format("CheckWindow(%d, '%s')", matchTimeout,
                tag));

        super.checkWindowBase(
                new RegionProvider() {
                    public Region getRegion() {
                        return Region.EMPTY;
                    }

                    public CoordinatesType getCoordinatesType() {
                        return null;
                    }
                },
                tag,
                false,
                matchTimeout
        );
    }

    @SuppressWarnings("UnusedDeclaration")
    /**
     * @see #checkRegion(Region, int, String).
     * {@code tag} defaults to {@code null}. Default match timeout is used.
     */
    public void checkRegion(Region region) {
        checkRegion(region, USE_DEFAULT_MATCH_TIMEOUT, null);
    }

    @SuppressWarnings("UnusedDeclaration")
    /**
     * Takes a snapshot of the application under test and matches a specific
     * region within it with the expected output.
     *
     * @param region       A non empty region representing the screen region to
     *                     check.
     * @param matchTimeout The amount of time to retry matching.
     *                     (Milliseconds)
     * @param tag          An optional tag to be associated with the snapshot.
     * @throws TestFailedException Thrown if a mismatch is detected and
     *                             immediate failure reports are enabled.
     */
    public void checkRegion(final Region region, int matchTimeout, String tag) {

        if (getIsDisabled()) {
            logger.log(String.format("CheckRegion([%s], %d, '%s'): Ignored",
                    region, matchTimeout, tag));
            return;
        }

        ArgumentGuard.notNull(region, "region");

        logger.verbose(String.format("CheckRegion([%s], %d, '%s')", region,
                matchTimeout, tag));

        super.checkWindowBase(
                new RegionProvider() {

                    public Region getRegion() {
                        return region;
                    }

                    public CoordinatesType getCoordinatesType() {
                        // If we're given a region, it is relative to the
                        // frame's viewport.
                        return CoordinatesType.CONTEXT_AS_IS;
                    }
                },
                tag,
                false,
                matchTimeout
        );
    }

    @SuppressWarnings("UnusedDeclaration")
    /**
     * @see #checkRegion(org.openqa.selenium.WebElement, String).
     * {@code tag} defaults to {@code null}.
     */
    public void checkRegion(WebElement element) {
        checkRegion(element, null);
    }

    @SuppressWarnings("UnusedDeclaration")
    /**
     * @see #checkRegion(org.openqa.selenium.WebElement, int, String).
     * Default match timeout is used.
     */
    public void checkRegion(WebElement element, String tag) {
        checkRegion(element, USE_DEFAULT_MATCH_TIMEOUT, tag);
    }

    @SuppressWarnings("UnusedDeclaration")
    /**
     * Takes a snapshot of the application under test and matches a region of
     * a specific element with the expected region output.
     *
     * @param element      The element which represents the region to check.
     * @param matchTimeout The amount of time to retry matching.
     *                     (Milliseconds)
     * @param tag          An optional tag to be associated with the snapshot.
     * @throws TestFailedException if a mismatch is detected and
     *                             immediate failure reports are enabled
     */
    public void checkRegion(final WebElement element, int matchTimeout,
                            String tag) {
        if (getIsDisabled()) {
            logger.log(String.format("CheckRegion(element, %d, '%s'): Ignored",
                    matchTimeout, tag));
            return;
        }

        ArgumentGuard.notNull(element, "element");

        logger.log(String.format("CheckRegion(element, %d, '%s')",
                matchTimeout, tag));

        // We'll try to scroll to the top/left of the element to make sure as
        // much as possible that we can check it.
        logger.verbose("Getting current scroll position..");
        Location originalScrollPos = driver.getCurrentScrollPosition();
        logger.verbose("Done! Getting element's location..");
        Point elementLocation = element.getLocation();
        logger.verbose("Done! Trying to scroll to element..");
        driver.scrollTo(
                new Location(elementLocation.getX(), elementLocation.getY()));
        logger.verbose("Done! calling checkWindowBase..");
        super.checkWindowBase(
                new RegionProvider() {

                    public Region getRegion() {
                        Point p = element.getLocation();
                        Dimension d = element.getSize();
                        return new Region(p.getX(), p.getY(), d.getWidth(),
                                d.getHeight());
                    }

                    public CoordinatesType getCoordinatesType() {
                        // If we're given a region, it is relative to the
                        // frame's viewport.
                        return CoordinatesType.CONTEXT_RELATIVE;
                    }
                },
                tag,
                false,
                matchTimeout
        );
        logger.verbose("Done! trying to scroll back to original position..");
        driver.scrollTo(originalScrollPos);
        logger.verbose("Done!");
    }

    @SuppressWarnings("UnusedDeclaration")
    /**
     * @see #checkRegion(org.openqa.selenium.By, String).
     * {@code tag} defaults to {@code null}.
     */
    public void checkRegion(By selector) {
        checkRegion(selector, null);
    }

    @SuppressWarnings("UnusedDeclaration")
    /**
     * @see #checkRegion(org.openqa.selenium.By, int, String).
     * Default match timeout is used.
     */
    public void checkRegion(By selector, String tag) {
        checkRegion(selector, USE_DEFAULT_MATCH_TIMEOUT, tag);
    }

    @SuppressWarnings("UnusedDeclaration")
    /**
     * Takes a snapshot of the application under test and matches a region
     * specified by the given selector with the expected region output.
     *
     * @param selector     Selects the region to check.
     * @param matchTimeout The amount of time to retry matching.
     *                     (Milliseconds)
     * @param tag          An optional tag to be associated with the screenshot.
     * @throws TestFailedException if a mismatch is detected and
     *                             immediate failure reports are enabled
     */
    public void checkRegion(By selector, int matchTimeout, String tag) {

        if (getIsDisabled()) {
            logger.log(String.format("CheckRegion(selector, %d, '%s'): Ignored",
                    matchTimeout, tag));
            return;
        }

        checkRegion(driver.findElement(selector), matchTimeout, tag);
    }

    @SuppressWarnings("UnusedDeclaration")
    /**
     * @see #checkRegionInFrame(int, org.openqa.selenium.By, String).
     * {@code tag} defaults to {@code null}.
     */
    public void checkRegionInFrame(int frameIndex, By selector) {
        checkRegionInFrame(frameIndex, selector, null);
    }

    @SuppressWarnings("UnusedDeclaration")
    /**
     * @see #checkRegionInFrame(int, org.openqa.selenium.By, int, String).
     * Default match timeout is used.
     */
    public void checkRegionInFrame(int frameIndex, By selector, String tag) {
        checkRegionInFrame(frameIndex, selector, USE_DEFAULT_MATCH_TIMEOUT,
                tag);
    }

    /**
     * Switches into the given frame, takes a snapshot of the application under
     * test and matches a region specified by the given selector.
     *
     * @param frameIndex   The index of the frame to switch to. (The same index
     *                     as would be used in a call to
     *                     driver.switchTo().frame()).
     * @param selector     A Selector specifying the region to check.
     * @param matchTimeout The amount of time to retry matching.
     *                     (Milliseconds)
     * @param tag          An optional tag to be associated with the snapshot.
     */
    public void checkRegionInFrame(int frameIndex, By selector,
                                   int matchTimeout, String tag) {
        if (getIsDisabled()) {
            logger.log(String.format(
                    "CheckRegionInFrame(%d, selector, %d, '%s'): Ignored",
                    frameIndex, matchTimeout, tag));
            return;
        }
        driver.switchTo().frame(frameIndex);
        checkRegion(selector, matchTimeout, tag);
        driver.switchTo().parentFrame();
    }

    @SuppressWarnings("UnusedDeclaration")
    /**
     * @see #checkRegionInFrame(String, org.openqa.selenium.By, String).
     * {@code tag} defaults to {@code null}.
     */
    public void checkRegionInFrame(String frameNameOrId, By selector) {
        checkRegionInFrame(frameNameOrId, selector, null);
    }

    @SuppressWarnings("UnusedDeclaration")
    /**
     * @see #checkRegionInFrame(String, org.openqa.selenium.By, int, String).
     * Default match timeout is used
     */
    public void checkRegionInFrame(String frameNameOrId, By selector,
                                   String tag) {
        checkRegionInFrame(frameNameOrId, selector, USE_DEFAULT_MATCH_TIMEOUT,
                tag);
    }

    /**
     * Switches into the given frame, takes a snapshot of the application under
     * test and matches a region specified by the given selector.
     *
     * @param frameNameOrId The name or id of the frame to switch to. (as would
     *                      be used in a call to driver.switchTo().frame()).
     * @param selector      A Selector specifying the region to check.
     * @param matchTimeout  The amount of time to retry matching.
     *                      (Milliseconds)
     * @param tag           An optional tag to be associated with the snapshot.
     */
    public void checkRegionInFrame(String frameNameOrId, By selector,
                                   int matchTimeout, String tag) {
        if (getIsDisabled()) {
            logger.log(String.format(
                    "CheckRegionInFrame('%s', selector, %d, '%s'): Ignored",
                    frameNameOrId, matchTimeout, tag));
            return;
        }
        driver.switchTo().frame(frameNameOrId);
        checkRegion(selector, matchTimeout, tag);
        driver.switchTo().parentFrame();
    }

    @SuppressWarnings("UnusedDeclaration")
    /**
     * @see #checkRegionInFrame(org.openqa.selenium.WebElement,
     * org.openqa.selenium.By, String). {@code tag} defaults to {@code null}.
     */
    public void checkRegionInFrame(WebElement frameReference, By selector) {
        checkRegionInFrame(frameReference, selector, null);
    }

    @SuppressWarnings("UnusedDeclaration")
    /**
     * @see #checkRegionInFrame(org.openqa.selenium.WebElement,
     * org.openqa.selenium.By, int, String).
     *
     * Default match timeout is used.
     */
    public void checkRegionInFrame(WebElement frameReference, By selector,
                                   String tag) {
        checkRegionInFrame(frameReference, selector, USE_DEFAULT_MATCH_TIMEOUT,
                tag);
    }

    /**
     * Switches into the given frame, takes a snapshot of the application under
     * test and matches a region specified by the given selector.
     *
     * @param frameReference       The element which is the frame to switch to. (as
     *                             would be used in a call to
     *                             driver.switchTo().frame()).
     * @param selector             A Selector specifying the region to check.
     * @param matchTimeout         The amount of time to retry matching.
     *                             (Milliseconds)
     * @param tag                  An optional tag to be associated with the snapshot.
     */
    public void checkRegionInFrame(WebElement frameReference, By selector,
                                   int matchTimeout, String tag) {
        if (getIsDisabled()) {
            logger.log(String.format(
                    "CheckRegionInFrame(frame, selector, %d, '%s'): Ignored",
                    matchTimeout, tag));
            return;
        }
        driver.switchTo().frame(frameReference);
        checkRegion(selector, matchTimeout, tag);
        driver.switchTo().parentFrame();
    }

    /**
     * Checks the current frame
     * @param matchTimeout The amount of time to retry matching.
     *                     (Milliseconds)
     * @param tag An optional tag to be associated with the snapshot.
     */
    protected void checkCurrentFrame(int matchTimeout, String tag) {

        logger.verbose(String.format("CheckCurrentFrame(%d, '%s')",
                matchTimeout, tag));

        checkFrame = true;

        logger.verbose("Getting screenshot as base64..");
        String screenshot64 = driver.getScreenshotAs(OutputType.BASE64);
        logger.verbose("Done! Building required object...");
        EyesWebDriverScreenshot screenshot =
                new EyesWebDriverScreenshot(logger, driver,
                        ImageUtils.imageFromBase64(screenshot64));
        logger.verbose("Done!");
        frameWindowToCheck = screenshot.getFrameWindow();


        super.checkWindowBase(
                new RegionProvider() {

                    public Region getRegion() {
                        return Region.EMPTY;
                    }

                    public CoordinatesType getCoordinatesType() {
                        return null;
                    }
                },
                tag,
                false,
                matchTimeout
        );
        checkFrame = false;
        frameWindowToCheck = null;
    }

    @SuppressWarnings("UnusedDeclaration")
    /**
     * @see #checkFrame(String, int, String).
     * {@code tag} defaults to {@code null}. Default match timeout is used.
     */
    public void checkFrame(String frameNameOrId) {
        checkFrame(frameNameOrId, USE_DEFAULT_MATCH_TIMEOUT, null);
    }

    @SuppressWarnings("UnusedDeclaration")
    /**
     * @see #checkFrame(String, int, String).
     * Default match timeout is used.
     */
    public void checkFrame(String frameNameOrId, String tag) {
        checkFrame(frameNameOrId, USE_DEFAULT_MATCH_TIMEOUT, tag);
    }

    /**
     * Matches the frame given as parameter, by switching into the frame and
     * using stitching to get an image of the frame.
     *
     * @param frameNameOrId The name or id of the frame to check. (The same
     *                      name/id as would be used in a call to
     *                   driver.switchTo().frame()).
     * @param matchTimeout The amount of time to retry matching.
     *                     (Milliseconds)
     * @param tag An optional tag to be associated with the match.
     */
    public void checkFrame(String frameNameOrId, int matchTimeout, String tag) {
        if (getIsDisabled()) {
            logger.log(String.format("CheckFrame(%s, %d, '%s'): Ignored",
                    frameNameOrId, matchTimeout, tag));
            return;
        }

        ArgumentGuard.notNull(frameNameOrId, "frameNameOrId");

        logger.log(String.format("CheckFrame(%s, %d, '%s')",
                frameNameOrId, matchTimeout, tag));

        logger.verbose("Switching to frame with name/id: " + frameNameOrId +
                " ...");
        driver.switchTo().frame(frameNameOrId);
        logger.verbose("Done.");

        checkCurrentFrame(matchTimeout, tag);

        logger.verbose("Switching back to parent frame");
        driver.switchTo().parentFrame();
        logger.verbose("Done!");
    }

    @SuppressWarnings("UnusedDeclaration")
    /**
     * @see #checkFrame(int, int, String).
     * {@code tag} defaults to {@code null}. Default match timeout is used.
     */
    public void checkFrame(int frameIndex) {
        checkFrame(frameIndex, USE_DEFAULT_MATCH_TIMEOUT, null);
    }

    @SuppressWarnings("UnusedDeclaration")
    /**
     * @see #checkFrame(int, int, String).
     * Default match timeout is used.
     */
    public void checkFrame(int frameIndex, String tag) {
        checkFrame(frameIndex, USE_DEFAULT_MATCH_TIMEOUT, tag);
    }

    /**
     * Matches the frame given as parameter, by switching into the frame and
     * using stitching to get an image of the frame.
     *
     * @param frameIndex   The index of the frame to switch to. (The same index
     *                     as would be used in a call to
     *                     driver.switchTo().frame()).
     * @param matchTimeout The amount of time to retry matching.
     *                     (Milliseconds)
     * @param tag An optional tag to be associated with the match.
     */
    public void checkFrame(int frameIndex, int matchTimeout, String tag) {
        if (getIsDisabled()) {
            logger.log(String.format("CheckFrame(%d, %d, '%s'): Ignored",
                    frameIndex, matchTimeout, tag));
            return;
        }

        ArgumentGuard.greaterThanOrEqualToZero(frameIndex, "frameIndex");

        logger.log(String.format("CheckFrame(%d, %d, '%s')",
                frameIndex, matchTimeout, tag));

        logger.verbose("Switching to frame with index: " + frameIndex + " ...");
        driver.switchTo().frame(frameIndex);
        logger.verbose("Done!");

        checkCurrentFrame(matchTimeout, tag);

        logger.verbose("Switching back to parent frame...");
        driver.switchTo().parentFrame();
        logger.verbose("Done!");

    }

    @SuppressWarnings("UnusedDeclaration")
    /**
     * @see #checkFrame(org.openqa.selenium.WebElement, int, String).
     * {@code tag} defaults to {@code null}. Default match timeout is used.
     */
    public void checkFrame(WebElement frameReference) {
        checkFrame(frameReference, USE_DEFAULT_MATCH_TIMEOUT, null);
    }

    @SuppressWarnings("UnusedDeclaration")
    /**
     * @see #checkFrame(org.openqa.selenium.WebElement, int, String).
     * Default match timeout is used.
     */
    public void checkFrame(WebElement frameReference, String tag) {
        checkFrame(frameReference, USE_DEFAULT_MATCH_TIMEOUT, tag);
    }

    /**
     * Matches the frame given as parameter, by switching into the frame and
     * using stitching to get an image of the frame.
     *
     * @param frameReference The element which is the frame to switch to. (as
     *                       would be used in a call to
     *                       driver.switchTo().frame() ).
     * @param matchTimeout The amount of time to retry matching (milliseconds).
     * @param tag An optional tag to be associated with the match.
     */
    public void checkFrame(WebElement frameReference, int matchTimeout,
                           String tag) {
        if (getIsDisabled()) {
            logger.log(String.format("checkFrame(element, %d, '%s'): Ignored",
                    matchTimeout, tag));
            return;
        }

        ArgumentGuard.notNull(frameReference, "frameReference");

        logger.log(String.format("CheckFrame(element, %d, '%s')",
                matchTimeout, tag));

        logger.verbose("Switching to frame based on element reference...");
        driver.switchTo().frame(frameReference);
        logger.verbose("Done!");

        checkCurrentFrame(matchTimeout, tag);

        logger.verbose("Switching back to parent frame...");
        driver.switchTo().parentFrame();
        logger.verbose("Done!");
    }

    /**
     * Matches the frame given by the frames path, by switching into the frame and
     * using stitching to get an image of the frame.
     * @param framePath The path to the frame to check. This is a list of
     *                  frame names/IDs (where each frame is nested in the
     *                  previous frame).
     * @param matchTimeout The amount of time to retry matching (milliseconds).
     * @param tag An optional tag to be associated with the match.
     */
    public void checkFrame(String[] framePath, int matchTimeout, String tag) {
        if (getIsDisabled()) {
            logger.log(String.format(
                    "checkFrame(framePath, %d, '%s'): Ignored",
                    matchTimeout,
                    tag));
            return;
        }
        ArgumentGuard.notNull(framePath, "framePath");
        ArgumentGuard.greaterThanZero(framePath.length, "framePath.length");
        logger.log(String.format(
                "checkFrame(framePath, %d, '%s')", matchTimeout, tag));
        FrameChain originalFrameChain = driver.getFrameChain();
        // We'll switch into the PARENT frame of the frame we want to check,
        // and call check frame.
        logger.verbose("Switching to parent frame according to frames path..");
        String[] parentFramePath = new String[framePath.length-1];
        System.arraycopy(framePath, 0, parentFramePath, 0,
                parentFramePath.length);
        ((EyesTargetLocator)(driver.switchTo())).frames(parentFramePath);
        logger.verbose("Done! Calling checkFrame..");
        checkFrame(framePath[framePath.length-1], matchTimeout, tag);
        logger.verbose("Done! switching to default content..");
        driver.switchTo().defaultContent();
        logger.verbose("Done! Switching back into the original frame..");
        ((EyesTargetLocator)(driver.switchTo())).frames(originalFrameChain);
        logger.verbose("Done!");
    }

    @SuppressWarnings("UnusedDeclaration")
    /**
     * @see #checkFrame(String[], int, String) .
     * Default match timeout is used.
     */
    public void checkFrame(String[] framesPath, String tag) {
        checkFrame(framesPath, USE_DEFAULT_MATCH_TIMEOUT, tag);
    }

    @SuppressWarnings("UnusedDeclaration")
    /**
     * @see #checkFrame(String[], int, String) .
     * Default match timeout is used.
     * {@code tag} defaults to {@code null}.
     */
    public void checkFrame(String[] framesPath) {
        checkFrame(framesPath, USE_DEFAULT_MATCH_TIMEOUT, null);
    }

    /**
     * Switches into the given frame, takes a snapshot of the application under
     * test and matches a region specified by the given selector.
     * @param framePath The path to the frame to check. This is a list of
     *                  frame names/IDs (where each frame is nested in the
     *                  previous frame).
     * @param selector A Selector specifying the region to check.
     * @param matchTimeout The amount of time to retry matching (milliseconds).
     * @param tag An optional tag to be associated with the snapshot.
     */
    public void checkRegionInFrame(String[] framePath, By selector,
                                   int matchTimeout, String tag) {
        if (getIsDisabled()) {
            logger.log(String.format(
                "checkRegionInFrame(framePath, selector, %d, '%s'): Ignored",
                matchTimeout, tag));
            return;
        }
        ArgumentGuard.notNull(framePath, "framePath");
        ArgumentGuard.greaterThanZero(framePath.length, "framePath.length");
        logger.log(String.format(
                "checkFrame(framePath, %d, '%s')", matchTimeout, tag));
        FrameChain originalFrameChain = driver.getFrameChain();
        // We'll switch into the PARENT frame of the frame we want to check,
        // and call check frame.
        logger.verbose("Switching to parent frame according to frames path..");
        String[] parentFramePath = new String[framePath.length-1];
        System.arraycopy(framePath, 0, parentFramePath, 0,
                parentFramePath.length);
        ((EyesTargetLocator)(driver.switchTo())).frames(parentFramePath);
        logger.verbose("Done! Calling checkRegionInFrame..");
        checkRegionInFrame(framePath[framePath.length-1], selector,
                matchTimeout, tag);
        logger.verbose("Done! switching back to default content..");
        driver.switchTo().defaultContent();
        logger.verbose("Done! Switching into the original frame..");
        ((EyesTargetLocator)(driver.switchTo())).frames(originalFrameChain);
        logger.verbose("Done!");
    }

    @SuppressWarnings("UnusedDeclaration")
    /**
     * @see #checkRegionInFrame(String, org.openqa.selenium.By, int, String) .
     * Default match timeout is used.
     */
    public void checkRegionInFrame(String[] framePath, By selector,
                                   String tag) {
        checkRegionInFrame(framePath, selector, USE_DEFAULT_MATCH_TIMEOUT, tag);
    }

    @SuppressWarnings("UnusedDeclaration")
    /**
     * @see #checkRegionInFrame(String, org.openqa.selenium.By, int, String) .
     * Default match timeout is used.
     * {@code tag} defaults to {@code null}.
     */
    public void checkRegionInFrame(String[] framePath, By selector) {
        checkRegionInFrame(framePath, selector, USE_DEFAULT_MATCH_TIMEOUT,
                null);
    }

    /**
     * Adds a mouse trigger.
     *
     * @param action  Mouse action.
     * @param control The control on which the trigger is activated (context
     *                relative coordinates).
     * @param cursor  The cursor's position relative to the control.
     */
    protected void addMouseTrigger(MouseAction action, Region control,
                                   Location cursor) {
        if (getIsDisabled()) {
            logger.verbose(String.format(
                    "AddMouseTrigger: Ignoring %s (disabled)",
                    action));
            return;
        }

        // Triggers are actually performed on the previous window.
        if (lastScreenshot == null) {
            logger.verbose(String.format(
                    "AddMouseTrigger: Ignoring %s (no screenshot)",
                    action));
            return;
        }

        if (!FrameChain.isSameFrameChain(driver.getFrameChain(),
                ((EyesWebDriverScreenshot) lastScreenshot).getFrameChain())) {
            logger.verbose(String.format(
                    "AddMouseTrigger: Ignoring %s (different frame)",
                    action));
            return;
        }

        addMouseTriggerBase(action, control, cursor);
    }

    /**
     * Adds a mouse trigger.
     *
     * @param action  Mouse action.
     * @param element The WebElement on which the click was called.
     */
    protected void addMouseTrigger(MouseAction action, WebElement element) {
        if (getIsDisabled()) {
            logger.verbose(String.format(
                    "AddMouseTrigger: Ignoring %s (disabled)",
                    action));
            return;
        }

        ArgumentGuard.notNull(element, "element");

        Point pl = element.getLocation();
        Dimension ds = element.getSize();

        Region elementRegion = new Region(pl.getX(), pl.getY(), ds.getWidth(),
                ds.getHeight());

        // Triggers are actually performed on the previous window.
        if (lastScreenshot == null) {
            logger.verbose(String.format(
                    "AddMouseTrigger: Ignoring %s (no screenshot)",
                    action));
            return;
        }

        if (!FrameChain.isSameFrameChain(driver.getFrameChain(),
                ((EyesWebDriverScreenshot) lastScreenshot).getFrameChain())) {
            logger.verbose(String.format(
                    "AddMouseTrigger: Ignoring %s (different frame)",
                    action));
            return;
        }

        // Get the element region which is intersected with the screenshot,
        // so we can calculate the correct cursor position.
        elementRegion = lastScreenshot.getIntersectedRegion
                (elementRegion, CoordinatesType.CONTEXT_RELATIVE);

        addMouseTriggerBase(action, elementRegion,
                elementRegion.getMiddleOffset());
    }

    /**
     * Adds a keyboard trigger.
     *
     * @param control The control's context-relative region.
     * @param text    The trigger's text.
     */
    protected void addTextTrigger(Region control, String text) {
        if (getIsDisabled()) {
            logger.verbose(String.format(
                    "AddTextTrigger: Ignoring '%s' (disabled)", text));
            return;
        }

        if (lastScreenshot == null) {
            logger.verbose(String.format(
                    "AddTextTrigger: Ignoring '%s' (no screenshot)", text));
            return;
        }

        if (!FrameChain.isSameFrameChain(driver.getFrameChain(),
                ((EyesWebDriverScreenshot) lastScreenshot).getFrameChain())) {
            logger.verbose(String.format(
                    "AddTextTrigger: Ignoring '%s' (different frame)", text));
            return;
        }

        addTextTriggerBase(control, text);
    }

    /**
     * Adds a keyboard trigger.
     *
     * @param element The element for which we sent keys.
     * @param text    The trigger's text.
     */
    protected void addTextTrigger(WebElement element, String text) {
        if (getIsDisabled()) {
            logger.verbose(String.format(
                    "AddTextTrigger: Ignoring '%s' (disabled)", text));
            return;
        }
        ArgumentGuard.notNull(element, "element");

        Point pl = element.getLocation();
        Dimension ds = element.getSize();

        Region elementRegion = new Region(pl.getX(), pl.getY(), ds.getWidth(),
                ds.getHeight());

        addTextTrigger(elementRegion, text);
    }

    @Override
    protected RectangleSize getViewportSize() {
        int width = 0;
        int height = 0;

        try {
            width = driver.extractViewportWidth();
            height = driver.extractViewportHeight();
            return new RectangleSize(width, height);
        } catch (Exception ex) {
            logger.verbose(String.format(
                "getViewportSize(): Failed to extract viewport size using Javascript: %s",
                ex.getMessage()));
        }
        // If we failed to extract the viewport size using JS, will use the
        // window size instead.
        if (width == 0 || height == 0) {
            logger.verbose(
                    "getViewportSize(): Using window size as viewport size.");
            Dimension windowSize = driver.manage().window().getSize();
            width = windowSize.getWidth();
            height = windowSize.getHeight();
            if (driver.isLandscapeOrientation() && height > width) {
                //noinspection SuspiciousNameCombination
                int height2 = width;
                //noinspection SuspiciousNameCombination
                width = height;
                height = height2;
            }
        }

        return new RectangleSize(width, height);
    }

    @Override
    protected void setViewportSize(RectangleSize size) {
        logger.verbose("setViewportSize(" + size + ")");

        final int SLEEP = 1000;
        final int RETRIES = 3;

        Dimension startingBrowserSize =
                new Dimension(size.getWidth(), size.getHeight());

        FrameChain originalFrame = driver.getFrameChain();
        driver.switchTo().defaultContent();

        int retriesLeft = RETRIES;
        Dimension browserSize;
        logger.verbose("Trying to set browser size to: " + startingBrowserSize);
        do {
            driver.manage().window().setSize(startingBrowserSize);
            GeneralUtils.sleep(SLEEP);
            browserSize = driver.manage().window().getSize();
            logger.verbose("Current browser size: " + browserSize);
        } while (--retriesLeft > 0 && !browserSize.equals(startingBrowserSize));

        if (!browserSize.equals(startingBrowserSize)) {
            String errMsg = "Failed to set browser size!";
            logger.log(errMsg);
            // Just in case the user will continue using the driver after the
            // exception.
            ((EyesTargetLocator) driver.switchTo()).frames(originalFrame);
            throw new TestFailedException(errMsg);
        }

        RectangleSize actualViewportSize = getViewportSize();
        logger.verbose("setViewportSize(): initial viewport size:" +
                actualViewportSize);

        driver.manage().window().setSize(new Dimension(
                (2 * browserSize.width) - actualViewportSize.getWidth(),
                (2 * browserSize.height) - actualViewportSize.getHeight()));

        retriesLeft = RETRIES;
        do {
            GeneralUtils.sleep(SLEEP);
            actualViewportSize = getViewportSize();
            logger.verbose("setViewportSize(): viewport size: "
                    + actualViewportSize);
        } while (--retriesLeft > 0 && !actualViewportSize.equals(size));

        if (!actualViewportSize.equals(size)) {
            // One last attempt. Solves the "maximized browser" bug (border size
            // for maximized browser sometimes different than non-maximized, so
            // the original browser size calculation is wrong).
            logger.verbose("SetViewportSize(): attempting one more time...");
            browserSize = driver.manage().window().getSize();
            Dimension updatedBrowserSize = new Dimension(
                    browserSize.width +
                            (size.getWidth() - actualViewportSize.getWidth()),
                    browserSize.height +
                            (size.getHeight() - actualViewportSize.getHeight()));

            logger.verbose("SetViewportSize(): browser size: " + browserSize);
            logger.verbose("SetViewportSize(): required browser size: " +
                    updatedBrowserSize);
            driver.manage().window().setSize(updatedBrowserSize);

            retriesLeft = RETRIES;
            do {
                GeneralUtils.sleep(SLEEP);
                actualViewportSize = getViewportSize();
                logger.verbose("SetViewportSize(): browser size: "
                        + driver.manage().window().getSize());
                logger.verbose("setViewportSize(): viewport size: "
                        + actualViewportSize);
            } while (--retriesLeft > 0 && !actualViewportSize.equals(size));
        }

        if (!actualViewportSize.equals(size)) {
            String errMsg = "Failed to set the viewport size.";
            logger.log(errMsg);
            // Just in case the user will continue using the driver after the
            // exception.
            ((EyesTargetLocator) driver.switchTo()).frames(originalFrame);
            throw new TestFailedException(errMsg);
        }

        ((EyesTargetLocator) driver.switchTo()).frames(originalFrame);
        this.viewportSize = size;
    }

    @Override
    protected EyesScreenshot getScreenshot() {

        logger.verbose("getScreenshot()");
        EyesWebDriverScreenshot result;

        String originalOverflow = null;
        if (hideScrollbars) {
            originalOverflow = driver.hideScrollbars();
        }
        try {
            if (checkFrame) {
                logger.verbose("Check frame requested");
                BufferedImage entireFrame = driver.getStitchedRegion(
                        new RegionProvider() {
                            public Region getRegion() {
                                return frameWindowToCheck;
                            }

                            public CoordinatesType getCoordinatesType() {
                                return CoordinatesType.SCREENSHOT_AS_IS;
                            }
                        });
                logger.verbose("Building screenshot object...");
                result = new EyesWebDriverScreenshot(logger, driver, entireFrame,
                        new RectangleSize(entireFrame.getWidth(),
                                entireFrame.getHeight()));
                logger.verbose("Done!");
            } else if (forceFullPageScreenshot) {
                logger.verbose("Full page screenshot requested.");
                result = new EyesWebDriverScreenshot(logger, driver,
                        driver.getFullPageScreenshot());
                logger.verbose("Done!");
            } else {
                logger.verbose("Screenshot requested...");
                String screenshot64 = driver.getScreenshotAs(OutputType.BASE64);
                logger.verbose("Done! Creating image object...");
                BufferedImage screenshotImage = ImageUtils.imageFromBase64(
                        screenshot64);
                logger.verbose("Done! Creating screenshot object...");
                result = new EyesWebDriverScreenshot(logger, driver,
                        screenshotImage);
                logger.verbose("Done!");
            }
            return result;
        } finally {
            if (hideScrollbars) {
                driver.setOverflow(originalOverflow);
            }
        }
    }

    @Override
    protected String getTitle() {
        if (!dontGetTitle) {
            try {
                return driver.getTitle();
            } catch (Exception ex) {
                logger.verbose("getTitle(): failed (" + ex.getMessage() + ")");
                dontGetTitle = true;
            }
        }

        return "";
    }

    @Override
    protected String getInferredEnvironment() {
        String userAgent = driver.getUserAgent();
        if (userAgent != null) {
            return "useragent:" + userAgent;
        }

        return null;
    }

    /**
     * {@inheritDoc}
     *
     * This override also checks for mobile operating system.
     */
    @Override
    protected AppEnvironment getAppEnvironment() {

        AppEnvironment appEnv = super.getAppEnvironment();

        // If hostOs isn't set, we'll try and extract and OS ourselves.
        if (appEnv.getOs() == null) {
            logger.log("No OS set, checking for mobile OS...");
            if (driver.isMobileDevice()) {
                String platformName = null;
                logger.log("Mobile device detected! Checking device type..");
                if (driver.isAndroid()) {
                    logger.log("Android detected.");
                    platformName = "Android";
                } else if (driver.isIOS()) {
                    logger.log("iOS detected.");
                    platformName = "iOS";
                } else {
                    logger.log("Unknown device type.");
                }
                // We only set the OS if we identified the device type.
                if (platformName != null) {
                    String os = platformName;
                    String platformVersion = driver.getPlatformVersion();
                    if (platformVersion != null) {
                        String majorVersion =
                                platformVersion.split("\\.", 2)[0];

                        if (!majorVersion.isEmpty()) {
                            os += " " + majorVersion;
                        }
                    }

                    logger.verbose("Setting OS: " + os);
                    appEnv.setOs(os);
                }
            } else {
                logger.log("No mobile OS detected.");
            }
        }
        return appEnv;
    }
}
