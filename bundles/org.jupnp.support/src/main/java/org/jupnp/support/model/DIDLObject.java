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
package org.jupnp.support.model;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.w3c.dom.Element;

/**
 * @author Christian Bauer
 * @author Mario Franco
 * @author Amit Kumar Mondal - Code Refactoring
 */
public abstract class DIDLObject {

    public abstract static class Property<V> {

        public interface NAMESPACE {
        }

        private V value;
        private final String descriptorName;
        private final List<Property<DIDLAttribute>> attributes = new ArrayList<>();

        protected Property() {
            this(null, null);
        }

        protected Property(String descriptorName) {
            this(null, descriptorName);
        }

        protected Property(V value, String descriptorName) {
            this.value = value;
            // TODO Not sure this is a good fix for https://github.com/4thline/cling/issues/62
            this.descriptorName = descriptorName == null
                    ? getClass().getSimpleName().toLowerCase(Locale.ROOT).replace("didlobject$property$upnp$", "")
                    : descriptorName;
        }

        protected Property(V value, String descriptorName, List<Property<DIDLAttribute>> attributes) {
            this.value = value;
            // TODO Not sure this is a good fix for https://github.com/4thline/cling/issues/62
            this.descriptorName = descriptorName == null
                    ? getClass().getSimpleName().toLowerCase(Locale.ROOT).replace("didlobject$property$upnp$", "")
                    : descriptorName;
            this.attributes.addAll(attributes);
        }

        public V getValue() {
            return value;
        }

        public void setValue(V value) {
            this.value = value;
        }

        public String getDescriptorName() {
            return descriptorName;
        }

        public void setOnElement(Element element) {
            element.setTextContent(toString());
            for (Property<DIDLAttribute> attr : attributes) {
                element.setAttributeNS(attr.getValue().getNamespaceURI(),
                        attr.getValue().getPrefix() + ':' + attr.getDescriptorName(), attr.getValue().getValue());
            }
        }

        public void addAttribute(Property<DIDLAttribute> attr) {
            this.attributes.add(attr);
        }

        public void removeAttribute(Property<DIDLAttribute> attr) {
            this.attributes.remove(attr);
        }

        public void removeAttribute(String descriptorName) {
            for (Property<DIDLAttribute> attr : attributes) {
                if (attr.getDescriptorName().equals(descriptorName)) {
                    this.removeAttribute(attr);
                    break;
                }
            }
        }

