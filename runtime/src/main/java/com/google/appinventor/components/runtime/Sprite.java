// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2019 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.components.runtime;

import android.os.Handler;

import com.google.appinventor.components.common.Direction;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.errors.IllegalArgumentError;
import com.google.appinventor.components.runtime.util.BoundingBox;
import com.google.appinventor.components.runtime.util.TimerInternal;
import com.google.appinventor.components.runtime.util.YailList;

import java.util.HashSet;
import java.util.Set;

/**
 * Superclass of sprites able to move and interact with other sprites.
 * <p>
 * While the Simple programmer sees the x- and y-coordinates as integers,
 * they are maintained internally as doubles so fractional changes (caused
 * by multiplying the speed by a cosine or sine value) have the chance to
 * add up.
 *
 * @author spertus.google.com (Ellen Spertus)
 */
/* @SimpleObject
 */public abstract class Sprite extends VisibleComponent
        implements AlarmHandler, OnDestroyListener, Deleteable {
    protected static final boolean DEFAULT_ORIGIN_AT_CENTER = false;
    private static final String LOG_TAG = "Sprite";
    private static final boolean DEFAULT_ENABLED = true;  // Enable timer for movement
    private static final int DEFAULT_HEADING = 0;      // degrees
    private static final int DEFAULT_INTERVAL = 100;  // ms
    private static final float DEFAULT_SPEED = 0.0f;   // pixels per interval
    private static final boolean DEFAULT_VISIBLE = true;
    private static final double DEFAULT_Z = 1.0;
    private static final int DIRECTION_NONE = 0;
    protected final Canvas canvas;              // enclosing Canvas
    private final TimerInternal timerInternal;  // timer to control movement
    private final Handler androidUIHandler;     // for posting actions

    // Keeps track of which other sprites are currently colliding with this one.
    // That way, we don't raise CollidedWith() more than once for each collision.
    // Events are only raised when sprites are added to this collision set.  They
    // are removed when they no longer collide.
    private final Set<Sprite> registeredCollisions;

    // This variable prevents events from being raised before construction of
    // all components has taken place.  This was added to fix bug 2262218.
    protected boolean initialized = false;

    // Properties: These are protected, instead of private, both so they
    // can be used by subclasses and tests.
    protected int interval;      // number of milliseconds until next move
    protected boolean visible = true;
    protected double xLeft;      // leftmost x-coordinate
    protected double yTop;       // uppermost y-coordinate
    protected double zLayer;     // z-coordinate, higher values go in front
    protected float speed;       // magnitude in pixels

    // Added to support having coordinates at center.
    protected boolean originAtCenter;
    protected double xCenter;
    protected double yCenter;

    protected Form form;

    /**
     * The angle, in degrees above the positive x-axis, specified by the user.
     * This is private in order to enforce that changing it also changes
     * {@link #heading}, {@link #headingRadians}, {@link #headingCos}, and
     * {@link #headingSin}.
     */
    protected double userHeading;

    /**
     * The angle, in degrees <em>below</em> the positive x-axis, specified by the
     * user.  We use this to compute new coordinates because, on Android, the
     * y-coordinate increases "below" the x-axis.
     */
    protected double heading;
    protected double headingRadians;  // heading in radians
    protected double headingCos;      // cosine(heading)
    protected double headingSin;      // sine(heading)

    /**
     * Creates a new Sprite component.  This version exists to allow injection
     * of a mock handler for testing.
     *
     * @param container where the component will be placed
     * @param handler   a scheduler to which runnable events will be posted
     */
    protected Sprite(ComponentContainer container, Handler handler) {
        super();
        androidUIHandler = handler;

        // Add to containing Canvas.
        if (!(container instanceof Canvas)) {
            throw new IllegalArgumentError("Sprite constructor called with container " + container);
        }
        this.canvas = (Canvas) container;
        this.canvas.addSprite(this);

        // Maintain a list of collisions.
        registeredCollisions = new HashSet<Sprite>();

        // Set in motion.
        timerInternal = new TimerInternal(this, DEFAULT_ENABLED, DEFAULT_INTERVAL, handler);

        this.form = container.$form();

        // Set default property values.
        OriginAtCenter(DEFAULT_ORIGIN_AT_CENTER);
        Heading(0);  // Default initial heading
        Enabled(DEFAULT_ENABLED);
        Interval(DEFAULT_INTERVAL);
        Speed(DEFAULT_SPEED);
        Visible(DEFAULT_VISIBLE);
        Z(DEFAULT_Z);

        container.$form().registerForOnDestroy(this);
    }

    /**
     * Creates a new Sprite component.  This is called by the constructors of
     * concrete subclasses, such as {@link Ball} and {@link ImageSprite}.
     *
     * @param container where the component will be placed
     */
    protected Sprite(ComponentContainer container) {
        // Note that although this is creating a new Handler, there is
        // only one UI thread in an Android app and posting to this
        // handler queues up a Runnable for execution on that thread.
        this(container, new Handler());
    }

    protected static double coerceToDouble(Object o) {
        if (o instanceof Number) {
            return ((Number) o).doubleValue();
        } else {
            try {
                return Double.parseDouble(o.toString());
            } catch (NumberFormatException e) {
                return Double.NaN;
            }
        }
    }

    // Properties (Enabled, Heading, Interval, Speed, Visible, X, Y, Z, OriginAtCenter)
    // The SimpleProperty annotations for X and Y appear in the concrete
    // subclasses so each can have its own description. Currently, OriginAtCenter
    // is a property of Ball only.

    /**
     * Determines whether two sprites are in collision.  Note that we cannot
     * merely see whether the rectangular regions around each intersect, since
     * some types of sprite, such as BallSprite, are not rectangular.
     *
     * @param sprite1 one sprite
     * @param sprite2 another sprite
     * @return {@code true} if they are in collision, {@code false} otherwise
     */
    public static boolean colliding(Sprite sprite1, Sprite sprite2) {
        // If the bounding boxes don't intersect, there can be no collision.
        BoundingBox rect1 = sprite1.getBoundingBox(1);
        BoundingBox rect2 = sprite2.getBoundingBox(1);
        if (!rect1.intersectDestructively(rect2)) {
            return false;
        }

        // If we get here, rect1 has been mutated to hold the intersection of the
        // two bounding boxes.  Now check every point in the intersection to see if
        // both sprites contain that point.
        // TODO(user): Handling abutting sprites properly
        for (double x = rect1.getLeft(); x <= rect1.getRight(); x++) {
            for (double y = rect1.getTop(); y <= rect1.getBottom(); y++) {
                if (sprite1.containsPoint(x, y) && sprite2.containsPoint(x, y)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void Initialize() {
        initialized = true;
        canvas.registerChange(this);
    }

    /**
     * Controls whether the `%type%` moves when its speed is non-zero.
     *
     * @return {@code true} indicates a running timer, {@code false} a stopped
     * timer
     */
  /* @SimpleProperty(
      description = "Controls whether the %type% moves and can be interacted with " +
          "through collisions, dragging, touching, and flinging.") */
    public boolean Enabled() {
        return timerInternal.Enabled();
    }

    /**
     * Enabled property setter method: starts or stops the timer.
     *
     * @param enabled {@code true} starts the timer, {@code false} stops it
     * @suppressdoc
     */
  /* @DesignerProperty(
      editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN,
      defaultValue = DEFAULT_ENABLED ? "True" : "False") */
    /* @SimpleProperty(category = PropertyCategory.BEHAVIOR) */
    public void Enabled(boolean enabled) {
        timerInternal.Enabled(enabled);
    }

    /**
     * The `%type%`'s heading in degrees above the positive x-axis. Zero degrees is toward the right
     * of the screen; 90 degrees is toward the top of the screen.
     *
     * @return degrees above the positive x-axis
     */
  /* @SimpleProperty(
      description = "Returns the %type%'s heading in degrees above the positive " +
          "x-axis.  Zero degrees is toward the right of the screen; 90 degrees is toward the " +
          "top of the screen.") */
    public double Heading() {
        return userHeading;
    }

    /**
     * Sets heading in which sprite should move.  In addition to changing the
     * local variables {@link #userHeading} and {@link #heading}, this
     * sets {@link #headingCos}, {@link #headingSin}, and {@link #headingRadians}.
     *
     * @param userHeading degrees above the positive x-axis
     * @suppressdoc
     */
    /* @SimpleProperty(category = PropertyCategory.BEHAVIOR) */
  /* @DesignerProperty(
      editorType = PropertyTypeConstants.PROPERTY_TYPE_FLOAT,
      defaultValue = DEFAULT_HEADING + "") */
    public void Heading(double userHeading) {
        this.userHeading = userHeading;
        // Flip, because y increases in the downward direction on Android canvases
        heading = -userHeading;
        headingRadians = Math.toRadians(heading);
        headingCos = Math.cos(headingRadians);
        headingSin = Math.sin(headingRadians);
        // changing the heading needs to force a redraw for image sprites that rotate
        registerChange();
    }

    /**
     * The interval in milliseconds at which the `%type%`'s position is updated. For example, if the
     * `Interval` is 50 and the {@link #Speed(float)} is 10, then the `%type%` will move 10 pixels
     * every 50 milliseconds.
     *
     * @return timer interval in ms
     */
  /* @SimpleProperty(
      description = "The interval in milliseconds at which the %type%'s " +
          "position is updated.  For example, if the interval is 50 and the speed is 10, " +
          "then every 50 milliseconds the sprite will move 10 pixels in the heading direction.") */
    public int Interval() {
        return timerInternal.Interval();
    }

    /**
     * Interval property setter method: sets the interval between timer events.
     *
     * @param interval timer interval in ms
     * @suppressdoc
     */
  /* @DesignerProperty(
      editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER,
      defaultValue = DEFAULT_INTERVAL + "") */
    /* @SimpleProperty(category = PropertyCategory.BEHAVIOR) */
    public void Interval(int interval) {
        timerInternal.Interval(interval);
    }

    /**
     * The speed at which the `%type%` moves. The `%type%` moves this many pixels every
     * {@link #Interval()} milliseconds if {@link #Enabled(boolean)} is `true`{:.logic.block}.
     *
     * @param speed the magnitude (in pixels) to move every {@link #interval}
     *              milliseconds
     */
  /* @SimpleProperty(
      description = "The number of pixels that the %type% should move every interval, if enabled.",
      category = PropertyCategory.BEHAVIOR) */
  /* @DesignerProperty(
      editorType = PropertyTypeConstants.PROPERTY_TYPE_FLOAT,
      defaultValue = DEFAULT_SPEED + "") */
    public void Speed(float speed) {
        this.speed = speed;
    }

    /**
     * Gets the speed with which this sprite moves.
     *
     * @return the magnitude (in pixels) the sprite moves every {@link #interval}
     * milliseconds.
     * @suppressdoc
     */
  /* @SimpleProperty(
    description = "The speed at which the %type% moves. The %type% moves " +
        "this many pixels every interval if enabled.") */
    public float Speed() {
        return speed;
    }

    /**
     * The `Visible` property determines whether the %type% is visible (`true`{:.logic.block}) or
     * invisible (`false`{:.logic.block}).
     *
     * @return {@code true} if the sprite is visible, {@code false} otherwise
     */
    /* @SimpleProperty(description = "Whether the %type% is visible.") */
    public boolean Visible() {
        return visible;
    }

    /**
     * Sets whether sprite should be visible.
     *
     * @param visible {@code true} if the sprite should be visible; {@code false}
     *                otherwise.
     */
  /* @DesignerProperty(
      editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN,
      defaultValue = DEFAULT_VISIBLE ? "True" : "False") */
    /* @SimpleProperty(category = PropertyCategory.APPEARANCE) */
    public void Visible(boolean visible) {
        this.visible = visible;
        registerChange();
    }

    public double X() {
        return originAtCenter ? xCenter : xLeft;
    }

    private double xLeftToCenter(double xLeft) {
        return xLeft + Width() / 2;
    }

    private double xCenterToLeft(double xCenter) {
        return xCenter - Width() / 2;
    }

    // Note that this does not call registerChange(). This was pulled out of X()
    // so both X and Y could be changed with only a single call to registerChange().
    private void updateX(double x) {
        if (originAtCenter) {
            xCenter = x;
            xLeft = xCenterToLeft(x);
        } else {
            xLeft = x;
            xCenter = xLeftToCenter(x);
        }
    }

    /* @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_FLOAT,
        defaultValue = "0.0") */
  /* @SimpleProperty(
      category = PropertyCategory.APPEARANCE) */
    public void X(double x) {
        updateX(x);
        registerChange();
    }

    private double yTopToCenter(double yTop) {
        return yTop + Width() / 2;
    }

    private double yCenterToTop(double yCenter) {
        return yCenter - Width() / 2;
    }

    // Note that this does not call registerChange(). This was pulled out of Y()
    // so both X and Y could be changed with only a single call to registerChange().
    private void updateY(double y) {
        if (originAtCenter) {
            yCenter = y;
            yTop = yCenterToTop(y);
        } else {
            yTop = y;
            yCenter = yTopToCenter(y);
        }
    }

    /* @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_FLOAT,
        defaultValue = "0.0") */
    /* @SimpleProperty(category = PropertyCategory.APPEARANCE) */
    public void Y(double y) {
        updateY(y);
        registerChange();
    }

    public double Y() {
        return originAtCenter ? yCenter : yTop;
    }

    /**
     * How the `%type%` should be layered relative to other {@link Ball}s and {@link ImageSprite}s,
     * with higher-numbered layers appearing in front of lower-numbered layers.
     *
     * @param layer higher numbers indicate that this sprite should appear
     *              in front of ones with lower numbers; if values are equal for
     *              sprites, either can go in front of the other
     */
    /* @SimpleProperty(category = PropertyCategory.APPEARANCE) */
  /* @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_FLOAT,
                    defaultValue = DEFAULT_Z + "") */
    public void Z(double layer) {
        this.zLayer = layer;
        canvas.changeSpriteLayer(this);  // Tell canvas about change
    }

    // Methods for event handling: general purpose method postEvent() and
    // Simple events: CollidedWith, Dragged, EdgeReached, Touched, NoLongerCollidingWith,
    // Flung, TouchUp, and TouchDown.

    /* @SimpleProperty(
        description = "How the %type% should be layered relative to other Balls and ImageSprites, " +
            "with higher-numbered layers in front of lower-numbered layers.") */
    public double Z() {
        return zLayer;
    }

    // TODO(halabelson): Fix collision detection for rotated sprites.

    // This gets overridden in Ball with the @SimpleProperty and @DesignerProperty
    // annotations so it can be made a property for Ball but not for ImageSprite.
    protected void OriginAtCenter(boolean b) {
        originAtCenter = b;
    }

    /**
     * Posts a dispatch for the specified event.  This guarantees that event
     * handlers run with serial semantics, e.g., appear atomic relative to
     * each other.
     * <p>
     * This method is overridden in tests.
     *
     * @param sprite    the instance on which the event takes place
     * @param eventName the name of the event
     * @param args      the arguments to the event handler
     */
    protected void postEvent(final Sprite sprite,
                             final String eventName,
                             final Object... args) {
        androidUIHandler.post(new Runnable() {
            public void run() {
                EventDispatcher.dispatchEvent(sprite, eventName, args);
            }
        });
    }

    /**
     * Event handler called when two enabled sprites ({@link Ball}s or {@link ImageSprite}s)
     * collide. Note that checking for collisions with a rotated `ImageSprite` currently
     * checks against its unrotated position. Therefore, collision
     * checking will be inaccurate for tall narrow or short wide sprites that are
     * rotated.
     *
     * @param other the other sprite in the collision
     */
    /* @SimpleEvent
     */
    public void CollidedWith(Sprite other) {
        if (!registeredCollisions.contains(other)) {
            registeredCollisions.add(other);
            postEvent(this, "CollidedWith", other);
        }
    }

    /**
     * Event handler for Dragged events.  On all calls, the starting coordinates
     * are where the screen was first touched, and the "current" coordinates
     * describe the endpoint of the current line segment.  On the first call
     * within a given drag, the "previous" coordinates are the same as the
     * starting coordinates; subsequently, they are the "current" coordinates
     * from the prior call. Note that the `%type%` won't actually move
     * anywhere in response to the Dragged event unless
     * {@link #MoveTo(double, double)} is specifically called.
     *
     * @param startX   the starting x-coordinate
     * @param startY   the starting y-coordinate
     * @param prevX    the previous x-coordinate (possibly equal to startX)
     * @param prevY    the previous y-coordinate (possibly equal to startY)
     * @param currentX the current x-coordinate
     * @param currentY the current y-coordinate
     */
  /* @SimpleEvent(
      description = "Event handler called when a %type% is dragged. " +
          "On all calls, the starting coordinates " +
          "are where the screen was first touched, and the \"current\" coordinates " +
          "describe the endpoint of the current line segment. On the first call " +
          "within a given drag, the \"previous\" coordinates are the same as the " +
          "starting coordinates; subsequently, they are the \"current\" coordinates " +
          "from the prior call. Note that the %type% won't actually move " +
          "anywhere in response to the Dragged event unless MoveTo is explicitly called. " +
          "For smooth movement, each of its coordinates should be set to the sum of its " +
          "initial value and the difference between its current and previous values.") */
    public void Dragged(float startX, float startY,
                        float prevX, float prevY,
                        float currentX, float currentY) {
        postEvent(this, "Dragged", startX, startY, prevX, prevY, currentX, currentY);
    }

    /**
     * Event handler called when the `%type%` reaches an `edge`{:.variable.block} of the screen.
     * If {@link #Bounce(int)} is then called with that edge, the sprite will appear to bounce off
     * of the edge it reached. Edge here is represented as an integer that indicates one of eight
     * directions north(1), northeast(2), east(3), southeast(4), south (-1), southwest(-2), west(-3),
     * and northwest(-4).
     */
  /* @SimpleEvent(
      description = "Event handler called when the %type% reaches an edge of the screen. " +
          "If Bounce is then called with that edge, the %type% will appear to " +
          "bounce off of the edge it reached. Edge here is represented as an integer that " +
          "indicates one of eight directions north (1), northeast (2), east (3), southeast (4), " +
          "south (-1), southwest (-2), west (-3), and northwest (-4).") */
    public void EdgeReached(/* @Options(Direction.class) */ int edge) {
        // Make sure that "edge" is a valid Direction.
        Direction dir = Direction.fromUnderlyingValue(edge);
        if (dir == null) {
            return;
        }
        EdgeReachedAbstract(dir);
    }

    /**
     * Called when the sprite hits an edge of the screen.
     */
    @SuppressWarnings("RegularMethodName")
    public void EdgeReachedAbstract(Direction edge) {
        // We have to post the edge as an int for backwards compatibility.
        postEvent(this, "EdgeReached", edge.toUnderlyingValue());
    }

    /**
     * Event indicating that a pair of sprites are no longer colliding.
     *
     * @param other the sprite formerly colliding with this sprite
     * @internaldoc This also registers the removal of the collision to a
     * private variable {@link #registeredCollisions} so that
     * {@link #CollidedWith(Sprite)} and this event are only raised once per
     * beginning and ending of a collision.
     */
  /* @SimpleEvent(
      description = "Event handler called when a pair of sprites (Balls and ImageSprites) are no " +
          "longer colliding.") */
    public void NoLongerCollidingWith(Sprite other) {
        registeredCollisions.remove(other);
        postEvent(this, "NoLongerCollidingWith", other);
    }

    /**
     * When the user touches the sprite and then immediately lifts finger: provides
     * the (x,y) position of the touch, relative to the upper left of the canvas.
     *
     * @param x x-coordinate of touched point
     * @param y y-coordinate of touched point
     */
  /* @SimpleEvent(
    description = "Event handler called when the user touches an enabled " +
        "%type% and then immediately lifts their finger. The provided x and y coordinates " +
        "are relative to the upper left of the canvas.") */
    public void Touched(float x, float y) {
        postEvent(this, "Touched", x, y);
    }

    /**
     * When a fling gesture (quick swipe) is made on the sprite: provides
     * the (x,y) position of the start of the fling, relative to the upper
     * left of the canvas. Also provides the speed (pixels per millisecond) and heading
     * (-180 to 180 degrees) of the fling, as well as the x velocity and y velocity
     * components of the fling's vector.
     *
     * @param x       x-coordinate of touched point
     * @param y       y-coordinate of touched point
     * @param speed   the speed of the fling sqrt(xspeed^2 + yspeed^2)
     * @param heading the heading of the fling
     * @param xvel    the speed in x-direction of the fling
     * @param yvel    the speed in y-direction of the fling
     */
  /* @SimpleEvent(
      description = "Event handler called when a fling gesture (quick swipe) is made on " +
          "an enabled %type%. This provides the x and y coordinates of the start of the " +
          "fling (relative to the upper left of the canvas), the speed (pixels per millisecond), " +
          "the heading (-180 to 180 degrees), and the x and y velocity components of " +
          "the fling's vector.") */
    public void Flung(float x, float y, float speed, float heading, float xvel, float yvel) {
        postEvent(this, "Flung", x, y, speed, heading, xvel, yvel);
    }

    // Methods providing Simple functions:
    // Bounce, CollidingWith, MoveIntoBounds, MoveTo, MoveToPoint, PointTowards.

    /**
     * When the user stops touching the sprite (lifts finger after a
     * TouchDown event): provides the (x,y) position of the touch, relative
     * to the upper left of the canvas.
     *
     * @param x x-coordinate of touched point
     * @param y y-coordinate of touched point
     */
  /* @SimpleEvent(
      description = "Event handler called when the user stops touching an enabled %type% " +
          "(lifting their finger after a TouchDown event). This provides the " +
          "x and y coordinates of the touch, relative to the upper left of the canvas.") */
    public void TouchUp(float x, float y) {
        postEvent(this, "TouchUp", x, y);
    }

    /**
     * When the user begins touching the sprite (places finger on sprite and
     * leaves it there): provides the (x,y) position of the touch, relative
     * to the upper left of the canvas
     *
     * @param x x-coordinate of touched point
     * @param y y-coordinate of touched point
     */
  /* @SimpleEvent(
      description = "Event handler called when the user begins touching an enabled %type% " +
          "(placing their finger on a %type% and leaving it there). This provides the " +
          "x and y coordinates of the touch, relative to the upper left of the canvas.") */
    public void TouchDown(float x, float y) {
        postEvent(this, "TouchDown", x, y);
    }

    // This is primarily used to enforce raising only
    // one {@link #CollidedWith(Sprite)} event per collision but is also
    // made available to the Simple programmer.

    /**
     * Makes this `%type%` bounce, as if off a wall. For normal bouncing, the `edge` argument should
     * be the one returned by {@link #EdgeReached}.
     *
     * @param edge the direction of the object (real or imaginary) to bounce off
     *             of; this should be one of the values of
     *             {@link com.google.appinventor.components.common.Direction}.
     */
  /* @SimpleFunction(
    description = "Makes the %type% bounce, as if off a wall. " +
        "For normal bouncing, the edge argument should be the one returned by EdgeReached.") */
    public void Bounce(/* @Options(Direction.class) */ int edge) {
        // Make sure that "edge" is a valid Direction.
        Direction dir = Direction.fromUnderlyingValue(edge);
        if (dir == null) {
            return;
        }
        BounceAbstract(dir);
    }

    /**
     * Makes the Sprite bounce off the wall defined by edge.
     *
     * @param edge The direction of the edge to bounce off of. For instance in this case:
     *             ----------
     *             |   \    |
     *             |    \   |
     *             |     \  |
     *             |      \ |
     *             |       *|
     *             |      / |
     *             ----------
     *             The correct direction would be `Direction.East`.
     */
    @SuppressWarnings("RegularMethodName")
    public void BounceAbstract(Direction edge) {
        MoveIntoBounds();

        // Normalize heading to [0, 360)
        double normalizedAngle = userHeading % 360;
        if (normalizedAngle < 0) {
            normalizedAngle += 360;
        }

        // Only transform heading if sprite was moving in that direction.
        // This avoids oscillations.
        if ((edge == Direction.East
                && (normalizedAngle < 90 || normalizedAngle > 270))
                || (edge == Direction.West
                && (normalizedAngle > 90 && normalizedAngle < 270))) {
            Heading(180 - normalizedAngle);
        } else if ((edge == Direction.North
                && normalizedAngle > 0 && normalizedAngle < 180)
                || (edge == Direction.South && normalizedAngle > 180)) {
            Heading(360 - normalizedAngle);
        } else if ((edge == Direction.Northeast
                && normalizedAngle > 0 && normalizedAngle < 90)
                || (edge == Direction.Northwest
                && normalizedAngle > 90 && normalizedAngle < 180)
                || (edge == Direction.Southwest
                && normalizedAngle > 180 && normalizedAngle < 270)
                || (edge == Direction.Southeast && normalizedAngle > 270)) {
            Heading(180 + normalizedAngle);
        }
    }

    // Description is different for Ball and ImageSprite so overridden and described in subclasses.

    /**
     * Indicates whether a collision has been registered between this `%type%`
     * and the passed `other` sprite.
     *
     * @param other the sprite to check for collision with this sprite
     * @return {@code true} if a collision event has been raised for the pair of
     * sprites and they still are in collision, {@code false} otherwise.
     */
  /* @SimpleFunction(
      description = "Indicates whether a collision has been registered between this %type% " +
          "and the passed sprite (Ball or ImageSprite).") */
    public boolean CollidingWith(Sprite other) {
        return registeredCollisions.contains(other);
    }

    /**
     * Moves the sprite back in bounds if part of it extends out of bounds,
     * having no effect otherwise. If the sprite is too wide to fit on the
     * canvas, this aligns the left side of the sprite with the left side of the
     * canvas. If the sprite is too tall to fit on the canvas, this aligns the
     * top side of the sprite with the top side of the canvas.
     */
  /* @SimpleFunction(
      description = "Moves the %type% back in bounds if part of it extends out of bounds, " +
          "having no effect otherwise. If the %type% is too wide to fit on the " +
          "canvas, this aligns the left side of the %type% with the left side of the " +
          "canvas. If the %type% is too tall to fit on the canvas, this aligns the " +
          "top side of the %type% with the top side of the canvas.") */
    public void MoveIntoBounds() {
        moveIntoBounds(canvas.Width(), canvas.Height());
    }

    /**
     * Moves the %type% so that its left top corner is at the specified x and y coordinates.
     *
     * @param x the x-coordinate
     * @param y the y-coordinate
     */
    public void MoveTo(double x, double y) {
        updateX(x);
        updateY(y);
        registerChange();
    }

    /**
     * Moves the %type% so that its origin is at the specified x and y coordinates.
     *
     * @param coordinates a list of length 2 where the first item is the x-coordinate and the
     *                    second item is the y-coordinate.
     */
  /* @SimpleFunction(
      description = "Moves the origin of %type% to the position of the cooordinates given "
          + " by the list formatted as [x-coordinate, y-coordinate].") */
    public void MoveToPoint(YailList coordinates) {
        MoveTo(coerceToDouble(coordinates.getObject(0)), coerceToDouble(coordinates.getObject(1)));
    }

    /**
     * Turns this `%type%` to point towards a given `target` sprite. The new heading will be parallel
     * to the line joining the centerpoints of the two sprites.
     *
     * @param target the other sprite to point towards
     */
  /* @SimpleFunction(
    description = "Turns the %type% to point towards a designated " +
        "target sprite (Ball or ImageSprite). The new heading will be parallel to the line joining " +
        "the centerpoints of the two sprites.") */
    public void PointTowards(Sprite target) {
        Heading(-Math.toDegrees(Math.atan2(target.yCenter - yCenter, target.xCenter - xCenter)));
    }

    // Internal methods supporting move-related functionality

    /**
     * Turns this `%type%` to point toward the point with the coordinates `(x, y)`.
     *
     * @param x parameter of the point to turn to
     * @param y parameter of the point to turn to
     */
  /* @SimpleFunction(
    description = "Sets the heading of the %type% toward the point " +
        "with the coordinates (x, y).") */
    public void PointInDirection(double x, double y) {
        Heading(-Math.toDegrees(Math.atan2(y - yCenter, x - xCenter)));
    }

    /**
     * Responds to a move or change of this sprite by redrawing the
     * enclosing Canvas and checking for any consequences that need
     * handling.  Specifically, this (1) notifies the Canvas of a change
     * so it can detect any collisions, etc., and (2) raises the
     * {@link #EdgeReached(int)} event if the Sprite has reached the edge of the
     * Canvas.
     */
    protected void registerChange() {
        // This was added to fix bug 2262218, where Ball.CollidedWith() was called
        // before all components had been constructed.
        if (!initialized) {
            // During REPL, components are not initalized, but we still want to repaint the canvas.
            canvas.getView().invalidate();
            return;
        }
        Direction edge = hitEdgeAbstract();
        if (edge != null) {
            EdgeReachedAbstract(edge);
        }
        canvas.registerChange(this);
    }

    /**
     * Specifies which edge of the canvas has been hit by the Sprite, if
     * any, moving the sprite back in bounds.
     *
     * @return {@link DIRECTION_NONE} if no edge has been hit, or the value of a
     * {@link Direction}  if that edge of the canvas has been hit
     */
    protected int hitEdge() {
        Direction edge = hitEdgeAbstract();
        if (edge == null) {
            return DIRECTION_NONE;
        }
        return edge.toUnderlyingValue();
    }

    /**
     * Specifies which edge of the canvas has been hit by the Sprite, if
     * any, moving the sprite back in bounds.
     *
     * @return {@link DIRECTION_NONE} if no edge has been hit, or the value of a
     * {@link Direction}  if that edge of the canvas has been hit
     */
    protected int hitEdge(int canvasWidth, int canvasHeight) {
        Direction edge = hitEdgeAbstract(canvasWidth, canvasHeight);
        if (edge == null) {
            return DIRECTION_NONE;
        }
        return edge.toUnderlyingValue();
    }

    /**
     * Specifies which edge of the canvas has been hit by the Sprite, if
     * any, moving the sprite back in bounds.
     *
     * @return {@link Direction} The direction associated with the edge that has
     * been hit.
     */
    protected Direction hitEdgeAbstract() {
        if (!canvas.ready()) {
            return null;
        }
        return hitEdgeAbstract(canvas.Width(), canvas.Height());
    }

    /**
     * Specifies which edge of the canvas has been hit by the Sprite, if
     * any, moving the sprite back in bounds.
     *
     * @return {@link Direction} The direction associated with the edge that has
     * been hit.
     */
    protected Direction hitEdgeAbstract(int canvasWidth, int canvasHeight) {
        // More than one boolean value can be true.
        boolean west = overWestEdge();
        boolean north = overNorthEdge();
        boolean east = overEastEdge(canvasWidth);
        boolean south = overSouthEdge(canvasHeight);

        if (!(north || south || east || west)) {
            return null;
        }

        MoveIntoBounds();

        if (west) {
            if (north) {
                return Direction.Northwest;
            } else if (south) {
                return Direction.Southwest;
            }
            return Direction.West;
        }

        if (east) {
            if (north) {
                return Direction.Northeast;
            } else if (south) {
                return Direction.Southeast;
            }
            return Direction.East;
        }

        if (north) {
            return Direction.North;
        }
        return Direction.South;
    }

    /**
     * Moves the sprite back in bounds if part of it extends out of bounds,
     * having no effect otherwise. If the sprite is too wide to fit on the
     * canvas, this aligns the left side of the sprite with the left side of the
     * canvas. If the sprite is too tall to fit on the canvas, this aligns the
     * top side of the sprite with the top side of the canvas.
     */
    protected final void moveIntoBounds(int canvasWidth, int canvasHeight) {
        boolean moved = false;

        // We set the xLeft and/or yTop fields directly, instead of calling X(123) and Y(123), to avoid
        // having multiple calls to registerChange.

        // Check if the sprite is too wide to fit on the canvas.
        if (Width() > canvasWidth) {
            // Sprite is too wide to fit. If it isn't already at the left edge, move it there.
            // It is important not to set moved to true if xLeft is already 0. Doing so can cause a stack
            // overflow.
            if (xLeft != 0) {
                xLeft = 0;
                xCenter = xLeftToCenter(xLeft);
                moved = true;
            }
        } else if (overWestEdge()) {
            xLeft = 0;
            xCenter = xLeftToCenter(xLeft);
            moved = true;
        } else if (overEastEdge(canvasWidth)) {
            xLeft = canvasWidth - Width();
            xCenter = xLeftToCenter(xLeft);
            moved = true;
        }

        // Check if the sprite is too tall to fit on the canvas. We don't want to cause a stack
        // overflow by moving the sprite to the top edge and then to the bottom edge, repeatedly.
        if (Height() > canvasHeight) {
            // Sprite is too tall to fit. If it isn't already at the top edge, move it there.
            // It is important not to set moved to true if yTop is already 0. Doing so can cause a stack
            // overflow.
            if (yTop != 0) {
                yTop = 0;
                yCenter = yTopToCenter(yTop);
                moved = true;
            }
        } else if (overNorthEdge()) {
            yTop = 0;
            yCenter = yTopToCenter(yTop);
            moved = true;
        } else if (overSouthEdge(canvasHeight)) {
            yTop = canvasHeight - Height();
            yCenter = yTopToCenter(yTop);
            moved = true;
        }

        // Then, call registerChange (just once!) if necessary.
        if (moved) {
            registerChange();
        }
    }

    // Methods for determining collisions with other Sprites and the edge
    // of the Canvas.

    /**
     * Updates the x- and y-coordinates based on the heading and speed.  The
     * caller is responsible for calling {@link #registerChange()}.
     */
    protected void updateCoordinates() {
        xLeft += speed * headingCos;
        xCenter = xLeftToCenter(xLeft);
        yTop += speed * headingSin;
        yCenter = yTopToCenter(yTop);
    }

    private final boolean overWestEdge() {
        return xLeft < 0;
    }

    private final boolean overEastEdge(int canvasWidth) {
        return xLeft + Width() > canvasWidth;
    }

    private final boolean overNorthEdge() {
        return yTop < 0;
    }

    private final boolean overSouthEdge(int canvasHeight) {
        return yTop + Height() > canvasHeight;
    }

    /**
     * Provides the bounding box for this sprite.  Modifying the returned value
     * does not affect the sprite.
     *
     * @param border the number of pixels outside the sprite to include in the
     *               bounding box
     * @return the bounding box for this sprite
     */
    public BoundingBox getBoundingBox(int border) {
        return new BoundingBox(xLeft - border, yTop - border,
                xLeft + Width() - 1 + border, yTop + Height() - 1 + border);
    }

    /**
     * Determines whether this sprite intersects with the given rectangle.
     *
     * @param rect the rectangle
     * @return {@code true} if they intersect, {@code false} otherwise
     */
    public boolean intersectsWith(BoundingBox rect) {
        // If the bounding boxes don't intersect, there can be no intersection.
        BoundingBox rect1 = getBoundingBox(0);
        if (!rect1.intersectDestructively(rect)) {
            return false;
        }

        // If we get here, rect1 has been mutated to hold the intersection of the
        // two bounding boxes.  Now check every point in the intersection to see if
        // the sprite contains it.
        for (double x = rect1.getLeft(); x < rect1.getRight(); x++) {
            for (double y = rect1.getTop(); y < rect1.getBottom(); y++) {
                if (containsPoint(x, y)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Indicates whether the specified point is contained by this sprite.
     * Subclasses of Sprite that are not rectangular should override this method.
     *
     * @param qx the x-coordinate
     * @param qy the y-coordinate
     * @return whether (qx, qy) falls within this sprite
     */
    public boolean containsPoint(double qx, double qy) {
        return qx >= xLeft && qx < xLeft + Width() &&
                qy >= yTop && qy < yTop + Height();
    }

    // Convenience methods for dealing with hitting the screen edge and collisions

    // AlarmHandler implementation

    /**
     * Moves and redraws sprite, registering changes.
     */
    public void alarm() {
        // This check on initialized is currently redundant, since registerChange()
        // checks it too.
        if (initialized && speed != 0) {
            updateCoordinates();
            registerChange();
        }
    }

    // Component implementation

    @Override
    public HandlesEventDispatching getDispatchDelegate() {
        return canvas.$form();
    }

    // OnDestroyListener implementation

    @Override
    public void onDestroy() {
        timerInternal.Enabled(false);
    }

    // Deleteable implementation

    @Override
    public void onDelete() {
        timerInternal.Enabled(false);
        canvas.removeSprite(this);
    }

    // Abstract methods that must be defined by subclasses

    /**
     * Draws the sprite on the given canvas
     *
     * @param canvas the canvas on which to draw
     */
    protected abstract void onDraw(android.graphics.Canvas canvas);
}
