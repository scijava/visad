
//
// DisplayRendererJ3D.java
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

package visad.java3d;

import visad.*;

import java.awt.*;
import java.awt.event.*;

import javax.media.j3d.*;
import java.vecmath.*;

import java.util.*;
import java.rmi.*;


/**
   DisplayRendererJ3D is the VisAD abstract super-class for background and
   metadata rendering algorithms.  These complement depictions of Data
   objects created by DataRenderer objects.<P>

   DisplayRendererJ3D also manages the overall relation of DataRenderer
   output to Java3D and manages the scene graph.<P>

   It creates the binding between Control objects and scene graph
   Behavior objects for direct manipulation of Control objects.<P>

   DisplayRendererJ3D is not Serializable and should not be copied
   between JVMs.<P>
*/
public abstract class DisplayRendererJ3D extends DisplayRenderer {

  /** View associated with this VirtualUniverse */
  private View view;
  /** Canvas3D associated with this VirtualUniverse */
  private Canvas3D canvas;

  /** root BranchGroup of scene graph under Locale */
  private BranchGroup root = null;
  /** single TransformGroup between root and BranchGroups for all
      Data depictions */
  private TransformGroup trans = null;
  /** BranchGroup between trans and all direct manipulation
      Data depictions */
  private BranchGroup direct = null;
  /** Behavior for delayed removal of BranchGroups */
  RemoveBehavior remove = null;

  /** TransformGroup between trans and cursor */
  private TransformGroup cursor_trans = null;
  /** single Switch between cursor_trans and cursor */
  private Switch cursor_switch = null;
  /** children of cursor_switch */
  private BranchGroup cursor_on = null, cursor_off = null;
  /** on / off state of cursor */
  private boolean cursorOn = false;
  /** on / off state of direct manipulation location display */
  private boolean directOn = false;

  /** single Switch between trans and scales */
  private Switch scale_switch = null;
  /** children of scale_switch */
  private BranchGroup scale_on = null, scale_off = null;
  /** on / off state of cursor in GraphicsModeControl */

  /** distance threshhold for successful pick */
  private static final float PICK_THRESHHOLD = 0.05f;
  /** Vector of DirectManipulationRenderers */
  private Vector directs = new Vector();

  /** cursor location */
  private float cursorX, cursorY, cursorZ;
  /** normalized direction perpendicular to current cursor plane */
  private float line_x, line_y, line_z;
  /** start value for cursor */
  private float point_x, point_y, point_z;

  public DisplayRendererJ3D () {
    super();
  }

  public View getView() {
    return view;
  }

  public Canvas3D getCanvas() {
    return canvas;
  }

  public BranchGroup getRoot() {
    return root;
  }

  public TransformGroup getTrans() {
    return trans;
  }

  public BranchGroup getCursorOnBranch() {
    return cursor_on;
  }

  public void setCursorOn(boolean on) {
    cursorOn = on;
    if (on) {
      cursor_switch.setWhichChild(1); // set cursor on
      setCursorStringVector();
    }
    else {
      cursor_switch.setWhichChild(0); // set cursor off
      setCursorStringVector(null);
    }
  }

  public void setDirectOn(boolean on) {
    directOn = on;
    if (!on) {
      setCursorStringVector(null);
    }
  }

  public BranchGroup getDirect() {
    return direct;
  }

  /** create scene graph root, if none exists, with Transform
      and direct manipulation root;
      create special graphics (e.g., 3-D box, SkewT background),
      any lights, any user interface embedded in scene */
  public abstract BranchGroup createSceneGraph(View v, Canvas3D c);

