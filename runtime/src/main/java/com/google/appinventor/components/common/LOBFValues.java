// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2023 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.components.common;

import java.util.HashMap;
import java.util.Map;

/**
 * Defines a parameter of LOBF formula used by the regression components.
 */
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public enum LOBFValues implements OptionList<String> {
    CorrCoef("correlation coefficient"),
    @Default
    Slope("slope"),
    Yintercept("Yintercept"),
    Predictions("predictions"),
    AllValues("all values");

    private static final Map<String, LOBFValues> lookup = new HashMap<>();

    static {
        for (LOBFValues value : LOBFValues.values()) {
            lookup.put(value.toUnderlyingValue(), value);
        }
    }

    private final String lobfValues;

    LOBFValues(String lobfV) {
        this.lobfValues = lobfV;
    }

    public static LOBFValues fromUnderlyingValue(String value) {
        return lookup.get(value);
    }

    public String toUnderlyingValue() {
        return lobfValues;
    }
}
