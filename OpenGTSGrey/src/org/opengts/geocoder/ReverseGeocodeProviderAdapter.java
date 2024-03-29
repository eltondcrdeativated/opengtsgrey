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
//  2008/05/14  Martin D. Flynn
//     -Initial release
//  2009/05/24  Martin D. Flynn
//     -Added command-line Reverse-Geocoder test.
// ----------------------------------------------------------------------------
package org.opengts.geocoder;

import org.opengts.util.*;

import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.tables.Account;


public abstract class ReverseGeocodeProviderAdapter
    implements ReverseGeocodeProvider
{

    // ------------------------------------------------------------------------

    private String       name           = null;
    private String       accessKey      = null;
    private RTProperties properties     = null;

    /**
    *** Constructor
    *** @param name  The name of this reverse-geocode provider
    *** @param key     The access key (may be null)
    *** @param rtProps The properties (may be null)
    **/
    public ReverseGeocodeProviderAdapter(String name, String key, RTProperties rtProps)
    {
        super();
        this.setName(name);
        this.setAuthorization(key);
        this.setProperties(rtProps);
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the name of this ReverseGeocodeProvider
    *** @param name  The name of this reverse-geocode provider
    **/
    public void setName(String name)
    {
        this.name = (name != null)? name : "";
    }

    /**
    *** Gets the name of this ReverseGeocodeProvider
    *** @return The name of this reverse-geocode provider
    **/
    public String getName()
    {
        return (this.name != null)? this.name : "";
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the authorization key of this ReverseGeocodeProvider
    *** @param key  The key of this reverse-geocode provider
    **/
    public void setAuthorization(String key)
    {
        this.accessKey = key;
    }
    
    /**
    *** Gets the authorization key of this ReverseGeocodeProvider
    *** @return The access key of this reverse-geocode provider
    **/
    public String getAuthorization()
    {
        return this.accessKey;
    }

    // ------------------------------------------------------------------------

    /**
    *** Parse and return the user name and password
    *** @return The username and password (always a 2 element array)
    **/
    protected String[] _getUserPass()
    {
        String username = null;
        String password = null;
        String key = this.getAuthorization();
        if ((key != null) && !key.equals("")) {
            int p = key.indexOf(":");
            if (p >= 0) {
                username = key.substring(0,p);
                password = key.substring(p+1);
            } else {
                username = key;
                password = "";
            }
        } else {
            username = null;
            password = null;
        }
        return new String[] { username, password };
    }

    /** 
    *** Return authorization username.  This assumes that the username and password are
    *** separated by a ':' character
    *** @return The username
    **/
    protected String getUsername()
    {
        return this._getUserPass()[0];
    }
    
    /** 
    *** Return authorization password.  This assumes that the username and password are
    *** separated by a ':' character
    *** @return The password
    **/
    protected String getPassword()
    {
        return this._getUserPass()[1];
    }


    // ------------------------------------------------------------------------

    /**
    *** Sets the properties for this ReverseGeocodeProvider
    *** @param rtProps  The properties for this reverse-geocode provider
    **/
    public void setProperties(RTProperties rtProps)
    {
        this.properties = rtProps;
    }

    /**
    *** Gets the properties for this ReverseGeocodeProvider
    *** @return The properties for this reverse-geocode provider
    **/
    public RTProperties getProperties()
    {
        if (this.properties == null) {
            this.properties = new RTProperties();
        }
        return this.properties;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a String representation of this instance
    *** @return A String representation of this instance
    **/
    public String toString()
    {
        StringBuffer sb= new StringBuffer();
        sb.append(this.getName());
        String auth = this.getAuthorization();
        if (!StringTools.isBlank(auth)) {
            sb.append(" [");
            sb.append(auth);
            sb.append("]");
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* ReverseGeocodeProvider interface methods */
    public abstract boolean isFastOperation();
    public abstract ReverseGeocode getReverseGeocode(GeoPoint gp);

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static final String ARG_ACCOUNT[]   = new String[] { "account"          , "acct"  };
    private static final String ARG_PNL[]       = new String[] { "privateLabelName" , "pln"   };
    private static final String ARG_GEOPOINT[]  = new String[] { "geoPoint"         , "gp"    };

    private static void usage()
    {
        String n = ReverseGeocodeProviderAdapter.class.getName();
        Print.logInfo("Usage:");
        Print.logInfo("   java ... " + n + " -geoPoint=<gp> -account=<id>");
        Print.logInfo(" or");
        Print.logInfo("   java ... " + n + " -geoPoint=<gp> -pnl=<name>");
        Print.logInfo("Common Options:");
        Print.logInfo("   -geoPoint=<gp>  GeoPoint in the form <latitude>/<longitude>");
        Print.logInfo("   -account=<id>   Acount ID from which to obtain the ReverseGeocodeProvider");
        Print.logInfo("   -pln=<name>     PrivateLabel name/host");
        System.exit(1);
    }

    public static void main(String args[])
    {
        DBConfig.cmdLineInit(args,true);  // main

        /* geoPoint */
        GeoPoint gp = new GeoPoint(RTConfig.getString(ARG_GEOPOINT,null));
        if (!gp.isValid()) {
            Print.sysPrintln("ERROR: No GeoPoint specified");
            usage();
        }

        /* get PrivateLabel */
        BasicPrivateLabel privLabel = null;
        String accountID = RTConfig.getString(ARG_ACCOUNT, "");
        if (!StringTools.isBlank(accountID)) {
            Account acct = null;
            try {
                acct = Account.getAccount(accountID); // may throw DBException
                if (acct == null) {
                    Print.sysPrintln("ERROR: Account-ID does not exist: " + accountID);
                    usage();
                }
                privLabel = acct.getPrivateLabel();
            } catch (DBException dbe) {
                Print.logException("Error loading Account: " + accountID, dbe);
                //dbe.printException();
                System.exit(99);
            }
        } else {
            String pln = RTConfig.getString(ARG_PNL,"default");
            if (StringTools.isBlank(pln)) {
                Print.sysPrintln("ERROR: Must specify '-account=<Account>'");
                usage();
            } else {
                privLabel = BasicPrivateLabelLoader.getPrivateLabel(pln);
                if (privLabel == null) {
                    Print.sysPrintln("ERROR: PrivateLabel name not found: %s", pln);
                    usage();
                }
            }
        }

        /* get reverse-geocoder */
        ReverseGeocodeProvider rgp = privLabel.getReverseGeocodeProvider();
        if (rgp == null) {
            Print.sysPrintln("ERROR: No ReverseGeocodeProvider for PrivateLabel: %s", privLabel.getName());
            System.exit(99);
        }

        /* get ReverseGeocode */
        try {
            // make sure the Domain properties are available to RTConfig
            privLabel.pushRTProperties();   // stack properties (may be redundant in servlet environment)
            ReverseGeocode rg = rgp.getReverseGeocode(gp); // get the reverse-geocode
            if (rg != null) {
                Print.sysPrintln("");
                Print.sysPrintln("ReverseGeocode:");
                Print.sysPrintln(rg.toString());
                Print.sysPrintln("");
            } else {
                Print.sysPrintln("ERROR: Unable to obtain ReverseGeocode for specified point: " + gp);
            }
        } catch (Throwable th) {
            // ignore
        } finally {
            privLabel.popRTProperties();    // remove from stack
        }

    }

}
