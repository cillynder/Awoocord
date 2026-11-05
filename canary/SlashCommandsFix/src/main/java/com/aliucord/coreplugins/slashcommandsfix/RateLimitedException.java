/*
 * This file is part of Aliucord, an Android Discord client mod.
 * Copyright (c) 2021 Juby210 & Vendicated
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.coreplugins.slashcommandsfix;

import java.io.IOException;

/** Discord rate limited a command index request (or we're still waiting out a previous limit). */
final class RateLimitedException extends IOException {
    final long retryAfterMs;

    RateLimitedException(String endpoint, long retryAfterMs) {
        super("Rate limited on " + endpoint + ", retry in " + retryAfterMs + " ms");
        this.retryAfterMs = retryAfterMs;
    }
}