  /** create scene graph root, if none exists, with Transform
      and direct manipulation root */
  public BranchGroup createBasicSceneGraph(View v, Canvas3D c) {
    if (root != null) return root;
    view = v;
    canvas = c;
    // Create the root of the branch graph
    root = new BranchGroup();
    // create the TransformGroup that is the parent of
    // Data object Group objects
    trans = new TransformGroup();
    trans.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
    trans.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
    trans.setCapability(Group.ALLOW_CHILDREN_READ);
    trans.setCapability(Group.ALLOW_CHILDREN_WRITE);
    trans.setCapability(Group.ALLOW_CHILDREN_EXTEND);
    root.addChild(trans);

    // initialize scale
    double scale = 0.5;
    ProjectionControl proj = getDisplay().getProjectionControl();
    Transform3D tstart = new Transform3D(proj.getMatrix());
    Transform3D t1 =
      MouseBehavior.make_matrix(0.0, 0.0, 0.0, scale, 0.0, 0.0, 0.0);
    t1.mul(tstart);
    double[] matrix = new double[16];
    t1.get(matrix);
    try {
      proj.setMatrix(matrix);
    }
    catch (VisADException e) {
    }
    catch (RemoteException e) {
    }
 
    // create the BranchGroup that is the parent of direct
    // manipulation Data object BranchGroup objects
    direct = new BranchGroup();
    direct.setCapability(Group.ALLOW_CHILDREN_READ);
    direct.setCapability(Group.ALLOW_CHILDREN_WRITE);
    direct.setCapability(Group.ALLOW_CHILDREN_EXTEND);
    direct.setCapability(Node.ENABLE_PICK_REPORTING);
    trans.addChild(direct);

    // create removeBehavior
    remove = new RemoveBehavior(this);
    BoundingSphere bounds =
      new BoundingSphere(new Point3d(0.0,0.0,0.0), 100.0);
    remove.setSchedulingBounds(bounds);
    trans.addChild(remove);

    cursor_trans = new TransformGroup();
    cursor_trans.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
    cursor_trans.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
    cursor_trans.setCapability(Group.ALLOW_CHILDREN_READ);
    cursor_trans.setCapability(Group.ALLOW_CHILDREN_WRITE);
    cursor_trans.setCapability(Group.ALLOW_CHILDREN_EXTEND);
    trans.addChild(cursor_trans);
    cursor_switch = new Switch();
    cursor_switch.setCapability(Switch.ALLOW_SWITCH_READ);
    cursor_switch.setCapability(Switch.ALLOW_SWITCH_WRITE);
    cursor_trans.addChild(cursor_switch);
    cursor_on = new BranchGroup();
    cursor_on.setCapability(Group.ALLOW_CHILDREN_READ);
    cursor_on.setCapability(Group.ALLOW_CHILDREN_WRITE);
    cursor_off = new BranchGroup();
    cursor_switch.addChild(cursor_off);
    cursor_switch.addChild(cursor_on);
    cursor_switch.setWhichChild(0); // initially off
    cursorOn = false;

    scale_switch = new Switch();
    scale_switch.setCapability(Switch.ALLOW_SWITCH_READ);
    scale_switch.setCapability(Switch.ALLOW_SWITCH_WRITE);
    trans.addChild(scale_switch);
    scale_on = new BranchGroup();
    scale_on.setCapability(Group.ALLOW_CHILDREN_READ);
    scale_on.setCapability(Group.ALLOW_CHILDREN_WRITE);
    scale_on.setCapability(Group.ALLOW_CHILDREN_EXTEND);
    scale_off = new BranchGroup();
    scale_switch.addChild(scale_off);
    scale_switch.addChild(scale_on);
    scale_switch.setWhichChild(0); // initially off

    return root;
  }

  public void addSceneGraphComponent(Group group) {
    trans.addChild(group);
  }

  public void addDirectManipulationSceneGraphComponent(Group group,
                         DirectManipulationRendererJ3D renderer) {
    direct.addChild(group);
    directs.addElement(renderer);

/* WLH 12 Dec 97 - this didn't help - but might in future
    if (last == null) {
      direct.addChild(branch);
    }
    else {
      int n = direct.numChildren();
      for (int i=0; i<n; i++) {
        if (last.equals(direct.getChild(i))) {
          direct.setChild(branch, i);
        }
      }
    }
*/
  }

  public void switchScene(DataRenderer renderer, int index) {
    remove.addRemove((RendererJ3D) renderer, index);
  }

  public void clearScene(DataRenderer renderer) {
    directs.removeElement(renderer);
  }

  public double[] getCursor() {
    double[] cursor = new double[3];
    cursor[0] = cursorX;
    cursor[1] = cursorY;
    cursor[2] = cursorZ;
    return cursor;
  }

  public void depth_cursor(PickRay ray) {
    Point3d origin = new Point3d();
    Vector3d direction = new Vector3d();
    ray.get(origin, direction);
    line_x = (float) direction.x;
    line_y = (float) direction.y;
    line_z = (float) direction.z;
    point_x = cursorX;
    point_y = cursorY;
    point_z = cursorZ;
  }

  public void drag_depth(float diff) {
    cursorX = point_x + diff * line_x;
    cursorY = point_y + diff * line_y;
    cursorZ = point_z + diff * line_z;
    setCursorLoc();
  }

