/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package testament.powerassert;

/**
 * <p>
 * This is internal class for enhanced error.
 * </p>
 * 
 * @version 2012/01/24 13:14:52
 */
@SuppressWarnings("serial")
public class PowerAssertionError extends AssertionError {

    /** The related variables holder. */
    final PowerAssertContext context;

    /**
     * <p>
     * This is internal constructor. Don't use it.
     * </p>
     */
    public PowerAssertionError(PowerAssertContext context) {
        super("");

        this.context = context;
    }

    /**
     * <p>
     * This is internal constructor. Don't use it.
     * </p>
     */
    public PowerAssertionError(boolean detailMessage, PowerAssertContext context) {
        super(detailMessage);

        this.context = context;
    }

    /**
     * <p>
     * This is internal constructor. Don't use it.
     * </p>
     */
    public PowerAssertionError(char detailMessage, PowerAssertContext context) {
        super(detailMessage);

        this.context = context;
    }

    /**
     * <p>
     * This is internal constructor. Don't use it.
     * </p>
     */
    public PowerAssertionError(double detailMessage, PowerAssertContext context) {
        super(detailMessage);

        this.context = context;
    }

    /**
     * <p>
     * This is internal constructor. Don't use it.
     * </p>
     */
    public PowerAssertionError(float detailMessage, PowerAssertContext context) {
        super(detailMessage);

        this.context = context;
    }

    /**
     * <p>
     * This is internal constructor. Don't use it.
     * </p>
     */
    public PowerAssertionError(int detailMessage, PowerAssertContext context) {
        super(detailMessage);

        this.context = context;
    }

    /**
     * <p>
     * This is internal constructor. Don't use it.
     * </p>
     */
    public PowerAssertionError(long detailMessage, PowerAssertContext context) {
        super(detailMessage);

        this.context = context;
    }

    /**
     * <p>
     * This is internal constructor. Don't use it.
     * </p>
     */
    public PowerAssertionError(Object detailMessage, PowerAssertContext context) {
        super(detailMessage);

        this.context = context;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMessage() {
        return super.getMessage() + "\n" + context;
    }
}
