
//
// RemoteGraphicsModeControlImpl.java
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

import java.rmi.server.UnicastRemoteObject;

/** RemoteGraphicsModeControlImpl is the VisAD adapter for a Display's
    rendering mode settings.
*/
public class RemoteGraphicsModeControlImpl extends RemoteControlImpl
        implements RemoteGraphicsModeControl
{
  /** create a Remote reference to a GraphicsModeControl */
  public RemoteGraphicsModeControlImpl(GraphicsModeControl gmc)
	throws VisADException, RemoteException
  {
    super(gmc);
  }

  /** return 'true' if the display is being rendered as a 2D object */
  public boolean getMode2D()
	throws VisADException, RemoteException
  {
    return ((GraphicsModeControl )Control).getMode2D();
  }

  /** get the width of line rendering */
  public float getLineWidth()
	throws VisADException, RemoteException
  {
    return ((GraphicsModeControl )Control).getLineWidth();
  }

  /** get the size for point rendering */
  public float getPointSize()
	throws VisADException, RemoteException
  {
    return ((GraphicsModeControl )Control).getPointSize();
  }

  /** if true, this will cause some rendering as points
      rather than lines or surfaces */
  public boolean getPointMode()
	throws VisADException, RemoteException
  {
    return ((GraphicsModeControl )Control).getPointMode();
  }

  /** if true, this will enable use of texture mapping, where appropriate */
  public boolean getTextureEnable()
	throws VisADException, RemoteException
  {
    return ((GraphicsModeControl )Control).getTextureEnable();
  }

  /** if true, this will enable numerical scales along display spatial axes */
  public boolean getScaleEnable()
	throws VisADException, RemoteException
  {
    return ((GraphicsModeControl )Control).getScaleEnable();
  }

  /** get graphics-API-specific transparency mode
      (e.g., SCREEN_DOOR, BLENDED) */
  public int getTransparencyMode()
	throws VisADException, RemoteException
  {
    return ((GraphicsModeControl )Control).getTransparencyMode();
  }

  /** get graphics-API-specific projection policy
      (e.g., PARALLEL_PROJECTION, PERSPECTIVE_PROJECTION) */
  public int getProjectionPolicy()
	throws VisADException, RemoteException
  {
    return ((GraphicsModeControl )Control).getProjectionPolicy();
  }
}
