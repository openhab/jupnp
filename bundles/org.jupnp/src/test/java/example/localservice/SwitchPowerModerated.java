/**
 * Copyright (C) 2014 4th Line GmbH, Switzerland and others
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License Version 1 or later
 * ("CDDL") (collectively, the "License"). You may not use this file
 * except in compliance with the License. See LICENSE.txt for more
 * information.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package example.localservice;

import org.jupnp.binding.annotations.UpnpStateVariable;

import example.binarylight.SwitchPower;

public class SwitchPowerModerated extends SwitchPower {

    @UpnpStateVariable(eventMaximumRateMilliseconds = 500)
    public String moderatedMaxRateVar = "one";

    @UpnpStateVariable(eventMinimumDelta = 3)
    public Integer moderatedMinDeltaVar = 1;

}
