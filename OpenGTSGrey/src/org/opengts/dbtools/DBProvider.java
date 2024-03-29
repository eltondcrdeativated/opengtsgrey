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
// Eventually, this module should contain all database specific attributes, so that
// only this module need be changed in order to support another database provider.
// ----------------------------------------------------------------------------
// UTF-8 Encoding:
//  MySQL: 
//      SHOW CHARACTER SET;
//      SHOW FULL COLUMNS FROM <table>;  // new column "Collation"
//      ALTER TABLE <table> CONVERT TO CHARACTER SET utf8 COLLATE utf8_general_ci
//      ALTER TABLE <table> MODIFY <column> TEXT       CHARACTER SET utf8;
//      ALTER TABLE <table> MODIFY <column> VARCHAR(M) CHARACTER SET utf8;
// ----------------------------------------------------------------------------
// Change History:
//  2007/09/16  Martin D. Flynn
//     -Initial release
//  2007/11/28  Martin D. Flynn
//     -Added several data type methods:
//      'isDataTypeUnsigned(...)', 'isDataTypeNumeric(...)', 'isDataTypeString(...)', ...
//  2007/12/13  Martin D. Flynn
//     -Update 'updateRecordInTable' to display a warning when updating a primary key
//  2008/01/10  Martin D. Flynn
//     -Added "SUM(x)" field
//  2008/02/27  Martin D. Flynn
//     -Added 'isDataTypeDecimal' method
//     -Fixed '_getTypeIndex' to properly check for 'STRING[...]' types
//  2008/03/12  Martin D. Flynn
//     -Added 'supportsOffset' method
//     -Added separate 'createAlternateIndex' method
//  2008/04/11  Martin D. Flynn
//     -Added additional error message when JDBC driver is not found.
//  2008/05/14  Martin D. Flynn
//     -Added support for creating multiple alternate indexes
//  2008/12/01  Martin D. Flynn
//     -Added support MySQL InnoDB indexes.
//  2009/01/01  Martin D. Flynn
//     -Modified "getDBUri(...)" to check for new runtime property RTKey.DB_URL_DB
//      when needing to get a db connection URL which includes the db-name.
//     -Changed DB_CONNECTION[_DB] to DB_URL[_DB].
//  2009/01/28  Martin D. Flynn
//     -Added changes to support UTF8 character sets
//  2009/04/02  Martin D. Flynn
//     -Fixed to add "CHARACTER SET utf8" to specific fields upon initial table
//      creation (previously required an additional call to "dbAdmin -tables=ca").
//  2009/05/01  Martin D. Flynn
//     -Added DateTime datatype
// ----------------------------------------------------------------------------
package org.opengts.dbtools;

import java.lang.*;
import java.lang.reflect.*;
import java.io.File;
import java.util.*;
import java.sql.*;

import org.opengts.util.*;

/**
*** <code>DBProvider</code> specifies SQL database provider specific attributes.
**/

public class DBProvider
{

    // ------------------------------------------------------------------------

    public static final String      PRIMARY_INDEX_NAME      = "PRIMARY";    // MySQL
    public static final String      UNIQUE_INDEX_NAME       = "UNIQUE";     // MySQL
    public static final String      DEFAULT_ALT_INDEX_NAME  = "altIndex";

    // ------------------------------------------------------------------------

    // "like" wildcard character
    public static final String      LIKE_WILDCARD           = "%";
    
    // ------------------------------------------------------------------------

    // MySQL: "COUNT(*)"
    private static final String     FLD_count_              = "COUNT(*)";
    public static String FLD_COUNT() { return FLD_count_; }
    
    // MySQL: "SUM(<column>)"
    private static final String     FLD_sum_                = "SUM";
    public static String FLD_SUM(String col) { return "SUM(" + col + ")"; }

    // ------------------------------------------------------------------------

    public static final String      TYPE_UNKNOWN            = "UNKNOWN";
    public static final String      TYPE_BOOLEAN            = "BOOLEAN";
    public static final String      TYPE_INT8               = "INT8";
    public static final String      TYPE_UINT8              = "UINT8";
    public static final String      TYPE_INT16              = "INT16";
    public static final String      TYPE_UINT16             = "UINT16";
    public static final String      TYPE_INT32              = "INT32";
    public static final String      TYPE_UINT32             = "UINT32";
    public static final String      TYPE_INT64              = "INT64";
    public static final String      TYPE_UINT64             = "UINT64";
    public static final String      TYPE_FLOAT              = "FLOAT";
    public static final String      TYPE_DOUBLE             = "DOUBLE";
    public static final String      TYPE_SBLOB              = "SBLOB";      // [SMALL]BLOB (4095 bytes)
    public static final String      TYPE_BLOB               = "BLOB";       // BLOB (65535 bytes)
    public static final String      TYPE_MBLOB              = "MBLOB";      // MEDIUMBLOB (16777215 bytes)
    public static final String      TYPE_TEXT               = "TEXT";       // CLOB (65535 bytes)
    public static final String      TYPE_STRING             = "STRING";
    public static final String      TYPE_DATETIME           = "DATETIME";
    public static String TYPE_STRING(int size) { return TYPE_STRING + "[" + size + "]"; }
    public static final String      TYPE_ARRAY[]            = new String[] {
        TYPE_UNKNOWN ,
        TYPE_BOOLEAN ,
        TYPE_INT8    ,
        TYPE_UINT8   ,
        TYPE_INT16   ,
        TYPE_UINT16  ,
        TYPE_INT32   ,
        TYPE_UINT32  ,
        TYPE_INT64   ,
        TYPE_UINT64  ,
        TYPE_FLOAT   ,
        TYPE_DOUBLE  ,
        TYPE_SBLOB   ,
        TYPE_BLOB    ,
        TYPE_MBLOB   ,
        TYPE_TEXT    ,
        TYPE_STRING  ,
        TYPE_DATETIME,
    };

    public static final int         SQL_UNKNOWN             = 0;    // TYPE_UNKNOWN
    public static final int         SQL_BOOLEAN             = 1;    // TYPE_BOOLEAN
    public static final int         SQL_INT8                = 2;    // TYPE_INT8
    public static final int         SQL_UINT8               = 3;    // TYPE_UINT8
    public static final int         SQL_INT16               = 4;    // TYPE_INT16
    public static final int         SQL_UINT16              = 5;    // TYPE_UINT16
    public static final int         SQL_INT32               = 6;    // TYPE_INT32
    public static final int         SQL_UINT32              = 7;    // TYPE_UINT32
    public static final int         SQL_INT64               = 8;    // TYPE_INT64
    public static final int         SQL_UINT64              = 9;    // TYPE_UINT64
    public static final int         SQL_FLOAT               = 10;   // TYPE_FLOAT
    public static final int         SQL_DOUBLE              = 11;   // TYPE_DOUBLE
    public static final int         SQL_SBLOB               = 12;   // TYPE_SBLOB
    public static final int         SQL_BLOB                = 13;   // TYPE_BLOB
    public static final int         SQL_MBLOB               = 14;   // TYPE_MBLOB
    public static final int         SQL_TEXT                = 15;   // TYPE_TEXT
    public static final int         SQL_VARCHAR             = 16;   // TYPE_STRING
    public static final int         SQL_DATETIME            = 17;   // TYPE_DATETIME

    public static String SQL_VARCHAR(int size) { return SQL_VARCHAR + "(" + size + ")"; }

    // ------------------------------------------------------------------------
    
    public    static final int      DATATYPE_NONE           = 0x0000;
    public    static final int      DATATYPE_BOOLEAN        = 0x0001;
    public    static final int      DATATYPE_UNSIGNED       = 0x0002;
    public    static final int      DATATYPE_SIGNED         = 0x0004;
    public    static final int      DATATYPE_DECIMAL        = 0x0008;
    public    static final int      DATATYPE_STRING         = 0x0010;
    public    static final int      DATATYPE_BINARY         = 0x0020;
    public    static final int      DATATYPE_DATETIME       = 0x0040;

    public    static final int      DATATYPE_NUMERIC        = DATATYPE_UNSIGNED | DATATYPE_SIGNED | DATATYPE_DECIMAL;

    public    static final int      DATATYPE_ARRAY[]        = new int[] {
        DATATYPE_NONE    ,  // TYPE_UNKNOWN ,
        DATATYPE_BOOLEAN ,  // TYPE_BOOLEAN ,
        DATATYPE_SIGNED  ,  // TYPE_INT8    ,
        DATATYPE_UNSIGNED,  // TYPE_UINT8   ,
        DATATYPE_SIGNED  ,  // TYPE_INT16   ,
        DATATYPE_UNSIGNED,  // TYPE_UINT16  ,
        DATATYPE_SIGNED  ,  // TYPE_INT32   ,
        DATATYPE_UNSIGNED,  // TYPE_UINT32  ,
        DATATYPE_SIGNED  ,  // TYPE_INT64   ,
        DATATYPE_UNSIGNED,  // TYPE_UINT64  ,
        DATATYPE_DECIMAL ,  // TYPE_FLOAT   ,
        DATATYPE_DECIMAL ,  // TYPE_DOUBLE  ,
        DATATYPE_BINARY  ,  // TYPE_SBLOB   ,
        DATATYPE_BINARY  ,  // TYPE_BLOB    ,
        DATATYPE_BINARY  ,  // TYPE_MBLOB   ,
        DATATYPE_STRING  ,  // TYPE_TEXT    ,
        DATATYPE_STRING  ,  // TYPE_STRING  ,
        DATATYPE_DATETIME,  // TYPE_DATETIME,
    };

    // ------------------------------------------------------------------------

    public    static final int      DB_MYSQL                = 1; // MySQL:
    public    static final int      DB_POSTGRESQL           = 2; // PostgreSQL: not fully supported!
    public    static final int      DB_DERBY                = 3; // Derby:
    public    static final int      DB_SQLSERVER            = 4; // SQLServer:

    // ------------------------------------------------------------------------

