/*
VisAD system for interactive analysis and visualization of numerical
data.  Copyright (C) 1996 - 2000 Bill Hibbard, Curtis Rueden, Tom
Rink, Dave Glowacki, Steve Emmerson, Tom Whittaker, Don Murray, and
Tommy Jasmin.

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Library General Public
License as published by the Free Software Foundation; either
version 2 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Library General Public License for more details.

You should have received a copy of the GNU Library General Public
License along with this library; if not, write to the Free
Software Foundation, Inc., 59 Temple Place - Suite 330, Boston,
MA 02111-1307, USA
*/

import java.io.IOException;

import java.rmi.RemoteException;

import visad.*;

import visad.java3d.DisplayImplJ3D;

public class Test68
  extends TestSkeleton
{
  private int port = 0;

  public Test68() { }

  public Test68(String[] args)
    throws RemoteException, VisADException
  {
    super(args);
  }

  int checkExtraKeyword(String testName, int argc, String[] args)
  {
    int d = 0;
    try {
      d = Integer.parseInt(args[argc]);
    }
    catch (NumberFormatException exc) { }
    if (d < 1 || d > 9999) {
      System.err.println(testName + ": Ignoring parameter \"" + args[argc] +
        "\": port must be between 1 and 9999");
    } else {
      port = d;
    }

    return 1;
  }

  DisplayImpl[] setupServerDisplays()
    throws RemoteException, VisADException
  {
    DisplayImpl[] dpys = new DisplayImpl[1];
    dpys[0] = new DisplayImplJ3D("display", DisplayImplJ3D.APPLETFRAME);
    return dpys;
  }

  void setupServerData(LocalDisplay[] dpys)
    throws RemoteException, VisADException
  {
    RealType[] types = {RealType.Latitude, RealType.Longitude};
    RealTupleType earth_location = new RealTupleType(types);
    RealType vis_radiance = new RealType("vis_radiance", null, null);
    RealType ir_radiance = new RealType("ir_radiance", null, null);
    RealType[] types2 = {vis_radiance, ir_radiance};
    RealTupleType radiance = new RealTupleType(types2);
    FunctionType image_tuple = new FunctionType(earth_location, radiance);

    int size = 32;
    FlatField imaget1 = FlatField.makeField(image_tuple, size, false);

    dpys[0].addMap(new ScalarMap(RealType.Latitude, Display.YAxis));
    dpys[0].addMap(new ScalarMap(RealType.Longitude, Display.XAxis));
    dpys[0].addMap(new ScalarMap(vis_radiance, Display.ZAxis));
    dpys[0].addMap(new ScalarMap(RealType.Latitude, Display.Red));
    dpys[0].addMap(new ScalarMap(RealType.Longitude, Display.Green));
    dpys[0].addMap(new ScalarMap(vis_radiance, Display.Blue));

    DataReferenceImpl ref_imaget1 = new DataReferenceImpl("ref_imaget1");
    ref_imaget1.setData(imaget1);
    dpys[0].addReference(ref_imaget1, null);

    // create the SocketSlaveDisplay for automatic handling of socket clients
    SocketSlaveDisplay serv = null;
    try {
      DisplayImpl display = (DisplayImpl) dpys[0];
      if (port > 0) {
        serv = new SocketSlaveDisplay(display, port);
      } else {
        serv = new SocketSlaveDisplay(display);
      }
    }
    catch (IOException exc) {
      System.err.println("Unable to create the SocketSlaveDisplay:");
      exc.printStackTrace();
    }
    if (serv != null) {
      System.out.println("SocketSlaveDisplay created.\n" +
        "To connect a client from within a web browser,\n" +
        "use the VisADApplet applet found in visad/examples.\n" +
        "Note that an applet cannot communicate with a server\n" +
        "via the network unless both applet and server\n" +
        "originate from the same machine.  In the future,\n" +
        "VisAD's SocketSlaveDisplay will support communication\n" +
        "through a proxy server.");
    }
  }

  public String toString() { return " port: SocketSlaveDisplay"; }

  public static void main(String[] args)
    throws RemoteException, VisADException
  {
    new Test68(args);
  }
}
