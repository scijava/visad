/*
VisAD system for interactive analysis and visualization of numerical
data.  Copyright (C) 1996 - 1999 Bill Hibbard, Curtis Rueden, Tom
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

import java.awt.Component;

import java.rmi.RemoteException;

import visad.*;

import visad.java3d.DisplayImplJ3D;
import visad.util.LabeledColorWidget;

public class Test61
  extends UISkeleton
{
  public Test61() { }

  public Test61(String[] args)
    throws RemoteException, VisADException
  {
    super(args);
  }

  DisplayImpl[] setupServerDisplays()
    throws RemoteException, VisADException
  {
    DisplayImpl[] dpys = new DisplayImpl[1];
    dpys[0] = new DisplayImplJ3D("display", DisplayImplJ3D.APPLETFRAME);
    return dpys;
  }

  private static float[][] buildTable(float[][] table)
  {
    int length = table[0].length;
    for (int i=0; i<length; i++) {
      float a = ((float) i) / ((float) (table[3].length - 1));
      table[3][i] = a;
    }
    return table;
  }

  void setupServerData(LocalDisplay[] dpys)
    throws RemoteException, VisADException
  {
    RealType xr = new RealType("xr");
    RealType yr = new RealType("yr");
    RealType zr = new RealType("zr");
    RealType wr = new RealType("wr");
    RealType[] types3d = {xr, yr, zr};
    RealTupleType earth_location3d = new RealTupleType(types3d);
    FunctionType grid_tuple = new FunctionType(earth_location3d, wr);

    int NX = 32;
    int NY = 32;
    int NZ = 32;
    Integer3DSet set = new Integer3DSet(NX, NY, NZ);
    FlatField grid3d = new FlatField(grid_tuple, set);

    float[][] values = new float[1][NX * NY * NZ];
    int k = 0;
    for (int iz=0; iz<NZ; iz++) {
      // double z = Math.PI * (-1.0 + 2.0 * iz / (NZ - 1.0));
      double z = Math.PI * (-1.0 + 2.0 * iz * iz / ((NZ - 1.0)*(NZ - 1.0)) );
      for (int iy=0; iy<NY; iy++) {
        double y = -1.0 + 2.0 * iy / (NY - 1.0);
        for (int ix=0; ix<NX; ix++) {
          double x = -1.0 + 2.0 * ix / (NX - 1.0);
          double r = x - 0.5 * Math.cos(z);
          double s = y - 0.5 * Math.sin(z);
          double dist = Math.sqrt(r * r + s * s);
          values[0][k] = (float) ((dist < 0.1) ? 10.0 : 1.0 / dist);
          k++;
        }
      }
    }
    grid3d.setSamples(values);

    dpys[0].addMap(new ScalarMap(xr, Display.XAxis));
    dpys[0].addMap(new ScalarMap(yr, Display.YAxis));
    dpys[0].addMap(new ScalarMap(zr, Display.ZAxis));

    GraphicsModeControl mode = dpys[0].getGraphicsModeControl();
    mode.setScaleEnable(true);

    // new
    RealType duh = new RealType("duh");
    // Integer2DSet set2 = new Integer2DSet(2,2);
    Linear2DSet set2 = new Linear2DSet(0.0, (double) NX, 2,
                                       0.0, (double) NY, 2);
    RealType[] types2d = {xr, yr};
    RealTupleType domain2 = new RealTupleType(types2d);
    FunctionType ftype2 = new FunctionType(domain2, duh);
    float[][] v2 = {{1.0f,2.0f,3.0f,4.0f}};
    FlatField field2 = new FlatField(ftype2,set2);
    field2.setSamples(v2);
    dpys[0].addMap(new ScalarMap(duh, Display.RGB));

    ScalarMap map1color = new ScalarMap(wr, Display.RGBA);
    dpys[0].addMap(map1color);

    ColorAlphaControl control = (ColorAlphaControl) map1color.getControl();
    control.setTable(buildTable(control.getTable()));

    DataReferenceImpl ref_grid3d = new DataReferenceImpl("ref_grid3d");
    ref_grid3d.setData(grid3d);

    DataReferenceImpl ref2 = new DataReferenceImpl("ref2");
    ref2.setData(field2);

    dpys[0].addReference(ref2, null);

    dpys[0].addReference(ref_grid3d, null);
  }

  String getFrameTitle0() { return "VisAD Color Alpha Widget"; }

  Component getSpecialComponent(LocalDisplay[] dpys)
    throws RemoteException, VisADException
  {
    ScalarMap map1color = (ScalarMap )dpys[0].getMapVector().lastElement();
    return new LabeledColorWidget(map1color);
  }

  public String toString()
  {
    return ": volume rendering and ColorAlphaWidget";
  }

  public static void main(String[] args)
    throws RemoteException, VisADException
  {
    new Test61(args);
  }
}