    public    static final long     FLAGS_NONE              = 0x0000000000000000L;
    public    static final long     FLAGS_LIMIT             = 0x0000000000000001L;
    public    static final long     FLAGS_OFFSET            = 0x0000000000000002L;

    // ------------------------------------------------------------------------
    // MySQL drivers
    // Example configuration properties:
    //   db.sql.provider=mysql
    //   db.sql.dbname=gts
    //   db.sql.host=localhost
    //   db.sql.port=3306
    //   db.sql.user=gts
    //   db.sql.pass=opengts
    //   db.sql.connection=jdbc:mysql://localhost:${db.sql.port}/
    // ------------------------------------------------------------------------
    // Fully supported
    // ------------------------------------------------------------------------
    // Porting to InnoDB:
    //  - http://www.linux.com/articles/46370
    
    protected static DBProvider MySQL_MyISAM_Provider = new DBProvider(
        "mysql", DB_MYSQL, 3306,
        new String[] { "com.mysql.jdbc.Driver", "org.gjt.mm.mysql.Driver" },
        "",                                         // table name prefix
        "type=MyISAM",                              // "CREATE TABLE" suffix
        FLAGS_LIMIT|FLAGS_OFFSET,                   // flags
        new String[] {
            /* SQL_UNKNOWN */ "",
            /* SQL_BOOLEAN */ "TINYINT",            //  8bit          Java 'boolean'
            /* SQL_INT8    */ "TINYINT",            //  8bit (signed) Java 'byte'
            /* SQL_UINT8   */ "TINYINT UNSIGNED",   //  8bit          Java 'byte'
            /* SQL_INT16   */ "SMALLINT",           // 16bit (signed)
            /* SQL_UINT16  */ "SMALLINT UNSIGNED",  // 16bit (signed)
            /* SQL_INT32   */ "INT",                // 32bit (signed) Java 'int'
            /* SQL_UINT32  */ "INT UNSIGNED",       // 32bit          Java 'int'
            /* SQL_INT64   */ "BIGINT",             // 64bit (signed) Java 'long'
            /* SQL_UINT64  */ "BIGINT UNSIGNED",    // 64bit          Java 'long'
            /* SQL_FLOAT   */ "FLOAT",
            /* SQL_DOUBLE  */ "DOUBLE",
            /* SQL_SBLOB   */ "BLOB",               // max (2^12 - 1) bytes
            /* SQL_BLOB    */ "BLOB",               // max (2^16 - 1) bytes
            /* SQL_MBLOB   */ "MEDIUMBLOB",         // max (2^24 - 1) bytes
            /* SQL_TEXT    */ "TEXT",               // max (2^16 - 1) bytes
            /* SQL_VARCHAR */ "VARCHAR",
            /* SQL_DATETIME*/ "DATETIME",
        }
    );
    
    protected static DBProvider MySQL_InnoDB_Provider = new DBProvider(
        "mysql_innodb", DB_MYSQL, 3306,
        new String[] { "com.mysql.jdbc.Driver", "org.gjt.mm.mysql.Driver" },
        "",                                         // table name prefix
        "type=InnoDB",                              // "CREATE TABLE" suffix
        FLAGS_LIMIT|FLAGS_OFFSET,                   // flags
        new String[] {
            /* SQL_UNKNOWN */ "",
            /* SQL_BOOLEAN */ "TINYINT",            //  8bit          Java 'boolean'
            /* SQL_INT8    */ "TINYINT",            //  8bit (signed) Java 'byte'
            /* SQL_UINT8   */ "TINYINT UNSIGNED",   //  8bit          Java 'byte'
            /* SQL_INT16   */ "SMALLINT",           // 16bit (signed)
            /* SQL_UINT16  */ "SMALLINT UNSIGNED",  // 16bit (signed)
            /* SQL_INT32   */ "INT",                // 32bit (signed) Java 'int'
            /* SQL_UINT32  */ "INT UNSIGNED",       // 32bit          Java 'int'
            /* SQL_INT64   */ "BIGINT",             // 64bit (signed) Java 'long'
            /* SQL_UINT64  */ "BIGINT UNSIGNED",    // 64bit          Java 'long'
            /* SQL_FLOAT   */ "FLOAT",
            /* SQL_DOUBLE  */ "DOUBLE",
            /* SQL_SBLOB   */ "BLOB",               // max (2^12 - 1) bytes
            /* SQL_BLOB    */ "BLOB",               // max (2^16 - 1) bytes
            /* SQL_MBLOB   */ "MEDIUMBLOB",         // max (2^24 - 1) bytes
            /* SQL_TEXT    */ "TEXT",               // max (2^16 - 1) bytes
            /* SQL_VARCHAR */ "VARCHAR",
            /* SQL_DATETIME*/ "DATETIME",
        }
    );

    // ------------------------------------------------------------------------
    // PostgreSQL driver
    // http://jdbc.postgresql.org/download.html
    // Example configuration properties:
    //   db.sql.provider=postgresql
    //   db.sql.dbname=gts
    //   db.sql.host=localhost
    //   db.sql.port=5432
    //   db.sql.user=gts
    //   db.sql.pass=opengts
    //   db.sql.connection=jdbc:postgresql://localhost:${db.sql.port}/
    // ------------------------------------------------------------------------
    // Not yet supported
    // ------------------------------------------------------------------------
    
    protected static DBProvider psqlProvider = new DBProvider(
        "postgresql", DB_POSTGRESQL, 5432,
        new String[] { "org.postgresql.Driver" },
        "PG",                                       // table name prefix
        null,                                       // "CREATE TABLE" suffix (ie. index type)
        FLAGS_LIMIT|FLAGS_OFFSET,                   // flags
        new String[] {
            /* SQL_UNKNOWN */ "",
            /* SQL_BOOLEAN */ "TINYINT",            //  8bit          Java 'boolean'
            /* SQL_INT8    */ "TINYINT",            //  8bit (signed) Java 'byte'
            /* SQL_UINT8   */ "TINYINT UNSIGNED",   //  8bit          Java 'byte'
            /* SQL_INT16   */ "SMALLINT",           // 16bit (signed)
            /* SQL_UINT16  */ "SMALLINT UNSIGNED",  // 16bit (signed)
            /* SQL_INT32   */ "INT",                // 32bit (signed) Java 'int'
            /* SQL_UINT32  */ "INT UNSIGNED",       // 32bit          Java 'int'
            /* SQL_INT64   */ "BIGINT",             // 64bit (signed) Java 'long'
            /* SQL_UINT64  */ "BIGINT UNSIGNED",    // 64bit          Java 'long'
            /* SQL_FLOAT   */ "FLOAT",
            /* SQL_DOUBLE  */ "DOUBLE",
            /* SQL_SBLOB   */ "BLOB",               // max (2^16 - 1) bytes
            /* SQL_BLOB    */ "BLOB",               // max (2^16 - 1) bytes
            /* SQL_MBLOB   */ "MEDIUMBLOB",         // max (2^24 - 1) bytes
            /* SQL_TEXT    */ "TEXT",               // max (2^16 - 1) bytes
            /* SQL_VARCHAR */ "VARCHAR",
            /* SQL_DATETIME*/ "DATETIME",
        }
    );

    // ------------------------------------------------------------------------
    // Apache Derby driver
    // http://db.apache.org/derby
    // Example configuration properties:
    //   db.sql.provider=derby
    //   db.sql.dbname=gts
    //   db.sql.host=localhost
    //   db.sql.port=1527
    //   db.sql.user=gts
    //   db.sql.pass=opengts
    //   db.sql.connection=jdbc:derby://localhost:${db.sql.port}/${db.sql.dbname};create=true
    // ------------------------------------------------------------------------
    // Partially supported
    // ------------------------------------------------------------------------
    
    protected static DBProvider Derby_Provider = new DBProvider(
        "derby", DB_DERBY, 1527,
        new String[] { "org.apache.derby.jdbc.ClientDriver", "org.apache.derby.jdbc.EmbeddedDriver" },
        "DB",                                       // table name prefix
        null,                                       // "CREATE TABLE" suffix
        FLAGS_NONE,                                 // flags
        new String[] {
            /* SQL_UNKNOWN */ "",
            /* SQL_BOOLEAN */ "SMALLINT",
            /* SQL_INT8    */ "SMALLINT",
            /* SQL_UINT8   */ "SMALLINT",
            /* SQL_INT16   */ "SMALLINT",
            /* SQL_UINT16  */ "SMALLINT",
            /* SQL_INT32   */ "INTEGER",
            /* SQL_UINT32  */ "INTEGER",
            /* SQL_INT64   */ "BIGINT",
            /* SQL_UINT64  */ "BIGINT",
            /* SQL_FLOAT   */ "FLOAT",
            /* SQL_DOUBLE  */ "DOUBLE",
            /* SQL_SBLOB   */ "BLOB",
            /* SQL_BLOB    */ "BLOB",
            /* SQL_MBLOB   */ "BLOB",
            /* SQL_TEXT    */ "CLOB",
            /* SQL_VARCHAR */ "VARCHAR",
            /* SQL_DATETIME*/ "DATETIME",
        }
    );

    // ------------------------------------------------------------------------
    // Microsoft SQL Server driver
    // http://db.apache.org/derby
    // Example configuration properties:
    //   db.sql.provider=sqlserver
    //   db.sql.dbname=gts
    //   db.sql.host=localhost
    //   db.sql.port=1433
    //   db.sql.user=gts
    //   db.sql.pass=opengts
    //   db.sql.connection=jdbc:sqlserver://localhost:${db.sql.port}
    // ------------------------------------------------------------------------
    // Mostly supported
    // ------------------------------------------------------------------------
    
