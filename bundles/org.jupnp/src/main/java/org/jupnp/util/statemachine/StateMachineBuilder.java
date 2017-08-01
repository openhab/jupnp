 /*
 * Copyright (C) 2012 4th Line GmbH, Switzerland
 *
 * The contents of this file are subject to the terms of either the GNU
 * Lesser General Public License Version 2 or later ("LGPL") or the
 * Common Development and Distribution License Version 1 or later
 * ("CDDL") (collectively, the "License"). You may not use this file
 * except in compliance with the License. See LICENSE.txt for more
 * information.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package org.jupnp.util.statemachine;

import java.lang.reflect.Proxy;
import java.util.Arrays;

/**
 * @author Christian Bauer
 */
public class StateMachineBuilder {

    public static <T extends StateMachine> T build(Class<T> stateMachine, Class initialState) {
        return build(stateMachine, initialState, null, null);
    }

    public static <T extends StateMachine> T build(Class<T> stateMachine,
                                                   Class initialState,
                                                   Class[] constructorArgumentTypes,
                                                   Object[] constructorArguments) {
        return (T)
                Proxy.newProxyInstance(
                        stateMachine.getClassLoader(),
                        new Class[]{stateMachine},
                        new StateMachineInvocationHandler(
                                Arrays.asList(stateMachine.getAnnotation(States.class).value()),
                                initialState,
                                constructorArgumentTypes,
                                constructorArguments
                        )
                );
    }

}