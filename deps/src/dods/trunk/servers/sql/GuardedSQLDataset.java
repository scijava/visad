// $Id: GuardedSQLDataset.java,v 1.3 2004-02-06 15:23:49 donm Exp $
/*
 * Copyright 1997-2000 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package dods.servers.sql;

import dods.servlet.GuardedDataset;
import dods.dap.DODSException;
import dods.dap.parser.ParseException;
import dods.dap.Server.ServerDDS;
import dods.dap.DAS;


/** Interface for datasets to be used by the <code>drds</code>.
 *  A GuardedDatasetis used by DODServlet (and it's children) to
 *  provide a thread safe place to store the DAS and DDS objects
 *  generated by a particular client request. And, it's used as a
 *  mechanism for caching those object for later release.
 *
 *  @author ndp
 *  @see dods.servlet.GuardedDataset
 *  @see dods.servers.sql.drds
 *
 */

public interface GuardedSQLDataset extends GuardedDataset {

    public sqlDDS getSQLDDS() throws DODSException, ParseException;

}