        public Property<DIDLAttribute> getAttribute(String descriptorName) {
            for (Property<DIDLAttribute> attr : attributes) {
                if (attr.getDescriptorName().equals(descriptorName)) {
                    return attr;
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return getValue() != null ? getValue().toString() : "";
        }

        public static class PropertyPersonWithRole extends Property<PersonWithRole> {

            public PropertyPersonWithRole() {
            }

            public PropertyPersonWithRole(String descriptorName) {
                super(descriptorName);
            }

            public PropertyPersonWithRole(PersonWithRole value, String descriptorName) {
                super(value, descriptorName);
            }

            @Override
            public void setOnElement(Element element) {
                if (getValue() != null) {
                    getValue().setOnElement(element);
                }
            }
        }

        public static class DC {

            public interface NAMESPACE extends Property.NAMESPACE {
                String URI = "http://purl.org/dc/elements/1.1/";
            }

            public static class DESCRIPTION extends Property<String> implements NAMESPACE {
                public DESCRIPTION() {
                }

                public DESCRIPTION(String value) {
                    super(value, null);
                }
            }

            public static class PUBLISHER extends Property<Person> implements NAMESPACE {
                public PUBLISHER() {
                }

                public PUBLISHER(Person value) {
                    super(value, null);
                }
            }

            public static class CONTRIBUTOR extends Property<Person> implements NAMESPACE {
                public CONTRIBUTOR() {
                }

                public CONTRIBUTOR(Person value) {
                    super(value, null);
                }
            }

            public static class DATE extends Property<String> implements NAMESPACE {
                public DATE() {
                }

                public DATE(String value) {
                    super(value, null);
                }
            }

            public static class LANGUAGE extends Property<String> implements NAMESPACE {
                public LANGUAGE() {
                }

                public LANGUAGE(String value) {
                    super(value, null);
                }
            }

            public static class RELATION extends Property<URI> implements NAMESPACE {
                public RELATION() {
                }

                public RELATION(URI value) {
                    super(value, null);
                }
            }

            public static class RIGHTS extends Property<String> implements NAMESPACE {
                public RIGHTS() {
                }

                public RIGHTS(String value) {
                    super(value, null);
                }
            }
        }

        public abstract static class SEC {

            public interface NAMESPACE extends Property.NAMESPACE {
                String URI = "http://www.sec.co.kr/";
            }

            public static class CAPTIONINFOEX extends Property<URI> implements NAMESPACE {
                public CAPTIONINFOEX() {
                    this(null);
                }

                public CAPTIONINFOEX(URI value) {
                    super(value, "CaptionInfoEx");
                }

                public CAPTIONINFOEX(URI value, List<Property<DIDLAttribute>> attributes) {
                    super(value, "CaptionInfoEx", attributes);
                }
            }

            public static class CAPTIONINFO extends Property<URI> implements NAMESPACE {
                public CAPTIONINFO() {
                    this(null);
                }

                public CAPTIONINFO(URI value) {
                    super(value, "CaptionInfo");
                }

                public CAPTIONINFO(URI value, List<Property<DIDLAttribute>> attributes) {
                    super(value, "CaptionInfo", attributes);
                }
            }

            public static class TYPE extends Property<DIDLAttribute> implements NAMESPACE {
                public TYPE() {
                    this(null);
                }

                public TYPE(DIDLAttribute value) {
                    super(value, "type");
                }
            }
        }

        public abstract static class UPNP {

            public interface NAMESPACE extends Property.NAMESPACE {
                String URI = "urn:schemas-upnp-org:metadata-1-0/upnp/";
            }

            public static class ARTIST extends PropertyPersonWithRole implements NAMESPACE {
                public ARTIST() {
                }

                public ARTIST(PersonWithRole value) {
                    super(value, null);
                }
            }

            public static class ACTOR extends PropertyPersonWithRole implements NAMESPACE {
                public ACTOR() {
                }

                public ACTOR(PersonWithRole value) {
                    super(value, null);
                }
            }

            public static class AUTHOR extends PropertyPersonWithRole implements NAMESPACE {
                public AUTHOR() {
                }

                public AUTHOR(PersonWithRole value) {
                    super(value, null);
                }
            }

            public static class PRODUCER extends Property<Person> implements NAMESPACE {
                public PRODUCER() {
                }

                public PRODUCER(Person value) {
                    super(value, null);
                }
            }

            public static class DIRECTOR extends Property<Person> implements NAMESPACE {
                public DIRECTOR() {
                }

                public DIRECTOR(Person value) {
                    super(value, null);
                }
            }

            public static class GENRE extends Property<String> implements NAMESPACE {
                public GENRE() {
                }

                public GENRE(String value) {
                    super(value, null);
                }
            }

            public static class ALBUM extends Property<String> implements NAMESPACE {
                public ALBUM() {
                }

                public ALBUM(String value) {
                    super(value, null);
                }
            }

            public static class PLAYLIST extends Property<String> implements NAMESPACE {
                public PLAYLIST() {
                }

                public PLAYLIST(String value) {
                    super(value, null);
                }
            }

            public static class REGION extends Property<String> implements NAMESPACE {
                public REGION() {
                }

                public REGION(String value) {
                    super(value, null);
                }
            }

            public static class RATING extends Property<String> implements NAMESPACE {
                public RATING() {
                }

                public RATING(String value) {
                    super(value, null);
                }
            }

            public static class TOC extends Property<String> implements NAMESPACE {
                public TOC() {
                }

                public TOC(String value) {
                    super(value, null);
                }
            }

            public static class ALBUM_ART_URI extends Property<URI> implements NAMESPACE {
                public ALBUM_ART_URI() {
                    this(null);
                }

                public ALBUM_ART_URI(URI value) {
                    super(value, "albumArtURI");
                }

                public ALBUM_ART_URI(URI value, List<Property<DIDLAttribute>> attributes) {
                    super(value, "albumArtURI", attributes);
                }
            }

            public static class ARTIST_DISCO_URI extends Property<URI> implements NAMESPACE {
                public ARTIST_DISCO_URI() {
                    this(null);
                }

                public ARTIST_DISCO_URI(URI value) {
                    super(value, "artistDiscographyURI");
                }
            }

            public static class LYRICS_URI extends Property<URI> implements NAMESPACE {
                public LYRICS_URI() {
                    this(null);
                }

                public LYRICS_URI(URI value) {
                    super(value, "lyricsURI");
                }
            }

            public static class STORAGE_TOTAL extends Property<Long> implements NAMESPACE {
                public STORAGE_TOTAL() {
                    this(null);
                }

                public STORAGE_TOTAL(Long value) {
                    super(value, "storageTotal");
                }
            }

            public static class STORAGE_USED extends Property<Long> implements NAMESPACE {
                public STORAGE_USED() {
                    this(null);
                }

                public STORAGE_USED(Long value) {
                    super(value, "storageUsed");
                }
            }

            public static class STORAGE_FREE extends Property<Long> implements NAMESPACE {
                public STORAGE_FREE() {
                    this(null);
                }

                public STORAGE_FREE(Long value) {
                    super(value, "storageFree");
                }
            }

            public static class STORAGE_MAX_PARTITION extends Property<Long> implements NAMESPACE {
                public STORAGE_MAX_PARTITION() {
                    this(null);
                }

                public STORAGE_MAX_PARTITION(Long value) {
                    super(value, "storageMaxPartition");
                }
            }

            public static class STORAGE_MEDIUM extends Property<StorageMedium> implements NAMESPACE {
                public STORAGE_MEDIUM() {
                    this(null);
                }

                public STORAGE_MEDIUM(StorageMedium value) {
                    super(value, "storageMedium");
                }
            }

            public static class LONG_DESCRIPTION extends Property<String> implements NAMESPACE {
                public LONG_DESCRIPTION() {
                    this(null);
                }

                public LONG_DESCRIPTION(String value) {
                    super(value, "longDescription");
                }
            }

            public static class ICON extends Property<URI> implements NAMESPACE {
                public ICON() {
                    this(null);
                }

                public ICON(URI value) {
                    super(value, "icon");
                }
            }

            public static class RADIO_CALL_SIGN extends Property<String> implements NAMESPACE {
                public RADIO_CALL_SIGN() {
                    this(null);
                }

                public RADIO_CALL_SIGN(String value) {
                    super(value, "radioCallSign");
                }
            }

            public static class RADIO_STATION_ID extends Property<String> implements NAMESPACE {
                public RADIO_STATION_ID() {
                    this(null);
                }

                public RADIO_STATION_ID(String value) {
                    super(value, "radioStationID");
                }
            }

            public static class RADIO_BAND extends Property<String> implements NAMESPACE {
                public RADIO_BAND() {
                    this(null);
                }

                public RADIO_BAND(String value) {
                    super(value, "radioBand");
                }
            }

            public static class CHANNEL_NR extends Property<Integer> implements NAMESPACE {
                public CHANNEL_NR() {
                    this(null);
                }

                public CHANNEL_NR(Integer value) {
                    super(value, "channelNr");
                }
            }

            public static class CHANNEL_NAME extends Property<String> implements NAMESPACE {
                public CHANNEL_NAME() {
                    this(null);
                }

                public CHANNEL_NAME(String value) {
                    super(value, "channelName");
                }
            }

            public static class SCHEDULED_START_TIME extends Property<String> implements NAMESPACE {
                public SCHEDULED_START_TIME() {
                    this(null);
                }

                public SCHEDULED_START_TIME(String value) {
                    super(value, "scheduledStartTime");
                }
            }

            public static class SCHEDULED_END_TIME extends Property<String> implements NAMESPACE {
                public SCHEDULED_END_TIME() {
                    this(null);
                }

                public SCHEDULED_END_TIME(String value) {
                    super(value, "scheduledEndTime");
                }
            }

            public static class DVD_REGION_CODE extends Property<Integer> implements NAMESPACE {
                public DVD_REGION_CODE() {
                    this(null);
                }

                public DVD_REGION_CODE(Integer value) {
                    super(value, "DVDRegionCode");
                }
            }

            public static class ORIGINAL_TRACK_NUMBER extends Property<Integer> implements NAMESPACE {
                public ORIGINAL_TRACK_NUMBER() {
                    this(null);
                }

                public ORIGINAL_TRACK_NUMBER(Integer value) {
                    super(value, "originalTrackNumber");
                }
            }

            public static class USER_ANNOTATION extends Property<String> implements NAMESPACE {
                public USER_ANNOTATION() {
                    this(null);
                }

                public USER_ANNOTATION(String value) {
                    super(value, "userAnnotation");
                }
            }
        }

        public abstract static class DLNA {

            public interface NAMESPACE extends Property.NAMESPACE {
                String URI = "urn:schemas-dlna-org:metadata-1-0/";
            }

            public static class PROFILE_ID extends Property<DIDLAttribute> implements NAMESPACE {
                public PROFILE_ID() {
                    this(null);
                }

                public PROFILE_ID(DIDLAttribute value) {
                    super(value, "profileID");
                }
            }
        }
    }

    public static class Class {

        protected String value;
        protected String friendlyName;
        protected boolean includeDerived;

        public Class() {
        }

        public Class(String value) {
            this.value = value;
        }

        public Class(String value, String friendlyName) {
            this.value = value;
            this.friendlyName = friendlyName;
        }

        public Class(String value, String friendlyName, boolean includeDerived) {
            this.value = value;
            this.friendlyName = friendlyName;
            this.includeDerived = includeDerived;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getFriendlyName() {
            return friendlyName;
        }

        public void setFriendlyName(String friendlyName) {
            this.friendlyName = friendlyName;
        }

        public boolean isIncludeDerived() {
            return includeDerived;
        }

        public void setIncludeDerived(boolean includeDerived) {
            this.includeDerived = includeDerived;
        }

        public boolean equals(DIDLObject instance) {
            return getValue().equals(instance.getClazz().getValue());
        }
    }

    protected String id;
    protected String parentID;

    protected String title; // DC
    protected String creator; // DC

    protected boolean restricted = true; // Let's just assume read-only is default
    protected WriteStatus writeStatus; // UPNP
    protected Class clazz; // UPNP

    protected List<Res> resources = new ArrayList<>();
    protected List<Property<?>> properties = new ArrayList<>();

    protected List<DescMeta<?>> descMetadata = new ArrayList<>();

    protected DIDLObject() {
    }

    protected DIDLObject(DIDLObject other) {
        this(other.getId(), other.getParentID(), other.getTitle(), other.getCreator(), other.isRestricted(),
                other.getWriteStatus(), other.getClazz(), other.getResources(), other.getProperties(),
                other.getDescMetadata());
    }

    protected DIDLObject(String id, String parentID, String title, String creator, boolean restricted,
            WriteStatus writeStatus, Class clazz, List<Res> resources, List<Property<?>> properties,
            List<DescMeta<?>> descMetadata) {
        this.id = id;
        this.parentID = parentID;
        this.title = title;
        this.creator = creator;
        this.restricted = restricted;
        this.writeStatus = writeStatus;
        this.clazz = clazz;
        this.resources = resources;
        this.properties = properties;
        this.descMetadata = descMetadata;
    }

    public String getId() {
        return id;
    }

    public DIDLObject setId(String id) {
        this.id = id;
        return this;
    }

    public String getParentID() {
        return parentID;
    }

    public DIDLObject setParentID(String parentID) {
        this.parentID = parentID;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public DIDLObject setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getCreator() {
        return creator;
    }

    public DIDLObject setCreator(String creator) {
        this.creator = creator;
        return this;
    }

    public boolean isRestricted() {
        return restricted;
    }

    public DIDLObject setRestricted(boolean restricted) {
        this.restricted = restricted;
        return this;
    }

    public WriteStatus getWriteStatus() {
        return writeStatus;
    }

    public DIDLObject setWriteStatus(WriteStatus writeStatus) {
        this.writeStatus = writeStatus;
        return this;
    }

    public Res getFirstResource() {
        return !getResources().isEmpty() ? getResources().get(0) : null;
    }

    public List<Res> getResources() {
        return resources;
    }

    public DIDLObject setResources(List<Res> resources) {
        this.resources = resources;
        return this;
    }

    public DIDLObject addResource(Res resource) {
        getResources().add(resource);
        return this;
    }

    public Class getClazz() {
        return clazz;
    }

    public DIDLObject setClazz(Class clazz) {
        this.clazz = clazz;
        return this;
    }

    public List<Property<?>> getProperties() {
        return properties;
    }

    public DIDLObject setProperties(List<Property<?>> properties) {
        this.properties = properties;
        return this;
    }

    public DIDLObject addProperty(Property property) {
        if (property == null) {
            return this;
        }
        getProperties().add(property);
        return this;
    }

    public DIDLObject replaceFirstProperty(Property<?> property) {
        if (property == null) {
            return this;
        }
        getProperties().removeIf(p -> p.getClass().isAssignableFrom(property.getClass()));
        addProperty(property);
        return this;
    }

    public DIDLObject replaceProperties(java.lang.Class<? extends Property<?>> propertyClass,
            Property<?>[] properties) {
        if (properties.length == 0) {
            return this;
        }
        removeProperties(propertyClass);
        return addProperties(properties);
    }

    public DIDLObject addProperties(Property<?>[] properties) {
        if (properties == null) {
            return this;
        }
        for (Property<?> property : properties) {
            addProperty(property);
        }
        return this;
    }

    public DIDLObject removeProperties(java.lang.Class<? extends Property<?>> propertyClass) {
        getProperties().removeIf(propertyClass::isInstance);
        return this;
    }

    public boolean hasProperty(java.lang.Class<? extends Property<?>> propertyClass) {
        for (Property<?> property : getProperties()) {
            if (propertyClass.isInstance(property)) {
                return true;
            }
        }
        return false;
    }

    public <V> Property<V> getFirstProperty(java.lang.Class<? extends Property<V>> propertyClass) {
        for (Property property : getProperties()) {
            if (propertyClass.isInstance(property)) {
                return property;
            }
        }
        return null;
    }

    public <V> Property<V> getLastProperty(java.lang.Class<? extends Property<V>> propertyClass) {
        Property found = null;
        for (Property property : getProperties()) {
            if (propertyClass.isInstance(property)) {
                found = property;
            }
        }
        return found;
    }

    public <V> Property<V>[] getProperties(java.lang.Class<? extends Property<V>> propertyClass) {
        List<Property<V>> list = new ArrayList<>();
        for (Property property : getProperties()) {
            if (propertyClass.isInstance(property)) {
                list.add(property);
            }
        }
        return list.toArray(new Property[list.size()]);
    }

    public <V> Property<V>[] getPropertiesByNamespace(java.lang.Class<? extends Property.NAMESPACE> namespace) {
        List<Property<V>> list = new ArrayList<>();
        for (Property property : getProperties()) {
            if (namespace.isInstance(property)) {
                list.add(property);
            }
        }
        return list.toArray(new Property[list.size()]);
    }

    public <V> V getFirstPropertyValue(java.lang.Class<? extends Property<V>> propertyClass) {
        Property<V> prop = getFirstProperty(propertyClass);
        return prop == null ? null : prop.getValue();
    }

    public <V> List<V> getPropertyValues(java.lang.Class<? extends Property<V>> propertyClass) {
        List<V> list = new ArrayList<>();
        for (Property property : getProperties(propertyClass)) {
            list.add((V) property.getValue());
        }
        return list;
    }

    public List<DescMeta<?>> getDescMetadata() {
        return descMetadata;
    }

    public void setDescMetadata(List<DescMeta<?>> descMetadata) {
        this.descMetadata = descMetadata;
    }

    public DIDLObject addDescMetadata(DescMeta<?> descMetadata) {
        getDescMetadata().add(descMetadata);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DIDLObject that = (DIDLObject) o;

        if (!id.equals(that.id)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
