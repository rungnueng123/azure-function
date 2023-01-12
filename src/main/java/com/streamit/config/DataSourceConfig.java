package com.streamit.config;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;

public class DataSourceConfig {
	
	public DataSource getDataSource() {
        SQLServerDataSource ds = new SQLServerDataSource();
        ds.setURL("jdbc:sqlserver://hfympcdb.database.windows.net:1433;database=mpcdb");
        ds.setUser("streammpc");
        ds.setPassword("P@ssw0rd");
//        ds.setURL("jdbc:sqlserver://hfympcdb.database.windows.net:1433;database=mpcdb;encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30;Authentication=ActiveDirectoryMSI");
//        ds.setMSIClientId("e5024f45-a51c-4b40-9ffe-6afeef17024c");

        //Connection conn = ds.getConnection();
        
        return ds;
    }

    public Connection execute() {
    	Connection conn = null;
	    try {
	    	conn = this.getDataSource().getConnection();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	    return conn;
    }

}
