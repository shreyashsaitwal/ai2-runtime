// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2016 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.components.runtime;

import com.google.appinventor.components.common.ComponentConstants;

/**
 * A formatting element in which to place components that should be displayed one below another.
 * (The first child component is stored on top, the second beneath it, etc.) If you wish to have
 * components displayed next to one another, use {@link HorizontalScrollArrangement} instead.
 * <p>
 * This version is scrollable.
 *
 * @author sharon@google.com (Sharon Perl)
 * @author jis@mit.edu (Jeffrey I. Schiller)
 */

public class VerticalScrollArrangement extends HVArrangement {

    public VerticalScrollArrangement(ComponentContainer container) {
        super(container, ComponentConstants.LAYOUT_ORIENTATION_VERTICAL,
                ComponentConstants.SCROLLABLE_ARRANGEMENT);
    }

}
