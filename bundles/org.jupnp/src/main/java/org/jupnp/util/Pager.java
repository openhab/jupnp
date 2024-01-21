/*
 * Copyright (C) 2011-2024 4th Line GmbH, Switzerland and others
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
 *
 * SPDX-License-Identifier: CDDL-1.0
 */
package org.jupnp.util;

import java.io.Serializable;

/**
 * @author Christian Bauer
 */
public class Pager implements Serializable {

    private Long numOfRecords = 0l;
    private Integer page = 1;
    private Long pageSize = 15l;

    public Pager() {
    }

    public Pager(Long numOfRecords) {
        this.numOfRecords = numOfRecords;
    }

    public Pager(Long numOfRecords, Integer page) {
        this.numOfRecords = numOfRecords;
        this.page = page;
    }

    public Pager(Long numOfRecords, Integer page, Long pageSize) {
        this.numOfRecords = numOfRecords;
        this.page = page;
        this.pageSize = pageSize;
    }

    public Long getNumOfRecords() {
        return numOfRecords;
    }

    public void setNumOfRecords(Long numOfRecords) {
        this.numOfRecords = numOfRecords;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        if (page != null)
            this.page = page;
    }

    public Long getPageSize() {
        return pageSize;
    }

    public void setPageSize(Long pageSize) {
        if (pageSize != null)
            this.pageSize = pageSize;
    }

    public int getNextPage() {
        return page + 1;
    }

    public int getPreviousPage() {
        return page - 1;
    }

    public int getFirstPage() {
        return 1;
    }

    public long getIndexRangeBegin() {
        long retval = (getPage() - 1) * getPageSize();
        return Math.max(Math.min(getNumOfRecords() - 1, retval >= 0 ? retval : 0), 0);
    }

    public long getIndexRangeEnd() {
        long firstIndex = getIndexRangeBegin();
        long pageIndex = getPageSize() - 1;
        long lastIndex = getNumOfRecords() - 1;
        return Math.min(firstIndex + pageIndex, lastIndex);
    }

    public long getLastPage() {
        long lastPage = (numOfRecords / pageSize);
        if (numOfRecords % pageSize == 0)
            lastPage--;
        return lastPage + 1;
    }

    public boolean isPreviousPageAvailable() {
        return getIndexRangeBegin() + 1 > getPageSize();
    }

    public boolean isNextPageAvailable() {
        return numOfRecords - 1 > getIndexRangeEnd();
    }

    public boolean isSeveralPages() {
        return getNumOfRecords() != 0 && getNumOfRecords() > getPageSize();
    }

    public String toString() {
        return "Pager - Records: " + getNumOfRecords() + " Page size: " + getPageSize();
    }
}
