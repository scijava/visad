//
// SliceManager.java
//

/*
VisAD system for interactive analysis and visualization of numerical
data.  Copyright (C) 1996 - 2002 Bill Hibbard, Curtis Rueden, Tom
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

package visad.bio;

import java.awt.Color;
import java.io.*;
import java.rmi.RemoteException;
import javax.swing.JOptionPane;
import visad.*;
import visad.util.*;

/** SliceManager is the class encapsulating VisBio's slice logic. */
public class SliceManager
  implements ControlListener, DisplayListener, PlaneListener
{

  // -- CONSTANTS --

  /** Header for slice manager data in state file. */
  private static final String SM_HEADER = "# Slice manager";


  // -- DATA TYPE CONSTANTS --

  /** RealType for mapping measurements to Z axis. */
  static final RealType Z_TYPE = RealType.getRealType("bio_line_z");

  /** RealType for mapping timestep values to animation. */
  static final RealType TIME_TYPE = RealType.getRealType("bio_time");

  /** RealType for mapping slice values to select value and Z axis. */
  static final RealType SLICE_TYPE = RealType.getRealType("bio_slice");

  /** RealType for mapping to Red. */
  static final RealType RED_TYPE = RealType.getRealType("bio_red");

  /** RealType for mapping to Green. */
  static final RealType GREEN_TYPE = RealType.getRealType("bio_green");

  /** RealType for mapping to Blue. */
  static final RealType BLUE_TYPE = RealType.getRealType("bio_blue");


  // -- DATA TYPE INFORMATION --

  /** Domain type for 2-D image stack data. */
  RealTupleType domain2;

  /** Domain type for 3-D image stack data. */
  RealTupleType domain3;

  /** Tuple type for fields with (r, g, b) range. */
  RealTupleType colorRange;

  /** List of domain type components for image stack data. */
  RealType[] dtypes;

  /** List of range type components for image stack data. */
  RealType[] rtypes;

  /** Domain mappings for 2-D slice display. */
  ScalarMap x_map2, y_map2;

  /** X, Y and Z bounds for the data. */
  float min_x, max_x, min_y, max_y, min_z, max_z;

  /** X and Y resolution of image data. */
  int res_x, res_y;

  /** X and Y resolution for arbitrary slices. */
  private int sliceRes_x, sliceRes_y;

  /** X, Y and Z resolution for volume rendering. */
  private int volumeRes;


  // -- SLICE-RELATED FIELDS --

  /** Animation control associated with 2-D animation mapping. */
  AnimationControl anim_control2;

  /** Animation control associated with 3-D animation mapping. */
  AnimationControl anim_control3;

  /** Animation controls associated with preview animation mappings. */
  AnimationControl anim_control_prev, anim_control_next;

  /** Value control associated with 2-D select value mapping. */
  ValueControl value_control2;

  /** Plane selection object. */
  ArbitrarySlice arb;

  /** Image stack alignment plane. */
  AlignmentPlane align;

  /** Is arbitrary plane selection on? */
  private boolean planeSelect;

  /** Should arbitrary plane be updated every time it changes? */
  private boolean continuous;

  /** Has arbitrary plane moved since last right mouse button press? */
  private boolean planeChanged;

  /** Is volume rendering display mode on? */
  private boolean volume;


  // -- DISPLAY MAPPING INFORMATION --

  /** High-resolution field for current timestep. */
  private FieldImpl field;

  /** Low-resolution field for all timesteps. */
  private FieldImpl lowresField;

  /** Collapsed field at current timestep, for arbitrary slicing. */
  private FlatField sliceField;

  /** Collapsed, squarized field at current timestep, for volume rendering. */
  private FlatField volumeField;

  /** List of range component mappings for 2-D display. */
  private ScalarMap[] rmaps2;

  /** List of range component mappings for 3-D display. */
  private ScalarMap[] rmaps3;

  /** List of range component mappings for preview displays. */
  private ScalarMap[][] rmapsP;

  /** List of color widgets associated with mappings to RGB and RGBA. */
  private LabeledColorWidget[] widgets;


  // -- DATA REFERENCES --

  /** Reference for image stack data for 2-D display. */
  private DataReferenceImpl ref2;

  /** Reference for image stack data for 3-D display. */
  private DataReferenceImpl ref3;

  /** Reference for data for previous display. */
  private DataReferenceImpl ref_prev;

  /** Reference for data for next display. */
  private DataReferenceImpl ref_next;

  /** Reference for low-resolution image timestack data for 2-D display. */
  private DataReferenceImpl lowresRef2;

  /** Reference for low-resolution image timestack data for 3-D display. */
  private DataReferenceImpl lowresRef3;

  /** Reference for arbitrary plane data. */
  private DataReferenceImpl planeRef;

  /** Data renderer for 2-D image stack data. */
  private DataRenderer renderer2;

  /** Data renderer for 3-D image stack data. */
  private DataRenderer renderer3;

  /** Data renderer for arbitrary plane data in 2-D. */
  private DataRenderer planeRenderer2;

  /** Data renderer for low-resolution image timestack data in 2-D. */
  private DataRenderer lowresRenderer2;

  /** Data renderer for low-resolution image timestack data in 3-D. */
  private DataRenderer lowresRenderer3;


  // -- THUMBNAIL-RELATED FIELDS --

  /** Resolution of thumbnails. */
  private int[] thumbSize;

  /** Should low-resolution slices be displayed? */
  private boolean lowres;

  /** Should low-resolution thumbnails be created? */
  private boolean doThumbs;

  /** Does current data have low-resolution thumbnails? */
  private boolean hasThumbs;

  /** Automatically switch resolution when certain events occur? */
  private boolean autoSwitch;



  // -- OTHER FIELDS --

  /** VisBio frame. */
  private VisBio bio;

  /** List of files containing current data series. */
  private File[] files;

  /** Should each file be interpreted as a slice rather than a timestep? */
  private boolean filesAsSlices;

  /** Number of timesteps in data series. */
  private int timesteps;

  /** Number of slices in data series. */
  private int slices;

  /** Current index in data series. */
  private int index;

  /** Current slice in data series. */
  private int slice;

  /** Timestep of data at last resolution switch. */
  private int mode_index;

  /** Slice number of data at last resolution switch. */
  private int mode_slice;


  // -- CONSTRUCTORS --

  /** Constructs a slice manager. */
  public SliceManager(VisBio biovis) throws VisADException, RemoteException {
    bio = biovis;
    lowres = false;
    doThumbs = true;
    autoSwitch = true;
    planeSelect = false;
    continuous = false;
    planeChanged = false;
    colorRange = new RealTupleType(
      new RealType[] {RED_TYPE, GREEN_TYPE, BLUE_TYPE});

    // data references
    ref2 = new DataReferenceImpl("bio_ref2");
    ref3 = new DataReferenceImpl("bio_ref3");
    lowresRef2 = new DataReferenceImpl("bio_lowresRef2");
    lowresRef3 = new DataReferenceImpl("bio_lowresRef3");
    ref_prev = new DataReferenceImpl("bio_ref_prev");
    ref_next = new DataReferenceImpl("bio_ref_next");
    planeRef = new DataReferenceImpl("bio_planeRef");
  }


  // -- API METHODS --

  /** Gets the currently displayed timestep index. */
  public int getIndex() { return index; }

  /** Gets the currently displayed image slice. */
  public int getSlice() { return slice; }

  /** Gets the number of timestep indices. */
  public int getNumberOfIndices() { return timesteps; }

  /** Gets the number of image slices. */
  public int getNumberOfSlices() { return slices; }

  /** Gets whether the currently loaded data has low-resolution thumbnails. */
  public boolean hasThumbnails() { return hasThumbs; }

  /** Sets the display detail (low-resolution or full resolution). */
  public void setMode(boolean lowres) {
    setMode(lowres, true);
  }

  /** Sets the currently displayed timestep index. */
  public void setIndex(int index) {
    if (this.index == index || bio.horiz.isBusy() && !lowres && !autoSwitch) {
      return;
    }
    boolean doRefresh = true;
    if (autoSwitch && !lowres) {
      setMode(true, false);
      doRefresh = false;
    }
    this.index = index;
    if (autoSwitch && lowres && index == mode_index) {
      setMode(false, false);
      doRefresh = false;
    }
    if (doRefresh) refresh(false, true);
    else {
      if (volume) doVolumeMode();
      updateStuff();
    }
    align.setIndex(index);
  }

  /** Sets the currently displayed image slice. */
  public void setSlice(int slice) {
    if (this.slice == slice) return;
    this.slice = slice;
    refresh(true, false);
  }

  /** Sets whether to auto-switch resolutions when certain events occur. */
  public void setAutoSwitch(boolean value) { autoSwitch = value; }

  /** Sets whether to create low-resolution thumbnails of the data. */
  public void setThumbnails(boolean thumbnails, int xres, int yres) {
    doThumbs = thumbnails;
    thumbSize = new int[] {xres, yres};
  }

  /** Sets whether to do arbitrary plane selection. */
  public void setPlaneSelect(boolean value) {
    if (bio.display3 == null) return;
    planeSelect = value;
    arb.toggle(value);
    planeRenderer2.toggle(value);
    renderer2.toggle(!value && !lowres);
    if (hasThumbs) lowresRenderer2.toggle(!value && lowres);
    if (value && planeRef.getData() == null) updateSlice();
  }

  /** Sets whether arbitrary plane is continuously updated. */
  public void setPlaneContinuous(boolean value) { continuous = value; }

  /** Sets whether 3-D display should use image stack or volume rendering. */
  public void setVolumeRender(boolean volume) {
    if (bio.display3 == null) return;
    if (this.volume == volume) return;
    this.volume = volume;
    try {
      if (volume) updateVolumeField();
      else {
        if (lowres) lowresRef3.setData(lowresField);
        else ref3.setData(field);
      }
    }
    catch (VisADException exc) { exc.printStackTrace(); }
    catch (RemoteException exc) { exc.printStackTrace(); }
    bio.toolColor.doAlpha(!volume);
    syncColors();
  }

  /** Sets the resolution at which volume rendering occurs. */
  public void setVolumeResolution(int res) {
    if (volumeRes == res) return;
    volumeRes = res;
    if (volume) updateVolumeField();
  }

  /** Links the data series to the given list of files. */
  public void setSeries(File[] files) { setSeries(files, false); }
  
  /**
   * Links the data series to the given list of files, treating
   * each file as a slice (instead of a timestep) if specified.
   */
  public void setSeries(File[] files, boolean filesAsSlices) {
    this.files = files;
    this.filesAsSlices = filesAsSlices;
    if (filesAsSlices) doThumbs = false;
    index = 0;
    boolean success = false;
    try {
      setFile(true);
      success = true;
    }
    catch (VisADException exc) { }
    catch (RemoteException exc) { }
    if (success) {
      bio.horiz.updateSlider(timesteps);
      bio.vert.updateSlider(slices);
      bio.state.saveState();
    }
  }

  /** Returns the current data series file list. */
  public File[] getSeries() { return files; }

  /** Returns whether each file is a single slice of one timestep. */
  public boolean getFilesAsSlices() { return filesAsSlices; }

  /** Returns the field data currently in memory. */
  public FieldImpl getField() { return field; }

  /** Gets whether arbitrary plane selection is in effect. */
  public boolean getPlaneSelect() { return planeSelect; }


  // -- INTERNAL API METHODS --

  /** ControlListener method used for programmatically updating GUI. */
  public void controlChanged(ControlEvent e) {
    Control c = e.getControl();
    if (c == anim_control2) {
      int index = anim_control2.getCurrent();
      if (this.index != index) bio.horiz.setValue(index + 1);
    }
    else if (c == value_control2) {
      // update sliders to match current timestep and slice values
      int slice = (int) value_control2.getValue();
      if (this.slice != slice) bio.vert.setValue(slice + 1);
    }
    else syncColors();
  }

  /** DisplayListener method used for mouse activity in 3-D display. */
  public void displayChanged(DisplayEvent e) {
    if (e.getId() != DisplayEvent.MOUSE_RELEASED_RIGHT) return;
    bio.state.saveState();
    if (planeSelect && planeChanged && !continuous) updateSlice();
    planeChanged = false;
  }

  /** PlaneListener method used for detecting PlaneSelector changes. */
  public void planeChanged() {
    planeChanged = true;
    if (continuous) updateSlice();
  }

  /** Dumps current dataset and takes out the garbage, to conserve memory. */
  void purgeData(boolean refs) throws VisADException, RemoteException {
    if (refs) {
      FunctionType ftype = (FunctionType) field.getType();
      field = new FieldImpl(ftype, field.getDomainSet());
      ref2.setData(field);
      ref3.setData(field);
    }
    else field = null;
    System.gc();
  }

  /** Sets the current file to match the current index. */
  void setFile(boolean initialize)
    throws VisADException, RemoteException
  {
    bio.setWaitCursor(true);
    try {
      if (initialize) init(files, 0);
      else if (!filesAsSlices) {
        purgeData(true);

        // load new data
        field = BioUtil.loadData(files[index], true);
        sliceField = volumeField = null;
        if (field != null) {
          ref2.setData(field);
          ref3.setData(field);
        }
        else {
          bio.setWaitCursor(false);
          JOptionPane.showMessageDialog(bio,
            files[index].getName() + " does not contain an image stack",
            "Cannot load file", JOptionPane.ERROR_MESSAGE);
          return;
        }
      }
    }
    finally {
      bio.setWaitCursor(false);
    }
  }

  /** Sets the color range to match the specified configuration. */
  void setColorRange(boolean dynamic, double lo, double hi) {
    if (rtypes == null) return;
    try {
      for (int i=0; i<rtypes.length; i++) {
        if (dynamic) rmaps2[i].resetAutoScale();
        else rmaps2[i].setRange(lo, hi);
        if (bio.display3 != null) {
          if (dynamic) rmaps3[i].resetAutoScale();
          else rmaps3[i].setRange(lo, hi);
        }
      }
    }
    catch (VisADException exc) { exc.printStackTrace(); }
    catch (RemoteException exc) { exc.printStackTrace(); }
  }

  /** Sets the color controls to match the current widget color tables. */
  void syncColors() {
    int colorMode = bio.display2.getGraphicsModeControl().getColorMode();
    int cm = bio.composite ? GraphicsModeControl.AVERAGE_COLOR_MODE :
      GraphicsModeControl.SUM_COLOR_MODE;
    if (colorMode != cm) {
      try {
        bio.display2.getGraphicsModeControl().setColorMode(cm);
        if (bio.display3 != null) {
          bio.display3.getGraphicsModeControl().setColorMode(cm);
        }
      }
      catch (VisADException exc) { exc.printStackTrace(); }
      catch (RemoteException exc) { exc.printStackTrace(); }
    }
    if (widgets == null) return;
    for (int i=0; i<widgets.length; i++) {
      float[][] table = widgets[i].getTable();
      try {
        BaseColorControl cc2 = (BaseColorControl) rmaps2[i].getControl();
        float[][] t2 = BioUtil.adjustColorTable(table, null, false);
        if (!BioUtil.tablesEqual(t2, cc2.getTable())) cc2.setTable(t2);
        if (bio.display3 != null) {
          BaseColorControl cc3 = (BaseColorControl) rmaps3[i].getControl();
          float[][] t3 = cc3.getTable();
          t3 = BioUtil.adjustColorTable(table, t3[3], true);
          if (!BioUtil.tablesEqual(t3, cc3.getTable())) cc3.setTable(t3);
        }
        if (hasThumbs && bio.previous != null && bio.next != null) {
          if (t2 == null) t2 = BioUtil.adjustColorTable(table, null, false);
          for (int j=0; j<rmapsP.length; j++) {
            BaseColorControl ccP = (BaseColorControl)
              rmapsP[j][i].getControl();
            if (!BioUtil.tablesEqual(t2, ccP.getTable())) ccP.setTable(t2);
          }
        }
      }
      catch (VisADException exc) { exc.printStackTrace(); }
      catch (RemoteException exc) { exc.printStackTrace(); }
    }
  }

  /** Ensures slices are set up properly for animation. */
  void startAnimation() {
    // switch to low resolution
    if (!lowres) {
      lowres = true;
      bio.toolView.setMode(true);
      setMode(true);
    }
  }

  /** Sets the arbitrary slice resolution. */
  void setSliceRange(int x, int y) {
    if (sliceRes_x == x && sliceRes_y == y) return;
    sliceRes_x = x;
    sliceRes_y = y;
    bio.state.saveState();
    if (planeSelect) updateSlice();
    else sliceField = null;
  }

  /** Gets the color widgets for color mappings to RGB and RGBA. */
  LabeledColorWidget[] getColorWidgets() { return widgets; }

  /** Writes the current program state to the given output stream. */
  void saveState(PrintWriter fout) throws IOException, VisADException {
    fout.println(SM_HEADER);
    fout.println(files.length);
    for (int i=0; i<files.length; i++) fout.println(files[i].getPath());
    fout.println(filesAsSlices);
    fout.println(hasThumbs);
    fout.println(thumbSize[0]);
    fout.println(thumbSize[1]);
    fout.println(sliceRes_x);
    fout.println(sliceRes_y);
    if (arb != null) arb.saveState(fout);
    if (align != null) align.saveState(fout);
  }

  /** Restores the current program state from the given input stream. */
  void restoreState(BufferedReader fin) throws IOException, VisADException {
    if (!fin.readLine().trim().equals(SM_HEADER)) {
      throw new VisADException("SliceManager: incorrect state format");
    }
    int len = Integer.parseInt(fin.readLine().trim());
    File[] files = new File[len];
    for (int i=0; i<len; i++) files[i] = new File(fin.readLine().trim());
    boolean fas = fin.readLine().trim().equals("true");
    boolean thumbs = fin.readLine().trim().equals("true");
    int thumbX = Integer.parseInt(fin.readLine().trim());
    int thumbY = Integer.parseInt(fin.readLine().trim());
    int sliceX = Integer.parseInt(fin.readLine().trim());
    int sliceY = Integer.parseInt(fin.readLine().trim());
    boolean equal = doThumbs == thumbs && thumbSize[0] == thumbX &&
      thumbSize[1] == thumbY && len == timesteps && filesAsSlices == fas;
    if (equal) {
      for (int i=0; i<len; i++) {
        String path = files[i].getAbsolutePath();
        if (!path.equals(this.files[i].getAbsolutePath())) {
          equal = false;
          break;
        }
      }
    }
    if (!equal) {
      // dataset is different; load it
      setThumbnails(thumbs, thumbX, thumbY);
      setSeries(files, fas);
    }
    if (arb != null) arb.restoreState(fin);
    if (align != null) align.restoreState(fin);
  }


  // -- HELPER METHODS --

  /**
   * Initializes the displays to use the image stack data
   * from the given files.
   */
  private void init(File[] files, int index) throws VisADException {
    final File[] f = files;
    final int curfile = index;
    final ProgressDialog dialog = new ProgressDialog(bio, "Loading");

    Thread t = new Thread(new Runnable() {
      public void run() {
        bio.display2.disableAction();
        if (bio.display3 != null) bio.display3.disableAction();
        try {
          clearDisplays();

          // reset measurements
          if (bio.mm.lists != null) bio.mm.clear();

          field = null;
          sliceField = volumeField = null;
          FieldImpl[][] thumbs = null;
          mode_index = mode_slice = 0;

          if (filesAsSlices) {
            // load data at all indices and compile into a single timestep
            slices = f.length;
            timesteps = 1;
            for (int i=0; i<slices; i++) {
              dialog.setText("Loading " + f[i].getName());
              FieldImpl image = BioUtil.loadData(f[i], false);
              if (image == null) return;
              if (field == null) {
                FunctionType stack_type =
                  new FunctionType(SLICE_TYPE, image.getType());
                field = new FieldImpl(stack_type, new Integer1DSet(slices));
              }
              field.setSample(i, image);
              dialog.setPercent(100 * (i + 1) / slices);
            }
            doThumbs = false;
          }
          else if (doThumbs) {
            // load data at all indices and create thumbnails
            timesteps = f.length;
            for (int i=0; i<timesteps; i++) {
              // do current timestep last
              int ndx = i == timesteps - 1 ? curfile :
                (i >= curfile ? i + 1 : i);
              purgeData(false);
              dialog.setText("Loading " + f[ndx].getName());
              field = BioUtil.loadData(f[ndx], true);
              if (field == null) return;
              if (thumbs == null) {
                slices = field.getLength();
                thumbs = new FieldImpl[timesteps][slices];
              }
              for (int j=0; j<slices; j++) {
                FieldImpl image = (FieldImpl) field.getSample(j);
                thumbs[ndx][j] = DualRes.rescale(image, thumbSize);
                dialog.setPercent(
                  100 * (slices * i + j + 1) / (timesteps * slices));
              }
            }
          }
          else {
            // load data at current index only
            timesteps = f.length;
            dialog.setText("Loading " + f[curfile].getName());
            field = BioUtil.loadData(f[curfile], true);
            if (field == null) return;
            slices = field.getLength();
            dialog.setPercent(100);
          }
          if (field == null) return;

          hasThumbs = doThumbs;
          autoSwitch = hasThumbs;

          dialog.setText("Analyzing data");

          // The FieldImpl must be in one of the following forms:
          //     (index -> ((x, y) -> range))
          //     (index -> ((x, y) -> (r1, r2, ..., rn)))
          //
          // dtypes = {x, y, index}; rtypes = {r1, r2, ..., rn}

          // extract types
          FunctionType time_function = (FunctionType) field.getType();
          RealTupleType time_domain = time_function.getDomain();
          MathType time_range = time_function.getRange();
          if (time_domain.getDimension() > 1 ||
            !(time_range instanceof FunctionType))
          {
            throw new VisADException("Field is not an image stack");
          }
          RealType slice_type = (RealType) time_domain.getComponent(0);
          FunctionType image_function = (FunctionType) time_range;
          domain2 = image_function.getDomain();
          RealType[] image_dtypes = domain2.getRealComponents();
          if (image_dtypes.length < 2) {
            throw new VisADException("Data stack does not contain images");
          }
          dtypes = new RealType[] {
            image_dtypes[0], image_dtypes[1], slice_type
          };
          domain3 = new RealTupleType(dtypes);
          MathType range = image_function.getRange();
          if (!(range instanceof RealTupleType) &&
            !(range instanceof RealType))
          {
            throw new VisADException("Invalid field range");
          }
          rtypes = range instanceof RealTupleType ?
            ((RealTupleType) range).getRealComponents() :
            new RealType[] {(RealType) range};

          // convert thumbnails into animation stacks
          lowresField = null;
          if (doThumbs) {
            FunctionType slice_function =
              new FunctionType(slice_type, image_function);
            FunctionType lowres_function =
              new FunctionType(TIME_TYPE, slice_function);
            lowresField = new FieldImpl(lowres_function,
              new Integer1DSet(TIME_TYPE, timesteps));
            Set lowres_set = new Integer1DSet(slice_type, slices);
            for (int j=0; j<timesteps; j++) {
              FieldImpl step = new FieldImpl(slice_function, lowres_set);
              step.setSamples(thumbs[j], false);
              lowresField.setSample(j, step, false);
            }
          }

          dialog.setText("Configuring displays");

          // set new data
          ref2.setData(field);
          ref3.setData(field);
          if (doThumbs) {
            lowresRef2.setData(lowresField);
            lowresRef3.setData(lowresField);
            ref_prev.setData(lowresField);
            ref_next.setData(lowresField);
          }

          bio.brightness = -1; // force color update
          configureDisplays();

          // initialize tool panels
          bio.toolView.init();
          bio.toolColor.init();
          bio.toolAlign.init();
          bio.toolMeasure.init();

          // initialize measurement list array
          bio.mm.initLists(timesteps);
        }
        catch (VisADException exc) { dialog.setException(exc); }
        catch (RemoteException exc) {
          dialog.setException(
            new VisADException("RemoteException: " + exc.getMessage()));
        }

        bio.display2.enableAction();
        if (bio.display3 != null) bio.display3.enableAction();
        bio.state.saveState(); // save initial state file
        dialog.kill();
      }
    });
    t.start();
    dialog.show();
    try { dialog.checkException(); }
    catch (VisADException exc) {
      JOptionPane.showMessageDialog(bio,
        "Cannot import data from " + files[index].getName() + "\n" +
        exc.getMessage(), "Cannot load file", JOptionPane.ERROR_MESSAGE);
      throw exc;
    }
  }

  /** Clears display mappings and references. */
  private void clearDisplays() throws VisADException, RemoteException {
    bio.display2.removeAllReferences();
    bio.display2.clearMaps();
    if (bio.display3 != null) {
      bio.display3.removeAllReferences();
      bio.display3.clearMaps();
      bio.previous.removeAllReferences();
      bio.previous.clearMaps();
      bio.next.removeAllReferences();
      bio.next.clearMaps();
    }
    bio.toolColor.removeAllWidgets();
  }

  /** Configures display mappings and references. */
  private void configureDisplays() throws VisADException, RemoteException {
    // set up mappings to 2-D display
    x_map2 = new ScalarMap(dtypes[0], Display.XAxis);
    y_map2 = new ScalarMap(dtypes[1], Display.YAxis);
    ScalarMap slice_map2 = new ScalarMap(dtypes[2], Display.SelectValue);
    ScalarMap anim_map2 = null;
    ScalarMap r_map2 = new ScalarMap(RED_TYPE, Display.Red);
    ScalarMap g_map2 = new ScalarMap(GREEN_TYPE, Display.Green);
    ScalarMap b_map2 = new ScalarMap(BLUE_TYPE, Display.Blue);
    bio.display2.addMap(x_map2);
    bio.display2.addMap(y_map2);
    bio.display2.addMap(slice_map2);
    if (hasThumbs) {
      anim_map2 = new ScalarMap(TIME_TYPE, Display.Animation);
      bio.display2.addMap(anim_map2);
    }
    bio.display2.addMap(r_map2);
    bio.display2.addMap(g_map2);
    bio.display2.addMap(b_map2);

    // add color maps for all range components
    rmaps2 = new ScalarMap[rtypes.length];
    for (int i=0; i<rtypes.length; i++) {
      rmaps2[i] = new ScalarMap(rtypes[i], Display.RGB);
      // CTR - TODO - color range options configuration
      rmaps2[i].setRange(0, 255);
      bio.display2.addMap(rmaps2[i]);
    }

    // set up 2-D data references
    DisplayRenderer dr2 = bio.display2.getDisplayRenderer();
    boolean on = renderer2 == null ? true : renderer2.getEnabled();
    renderer2 = dr2.makeDefaultRenderer();
    renderer2.toggle(on);
    bio.display2.addReferences(renderer2, ref2);
    on = planeRenderer2 == null ? false : planeRenderer2.getEnabled();
    planeRenderer2 = dr2.makeDefaultRenderer();
    planeRenderer2.suppressExceptions(true);
    planeRenderer2.toggle(on);
    bio.display2.addReferences(planeRenderer2, planeRef);
    if (hasThumbs) {
      on = lowresRenderer2 == null ? false : lowresRenderer2.getEnabled();
      lowresRenderer2 = dr2.makeDefaultRenderer();
      lowresRenderer2.toggle(on);
      bio.display2.addReferences(lowresRenderer2, lowresRef2);
    }
    bio.mm.pool2.init();

    // set up 2-D ranges
    res_x = 0;
    res_y = 0;
    min_x = Float.MAX_VALUE;
    min_y = Float.MAX_VALUE;
    max_x = Float.MIN_VALUE;
    max_y = Float.MIN_VALUE;
    for (int i=0; i<(filesAsSlices ? slices : 1); i++) {
      GriddedSet set = (GriddedSet)
        ((FieldImpl) field.getSample(i)).getDomainSet();
      float[] lo = set.getLow();
      float[] hi = set.getHi();
      int[] lengths = set.getLengths();
      if (res_x < lengths[0]) res_x = lengths[0];
      if (res_y < lengths[1]) res_y = lengths[1];
      if (min_x > lo[0]) min_x = lo[0];
      if (max_x < hi[0]) max_x = hi[0];
      if (min_y > lo[1]) min_y = lo[1];
      if (max_y < hi[1]) max_y = hi[1];
    }

    // x-axis range
    if (min_x != min_x) min_x = 0;
    if (max_x != max_x) max_x = 0;
    x_map2.setRange(min_x, max_x);

    // y-axis range
    if (min_y != min_y) min_y = 0;
    if (max_y != max_y) max_y = 0;
    y_map2.setRange(min_y, max_y);

    // select value range
    min_z = 0;
    max_z = slices - 1;
    slice_map2.setRange(min_z, max_z);

    // color ranges
    r_map2.setRange(0, 255);
    g_map2.setRange(0, 255);
    b_map2.setRange(0, 255);

    // set up mappings to 3-D display
    ScalarMap x_map3 = null;
    ScalarMap y_map3 = null;
    ScalarMap z_map3a = null;
    ScalarMap z_map3b = null;
    ScalarMap anim_map3 = null;
    ScalarMap r_map3 = null;
    ScalarMap g_map3 = null;
    ScalarMap b_map3 = null;
    DisplayRenderer dr3 = null;
    widgets = new LabeledColorWidget[rtypes.length];
    if (bio.display3 == null) {
      for (int i=0; i<rtypes.length; i++) {
        widgets[i] = new LabeledColorWidget(
          new ColorMapWidget(rmaps2[i], false));
        bio.toolColor.addWidget(rmaps2[i].getScalarName(), widgets[i]);
        rmaps2[i].getControl().addControlListener(this);
      }
    }
    else {
      x_map3 = new ScalarMap(dtypes[0], Display.XAxis);
      y_map3 = new ScalarMap(dtypes[1], Display.YAxis);
      z_map3a = new ScalarMap(dtypes[2], Display.ZAxis);
      z_map3b = new ScalarMap(Z_TYPE, Display.ZAxis);
      if (hasThumbs) anim_map3 = new ScalarMap(TIME_TYPE, Display.Animation);
      r_map3 = new ScalarMap(RED_TYPE, Display.Red);
      g_map3 = new ScalarMap(GREEN_TYPE, Display.Green);
      b_map3 = new ScalarMap(BLUE_TYPE, Display.Blue);
      bio.display3.addMap(x_map3);
      bio.display3.addMap(y_map3);
      bio.display3.addMap(z_map3a);
      bio.display3.addMap(z_map3b);
      if (hasThumbs) bio.display3.addMap(anim_map3);
      bio.display3.addMap(r_map3);
      bio.display3.addMap(g_map3);
      bio.display3.addMap(b_map3);

      // add color maps for all range components
      rmaps3 = new ScalarMap[rtypes.length];
      for (int i=0; i<rtypes.length; i++) {
        rmaps3[i] = new ScalarMap(rtypes[i], Display.RGBA);
        // CTR - TODO - color range options configuration
        rmaps3[i].setRange(0, 255);
        bio.display3.addMap(rmaps3[i]);
        widgets[i] = new LabeledColorWidget(
          new ColorMapWidget(rmaps3[i], false));
        bio.toolColor.addWidget(rmaps3[i].getScalarName(), widgets[i]);
        rmaps3[i].getControl().addControlListener(this);
      }

      // set up 3-D data references
      dr3 = bio.display3.getDisplayRenderer();
      on = renderer3 == null ? true : renderer3.getEnabled();
      renderer3 = bio.display3.getDisplayRenderer().makeDefaultRenderer();
      renderer3.toggle(on);
      bio.display3.addReferences(renderer3, ref3);
      if (hasThumbs) {
        on = lowresRenderer3 == null ? false : lowresRenderer3.getEnabled();
        lowresRenderer3 = dr3.makeDefaultRenderer();
        lowresRenderer3.toggle(on);
        bio.display3.addReferences(lowresRenderer3, lowresRef3);
      }
      bio.mm.pool3.init();

      // x-axis and y-axis ranges
      x_map3.setRange(min_x, max_x);
      y_map3.setRange(min_y, max_y);

      // z-axis range
      z_map3a.setRange(min_z, max_z);
      z_map3b.setRange(min_z, max_z);

      // color ranges
      r_map3.setRange(0, 255);
      g_map3.setRange(0, 255);
      b_map3.setRange(0, 255);
    }

    // set up mappings to previous and next displays
    ScalarMap anim_map_prev = null;
    ScalarMap anim_map_next = null;
    if (hasThumbs && bio.previous != null && bio.next != null) {
      rmapsP = new ScalarMap[2][];
      for (int j=0; j<2; j++) {
        ScalarMap x_mapP = new ScalarMap(dtypes[0], Display.XAxis);
        ScalarMap y_mapP = new ScalarMap(dtypes[1], Display.YAxis);
        ScalarMap z_mapPa = new ScalarMap(dtypes[2], Display.ZAxis);
        ScalarMap z_mapPb = new ScalarMap(Z_TYPE, Display.ZAxis);
        ScalarMap anim_mapP = new ScalarMap(TIME_TYPE, Display.Animation);
        ScalarMap r_mapP = new ScalarMap(RED_TYPE, Display.Red);
        ScalarMap g_mapP = new ScalarMap(GREEN_TYPE, Display.Green);
        ScalarMap b_mapP = new ScalarMap(BLUE_TYPE, Display.Blue);
        DisplayImpl display;
        DataReferenceImpl ref;
        if (j == 0) {
          display = bio.previous;
          ref = ref_prev;
          anim_map_prev = anim_mapP;
        }
        else {
          display = bio.next;
          ref = ref_next;
          anim_map_next = anim_mapP;
        }
        display.addMap(x_mapP);
        display.addMap(y_mapP);
        display.addMap(z_mapPa);
        display.addMap(z_mapPb);
        display.addMap(anim_mapP);
        display.addMap(r_mapP);
        display.addMap(g_mapP);
        display.addMap(b_mapP);

        // add color maps for all range components
        rmapsP[j] = new ScalarMap[rtypes.length];
        for (int i=0; i<rtypes.length; i++) {
          rmapsP[j][i] = new ScalarMap(rtypes[i], Display.RGB);
          display.addMap(rmapsP[j][i]);
        }

        // set up preview data reference
        display.addReference(ref);

        // x-axis and y-axis ranges
        x_mapP.setRange(min_x, max_x);
        y_mapP.setRange(min_y, max_y);

        // z-axis range
        z_mapPa.setRange(min_z, max_z);
        z_mapPb.setRange(min_z, max_z);

        // color ranges
        r_mapP.setRange(0, 255);
        g_mapP.setRange(0, 255);
        b_mapP.setRange(0, 255);
      }
    }

    // set up animation controls
    if (value_control2 != null) value_control2.removeControlListener(this);
    if (anim_control2 != null) anim_control2.removeControlListener(this);
    value_control2 = (ValueControl) slice_map2.getControl();
    if (hasThumbs) {
      anim_control2 = (AnimationControl) anim_map2.getControl();
      bio.toolView.setControl(anim_control2);
      anim_control2.addControlListener(this);
      if (bio.display3 != null) {
        anim_control3 = (AnimationControl) anim_map3.getControl();
        anim_control_prev = (AnimationControl) anim_map_prev.getControl();
        anim_control_next = (AnimationControl) anim_map_next.getControl();
      }
    }
    value_control2.addControlListener(this);

    if (bio.display3 != null) {
      // initialize plane selector
      if (arb == null) {
        arb = new ArbitrarySlice(bio.display3);
        arb.addListener(this);
      }
      Color[] arbLines = {Color.cyan, Color.cyan, Color.cyan};
      arb.init(dtypes[0], dtypes[1], dtypes[2],
        RED_TYPE, GREEN_TYPE, BLUE_TYPE, arbLines, Color.white,
        min_x, min_y, min_z, max_x, max_y, max_z, min_x, max_y, max_z);

      // initialize alignment plane
      if (align == null) align = new AlignmentPlane(bio, bio.display3);
      Color[] alignLines = {Color.red, Color.yellow, Color.blue};
      align.init(dtypes[0], dtypes[1], dtypes[2],
        RED_TYPE, GREEN_TYPE, BLUE_TYPE, alignLines, Color.red,
        min_x, min_y, min_z, max_x, max_y, max_z, min_x, max_y, max_z);
    }

    // adjust display aspect ratio
    bio.setAspect(res_x, res_y, Double.NaN);

    // set up display listener for 3-D display
    if (bio.display3 != null) bio.display3.addDisplayListener(this);
  }

  /** Refreshes the current image slice shown onscreen. */
  private void refresh(boolean new_slice, boolean new_index) {
    if (files == null) return;

    // switch index values
    if (new_index) {
      sliceField = null;
      if (!lowres) {
        try { setFile(false); }
        catch (VisADException exc) { exc.printStackTrace(); }
        catch (RemoteException exc) { exc.printStackTrace(); }
      }

      // do volume rendering
      if (volume) updateVolumeField();

      updateStuff();
    }

    // switch slice values
    if (new_slice) {
      bio.mm.pool2.setSlice(slice);

      // update value control
      try { value_control2.setValue(slice); }
      catch (VisADException exc) { exc.printStackTrace(); }
      catch (RemoteException exc) { exc.printStackTrace(); }
    }

    // switch resolution in 2-D display
    if (planeSelect) updateSlice();
    else if (lowres) {
      if (hasThumbs) lowresRenderer2.toggle(true);
      renderer2.toggle(false);
    }
    else {
      renderer2.toggle(true);
      if (hasThumbs) lowresRenderer2.toggle(false);
    }

    // switch resolution in 3-D display
    if (bio.display3 != null) {
      if (lowres) {
        if (hasThumbs) lowresRenderer3.toggle(true);
        renderer3.toggle(false);
      }
      else {
        renderer3.toggle(true);
        if (hasThumbs) lowresRenderer3.toggle(false);
      }
    }
  }

  /** Does the work of setting the resolution mode. */
  private void setMode(boolean lowres, boolean doVolume) {
    bio.toolView.setMode(lowres);
    if (this.lowres == lowres) return;
    this.lowres = lowres;
    volumeField = sliceField = null;
    refresh(mode_slice != slice, mode_index != index);
    mode_index = index;
    mode_slice = slice;
    if (doVolume && volume) doVolumeMode();
  }

  /** Handles volume rendering details when the resolution mode changes. */
  private void doVolumeMode() {
    updateVolumeField();
    try {
      if (lowres) ref3.setData(field);
      else lowresRef3.setData(lowresField);
    }
    catch (VisADException exc) { exc.printStackTrace(); }
    catch (RemoteException exc) { exc.printStackTrace(); }
  }

  /** Updates various important display features. */
  void updateStuff() {
    // update measurement lists
    MeasureList list = bio.mm.lists[index];
    bio.mm.pool2.set(list);
    if (bio.mm.pool3 != null) bio.mm.pool3.set(list);

    // update animation controls
    try {
      if (anim_control2 != null) anim_control2.setCurrent(index);
      if (anim_control3 != null) anim_control3.setCurrent(index);
      if (anim_control_prev != null) anim_control_prev.setCurrent(index - 1);
      if (anim_control_next != null) anim_control_next.setCurrent(index + 1);
    }
    catch (VisADException exc) { exc.printStackTrace(); }
    catch (RemoteException exc) { exc.printStackTrace(); }
  }

  /** Updates 2-D display of arbitrary plane slice. */
  private void updateSlice() {
    bio.setWaitCursor(true);
    try {
      if (sliceField == null) updateSliceField();
      planeRef.setData(arb.extractSlice(sliceField,
        sliceRes_x, sliceRes_y, res_x, res_y));
    }
    catch (VisADException exc) { exc.printStackTrace(); }
    catch (RemoteException exc) { exc.printStackTrace(); }
    bio.setWaitCursor(false);
  }

  /** Updates arbitrary slice field to match the current timestep. */
  private void updateSliceField() {
    try {
      FieldImpl f = lowres ? (FieldImpl) lowresField.getSample(index) : field;
      try { sliceField = (FlatField) f.domainMultiply(); }
      catch (FieldException exc) {
        // images dimensions do not match; resample so that they do
        GriddedSet set = (GriddedSet) f.getDomainSet();
        int len = set.getLengths()[0];
        int res_x = 0;
        int res_y = 0;
        for (int i=0; i<len; i++) {
          FlatField flat = (FlatField) f.getSample(i);
          GriddedSet flat_set = (GriddedSet) flat.getDomainSet();
          int[] l = flat_set.getLengths();
          if (l[0] > res_x) res_x = l[0];
          if (l[1] > res_y) res_y = l[1];
        }
        FieldImpl nf = new FieldImpl((FunctionType) f.getType(), set);
        for (int i=0; i<len; i++) {
          FlatField flat = (FlatField) f.getSample(i);
          GriddedSet flat_set = (GriddedSet) flat.getDomainSet();
          int[] l = flat_set.getLengths();
          if (l[0] > res_x) res_x = l[0];
          if (l[1] > res_y) res_y = l[1];
        }
        for (int i=0; i<len; i++) {
          FlatField flat = (FlatField) f.getSample(i);
          GriddedSet flat_set = (GriddedSet) flat.getDomainSet();
          nf.setSample(i, flat.resample(
            new Integer2DSet(flat_set.getType(), res_x, res_y),
            Data.WEIGHTED_AVERAGE, Data.NO_ERRORS));
        }
        sliceField = (FlatField) nf.domainMultiply();
      }
    }
    catch (VisADException exc) { exc.printStackTrace(); }
    catch (RemoteException exc) { exc.printStackTrace(); }
  }

  /** Updates volume rendering field to match the current timestep. */
  private void updateVolumeField() {
    bio.setWaitCursor(true);
    if (sliceField == null) updateSliceField();
    GriddedSet set = (GriddedSet) sliceField.getDomainSet();
    int[] len = set.getLengths();
    if (len[0] == volumeRes && len[1] == volumeRes && len[2] == volumeRes) {
      volumeField = sliceField;
    }
    else {
      float[] lo = set.getLow();
      float[] hi = set.getHi();
      try {
        Linear3DSet nset = new Linear3DSet(set.getType(), lo[0], hi[0],
          volumeRes, lo[1], hi[1], volumeRes, lo[2], hi[2], volumeRes);
        volumeField = (FlatField) sliceField.resample(nset,
          Data.WEIGHTED_AVERAGE, Data.NO_ERRORS);
      }
      catch (VisADException exc) { exc.printStackTrace(); }
      catch (RemoteException exc) { exc.printStackTrace(); }
    }
    if (volume) {
      try {
        if (lowres) lowresRef3.setData(volumeField);
        else ref3.setData(volumeField);
      }
      catch (VisADException exc) { exc.printStackTrace(); }
      catch (RemoteException exc) { exc.printStackTrace(); }
    }
    bio.setWaitCursor(false);
  }

  void updateAnimationControls() {
    // update animation controls
    try {
      if (anim_control2 != null) anim_control2.setCurrent(index);
      if (anim_control3 != null) anim_control3.setCurrent(index);
      if (anim_control_prev != null) anim_control_prev.setCurrent(index - 1);
      if (anim_control_next != null) anim_control_next.setCurrent(index + 1);
    }
    catch (VisADException exc) { exc.printStackTrace(); }
    catch (RemoteException exc) { exc.printStackTrace(); }
  }

}
