/////////////////////////////////////////////////////////////////////////////
// Copyright (c) 1999, COAS, Oregon State University
// ALL RIGHTS RESERVED.   U.S. Government Sponsorship acknowledged.
//
// Please read the full copyright notice in the file COPYRIGHT
// in this directory.
//
// Author: Nathan Potter (ndp@coas.oregonstate.edu)
//
//                        College of Oceanic and Atmospheric Scieneces
//                        Oregon State University
//                        104 Ocean. Admin. Bldg.
//                        Corvallis, OR 97331-5503
//
/////////////////////////////////////////////////////////////////////////////


/* $Id: SqlBTFunction.java,v 1.3 2004-02-06 15:23:49 donm Exp $
*
*/


package dods.servers.sql;

import java.io.*;
import java.util.List;

import dods.dap.*;
import dods.dap.Server.*;


/** Represents a server side function that is SQL enabled. It differs from its
 * parent interface in that it has a method for expressing itself as a fragment of SQL
 * code that can be included in an SQL database query. Functions implementing
 * interface are not required to have an SQl representation. If no such
 * representation for the function exisits then the <code>getSQLCommand()</code>
 * should simply return a <code>null</code> and the function will be evaluated
 * in the regular manner by the DODS server after the data is recieved from the
 * DBMS.
 *
 * @author Nathan David Potter
 */


public interface SqlBTFunction extends BTFunction {

    /** This methods returns the SQL representation of this function.
     *  If this function cannot produce ansensible SQL representation then
     *  this method should return <code>null</code>.
     *  @parameter args A list of Clauses containing the arguments specified
     *  for this method in the DODS URL.
     *  @returns A String containing the SQL respresentation for this
     *  Server Side Function. If no such representation exisit,
     *  then it shall return <code>null</code>.
     */
    public String getSQLCommand(List args);

}