    protected static DBProvider MS_SQLServer_Provider = new DBProvider(
        "sqlserver", DB_SQLSERVER, 1433,
        new String[] { "com.microsoft.sqlserver.jdbc.SQLServerDriver" },
        "MS",                                       // table name prefix
        null,                                       // "CREATE TABLE" suffix
        FLAGS_LIMIT,                                // flags (does not support offset)
        new String[] {
            /* SQL_UNKNOWN */ "",
            /* SQL_BOOLEAN */ "TINYINT",            //  8bit          Java 'boolean'
            /* SQL_INT8    */ "TINYINT",            //  8bit (signed) Java 'byte'
            /* SQL_UINT8   */ "TINYINT",            //  8bit          Java 'byte'
            /* SQL_INT16   */ "SMALLINT",           // 16bit (signed)
            /* SQL_UINT16  */ "SMALLINT",           // 16bit (signed)
            /* SQL_INT32   */ "BIGINT",             // 32bit (signed) Java 'int'
            /* SQL_UINT32  */ "BIGINT",             // 32bit          Java 'int'
            /* SQL_INT64   */ "BIGINT",             // 64bit (signed) Java 'long'
            /* SQL_UINT64  */ "BIGINT",             // 64bit          Java 'long'
            /* SQL_FLOAT   */ "FLOAT",
            /* SQL_DOUBLE  */ "FLOAT",
            /* SQL_SBLOB   */ "VARBINARY",          // max 8000 bytes
            /* SQL_BLOB    */ "IMAGE",              // max (2^31 - 1) bytes
            /* SQL_MBLOB   */ "IMAGE",              // max (2^31 - 1) bytes
            /* SQL_TEXT    */ "TEXT",               // max (2^31 - 1) bytes
            /* SQL_VARCHAR */ "VARCHAR",
            /* SQL_DATETIME*/ "DATETIME",
        }
    );

    // ------------------------------------------------------------------------
    
    private static DBProvider globalDBProvider = null;
    private static HashMap<String,DBProvider> globalDBProviderMap = new HashMap<String,DBProvider>();
    static {
        globalDBProviderMap.put(MySQL_MyISAM_Provider.getJDBCName(), MySQL_MyISAM_Provider);
        globalDBProviderMap.put(MySQL_InnoDB_Provider.getJDBCName(), MySQL_InnoDB_Provider);
        globalDBProviderMap.put(Derby_Provider.getJDBCName()       , Derby_Provider);
        globalDBProviderMap.put(MS_SQLServer_Provider.getJDBCName(), MS_SQLServer_Provider);
    }

    /**
    *** Returns the current DBProvider as defined in the Runtime configuration file (priote4ry id "RTKey.DB_PROVIDER")
    *** @return the current DBProvider
    **/
    public static DBProvider getProvider()
    {
        if (globalDBProvider == null) {
            String dbpName = RTConfig.getString(RTKey.DB_PROVIDER);
            if (StringTools.isBlank(dbpName)) {
                globalDBProvider = MySQL_MyISAM_Provider;
                Print.logWarn("DBProvider not specified, using default ("+globalDBProvider.getJDBCName()+").");
            } else {
                globalDBProvider = globalDBProviderMap.get(dbpName);
                if (globalDBProvider == null) {
                    globalDBProvider = MySQL_MyISAM_Provider;
                    Print.logWarn("DBProvider '"+dbpName+"' not found, using default ("+globalDBProvider.getJDBCName()+").");
                } else {
                    Print.logDebug("DBProvider installed: " + globalDBProvider.getJDBCName());
                }
            }
            DBProvider.isTableLockingEnabled();
        }
        return globalDBProvider;
    }
    
