
//
// RemoteReferenceLinkImpl.java
//

/*
VisAD system for interactive analysis and visualization of numerical
data.  Copyright (C) 1996 - 1998 Bill Hibbard, Curtis Rueden, Tom
Rink and Dave Glowacki.
 
This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 1, or (at your option)
any later version.
 
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License in file NOTICE for more details.
 
You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
*/

package visad;
 
import java.rmi.Remote;
import java.rmi.RemoteException;
 
import java.util.Enumeration;
import java.util.Vector;

import java.rmi.server.UnicastRemoteObject;
 
/**
   RemoteReferenceLinkImpl is the VisAD remote adapter for DataDisplayLink-s.
*/
public class RemoteReferenceLinkImpl extends UnicastRemoteObject
        implements RemoteReferenceLink
{
  DataDisplayLink link;

  /** create a Remote reference to a DataDisplayLink */
  public RemoteReferenceLinkImpl(DataDisplayLink ddl)
	throws RemoteException
  {
    link = ddl;
  }

  /** return the name of the DataRenderer used to render this reference */
  public String getRendererClassName()
	throws VisADException, RemoteException
  {
    return link.getRenderer().getClass().getName();
  }

  /** return a reference to the remote Data object */
  public RemoteDataReference getReference()
	throws VisADException, RemoteException
  {
    DataReferenceImpl di = (DataReferenceImpl )link.getDataReference();
    return new RemoteDataReferenceImpl(di);
  }

  /** return the list of ConstantMap-s which apply to this Data object */
  public Vector getConstantMapVector()
	throws VisADException, RemoteException
  {
    return link.getConstantMaps();
  }

  /** return a String representation of the referenced data */
  public String toString()
  {
    String s;
    try {
      s = link.getDataReference().getName() + " -> " + getRendererClassName();
    } catch (RemoteException e) {
      return null;
    } catch (VisADException e) {
      return null;
    }

    Vector v;
    try {
      v = getConstantMapVector();
      Enumeration e = v.elements();
      if (e.hasMoreElements()) {
	s = s + ":";
	while (e.hasMoreElements()) {
	  ConstantMap cm = (ConstantMap )e.nextElement();
	  s = s + " [" + cm.getConstant() + " -> " + cm.getDisplayScalar() + "]";
	}
      }
    } catch (RemoteException e) {
    } catch (VisADException e) {
    }

    return s;
  }
}
