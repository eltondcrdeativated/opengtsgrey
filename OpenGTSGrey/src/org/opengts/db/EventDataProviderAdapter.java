// ----------------------------------------------------------------------------
// Copyright 2006-2009, GeoTelematic Solutions, Inc.
// All rights reserved
// ----------------------------------------------------------------------------
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// 
// http://www.apache.org/licenses/LICENSE-2.0
// 
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// ----------------------------------------------------------------------------
// Change History:
//  2009/08/07  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.lang.*;
import java.util.*;

import org.opengts.util.*;

public class EventDataProviderAdapter
    implements EventDataProvider
{

    public EventDataProviderAdapter()
    {
    }

    // ------------------------------------------------------------------------

    public String getAccountID()
    {
        return null;
    }

    public String getDeviceID()
    {
        return null;
    }

    public String getDeviceDescription() {
        return ""; 
    }

    public String getDeviceVIN() {
        return ""; 
    }

    public long getTimestamp()
    {
        return 0L;
    }

    public int getStatusCode()
    {
        return StatusCodes.STATUS_NONE;
    }
    
    public String getStatusCodeDescription(BasicPrivateLabel bpl)
    {
        return "";
    }
    
    public int getPushpinIconIndex(String iconSelector, OrderedSet<String> iconKeys,
        boolean isFleet, BasicPrivateLabel bpl)
    {
        return 0; // black
    }

    public double getLatitude()
    {
        return 0.0;
    }
    
    public double getLongitude()
    {
        return 0.0;
    }

    public int    getSatelliteCount()
    {
        return 0;
    }

    public double getBatteryLevel()
    {
        return 0.0;
    }

    public double getSpeedKPH()
    {
        return 0.0;
    }
    
    public double getHeading()
    {
        return 0.0;
    }

    public double getAltitude()
    {
        return 0.0;
    }

    public String getGeozoneID()
    {
        return "";
    }

    public String getAddress()
    {
        return "";
    }

    public long   getInputMask()
    {
        return 0L;
    }

    public double getOdometerKM()
    {
        return 0.0;
    }

    /* icon selector properties */
    public void setIsLastEvent(boolean isLast)
    {
        //
    }

    public boolean getIsLastEvent()
    {
        return false;
    }

}