    /**
    *** Returns the name of the current DBProvider
    *** @return The current DBProvider name
    **/
    public static String getProviderName()
    {
        return DBProvider.getProvider().getJDBCName();
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Loads the current DBProvider class drivers
    *** @return the name of the loaded DBProvider class
    **/
    public static String loadJDBCDriver()
    {
        String driver[] = DBProvider.getProvider().getDrivers();
        if (driver != null) {
            
            /* try loading drivers */
            for (int i = 0; i < driver.length; i++) {
                try {
                    Class.forName(driver[i]);
                    return driver[i]; // stop at the first one that works
                } catch (Throwable t) { // ClassNotFoundException
                    Print.logWarn("JDBC driver class not found: " + driver[i]);
                    // continue
                }
            }
            
            /* error message */
            Print.logError("-------------------------------------------------------------------------");
            String extDir = System.getProperty("java.ext.dirs");
            if (extDir != null) {
                String edir[] = StringTools.split(extDir, File.pathSeparatorChar);
                if (edir.length > 1) {
                    Print.logError("Install the JDBC jar file in one of the following directories:");
                    for (int i = 0; i < edir.length; i++) {
                        Print.logError(" > " + edir[i]);
                    }
                } else {
                    Print.logError("Install the JDBC jar file in the following directory:");
                    Print.logError(" > " + extDir);
                }
            } else {
                Print.logError("Install JDBC drivers");
            }
            Print.logError("(NOTE: The installed jar file permissions must also be world-readable)");
            Print.logError("-------------------------------------------------------------------------");
            
        }
        return null;
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /** 
    *** Returns the host name for the DBProvider server
    *** @return The name of the host serving the database
    **/
    public static String getDBHost()
    {
        return RTConfig.getString(RTKey.DB_HOST);
    }
    
    /**
    *** Returns the port number for the DBProvider server
    *** @return The port number
    **/
    public static int getDBPort()
    {
        return DBProvider.getProvider().getDefaultPort();
    }

    /**
    *** Returns the database name
    *** @return The database name
    **/
    public static String getDBName()
    {
        return RTConfig.getString(RTKey.DB_NAME);
    }

    /**
    *** Returns the DB user name
    *** @return The DB user name
    **/
    public static String getDBUsername()
    {
        if (RTConfig.hasProperty(RTKey.DB_USER,false)) {
            return RTConfig.getString(RTKey.DB_USER, "");
        } else {
            String user = RTConfig.getString(RTKey.DB_USER);
            return (user != null)? user : "";
        }
    }

    /**
    *** Returns the DB user password
    *** @return The DB user password
    **/
    public static String getDBPassword()
    {
        if (RTConfig.hasProperty(RTKey.DB_PASS,false)) {
            return RTConfig.getString(RTKey.DB_PASS);
        } else {
            String pass = RTConfig.getString(RTKey.DB_PASS);
            return (pass != null)? pass : "";
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static boolean didLogJdbcURL    = false;

    private static String  JDBC_URL[]       = new String[] { RTKey.DB_URL    };
    private static String  JDBC_URL_DB[]    = new String[] { RTKey.DB_URL_DB };

    /**
    *** Returns the database access URI
    *** @param inclDBName  True to include the name of the database in the URI
    *** @return The database access URI
    **/
    public static String getDBUri(boolean inclDBName)
    {

        /* check runtime properties for URL */
        String urlStr = null;
        if (inclDBName) {
            urlStr = RTConfig.getString(JDBC_URL_DB,"").trim();
            if (!StringTools.isBlank(urlStr)) {
                // explicitly specified in runtime properties
                return urlStr;
            }
            // start with standard connection url
            urlStr = RTConfig.getString(JDBC_URL,"").trim();
        } else {
            urlStr = RTConfig.getString(JDBC_URL,"").trim();
            if (!StringTools.isBlank(urlStr)) {
                // explicitly specified in runtime properties
                return urlStr;
            }
        }

        /* construct default URL */
        StringBuffer uri = new StringBuffer(urlStr);
        DBProvider dbProv = DBProvider.getProvider();
        switch (dbProv.getID()) {
            case DB_MYSQL: {
                // MySQL: jdbc:mysql://<host>:<port>/[<database>]
                if (uri.length() == 0) {
                    uri.append("jdbc:mysql://").append(getDBHost()).append(":").append(getDBPort()).append("/");
                }
                if (inclDBName) {
                    uri.append(getDBName());
                }
                break;
            }
            case DB_POSTGRESQL: {
                // PostgreSQL: jdbc:postgresql://<host>:<port>/[<database>]
                if (uri.length() == 0) {
                    uri.append("jdbc:postgresql://").append(getDBHost()).append(":").append(getDBPort()).append("/");
                }
                if (inclDBName) {
                    uri.append(getDBName());
                }
                break;
            }
            case DB_DERBY: {
                // Derby: jdbc:derby://<host>:<port>/<database>[;create=true]
                // ij> connect 'jdbc:derby://localhost:1527/gts;create=true';
                if (uri.length() == 0) {
                    uri.append("jdbc:derby://").append(getDBHost()).append(":").append(getDBPort()).append("/");
                    uri.append(getDBName()).append(";create=true");
                }
                break;
            }
            case DB_SQLSERVER: {
                // ["Building the Connection URL"] http://msdn2.microsoft.com/en-us/library/ms378428.aspx
                // SQLServer: jdbc:sqlserver://<host>[;instanceName=<instance>]:<port>;property=value[;databaseName=<db>]
                // Note: in some cases, specifying both the instanceName and port, will cause the connection to hang/timeout.
                if (uri.length() == 0) {
                    uri.append("jdbc:sqlserver://").append(getDBHost()).append(":").append(getDBPort());
                }
                if (inclDBName && (uri.indexOf("databaseName=") < 0)) {
                    uri.append(";databaseName=").append(getDBName()); 
                }
                break;
            }
            default : {
                // unrecognized DB provider
                return null;
            }
        }

        /* return URL */
        if (!didLogJdbcURL) {
            didLogJdbcURL = true;
            if (uri.length() > 0) {
                Print.logDebug("Connection URL: " + uri);
            }
        }
        return uri.toString();

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns the index of the specified SQL SQL datatype
    *** @param localType     The local type
    *** @param isDefinedType Is a 'defined' type
    *** @return The type 'index'
    **/
    protected static int _getTypeIndex(String localType, boolean isDefinedType)
    {
        if (localType != null) {
            localType = localType.toUpperCase();
            for (int i = 1; i < TYPE_ARRAY.length; i++) {
                if (localType.equals(TYPE_ARRAY[i])) {
                    return i;
                }
            }
            if (localType.startsWith(TYPE_STRING + "[")) {
                return SQL_VARCHAR;
            }
        }
        if (isDefinedType) {
            Print.logWarn("Type not found: " + localType);
        }
        return 0;
    }

    /**
    *** Returns 'true' is the specified types are equivalent
    *** @param defType  The defined type
    *** @param actType  The actual tupe
    *** @return True if the types are equivalent
    **/
    public static boolean areTypesEquivalent(String defType, String actType)
    {
        boolean equivalent = false;
        if ((defType == null) || (actType == null)) {
            // one of the types is null
            equivalent = false;
        } else
        if (defType.equalsIgnoreCase(actType)) {
            // types are equal
            equivalent = true;
        } else
        if (defType.startsWith(TYPE_STRING + "[")) {
            // STRING[xx] types must match exactly (and they don't, if we are here)
            equivalent = false;
        } else {
            // return true if types are backed by the same sql type
            int defNdx = DBProvider._getTypeIndex(defType,true);
            int actNdx = DBProvider._getTypeIndex(actType,false);
            String sqlTypes[] = DBProvider.getProvider().getSQLTypes();
            equivalent = sqlTypes[defNdx].equals(sqlTypes[actNdx]);
        }
        //Print.logInfo("Type equivalent? " + defType + " <==> " + actType + " : " + equivalent);
        return equivalent;
    }

    /**
    *** Returns the SQL type for the defined specified local type
    *** @param localType  The local type
    *** @return The SQL type
    **/
    public static String getSqlTypeFromDataType(String localType)
    {
        if (localType != null) {
            localType = localType.toUpperCase();
            if (localType.startsWith(TYPE_STRING + "[")) {
                String x = localType.substring(TYPE_STRING.length() + 1);
                int len = StringTools.parseInt(x, 32);
                return DBProvider.getSQLType(SQL_VARCHAR) + "(" + len + ")";
            } else {
                int ndx = DBProvider._getTypeIndex(localType,true);
                return DBProvider.getSQLType(ndx);
            }
        } else {
            return "";
        }
    }

    /**
    *** Returns the defined local type for the specified SQL type
    *** @param sqlType  The SQL type
    *** @return  The defined local type
    **/
    public static String getDataTypeFromSqlType(String sqlType)
    {
        if (sqlType != null) {
            sqlType = sqlType.toUpperCase();
            DBProvider dbProv = DBProvider.getProvider();
            String sqlTypes[] = dbProv.getSQLTypes();
            if (sqlType.startsWith(sqlTypes[SQL_INT8])) {
                return (sqlType.indexOf("UNSIGNED") > 0)? TYPE_UINT8 : TYPE_INT8;
            } else
            if (sqlType.startsWith(sqlTypes[SQL_INT16])) {
                return (sqlType.indexOf("UNSIGNED") > 0)? TYPE_UINT16 : TYPE_INT16;
            } else
            if (sqlType.startsWith(sqlTypes[SQL_INT32])) {
                return (sqlType.indexOf("UNSIGNED") > 0)? TYPE_UINT32 : TYPE_INT32;
            } else
            if (sqlType.startsWith(sqlTypes[SQL_INT64])) {
                return (sqlType.indexOf("UNSIGNED") > 0)? TYPE_UINT64 : TYPE_INT64;
            } else
            if (sqlType.startsWith(sqlTypes[SQL_FLOAT])) {
                return TYPE_FLOAT;
            } else
            if (sqlType.startsWith(sqlTypes[SQL_DOUBLE])) {
                return TYPE_DOUBLE;
            } else
            if (sqlType.startsWith(sqlTypes[SQL_SBLOB])) {
                return TYPE_SBLOB;
            } else
            if (sqlType.startsWith(sqlTypes[SQL_BLOB])) {
                return TYPE_BLOB;
            } else
            if (sqlType.startsWith(sqlTypes[SQL_MBLOB])) {
                return TYPE_MBLOB;
            } else
            if (sqlType.startsWith(sqlTypes[SQL_TEXT])) {
                return TYPE_TEXT;
            } else
            if (sqlType.startsWith(sqlTypes[SQL_VARCHAR] + "(")) {
                String x = sqlType.substring(sqlTypes[SQL_VARCHAR].length() + 1);
                int len = StringTools.parseInt(x, 32);
                return TYPE_STRING(len);
            } else {
                Print.logWarn("Unrecognized SQL type: " + sqlType);
                return TYPE_UNKNOWN;
            }
        } else {
            return TYPE_UNKNOWN;
        }
    }

    /**
    *** Returns the local data type for the specified index
    *** @param dataTypeNdx  The data type index (Note: NO BOUNDS CHEKCING IS PERFORMED!)
    *** @return The local data type definition
    **/
    public static String getSQLType(int dataTypeNdx)
    {
        try {
            String dt[] = DBProvider.getProvider().getSQLTypes();
            return dt[dataTypeNdx];
        } catch (Throwable th) { // IndexOufOfBounds, etc
            return "";
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns the existing fields in the specified table
    *** @param tableName The table name
    *** @return An array of fields
    *** @throws DBException if an error occured fetching the fields
    **/
    public static DBField[] getActualTableFields(String tableName)
        throws DBException
    {

        /* get table index map */
        DBTableIndexMap indexMap = null;
        try {
            indexMap = DBProvider.getActualTableIndexMap(tableName);
        } catch (DBException dbe) {
            Print.logWarn("Unable to retrieve table index information: " + dbe.getMessage());
        }

        /* this is currently only supported by MySQL and MS SQLServer */
        DBProvider dbp = DBProvider.getProvider();
        String showCols = null;
        if (dbp.getID() == DB_MYSQL) {
            // Ref: http://dev.mysql.com/doc/refman/5.0/en/show-columns.html
            // MySQL: "SHOW FULL COLUMNS FROM <TableName>"
            showCols = "SHOW FULL COLUMNS FROM " + tableName;
        } else
        if (dbp.getID() == DB_SQLSERVER) {
            // SQLServer: "SELECT column_name FROM <dbname>.information_schema.columns WHERE table_name = '<table>'"
            showCols = "SELECT column_name as [Field],(CAST(Data_type as nvarchar) + '(' + CAST(isnull(Character_maximum_length,numeric_precision) as nvarchar) + ')' ) as [Type], 'KEY' = CASE IS_NULLABLE WHEN 'NO' THEN 'PRI' ELSE '' END from information_schema.columns WHERE table_name='" + tableName + "';";
        } else {
            // Derby:: "SELECT * FROM sys.syscolumns WHERE ??? 
            return null;
        }

        /* extract fields */
        Statement stmt = null;
        ResultSet rs = null;
        Vector<DBField> dbf = new Vector<DBField>();
        try {
            stmt = DBFactory.execute(showCols);
            rs = stmt.getResultSet();
            while (rs.next()) {
                String fldName = rs.getString("Field");
                String fldType = rs.getString("Type");
                String charSet = rs.getString("Collation"); // latin1_swedish_ci, utf8_general_ci
                String fldDflt = StringTools.trim(rs.getString("Default"));
                String fldExtr = StringTools.trim(rs.getString("Extra"));
                Set<String> ndxSet  = null;
                if (indexMap != null) {
                    ndxSet = indexMap.getIndexesForColumn(fldName);
                } else {
                    ndxSet = new HashSet<String>();
                    String keyType = rs.getString("Key");  // useless for multiple alternate keys
                    if (keyType == null) {
                        ndxSet.add("?");
                    } else
                    if (keyType.equalsIgnoreCase("PRI")) {
                        ndxSet.add(PRIMARY_INDEX_NAME);
                    } else
                    if (keyType.equalsIgnoreCase("UNI")) {
                        //ndxSet.add(DEFAULT_ALT_INDEX_NAME); 
                        ndxSet.add(UNIQUE_INDEX_NAME);
                    } else
                    if (keyType.equalsIgnoreCase("MUL")) {
                        ndxSet.add(DEFAULT_ALT_INDEX_NAME); 
                    } else
                    if (!keyType.equals("")) {
                        ndxSet.add(PRIMARY_INDEX_NAME);
                    }
                    if (ndxSet.isEmpty()) { ndxSet = null; }
                }
                boolean autoIncr = fldExtr.equalsIgnoreCase("auto_increment");
                DBField field = new DBField(tableName, fldName, fldType, autoIncr, charSet, ndxSet);
                dbf.add(field);
            }
        } catch (SQLException sqe) {
            throw new DBException("Unable to get fields", sqe);
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
        }

        /* return fields */
        return dbf.toArray(new DBField[dbf.size()]);
        
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns a map of column names to index names
    *** @param tableName The table name
    *** @return A map of column names to index names
    *** @throws DBException if an error occured fetching the indexes
    **/
    public static DBTableIndexMap getActualTableIndexMap(String tableName)
        throws DBException
    {

        /* this is currently only supported by MySQL */
        DBProvider dbp = DBProvider.getProvider();
        String showCols = null;
        if (dbp.getID() == DB_MYSQL) {
            // MySQL: "SHOW INDEXES FROM <TableName>"
            showCols = "SHOW INDEXES FROM " + tableName;
        } else
        if (dbp.getID() == DB_SQLSERVER) {
            // SQLServer: "SELECT ???"
            return null;
       } else {
            // Derby:: "SELECT ???"
            return null;
        }

        /* extract fields */
        DBTableIndexMap indexMap = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = DBFactory.execute(showCols);
            rs = stmt.getResultSet();
            while (rs.next()) {
                String fldName = rs.getString("Column_name");
                String ndxName = rs.getString("Key_name");
                if ((fldName != null) && !fldName.equals("") && (ndxName != null) && !ndxName.equals("")) {
                    if (indexMap == null) { indexMap = new DBTableIndexMap(tableName); }
                    indexMap.addIndexColumn(ndxName, fldName);
                } else {
                    throw new DBException("Expected index column names not found");
                }
            }
        } catch (SQLException sqe) {
            throw new DBException("Unable to get indexes", sqe);
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
        }

        /* return index map */
        return indexMap;
        
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Get data type mask for type
    *** @param localType The nocal type name
    *** @return The data type mask for type
    **/
    public static int getDataTypeMask(String localType)
    {
        int ndx = DBProvider._getTypeIndex(localType,true);
        return DATATYPE_ARRAY[ndx];
    }

    /**
    *** Returns true if the specified data type is a Boolean type
    *** @param typeNdx The data type mask for type
    *** @return True if the specified data type is a Boolean type
    **/
    public static boolean isDataTypeBoolean(int typeNdx)
    {
        return ((typeNdx & DATATYPE_BOOLEAN) != 0);
    }
    public static boolean isDataTypeBoolean(String localType)
    {
        return isDataTypeBoolean(DBProvider.getDataTypeMask(localType));
    }

    /**
    *** Returns true if the specified data type is numeric
    *** @param typeNdx The data type mask for type
    *** @return True if the specified data type is numeric
    **/
    public static boolean isDataTypeNumeric(int typeNdx)
    {
        return ((typeNdx & DATATYPE_NUMERIC) != 0);
    }
    public static boolean isDataTypeNumeric(String localType)
    {
        return isDataTypeNumeric(DBProvider.getDataTypeMask(localType));
    }

    /**
    *** Returns true if the specified data type is a floating point type
    *** @param typeNdx The data type mask for type
    *** @return True if the specified data type is a floating point type
    **/
    public static boolean isDataTypeDecimal(int typeNdx)
    {
        return ((typeNdx & DATATYPE_DECIMAL) != 0);
    }
    public static boolean isDataTypeDecimal(String localType)
    {
        return isDataTypeDecimal(DBProvider.getDataTypeMask(localType));
    }

    /**
    *** Returns true if the specified data type is unsigned
    *** @param typeNdx The data type mask for type
    *** @return True if the specified data type is unsigned
    **/
    public static boolean isDataTypeUnsigned(int typeNdx)
    {
        return ((typeNdx & DATATYPE_UNSIGNED) != 0);
    }

    /**
    *** Returns true if the specified data type is unsigned
    *** @param localType The local data type name
    *** @return True if the specified data type is a Boolean type
    **/
    public static boolean isDataTypeUnsigned(String localType)
    {
        return isDataTypeUnsigned(DBProvider.getDataTypeMask(localType));
    }

    /** 
    *** Returns true if the specified data type is a String type
    *** @param typeNdx The data type mask for type
    *** @return True if the specified data type is a string
    **/
    public static boolean isDataTypeString(int typeNdx)
    {
        return ((typeNdx & DATATYPE_STRING) != 0);
    }

    /**
    *** Returns true if the specified data type is a String type
    *** @param localType The local data type name
    *** @return True if the specified data type is a String type
    **/
    public static boolean isDataTypeString(String localType)
    {
        return isDataTypeString(DBProvider.getDataTypeMask(localType));
    }

    /**
    *** Returns true if the specified data type is a Binary type
    *** @param typeNdx The data type mask for type mask for type
    *** @return True if the specified data type is a String type
    **/
    public static boolean isDataTypeBinary(int typeNdx)
    {
        return ((typeNdx & DATATYPE_BINARY) != 0);
    }

    /**
    *** Returns true if the specified data type is a Binary type
    *** @param localType The local data type name
    *** @return True if the specified data type is a String type
    **/
    public static boolean isDataTypeBinary(String localType)
    {
        return isDataTypeBinary(DBProvider.getDataTypeMask(localType));
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // create a database (needed for initialization only)

    /**
    *** Create the database specified by the Runtime config (needed for 
    *** initialization only)
    *** @param rootUser The database root user account name
    *** @param rootPass The database root user password
    *** @throws SQLException  If an SQL error occurs
    *** @throws DBException   If a database error occurs
    **/
    public static void createDatabase(String rootUser, String rootPass)
        throws DBException, SQLException
    {
        String dbName = DBProvider.getDBName();

        /* validate arguments */
        //if (StringTools.isBlank(rootUser)) {
        //    throw new DBException("Root user not specified");
        //} else
        if (StringTools.isBlank(dbName)) {
            throw new DBException("No database name specified");
        }

        /* connection */
        String dbUri = DBProvider.getDBUri(false);
        DBConnection dbc = null;

        /* create */
        switch (DBProvider.getProvider().getID()) {
            case DB_MYSQL    : {
                // MySQL: CREATE DATABASE <DataBase>
                dbc = DBConnection.getConnection(dbUri, rootUser, rootPass);
                dbc.executeUpdate("CREATE DATABASE " + dbName + ";");
                break;
            }
            case DB_POSTGRESQL: {
                // PostgreSQL: CREATE DATABASE <DataBase>
                dbc = DBConnection.getConnection(dbUri, rootUser, rootPass);
                dbc.executeUpdate("CREATE DATABASE " + dbName + ";");
                break;
            }
            case DB_DERBY: {
                // Derby: already created as part of the connection
                dbc = DBConnection.getConnection(dbUri, rootUser, rootPass);
                break;
            }
            case DB_SQLSERVER: {
                // SQLServer: CREATE DATABASE <DataBase>
                dbc = DBConnection.getConnection(dbUri, rootUser, rootPass);
                dbc.executeUpdate("CREATE DATABASE " + dbName + ";");
                break;
            }
        }

        /* log success */
        Print.logInfo("Database created: " + dbName);

        // To drop a database: (not implemented here)
        //    "DROP DATABASE <DataBase_Name>;"

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Translates table name for DB provider
    *** @param tableName The base table name
    *** @return The translated table name (table name prefixes added, etc.)
    **/
    public static String translateTableName(String tableName)
    {
        DBProvider dbp = DBProvider.getProvider();
        String tnp = dbp.getTableNamePrefix();
        return tnp + tableName;
    }

    // ------------------------------------------------------------------------

    /**
    *** Create the backing table for the specified DBFactory
    *** @param factory The DBFactory
    *** @throws SQLException  If an SQL error occurs
    *** @throws DBException   If a database error occurs
    **/
    public static void createTable(DBFactory factory)
        throws SQLException, DBException
    {
        String tableName    = factory.getTableName();
        DBField fields[]    = factory.getFields();
        DBField priKeys[]   = factory.getKeyFields();
        String priKeyType   = factory.getKeyType();
        DBProvider.createTable(tableName, fields, priKeys, priKeyType, factory.getAlternateIndexes());
    }

    /**
    *** Create the specified SQL table
    *** @param tableName The table name
    *** @param flds      The table columns
    *** @param priKeys   The table primary keys
    *** @param keyType   The key type
    *** @param altIndexes The table alternate indexes
    *** @throws SQLException  If an SQL error occurs
    *** @throws DBException   If a database error occurs
    **/
    public static void createTable(String tableName, DBField flds[], DBField priKeys[], String keyType, DBAlternateIndex altIndexes[])
        throws SQLException, DBException
    {
        DBProvider dbp = DBProvider.getProvider();
        int dbProvID = dbp.getID();
        StringBuffer sb = new StringBuffer();

        // MySQL:      CREATE TABLE <table> ( <fields...>, <KeyType> ( <Keys...> ) )
        // SQLServer:  CREATE TABLE <table> ( <fields...>, <KeyType> ( <Keys...> ) )
        // PostgreSQL: CREATE TABLE <table> ( <fields...>, <KeyType> ( <Keys...> ) )
        // Derby:      CREATE TABLE <table> ( <fields...>, <KeyType> ( <Keys...> ) )
        sb.append("CREATE TABLE ").append(tableName).append(" (");

        /* fields */
        DBField autoIncrField = null;
        for (int fx = 0; fx < flds.length; fx++) {
            if (fx > 0) { sb.append(","); }
            sb.append(flds[fx].getFieldDefinition());
            if (dbProvID == DB_MYSQL) {
                // TODO: need to qualify other DBProviders
                if (flds[fx].isAutoIncrement()) {
                    autoIncrField = flds[fx];
                    sb.append(" NOT NULL");
                    sb.append(" auto_increment");
                    Print.logInfo("Adding 'auto_increment' key: " + tableName+"."+autoIncrField.getName());
                } else
                if (flds[fx].isUTF8()) {
                    sb.append(" CHARACTER SET utf8");
                }
            }
        }

        /* primary keys */
        if ((priKeys != null) && (priKeys.length > 0)) {
            sb.append(", ");
            sb.append(keyType).append(" (");
            for (int pkx = 0; pkx < priKeys.length; pkx++) {
                if (pkx > 0) { sb.append(","); }
                sb.append(priKeys[pkx].getName());
            }
            sb.append(")");
        }

        /* table type */
        sb.append(")"); 
        String indexType = dbp.getTableIndexType();
        if (indexType != null) {
            sb.append(" ").append(indexType);
        }

        /* create table */
        DBFactory.executeUpdate(sb.toString());

        /* alternate index (created separately) */
        if ((altIndexes != null) && (altIndexes.length > 0)) {
            for (int i = 0; i < altIndexes.length; i++) {
                DBProvider.createAlternateIndex(tableName, altIndexes[i]);
            }
        }

        /* separate "autoIndex" alternate index */
        if (autoIncrField != null) {
            // the "auto_increment" field must be part of the PRIMARY KEY
            // we also add a separate index for the auto-increment field
            // so that the index value is unique over the entire table.
            String altIndexName = "autoIncrement";
            Print.logInfo("Adding 'auto_increment' altIndex: " + tableName + " " + altIndexName);
            DBField altNdxFlds[] = new DBField[] { autoIncrField };
            DBProvider.createAlternateIndex(tableName, altIndexName, altNdxFlds, true);
        }

    }

    /**
    *** Create an alternate Index
    *** @param tableName The table name
    *** @param altIndex The alternate index
    *** @throws SQLException  If an SQL error occurs
    *** @throws DBException   If a database error occurs
    **/
    public static void createAlternateIndex(String tableName, DBAlternateIndex altIndex)
        throws SQLException, DBException
    {
        String  indexName   = altIndex.getIndexName();
        DBField indexFlds[] = altIndex.getFields();
        boolean isUnique    = altIndex.isUnique();
        DBProvider.createAlternateIndex(tableName, indexName, indexFlds, isUnique);
    }
    
    /**
    *** Create an alternate Index
    *** @param tableName The table name
    *** @param indexName The alternate index
    *** @param altKeys The alternate keys
    *** @param isUnique If the index is unique ??? [CHECK]
    *** @throws SQLException  If an SQL error occurs
    *** @throws DBException   If a database error occurs
    **/
    private static void createAlternateIndex(String tableName, String indexName, DBField altKeys[], boolean isUnique)
        throws SQLException, DBException
    {
        if ((altKeys != null) && (altKeys.length > 0)) {

            /* default index name */
            if (StringTools.isBlank(indexName)) { 
                indexName = DEFAULT_ALT_INDEX_NAME; 
            }
            
            /* KeyType */
            DBFactory.KeyType keyType = isUnique? DBFactory.KeyType.UNIQUE_INDEX : DBFactory.KeyType.INDEX;

            /* assemble SQL statement */
            // MySQL:      CREATE [UNIQUE] INDEX altIndex ON <table> (<key>[,<key>[,...]])
            // Derby:      CREATE [UNIQUE] INDEX altIndex ON <table> (<key>[,<key>[,...]])
            // SQLServer:  CREATE [UNIQUE] INDEX altIndex ON <table> (<key>[,<key>[,...]])
            // PostgreSQL: CREATE [UNIQUE] INDEX altIndex ON <table> (<key>[,<key>[,...]])
            //Print.logDebug("Creating ALternate Index: " + indexName);
            StringBuffer altSB = new StringBuffer();
            altSB.append("CREATE ");
            altSB.append(DBFactory.getKeyTypeName(keyType)).append(" ");
            altSB.append(indexName).append(" ");
            altSB.append("ON ").append(tableName).append(" ");
            altSB.append("(");
            for (int akx = 0; akx < altKeys.length; akx++) {
                if (akx > 0) { altSB.append(","); }
                altSB.append(altKeys[akx].getName());
            }
            altSB.append(")");

            /* create index */
            DBFactory.executeUpdate(altSB.toString());

        } else {
            
            Print.logWarn("Ignoring alternate index with no specified fields: " + tableName + " " + indexName);
            
        }
    }

    /**
    *** Removes an alternate Index
    *** @param tableName The table name
    *** @param indexName The alternate index name
    *** @throws SQLException  If an SQL error occurs
    *** @throws DBException   If a database error occurs
    **/
    public static void removeAlternateIndex(String tableName, String indexName)
        throws SQLException, DBException
    {
        if (StringTools.isBlank(indexName)) { 
            indexName = DEFAULT_ALT_INDEX_NAME;
        }
        StringBuffer sb = new StringBuffer();
        // MySQL: ALTER TABLE <table> DROP INDEX altIndex
        sb.append("ALTER TABLE ").append(tableName).append(" ");
        sb.append("DROP ");
        sb.append(DBFactory.getKeyTypeName(DBFactory.KeyType.INDEX));
        sb.append(" ");
        sb.append(indexName);
        DBFactory.executeUpdate(sb.toString());
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Create primary key Index (see createTable)
    *** @param dbFact The DBFactory
    *** @throws SQLException  If an SQL error occurs
    *** @throws DBException   If a database error occurs
    **/
    public static void createPrimaryIndex(DBFactory dbFact)
        throws SQLException, DBException
    {
        // MySQL: ALTER TABLE <table> ADD PRIMARY KEY ( <key>,<key>,... )
        DBField priKeys[] = (dbFact != null)? dbFact.getKeyFields() : null;
        if ((priKeys != null) && (priKeys.length > 0)) {
            StringBuffer sb = new StringBuffer();
            sb.append("ALTER TABLE ").append(dbFact.getTableName());
            sb.append(" ADD ").append(dbFact.getKeyType());
            sb.append(" (");
            for (int pkx = 0; pkx < priKeys.length; pkx++) {
                if (pkx > 0) { sb.append(","); }
                sb.append(priKeys[pkx].getName());
            }
            sb.append(")");
            DBFactory.executeUpdate(sb.toString());
        }
    }

    /**
    *** Remove primary key Index
    *** @param tableName The table name
    *** @throws SQLException  If an SQL error occurs
    *** @throws DBException   If a database error occurs
    **/
    public static void removePrimaryIndex(String tableName)
        throws SQLException, DBException
    {
        // MySQL: ALTER TABLE <table> DROP PRIMARY KEY
        StringBuffer altSB = new StringBuffer();
        altSB.append("ALTER TABLE ").append(tableName).append(" ");
        altSB.append("DROP PRIMARY KEY");
        DBFactory.executeUpdate(altSB.toString());
    }

    // ------------------------------------------------------------------------

    /**
    *** Drop (delete) the specified table
    *** @param tableName The table name
    *** @throws SQLException  If an SQL error occurs
    *** @throws DBException   If a database error occurs
    **/
    public static void dropTable(String tableName)
        throws SQLException, DBException
    {
        String drop = null;
        switch (DBProvider.getProvider().getID()) {
            case DB_MYSQL:
                // MySQL: DROP TABLE IF EXISTS <TableName>
                // no error is generated if the table does not exist
                drop = "DROP TABLE IF EXISTS " + tableName;
                break;
            case DB_POSTGRESQL:
                // PostgreSQL: DROP TABLE <TableName>
                drop = "DROP TABLE " + tableName;
                break;
            case DB_DERBY:
                // Derby: DROP TABLE <TableName>
                drop = "DROP TABLE " + tableName;
                break;
            case DB_SQLSERVER:
                // SQLServer: DROP TABLE <TableName>
                drop = "DROP TABLE " + tableName;
                break;
        }
        
        /* execue 'drop' statement */
        if (drop != null) {
            try {
                DBFactory.executeUpdate(drop);
            } catch (SQLException sqe) {
                String sqlMsg = sqe.getMessage();
                int errCode = sqe.getErrorCode();
                if (errCode == DBFactory.SQLERR_TABLE_NONEXIST) { // MySQL: ?
                    // ignore
                } else
                if (errCode == DBFactory.MSQL_ERR_CANT_DROP_TABLE) { // SQLServer: :
                    // ignore
                } else
                if (sqlMsg.indexOf("does not exist") >= 0) {
                    // ignore
                } else {
                    // re-throw
                    throw sqe;
                }
            }
        } else {
            Print.logError("SQL 'DROP' not supported");
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Insert record into table
    *** @param rec The record to insert
    *** @throws SQLException  If an SQL error occurs
    *** @throws DBException   If a database error occurs
    **/
    public static boolean insertRecordIntoTable(DBRecord rec)
        throws SQLException, DBException
    {
        DBRecordKey recKey = rec.getRecordKey();
        StringBuffer sb = new StringBuffer();
        String tableName = recKey.getTableName();
        Map existingColumns = recKey.getFactory().getExistingColumnMap(false);
        DBField field[] = recKey.getFields();
        DBField autoIncrField = null;
        DBFieldValues fieldValues = recKey.getFieldValues();

        /* insert */
        // MySQL:      INSERT INTO <table> (<column>,<column>,...) VALUES (<value>,<value>,...)
        // SQLServer:  INSERT INTO <table> (<column>,<column>,...) VALUES (<value>,<value>,...)
        // Derby:      INSERT INTO <table> (<column>,<column>,...) VALUES (<value>,<value>,...)
        // PostgreSQL: INSERT INTO <table> (<column>,<column>,...) VALUES (<value>,<value>,...)
        boolean addedField = false;
        sb.append("INSERT INTO ").append(tableName);
        StringBuffer colSB = new StringBuffer();
        StringBuffer valSB = new StringBuffer();
        for (int i = 0; i < field.length; i++) {
            String fldName = field[i].getName();
            if (field[i].isAutoIncrement()) {
                // we skip 'auto_increment' fields on INSERT
                autoIncrField = field[i]; // "There must be only one"
                continue;
            }
            if ((existingColumns == null) || existingColumns.containsKey(fldName)) {
                // insert existing columns
                if (addedField) { 
                    colSB.append(","); 
                    valSB.append(","); 
                }
                Object fldVal = fieldValues.getFieldValue(fldName,true);
                String dbVal  = field[i].getQValue(fldVal);
                colSB.append(fldName);
                valSB.append(dbVal);
                addedField = true;
            } else {
                // ignore non-existant columns
                Print.logWarn("Field does not exist: " + tableName + "." + fldName + " [ignored]");
            }
        }
        sb.append(" (").append(colSB).append(")");
        sb.append(" VALUES (").append(valSB).append(")");
            
        /* MySQL also supports this version of "INSERT" */
        // MySQL: INSERT INTO <table> SET <column>=<value>, ...
        /*
        sb.append("INSERT INTO ").append(tableName).append(" SET ");
        for (int i = 0; i < field.length; i++) {
            String fldName = field[i].getName();
            if ((existingColumns == null) || existingColumns.containsKey(fldName)) {
                // insert existing columns
                if (addedField) { sb.append(", "); }
                Object fldVal = fieldValues.getFieldValue(fldName,true);
                String dbVal  = field[i].getQValue(fldVal);
                sb.append(fldName).append("=").append(dbVal);
                addedField = true;
            } else {
                // ignore non-existant columns
                Print.logWarn("Field does not exist: " + tableName + "." + fldName + " [ignored]");
            }
        }
        */
        
        /* execute */
        if (addedField) {
            // ResultSet rs = <Statement>.getGeneratedKeys();
            if (autoIncrField != null) {
                long autoIncrVal = DBRecord.executeUpdate(sb.toString(), true);
                if (autoIncrVal >= 0) {
                    Print.logDebug("Auto-Increment value: " + autoIncrVal);
                    fieldValues.setFieldValue(autoIncrField.getName(), autoIncrVal);
                }
            } else {
                DBRecord.executeUpdate(sb.toString());
            }
            return true;
        } else {
            Print.logInfo("Nothing was inserted!!! [" + tableName + "]");
            return false;
        }
        
    }
    
    /**
    *** Assemble an update statement (and execute) for the specified record and
    *** array of fields to update
    *** @param rec The record to update
    *** @param updFldArray The array of fields to update
    *** @throws SQLException  If an SQL error occurs
    *** @throws DBException   If a database error occurs
    **/
    public static boolean updateRecordInTable(DBRecord rec, String updFldArray[])
        throws SQLException, DBException
    {
       
        /* fields */
        // TODO: the process of determining which fields to update could be better optimized
        Set<String> updFldSet = null;
        if (updFldArray != null) {
            updFldSet = new HashSet<String>();
            for (int i = 0; i < updFldArray.length; i++) { 
                // ignore field entries which are null
                if (updFldArray[i] != null) {
                    updFldSet.add(updFldArray[i]); 
                }
            }
        }
        
        /* update fields */
        return DBProvider.updateRecordInTable(rec, updFldSet);

    }
    
    /**
    *** Assemble an update statement (and execute) for the specified record and
    *** set of fields to update
    *** @param rec The record to update
    *** @param updFldSet The set of fields to update
    *** @throws SQLException  If an SQL error occurs
    *** @throws DBException   If a database error occurs
    **/
    public static boolean updateRecordInTable(DBRecord rec, Set<String> updFldSet)
        throws SQLException, DBException
    {
        DBRecordKey recKey = rec.getRecordKey();
        StringBuffer sb    = new StringBuffer();
        String tableName   = recKey.getTableName();

        // MySQL:      UPDATE <table> SET <column>=<value>, ...
        // SQLServer:  UPDATE <table> SET <column>=<value>, ...
        // PostgreSQL: UPDATE <table> SET <column>=<value>, ...
        // Derby:      UPDATE <table> SET <column>=<value>, ...
        sb.append("UPDATE ").append(tableName);
        
        /* set */
        sb.append(" SET ");
        Map existingColumns = recKey.getFactory().getExistingColumnMap(false);
        DBFieldValues fieldValues = recKey.getFieldValues();
        DBField field[] = recKey.getFields();
        boolean addedField = false;
        for (int i = 0; i < field.length; i++) {
            String fldName = field[i].getName();
            if ((updFldSet == null) || updFldSet.contains(fldName)) {
                // we are either updating all fields, or a specific list of fields
                if (field[i].isPrimaryKey()) {
                    // ignore update to primary key
                    if (updFldSet != null) {
                        // field explicitly specified, display disallowed update warning
                        Print.logWarn("Primary key update not allowed: " + tableName + "." + fldName + " [ignored]");
                    } 
                    continue;
                }
                if (field[i].isAutoIncrement()) {
                    // we quietly skip 'auto_increment' fields
                    continue;
                }
                // field is not a primary key
                if ((existingColumns == null) || existingColumns.containsKey(fldName)) {
                    // update existing columns
                    if (addedField) { sb.append(", "); }
                    Object fldVal = fieldValues.getFieldValue(fldName,true);
                    String dbVal  = field[i].getQValue(fldVal);
                    sb.append(fldName).append("=").append(dbVal);
                    addedField = true;
                } else {
                    // ignore non-existant columns (ie. field defined in Java class, but not in MySQL table)
                    Print.logWarn("Field does not exist: " + tableName + "." + fldName + " [ignored]");
                }
            }
        }

        /* where */
        sb.append(recKey.getWhereClause(DBWhere.KEY_FULL));

        /* execute */
        if (addedField) {
            DBRecord.executeUpdate(sb.toString());
            return true;
        } else {
            Print.logInfo("Nothing was updated!!! [" + tableName + "]");
            return false;
        }
        
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // grant access to database (needed for initialization only)
    
    /** Execute an SQL statement granting access to the DB user (needed for 
    *** initialization only) 
    *** @param rootUser The root user accoount
    *** @param rootPass The root user password
    *** @throws SQLException  If an SQL error occurs
    *** @throws DBException   If a database error occurs
    **/
    public static void grantDBUser(String rootUser, String rootPass)
        throws DBException, SQLException
    {
        String grantUser = DBProvider.getDBUsername();
        String grantPass = DBProvider.getDBPassword();
        String dbName    = DBProvider.getDBName();

        /* validate arguments */
        if (StringTools.isBlank(rootUser)) {
            throw new DBException("Root user not specified");
        } else
        if (StringTools.isBlank(grantUser)) {
            throw new DBException("User not specified for GRANT access");
        } else
        if (grantUser.equalsIgnoreCase("root")) {
            throw new DBException("Refusing to change privileges for 'root'");
        } else
        if (StringTools.isBlank(dbName)) {
            throw new DBException("No database name specified");
        }

        /* connection */
        String dbUri = DBProvider.getDBUri(false);
        DBConnection dbc = DBConnection.getConnection(dbUri, rootUser, rootPass);

        /* grant */
        switch (DBProvider.getProvider().getID()) {
            case DB_MYSQL: {
                // MySQL: "GRANT ALL ON <dbname>.* TO <user>@localhost IDENTIFIED BY '<password>' WITH GRANT OPTION;"
                // MySQL: "GRANT ALL ON <dbname>.* TO <user>@"%" IDENTIFIED BY '<password>' WITH GRANT OPTION;"
                // MySQL: "FLUSH PRIVILEGES;"
                String grantS = "GRANT ALL ON " + dbName + ".* TO " + grantUser + "@";
                String grantE = " IDENTIFIED BY " + DBField.quote(grantPass) + " WITH GRANT OPTION;";
                dbc.executeUpdate(grantS + "localhost" + grantE);
                dbc.executeUpdate(grantS + DBField.quote("%") + grantE);
                dbc.executeUpdate("FLUSH PRIVILEGES;");
                break;
            }
            case DB_POSTGRESQL: {
                // PostgreSQL: ?
                String grantS = "GRANT ALL ON " + dbName + " TO " + grantUser;
                String grantE = " WITH GRANT OPTION;";
                dbc.executeUpdate(grantS + grantE);
                break;
            }
            case DB_DERBY: {
                // Derby: ?
                break;
            }
            case DB_SQLSERVER: {
                // SQLServer: CREATE LOGIN <user> WITH PASSWORD='<password>', DEFAULT_DATABASE=[<dbname>]
                try {
                    String createLogin = "CREATE LOGIN " + grantUser + " WITH PASSWORD='" + grantPass + "', DEFAULT_DATABASE=[" + dbName + "]";
                    dbc.executeUpdate(createLogin);
                } catch (SQLException sqe) {
                    int errCode = sqe.getErrorCode();
                    if (errCode == DBFactory.MSQL_ERR_LOGIN_EXISTS) {
                        Print.logWarn("This login already exists: " + grantUser);
                    } else {
                        throw sqe;
                    }
                }
                // SQLServer: USE <dbname>; CREATE USER <user> FOR LOGIN <user>
                try {
                    String createUser  = "USE " + dbName + "; CREATE USER " + grantUser + " FOR LOGIN " + grantUser;
                    dbc.executeUpdate(createUser);
                } catch (SQLException sqe) {
                    int errCode = sqe.getErrorCode();
                    if (errCode == DBFactory.MSQL_ERR_USER_EXISTS) {
                        Print.logWarn("This user already exists: " + grantUser);
                    } else {
                        throw sqe;
                    }
                }
                // SQLServer: USE <dbname>; GRANT SELECT,INSERT,UPDATE,DELETE,REFERENCES TO <user>
                try {
                    String perm = "SELECT,INSERT,UPDATE,DELETE,REFERENCES";
                    String grantAll = "USE " + dbName + "; GRANT " + perm + " TO " + grantUser;
                    dbc.executeUpdate(grantAll);
                } catch (SQLException sqe) {
                    throw sqe;
                }
                break;
            }
        }

        /* log success */
        Print.logInfo("Privileges granted to user: " + grantUser);

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // attempt to obtain the database server default cahracter set
    
    /**
    *** Gets the database provider default character encoding.  The returned
    *** String has the format "&gt;CharacterSet&lt;[&gt;Collation&lt;]"
    *** @return The database provider default character encoding.
    *** @throws SQLException  If an SQL error occurs
    *** @throws DBException   If a database error occurs
    **/
    public static String getDefaultCharacterSet()
        throws DBException, SQLException
    {
        String user   = DBProvider.getDBUsername();
        String pass   = DBProvider.getDBPassword();
        String dbName = DBProvider.getDBName();

        /* validate arguments */
        if (StringTools.isBlank(user)) {
            throw new DBException("User not specified");
        } else
        if (StringTools.isBlank(dbName)) {
            throw new DBException("No database name specified");
        }

        /* character set */
        StringBuffer charSet = new StringBuffer();
        Statement stmt = null;
        ResultSet rs   = null;
        switch (DBProvider.getProvider().getID()) {
            case DB_MYSQL: {
                // character set
                try {
                    stmt = DBFactory.execute("SHOW VARIABLES LIKE 'character_set_database'");
                    rs = stmt.getResultSet();
                    if (rs.next()) {
                        charSet.append(StringTools.trim(rs.getString("Value")));
                    }
                } finally {
                    if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
                    if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
                }
                // collation
                try {
                    stmt = DBFactory.execute("SHOW VARIABLES LIKE 'collation_database'");
                    rs = stmt.getResultSet();
                    if (rs.next()) {
                        charSet.append("[").append(StringTools.trim(rs.getString("Value"))).append("]");
                    }
                } finally {
                    if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
                    if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
                }
                break;
            }
        }
        
        /* return character set */
        return charSet.toString();

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Table lock/unlock
    // SQLServer: http://msdn2.microsoft.com/en-us/library/aa213026(SQL.80).aspx
    // Note: By default, table locking is disabled.

    private static Boolean          lockingEnabled = null;
    private static Stack<Throwable> lockLevel      = new Stack<Throwable>();
    private static int              lockSeq        = 0;
    private static int              unlockSeq      = 0;

    /**
    *** Returns true if table locking is enabled
    *** @return True if table locking is enabled
    **/
    public static boolean isTableLockingEnabled()
    {
        if (DBConnection.ALWAYS_NEW_CONNECTION) {
            // If DBConnection.ALWAYS_NEW_CONNECTION is 'true', then
            // return 'false'. Otherwise table deadlocks _will_ occur.
            Print.logDebug("Table locking is disabled (per DBConnection.ALWAYS_NEW_CONNECTION)");
            return false;
        } else {
            if (lockingEnabled == null) {
                lockingEnabled = new Boolean(RTConfig.getBoolean(RTKey.DB_TABLE_LOCKING));
                Print.logDebug("Table locking is " + (lockingEnabled.booleanValue()?"enabled":"disabled"));
            }
            return lockingEnabled.booleanValue();
        }
    }

    /**
    *** Returns the current recursive lock level
    *** @return The current recursive lock level
    **/
    public static int getLockLevel()
    {
        return DBProvider.lockLevel.size();
    }

    /**
    *** Lock specified tables for write/read
    *** @param writeTables The array of tables to lock for writing
    *** @param readTables The array of tables to lock for reading
    *** @return True if the query was successful
    *** @throws DBException   If a database error occurs
    **/
    public static boolean lockTables(String writeTables[], String readTables[])
        throws DBException
    {
        return DBProvider._lockTables(writeTables, readTables, false);
    }
    
    /**
    *** Lock specified tables for write/read
    *** @param writeTables The array of tables to lock for writing
    *** @param readTables The array of tables to lock for reading
    *** @param forceLock If the lock query should be attemped whether or not 
    ***        locking is enabled
    *** @return True if the query was successful
    *** @throws DBException   If a database error occurs
    **/
    private static boolean _lockTables(String writeTables[], String readTables[], boolean forceLock)
        throws DBException
    {

        /* keep track of how many times we attempt a lock (this is never decremented) */
        ++DBProvider.lockSeq;
        //Print.logDebug("Lock Sequence: " + DBProvider.lockSeq);
        
        /* nothing to lock */
        if ((writeTables == null) && (readTables == null)) {
            return false; 
        }

        /* supported by DBProvider? */
        DBProvider dbp = DBProvider.getProvider();
        int dbid = dbp.getID();
        if (dbid != DBProvider.DB_MYSQL) {
            return false;
        }
        
        /* check recursive locking */
        if (!DBProvider.lockLevel.empty()) {
            // You amy get this message when this section is acessed by multiple threads
            Print.logStackTrace("Locking: Lock-Level is not empty!");
            Print.logStackTrace("Location of prior table locking", DBProvider.lockLevel.peek());
        }
        
        try {
            
            // MySQL: LOCK TABLES ...
            StringBuffer sb = new StringBuffer();
            sb.append("LOCK TABLES ");
            
            /* lock writes */
            if (writeTables != null) {
                for (int w = 0; w < writeTables.length; w++) {
                    if (w > 0) { sb.append(","); }
                    // MySQL: [LOCK TABLES] ..., <table> WRITE
                    sb.append(writeTables[w]).append(" WRITE");
                }
            }
            
            /* lock reads */
            if (readTables != null) {
                if (writeTables != null) { sb.append(","); }
                for (int r = 0; r < readTables.length; r++) {
                    if (r > 0) { sb.append(","); }
                    // MySQL: [LOCK TABLES] ..., <table> READ
                    sb.append(readTables[r]).append(" READ");
                }
            }
            
            /* execute locking */
            if (forceLock || DBProvider.isTableLockingEnabled()) {
                Throwable t = new Throwable();
                t.fillInStackTrace();
                DBRecord.executeUpdate(sb.toString());
                DBProvider.lockLevel.push(t);
            }
            
            return true;
            
        } catch (SQLException sqe) {
            //this.setLastCaughtSQLException(sqe);
            Print.logSQLError("lock " + writeTables[0], sqe);
            return false;
        }
        
    }

    /**
    *** Lock specified tables for read
    *** @param tableName The table to lock for reading
    *** @param forceLock If the lock query should be attemped whether or not 
    ***        locking is enabled
    *** @return True if the query was successful
    *** @throws DBException   If a database error occurs
    **/
    // currently, this is only called by "<DBRecordKey>.exists()", with forceLock=true
    public static boolean lockTableForRead(String tableName, boolean forceLock)
        throws DBException
    {
        return DBProvider._lockTables(null, new String[] { tableName }, forceLock);
    }

    /**
    *** Unlock locked tables
    *** @return True if the query was successful
    *** @throws DBException   If a database error occurs
    **/
    public static boolean unlockTables()
        throws DBException
    {

        /* keep track of how many times we attempt an unlock (this is never decremented) */
        ++DBProvider.unlockSeq;
        //Print.logDebug("UnLock Sequence: " + DBProvider.unlockSeq);

        /* supported by DBProvider? */
        DBProvider dbp = DBProvider.getProvider();
        int dbid = dbp.getID();
        if (dbid != DBProvider.DB_MYSQL) {
            return false;
        }

        /* lock tables */
        try {
            if (DBProvider.isTableLockingEnabled() || !DBProvider.lockLevel.empty()) {
                // MySQL: UNLOCK TABLES
                String unlockSql = "UNLOCK TABLES";
                DBRecord.executeUpdate(unlockSql);
                if (!DBProvider.lockLevel.empty()) {
                    DBProvider.lockLevel.pop();
                } else {
                    Print.logStackTrace("Lock-Level stack is empty");
                }
            }
            if (!DBProvider.lockLevel.empty()) { 
                // TODO: this could get out-of-sync, if the above had previously thrown an exception
                Print.logStackTrace("Unlock: Lock-Level is not empty!"); 
            }
            return true;
        } catch (SQLException sqe) {
            //this.setLastCaughtSQLException(sqe);
            Print.logSQLError("unlock tables", sqe);
            return false;
        }
        
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private int          jdbcId           = 0;
    private String       jdbcName         = null;
    private String       jdbcDrivers[]    = null;
    private String       tableIndexType   = null;
    private String       dataTypes[]      = null;
    
    private long         jdbcFlags        = 0x0000L;
    private int          defaultPort      = 0;
    private String       tableNamePrefix  = null;

    /**
    *** Constructor
    *** @param name The JDBC name
    *** @param id The JDBC ID
    *** @param dftPort The default connection port
    *** @param drivers The JDBC drivers
    *** @param tblNamePfx The table name prefix
    *** @param tblNdxType The table idex type ("CREATE TABLE" prefix)
    *** @param flags The JDBC flags
    *** @param dataTypes The array of data type names
    *** @throws RuntimeException If <code>dataTypes</code> is not the correct 
    ***         length
    **/
    public DBProvider(
        String name, int id, int dftPort,
        String drivers[], 
        String tblNamePfx,
        String tblNdxType,
        long flags,             // FLAGS_LIMIT
        String dataTypes[])
    {
        super();
        
        /* vars */
        this.jdbcId          = id;
        this.jdbcName        = name;
        this.jdbcDrivers     = drivers;
        this.jdbcFlags       = flags;
        this.defaultPort     = dftPort;
        this.tableNamePrefix = (tblNamePfx != null)? tblNamePfx : "";
        this.tableIndexType  = (tblNdxType != null)? tblNdxType : "";
        
        /* data types */
        this.dataTypes       = dataTypes;
        if ((this.dataTypes == null) || (this.dataTypes.length != TYPE_ARRAY.length)) {
            // This is serious enough to stop everything right here!
            String msg = "Invalid number of data-types for DBProvider: " + this.jdbcName;
            Print.logStackTrace(msg);
            throw new RuntimeException(msg);
        }
        
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the JDBC ID
    *** @return The JDBC ID
    **/
    public int getID()
    {
        return this.jdbcId;
    }
    
    /**
    *** Gets the JDBC name
    *** @return The JDBC name
    **/
    public String getJDBCName()
    {
        return this.jdbcName;
    }
    
    /**
    *** Gets the JDBC drivers
    *** @return The JDBC drivers
    **/
    public String[] getDrivers()
    {
        return this.jdbcDrivers;
    }
    
    /**
    *** Gets the default connection port. Returns the port from RTConfig, if 
    *** defined; else returns the DBProvider's assigned default port
    *** @return The default connection port
    **/
    public int getDefaultPort()
    {
        if (RTConfig.hasProperty(RTKey.DB_PORT,false)) {
            return RTConfig.getInt(RTKey.DB_PORT);
        } else {
            return this.defaultPort;
        }
    }
    
    /**
    *** Gets the table name prefix
    *** @return The table name prefix
    **/
    public String getTableNamePrefix()
    {
        if (RTConfig.hasProperty(RTKey.DB_TABLE_NAME_PREFIX,false)) {
            return RTConfig.getString(RTKey.DB_TABLE_NAME_PREFIX);
        } else {
            return this.tableNamePrefix;
        }
    }

    /**
    *** Gets the table index type
    *** @return The table index type
    **/
    public String getTableIndexType()
    {
        return this.tableIndexType;
    }
    
    /**
    *** Gets an array of the SQL datatypes
    *** @return An array of the SQL datatypes
    **/
    public String[] getSQLTypes()
    {
        return this.dataTypes;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the JDBC flags
    *** @return The JDBC flags
    **/
    public long getFlags()
    {
        return this.jdbcFlags;
    }
    
    /**
    *** Returns true if the DBProvider supports limits
    *** @return True if the DBProvider supports limits
    **/
    public boolean supportsLimit()
    {
        return ((this.jdbcFlags & FLAGS_LIMIT) != 0);
    }
    
    /**
    *** Returns true if the DBProvider supports offsets
    *** @return True if the DBProvider supports offsets
    **/
    public boolean supportsOffset()
    {
        return ((this.jdbcFlags & FLAGS_OFFSET) != 0);
    }

    // ------------------------------------------------------------------------

}
