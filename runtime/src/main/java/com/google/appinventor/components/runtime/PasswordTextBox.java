// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.components.runtime;

import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.common.PropertyTypeConstants;

/**
 * Users enter passwords in a password text box component, which hides the text that has been typed in it.
 * <p>
 * ![Example of a PasswordTextBox](images/passwordtextbox.png)
 * <p>
 * A password text box is the same as the ordinary {@link TextBox} component, except that it does
 * not display the characters typed by the user.
 * <p>
 * You can get or set the value of the text in the box with the {@link #Text(String)} property. If
 * {@link #Text()} is blank, you can use the {@link #Hint(String)} property to provide the user
 * with a suggestion of what to type. The {@link #Hint()} appears as faint text in the box.
 * <p>
 * Password text box components are usually used with a {@link Button} component. The user taps the
 * {@code Button} after entering text.
 */
public final class PasswordTextBox extends TextBoxBase {

    private boolean passwordVisible;

    // If true, then accept numeric keyboard input only
    private boolean acceptsNumbersOnly;

    /**
     * Creates a new PasswordTextBox component.
     *
     * @param container container, component will be placed in
     */
    public PasswordTextBox(ComponentContainer container) {
        super(container, new EditText(container.$context()));

        // make the box single line
        view.setSingleLine(true);
        // Add a transformation method to hide password text.   This must
        // be done after the SingleLine command
        view.setTransformationMethod(new PasswordTransformationMethod());

        // make sure the done action is Done and not Next.  See comment in Textbox.java
        view.setImeOptions(EditorInfo.IME_ACTION_DONE);

        PasswordVisible(false);
        NumbersOnly(false);

    }

    @SimpleProperty(description = "Visibility of password.")
    public void PasswordVisible(boolean visible) {
        passwordVisible = visible;
        setPasswordInputType(acceptsNumbersOnly, visible);
    }

    /**
     * Specifies whether the password is hidden (default) or shown.
     *
     * @return true if the password should be shown, otherwise false.
     */
    @SimpleProperty(description = "Visibility of password.")
    public boolean PasswordVisible() {
        return passwordVisible;
    }

    /**
     * NumbersOnly property getter method.
     *
     * @return {@code true} indicates that the password textbox accepts numbers only, {@code false} indicates
     * that it accepts any text
     */
    @SimpleProperty(
            description = "If true, then this password text box accepts only numbers as keyboard input.  " +
                    "Numbers can include a decimal point and an optional leading minus sign.  " +
                    "This applies to keyboard input only.  Even if NumbersOnly is true, you " +
                    "can use [set Text to] to enter any text at all.")
    public boolean NumbersOnly() {
        return acceptsNumbersOnly;
    }

    /**
     * If true, then this `%type%`` accepts only numbers as keyboard input. Numbers can include a
     * decimal point and an optional leading minus sign. This applies to keyboard input only. Even
     * if `NumbersOnly` is true, you can set the text to anything at all using the
     * {@link #Text(String)} property.
     *
     * @param acceptsNumbersOnly {@code true} restricts input to numeric,
     *                           {@code false} allows any text
     */
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "False")
    @SimpleProperty(
            description = "If true, then this password text box accepts only numbers as keyboard input.  " +
                    "Numbers can include a decimal point and an optional leading minus sign.  " +
                    "This applies to keyboard input only.  Even if NumbersOnly is true, you " +
                    "can use [set Text to] to enter any text at all.")
    public void NumbersOnly(boolean acceptsNumbersOnly) {
        this.acceptsNumbersOnly = acceptsNumbersOnly;
        setPasswordInputType(acceptsNumbersOnly, passwordVisible);
    }

    private void setPasswordInputType(boolean acceptsNumbersOnly, boolean passwordVisible) {
        if (passwordVisible && acceptsNumbersOnly) {
            view.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL);
        } else if (passwordVisible && !acceptsNumbersOnly) {
            view.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        } else if (acceptsNumbersOnly && !passwordVisible) {
            view.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        } else if (!acceptsNumbersOnly && !passwordVisible) {
            view.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
    }

}
