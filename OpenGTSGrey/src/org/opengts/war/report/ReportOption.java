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
//  2007/03/30  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.war.report;

import java.util.*;
import java.io.*;

import org.opengts.util.*;

public class ReportOption
{

    // ------------------------------------------------------------------------

    private String       optName  = null;
    private I18N.Text    optDesc  = null;
    private RTProperties optProps = null;

    // ------------------------------------------------------------------------
    
    public ReportOption(String name)
    {
        this.optName  = StringTools.trim(name);
    }
    
    // ------------------------------------------------------------------------

    public String getName()
    {
        return this.optName;
    }

    // ------------------------------------------------------------------------

    public void setDescription(I18N.Text desc)
    {
        this.optDesc = desc;
    }

    public String getDescription(Locale locale)
    {
        return (this.optDesc != null)? this.optDesc.toString(locale) : "";
    }

    // ------------------------------------------------------------------------

    public boolean hasProperties()
    {
        return (this.optProps != null);
    }
    
    public RTProperties getProperties()
    {
        return this.optProps;
    }
    
    public void setValue(String key, String val)
    {
        if (this.optProps == null) {
            this.optProps = new RTProperties();
        }
        this.optProps.setString(key, val);
    }
    
    public String getValue(String key)
    {
        return (this.optProps != null)? this.optProps.getString(key,null) : null;
    }

    // ------------------------------------------------------------------------

}
