package com.ursulagis.desktop.gui.nww.replacementLayers;

/*
 * Copyright (C) 2019 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */


import gov.nasa.worldwind.util.*;

import java.net.*;

/**
 * @author Sufaev
 */
public abstract class MercatorTileUrlBuilder implements TileUrlBuilder
{

    private int firstLevelOffset;

    public MercatorTileUrlBuilder setFirstLevelOffset(int firstLevelOffset) {
        this.firstLevelOffset = firstLevelOffset;
        return this;
    }

    public int getFirstLevelOffset() {
        return firstLevelOffset;
    }

    @Override
    public URL getURL(Tile tile, String imageFormat) throws MalformedURLException
    {
    	//tile.getSector().getCentroid()
        return getMercatorURL(tile.getColumn(), (1 << (tile.getLevelNumber() + firstLevelOffset)) - 1 - tile.getRow(), tile.getLevelNumber() + firstLevelOffset);
    }

    protected abstract URL getMercatorURL(int x, int y, int z) throws MalformedURLException;

}