  public void drag_cursor(PickRay ray, boolean first) {
    Point3d origin = new Point3d();
    Vector3d direction = new Vector3d();
    ray.get(origin, direction);
    float o_x = (float) origin.x;
    float o_y = (float) origin.y;
    float o_z = (float) origin.z;
    float d_x = (float) direction.x;
    float d_y = (float) direction.y;
    float d_z = (float) direction.z;
    if (first) {
      line_x = d_x;
      line_y = d_y;
      line_z = d_z;
    }
    float dot = (cursorX - o_x) * line_x +
                (cursorY - o_y) * line_y +
                (cursorZ - o_z) * line_z;
    float dot2 = d_x * line_x + d_y * line_y + d_z * line_z;
    if (dot2 == 0.0) return;
    dot = dot / dot2;
    // new cursor location is intersection
    cursorX = o_x + dot * d_x;
    cursorY = o_y + dot * d_y;
    cursorZ = o_z + dot * d_z;
    setCursorLoc();
  }

  private void setCursorLoc() {
    Transform3D t = new Transform3D();
    t.setTranslation(new Vector3f(cursorX, cursorY, cursorZ));
    cursor_trans.setTransform(t);
    if (cursorOn) {
      setCursorStringVector();
    }
  }

  /** whenever cursorOn or directOn is true, display
      Strings in cursorStringVector */
  public void drawCursorStringVector(Canvas3D canvas) {
    GraphicsContext3D graphics = canvas.getGraphicsContext3D();
    Appearance appearance = new Appearance();
    ColoringAttributes color = new ColoringAttributes();
    color.setColor(1.0f, 1.0f, 1.0f);
    appearance.setColoringAttributes(color);
    graphics.setAppearance(appearance);

    Point3d position1 = new Point3d();
    Point3d position2 = new Point3d();
    Point3d position3 = new Point3d();
    Point3d position4 = new Point3d();
    canvas.getPixelLocationInImagePlate(1, 10, position1);
    canvas.getPixelLocationInImagePlate(10, 10, position2);
    canvas.getPixelLocationInImagePlate(1, 1, position3);
    canvas.getPixelLocationInImagePlate(10, 1, position4);
    Transform3D t = new Transform3D();
    canvas.getImagePlateToVworld(t);
    t.transform(position1);
    t.transform(position2);
    t.transform(position3);

    // draw cursor strings in upper left corner of screen
    double[] start = {(double) position1.x,
                      (double) position1.y,
                      (double) position1.z};
    double[] base =  {(double) (position2.x - position1.x),
                      (double) (position2.y - position1.y),
                      (double) (position2.z - position1.z)};
    double[] up =    {(double) (position3.x - position1.x),
                      (double) (position3.y - position1.y),
                      (double) (position3.z - position1.z)};
    if (cursorOn || directOn) {
      Enumeration strings = getCursorStringVector().elements();
      while(strings.hasMoreElements()) {
        String string = (String) strings.nextElement();
        try {
          VisADLineArray array =
            PlotText.render_label(string, start, base, up, false);
          graphics.draw(((DisplayImplJ3D) getDisplay()).makeGeometry(array));
          start[1] -= 1.2 * up[1];
        }
        catch (VisADException e) {
        }
      }
    }

    // draw Exception strings in lower left corner of screen
    double[] startl = {(double) position3.x,
                       (double) -position3.y,
                       (double) position3.z};
    Vector rendererVector = getDisplay().getRendererVector();
    Enumeration renderers = rendererVector.elements();
    while (renderers.hasMoreElements()) {
      DataRenderer renderer = (DataRenderer) renderers.nextElement();
      Vector exceptionVector = renderer.getExceptionVector();
      Enumeration exceptions = exceptionVector.elements();
      while (exceptions.hasMoreElements()) {
        String string = (String) exceptions.nextElement();
        try {
          VisADLineArray array =
            PlotText.render_label(string, startl, base, up, false);
          graphics.draw(((DisplayImplJ3D) getDisplay()).makeGeometry(array));
          startl[1] += 1.2 * up[1];
        }
        catch (VisADException e) {
        }
      }
    }
    if (getWaitFlag()) {
      try {
        VisADLineArray array =
          PlotText.render_label("please wait . . .", startl, base, up, false);
        graphics.draw(((DisplayImplJ3D) getDisplay()).makeGeometry(array));
        startl[1] += 1.2 * up[1];
      }
      catch (VisADException e) {
      }
    }

    // draw Animation string in upper right corner of screen
    String animation_string = getAnimationString();
    if (animation_string != null) {
      int nchars = animation_string.length();
      if (nchars < 12) nchars = 12;
      double[] starta = {(double) (-position2.x - nchars *
                                        (position2.x - position1.x)),
                         (double) position2.y,
                         (double) position2.z};
      try {
        VisADLineArray array =
          PlotText.render_label(animation_string, starta, base, up, false);
        graphics.draw(((DisplayImplJ3D) getDisplay()).makeGeometry(array));
        starta[1] -= 1.2 * up[1];
      }
      catch (VisADException e) {
      }
    }
  }

