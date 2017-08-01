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

import java.util.logging.Logger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Christian Bauer
 */
public class StateMachineInvocationHandler implements InvocationHandler {

    private static Logger log = Logger.getLogger(StateMachineInvocationHandler.class.getName());

    public static final String METHOD_ON_ENTRY = "onEntry";
    public static final String METHOD_ON_EXIT = "onExit";

    final Class initialStateClass;
    final Map<Class, Object> stateObjects = new ConcurrentHashMap();
    Object currentState;

    StateMachineInvocationHandler(List<Class<?>> stateClasses,
                                  Class<?> initialStateClass,
                                  Class[] constructorArgumentTypes,
                                  Object[] constructorArguments) {

        log.fine("Creating state machine with initial state: " + initialStateClass);

        this.initialStateClass = initialStateClass;

        for (Class<?> stateClass : stateClasses) {
            try {

                Object state =
                        constructorArgumentTypes != null
                        ? stateClass
                                .getConstructor(constructorArgumentTypes)
                                .newInstance(constructorArguments)
                        : stateClass.newInstance();

                log.fine("Adding state instance: " + state.getClass().getName());
                stateObjects.put(stateClass, state);

            } catch (NoSuchMethodException ex) {
                throw new RuntimeException(
                        "State " + stateClass.getName() + " has the wrong constructor: " + ex, ex
                );
            } catch (Exception ex) {
                throw new RuntimeException(
                        "State " + stateClass.getName() + " can't be instantiated: " + ex, ex
                );
            }
        }

        if (!stateObjects.containsKey(initialStateClass)) {
            throw new RuntimeException("Initial state not in list of states: " + initialStateClass);
        }

        currentState = stateObjects.get(initialStateClass);
        synchronized (this) {
            invokeEntryMethod(currentState);
        }
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        synchronized(this) {

            if (StateMachine.METHOD_CURRENT_STATE.equals(method.getName())
                    && method.getParameterTypes().length == 0) {
                return currentState;
            }

            if (StateMachine.METHOD_FORCE_STATE.equals(method.getName())
                    && method.getParameterTypes().length == 1
                    && args.length == 1 && args[0] != null && args[0] instanceof Class) {
                Object forcedState = stateObjects.get((Class)args[0]);
                if (forcedState == null) {
                    throw new TransitionException("Can't force to invalid state: " + args[0]);
                }
                log.finer("Forcing state machine into state: " + forcedState.getClass().getName());
                invokeExitMethod(currentState);
                currentState = forcedState;
                invokeEntryMethod(forcedState);
                return null;
            }

            Method signalMethod = getMethodOfCurrentState(method);
            log.fine("Invoking signal method of current state: " + signalMethod.toString());
            Object methodReturn = signalMethod.invoke(currentState, args);

            if (methodReturn != null && methodReturn instanceof Class) {
                Class nextStateClass = (Class) methodReturn;
                if (stateObjects.containsKey(nextStateClass)) {
                    log.fine("Executing transition to next state: " + nextStateClass.getName());
                    invokeExitMethod(currentState);
                    currentState = stateObjects.get(nextStateClass);
                    invokeEntryMethod(currentState);
                }
            }
            return methodReturn;
        }
    }

    private Method getMethodOfCurrentState(Method method) {
        try {
            return currentState.getClass().getMethod(
                    method.getName(),
                    method.getParameterTypes()
            );
        } catch (NoSuchMethodException ex) {
            throw new TransitionException(
                    "State '" + currentState.getClass().getName() + "' doesn't support signal '" + method.getName() + "'"
            );
        }
    }

    private void invokeEntryMethod(Object state) {
        log.fine("Trying to invoke entry method of state: " + state.getClass().getName());
        try {
            Method onEntryMethod = state.getClass().getMethod(METHOD_ON_ENTRY);
            onEntryMethod.invoke(state);
        } catch (NoSuchMethodException ex) {
            log.finer("No entry method found on state: " + state.getClass().getName());
            // That's OK, just don't call it
        } catch (Exception ex) {
            throw new TransitionException(
                    "State '" + state.getClass().getName() + "' entry method threw exception: " + ex, ex
            );
        }
    }

    private void invokeExitMethod(Object state) {
        log.finer("Trying to invoking exit method of state: " + state.getClass().getName());
        try {
            Method onExitMethod = state.getClass().getMethod(METHOD_ON_EXIT);
            onExitMethod.invoke(state);
        } catch (NoSuchMethodException ex) {
            log.finer("No exit method found on state: " + state.getClass().getName());
            // That's OK, just don't call it
        } catch (Exception ex) {
            throw new TransitionException(
                    "State '" + state.getClass().getName() + "' exit method threw exception: " + ex, ex
            );
        }
    }

}
