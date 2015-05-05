/*
 * Applitools software.
 */
package com.applitools.eyes;

import com.applitools.utils.ArgumentGuard;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

/**
 * Encapsulates a frame/iframe. This is a generic type class,
 * and it's actual type is determined by the reference used by the user in
 * order to switch into the frame.
 */
public class Frame {
    // A user can switch into a frame by either its name,
    // index or by passing the relevant web element.
    protected final Logger logger;
    protected final WebElement reference;
    protected final String id;
    protected final Location location;
    protected final RectangleSize size;
    protected final Location parentScrollPosition;

    /**
     * @param logger A Logger instance.
     * @param reference The web element for the frame, used as a reference to
     *                  switch into the frame.
     * @param frameId The id of the frame. Can be used later for comparing
     *                two frames.
     * @param location The location of the frame within the current frame.
     * @param size The entire frame size.
     * @param parentScrollPosition The scroll position the frame's parent was
     *                             in when the frame was switched to.
     */
    public Frame(Logger logger, WebElement reference,
                 String frameId, Location location, RectangleSize size,
                 Location parentScrollPosition) {
        ArgumentGuard.notNull(logger, "logger");
        ArgumentGuard.notNull(reference, "reference");
        ArgumentGuard.notNull(frameId, "frameId");
        ArgumentGuard.notNull(location, "location");
        ArgumentGuard.notNull(size, "size");
        ArgumentGuard.notNull(parentScrollPosition, "parentScrollPosition");

        logger.verbose("Frame constructor...");

        this.logger = logger;
        this.reference = reference;
        this.id = frameId;
        this.parentScrollPosition = new Location(parentScrollPosition.getX(),
                parentScrollPosition.getY());
        this.size = size;

        // Frame borders also have effect on the frame's
        // location.
        int leftBorderWidth, topBorderWidth;
        String propValue;
        try {
            logger.verbose("Get frame border left width...");
            if (reference instanceof EyesRemoteWebElement) {
                logger.verbose(
                        "Frame reference is an EyesWebElement, using 'getComputedStyle'.");
                try {
                    propValue = ((EyesRemoteWebElement) reference)
                            .getComputedStyle("border-left-width");
                } catch (WebDriverException e) {
                    logger.verbose(String.format("Using getComputedStyle failed: %s.",
                            e.getMessage()));
                    logger.verbose("Using getCssValue...");
                    propValue = reference.getCssValue("border-left-width");
                }
                logger.verbose("Done!");
            } else {
                // OK, this is weird, we got an element which is not
                // EyesWebElement?? Log it and try to move on.
                logger.verbose("Frame reference is not an EyesWebElement! " +
                        "(when trying to get border-left-width) " +
                        "Element's class: " +
                        reference.getClass().getName());
                logger.verbose("Using getCssValue...");
                propValue = reference.getCssValue("border-left-width");
                logger.verbose("Done!");
            }
            // Convert border value from the format "2px" to int.
            leftBorderWidth = Math.round(Float.valueOf(
                    propValue.trim().replace("px", "")
            ));
            logger.verbose("border-left-width: " + leftBorderWidth);
        } catch (WebDriverException e) {
            logger.verbose(String.format(
                "Couldn't get frame border-left-width: %s. Falling back to default",
                    e.getMessage()));
            leftBorderWidth = 0;
        }
        try {
            logger.verbose("Get frame border top width...");
            if (reference instanceof EyesRemoteWebElement) {
                logger.verbose(
                        "Frame reference is an EyesWebElement, using 'getComputedStyle'.");
                try {
                    propValue = ((EyesRemoteWebElement) reference)
                            .getComputedStyle("border-top-width");
                } catch (WebDriverException e) {
                    logger.verbose(String.format("Using getComputedStyle failed: %s.",
                            e.getMessage()));
                    logger.verbose("Using getCssValue...");
                    propValue = reference.getCssValue("border-top-width");
                }
                logger.verbose("Done!");
            } else {
                // OK, this is weird, we got an element which is not
                // EyesWebElement?? Log it and try to move on.
                logger.verbose("Frame reference is not an EyesWebElement " +
                        "(when trying to get border-top-width) " +
                        "Element's class: " +
                        reference.getClass().getName());
                logger.verbose("Using getCssValue...");
                propValue = reference.getCssValue("border-top-width");
                logger.verbose("Done!");
            }
            topBorderWidth = Math.round(Float.valueOf(
                    propValue.trim().replace("px", "")
            ));
            logger.verbose("border-top-width: " + topBorderWidth);
        } catch (WebDriverException e) {
            logger.verbose(String.format(
                    "Couldn't get frame border-top-width: %s. Falling back to default",
                    e.getMessage()));
            topBorderWidth = 0; 
        }

        Location frameLocation = new Location(location);
        frameLocation.offset(leftBorderWidth, topBorderWidth);
        this.location = frameLocation;
        logger.verbose("Done!");
    }

    public WebElement getReference() {
        return reference;
    }

    public String getId() {
        return id;
    }

    public Location getLocation() {
        return location;
    }

    public RectangleSize getSize() {
        return size;
    }

    public Location getParentScrollPosition() {
        return parentScrollPosition;
    }
}