  public DirectManipulationRendererJ3D findDirect(PickRay ray) {
    Point3d origin = new Point3d();
    Vector3d direction = new Vector3d();
    ray.get(origin, direction);
    DirectManipulationRendererJ3D renderer = null;
    float distance = Float.MAX_VALUE;
    Enumeration renderers = directs.elements();
    while (renderers.hasMoreElements()) {
      DirectManipulationRendererJ3D r =
        (DirectManipulationRendererJ3D) renderers.nextElement();
      float d = r.checkClose(origin, direction);
      if (d < distance) {
        distance = d;
        renderer = r;
      }
    }
    if (distance < PICK_THRESHHOLD) {
      return renderer;
    }
    else {
      return null;
    }
  }

  public boolean anyDirects() {
    return !directs.isEmpty();
  }

  public void setScaleOn(boolean on) {
    if (on) {
      scale_switch.setWhichChild(1); // on
    }
    else {
      scale_switch.setWhichChild(0); // off
    }
  }

  public void setScale(int axis, int axis_ordinal,
              VisADLineArray array) throws VisADException {
    // add array to scale_on
    // replace any existing at axis, axis_ordinal
    DisplayImplJ3D display = (DisplayImplJ3D) getDisplay();
    GeometryArray geometry = display.makeGeometry(array);
    GraphicsModeControl mode = display.getGraphicsModeControl();
    Appearance appearance =
      ShadowTypeJ3D.makeAppearance(mode, null, null, geometry);
    Shape3D shape = new Shape3D(geometry, appearance);
    BranchGroup group = new BranchGroup();
    group.setCapability(BranchGroup.ALLOW_DETACH);
    group.addChild(shape);
    // may only add BranchGroup to 'live' scale_on
    int dim = getMode2D() ? 2 : 3;
    synchronized (scale_on) {
      int n = scale_on.numChildren();
      int m = dim * axis_ordinal + axis;
      if (m >= n) {
        for (int i=n; i<=m; i++) {
          BranchGroup empty = new BranchGroup();
          empty.setCapability(BranchGroup.ALLOW_DETACH);
          scale_on.addChild(empty);
        }
      }
      scale_on.setChild(group, m);
    }
  }

  public void clearScales() {
    if (scale_on != null) {
      synchronized (scale_on) {
        int n = scale_on.numChildren();
        for (int i=n-1; i>=0; i--) {
          scale_on.removeChild(i);
        }
      }
    }
  }

  public void setTransform3D(Transform3D t) {
    trans.setTransform(t);
  }

  public Control makeControl(ScalarMap map) {
    DisplayRealType type = map.getDisplayScalar();
    DisplayImplJ3D display = (DisplayImplJ3D) getDisplay();
    if (type == null) return null;
    if (type.equals(Display.XAxis) ||
        type.equals(Display.YAxis) ||
        type.equals(Display.ZAxis) ||
        type.equals(Display.Latitude) ||
        type.equals(Display.Longitude) ||
        type.equals(Display.Radius)) {
      return (ProjectionControlJ3D) display.getProjectionControl();
    }
    else if (type.equals(Display.RGB) ||
             type.equals(Display.HSV) ||
             type.equals(Display.CMY)) {
      return new ColorControl(display);
    }
    else if (type.equals(Display.RGBA)) {
      return new ColorAlphaControl(display);
    }
    else if (type.equals(Display.Animation)) {
      // note only one RealType may be mapped to Animation
      // so control must be null
      Control control = display.getControl(AnimationControlJ3D.class);
      if (control != null) return control;
      else return new AnimationControlJ3D(display, map.getScalar());
    }
    else if (type.equals(Display.SelectValue)) {
      return new ValueControlJ3D(display);
    }
    else if (type.equals(Display.SelectRange)) {
      return new RangeControl(display);
    }
    else if (type.equals(Display.IsoContour)) {
      return new ContourControl(display);
    }
    else if (type.equals(Display.Flow1X) ||
             type.equals(Display.Flow1Y) ||
             type.equals(Display.Flow1Z)) {
      Control control = display.getControl(Flow1Control.class);
      if (control != null) return control;
      else return new Flow1Control(display);
    }
    else if (type.equals(Display.Flow2X) ||
             type.equals(Display.Flow2Y) ||
             type.equals(Display.Flow2Z)) {
      Control control = display.getControl(Flow2Control.class);
      if (control != null) return control;
      else return new Flow2Control(display);
    }
    else if (type.equals(Display.Shape)) {
      return new ShapeControl(display);
    }
    else {
      return null;
    }
  }

  public DataRenderer makeDefaultRenderer() {
    return new DefaultRendererJ3D();
  }

}

